package ai.deepar.deepar_example;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 *
 *
 * In a real project you'd replace it with actual tests like:
 *   - "Does the camera permission dialog appear on first launch?"
 *   - "Does switching filters actually change the effect?"
 *   - "Does the screenshot button save a file?"
 *
 *   For now you can safely ignore or delete it.
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("ai.deepar.deepar_example", appContext.getPackageName());
    }
}
