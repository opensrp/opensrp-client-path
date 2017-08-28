package org.smartregister.path.fragment;

import org.junit.Before;
import org.junit.Test;

import shared.BaseUnitTest;


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
        junit.framework.Assert.assertNotNull(pathJsonFormFragment);

    }

    @Test
    public void motherLookUpListenerIsNotNullOnFragmentInstantiation() throws Exception {
        junit.framework.Assert.assertNotNull(pathJsonFormFragment.motherLookUpListener());

    }

    @Test
    public void contextNotNullOnFragmentInstantiation() throws Exception {
        junit.framework.Assert.assertNotNull(pathJsonFormFragment.context());

    }

}