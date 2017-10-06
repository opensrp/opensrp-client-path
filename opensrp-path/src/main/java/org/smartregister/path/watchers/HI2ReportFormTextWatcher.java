package org.smartregister.path.watchers;

import android.text.Editable;
import android.text.TextWatcher;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.fragments.JsonFormFragment;

import org.smartregister.path.service.HIA2Service;

import java.util.HashMap;
import java.util.Map;

public class HI2ReportFormTextWatcher implements TextWatcher {

    private static final Map<String, String[]> aggregateFieldsMap;

    static {
        aggregateFieldsMap = new HashMap<>();
        aggregateFieldsMap.put(HIA2Service.CHN1_011, new String[]{HIA2Service.CHN1_005, HIA2Service.CHN1_010});
        aggregateFieldsMap.put(HIA2Service.CHN1_021, new String[]{HIA2Service.CHN1_015, HIA2Service.CHN1_020});
        aggregateFieldsMap.put(HIA2Service.CHN1_025, new String[]{HIA2Service.CHN1_011, HIA2Service.CHN1_021});
        aggregateFieldsMap.put(HIA2Service.CHN2_015, new String[]{HIA2Service.CHN2_005, HIA2Service.CHN2_010});
        aggregateFieldsMap.put(HIA2Service.CHN2_030, new String[]{HIA2Service.CHN2_020, HIA2Service.CHN2_025});
        aggregateFieldsMap.put(HIA2Service.CHN2_041, new String[]{HIA2Service.CHN2_035, HIA2Service.CHN2_040});
        aggregateFieldsMap.put(HIA2Service.CHN2_051, new String[]{HIA2Service.CHN2_045, HIA2Service.CHN2_050});
        aggregateFieldsMap.put(HIA2Service.CHN2_061, new String[]{HIA2Service.CHN2_055, HIA2Service.CHN2_060});
    }

    private static final Map<String, String> indicatorKeyMap;

    static {
        indicatorKeyMap = new HashMap<>();
        indicatorKeyMap.put(HIA2Service.CHN1_005, HIA2Service.CHN1_011);
        indicatorKeyMap.put(HIA2Service.CHN1_010, HIA2Service.CHN1_011);
        indicatorKeyMap.put(HIA2Service.CHN1_015, HIA2Service.CHN1_021);
        indicatorKeyMap.put(HIA2Service.CHN1_020, HIA2Service.CHN1_021);
        indicatorKeyMap.put(HIA2Service.CHN1_011, HIA2Service.CHN1_025);
        indicatorKeyMap.put(HIA2Service.CHN1_021, HIA2Service.CHN1_025);
        indicatorKeyMap.put(HIA2Service.CHN2_005, HIA2Service.CHN2_015);
        indicatorKeyMap.put(HIA2Service.CHN2_010, HIA2Service.CHN2_015);
        indicatorKeyMap.put(HIA2Service.CHN2_020, HIA2Service.CHN2_030);
        indicatorKeyMap.put(HIA2Service.CHN2_025, HIA2Service.CHN2_030);
        indicatorKeyMap.put(HIA2Service.CHN2_035, HIA2Service.CHN2_041);
        indicatorKeyMap.put(HIA2Service.CHN2_040, HIA2Service.CHN2_041);
        indicatorKeyMap.put(HIA2Service.CHN2_045, HIA2Service.CHN2_051);
        indicatorKeyMap.put(HIA2Service.CHN2_050, HIA2Service.CHN2_051);
        indicatorKeyMap.put(HIA2Service.CHN2_055, HIA2Service.CHN2_061);
        indicatorKeyMap.put(HIA2Service.CHN2_060, HIA2Service.CHN2_061);
    }

    private final JsonFormFragment formFragment;
    private final String hia2Indicator;

    public HI2ReportFormTextWatcher(JsonFormFragment formFragment, String hi2IndicatorCode) {
        this.formFragment = formFragment;
        hia2Indicator = hi2IndicatorCode;

    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        //default overridden method from interface
    }

    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        //default overridden method from interface
    }

    public void afterTextChanged(Editable editable) {

        if (indicatorKeyMap.containsKey(hia2Indicator)) {

            Integer aggregateValue = 0;

            String[] operandIndicators = aggregateFieldsMap.get(indicatorKeyMap.get(hia2Indicator));

            for (int i = 0; i < operandIndicators.length; i++) {
                MaterialEditText editTextIndicatorView = (MaterialEditText) formFragment.getMainView().findViewWithTag(operandIndicators[i]);
                aggregateValue += editTextIndicatorView.getText() == null || editTextIndicatorView.getText().toString().isEmpty() ? 0 : Integer.valueOf(editTextIndicatorView.getText().toString());

            }

            MaterialEditText aggregateEditText = (MaterialEditText) formFragment.getMainView().findViewWithTag(indicatorKeyMap.get(hia2Indicator));
            aggregateEditText.setText(Integer.toString(aggregateValue));
        }

    }

}
