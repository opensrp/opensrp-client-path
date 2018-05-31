package shared;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.smartregister.path.BuildConfig;

import shared.customshadows.FontTextViewShadow;

/**
 * Created by onadev on 13/06/2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, application = VaccinatorApplicationTestVersion.class, shadows = {FontTextViewShadow.class}, sdk = 21)
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
