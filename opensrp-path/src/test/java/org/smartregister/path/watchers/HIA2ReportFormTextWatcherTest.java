package org.smartregister.path.watchers;

import android.text.Editable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.smartregister.path.application.VaccinatorApplication;

import shared.BaseUnitTest;

/**
 * Created by ndegwamartin on 06/10/2017.
 */
@PrepareForTest({VaccinatorApplication.class})
public class HIA2ReportFormTextWatcherTest extends BaseUnitTest {

    @Mock
    private HIA2ReportFormTextWatcher hia2ReportFormTextWatcher;

    @Mock
    private Editable editable;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Captor
    private ArgumentCaptor<Integer> param1IntCaptor;

    @Captor
    private ArgumentCaptor<Integer> param2IntCaptor;

    @Captor
    private ArgumentCaptor<Integer> param3IntCaptor;

    @Captor
    private ArgumentCaptor<Editable> editableArgumentCaptor;

    @Before
    public void setUp() {

        org.mockito.MockitoAnnotations.initMocks(this);
    }

    @Test
    public void beforeTextChangedWasCalledWithTheCorrectParameters() {
        hia2ReportFormTextWatcher.beforeTextChanged(STRING_TEST_CONSTANTS.EMPTY_STRING, INT_TEST_CONSTANTS.INT_1, INT_TEST_CONSTANTS.INT_2, INT_TEST_CONSTANTS.INT_3);
        Mockito.verify(hia2ReportFormTextWatcher).beforeTextChanged(stringCaptor.capture(), param1IntCaptor.capture(), param2IntCaptor.capture(), param3IntCaptor.capture());
        org.junit.Assert.assertEquals(stringCaptor.getValue(), STRING_TEST_CONSTANTS.EMPTY_STRING);
        org.hamcrest.MatcherAssert.assertThat(param1IntCaptor.getValue(), org.hamcrest.CoreMatchers.equalTo(INT_TEST_CONSTANTS.INT_1));
        org.hamcrest.MatcherAssert.assertThat(param2IntCaptor.getValue(), org.hamcrest.CoreMatchers.equalTo(INT_TEST_CONSTANTS.INT_2));
        org.hamcrest.MatcherAssert.assertThat(param3IntCaptor.getValue(), org.hamcrest.CoreMatchers.equalTo(INT_TEST_CONSTANTS.INT_3));
    }

    @Test
    public void onTextChangedWasCalledWithTheCorrectParameters() {
        hia2ReportFormTextWatcher.onTextChanged(STRING_TEST_CONSTANTS.EMPTY_STRING, INT_TEST_CONSTANTS.INT_1, INT_TEST_CONSTANTS.INT_2, INT_TEST_CONSTANTS.INT_3);
        Mockito.verify(hia2ReportFormTextWatcher).onTextChanged(stringCaptor.capture(), param1IntCaptor.capture(), param2IntCaptor.capture(), param3IntCaptor.capture());
        org.junit.Assert.assertEquals(stringCaptor.getValue(), STRING_TEST_CONSTANTS.EMPTY_STRING);
        org.hamcrest.MatcherAssert.assertThat(param1IntCaptor.getValue(), org.hamcrest.CoreMatchers.equalTo(INT_TEST_CONSTANTS.INT_1));
        org.hamcrest.MatcherAssert.assertThat(param2IntCaptor.getValue(), org.hamcrest.CoreMatchers.equalTo(INT_TEST_CONSTANTS.INT_2));
        org.hamcrest.MatcherAssert.assertThat(param3IntCaptor.getValue(), org.hamcrest.CoreMatchers.equalTo(INT_TEST_CONSTANTS.INT_3));
    }

    @Test
    public void afterTextChangedWasCalledWithTheCorrectParameters() {
        hia2ReportFormTextWatcher.afterTextChanged(editable);
        Mockito.verify(hia2ReportFormTextWatcher).afterTextChanged(editableArgumentCaptor.capture());
        org.hamcrest.MatcherAssert.assertThat(editableArgumentCaptor.getValue(), org.hamcrest.CoreMatchers.equalTo(editableArgumentCaptor.getValue()));
    }
}
