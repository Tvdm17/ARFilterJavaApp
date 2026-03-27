package ai.deepar.deepar_example;

// ============================================================
// ANDROID STUDIO IMPORT NOTES
// ============================================================
// When opening this project in Android Studio:
//
// 1. GRADLE SYNC: Let Android Studio sync Gradle automatically.
//    The DeepAR SDK is fetched from a custom Maven repo defined in
//    the ROOT build.gradle (not the app one). Make sure internet is
//    available on first sync.
//
// 2. LICENSE KEY: The DeepAR license key (in initializeDeepAR()) is
//    tied to the applicationId in app/build.gradle:
//      applicationId "tech.virtuglow.demoandroid"
//    If you rename the package or change applicationId, you MUST get
//    a new license key from https://developer.deepar.ai
//
// 3. ASSETS: The .deepar filter files (e.g. viking_helmet.deepar) must
//    be placed in app/src/main/assets/. They are NOT included in this
//    repo — download them separately from the DeepAR developer portal.
//    Without them the app launches but shows a blank/no-effect camera.
//
// 4. CAMERA PERMISSION: This activity requests CAMERA at runtime (onStart).
//    On Android 6+ the user sees a system permission dialog. If denied,
//    nothing initializes. There is no retry UI — handle that if needed.
//
// 5. NEW UI PLAN: The current UI (buttons, arrows) is inside activity_main.xml.
//    The SurfaceView (id: @+id/surface) is the AR render target — keep it.
//    You can replace/redesign every button/layout around it freely.
//    Just don't remove the SurfaceView or its SurfaceHolder.Callback binding.
//
// 6. NAMESPACE vs APPLICATION ID mismatch:
//    - namespace (Java package): ai.deepar.deepar_example
//    - applicationId (Play Store/device ID): tech.virtuglow.demoandroid
//    These are intentionally different. Don't confuse them.
// ============================================================

import static android.os.Environment.getExternalStoragePublicDirectory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

// [ANDROID] Standard AndroidX imports
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

// [ANDROID] Guava's ListenableFuture — used by CameraX to return async results
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

// [DEEPAR] All ai.deepar.ar.* imports are from the DeepAR SDK (ai.deepar.ar:DeepAR:5.6.20)
import ai.deepar.ar.ARErrorType;       // [DEEPAR] Error type enum for AR events
import ai.deepar.ar.AREventListener;   // [DEEPAR] Callback interface for all AR lifecycle events
import ai.deepar.ar.ARTouchInfo;       // [DEEPAR] Wraps touch coordinates + touch phase for AR interactions
import ai.deepar.ar.ARTouchType;       // [DEEPAR] Enum: Start / Move / End (maps to ACTION_DOWN/MOVE/UP)
import ai.deepar.ar.CameraResolutionPreset; // [DEEPAR] Predefined resolution constants (e.g. P1920x1080)
import ai.deepar.ar.DeepAR;            // [DEEPAR] The main DeepAR engine object — handles rendering, effects, capture
import ai.deepar.ar.DeepARImageFormat; // [DEEPAR] Enum for frame format (RGBA_8888, NV21, etc.)

/**
 * MainActivity — the single screen that runs the AR camera.
 *
 * [ANDROID] AppCompatActivity: standard base class for activities using modern AndroidX features.
 *
 * [ANDROID] SurfaceHolder.Callback: interface that notifies us when the SurfaceView's drawing
 *   surface is created, changed, or destroyed. We use it to tell DeepAR where to render.
 *
 * [DEEPAR] AREventListener: DeepAR callback interface. Must be implemented to receive events like
 *   screenshotTaken, videoRecordingStarted, initialized, error, etc.
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, AREventListener {

    // -------------------------------------------------------
    // CAMERA CONFIGURATION
    // -------------------------------------------------------

    /**
     * [ANDROID] CameraSelector constant for which camera to open by default.
     * LENS_FACING_FRONT = selfie camera, LENS_FACING_BACK = rear camera.
     * Change this if you want to start with rear camera.
     */
    private final int defaultLensFacing = CameraSelector.LENS_FACING_FRONT;

    /**
     * [DEEPAR] ARSurfaceProvider wraps DeepAR's OpenGL texture into a CameraX-compatible surface.
     * Only used when useExternalCameraTexture = true (the "texture" code path).
     * Currently NOT active (useExternalCameraTexture = false).
     */
    private ARSurfaceProvider surfaceProvider = null;

    /** [ANDROID] Tracks which camera is currently active (front or back). */
    private int lensFacing = defaultLensFacing;

    /**
     * [ANDROID] CameraX uses ListenableFuture to asynchronously return the ProcessCameraProvider.
     * Store it as a field so we can unbind cameras when switching or stopping.
     */
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // -------------------------------------------------------
    // FRAME BUFFER SETUP
    // -------------------------------------------------------

    /**
     * [ANDROID / DEEPAR] Double-buffering strategy to prevent frame drops.
     * While DeepAR processes one frame, CameraX writes the next into the other buffer.
     * NUMBER_OF_BUFFERS = 2 means we alternate between two ByteBuffers.
     *
     * Each buffer holds one full RGBA_8888 frame: width * height * 4 bytes.
     */
    private ByteBuffer[] buffers;
    private int currentBuffer = 0;
    private static final int NUMBER_OF_BUFFERS = 2;

    /**
     * [DEEPAR] Toggle between two camera-to-DeepAR integration modes:
     *
     *   false (current / default):
     *     CameraX -> ImageAnalysis -> ByteBuffer -> deepAR.receiveFrame()
     *     We manually copy pixel data from the camera into a ByteBuffer
     *     and push it to DeepAR on the main thread.
     *
     *   true (alternate / GL texture path):
     *     CameraX -> Preview -> ARSurfaceProvider -> deepAR.receiveFrameExternalTexture()
     *     Camera frames go directly into an OpenGL texture. More efficient for
     *     high resolutions, but requires EGL context setup.
     *
     * NEW UI NOTE: If you redesign the UI, you don't need to change this.
     * Keep false for simplicity unless you have GPU performance needs.
     */
    private static final boolean useExternalCameraTexture = false;

    // -------------------------------------------------------
    // DEEPAR ENGINE
    // -------------------------------------------------------

    /**
     * [DEEPAR] The core DeepAR object. Manages:
     *   - Face tracking and AR rendering
     *   - Effect loading and switching
     *   - Screenshot / video recording
     *   - Sending frames for processing
     *
     * Must be initialized with a valid license key and released when done.
     */
    private DeepAR deepAR;

    // -------------------------------------------------------
    // EFFECT / FILTER STATE
    // -------------------------------------------------------

    /** [DEEPAR] Index into the 'effects' list pointing to the currently applied filter. */
    private int currentEffect = 0;

    /** [ANDROID] Screen orientation constant (portrait/landscape) used to rotate frames correctly. */
    private int screenOrientation;

    /**
     * [DEEPAR] Ordered list of effect filenames. "none" means no filter.
     * All other entries are .deepar asset files that must exist in app/src/main/assets/.
     * NEW UI: You can populate this from a server, database, or dynamic asset download.
     */
    ArrayList<String> effects;

    // -------------------------------------------------------
    // RECORDING STATE
    // -------------------------------------------------------

    /** [DEEPAR] True while a video recording is in progress. */
    private boolean recording = false;

    /**
     * [GENERAL] Tracks which capture mode the UI is in:
     *   false = screenshot mode (default)
     *   true  = video recording mode
     * This controls what the center button does when tapped.
     */
    private boolean currentSwitchRecording = false;

    // -------------------------------------------------------
    // SCREEN DIMENSIONS
    // -------------------------------------------------------

    /**
     * [ANDROID] Screen pixel dimensions, populated in getScreenOrientation().
     * Used to allocate buffers and set video recording resolution.
     */
    private int width = 0;
    private int height = 0;

    /** [ANDROID] Output file for the video currently being recorded. */
    private File videoFileName;


    // ============================================================
    // ACTIVITY LIFECYCLE
    // ============================================================

    /**
     * [ANDROID] onCreate is called once when the Activity is first created.
     * We only inflate the layout here — DeepAR init happens in onStart
     * after permissions are confirmed.
     *
     * NEW UI: Replace R.layout.activity_main with your own layout here.
     * Keep the SurfaceView with id="@+id/surface" somewhere in your layout.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * [ANDROID] onStart is called every time the activity becomes visible
     * (including after returning from another activity or from background).
     *
     * We initialize DeepAR here (not onCreate) so it restarts properly
     * when the user returns to the app, since onStop releases DeepAR.
     *
     * [ANDROID] Runtime permission check: Camera permission must be granted
     * before we can access the camera hardware. On Android 6+, we request
     * it dynamically here with requestPermissions().
     */
    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // [ANDROID] Request CAMERA permission. Result delivered to onRequestPermissionsResult().
            // Request code 1 is an arbitrary integer we use to identify this specific request.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    1); // requestCode = 1
        } else {
            // Permission already granted — proceed with initialization
            initialize();
        }
    }

    /**
     * [ANDROID] Called when the user responds to the permission dialog.
     * requestCode matches what we passed to requestPermissions() above.
     * If all permissions granted, we call initialize().
     *
     * NEW UI: If you add more permissions (e.g. RECORD_AUDIO for video),
     * add them to the requestPermissions() call in onStart and check them here.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return; // At least one permission denied — abort
                }
            }
            initialize(); // All permissions granted
        }
    }

    /**
     * [GENERAL] Orchestrates the three initialization steps in order:
     *   1. DeepAR engine + camera
     *   2. Filter list
     *   3. UI views / button listeners
     *
     * NEW UI: You can replace initalizeViews() with your own setup,
     * but initializeDeepAR() and initializeFilters() must still run.
     */
    private void initialize() {
        initializeDeepAR();
        initializeFilters();
        initalizeViews();
    }

    /**
     * [DEEPAR] Builds the ordered list of AR effect filenames.
     * "none" is a sentinel — getFilterPath() returns null for it, which tells
     * DeepAR to clear any active effect.
     *
     * All other strings are filenames of .deepar bundles that must be placed
     * in app/src/main/assets/. They are loaded at runtime via:
     *   "file:///android_asset/<filename>"
     *
     * NEW UI: You can make this list dynamic — load from a server, a local DB,
     * or a folder scan. Just call deepAR.switchEffect() with the right path.
     */
    private void initializeFilters() {
        effects = new ArrayList<>();
        effects.add("none");                    // No filter (clear)
        effects.add("viking_helmet.deepar");    // [DEEPAR] 3D helmet face filter
        effects.add("MakeupLook.deepar");       // [DEEPAR] Beauty / makeup overlay
        effects.add("Split_View_Look.deepar");  // [DEEPAR] Split-screen effect
        effects.add("Emotions_Exaggerator.deepar"); // [DEEPAR] Face deformation based on emotion
        effects.add("Emotion_Meter.deepar");    // [DEEPAR] Emotion analysis HUD
        effects.add("Stallone.deepar");         // [DEEPAR] Celebrity face filter
        effects.add("flower_face.deepar");      // [DEEPAR] Floral decoration overlay
        effects.add("galaxy_background.deepar"); // [DEEPAR] Background replacement
        effects.add("Humanoid.deepar");         // [DEEPAR] Character transformation
        effects.add("Neon_Devil_Horns.deepar"); // [DEEPAR] 3D neon horns
        effects.add("Ping_Pong.deepar");        // [DEEPAR] Interactive game-style effect
        effects.add("Pixel_Hearts.deepar");     // [DEEPAR] Pixel heart decoration
        effects.add("Snail.deepar");            // [DEEPAR] Snail character
        effects.add("Hope.deepar");             // [DEEPAR] Text / graphics overlay
        effects.add("Vendetta_Mask.deepar");    // [DEEPAR] V for Vendetta mask
        effects.add("Fire_Effect.deepar");      // [DEEPAR] Particle fire effect
        effects.add("burning_effect.deepar");   // [DEEPAR] Body burn simulation
        effects.add("Elephant_Trunk.deepar");   // [DEEPAR] 3D elephant trunk face filter
    }

    /**
     * [ANDROID] Sets up all UI interactions: buttons, touch listener, mode switching.
     *
     * @SuppressLint("ClickableViewAccessibility") suppresses the accessibility warning
     * for custom touch handlers without performClick() — fine for a demo, but add
     * accessibility support in a production app.
     *
     * NEW UI: Replace or rewrite this entire method for your new UI.
     * The only things that MUST remain are:
     *   - arView.getHolder().addCallback(this) — registers SurfaceHolder.Callback
     *   - arView.setVisibility(GONE/VISIBLE) trick — forces onSurfaceChanged
     *   - deepAR.touchOccurred() calls if you want touch-reactive effects
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initalizeViews() {
        ImageButton previousMask = findViewById(R.id.previousMask);
        ImageButton nextMask = findViewById(R.id.nextMask);

        // [ANDROID] SurfaceView is the raw drawing canvas where DeepAR renders its output.
        // It is a hardware-accelerated surface that sits in its own window layer,
        // separate from the View hierarchy. This is why it must be used for GL rendering.
        SurfaceView arView = findViewById(R.id.surface);

        // [DEEPAR] Forward touch events to DeepAR so effects can react to user interaction.
        // ARTouchInfo wraps (x, y, phase). Phase = Start / Move / End.
        // Some effects (e.g. Ping_Pong) use touch to interact with virtual objects.
        //
        // NEW UI: Keep this listener if you want interactive effects.
        // Remove it if your UI absorbs the touches before they reach the SurfaceView.
        arView.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    deepAR.touchOccurred(new ARTouchInfo(motionEvent.getX(), motionEvent.getY(), ARTouchType.Start));
                    return true;
                case MotionEvent.ACTION_MOVE:
                    deepAR.touchOccurred(new ARTouchInfo(motionEvent.getX(), motionEvent.getY(), ARTouchType.Move));
                    return true;
                case MotionEvent.ACTION_UP:
                    deepAR.touchOccurred(new ARTouchInfo(motionEvent.getX(), motionEvent.getY(), ARTouchType.End));
                    return true;
            }
            return false;
        });

        // [ANDROID] Register this Activity as the SurfaceHolder.Callback so we receive
        // surfaceCreated / surfaceChanged / surfaceDestroyed events.
        arView.getHolder().addCallback(this);

        // [ANDROID] Force the surface to reinitialize by toggling visibility.
        // This ensures onSurfaceChanged() fires even if the surface already exists
        // from a previous session (e.g. screen rotation with hardware-accelerated surface).
        // NEW UI: Keep this trick if you use a SurfaceView for rendering.
        arView.setVisibility(View.GONE);
        arView.setVisibility(View.VISIBLE);

        // -------------------------------------------------------
        // SCREENSHOT BUTTON (center bottom)
        // -------------------------------------------------------
        // [DEEPAR] deepAR.takeScreenshot() asynchronously captures the current rendered frame.
        // The result is delivered to screenshotTaken(Bitmap) callback below.
        // Note: this button's click listener is REPLACED dynamically when switching to Record mode.
        final ImageButton screenshotBtn = findViewById(R.id.recordButton);
        screenshotBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deepAR.takeScreenshot(); // [DEEPAR] async — result in screenshotTaken()
            }
        });

        // -------------------------------------------------------
        // SWITCH CAMERA BUTTON (top left)
        // -------------------------------------------------------
        // [ANDROID] Toggle between front and back camera.
        // We must unbind the current camera before rebinding with a new selector,
        // otherwise CameraX throws an exception or shows a briefly mirrored frame.
        ImageButton switchCamera = findViewById(R.id.switchCamera);
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the lens direction
                lensFacing = (lensFacing == CameraSelector.LENS_FACING_FRONT)
                        ? CameraSelector.LENS_FACING_BACK
                        : CameraSelector.LENS_FACING_FRONT;

                // [ANDROID] Unbind immediately to stop old camera frames from leaking through
                ProcessCameraProvider cameraProvider = null;
                try {
                    cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // [ANDROID + DEEPAR] Restart camera with the new lens direction
                setupCamera();
            }
        });

        // -------------------------------------------------------
        // OPEN SECONDARY ACTIVITY BUTTON (top right)
        // -------------------------------------------------------
        // [ANDROID] Demonstrates launching a second Activity (BasicActivity).
        // When BasicActivity is open, DeepAR is NOT running (it's released in onStop).
        // This shows how to properly handle the DeepAR lifecycle across activities.
        ImageButton openActivity = findViewById(R.id.openActivity);
        openActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, BasicActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        // -------------------------------------------------------
        // SCREENSHOT / RECORD MODE TOGGLE BUTTONS
        // -------------------------------------------------------
        // [GENERAL] Two text buttons above the center button toggle the capture mode.
        // The center button's behavior changes dynamically depending on mode.
        // Active mode is shown with a semi-transparent background (alpha 0xA0 = ~63% opacity).

        final TextView screenShotModeButton = findViewById(R.id.screenshotModeButton);
        final TextView recordModeBtn = findViewById(R.id.recordModeButton);

        // [ANDROID] alpha is 0x00 = invisible, 0xA0 = semi-visible
        // Start in screenshot mode: screenshot button is highlighted, record is not
        recordModeBtn.getBackground().setAlpha(0x00);
        screenShotModeButton.getBackground().setAlpha(0xA0);

        // Screenshot mode button — switches center button back to taking screenshots
        screenShotModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSwitchRecording) { // Only switch if we're currently in record mode
                    if (recording) {
                        // [DEEPAR] Can't switch modes mid-recording — recording must stop first
                        Toast.makeText(getApplicationContext(),
                                "Cannot switch to screenshots while recording!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Update UI: highlight screenshot button, dim record button
                    recordModeBtn.getBackground().setAlpha(0x00);
                    screenShotModeButton.getBackground().setAlpha(0xA0);

                    // Reassign center button to take screenshots
                    screenshotBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deepAR.takeScreenshot(); // [DEEPAR] async screenshot capture
                        }
                    });
                    currentSwitchRecording = !currentSwitchRecording;
                }
            }
        });

        // Record mode button — switches center button to start/stop video recording
        recordModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentSwitchRecording) { // Only switch if we're currently in screenshot mode
                    // Update UI: highlight record button, dim screenshot button
                    recordModeBtn.getBackground().setAlpha(0xA0);
                    screenShotModeButton.getBackground().setAlpha(0x00);

                    // Reassign center button to toggle video recording
                    screenshotBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (recording) {
                                // ---- STOP RECORDING ----
                                // [DEEPAR] Stop the video recording session
                                deepAR.stopVideoRecording();

                                // [ANDROID] Notify the media scanner so the video shows up
                                // immediately in the device's Gallery app without a reboot
                                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                Uri contentUri = Uri.fromFile(videoFileName);
                                mediaScanIntent.setData(contentUri);
                                sendBroadcast(mediaScanIntent);

                                Toast.makeText(getApplicationContext(),
                                        "Recording " + videoFileName.getName() + " saved.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                // ---- START RECORDING ----
                                // [ANDROID] Save to Movies directory in external storage.
                                // Filename includes timestamp to avoid collisions.
                                // WRITE_EXTERNAL_STORAGE permission required on Android ≤ 32.
                                videoFileName = new File(
                                        getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                                        "video_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".mp4"
                                );

                                // [DEEPAR] Start recording to the given file path.
                                // width/2 and height/2 = record at half resolution to save space/CPU.
                                // You can record at full resolution: pass (width, height) instead.
                                deepAR.startVideoRecording(videoFileName.toString(), width / 2, height / 2);

                                Toast.makeText(getApplicationContext(),
                                        "Recording started.", Toast.LENGTH_SHORT).show();
                            }
                            recording = !recording; // Toggle recording state flag
                        }
                    });
                    currentSwitchRecording = !currentSwitchRecording;
                }
            }
        });

        // -------------------------------------------------------
        // FILTER NAVIGATION BUTTONS (bottom left / bottom right arrows)
        // -------------------------------------------------------
        // [DEEPAR] Navigate backwards/forwards through the effects list.
        // Wraps around at both ends (circular navigation).
        previousMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoPrevious();
            }
        });

        nextMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoNext();
            }
        });
    }

    // ============================================================
    // SCREEN ORIENTATION HELPER
    // ============================================================

    /**
     * [ANDROID] Determines the logical screen orientation from the physical rotation.
     * Source: https://stackoverflow.com/questions/10380989
     *
     * This is needed because DeepAR needs to know the orientation to correctly
     * rotate the camera frame before processing it.
     *
     * Also captures screen pixel dimensions (width, height) as a side effect —
     * used for buffer allocation and recording resolution.
     *
     * Returns one of: SCREEN_ORIENTATION_PORTRAIT, LANDSCAPE, REVERSE_PORTRAIT, REVERSE_LANDSCAPE
     */
    private int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        int orientation;

        // Portrait-native device (most phones): natural orientation is portrait (ROTATION_0)
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width
                || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        } else {
            // Landscape-native device (some tablets): natural orientation is landscape (ROTATION_0)
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    // ============================================================
    // DEEPAR INITIALIZATION
    // ============================================================

    /**
     * [DEEPAR] Creates and configures the DeepAR engine.
     *
     * Steps:
     *   1. new DeepAR(context) — allocate the engine
     *   2. setLicenseKey()    — must match the applicationId in build.gradle
     *   3. initialize()       — starts internal DeepAR threads; fires initialized() callback when ready
     *   4. setupCamera()      — start the camera pipeline (runs concurrently with DeepAR init)
     *
     * IMPORTANT: The license key is bound to applicationId "tech.virtuglow.demoandroid".
     * If you change the package name, you need a new key from https://developer.deepar.ai
     *
     * NEW UI NOTE: You do NOT need to change this method for UI redesigns.
     * Just make sure it's called before any other DeepAR operations.
     */
    private void initializeDeepAR() {
        deepAR = new DeepAR(this);
        // [DEEPAR] License key tied to applicationId. Replace if you change the app package.
        deepAR.setLicenseKey("f0c0e56f177f5cdf7e9963eddcf5ee5a14e1109aa8ab37160811b886fbbe71e730e0adc2786305c2");
        // [DEEPAR] 'this' is both the Context and the AREventListener (implemented below)
        deepAR.initialize(this, this);
        setupCamera();
    }

    // ============================================================
    // CAMERAX SETUP
    // ============================================================

    /**
     * [ANDROID] CameraX entry point. Requests a ProcessCameraProvider asynchronously.
     * ProcessCameraProvider manages the camera lifecycle and binds use cases (Preview, Analysis).
     *
     * addListener() runs our callback on the main executor when the provider is ready.
     *
     * NEW UI: This method is internal camera plumbing — no changes needed for UI redesign.
     */
    private void setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * [ANDROID + DEEPAR] Configures and binds the camera to either:
     *   A) ImageAnalysis (default, useExternalCameraTexture=false): frames delivered as ByteBuffers
     *   B) Preview via ARSurfaceProvider (useExternalCameraTexture=true): frames via GL texture
     *
     * Resolution handling:
     *   [DEEPAR] CameraResolutionPreset.P1920x1080 is a DeepAR constant (1920x1080).
     *   Width and height are swapped for portrait mode because the camera sensor is landscape.
     *
     * @param cameraProvider The bound CameraX provider
     */
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        // [DEEPAR] CameraResolutionPreset is a DeepAR enum defining standard resolutions.
        // P1920x1080 = 1080p. You can also use P1280x720, P640x480, etc.
        CameraResolutionPreset cameraResolutionPreset = CameraResolutionPreset.P1920x1080;
        int width;
        int height;

        // [ANDROID + DEEPAR] Camera sensor outputs landscape frames (width > height).
        // For portrait mode we swap width and height to match the display orientation.
        int orientation = getScreenOrientation();
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                || orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            width = cameraResolutionPreset.getWidth();
            height = cameraResolutionPreset.getHeight();
        } else {
            // Portrait: swap dimensions
            width = cameraResolutionPreset.getHeight();
            height = cameraResolutionPreset.getWidth();
        }

        Size cameraResolution = new Size(width, height);

        // [ANDROID] CameraSelector picks front or back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        if (useExternalCameraTexture) {
            // ---- PATH A: GL Texture (not currently active) ----
            // [DEEPAR] Camera preview feeds directly into DeepAR's GL texture.
            // More GPU-efficient but requires proper EGL setup.
            Preview preview = new Preview.Builder()
                    .setTargetResolution(cameraResolution)
                    .build();

            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview);

            if (surfaceProvider == null) {
                surfaceProvider = new ARSurfaceProvider(this, deepAR);
            }
            preview.setSurfaceProvider(surfaceProvider);
            // [DEEPAR] Mirror front camera so it looks like a mirror (not flipped)
            surfaceProvider.setMirror(lensFacing == CameraSelector.LENS_FACING_FRONT);

        } else {
            // ---- PATH B: ByteBuffer / ImageAnalysis (default) ----
            // [ANDROID] Allocate two ByteBuffers for double-buffering.
            // Each buffer stores one full RGBA_8888 frame (4 bytes per pixel).
            buffers = new ByteBuffer[NUMBER_OF_BUFFERS];
            for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
                buffers[i] = ByteBuffer.allocateDirect(width * height * 4);
                buffers[i].order(ByteOrder.nativeOrder()); // [ANDROID] Match CPU byte order
                buffers[i].position(0);
            }

            // [ANDROID] ImageAnalysis use-case: receives every camera frame as an ImageProxy.
            // STRATEGY_KEEP_ONLY_LATEST = drop old frames if analyzer is too slow (no queue buildup).
            // OUTPUT_IMAGE_FORMAT_RGBA_8888 = request pre-converted RGBA format (no YUV conversion needed).
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetResolution(cameraResolution)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            // [ANDROID] setAnalyzer runs on the main executor (UI thread here).
            // Consider a background executor for production if analysis is CPU-heavy.
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer);

            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis);
        }
    }

    /**
     * [ANDROID + DEEPAR] Called for every camera frame (as fast as the camera produces them).
     *
     * Frame flow:
     *   Camera -> ImageProxy (RGBA_8888) -> ByteBuffer -> deepAR.receiveFrame()
     *
     * Double-buffer pattern: we alternate between buffers[0] and buffers[1].
     * While DeepAR renders one buffer, we fill the other.
     *
     * IMPORTANT: image.close() MUST be called at the end — CameraX blocks further
     * frames until the current ImageProxy is closed. Forgetting this freezes the camera.
     */
    private ImageAnalysis.Analyzer imageAnalyzer = new ImageAnalysis.Analyzer() {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            // [ANDROID] image.getPlanes()[0] = the single RGBA plane (no separate UV planes in RGBA)
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            buffer.rewind(); // [JAVA] Reset position to 0 before reading

            // Copy camera frame bytes into the current double-buffer slot
            buffers[currentBuffer].put(buffer);
            buffers[currentBuffer].position(0); // [JAVA] Reset for DeepAR to read from start

            if (deepAR != null) {
                // [DEEPAR] Send the frame to DeepAR for processing:
                //   - buffers[currentBuffer]: pixel data
                //   - image.getWidth() / getHeight(): frame dimensions
                //   - getRotationDegrees(): how much to rotate the frame (0, 90, 180, 270)
                //   - lensFacing == FRONT: tells DeepAR to horizontally mirror the output
                //   - DeepARImageFormat.RGBA_8888: the pixel format we requested from CameraX
                //   - getPixelStride(): bytes between adjacent pixels (usually 4 for RGBA)
                deepAR.receiveFrame(
                        buffers[currentBuffer],
                        image.getWidth(), image.getHeight(),
                        image.getImageInfo().getRotationDegrees(),
                        lensFacing == CameraSelector.LENS_FACING_FRONT,
                        DeepARImageFormat.RGBA_8888,
                        image.getPlanes()[0].getPixelStride()
                );
            }

            // [ANDROID] Advance to the next buffer slot (wraps 0 -> 1 -> 0 -> ...)
            currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS;

            // [ANDROID] CRITICAL: Release the ImageProxy so CameraX can deliver the next frame
            image.close();
        }
    };

    // ============================================================
    // EFFECT NAVIGATION
    // ============================================================

    /**
     * [DEEPAR] Converts an effect filename to the android_asset URI format that DeepAR expects.
     *
     * "none" → null (tells DeepAR to remove any active effect)
     * "viking_helmet.deepar" → "file:///android_asset/MakeupLook.deeparviking_helmet.deepar"
     *
     * BUG NOTE: There is a bug here — the path concatenates a hardcoded "MakeupLook.deepar"
     * prefix before the filename, making all paths except "none" incorrect:
     *   "file:///android_asset/MakeupLook.deepar" + filterName
     * The correct pattern should be:
     *   "file:///android_asset/" + filterName
     *
     * Fix this before using with real effect files.
     */
    private String getFilterPath(String filterName) {
        if (filterName.equals("none")) {
            return null; // [DEEPAR] Passing null to switchEffect() clears the current effect
        }
        return "file:///android_asset/" + filterName;
    }

    /**
     * [DEEPAR] Advance to the next effect in the circular list.
     * deepAR.switchEffect() loads the .deepar file from assets and applies it immediately.
     * "effect" is the slot name — DeepAR supports multiple simultaneous effect slots.
     */
    private void gotoNext() {
        currentEffect = (currentEffect + 1) % effects.size();
        deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
    }

    /**
     * [DEEPAR] Go back to the previous effect in the circular list.
     * (currentEffect - 1 + size) % size safely handles wrap-around past index 0.
     */
    private void gotoPrevious() {
        currentEffect = (currentEffect - 1 + effects.size()) % effects.size();
        deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
    }

    // ============================================================
    // ACTIVITY LIFECYCLE — CLEANUP
    // ============================================================

    /**
     * [ANDROID] onStop is called when the activity is no longer visible
     * (user pressed Home, switched app, or opened BasicActivity).
     *
     * [DEEPAR] We release DeepAR here to free GPU/CPU resources while off-screen.
     * It will be re-initialized in onStart() when the user returns.
     *
     * [ANDROID] We also unbind CameraX and stop the surface provider
     * to release the camera hardware (other apps may need it).
     */
    @Override
    protected void onStop() {
        // Reset recording state — can't continue recording after stop
        recording = false;
        currentSwitchRecording = false;

        // [ANDROID] Unbind and release the camera
        ProcessCameraProvider cameraProvider = null;
        try {
            cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // [DEEPAR] Stop the GL texture surface provider if it was used
        if (surfaceProvider != null) {
            surfaceProvider.stop();
            surfaceProvider = null;
        }

        // [DEEPAR] Release all DeepAR resources (GPU textures, threads, ML models)
        deepAR.release();
        deepAR = null;

        super.onStop();
    }

    /**
     * [ANDROID] onDestroy is called when the Activity is being fully destroyed
     * (back pressed, or system kills the process).
     *
     * [DEEPAR] Belt-and-suspenders cleanup — release DeepAR if it wasn't already
     * released in onStop (defensive coding for edge cases).
     *
     * Note: deepAR.setAREventListener(null) prevents callbacks from firing
     * into a destroyed activity (avoids crashes/memory leaks).
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (surfaceProvider != null) {
            surfaceProvider.stop();
        }
        if (deepAR == null) {
            return; // Already released in onStop
        }
        deepAR.setAREventListener(null); // [DEEPAR] Detach listener before release
        deepAR.release();
        deepAR = null;
    }

    // ============================================================
    // SURFACEHOLDER.CALLBACK — where DeepAR renders its output
    // ============================================================

    /**
     * [ANDROID] Called when the SurfaceView's drawing surface is first created.
     * We don't need to do anything here — we wait for surfaceChanged() which
     * gives us the actual dimensions.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Nothing to do here — dimensions not known yet
    }

    /**
     * [ANDROID + DEEPAR] Called when the surface is created or its size changes.
     * This is where we hand the Surface to DeepAR as its render target.
     *
     * [DEEPAR] deepAR.setRenderSurface() tells DeepAR:
     *   - WHERE to draw (the Surface from the SurfaceView)
     *   - HOW BIG the output should be (width x height in pixels)
     *
     * NEW UI: If you replace the SurfaceView with a TextureView or other surface,
     * you must call setRenderSurface() with the new Surface here.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        deepAR.setRenderSurface(holder.getSurface(), width, height);
    }

    /**
     * [ANDROID + DEEPAR] Called when the SurfaceView is being destroyed
     * (e.g. activity pauses, screen off). Clear DeepAR's render target.
     *
     * [DEEPAR] Passing null surface with 0x0 dimensions detaches DeepAR from the surface.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (deepAR != null) {
            deepAR.setRenderSurface(null, 0, 0);
        }
    }

    // ============================================================
    // AREventListener CALLBACKS
    // All methods below are [DEEPAR] callbacks from the AREventListener interface.
    // DeepAR calls these on the main thread.
    // ============================================================

    /**
     * [DEEPAR] Called by DeepAR after takeScreenshot() completes.
     * The Bitmap contains the full rendered AR frame at screen resolution.
     *
     * [ANDROID] We save it as a JPEG to the public Pictures directory,
     * then broadcast ACTION_MEDIA_SCANNER_SCAN_FILE so it appears in the Gallery.
     *
     * NEW UI: Replace the Toast with an in-app notification, preview thumbnail, or share sheet.
     * You can also display the Bitmap directly in an ImageView before saving.
     */
    @Override
    public void screenshotTaken(Bitmap bitmap) {
        CharSequence now = DateFormat.format("yyyy_MM_dd_hh_mm_ss", new Date());
        try {
            // [ANDROID] Save to external Pictures directory (publicly visible)
            File imageFile = new File(
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "image_" + now + ".jpg"
            );
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100; // JPEG quality 0-100
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            // [ANDROID] Tell the media scanner to index this file so it appears in Gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(imageFile);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);

            Toast.makeText(MainActivity.this,
                    "Screenshot " + imageFile.getName() + " saved.", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * [DEEPAR] Fired when video recording has actually started (after startVideoRecording() call).
     * Use this to update UI state (e.g. show a red "recording" indicator).
     * NEW UI: Update a recording indicator dot/timer here.
     */
    @Override
    public void videoRecordingStarted() {
        // TODO NEW UI: show a "REC" indicator
    }

    /**
     * [DEEPAR] Fired when video recording has finished successfully (after stopVideoRecording()).
     * The file is fully written and ready to use at this point.
     * NEW UI: Show a "Saved!" banner or open a preview of the recorded video.
     */
    @Override
    public void videoRecordingFinished() {
        // TODO NEW UI: show save confirmation or thumbnail preview
    }

    /**
     * [DEEPAR] Fired if video recording fails (e.g. codec error, no space left).
     * NEW UI: Show an error message to the user.
     */
    @Override
    public void videoRecordingFailed() {
        // TODO NEW UI: show error toast/dialog
    }

    /**
     * [DEEPAR] Fired when the video encoder is ready but recording hasn't started yet.
     * Called between startVideoRecording() and videoRecordingStarted().
     * Rarely needed for basic usage.
     */
    @Override
    public void videoRecordingPrepared() {
        // Usually not needed for basic usage
    }

    /**
     * [DEEPAR] Fired when deepAR.release() has fully completed its shutdown.
     * All GPU resources are freed at this point.
     */
    @Override
    public void shutdownFinished() {
        // Can be used to confirm cleanup is complete
    }

    /**
     * [DEEPAR] Called when DeepAR has finished initialization (after deepAR.initialize()).
     * This is the correct place to apply an initial effect, because DeepAR is
     * ready to process effects only AFTER this callback fires.
     *
     * We restore whichever filter was selected before (handles app resume).
     */
    @Override
    public void initialized() {
        // [DEEPAR] Apply the current effect after re-initialization (e.g. returning from background)
        deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
    }

    /**
     * [DEEPAR] Called whenever a face enters or leaves the camera view.
     *   b = true: at least one face is visible
     *   b = false: no faces detected
     * NEW UI: Show a "point camera at your face" hint when b=false.
     */
    @Override
    public void faceVisibilityChanged(boolean b) {
        // TODO NEW UI: show "no face detected" hint
    }

    /**
     * [DEEPAR] Called when a tracked image (e.g. a target poster) appears/disappears.
     *   s = name of the image target
     *   b = true if visible, false if lost
     * Used for marker-based AR (not active in this demo).
     */
    @Override
    public void imageVisibilityChanged(String s, boolean b) {
        // Not used in this demo
    }

    /**
     * [DEEPAR] Called when DeepAR has processed a frame and it's available as an Image.
     * Only relevant when using offscreen rendering mode (not this demo's default mode).
     */
    @Override
    public void frameAvailable(Image image) {
        // Not used in this demo (we render directly to SurfaceView)
    }

    /**
     * [DEEPAR] Called when a DeepAR error occurs.
     *   arErrorType: category of the error (e.g. LICENSE_AUTHENTICATION_FAILED)
     *   s: human-readable error message
     *
     * NEW UI: Log errors and show a user-facing message for license failures.
     * LICENSE_AUTHENTICATION_FAILED usually means wrong key or wrong applicationId.
     */
    @Override
    public void error(ARErrorType arErrorType, String s) {
        // TODO: log and display error — especially important for license validation failures
    }

    /**
     * [DEEPAR] Called after deepAR.switchEffect() completes loading a new effect.
     *   s = the slot name ("effect" in this app)
     * Useful for showing a loading spinner while a heavy effect loads.
     * NEW UI: Hide a loading indicator here after the effect is applied.
     */
    @Override
    public void effectSwitched(String s) {
        // TODO NEW UI: hide loading indicator, update effect name label
    }
}
