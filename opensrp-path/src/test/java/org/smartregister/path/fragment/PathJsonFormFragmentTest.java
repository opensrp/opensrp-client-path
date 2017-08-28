package org.smartregister.path.fragment;

import org.junit.Before;
import org.junit.Test;

import shared.BaseUnitTest;

import static junit.framework.Assert.assertNotNull;

/**
 * Created by ona on 28/08/2017.
 */
public class PathJsonFormFragmentTest extends BaseUnitTest {

    protected PathJsonFormFragment pathJsonFormFragment;

    @Before
    public void setUp() {
        pathJsonFormFragment = PathJsonFormFragment.getFormFragment("testStep");
    }

    @Test
    public void setPathJsonFormFragmentNotNullOnInstantiation() throws Exception {
        assertNotNull(pathJsonFormFragment);

    }

    @Test
    public void motherLookUpListenerIsNotNullOnFragmentInstantiation() throws Exception {
        assertNotNull(pathJsonFormFragment.motherLookUpListener());

    }

    @Test
    public void contextNotNullOnFragmentInstantiation() throws Exception {
        assertNotNull(pathJsonFormFragment.context());

    }

}