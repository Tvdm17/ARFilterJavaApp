package ai.deepar.deepar_example;

// ============================================================
// DeepARManager — all DeepAR SDK logic in one place
// ============================================================
// This class owns and wraps the DeepAR engine completely.
// MainActivity never imports or talks to DeepAR directly —
// it only calls methods on this class.
//
// Responsibilities:
//   - Initialize / release the DeepAR engine
//   - Manage the effect list and switching
//   - Receive camera frames and forward to DeepAR
//   - Handle screenshot and video recording
//   - Bind DeepAR to the SurfaceView for rendering
//   - Fire UI callbacks via the Listener interface
//
// To use from a new UI:
//   1. Create a DeepARManager(activity, listener)
//   2. Call initialize() after camera permission is granted
//   3. Register the SurfaceView: call getSurfaceCallback() and add it to the SurfaceHolder
//   4. Call release() in onStop
// ============================================================

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import ai.deepar.ar.ARErrorType;
import ai.deepar.ar.AREventListener;
import ai.deepar.ar.ARTouchInfo;
import ai.deepar.ar.DeepAR;
import ai.deepar.ar.DeepARImageFormat;

public class DeepARManager implements AREventListener, SurfaceHolder.Callback {

    // -------------------------------------------------------
    // CONSTANTS
    // -------------------------------------------------------

    /**
     * [DEEPAR] License key — bound to applicationId "tech.virtuglow.demoandroid".
     * If you change the applicationId in build.gradle, get a new key at developer.deepar.ai
     */
    private static final String LICENSE_KEY =
            "f0c0e56f177f5cdf7e9963eddcf5ee5a14e1109aa8ab37160811b886fbbe71e730e0adc2786305c2";

    // -------------------------------------------------------
    // FIELDS
    // -------------------------------------------------------

    /** [DEEPAR] The core AR engine. Null when released (between onStop and onStart). */
    private DeepAR deepAR;

    /** [ANDROID] App context — passed to DeepAR for resource access. */
    private final Context context;

    /**
     * [GENERAL] Listener interface — lets MainActivity (or any UI) react to
     * DeepAR events without importing any DeepAR classes.
     */
    private final Listener listener;

    /** [DEEPAR] Index of the currently active effect in the effects list. */
    private int currentEffect = 0;

    /** [DEEPAR] Ordered list of effect filenames. "none" = no filter. */
    private ArrayList<String> effects;

    // -------------------------------------------------------
    // LISTENER INTERFACE
    // -------------------------------------------------------

    /**
     * [GENERAL] Callback interface for the UI layer.
     * Implement this in any activity to react to DeepAR events without importing DeepAR SDK classes.
     */
    public interface Listener {
        /** Called when a screenshot bitmap is ready to be saved or displayed. */
        void onScreenshotTaken(Bitmap bitmap);

        /** Called when video recording has actually started. Show a "REC" indicator here. */
        void onVideoRecordingStarted();

        /** Called when video recording has finished and the file is fully written. */
        void onVideoRecordingFinished();

        /** Called if video recording fails (e.g. codec error, out of space). */
        void onVideoRecordingFailed();

        /** Called when DeepAR finishes initializing and is ready to render. */
        void onInitialized();

        /** Called on any DeepAR error. LICENSE_AUTHENTICATION_FAILED = wrong key or package. */
        void onError(ARErrorType type, String message);
    }

    // -------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------

    public DeepARManager(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    // -------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------

    /**
     * [DEEPAR] Initialize the DeepAR engine.
     * Call this after camera permission is granted, before setupCamera().
     * DeepAR fires initialized() when ready — that is the earliest safe point
     * to call switchEffect().
     */
    public void initialize() {
        buildEffectList();
        deepAR = new DeepAR(context);
        deepAR.setLicenseKey(LICENSE_KEY);
        // [DEEPAR] 'this' satisfies AREventListener — DeepAR will call our callbacks below
        deepAR.initialize(context, this);
    }

    /**
     * [DEEPAR] Release all DeepAR resources (GPU textures, threads, ML models).
     * Call this in onStop() so the GPU is freed while the activity is off-screen.
     * DeepAR is re-initialized on the next initialize() call.
     */
    public void release() {
        if (deepAR != null) {
            deepAR.setAREventListener(null); // Detach listener before release to prevent callbacks into dead state
            deepAR.release();
            deepAR = null;
        }
    }

    // -------------------------------------------------------
    // FRAME DELIVERY (called by CameraManager every frame)
    // -------------------------------------------------------

    /**
     * [DEEPAR] Receives a camera frame as a ByteBuffer and forwards it to DeepAR.
     * Called by CameraManager's ImageAnalysis analyzer on every camera frame.
     *
     * Parameters mirror deepAR.receiveFrame():
     *   buffer      — RGBA_8888 pixel data
     *   width/height — frame dimensions in pixels
     *   rotation    — degrees to rotate frame to match display (0/90/180/270)
     *   mirror      — true for front camera (horizontal flip)
     *   format      — pixel format (always RGBA_8888 in this project)
     *   pixelStride — bytes between adjacent pixels (usually 4 for RGBA)
     */
    public void receiveFrame(ByteBuffer buffer, int width, int height,
                             int rotation, boolean mirror,
                             DeepARImageFormat format, int pixelStride) {
        if (deepAR != null) {
            deepAR.receiveFrame(buffer, width, height, rotation, mirror, format, pixelStride);
        }
    }

    // -------------------------------------------------------
    // TOUCH (forward touch events from UI to DeepAR)
    // -------------------------------------------------------

    /**
     * [DEEPAR] Forward a touch event to DeepAR so interactive effects (e.g. Ping_Pong) can react.
     * Called from MainActivity's SurfaceView touch listener.
     */
    public void touchOccurred(ARTouchInfo info) {
        if (deepAR != null) {
            deepAR.touchOccurred(info);
        }
    }

    // -------------------------------------------------------
    // EFFECTS
    // -------------------------------------------------------

    /**
     * [DEEPAR] Builds the ordered list of effect filenames bundled in app/src/main/assets/.
     * "none" = remove all effects (getFilterPath returns null for it).
     * All .deepar files must exist in assets/ or DeepAR will silently fail to load them.
     */
    private void buildEffectList() {
        effects = new ArrayList<>();
        effects.add("none");
        effects.add("viking_helmet.deepar");
        effects.add("MakeupLook.deepar");
        effects.add("Split_View_Look.deepar");
        effects.add("Emotions_Exaggerator.deepar");
        effects.add("Emotion_Meter.deepar");
        effects.add("Stallone.deepar");
        effects.add("flower_face.deepar");
        effects.add("galaxy_background.deepar");
        effects.add("Humanoid.deepar");
        effects.add("Neon_Devil_Horns.deepar");
        effects.add("Ping_Pong.deepar");
        effects.add("Pixel_Hearts.deepar");
        effects.add("Snail.deepar");
        effects.add("Hope.deepar");
        effects.add("Vendetta_Mask.deepar");
        effects.add("Fire_Effect.deepar");
        effects.add("burning_effect.deepar");
        effects.add("Elephant_Trunk.deepar");
    }

    /**
     * [DEEPAR] Converts an effect filename to the Android asset URI format.
     * "none" returns null — passing null to switchEffect() clears the active effect.
     */
    private String getFilterPath(String filterName) {
        if (filterName.equals("none")) return null;
        return "file:///android_asset/" + filterName;
    }

    /**
     * [DEEPAR] Advance to the next effect (wraps around to start after the last one).
     * "effect" is the slot name — DeepAR supports multiple simultaneous effect slots.
     */
    public void gotoNext() {
        currentEffect = (currentEffect + 1) % effects.size();
        if (deepAR != null) {
            deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
        }
    }

    /**
     * [DEEPAR] Go back to the previous effect (wraps around to end before the first one).
     */
    public void gotoPrevious() {
        currentEffect = (currentEffect - 1 + effects.size()) % effects.size();
        if (deepAR != null) {
            deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
        }
    }

    // -------------------------------------------------------
    // SCREENSHOT & RECORDING
    // -------------------------------------------------------

    /**
     * [DEEPAR] Trigger an async screenshot. Result delivered to screenshotTaken() → listener.onScreenshotTaken().
     */
    public void takeScreenshot() {
        if (deepAR != null) deepAR.takeScreenshot();
    }

    /**
     * [DEEPAR] Start recording video to the given file path at the given resolution.
     * @param filePath  absolute path to the .mp4 output file
     * @param width     recording width in pixels
     * @param height    recording height in pixels
     */
    public void startVideoRecording(String filePath, int width, int height) {
        if (deepAR != null) deepAR.startVideoRecording(filePath, width, height);
    }

    /**
     * [DEEPAR] Stop an in-progress recording.
     * The file is fully written only after videoRecordingFinished() fires.
     */
    public void stopVideoRecording() {
        if (deepAR != null) deepAR.stopVideoRecording();
    }

    // -------------------------------------------------------
    // SURFACEHOLDER.CALLBACK — bind DeepAR to the SurfaceView
    // -------------------------------------------------------

    /**
     * [ANDROID + DEEPAR] Returns this object as a SurfaceHolder.Callback so an activity
     * can register it with the SurfaceView's holder:
     *   arView.getHolder().addCallback(deepARManager.getSurfaceCallback())
     *
     * Keeping SurfaceHolder.Callback here means activities never need to
     * call deepAR.setRenderSurface() directly.
     */
    public SurfaceHolder.Callback getSurfaceCallback() {
        return this;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Dimensions not known yet — wait for surfaceChanged
    }

    /**
     * [DEEPAR] Tell DeepAR which Surface to render into and how big it is.
     * Called when the SurfaceView is first created or resized.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (deepAR != null) {
            deepAR.setRenderSurface(holder.getSurface(), width, height);
        }
    }

    /**
     * [DEEPAR] Detach DeepAR from the surface when the SurfaceView is destroyed.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (deepAR != null) {
            deepAR.setRenderSurface(null, 0, 0);
        }
    }

    // -------------------------------------------------------
    // AREventListener CALLBACKS
    // -------------------------------------------------------

    /**
     * [DEEPAR] DeepAR is fully initialized and ready. Apply the current effect here —
     * this is the earliest safe point to call switchEffect().
     * Also notifies the UI via listener.onInitialized().
     */
    @Override
    public void initialized() {
        if (deepAR != null) {
            deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
        }
        if (listener != null) listener.onInitialized();
    }

    /** [DEEPAR] Screenshot bitmap is ready — forward to UI to save/display. */
    @Override
    public void screenshotTaken(Bitmap bitmap) {
        if (listener != null) listener.onScreenshotTaken(bitmap);
    }

    /** [DEEPAR] Video recording has started. UI can show a "REC" indicator. */
    @Override
    public void videoRecordingStarted() {
        if (listener != null) listener.onVideoRecordingStarted();
    }

    /** [DEEPAR] Video file is fully written and ready to use. */
    @Override
    public void videoRecordingFinished() {
        if (listener != null) listener.onVideoRecordingFinished();
    }

    /** [DEEPAR] Recording failed. UI should show an error. */
    @Override
    public void videoRecordingFailed() {
        if (listener != null) listener.onVideoRecordingFailed();
    }

    @Override
    public void videoRecordingPrepared() {
        // Encoder is ready but recording has not started yet.
        // Could be used to enable the record button only once the encoder is ready.
    }

    @Override
    public void shutdownFinished() {
        // deepAR.release() has fully completed — GPU resources are freed.
        // Useful if you need to do work (e.g. re-init) guaranteed after teardown.
    }

    @Override
    public void faceVisibilityChanged(boolean visible) {
        // true = face detected, false = no face in frame.
        // Could show a "point camera at your face" hint when false.
    }

    @Override
    public void imageVisibilityChanged(String targetName, boolean visible) {
        // Marker-based (image-tracking) AR — not used in this project.
    }

    @Override
    public void frameAvailable(Image image) {
        // Fired in offscreen rendering mode — not used here because we render to a SurfaceView.
    }

    /**
     * [DEEPAR] A DeepAR error occurred. Most common: LICENSE_AUTHENTICATION_FAILED
     * which means the license key doesn't match the applicationId.
     */
    @Override
    public void error(ARErrorType type, String message) {
        if (listener != null) listener.onError(type, message);
    }

    /** [DEEPAR] Apply a specific effect by filename (e.g. "MakeupLook.deepar"). */
    public void switchEffect(String effectFileName) {
        if (deepAR != null) {
            deepAR.switchEffect("effect", getFilterPath(effectFileName));
        }
    }

    @Override
    public void effectSwitched(String slot) {
        // Called after switchEffect() completes.
        // Could be used to hide a loading spinner shown while the effect was loading.
    }
}
