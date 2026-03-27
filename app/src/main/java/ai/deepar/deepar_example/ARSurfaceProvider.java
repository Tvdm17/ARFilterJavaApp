package ai.deepar.deepar_example;

// ============================================================
// WHAT THIS FILE IS
// ============================================================
// ARSurfaceProvider is the bridge between CameraX and DeepAR
// when using the "external GL texture" camera path.
//
// This file is only active when useExternalCameraTexture = true
// in MainActivity (currently set to false, so this class is not
// used in the default build).
//
// HOW IT WORKS (the GL texture path):
//   1. CameraX asks for a Surface to write camera frames into
//      → onSurfaceRequested() is called
//   2. We ask DeepAR for an OpenGL texture handle
//      → deepAR.getExternalGlTexture() returns a native GL texture ID
//   3. We wrap that GL texture in a SurfaceTexture
//      → SurfaceTexture bridges Android camera output to OpenGL
//   4. When a frame arrives, surfaceTexture.updateTexImage() uploads it to GPU
//   5. We notify DeepAR → deepAR.receiveFrameExternalTexture()
//
// WHY USE THIS vs ByteBuffer?
//   - No CPU copy of pixel data → better performance at high resolution
//   - Frame stays on GPU the entire time (camera → GL texture → DeepAR render)
//   - ByteBuffer path copies pixels CPU-side each frame (slower but simpler)
//
// ANDROID STUDIO IMPORT NOTES:
//   - This class has no dependencies beyond DeepAR SDK + AndroidX Camera
//   - No layout or resource references — pure Java
//   - If you switch useExternalCameraTexture to true in MainActivity,
//     make sure EGL context is initialized before calling getExternalGlTexture()
// ============================================================

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

// [ANDROID] CameraX imports for the Preview use-case surface provider
import androidx.annotation.NonNull;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.core.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;

// [DEEPAR] The core DeepAR engine — we call it to get the GL texture and to send frames
import ai.deepar.ar.DeepAR;

/**
 * [DEEPAR + ANDROID] Surface provider for CameraX Preview use-case.
 *
 * Implements Preview.SurfaceProvider — CameraX calls onSurfaceRequested()
 * to get a Surface it can write camera frames into.
 *
 * We give it a Surface backed by DeepAR's own OpenGL texture, so camera
 * frames flow directly into the GPU without CPU involvement.
 *
 * NEW UI: You don't need to modify this class for UI changes.
 * It's pure camera-to-DeepAR plumbing.
 */
public class ARSurfaceProvider implements Preview.SurfaceProvider {

    /** [ANDROID] Tag for Logcat output — shows class name in log messages. */
    private static final String tag = ARSurfaceProvider.class.getSimpleName();

    /**
     * [ANDROID] Constructor. Stores references to the context (for executor access)
     * and the DeepAR engine (to request GL texture + send frames).
     */
    ARSurfaceProvider(Context context, DeepAR deepAR) {
        this.context = context;
        this.deepAR = deepAR;
    }

    /**
     * [ANDROID] Debug helper — logs the current EGL display and context handles.
     * EGL (Embedded-System Graphics Library) is the interface between OpenGL ES
     * and the native window system. Useful for debugging GL context issues.
     * EGL14 is the Android OpenGL ES 2.0+ EGL binding.
     */
    private void printEglState() {
        Log.d(tag, "display: " + EGL14.eglGetCurrentDisplay().getNativeHandle()
                + ", context: " + EGL14.eglGetCurrentContext().getNativeHandle());
    }

    /**
     * [ANDROID + DEEPAR] CameraX calls this method when it needs a Surface to write frames into.
     * We must call either request.provideSurface() or request.willNotProvideSurface()
     * before returning, otherwise CameraX blocks.
     *
     * Full flow:
     *   1. Get (or reuse) DeepAR's OpenGL texture handle
     *   2. Wrap it in a SurfaceTexture
     *   3. Register a frame-available listener on the SurfaceTexture
     *   4. Wrap the SurfaceTexture in a Surface
     *   5. Give the Surface to CameraX via request.provideSurface()
     */
    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
        Log.d(tag, "Surface requested");
        printEglState(); // Debug: log EGL state when surface is requested

        // -------------------------------------------------------
        // STEP 1: Get DeepAR's external OpenGL texture
        // -------------------------------------------------------
        // [DEEPAR] getExternalGlTexture() returns a native OpenGL ES texture ID
        // that DeepAR owns and manages internally. Returns 0 if EGL is not initialized.
        // We cache it (nativeGLTextureHandle) so we don't request a new one every time.
        if (nativeGLTextureHandle == 0) {
            nativeGLTextureHandle = deepAR.getExternalGlTexture();
            Log.d(tag, "request new external GL texture");
            printEglState();
        }

        // If DeepAR couldn't provide a texture (e.g. EGL not ready), bail out
        if (nativeGLTextureHandle == 0) {
            // [ANDROID] willNotProvideSurface() tells CameraX we can't deliver a surface.
            // CameraX will not retry until onSurfaceRequested() is called again.
            request.willNotProvideSurface();
            return;
        }

        // -------------------------------------------------------
        // STEP 2: Wrap the GL texture in a SurfaceTexture
        // -------------------------------------------------------
        Size resolution = request.getResolution(); // [ANDROID] Requested frame dimensions from CameraX

        if (surfaceTexture == null) {
            // [ANDROID] SurfaceTexture(int texName) creates a SurfaceTexture that renders
            // incoming frames into the specified OpenGL ES texture object.
            // This is the standard Android mechanism for receiving camera frames on the GPU.
            surfaceTexture = new SurfaceTexture(nativeGLTextureHandle);

            // [ANDROID] onFrameAvailableListener: called on the GL thread whenever
            // a new camera frame has been written into the SurfaceTexture.
            surfaceTexture.setOnFrameAvailableListener(__ -> {
                if (stop) {
                    return; // Don't process frames after stop() was called
                }

                // [ANDROID] updateTexImage() takes the most recent frame from the
                // SurfaceTexture's buffer queue and uploads it to the GL texture.
                // Must be called from a thread with an active EGL context.
                surfaceTexture.updateTexImage();

                if (isNotifyDeepar) {
                    // [DEEPAR] Send the frame to DeepAR via GL texture.
                    // Parameters:
                    //   - width, height: frame dimensions
                    //   - orientation: rotation degrees (0/90/180/270)
                    //   - mirror: true for front camera (to show as mirror image)
                    //   - nativeGLTextureHandle: the OpenGL texture ID with the frame
                    deepAR.receiveFrameExternalTexture(
                            resolution.getWidth(),
                            resolution.getHeight(),
                            orientation,
                            mirror,
                            nativeGLTextureHandle
                    );
                }
            });
        }

        // [ANDROID] Set the expected size of frames coming from the camera
        surfaceTexture.setDefaultBufferSize(resolution.getWidth(), resolution.getHeight());

        // -------------------------------------------------------
        // STEP 3: Wrap SurfaceTexture in a Surface
        // -------------------------------------------------------
        // [ANDROID] Surface is the producer end of a buffer queue.
        // CameraX writes camera frames into this Surface.
        // The frames are automatically transferred into the SurfaceTexture (and thus the GL texture).
        if (surface == null) {
            surface = new Surface(surfaceTexture);
        }

        // -------------------------------------------------------
        // STEP 4: Listen for orientation changes
        // -------------------------------------------------------
        // [ANDROID] Register a listener that updates the orientation field
        // when the screen rotates. Runs on the main executor.
        // transformationInfo.getRotationDegrees() gives the frame rotation needed
        // to match the display (0, 90, 180, or 270).
        request.setTransformationInfoListener(
                ContextCompat.getMainExecutor(context),
                transformationInfo -> orientation = transformationInfo.getRotationDegrees()
        );

        // -------------------------------------------------------
        // STEP 5: Provide the Surface to CameraX
        // -------------------------------------------------------
        // [ANDROID] provideSurface() tells CameraX "use this Surface for camera output".
        // The callback (result) fires when CameraX is done using the surface,
        // which is when we'd normally release it. We log the result code for debugging.
        request.provideSurface(surface, ContextCompat.getMainExecutor(context), result -> {
            switch (result.getResultCode()) {
                case SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY:
                    Log.i(tag, "RESULT_SURFACE_USED_SUCCESSFULLY"); // Normal: camera closed cleanly
                    break;
                case SurfaceRequest.Result.RESULT_INVALID_SURFACE:
                    Log.i(tag, "RESULT_INVALID_SURFACE"); // Surface was bad — shouldn't happen
                    break;
                case SurfaceRequest.Result.RESULT_REQUEST_CANCELLED:
                    Log.i(tag, "RESULT_REQUEST_CANCELLED"); // Camera was unbound before use
                    break;
                case SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED:
                    Log.i(tag, "RESULT_SURFACE_ALREADY_PROVIDED"); // Surface already submitted
                    break;
                case SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE:
                    Log.i(tag, "RESULT_WILL_NOT_PROVIDE_SURFACE"); // We called willNotProvideSurface()
                    break;
            }
        });
    }

    /**
     * [DEEPAR] Returns the current mirror flag.
     * Mirror = true means DeepAR will horizontally flip the output frame.
     * Used for front camera to show a natural "mirror" view.
     */
    public boolean isMirror() {
        return mirror;
    }

    /**
     * [DEEPAR] Sets the mirror flag and handles the camera-switch transition.
     *
     * When switching between front and back camera, there's a brief window
     * where old (wrongly-mirrored) frames might arrive. To prevent a visual
     * glitch, we pause frame delivery to DeepAR for 1 second after switching.
     *
     * @param mirror true = front camera (mirror), false = back camera (no mirror)
     */
    public void setMirror(boolean mirror) {
        this.mirror = mirror;

        // Nothing to delay if the surface isn't set up yet
        if (surfaceTexture == null || surface == null) {
            return;
        }

        // [DEEPAR] Temporarily stop sending frames while the camera switch completes.
        // After 1 second, resume. This avoids a briefly mirrored/wrong frame.
        isNotifyDeepar = false;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                isNotifyDeepar = true; // Resume after 1 second
            }
        }, 1000);
    }

    /**
     * [DEEPAR + ANDROID] Signals this provider to stop feeding frames to DeepAR.
     *
     * Call this in Activity.onDestroy() or when cleaning up.
     * After stop() is called, onFrameAvailable() will return early without
     * calling DeepAR — prevents use-after-free crashes if DeepAR is already released.
     */
    public void stop() {
        stop = true;
    }

    // -------------------------------------------------------
    // STATE FIELDS
    // -------------------------------------------------------

    /** [DEEPAR] Controls whether frames are forwarded to DeepAR. False during camera switch. */
    private boolean isNotifyDeepar = true;

    /** [GENERAL] Set by stop() — prevents frame delivery after cleanup. */
    private boolean stop = false;

    /**
     * [DEEPAR] Mirror flag. True = horizontally flip output (for front camera).
     * Default true = assume front camera at startup.
     */
    private boolean mirror = true;

    /**
     * [ANDROID + DEEPAR] Frame rotation in degrees (0/90/180/270).
     * Updated by the transformation info listener when screen rotates.
     * Passed to DeepAR so it can rotate the frame correctly before processing.
     */
    private int orientation = 0;

    /**
     * [ANDROID] SurfaceTexture backed by DeepAR's OpenGL texture.
     * Camera frames are written here by CameraX and uploaded to GPU automatically.
     */
    private SurfaceTexture surfaceTexture;

    /**
     * [ANDROID] Surface wrapping the SurfaceTexture — given to CameraX as output target.
     */
    private Surface surface;

    /**
     * [DEEPAR] The OpenGL ES texture ID returned by deepAR.getExternalGlTexture().
     * 0 = not yet allocated (initial state).
     */
    private int nativeGLTextureHandle = 0;

    /** [DEEPAR] Reference to the DeepAR engine for GL texture request and frame delivery. */
    private final DeepAR deepAR;

    /** [ANDROID] Activity/app context used for getting the main executor. */
    private final Context context;
}
