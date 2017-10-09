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
    private MaterialEditText balancetextview;
    private PathJsonFormFragment pathJsonFormFragment;
    private static final String STOCK_ISSUED = "Stock Issued";
    private static final String STOCK_LOSS_ADJUSTMENT = "Stock Loss/Adjustment";
    private static final String STOCK_RECEIVED = "Stock Received";

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
        stockVialsenteredinReceivedForm(key, value);
        stockDateEnteredinReceivedForm(key, value);
        stockDateEnteredinIssuedForm(key, value);
        stockVialsEnteredinIssuedForm(key, value);
        stockWastedVialsEnteredinIssuedForm(key, value);
        stockDateEnteredinAdjustmentForm(key, value);
        stockVialsenteredinAdjustmentForm(key, value);
    }

    private void stockDateEnteredinIssuedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_ISSUED)) {
                StockRepository str = VaccinatorApplication.getInstance().stockRepository();
                if (key.equalsIgnoreCase("Date_Stock_Issued") && value != null && !value.equalsIgnoreCase("")) {
                    if (balancetextview == null) {
                        ArrayList<View> views = getFormDataViews();
                        for (int i = 0; i < views.size(); i++) {
                            if (views.get(i) instanceof MaterialEditText) {
                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase("Vials_Issued")) {
                                    balancetextview = (MaterialEditText) views.get(i);
                                }
                            }
                        }
                    }
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    int existingbalance = 0;
                    int newBalance = 0;
                    Date encounterDate = new Date();
                    String vialsvalue = "";
                    String wastedvials = "0";
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

                                currentBalance = str.getVaccineUsedToday(encounterDate.getTime(), checkifmeasles(vaccineName.toLowerCase()));

                            }

                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_wasted_key))) {
                                if (questions.has(getString(R.string.value_key))) {
                                    if (!StringUtils.isBlank(questions.getString(getString(R.string.value_key)))) {
                                        wastedvials = questions.getString(getString(R.string.value_key));
                                    }
                                } else {
                                    wastedvials = "0";
                                }
                            }
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Vials_Issued")) {
                                if (questions.has(getString(R.string.value_key))) {
                                    if (!StringUtils.isBlank(questions.getString(getString(R.string.value_key)))) {
                                        vialsvalue = questions.getString(getString(R.string.value_key));
                                    }
                                } else {
                                    pathJsonFormFragment.getLabelViewFromTag("Balance", "");
                                }
                            }

                        }
                    }
                    if (!StringUtils.isBlank(vialsvalue)) {
                        newBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime()) - Integer.parseInt(vialsvalue) - Integer.parseInt(wastedvials);
                        pathJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + newBalance);
                    }

                    int DosesPerVial = 0;
                    int vialsused = 0;
                    VaccineTypeRepository vaccineTypeRepository = VaccinatorApplication.getInstance().vaccineTypeRepository();
                    int dosesPerVial = vaccineTypeRepository.getDosesPerVial(vaccineName);
                    if (currentBalance % dosesPerVial == 0) {
                        vialsused = currentBalance / dosesPerVial;
                    } else if (currentBalance != 0) {
                        vialsused = (currentBalance / dosesPerVial) + 1;
                    }
                    initializeBalanceTextView(currentBalance, vialsused, balancetextview);
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

    private void stockVialsEnteredinIssuedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_ISSUED)) {
                StockRepository str = VaccinatorApplication.getInstance().stockRepository();
                if (key.equalsIgnoreCase("Vials_Issued")) {
                    if (balancetextview == null) {
                        ArrayList<View> views = getFormDataViews();
                        for (int i = 0; i < views.size(); i++) {
                            if (views.get(i) instanceof MaterialEditText) {
                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase("Vials_Issued")) {
                                    balancetextview = (MaterialEditText) views.get(i);
                                }
                            }
                        }
                    }
                    String label = "";
                    int currentBalanceVaccineUsed = 0;
                    int displaybalance = 0;
                    int newBalance = 0;
                    Date encounterDate = new Date();
                    String vialsvalue = "";
                    String wastedvials = "0";
                    String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_ISSUED, "").trim();
                    int existingbalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
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
                                existingbalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                currentBalanceVaccineUsed = str.getVaccineUsedToday(encounterDate.getTime(), checkifmeasles(vaccineName.toLowerCase()));
                            }

                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_wasted_key))) {
                                if (questions.has(getString(R.string.value_key))) {
                                    if (!StringUtils.isBlank(questions.getString(getString(R.string.value_key)))) {
                                        wastedvials = questions.getString(getString(R.string.value_key));
                                    }
                                } else {
                                    wastedvials = "0";
                                }
                            }

                        }
                    }
                    pathJsonFormFragment.getLabelViewFromTag("Balance", "");

                    if (value != null && !StringUtils.isBlank(value)) {

                        newBalance = existingbalance - Integer.parseInt(value) - Integer.parseInt(wastedvials);
                        pathJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + newBalance);
                    } else {
                        pathJsonFormFragment.getLabelViewFromTag("Balance", "");
                    }
                    int DosesPerVial = 0;
                    int vialsused = 0;
                    VaccineTypeRepository vaccineTypeRepository = VaccinatorApplication.getInstance().vaccineTypeRepository();
                    int dosesPerVial = vaccineTypeRepository.getDosesPerVial(vaccineName);
                    if (currentBalanceVaccineUsed % dosesPerVial == 0) {
                        vialsused = currentBalanceVaccineUsed / dosesPerVial;
                    } else if (currentBalanceVaccineUsed != 0) {
                        vialsused = (currentBalanceVaccineUsed / dosesPerVial) + 1;
                    }
                    initializeBalanceTextView(currentBalanceVaccineUsed, vialsused, balancetextview);
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private void stockWastedVialsEnteredinIssuedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_ISSUED)) {
                StockRepository str = VaccinatorApplication.getInstance().stockRepository();
                if (key.equalsIgnoreCase(getString(R.string.vials_wasted_key))) {
                    if (balancetextview == null) {
                        ArrayList<View> views = getFormDataViews();
                        for (int i = 0; i < views.size(); i++) {
                            if (views.get(i) instanceof MaterialEditText) {
                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase("Vials_Issued")) {
                                    balancetextview = (MaterialEditText) views.get(i);
                                }
                            }
                        }
                    }
                    String label = "";
                    int currentBalanceVaccineUsed = 0;
                    int displaybalance = 0;
                    int newBalance = 0;
                    Date encounterDate = new Date();
                    String vialsvalue = "";
                    String wastedvials = value;
                    String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_ISSUED, "").trim();
                    int existingbalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());

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

                                existingbalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                currentBalanceVaccineUsed = str.getVaccineUsedToday(encounterDate.getTime(), checkifmeasles(vaccineName.toLowerCase()));
                            }

                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Vials_Issued")) {

                                if (questions.has(getString(R.string.value_key))) {
                                    if (!StringUtils.isBlank(questions.getString(getString(R.string.value_key)))) {
                                        vialsvalue = questions.getString(getString(R.string.value_key));
                                    }
                                } else {
                                    vialsvalue = "0";
                                }
                            }
                        }
                    }
                    pathJsonFormFragment.getLabelViewFromTag("Balance", "");
                    if (wastedvials == null || StringUtils.isBlank(wastedvials)) {
                        wastedvials = "0";
                    }
                    if (vialsvalue != null && !StringUtils.isBlank(vialsvalue)) {

                        newBalance = existingbalance - Integer.parseInt(vialsvalue) - Integer.parseInt(wastedvials);
                        pathJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + newBalance);
                    } else {
                        pathJsonFormFragment.getLabelViewFromTag("Balance", "");
                    }
                    int DosesPerVial = 0;
                    int vialsused = 0;
                    VaccineTypeRepository vaccine_typesRepository = VaccinatorApplication.getInstance().vaccineTypeRepository();
                    int dosesPerVial = vaccine_typesRepository.getDosesPerVial(vaccineName);
                    if (currentBalanceVaccineUsed % dosesPerVial == 0) {
                        vialsused = currentBalanceVaccineUsed / dosesPerVial;
                    } else if (currentBalanceVaccineUsed != 0) {
                        vialsused = (currentBalanceVaccineUsed / dosesPerVial) + 1;
                    }
                    initializeBalanceTextView(currentBalanceVaccineUsed, vialsused, balancetextview);
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private void stockDateEnteredinReceivedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_RECEIVED)) {
                if (key.equalsIgnoreCase("Date_Stock_Received") && value != null && !value.equalsIgnoreCase("")) {
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    String vialsvalue = "";
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
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_received_key))) {
                                if (questions.has(getString(R.string.value_key))) {
                                    label = questions.getString(getString(R.string.value_key));
                                    vialsvalue = label;
                                }
                            }
                            if (vialsvalue != null && !vialsvalue.equalsIgnoreCase("")) {
                                displaybalance = currentBalance + Integer.parseInt(vialsvalue);
                                pathJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + displaybalance);

                            } else {
                                pathJsonFormFragment.getLabelViewFromTag("Balance", "");

                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }

    }

    private void stockVialsenteredinReceivedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_RECEIVED)) {
                if (key.equalsIgnoreCase(getString(R.string.vials_received_key)) && value != null && !value.equalsIgnoreCase("")) {
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key))) {
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_Received")) {
                                if (questions.has(getString(R.string.value_key))) {
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
                            }

                            if (StringUtils.isNotBlank(value)) {
                                displaybalance = currentBalance + Integer.parseInt(value);
                                pathJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + displaybalance);
                            } else {
                                pathJsonFormFragment.getLabelViewFromTag("Balance", "");
                            }
                        }
                    }
                } else {
                    pathJsonFormFragment.getLabelViewFromTag("Balance", "");
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private void stockDateEnteredinAdjustmentForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_LOSS_ADJUSTMENT)) {
                if (key.equalsIgnoreCase("Date_Stock_loss_adjustment") && value != null && !value.equalsIgnoreCase("")) {
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    String vialsvalue = "";
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key))) {
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_loss_adjustment")) {
                                if (questions.has(getString(R.string.value_key))) {
                                    Date encounterDate = new Date();
                                    label = questions.getString(getString(R.string.value_key));
                                    if (label != null && StringUtils.isNotBlank(label)) {
                                        Date dateTime = JsonFormUtils.formatDate(label, false);
                                        if (dateTime != null) {
                                            encounterDate = dateTime;
                                        }
                                    }

                                    String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_LOSS_ADJUSTMENT, "").trim();
                                    StockRepository str = VaccinatorApplication.getInstance().stockRepository();
                                    currentBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                }
                            }
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase(getString(R.string.vials_adjustment_key))) {
                                if (questions.has(getString(R.string.value_key))) {
                                    label = questions.getString(getString(R.string.value_key));
                                    vialsvalue = label;
                                }
                            }
                            if (vialsvalue != null && !vialsvalue.equalsIgnoreCase("")) {
                                displaybalance = currentBalance + Integer.parseInt(vialsvalue);
                                pathJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + displaybalance);

                            } else {
                                pathJsonFormFragment.getLabelViewFromTag("Balance", "");

                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private void stockVialsenteredinAdjustmentForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString(getString(R.string.title_key)).contains(STOCK_LOSS_ADJUSTMENT)) {
                if (key.equalsIgnoreCase(getString(R.string.vials_adjustment_key)) && value != null && !value.equalsIgnoreCase("")) {
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has(getString(R.string.key))) {
                            if (questions.getString(getString(R.string.key)).equalsIgnoreCase("Date_Stock_loss_adjustment")) {
                                if (questions.has(getString(R.string.value_key))) {
                                    Date encounterDate = new Date();
                                    label = questions.getString(getString(R.string.value_key));
                                    if (label != null && StringUtils.isNotBlank(label)) {
                                        Date dateTime = JsonFormUtils.formatDate(label, false);
                                        if (dateTime != null) {
                                            encounterDate = dateTime;
                                        }
                                    }

                                    String vaccineName = object.getString(getString(R.string.title_key)).replace(STOCK_LOSS_ADJUSTMENT, "").trim();
                                    StockRepository str = VaccinatorApplication.getInstance().stockRepository();
                                    currentBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                }
                            }
                            if (StringUtils.isNotBlank(value) && !value.equalsIgnoreCase("-")) {
                                displaybalance = currentBalance + Integer.parseInt(value);
                                pathJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + displaybalance);

                            } else {
                                pathJsonFormFragment.getLabelViewFromTag("Balance", "");
                            }
                        }
                    }
                } else {
                    pathJsonFormFragment.getLabelViewFromTag("Balance", "");
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }
    }

    private String checkifmeasles(String vaccineName) {
        if (vaccineName.equalsIgnoreCase("M/MR")) {
            return "measles";
        }
        return vaccineName;
    }

    public boolean checkIfBalanceNegative() {
        boolean balancecheck = true;
        String balancestring = pathJsonFormFragment.getRelevantTextViewString("Balance");

        if (balancestring.contains("New balance")) {
            int balance = Integer.parseInt(balancestring.replace("New balance:", "").trim());
            if (balance < 0) {
                balancecheck = false;
            }
        }

        return balancecheck;
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
                    } else if (vaccineGroup.has(getString(R.string.key)) && vaccineGroup.getString(getString(R.string.key)).equals("Weight_Kg")
                            && vaccineGroup.has(getString(R.string.value_key)) && vaccineGroup.getString(getString(R.string.value_key)).length() > 0) {
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

