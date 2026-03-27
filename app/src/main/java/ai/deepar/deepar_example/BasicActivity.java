package ai.deepar.deepar_example;

// ============================================================
// WHAT THIS FILE IS
// ============================================================
// BasicActivity is a secondary screen that exists purely to demonstrate
// DeepAR's lifecycle behavior: when you navigate away from the AR camera
// (MainActivity), DeepAR is released (in onStop), and when you come back,
// it reinitializes from scratch.
//
// In a real app this could be:
//   - A settings screen
//   - A gallery of saved screenshots/videos
//   - An onboarding flow
//   - Any screen that is NOT the AR camera
//
// ANDROID STUDIO IMPORT NOTE:
//   This activity uses AppCompat + Material Design components (Toolbar, FAB, Snackbar).
//   These are standard AndroidX / Google Material dependencies — no DeepAR involvement here.
//
// NEW UI NOTE:
//   If you redesign the app, you can repurpose this activity as your non-AR screen,
//   or delete it entirely and replace with Fragments if you prefer single-activity architecture.
// ============================================================

import android.os.Bundle;

// [ANDROID] Material Design components — FloatingActionButton and Snackbar
// (imported but not actually used in the current code — can be removed)
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

// [ANDROID] AppCompatActivity = base class that backports modern Action Bar / theme features
import androidx.appcompat.app.AppCompatActivity;
// [ANDROID] Toolbar = Material Design app bar widget (replaces the old ActionBar)
import androidx.appcompat.widget.Toolbar;

import android.view.View;

/**
 * [ANDROID] A simple secondary activity demonstrating navigation away from the AR camera.
 *
 * When this activity is open:
 *   - MainActivity is paused (onStop called → DeepAR released, camera freed)
 *   - No AR processing happening
 *   - Camera hardware is available to other apps
 *
 * When the user presses Back:
 *   - This activity is destroyed
 *   - MainActivity resumes (onStart called → DeepAR reinitializes, camera restarted)
 *
 * This is the correct DeepAR lifecycle pattern: release in onStop, reinit in onStart.
 */
public class BasicActivity extends AppCompatActivity {

    /**
     * [ANDROID] Called once when the activity is first created.
     *
     * Steps:
     *   1. Inflate the layout (activity_basic.xml, which includes content_basic.xml)
     *   2. Find the Toolbar view and register it as the app's ActionBar
     *
     * No DeepAR code here — this is pure standard Android.
     *
     * NEW UI: Replace R.layout.activity_basic with your own layout.
     * If you don't need a Toolbar, remove the setSupportActionBar() call.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic); // Inflate layout

        // [ANDROID] Find the Toolbar widget and register it as the app bar.
        // setSupportActionBar() connects the Toolbar to the AppCompat action bar system,
        // enabling title, menu items, Up navigation, etc.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
}
