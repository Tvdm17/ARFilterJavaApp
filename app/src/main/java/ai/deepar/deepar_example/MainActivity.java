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
// VIDEO RECORDING is handled by PreviewActivity.
// This activity handles screenshots and filter navigation.
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
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import ai.deepar.ar.ARErrorType;
import ai.deepar.ar.ARTouchInfo;
import ai.deepar.ar.ARTouchType;

/**
 * The main AR camera screen for screenshots and filter navigation.
 *
 * Implements DeepARManager.Listener to receive AR events (screenshot ready, errors)
 * and update the UI accordingly. No direct DeepAR SDK calls happen here.
 *
 * For video recording, see PreviewActivity.
 */
public class MainActivity extends AppCompatActivity implements DeepARManager.Listener {

    // -------------------------------------------------------
    // MANAGERS
    // -------------------------------------------------------

    /** [DEEPAR via manager] Owns the DeepAR engine — effects, screenshot, rendering. */
    private DeepARManager deepARManager;

    /** [ANDROID via manager] Owns the CameraX pipeline — frame capture and camera switching. */
    private CameraManager cameraManager;

    // ============================================================
    // LIFECYCLE
    // ============================================================

    /**
     * [ANDROID] Inflate the layout only. All initialization waits for onStart
     * so DeepAR properly restarts after returning from another activity.
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
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupViews() {
        SurfaceView arView = findViewById(R.id.surface);

        // [DEEPAR] Register the render surface — DeepAR will draw into this SurfaceView.
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

        // ---- Previous / Next filter arrows ----
        findViewById(R.id.previousMask).setOnClickListener(v -> deepARManager.gotoPrevious());
        findViewById(R.id.nextMask).setOnClickListener(v -> deepARManager.gotoNext());

        // ---- Switch camera (front ↔ back) ----
        findViewById(R.id.switchCamera).setOnClickListener(v -> cameraManager.switchCamera());

        // ---- Screenshot button ----
        findViewById(R.id.screenshotButton).setOnClickListener(v -> deepARManager.takeScreenshot());
    }

    // ============================================================
    // DeepARManager.Listener — UI responses to AR events
    // ============================================================

    /**
     * [DEEPAR via listener] Screenshot bitmap is ready.
     * Save it as a JPEG to the public Pictures folder and notify the media scanner.
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
     * [DEEPAR via listener] DeepAR is ready. Apply effect from Intent if provided.
     */
    @Override
    public void onInitialized() {
        String effectName = getIntent().getStringExtra("EFFECT_NAME");
        if (effectName != null) {
            deepARManager.switchEffect(effectName);
        }
    }

    /**
     * [DEEPAR via listener] A DeepAR error occurred.
     * LICENSE_AUTHENTICATION_FAILED = license key doesn't match applicationId.
     */
    @Override
    public void onError(ARErrorType type, String message) {
        Toast.makeText(this, "DeepAR error: " + message, Toast.LENGTH_LONG).show();
    }

    // Recording is handled by PreviewActivity — these are no-ops here.
    @Override public void onVideoRecordingStarted() {}
    @Override public void onVideoRecordingFinished() {}
    @Override public void onVideoRecordingFailed() {}
}
