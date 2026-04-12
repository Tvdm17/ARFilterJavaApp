package ai.deepar.deepar_example;

// ============================================================
// MainActivity — UI only. No DeepAR imports. No CameraX imports.
// ============================================================
// This class is responsible for:
//   - Android lifecycle (onCreate, onStart, onStop, onDestroy)
//   - Camera permission request
//   - Wiring UI buttons to DeepARManager and CameraManager
//   - Reacting to DeepAR events (via DeepARManager.Listener) to update the UI
//
// ALL DeepAR logic lives in DeepARManager.java
// ALL CameraX logic lives in CameraManager.java
//
// HOW TO BUILD A NEW UI:
//   1. Replace R.layout.activity_main with your own layout
//   2. Keep the SurfaceView (id="surface") in your layout — DeepAR renders there
//   3. Rewrite setupViews() with your own button/gesture setup
//   4. Call the same manager methods: deepARManager.gotoNext(), .takeScreenshot(), etc.
//   5. Implement DeepARManager.Listener to react to screenshots, errors, etc.
//
// ANDROID STUDIO NOTES:
//   - applicationId "tech.virtuglow.demoandroid" must match the DeepAR license key
//   - .deepar filter files must be in app/src/main/assets/
//   - The SurfaceView MUST have getHolder().addCallback(deepARManager.getSurfaceCallback())
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
import android.text.format.DateFormat;
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
import java.text.SimpleDateFormat;
import java.util.Date;

import ai.deepar.ar.ARErrorType;
import ai.deepar.ar.ARTouchInfo;
import ai.deepar.ar.ARTouchType;

/**
 * The main AR camera screen.
 *
 * Implements DeepARManager.Listener to receive AR events (screenshot ready,
 * recording started, errors) and update the UI accordingly.
 * No direct DeepAR SDK calls happen here.
 */
public class MainActivity extends AppCompatActivity implements DeepARManager.Listener {

    // -------------------------------------------------------
    // MANAGERS
    // -------------------------------------------------------

    /** [DEEPAR via manager] Owns the DeepAR engine — effects, screenshot, recording, rendering. */
    private DeepARManager deepARManager;

    /** [ANDROID via manager] Owns the CameraX pipeline — frame capture and camera switching. */
    private CameraManager cameraManager;

    // -------------------------------------------------------
    // UI STATE
    // -------------------------------------------------------

    /** True while a video is being recorded. Used to toggle start/stop on button press. */
    private boolean recording = false;

    /**
     * Tracks which capture mode is active:
     *   false = screenshot mode (default)
     *   true  = video recording mode
     */
    private boolean isRecordingMode = false;

    /** Output file for the current video recording. */
    private File videoFile;

    //FOR RECORDING
    private LinearLayout recDisplay;
    private View recDot;
    private TextView recTimer;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long recordingStartTime;

    // ============================================================
    // LIFECYCLE
    // ============================================================

    /**
     * [ANDROID] Inflate the layout only. All initialization waits for onStart
     * so DeepAR properly restarts after returning from another activity.
     *
     * NEW UI: swap R.layout.activity_main with your own layout here.
     * Keep a SurfaceView with id="surface" in that layout.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * [ANDROID] Called every time the activity becomes visible (including after resume).
     * Checks camera permission then initializes everything.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // [ANDROID] Request camera permission — user sees a system dialog.
            // Result is delivered to onRequestPermissionsResult() below.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            initialize();
        }
    }

    /**
     * [ANDROID] Permission dialog result. If all permissions granted, initialize.
     */
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
     * Both will reinitialize when the user returns (onStart).
     */
    @Override
    protected void onStop() {
        recording = false;
        isRecordingMode = false;
        if (cameraManager != null) cameraManager.release();
        if (deepARManager != null) deepARManager.release();
        super.onStop();
    }

    /**
     * [ANDROID] Final cleanup when the activity is destroyed.
     * Belt-and-suspenders release in case onStop didn't run (edge cases).
     */
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
     * Order matters: DeepARManager must be created before CameraManager
     * because CameraManager needs a reference to it for frame delivery.
     */
    private void initialize() {
        deepARManager = new DeepARManager(this, this); // 'this' = DeepARManager.Listener
        deepARManager.initialize(); // Starts DeepAR engine + fires initialized() when ready

        cameraManager = new CameraManager(this, deepARManager);
        cameraManager.setupCamera(); // Starts CameraX pipeline

        setupViews();
    }

    // ============================================================
    // UI SETUP
    // ============================================================

    /**
     * Wires all buttons to manager calls.
     *
     * NEW UI: Replace this method entirely. Just keep:
     *   1. arView.getHolder().addCallback(deepARManager.getSurfaceCallback())
     *   2. arView.setVisibility(GONE → VISIBLE) trick
     *   3. arView.setOnTouchListener forwarding to deepARManager.touchOccurred()
     *      (only needed for interactive effects like Ping_Pong)
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupViews() {
        SurfaceView arView = findViewById(R.id.surface);

        // [DEEPAR] Register the render surface — DeepAR will draw into this SurfaceView.
        // getSurfaceCallback() returns DeepARManager itself (it implements SurfaceHolder.Callback).
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

        //Recording elements:
        recDisplay = findViewById(R.id.recDisplay);
        recDot = findViewById(R.id.recDot);
        recTimer = findViewById(R.id.recTimer);
        timerHandler = new Handler();


        // ---- Previous / Next filter arrows ----
        findViewById(R.id.previousMask).setOnClickListener(v -> deepARManager.gotoPrevious());
        findViewById(R.id.nextMask).setOnClickListener(v -> deepARManager.gotoNext());

        // ---- Switch camera (front ↔ back) ----
        findViewById(R.id.switchCamera).setOnClickListener(v -> cameraManager.switchCamera());

        // ---- Mode toggle buttons (Screenshot / Record) ----
        final ImageButton actionBtn = findViewById(R.id.recordButton);
        final TextView screenshotModeBtn = findViewById(R.id.screenshotModeButton);
        final TextView recordModeBtn = findViewById(R.id.recordModeButton);

        // Start in screenshot mode: screenshot tab highlighted, record tab dim
        screenshotModeBtn.getBackground().setAlpha(0xA0);
        recordModeBtn.getBackground().setAlpha(0x00);

        // Default action: take screenshot
        actionBtn.setOnClickListener(v -> deepARManager.takeScreenshot());

        screenshotModeBtn.setOnClickListener(v -> {
            if (!isRecordingMode) return; // Already in screenshot mode
            if (recording) {
                Toast.makeText(this, "Cannot switch while recording!", Toast.LENGTH_SHORT).show();
                return;
            }
            isRecordingMode = false;
            screenshotModeBtn.getBackground().setAlpha(0xA0);
            recordModeBtn.getBackground().setAlpha(0x00);
            actionBtn.setOnClickListener(btn -> deepARManager.takeScreenshot());
        });

        recordModeBtn.setOnClickListener(v -> {
            if (isRecordingMode) return; // Already in record mode
            isRecordingMode = true;
            recordModeBtn.getBackground().setAlpha(0xA0);
            screenshotModeBtn.getBackground().setAlpha(0x00);
            actionBtn.setOnClickListener(btn -> toggleRecording());
        });
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
            Toast.makeText(this, "Recording " + videoFile.getName() + " saved.", Toast.LENGTH_LONG).show();
        } else {
            // ---- Start recording ----
            // [ANDROID] WRITE_EXTERNAL_STORAGE permission required on Android ≤ 12 (API ≤ 32)
            videoFile = new File(
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "video_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".mp4"
            );
            // Record at half screen resolution to save space — change to full if needed
            deepARManager.startVideoRecording(videoFile.toString(),
                    cameraManager.getWidth() / 2,
                    cameraManager.getHeight() / 2);
            Toast.makeText(this, "Recording started.", Toast.LENGTH_SHORT).show();
        }
        recording = !recording;
    }

    // ============================================================
    // DeepARManager.Listener — UI responses to AR events
    // ============================================================

    /**
     * [DEEPAR via listener] Screenshot bitmap is ready.
     * Save it as a JPEG to the public Pictures folder and notify the media scanner.
     *
     * NEW UI: You could also display the bitmap in an ImageView preview,
     * or open a share sheet instead of saving automatically.
     */
    @Override
    public void onScreenshotTaken(Bitmap bitmap) {
        CharSequence timestamp = DateFormat.format("yyyy_MM_dd_hh_mm_ss", new Date());
        try {
            File imageFile = new File(
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "image_" + timestamp + ".jpg"
            );
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            // [ANDROID] Make the file visible in Gallery without a device reboot
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(imageFile));
            sendBroadcast(scanIntent);

            Toast.makeText(this, "Screenshot " + imageFile.getName() + " saved.", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * [DEEPAR via listener] Video recording has started.
     * NEW UI: Show a "REC" dot or start a recording timer here.
     */
    @Override
    public void onVideoRecordingStarted() {
        recordingStartTime = System.currentTimeMillis();
        recDisplay.setVisibility(View.VISIBLE);

        Runnable blinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (!recording) return;

                if (recDot.getVisibility() == View.VISIBLE){
                    recDot.setVisibility(View.INVISIBLE);
                }
                else{
                    recDot.setVisibility(View.VISIBLE);
                }
                timerHandler.postDelayed(this, 500);
            }
        };

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!recording) return;
                else{
                    long elapsed = System.currentTimeMillis() - recordingStartTime;
                    long seconds = (elapsed / 1000) % 60;
                    long minutes = (elapsed / 1000) / 60;
                    recTimer.setText(String.format("  %02d:%02d", minutes, seconds));
                    timerHandler.postDelayed(this, 1000);
                }
                }
        };
        timerHandler.post(blinkRunnable);
        timerHandler.post(timerRunnable);
    }

    /**
     * [DEEPAR via listener] Video file is fully written.
     * NEW UI: Show a "Saved" confirmation or thumbnail preview.
     */
    @Override
    public void onVideoRecordingFinished() {
        recording = false;
        recDisplay.setVisibility(View.GONE);
        timerHandler.removeCallbacksAndMessages(null);
        recTimer.setText("  00:00");
        Toast.makeText(this, "Video saved.", Toast.LENGTH_SHORT).show();
    }

    /**
     * [DEEPAR via listener] Recording failed.
     * NEW UI: Show an error message. recording flag should be reset here too.
     */
    @Override
    public void onVideoRecordingFailed() {
        recording = false;
        Toast.makeText(this, "Recording failed.", Toast.LENGTH_SHORT).show();
    }

    /**
     * [DEEPAR via listener] DeepAR is ready. Nothing to do here —
     * DeepARManager.initialized() already restores the current effect internally.
     */
    @Override
    public void onInitialized() {
        // DeepARManager already applied the current effect
    }

    /**
     * [DEEPAR via listener] A DeepAR error occurred.
     * LICENSE_AUTHENTICATION_FAILED = license key doesn't match applicationId.
     * NEW UI: Show a user-facing error dialog for license failures.
     */
    @Override
    public void onError(ARErrorType type, String message) {
        Toast.makeText(this, "DeepAR error: " + message, Toast.LENGTH_LONG).show();
    }
}
