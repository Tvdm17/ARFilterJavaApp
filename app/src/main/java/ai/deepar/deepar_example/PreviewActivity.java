package ai.deepar.deepar_example;

// ============================================================
// PreviewActivity — camera preview with a fixed filter + video recording
// ============================================================
// Responsibilities:
//   - Show the camera feed with a DeepAR filter applied (passed via Intent "EFFECT_NAME")
//   - Let the user record a video (single record button, no screenshot mode)
//   - Switch between front and back camera
//
// How the filter is set:
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import ai.deepar.ar.ARErrorType;
import ai.deepar.ar.ARTouchInfo;
import ai.deepar.ar.ARTouchType;

/**
 * Camera preview screen with a DeepAR filter and video recording.
 *
 * Implements DeepARManager.Listener to receive AR events (recording started/finished,
 * errors) and update the UI accordingly.
 * No direct DeepAR SDK calls happen here.
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

    // REC indicator views
    private LinearLayout recDisplay;
    private View recDot;
    private TextView recTimer;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long recordingStartTime;

    // ============================================================
    // LIFECYCLE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
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

        // Record button — toggles recording on/off
        findViewById(R.id.recordButton).setOnClickListener(v -> toggleRecording());
    }

    /**
     * Starts or stops video recording depending on current state.
     * File saved to Movies directory with a timestamped filename.
     * Recording resolution = half screen size to balance quality and file size.
     */
    private void toggleRecording() {
        if (recording) {
            // ---- Stop recording ----
            deepARManager.stopVideoRecording();
            // [ANDROID] Notify media scanner so video appears in Gallery immediately
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(videoFile));
            sendBroadcast(scanIntent);
        } else {
            // ---- Start recording ----
            videoFile = new File(
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "video_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".mp4"
            );
            // & ~1 rounds down to the nearest even number — bc  Android video encoder (H.264) requires both dimensions to be even
            int recordWidth  = (cameraManager.getWidth()  / 2) & ~16;
            int recordHeight = (cameraManager.getHeight() / 2) & ~16;
            deepARManager.startVideoRecording(videoFile.toString(), recordWidth, recordHeight);
        }
        recording = !recording;
    }

    // ============================================================
    // DeepARManager.Listener — UI responses to AR events
    // ============================================================

    /**
     * [DEEPAR via listener] DeepAR is ready — apply the filter passed via Intent.
     */
    @Override
    public void onInitialized() {
        String effectName = getIntent().getStringExtra("EFFECT_NAME");
        if (effectName != null) {
            deepARManager.switchEffect(effectName);
        }
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
                recTimer.setText(String.format("  %02d:%02d", minutes, seconds));
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

    /** Not used — PreviewActivity has no screenshot functionality. */
    @Override
    public void onScreenshotTaken(Bitmap bitmap) {}

    /** [DEEPAR via listener] A DeepAR error occurred. */
    @Override
    public void onError(ARErrorType type, String message) {
        Toast.makeText(this, "DeepAR error: " + message, Toast.LENGTH_LONG).show();
    }
}
