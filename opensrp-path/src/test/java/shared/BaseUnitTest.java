package shared;

import android.view.View;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.path.BuildConfig;

import java.util.ArrayList;
import java.util.Collection;

import shared.customshadows.FontTextViewShadow;
import shared.customshadows.ShadowRealmDatabase;

/**
 * Created by onadev on 13/06/2017.
 */


@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, application = VaccinatorApplicationTestVersion.class, shadows = {FontTextViewShadow.class, ShadowRealmDatabase.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
public abstract class BaseUnitTest {
    public static class INT_TEST_CONSTANTS {
        public static final int INT_1 = 1;
        public static final int INT_2 = 2;
        public static final int INT_3 = 3;
    }

    public static class STRING_TEST_CONSTANTS {
        public static final String EMPTY_STRING = "";
    }
}
