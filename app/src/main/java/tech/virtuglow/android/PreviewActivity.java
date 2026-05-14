package tech.virtuglow.android;

// ============================================================
// PreviewActivity — full-screen camera preview with a fixed DeepAR filter
// ============================================================
// Responsibilities:
//   - Show the camera feed with a DeepAR filter (passed via Intent extra "EFFECT_NAME")
//   - Capture button: tap = screenshot, hold = record video, release = stop recording
//   - Switch between front and back camera
//
// Starting this activity:
//   Intent intent = new Intent(context, PreviewActivity.class);
//   intent.putExtra("EFFECT_NAME", "MakeupLook.deepar");
//   startActivity(intent);
//
// ALL DeepAR logic lives in DeepARManager.java
// ALL CameraX logic lives in CameraManager.java
// ============================================================

import static android.os.Environment.getExternalStoragePublicDirectory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ai.deepar.ar.ARErrorType;
import ai.deepar.ar.ARTouchInfo;
import ai.deepar.ar.ARTouchType;

/**
 * Full-screen camera preview with a single DeepAR filter and capture controls.
 *
 * Capture button behaviour:
 *   - Tap  → screenshot saved to Pictures/
 *   - Hold → video recording starts; release stops it and saves to Movies/
 *
 * Implements DeepARManager.Listener to receive AR callbacks (screenshot ready,
 * recording started/finished, errors) and update the UI accordingly.
 * No direct DeepAR SDK calls happen here — everything goes through DeepARManager.
 */
public class PreviewActivity extends AppCompatActivity implements DeepARManager.Listener {

    // -------------------------------------------------------
    // MANAGERS
    // -------------------------------------------------------

    /** [DEEPAR via manager] Owns the DeepAR engine — effects, recording, rendering. */
    private DeepARManager deepARManager;

    /** [ANDROID via manager] Owns the CameraX pipeline — frame capture and camera switching. */
    private CameraManager cameraManager;

    // -------------------------------------------------------
    // UI STATE
    // -------------------------------------------------------

    /** True while a video is being recorded. Used to toggle start/stop on button press. */
    private boolean recording = false;

    /** Output file for the current video recording. */
    private File videoFile;

    // REC indicator shown while recording
    private LinearLayout recDisplay;   // container — hidden when not recording
    private View recDot;               // blinking red dot
    private TextView recTimer;         // mm:ss elapsed counter
    private Handler timerHandler;      // drives both the blink and the timer tick
    private Runnable timerRunnable;    // ticks every second to update recTimer text
    private long recordingStartTime;   // System.currentTimeMillis() when recording began

    // Long-press detection: ACTION_DOWN starts a delayed runnable;
    // if finger lifts before it fires it's a short press (screenshot),
    // if it fires it starts recording and ACTION_UP then stops it.
    private static final long LONG_PRESS_THRESHOLD = 500; // ms before hold triggers recording
    private final Handler captureHandler = new Handler();
    private Runnable longPressRunnable;

    private int currentIdx = 0;



    // ============================================================
    // LIFECYCLE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        String cameraMode = getIntent().getStringExtra("CAMERA_MODE");
        ImageButton btnRecordVideo = findViewById(R.id.recordButton);

        if ("PICTURE_ONLY".equals(cameraMode)) {
            // hide the video button if they haven't bought the item yet
            btnRecordVideo.setVisibility(View.GONE);
            Toast.makeText(this, "Purchase to unlock Video Recording!", Toast.LENGTH_LONG).show();
        } else {
            // standard mode
            btnRecordVideo.setVisibility(View.VISIBLE);
        }

    }

    /**
     * [ANDROID] Called every time the activity becomes visible (including after resume).
     * Checks camera permission then initializes everything.
     */
    @Override
    protected void onStart() {
        super.onStart();
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        if (!cameraGranted || !audioGranted) {
            // Both permissions are required: camera for the preview, audio for video recording
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            initialize();
        }
    }

    /** [ANDROID] Permission dialog result. If camera permission granted, initialize. */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) return;
            }
            initialize();
        }
    }

    /**
     * [ANDROID] Called when the activity is no longer visible.
     * Release camera and DeepAR to free GPU/hardware for other apps.
     */
    @Override
    protected void onStop() {
        recording = false;
        if (cameraManager != null) cameraManager.release();
        if (deepARManager != null) deepARManager.release();
        super.onStop();
    }

    /** [ANDROID] Final cleanup when the activity is destroyed. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deepARManager != null) deepARManager.release();
    }

    // ============================================================
    // INITIALIZATION
    // ============================================================

    /**
     * Creates the managers and sets up the UI.
     * DeepARManager must be created before CameraManager
     * because CameraManager needs a reference to it for frame delivery.
     */
    private void initialize() {
        deepARManager = new DeepARManager(this, this);
        deepARManager.initialize();

        cameraManager = new CameraManager(this, deepARManager);
        cameraManager.setupCamera();

        setupViews();
    }

    // ============================================================
    // UI SETUP
    // ============================================================

    @SuppressLint("ClickableViewAccessibility")
    private void setupViews() {
        SurfaceView arView = findViewById(R.id.surface);

        // [DEEPAR] Register the render surface.
        arView.getHolder().addCallback(deepARManager.getSurfaceCallback());

        // [ANDROID] Force onSurfaceChanged to fire even if the surface already existed.
        arView.setVisibility(View.GONE);
        arView.setVisibility(View.VISIBLE);

        // [DEEPAR] Forward touch events so interactive effects can respond to screen taps/drags.
        arView.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    deepARManager.touchOccurred(new ARTouchInfo(motionEvent.getX(), motionEvent.getY(), ARTouchType.Start));
                    return true;
                case MotionEvent.ACTION_MOVE:
                    deepARManager.touchOccurred(new ARTouchInfo(motionEvent.getX(), motionEvent.getY(), ARTouchType.Move));
                    return true;
                case MotionEvent.ACTION_UP:
                    deepARManager.touchOccurred(new ARTouchInfo(motionEvent.getX(), motionEvent.getY(), ARTouchType.End));
                    return true;
            }
            return false;
        });

        // REC indicator views
        recDisplay = findViewById(R.id.recDisplay);
        recDot = findViewById(R.id.recDot);
        recTimer = findViewById(R.id.recTimer);
        timerHandler = new Handler();

        // Back button — return to the previous screen
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Switch camera (front ↔ back)
        findViewById(R.id.switchCamera).setOnClickListener(v -> cameraManager.switchCamera());

        // Capture button: short press = screenshot, hold = record video, release = stop
        ImageButton captureButton = findViewById(R.id.recordButton);
        captureButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    longPressRunnable = () -> startRecording();
                    captureHandler.postDelayed(longPressRunnable, LONG_PRESS_THRESHOLD);
                    return true;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    captureHandler.removeCallbacks(longPressRunnable);
                    if (recording) {
                        stopRecording();
                    } else {
                        deepARManager.takeScreenshot();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    captureHandler.removeCallbacks(longPressRunnable);
                    if (recording) stopRecording();
                    return true;
            }
            return false;
        });
    }

    /** Creates the output file and tells DeepAR to start encoding. Sets recording = true. */
    private void startRecording() {
        videoFile = new File(
                getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "video_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".mp4"
        );
        // Half the screen resolution keeps file size reasonable.
        // & ~16 rounds down to the nearest multiple of 16 — required by the H.264 encoder.
        int recordWidth  = (cameraManager.getWidth()  / 2) & ~16;
        int recordHeight = (cameraManager.getHeight() / 2) & ~16;
        deepARManager.startVideoRecording(videoFile.toString(), recordWidth, recordHeight);
        recording = true;
    }

    /**
     * Tells DeepAR to finish encoding and broadcasts a media-scanner intent so the
     * saved video appears in the Gallery immediately without a reboot.
     * recording = false is set later in onVideoRecordingFinished() once the file is fully written.
     */
    private void stopRecording() {
        deepARManager.stopVideoRecording();
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(videoFile));
        sendBroadcast(scanIntent);
    }

    // ============================================================
    // DeepARManager.Listener — UI responses to AR events
    // ============================================================

    /**
     * [DEEPAR via listener] DeepAR is ready — apply the filter passed via Intent.
     */
    @Override
    public void onInitialized() {

        int id = getIntent().getIntExtra("MAKEOVER_ID", -1);

        String selectedFilter = getIntent().getStringExtra("EFFECT_NAME");

        if (id != -1) {
            Makeover item = DatabaseManager.getMakeoverById(id);

            if (item != null) {
                downloadAndApply(item.getDeeparFileName());
            } else {
                loadMakeoversFromDatabase();
            }
        } else if (selectedFilter != null && !selectedFilter.isEmpty()) {
            downloadAndApply(selectedFilter);
        } else {
            loadMakeoversFromDatabase();
        }
    }

    private void downloadAndApply(String fileName) {
        android.util.Log.d("DOWNLOAD_DEBUG", "Attempting to download: [" + fileName + "]");

        DatabaseManager.downloadEffect(this, fileName, new DatabaseManager.FileCallback() {
            @Override
            public void onLoaded(String localPath) {
                deepARManager.switchEffect(localPath);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(PreviewActivity.this, "Filter download failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMakeoversFromDatabase() {
        DatabaseManager.fetchFromAPI("get_all_makeovers", new DatabaseManager.APICallback(){
            @Override
            public void onSuccess(org.json.JSONArray response) {
                try {
                    DatabaseManager.ownedMakeovers.clear();
                    for (int i = 0; i < response.length(); i++) {
                        org.json.JSONObject obj = response.getJSONObject(i);
                        DatabaseManager.ownedMakeovers.add(new Makeover(
                                obj.getInt("makeoverID"),
                                obj.getString("name"),
                                obj.getString("deeparFile"),
                                obj.getString("imagePreview"),
                                obj.optDouble("price", 0.0),
                                obj.optDouble("averageRating", 0.0)
                        ));
                    }


                    if (!DatabaseManager.ownedMakeovers.isEmpty()) {
                        applyMakeover(0);
                    }
                } catch (Exception e) {
                    android.util.Log.e("PreviewActivity", "JSON Parsing error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(String msg) {
                Toast.makeText(PreviewActivity.this, "Failed to load makeovers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyMakeover(int index) {

        if (index < 0 || index >= DatabaseManager.ownedMakeovers.size()) return;
        Makeover item = DatabaseManager.ownedMakeovers.get(index);
        downloadAndApply(item.getDeeparFileName());

    }

    /**
     * [DEEPAR via listener] Video recording has started — show REC indicator.
     */
    @Override
    public void onVideoRecordingStarted() {
        recordingStartTime = System.currentTimeMillis();
        recDisplay.setVisibility(View.VISIBLE);

        Runnable blinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (!recording) return;
                recDot.setVisibility(recDot.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                timerHandler.postDelayed(this, 500);
            }
        };

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!recording) return;
                long elapsed = System.currentTimeMillis() - recordingStartTime;
                long seconds = (elapsed / 1000) % 60;
                long minutes = (elapsed / 1000) / 60;
                recTimer.setText(String.format(Locale.getDefault(), "  %02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(blinkRunnable);
        timerHandler.post(timerRunnable);
    }

    /** [DEEPAR via listener] Video file is fully written. */
    @Override
    public void onVideoRecordingFinished() {
        recording = false;
        recDisplay.setVisibility(View.GONE);
        timerHandler.removeCallbacksAndMessages(null);
        recTimer.setText("  00:00");
        Toast.makeText(this, "Video saved.", Toast.LENGTH_SHORT).show();
    }

    /** [DEEPAR via listener] Recording failed. */
    @Override
    public void onVideoRecordingFailed() {
        recording = false;
        Toast.makeText(this, "Recording failed.", Toast.LENGTH_SHORT).show();
    }

    /** [DEEPAR via listener] Screenshot bitmap is ready — compress and save to Pictures/. */
    @Override
    public void onScreenshotTaken(Bitmap bitmap) {
        File picturesDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File screenshotFile = new File(picturesDir,
                "screenshot_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".jpg");
        try (FileOutputStream out = new FileOutputStream(screenshotFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(screenshotFile));
            sendBroadcast(scanIntent);
            runOnUiThread(() -> Toast.makeText(this, "Screenshot saved.", Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(this, "Screenshot failed.", Toast.LENGTH_SHORT).show());
        }
    }

    /** [DEEPAR via listener] A DeepAR error occurred. */
    @Override
    public void onError(ARErrorType type, String message) {
        Toast.makeText(this, "DeepAR error: " + message, Toast.LENGTH_LONG).show();
    }
}
