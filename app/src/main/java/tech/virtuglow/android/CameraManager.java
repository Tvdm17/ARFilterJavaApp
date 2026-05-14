package tech.virtuglow.android;

// ============================================================
// CameraManager — all CameraX logic in one place
// ============================================================
// This class owns the CameraX pipeline and is the only class
// that imports CameraX. It delivers frames to DeepARManager.
//
// Responsibilities:
//   - Set up and tear down CameraX
//   - Switch between front and back camera
//   - Determine screen orientation for correct frame rotation
//   - Allocate frame buffers and feed frames to DeepARManager
//
// To use from a new UI:
//   1. Create a CameraManager(activity, deepARManager)
//   2. Call setupCamera() to start the pipeline
//   3. Call switchCamera() when the user taps the flip button
//   4. Call release() in onStop
// ============================================================

import android.content.pm.ActivityInfo;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import ai.deepar.ar.CameraResolutionPreset;
import ai.deepar.ar.DeepARImageFormat;

public class CameraManager {

    // -------------------------------------------------------
    // CONSTANTS
    // -------------------------------------------------------

    /**
     * [ANDROID + DEEPAR] Number of ByteBuffers to alternate between.
     * While DeepAR processes buffer[0], CameraX writes into buffer[1] and vice versa.
     * Prevents frame drops caused by DeepAR holding a buffer while the next frame arrives.
     */
    private static final int NUMBER_OF_BUFFERS = 2;


    // -------------------------------------------------------
    // FIELDS
    // -------------------------------------------------------

    /**
     * [ANDROID] AppCompatActivity is used as both:
     *   - Context: for ProcessCameraProvider.getInstance() and ContextCompat.getMainExecutor()
     *   - LifecycleOwner: for cameraProvider.bindToLifecycle()
     *   - WindowManager source: for getScreenOrientation()
     */
    private final AppCompatActivity activity;

    /** [DEEPAR] Receives frames and forwards them to the DeepAR engine. */
    private final DeepARManager deepARManager;

    /** [ANDROID] Which camera is currently active. Default: front (selfie) camera. */
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;

    /**
     * [ANDROID] CameraX provides the ProcessCameraProvider asynchronously via this future.
     * Stored as a field so we can call unbindAll() when switching cameras or stopping.
     */
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    /** [ANDROID + DEEPAR] Two ByteBuffers alternated each frame to avoid blocking. */
    private ByteBuffer[] buffers;
    private int currentBuffer = 0;

    /**
     * [DEEPAR] DeepAR requires every call (receiveFrame, switchEffect, etc.) to happen
     * on the same thread where it was initialized and where setRenderSurface() was called
     * (the main thread). CameraX's ImageAnalysis runs on a background executor, so we
     * post receiveFrame() back to the main thread via this handler.
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * [ANDROID] Screen dimensions in pixels — set once by getScreenOrientation() during bindImageAnalysis().
     * Exposed via getWidth()/getHeight() so PreviewActivity can halve them for the video recording resolution.
     */
    private int width = 0;
    private int height = 0;

    // -------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------

    public CameraManager(AppCompatActivity activity, DeepARManager deepARManager) {
        this.activity = activity;
        this.deepARManager = deepARManager;
    }

    // -------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------

    /**
     * [ANDROID] Start the CameraX pipeline.
     * Asynchronously gets the ProcessCameraProvider then calls bindImageAnalysis().
     * Call this after DeepARManager.initialize().
     */
    public void setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(activity);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    /**
     * [ANDROID] Toggle between front and back camera.
     * Unbinds immediately to avoid a brief flash of the wrong (mirrored) frame,
     * then calls setupCamera() to rebind with the new lens direction.
     */
    public void switchCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_FRONT)
                ? CameraSelector.LENS_FACING_BACK
                : CameraSelector.LENS_FACING_FRONT;
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll(); // Stop old camera immediately
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        setupCamera(); // Restart with new lens direction
    }

    /**
     * [ANDROID] Unbind all CameraX use-cases and free the camera hardware.
     * Call this in onStop() before DeepARManager.release() so the GPU is freed while off-screen.
     */
    public void release() {
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** [ANDROID] Screen width in pixels — used by PreviewActivity to set video recording resolution. */
    public int getWidth() { return width; }

    /** [ANDROID] Screen height in pixels — used by PreviewActivity to set video recording resolution. */
    public int getHeight() { return height; }


    // -------------------------------------------------------
    // PRIVATE — CAMERA BINDING
    // -------------------------------------------------------

    /**
     * [ANDROID + DEEPAR] Configure and bind the camera use-case.
     *
     * [DEEPAR] CameraResolutionPreset.P1920x1080 = 1080p (DeepAR constant).
     * Camera sensor is physically landscape (width > height), so in portrait
     * mode we swap width and height to match what the display expects.
     */
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        CameraResolutionPreset preset = CameraResolutionPreset.P1920x1080;
        int orientation = getScreenOrientation(); // Also sets this.width and this.height

        int frameWidth, frameHeight;
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            frameWidth = preset.getWidth();
            frameHeight = preset.getHeight();
        } else {
            // Portrait: swap because sensor is landscape
            frameWidth = preset.getHeight();
            frameHeight = preset.getWidth();
        }

        Size cameraResolution = new Size(frameWidth, frameHeight);
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        // [ANDROID] Allocate two RGBA_8888 frame buffers: width * height * 4 bytes each.
        buffers = new ByteBuffer[NUMBER_OF_BUFFERS];
        for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
            buffers[i] = ByteBuffer.allocateDirect(frameWidth * frameHeight * 4);
            buffers[i].order(ByteOrder.nativeOrder()); // Match CPU endianness
            buffers[i].position(0);
        }

        // [ANDROID] ImageAnalysis: CameraX delivers each frame as an ImageProxy.
        // STRATEGY_KEEP_ONLY_LATEST: drop queued frames if the analyzer is busy — prevents lag buildup.
        // OUTPUT_IMAGE_FORMAT_RGBA_8888: request pre-converted RGBA (no manual YUV conversion needed).
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(cameraResolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        // [ANDROID] Run frame analysis on a dedicated background thread, not the main thread.
        // The main thread is for UI only — processing every camera frame there causes
        // dropped frames and sluggish AR rendering.
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), imageAnalyzer);
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle((LifecycleOwner) activity, cameraSelector, imageAnalysis);
    }

    // -------------------------------------------------------
    // FRAME ANALYZER
    // -------------------------------------------------------

    /**
     * [ANDROID + DEEPAR] Called by CameraX for every camera frame on a background thread.
     *
     * Flow:
     *   1. Copy RGBA pixel data from ImageProxy into the current double-buffer slot (background thread)
     *   2. Capture all frame metadata as locals (width, rotation, etc.)
     *   3. Advance the buffer slot so the next frame writes to the other buffer
     *   4. Close the ImageProxy — CRITICAL: CameraX blocks until close() is called
     *   5. Post receiveFrame() to the main thread via mainHandler
     *
     * WHY THE POST TO MAIN THREAD (crash fix):
     *   DeepAR enforces that every method call — receiveFrame(), switchEffect(),
     *   setRenderSurface() — must come from the same thread where DeepAR was first
     *   initialized. DeepAR.initialize() is called from PreviewActivity.initialize(),
     *   which runs on the main thread. CameraX runs this analyzer on a background
     *   executor (pool-6-thread-1), so calling receiveFrame() here directly caused:
     *   IllegalStateException: "Method called from the thread that DeepAR was not
     *   initialized in."
     *   Pixel data is safe to copy on the background thread — only the DeepAR call
     *   itself must be on main.
     */
    private final ImageAnalysis.Analyzer imageAnalyzer = new ImageAnalysis.Analyzer() {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            // [ANDROID] image.getPlanes()[0] = the single RGBA plane (no separate UV in RGBA format)
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            buffer.rewind(); // Reset read position to 0

            buffers[currentBuffer].put(buffer);  // Copy frame pixels into our double-buffer slot
            buffers[currentBuffer].position(0);  // Reset so DeepAR reads from the start

            // Capture everything needed for the main-thread call before closing the image
            final int bufferIndex  = currentBuffer;
            final int frameWidth   = image.getWidth();
            final int frameHeight  = image.getHeight();
            final int rotation     = image.getImageInfo().getRotationDegrees();
            final boolean mirror   = lensFacing == CameraSelector.LENS_FACING_FRONT;
            final int pixelStride  = image.getPlanes()[0].getPixelStride();

            // Advance buffer slot now so the next frame goes into the other buffer
            currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS;

            // [ANDROID] MUST call close() — CameraX won't deliver the next frame until this returns.
            // Safe to close here because pixel data is already copied into buffers[bufferIndex].
            image.close();

            // [DEEPAR] receiveFrame() must be called on the thread DeepAR was initialized on
            // (the main thread, where setRenderSurface() was also called).
            // Posting here moves the DeepAR call off the CameraX background executor.
            mainHandler.post(() ->
                deepARManager.receiveFrame(
                        buffers[bufferIndex],
                        frameWidth,
                        frameHeight,
                        rotation,    // How many degrees to rotate the frame to match the display
                        mirror,      // Flip horizontally for front camera
                        DeepARImageFormat.RGBA_8888,
                        pixelStride  // Bytes per pixel — 4 for RGBA_8888
                )
            );
        }
    };

    // -------------------------------------------------------
    // ORIENTATION HELPER
    // -------------------------------------------------------

    /**
     * [ANDROID] Maps physical device rotation to a logical orientation constant.
     * Needed because the camera sensor is always landscape — we must tell
     * DeepAR and CameraX how much to rotate the frame to match the display.
     *
     * Side effect: updates this.width and this.height with current screen pixel dimensions.
     *
     * Returns one of: SCREEN_ORIENTATION_PORTRAIT / LANDSCAPE / REVERSE_PORTRAIT / REVERSE_LANDSCAPE
     */
    private int getScreenOrientation() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;

        int orientation;
        // Portrait-native device (most phones): natural position is ROTATION_0 = portrait
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width
                || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:   orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; break;
                case Surface.ROTATION_90:  orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; break;
                case Surface.ROTATION_180: orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT; break;
                case Surface.ROTATION_270: orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE; break;
                default:                   orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; break;
            }
        } else {
            // Landscape-native device (some tablets): natural position is ROTATION_0 = landscape
            switch (rotation) {
                case Surface.ROTATION_0:   orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; break;
                case Surface.ROTATION_90:  orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; break;
                case Surface.ROTATION_180: orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE; break;
                case Surface.ROTATION_270: orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT; break;
                default:                   orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; break;
            }
        }
        return orientation;
    }
}
