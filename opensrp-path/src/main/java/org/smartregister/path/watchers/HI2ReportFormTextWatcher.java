package org.smartregister.path.watchers;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.fragments.JsonFormFragment;

import java.util.HashMap;
import java.util.Map;

public class HI2ReportFormTextWatcher implements TextWatcher {

    private static final Map<String, String[]> aggregateFieldsMap;

    static {
        aggregateFieldsMap = new HashMap<>();
        aggregateFieldsMap.put("CHN1-011", new String[]{"CHN1-005", "CHN1-010"});
        aggregateFieldsMap.put("CHN1-021", new String[]{"CHN1-015", "CHN1-020"});
        aggregateFieldsMap.put("CHN1-025", new String[]{"CHN1-011", "CHN1-021"});
        aggregateFieldsMap.put("CHN2-015", new String[]{"CHN2-005", "CHN2-010"});
        aggregateFieldsMap.put("CHN2-030", new String[]{"CHN2-020", "CHN2-025"});
        aggregateFieldsMap.put("CHN2-041", new String[]{"CHN2-035", "CHN2-040"});
        aggregateFieldsMap.put("CHN2-051", new String[]{"CHN2-045", "CHN2-050"});
        aggregateFieldsMap.put("CHN2-061", new String[]{"CHN2-055", "CHN2-060"});
    }


    private static final Map<String, String> indicatorKeyMap;

    static {
        indicatorKeyMap = new HashMap<>();
        indicatorKeyMap.put("CHN1-005", "CHN1-011");
        indicatorKeyMap.put("CHN1-010", "CHN1-011");
        indicatorKeyMap.put("CHN1-015", "CHN1-021");
        indicatorKeyMap.put("CHN1-020", "CHN1-021");
        indicatorKeyMap.put("CHN1-011", "CHN1-025");
        indicatorKeyMap.put("CHN1-021", "CHN1-025");
        indicatorKeyMap.put("CHN2-005", "CHN2-015");
        indicatorKeyMap.put("CHN2-010", "CHN2-015");
        indicatorKeyMap.put("CHN2-020", "CHN2-030");
        indicatorKeyMap.put("CHN2-025", "CHN2-030");
        indicatorKeyMap.put("CHN2-035", "CHN2-041");
        indicatorKeyMap.put("CHN2-040", "CHN2-041");
        indicatorKeyMap.put("CHN2-045", "CHN2-051");
        indicatorKeyMap.put("CHN2-050", "CHN2-051");
        indicatorKeyMap.put("CHN2-055", "CHN2-061");
        indicatorKeyMap.put("CHN2-060", "CHN2-061");
    }


    private final View mView;
    private final JsonFormFragment formFragment;
    private final String hia2Indicator;


    public HI2ReportFormTextWatcher(JsonFormFragment formFragment, View view, String hi2IndicatorCode) {
        this.formFragment = formFragment;
        mView = view;
        hia2Indicator = hi2IndicatorCode;

    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

    }

    public void afterTextChanged(Editable editable) {

        if (indicatorKeyMap.containsKey(hia2Indicator)) {

            Integer aggregateValue = 0;

            String[] operandIndicators = aggregateFieldsMap.get(indicatorKeyMap.get(hia2Indicator));

            for (int i = 0; i < operandIndicators.length; i++) {
                MaterialEditText editTextIndicatorView = (MaterialEditText) formFragment.getMainView().findViewWithTag(operandIndicators[i]);
                aggregateValue += (editTextIndicatorView.getText() == null || editTextIndicatorView.getText().toString().isEmpty() ? 0 : Integer.valueOf(editTextIndicatorView.getText().toString()));

            }

            MaterialEditText aggregateEditText = (MaterialEditText) formFragment.getMainView().findViewWithTag(indicatorKeyMap.get(hia2Indicator));
            aggregateEditText.setText(Integer.toString(aggregateValue));
        }

    }

}
