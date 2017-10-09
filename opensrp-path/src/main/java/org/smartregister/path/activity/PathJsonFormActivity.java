package org.smartregister.path.activity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.immunization.repository.VaccineTypeRepository;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.fragment.PathJsonFormFragment;
import org.smartregister.path.repository.StockRepository;

import java.util.ArrayList;
import java.util.Date;

import util.JsonFormUtils;

/**
 * Created by keyman on 11/04/2017.
 */
public class PathJsonFormActivity extends JsonFormActivity {

    private int generatedId = -1;
    private MaterialEditText balanceTextView;
    private PathJsonFormFragment pathJsonFormFragment;
    private static final String STOCK_ISSUED = "Stock Issued";
    private static final String STOCK_LOSS_ADJUSTMENT = "Stock Loss/Adjustment";
    private static final String STOCK_RECEIVED = "Stock Received";
    private static final String ZERO_STRING = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void initializeFormFragment() {
        pathJsonFormFragment = PathJsonFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
        getSupportFragmentManager().beginTransaction()
                .add(com.vijay.jsonwizard.R.id.container, pathJsonFormFragment).commit();
    }

    @Override
    public void writeValue(String stepName, String key, String value, String openMrsEntityParent, String openMrsEntity, String openMrsEntityId) throws JSONException {
        super.writeValue(stepName, key, value, openMrsEntityParent, openMrsEntity, openMrsEntityId);
        refreshCalculateLogic(key, value);

    }

    @Override
    public void onFormFinish() {
        super.onFormFinish();
    }

    private void refreshCalculateLogic(String key, String value) {
        stockVialsEnteredInReceivedForm(key, value);
        stockDateEnteredInReceivedForm(key, value);
        stockDateEnteredInIssuedForm(key, value);
        stockVialsEnteredInIssuedForm(key, value);
        stockWastedVialsEnteredInIssuedForm(key, value);
        stockDateEnteredInAdjustmentForm(key, value);
        stockVialsEnteredInAdjustmentForm(key, value);
    }

    private void stockDateEnteredInIssuedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_ISSUED)) {
                StockRepository str = VaccinatorApplication.getInstance().stockRepository();
                if (key.equalsIgnoreCase("Date_Stock_Issued") && value != null && !value.equalsIgnoreCase("")) {
                    if (balanceTextView == null) {
                        ArrayList<View> views = getFormDataViews();
                        for (int i = 0; i < views.size(); i++) {
                            if (views.get(i) instanceof MaterialEditText && ((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase(getString(R.string.vials_issued_key))) {
                                balanceTextView = (MaterialEditText) views.get(i);
                            }
                        }
                    }
                    String label = "";
                    int currentBalance = 0;
                    int newBalance = 0;
                    Date encounterDate = new Date();
                    String vialsValue = "";
                    String wastedVials = ZERO_STRING;
                    String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_ISSUED, "").trim();
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key))) {
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_Issued") && questions.has(getString(R.string.value_key))) {
                                label = questions.getString(getString(R.string.value_key));
                                if (label != null && StringUtils.isNotBlank(label)) {
                                    Date dateTime = JsonFormUtils.formatDate(label, false);
                                    if (dateTime != null) {
                                        encounterDate = dateTime;
                                    }
                                }

                                currentBalance = str.getVaccineUsedToday(encounterDate.getTime(), checkIfMeasles(vaccineName.toLowerCase()));
                            }

                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_wasted_key))) {
                                if (questions.has(getString(R.string.value_key))) {
                                    if (!StringUtils.isBlank(questions.getString(getString(R.string.value_key)))) {
                                        wastedVials = questions.getString(getString(R.string.value_key));
                                    }
                                } else {
                                    wastedVials = ZERO_STRING;
                                }
                            }
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_issued_key))) {
                                if (questions.has(getString(R.string.value_key))) {
                                    if (!StringUtils.isBlank(questions.getString(getString(R.string.value_key)))) {
                                        vialsValue = questions.getString(getString(R.string.value_key));
                                    }
                                } else {
                                    pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");
                                }
                            }

                        }
                    }
                    if (!StringUtils.isBlank(vialsValue)) {
                        newBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime()) - Integer.parseInt(vialsValue) - Integer.parseInt(wastedVials);
                        pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "New balance: " + newBalance);
                    }

                    int vialsUsed = 0;
                    VaccineTypeRepository vaccineTypeRepository = VaccinatorApplication.getInstance().vaccineTypeRepository();
                    int dosesPerVial = vaccineTypeRepository.getDosesPerVial(vaccineName);
                    if (currentBalance % dosesPerVial == 0) {
                        vialsUsed = currentBalance / dosesPerVial;
                    } else if (currentBalance != 0) {
                        vialsUsed = (currentBalance / dosesPerVial) + 1;
                    }
                    initializeBalanceTextView(currentBalance, vialsUsed, balanceTextView);
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private void initializeBalanceTextView(int currentBalance, int vialsUsed, MaterialEditText balanceTextView) {
        if (balanceTextView != null) {
            balanceTextView.setErrorColor(Color.BLACK);
            if (currentBalance != 0) {
                Typeface typeFace = Typeface.create(balanceTextView.getTypeface(), Typeface.ITALIC);
                balanceTextView.setAccentTypeface(typeFace);
                balanceTextView.setError(currentBalance + " child(ren) vaccinated today. Assuming " + vialsUsed + " vial(s) used.");
            } else {
                balanceTextView.setError("");
            }
        }
    }

    private void stockVialsEnteredInIssuedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_ISSUED)) {
                StockRepository str = VaccinatorApplication.getInstance().stockRepository();
                if (key.equalsIgnoreCase(getString(R.string.vials_issued_key))) {
                    if (balanceTextView == null) {
                        ArrayList<View> views = getFormDataViews();
                        for (int i = 0; i < views.size(); i++) {
                            if (views.get(i) instanceof MaterialEditText && ((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase(getString(R.string.vials_issued_key))) {
                                balanceTextView = (MaterialEditText) views.get(i);
                            }
                        }
                    }
                    String label = "";
                    int currentBalanceVaccineUsed = 0;
                    int newBalance = 0;
                    Date encounterDate = new Date();
                    String wastedVials = ZERO_STRING;
                    String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_ISSUED, "").trim();
                    int existingBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key)) && questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_Issued")) {
                            if (questions.has(getString(R.string.value_key))) {
                                label = questions.getString(getString(R.string.value_key));
                                if (label != null && StringUtils.isNotBlank(label)) {
                                    Date dateTime = JsonFormUtils.formatDate(label, false);
                                    if (dateTime != null) {
                                        encounterDate = dateTime;
                                    }
                                }
                                existingBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                currentBalanceVaccineUsed = str.getVaccineUsedToday(encounterDate.getTime(), checkIfMeasles(vaccineName.toLowerCase()));
                            }

                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_wasted_key))) {
                                if (questions.has(getString(R.string.value_key))) {
                                    if (!StringUtils.isBlank(questions.getString(getString(R.string.value_key)))) {
                                        wastedVials = questions.getString(getString(R.string.value_key));
                                    }
                                } else {
                                    wastedVials = ZERO_STRING;
                                }
                            }

                        }
                    }
                    pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");

                    if (value != null && !StringUtils.isBlank(value)) {
                        newBalance = existingBalance - Integer.parseInt(value) - Integer.parseInt(wastedVials);
                        pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "New balance: " + newBalance);
                    } else {
                        pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");
                    }
                    int vialsUsed = 0;
                    VaccineTypeRepository vaccineTypeRepository = VaccinatorApplication.getInstance().vaccineTypeRepository();
                    int dosesPerVial = vaccineTypeRepository.getDosesPerVial(vaccineName);
                    if (currentBalanceVaccineUsed % dosesPerVial == 0) {
                        vialsUsed = currentBalanceVaccineUsed / dosesPerVial;
                    } else if (currentBalanceVaccineUsed != 0) {
                        vialsUsed = (currentBalanceVaccineUsed / dosesPerVial) + 1;
                    }
                    initializeBalanceTextView(currentBalanceVaccineUsed, vialsUsed, balanceTextView);
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private void stockWastedVialsEnteredInIssuedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_ISSUED)) {
                StockRepository str = VaccinatorApplication.getInstance().stockRepository();
                if (key.equalsIgnoreCase(getString(R.string.vials_wasted_key))) {
                    if (balanceTextView == null) {
                        ArrayList<View> views = getFormDataViews();
                        for (int i = 0; i < views.size(); i++) {
                            if (views.get(i) instanceof MaterialEditText && ((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase(getString(R.string.vials_issued_key))) {
                                balanceTextView = (MaterialEditText) views.get(i);
                            }
                        }
                    }
                    String label = "";
                    int currentBalanceVaccineUsed = 0;
                    int newBalance = 0;
                    Date encounterDate = new Date();
                    String vialsValue = "";
                    String wastedVials = value;
                    String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_ISSUED, "").trim();
                    int existingBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());

                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key))) {
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_Issued") && questions.has(getString(R.string.value_key))) {
                                label = questions.getString(getString(R.string.value_key));
                                if (label != null && StringUtils.isNotBlank(label)) {
                                    Date dateTime = JsonFormUtils.formatDate(label, false);
                                    if (dateTime != null) {
                                        encounterDate = dateTime;
                                    }
                                }

                                existingBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                currentBalanceVaccineUsed = str.getVaccineUsedToday(encounterDate.getTime(), checkIfMeasles(vaccineName.toLowerCase()));
                            }

                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_issued_key))) {
                                if (questions.has(getString(R.string.value_key))) {
                                    if (!StringUtils.isBlank(questions.getString(getString(R.string.value_key)))) {
                                        vialsValue = questions.getString(getString(R.string.value_key));
                                    }
                                } else {
                                    vialsValue = ZERO_STRING;
                                }
                            }
                        }
                    }

                    pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");
                    if (wastedVials == null || StringUtils.isBlank(wastedVials)) {
                        wastedVials = ZERO_STRING;
                    }

                    if (vialsValue != null && !StringUtils.isBlank(vialsValue)) {

                        newBalance = existingBalance - Integer.parseInt(vialsValue) - Integer.parseInt(wastedVials);
                        pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "New balance: " + newBalance);
                    } else {
                        pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");
                    }
                    int vialsUsed = 0;
                    VaccineTypeRepository vaccine_typesRepository = VaccinatorApplication.getInstance().vaccineTypeRepository();
                    int dosesPerVial = vaccine_typesRepository.getDosesPerVial(vaccineName);

                    if (currentBalanceVaccineUsed % dosesPerVial == 0) {
                        vialsUsed = currentBalanceVaccineUsed / dosesPerVial;
                    } else if (currentBalanceVaccineUsed != 0) {
                        vialsUsed = (currentBalanceVaccineUsed / dosesPerVial) + 1;
                    }

                    initializeBalanceTextView(currentBalanceVaccineUsed, vialsUsed, balanceTextView);
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private void stockDateEnteredInReceivedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_RECEIVED)) {
                if (key.equalsIgnoreCase("Date_Stock_Received") && value != null && !value.equalsIgnoreCase("")) {
                    String label = "";
                    int currentBalance = 0;
                    int displayBalance = 0;
                    String vialsValue = "";
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key))) {
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_Received") && questions.has(getString(R.string.value_key))) {
                                Date encounterDate = new Date();
                                label = questions.getString(getString(R.string.value_key));
                                if (label != null && StringUtils.isNotBlank(label)) {
                                    Date dateTime = JsonFormUtils.formatDate(label, false);
                                    if (dateTime != null) {
                                        encounterDate = dateTime;
                                    }
                                }

                                String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_RECEIVED, "").trim();
                                StockRepository str = VaccinatorApplication.getInstance().stockRepository();
                                currentBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                            }

                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_received_key)) && questions.has(getString(R.string.value_key))) {
                                label = questions.getString(getString(R.string.value_key));
                                vialsValue = label;
                            }

                            if (vialsValue != null && !vialsValue.equalsIgnoreCase("")) {
                                displayBalance = currentBalance + Integer.parseInt(vialsValue);
                                pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "New balance: " + displayBalance);

                            } else {
                                pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");

                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }

    }

    private void stockVialsEnteredInReceivedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_RECEIVED)) {
                if (key.equalsIgnoreCase(getString(R.string.vials_received_key)) && value != null && !value.equalsIgnoreCase("")) {
                    String label = "";
                    int currentBalance = 0;
                    int displayBalance = 0;
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key))) {
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_Received") && questions.has(getString(R.string.value_key))) {
                                Date encounterDate = new Date();
                                label = questions.getString(getString(R.string.value_key));
                                if (label != null && StringUtils.isNotBlank(label)) {
                                    Date dateTime = JsonFormUtils.formatDate(label, false);
                                    if (dateTime != null) {
                                        encounterDate = dateTime;
                                    }
                                }

                                String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_RECEIVED, "").trim();
                                StockRepository stockRepository = VaccinatorApplication.getInstance().stockRepository();
                                currentBalance = stockRepository.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());

                            }

                            if (StringUtils.isNotBlank(value)) {
                                displayBalance = currentBalance + Integer.parseInt(value);
                                pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "New balance: " + displayBalance);
                            } else {
                                pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");
                            }
                        }
                    }
                } else {
                    pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private void stockDateEnteredInAdjustmentForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_LOSS_ADJUSTMENT)) {
                if (key.equalsIgnoreCase("Date_Stock_loss_adjustment") && value != null && !value.equalsIgnoreCase("")) {
                    String label = "";
                    int currentBalance = 0;
                    int displayBalance = 0;
                    String vialsValue = "";
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key))) {
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_loss_adjustment") && questions.has(getString(R.string.value_key))) {
                                Date encounterDate = new Date();
                                label = questions.getString(getString(R.string.value_key));
                                if (label != null && StringUtils.isNotBlank(label)) {
                                    Date dateTime = JsonFormUtils.formatDate(label, false);
                                    if (dateTime != null) {
                                        encounterDate = dateTime;
                                    }
                                }

                                String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_LOSS_ADJUSTMENT, "").trim();
                                StockRepository stockRepository = VaccinatorApplication.getInstance().stockRepository();
                                currentBalance = stockRepository.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                            }

                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_adjustment_key)) && questions.has(getString(R.string.value_key))) {
                                label = questions.getString(getString(R.string.value_key));
                                vialsValue = label;
                            }

                            if (vialsValue != null && !vialsValue.equalsIgnoreCase("")) {
                                displayBalance = currentBalance + Integer.parseInt(vialsValue);
                                pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "New balance: " + displayBalance);

                            } else {
                                pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");

                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private void stockVialsEnteredInAdjustmentForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_LOSS_ADJUSTMENT)) {
                if (key.equalsIgnoreCase(getString(R.string.vials_adjustment_key)) && value != null && !value.equalsIgnoreCase("")) {
                    String label = "";
                    int currentBalance = 0;
                    int displayBalance = 0;
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key))) {
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_loss_adjustment") && questions.has(getString(R.string.value_key))) {
                                Date encounterDate = new Date();
                                label = questions.getString(getString(R.string.value_key));
                                if (label != null && StringUtils.isNotBlank(label)) {
                                    Date dateTime = JsonFormUtils.formatDate(label, false);
                                    if (dateTime != null) {
                                        encounterDate = dateTime;
                                    }
                                }

                                String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_LOSS_ADJUSTMENT, "").trim();
                                StockRepository stockRepository = VaccinatorApplication.getInstance().stockRepository();
                                currentBalance = stockRepository.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                            }

                            if (StringUtils.isNotBlank(value) && !value.equalsIgnoreCase("-")) {
                                displayBalance = currentBalance + Integer.parseInt(value);
                                pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "New balance: " + displayBalance);

                            } else {
                                pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");
                            }
                        }
                    }
                } else {
                    pathJsonFormFragment.getLabelViewFromTag(getString(R.string.balance), "");
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private String checkIfMeasles(String vaccineName) {
        if (vaccineName.equalsIgnoreCase("M/MR")) {
            return "measles";
        }
        return vaccineName;
    }

    public boolean checkIfBalanceNegative() {
        boolean balanceCheck = true;
        String balanceString = pathJsonFormFragment.getRelevantTextViewString(getString(R.string.balance));

        if (balanceString.contains("New balance")) {
            int balance = Integer.parseInt(balanceString.replace("New balance:", "").trim());
            if (balance < 0) {
                balanceCheck = false;
            }
        }

        return balanceCheck;
    }

    public boolean checkIfAtLeastOneServiceGiven() {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains("Record out of catchment area service")) {
                JSONArray fields = object.getJSONArray("fields");
                for (int i = 0; i < fields.length(); i++) {
                    JSONObject vaccineGroup = fields.getJSONObject(i);
                    if (vaccineGroup.has(getString(R.string.key)) && vaccineGroup.has(getString(R.string.is_vaccine_group_key))) {
                        if (vaccineGroup.getBoolean(getString(R.string.is_vaccine_group_key)) && vaccineGroup.has(getString(R.string.options_key))) {
                            JSONArray vaccineOptions = vaccineGroup.getJSONArray(getString(R.string.options_key));
                            for (int j = 0; j < vaccineOptions.length(); j++) {
                                JSONObject vaccineOption = vaccineOptions.getJSONObject(j);
                                if (vaccineOption.has(getString(R.string.value_key)) && vaccineOption.getBoolean(getString(R.string.value_key))) {
                                    return true;
                                }
                            }
                        }
                    } else if (vaccineGroup.has(getString(R.string.key)) && vaccineGroup.getString(getString(R.string.key)).equals("Weight_Kg") && vaccineGroup.has(getString(R.string.value_key)) && vaccineGroup.getString(getString(R.string.value_key)).length() > 0) {
                        return true;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }

        return false;
    }
}

