package org.smartregister.path.activity;

import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.api.constants.Gender;
import org.smartregister.Context;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.domain.Photo;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.domain.WeightWrapper;
import org.smartregister.growthmonitoring.fragment.EditWeightDialogFragment;
import org.smartregister.growthmonitoring.listener.WeightActionListener;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceSchedule;
import org.smartregister.immunization.domain.ServiceType;
import org.smartregister.immunization.domain.ServiceWrapper;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.fragment.VaccinationDialogFragment;
import org.smartregister.immunization.listener.ServiceActionListener;
import org.smartregister.immunization.listener.VaccinationActionListener;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.RecurringServiceUtils;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.immunization.view.ImmunizationRowGroup;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.fragment.StatusEditDialogFragment;
import org.smartregister.path.listener.StatusChangeListener;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.path.sync.PathClientProcessor;
import org.smartregister.path.tabfragments.ChildRegistrationDataFragment;
import org.smartregister.path.tabfragments.ChildUnderFiveFragment;
import org.smartregister.path.toolbar.ChildDetailsToolbar;
import org.smartregister.path.view.LocationPickerView;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.DetailsRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.service.AlertService;
import org.smartregister.util.DateUtil;
import org.smartregister.util.FormUtils;
import org.smartregister.util.OpenSRPImageLoader;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.DrishtiApplication;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import util.ImageUtils;
import util.JsonFormUtils;
import util.PathConstants;

import static org.smartregister.util.Utils.getName;
import static org.smartregister.util.Utils.getValue;

/**
 * Created by raihan on 1/03/2017.
 */

public class ChildDetailTabbedActivity extends BaseActivity implements VaccinationActionListener, WeightActionListener, StatusChangeListener, ServiceActionListener {

    private Menu overflow;
    private ChildDetailsToolbar detailtoolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private TextView saveButton;
    private static final int REQUEST_CODE_GET_JSON = 3432;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static Gender gender;
    //////////////////////////////////////////////////
    private static final String TAG = "ChildDetails";
    public static final String EXTRA_CHILD_DETAILS = "child_details";
    public static final String EXTRA_REGISTER_CLICKABLES = "register_clickables";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private ChildRegistrationDataFragment childDataFragment;
    private ChildUnderFiveFragment childUnderFiveFragment;
    public static final String DIALOG_TAG = "ChildDetailActivity_DIALOG_TAG";

    private File currentfile;
    private String location_name = "";

    private ViewPagerAdapter adapter;

    // Data
    private CommonPersonObjectClient childDetails;
    private Map<String, String> detailmaps;
    private AllSharedPreferences allSharedPreferences;
    ////////////////////////////////////////////////
    public DetailsRepository detailsRepository;

    private Map<String, String> details;
    public static final String inactive = "inactive";
    public static final String lostToFollowUp = "lost_to_follow_up";
    public static final String PMTCT_STATUS_LOWER_CASE = "pmtct_status";

    private static final String CHILD = "child";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            Serializable serializable = extras.getSerializable(EXTRA_CHILD_DETAILS);
            if (serializable != null && serializable instanceof CommonPersonObjectClient) {
                childDetails = (CommonPersonObjectClient) serializable;
            }
        }


        location_name = extras.getString("location_name");
        detailsRepository = detailsRepository == null ? this.getOpenSRPContext().detailsRepository() : detailsRepository;
        details = detailsRepository.getAllDetailsForClient(childDetails.entityId());
        util.Utils.putAll(details, childDetails.getColumnmaps());

        setContentView(R.layout.child_detail_activity_simple_tabs);

        childDataFragment = new ChildRegistrationDataFragment();

        childDataFragment.setArguments(this.getIntent().getExtras());

        childUnderFiveFragment = new ChildUnderFiveFragment();
        childUnderFiveFragment.setArguments(this.getIntent().getExtras());


        detailtoolbar = (ChildDetailsToolbar) findViewById(R.id.child_detail_toolbar);


        saveButton = (TextView) detailtoolbar.findViewById(R.id.save);
        saveButton.setVisibility(View.INVISIBLE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detailtoolbar.showOverflowMenu();
                invalidateOptionsMenu();
                childUnderFiveFragment.loadView(false, false, false);

                saveButton.setVisibility(View.INVISIBLE);
            }
        });

        detailtoolbar.showOverflowMenu();

        setSupportActionBar(detailtoolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
//        getSupportActionBar().
        initiallization();
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    saveButton.setVisibility(View.INVISIBLE);
                    invalidateOptionsMenu();
                    childUnderFiveFragment.loadView(false, false, false);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        setupViewPager(viewPager);

        detailtoolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        detailtoolbar.setTitle(updateActivityTitle());

        LinearLayout statusview = (LinearLayout) findViewById(R.id.statusview);
        statusview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                android.app.Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
                if (prev != null) {
                    ft.remove(prev);
                }
                StatusEditDialogFragment.newInstance(details).show(ft, DIALOG_TAG);
            }
        });

        tabLayout.setupWithViewPager(viewPager);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        allSharedPreferences = new AllSharedPreferences(preferences);

    }

    private void initiallization() {

        detailsRepository = detailsRepository == null ? this.getOpenSRPContext().detailsRepository() : detailsRepository;
        detailmaps = detailsRepository.getAllDetailsForClient(childDetails.entityId());
        profileWidget();
        ((TextView) detailtoolbar.findViewById(R.id.title)).setText(updateActivityTitle());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_child_detail_settings, menu);
        overflow = menu;
        VaccineRepository vaccineRepository = getVaccinatorApplicationInstance().vaccineRepository();
        List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

        ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(ChildDetailTabbedActivity.this);

        boolean showVaccineList = false;
        for (int i = 0; i < vaccineList.size(); i++) {
            Vaccine vaccine = vaccineList.get(i);
            boolean check = showVaccineListCheck(vaccine.getEventId(), vaccine.getFormSubmissionId());
            if (check) {
                showVaccineList = true;
                break;
            }
        }

        if (!showVaccineList) {
            overflow.findItem(R.id.immunization_data).setEnabled(false);
        }

        boolean showServiceList = false;

        RecurringServiceRecordRepository recurringServiceRecordRepository = getVaccinatorApplicationInstance().recurringServiceRecordRepository();
        List<ServiceRecord> serviceRecordList = recurringServiceRecordRepository.findByEntityId(childDetails.entityId());
        for (ServiceRecord serviceRecord : serviceRecordList) {
            boolean check = showVaccineListCheck(serviceRecord.getEventId(), serviceRecord.getFormSubmissionId());
            if (check) {
                showServiceList = true;
                break;

            }
        }

        if (!showServiceList) {
            overflow.findItem(R.id.recurring_services_data).setEnabled(false);
        }

        WeightRepository wp = getVaccinatorApplicationInstance().weightRepository();
        List<Weight> weightlist = wp.findLast5(childDetails.entityId());
        boolean showWeightEdit = false;
        for (int i = 0; i < weightlist.size(); i++) {
            Weight weight = weightlist.get(i);
            org.smartregister.domain.db.Event event = null;
            EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();
            if (weight.getEventId() != null) {
                event = ecUpdater.convert(db.getEventsByEventId(weight.getEventId()), org.smartregister.domain.db.Event.class);
            } else if (weight.getFormSubmissionId() != null) {
                event = ecUpdater.convert(db.getEventsByFormSubmissionId(weight.getFormSubmissionId()), org.smartregister.domain.db.Event.class);
            }
            if (event != null) {
                Date weight_create_date = event.getDateCreated().toDate();
                if (!DateUtil.checkIfDateThreeMonthsOlder(weight_create_date)) {
                    showWeightEdit = true;
                }
            } else {
                showWeightEdit = true;
            }
        }

        if (!showWeightEdit) {
            overflow.findItem(R.id.weight_data).setEnabled(false);
        }

        AlertService alertService = getOpenSRPContext().alertService();
        List<Alert> alertList = alertService.findByEntityIdAndAlertNames(childDetails.entityId(),
                VaccinateActionUtils.allAlertNames(CHILD));

        boolean showRecordBcg2 = showRecordBcg2(vaccineList, alertList);
        if (!showRecordBcg2) {
            overflow.findItem(R.id.record_bcg_2).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.registration_data:
                String formmetadata = getmetaDataForEditForm();
                startFormActivity("child_enrollment", childDetails.entityId(), formmetadata);
                // User chose the "Settings" item, show the app settings UI...
                return true;
            case R.id.immunization_data:
                if (viewPager.getCurrentItem() != 1) {
                    viewPager.setCurrentItem(1);
                }
                childUnderFiveFragment.loadView(true, false, false);
                saveButton.setVisibility(View.VISIBLE);
                for (int i = 0; i < overflow.size(); i++) {
                    overflow.getItem(i).setVisible(false);
                }
                return true;

            case R.id.recurring_services_data:
                if (viewPager.getCurrentItem() != 1) {
                    viewPager.setCurrentItem(1);
                }
                childUnderFiveFragment.loadView(false, true, false);
                saveButton.setVisibility(View.VISIBLE);
                for (int i = 0; i < overflow.size(); i++) {
                    overflow.getItem(i).setVisible(false);
                }
                return true;
            case R.id.weight_data:
                if (viewPager.getCurrentItem() != 1) {
                    viewPager.setCurrentItem(1);
                }
                childUnderFiveFragment.loadView(false, false, true);
                saveButton.setVisibility(View.VISIBLE);
                for (int i = 0; i < overflow.size(); i++) {
                    overflow.getItem(i).setVisible(false);
                }
                return true;

            case R.id.report_deceased:
                String reportDeceasedMetadata = getReportDeceasedMetadata();
                startFormActivity("report_deceased", childDetails.entityId(), reportDeceasedMetadata);
                return true;
            case R.id.change_status:
                FragmentTransaction ft = this.getFragmentManager().beginTransaction();
                android.app.Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
                if (prev != null) {
                    ft.remove(prev);
                }
                StatusEditDialogFragment.newInstance(details).show(ft, DIALOG_TAG);
                return true;
            case R.id.report_adverse_event:
                return launchAdverseEventForm();
            case R.id.record_bcg_2:
                showBcg2DialogFragment();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private boolean launchAdverseEventForm() {
        LaunchAdverseEventFormTask task = new LaunchAdverseEventFormTask();
        task.execute();
        return true;
    }

    private String getmetaDataForEditForm() {
        Context context = getOpenSRPContext();
        try {
            JSONObject form = FormUtils.getInstance(getApplicationContext()).getFormJson("child_enrollment");
            LocationPickerView lpv = new LocationPickerView(getApplicationContext());
            lpv.init(context);
            JsonFormUtils.addChildRegLocHierarchyQuestions(form, context);
            Log.d(TAG, "Form is " + form.toString());
            if (form != null) {
                form.put(JsonFormUtils.ENTITY_ID, childDetails.entityId());
                form.put(JsonFormUtils.RELATIONAL_ID, childDetails.getColumnmaps().get("relational_id"));
                form.put(JsonFormUtils.CURRENT_ZEIR_ID, getValue(childDetails.getColumnmaps(), "zeir_id", true).replace("-", ""));


                Intent intent = new Intent(getApplicationContext(), PathJsonFormActivity.class);
                //inject zeir id into the form
                JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("First_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(childDetails.getColumnmaps(), "first_name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Last_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(childDetails.getColumnmaps(), "last_name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Sex")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, getValue(childDetails.getColumnmaps(), "gender", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase(JsonFormUtils.ZEIR_ID)) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, false);
                        jsonObject.put(JsonFormUtils.VALUE, getValue(childDetails.getColumnmaps(), "zeir_id", true).replace("-", ""));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Child_Register_Card_Number")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "Child_Register_Card_Number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Child_Birth_Certificate")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "Child_Birth_Certificate", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_First_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(childDetails.getColumnmaps(), "mother_first_name", true).isEmpty() ? getValue(childDetails.getDetails(), "mother_first_name", true) : getValue(childDetails.getColumnmaps(), "mother_first_name", true));

                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_Last_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(childDetails.getColumnmaps(), "mother_last_name", true).isEmpty() ? getValue(childDetails.getDetails(), "mother_last_name", true) : getValue(childDetails.getColumnmaps(), "mother_last_name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_Date_Birth")) {

                        if (!TextUtils.isEmpty(getValue(childDetails.getColumnmaps(), "mother_dob", true))) {
                            try {
                                DateTime dateTime = new DateTime(getValue(childDetails.getColumnmaps(), "mother_dob", true));
                                Date dob = dateTime.toDate();
                                Date defaultDate = DATE_FORMAT.parse(JsonFormUtils.MOTHER_DEFAULT_DOB);
                                long timeDiff = Math.abs(dob.getTime() - defaultDate.getTime());
                                if (timeDiff > 86400000) { // Mother's date of birth occurs more than a day from the default date
                                    jsonObject.put(JsonFormUtils.VALUE, DATE_FORMAT.format(dob));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                        }
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_NRC")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(childDetails.getColumnmaps(), "mother_nrc_number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Mother_Guardian_Number")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "Mother_Guardian_Number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Father_Guardian_Name")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "Father_Guardian_Name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Father_Guardian_NRC")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "Father_NRC_Number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("First_Health_Facility_Contact")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        String dateString = getValue(detailmaps, "First_Health_Facility_Contact", false);
                        if (!TextUtils.isEmpty(dateString)) {
                            Date date = JsonFormUtils.formatDate(dateString, false);
                            if (date != null) {
                                jsonObject.put(JsonFormUtils.VALUE, DATE_FORMAT.format(date));
                            }
                        }
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Date_Birth")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        DateTime dateTime = new DateTime(getValue(childDetails.getColumnmaps(), "dob", true));
                        Date dob = dateTime.toDate();
                        jsonObject.put(JsonFormUtils.VALUE, DATE_FORMAT.format(dob));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Birth_Weight")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "Birth_Weight", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Place_Birth")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);

                        String placeofnearth_Choice = getValue(detailmaps, "Place_Birth", true);
                        if (placeofnearth_Choice.equalsIgnoreCase("Health facility")) {
                            placeofnearth_Choice = "Health facility";
                        }
                        if (placeofnearth_Choice.equalsIgnoreCase("Home")) {
                            placeofnearth_Choice = "Home";
                        }
                        jsonObject.put(JsonFormUtils.VALUE, placeofnearth_Choice);

//                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "Place_Birth", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Birth_Facility_Name")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        JSONArray birthFacilityHierarchy = null;
                        String birthFacilityName = getValue(detailmaps, "Birth_Facility_Name", false);

                        if (birthFacilityName != null && birthFacilityName.equalsIgnoreCase("other")) {
                            birthFacilityHierarchy = new JSONArray();
                            birthFacilityHierarchy.put(birthFacilityName);
                        } else {
                            String birthFacilityId = JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(), birthFacilityName);
                            birthFacilityHierarchy = JsonFormUtils.getOpenMrsLocationHierarchy(
                                    getOpenSRPContext(), birthFacilityId);
                        }

                        if (birthFacilityHierarchy != null) {
                            jsonObject.put(JsonFormUtils.VALUE, birthFacilityHierarchy.toString());
                        }
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Birth_Facility_Name_Other")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "Birth_Facility_Name_Other", false));
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Residential_Area")) {
                        JSONArray residentialAreaHierarchy = null;
                        String address3 = getValue(detailmaps, "address3", false);
                        if (address3 != null && address3.equalsIgnoreCase("Other")) {
                            residentialAreaHierarchy = new JSONArray();
                            residentialAreaHierarchy.put(address3);
                        } else {
                            residentialAreaHierarchy = JsonFormUtils.getOpenMrsLocationHierarchy(
                                    getOpenSRPContext(), address3);
                        }

                        if (residentialAreaHierarchy != null) {
                            jsonObject.put(JsonFormUtils.VALUE, residentialAreaHierarchy.toString());
                        }
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Residential_Area_Other")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "address5", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Residential_Address")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "address2", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Physical_Landmark")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "address1", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("CHW_Name")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "CHW_Name", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("CHW_Phone_Number")) {
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, "CHW_Phone_Number", true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("PMTCT_Status")) {
                        jsonObject.put(JsonFormUtils.READ_ONLY, true);
                        jsonObject.put(JsonFormUtils.VALUE, getValue(detailmaps, PMTCT_STATUS_LOWER_CASE, true));
                    }
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Home_Facility")) {
                        JSONArray homeFacilityHierarchy = JsonFormUtils.getOpenMrsLocationHierarchy(
                                getOpenSRPContext(), getValue(detailmaps,
                                        "Home_Facility",false));
                        if (homeFacilityHierarchy != null) {
                            jsonObject.put(JsonFormUtils.VALUE, homeFacilityHierarchy.toString());
                        }
                    }

                }
//            intent.putExtra("json", form.toString());
//            startActivityForResult(intent, REQUEST_CODE_GET_JSON);
                return form.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return "";
    }

    private void startFormActivity(String formName, String entityId, String metaData) {

        Intent intent = new Intent(getApplicationContext(), PathJsonFormActivity.class);

        intent.putExtra("json", metaData);
        startActivityForResult(intent, REQUEST_CODE_GET_JSON);


    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            try {
                String jsonString = data.getStringExtra("json");
                Log.d("JSONResult", jsonString);

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);

                JSONObject form = new JSONObject(jsonString);
                if (form.getString("encounter_type").equals("Death")) {
                    confirmReportDeceased(jsonString, allSharedPreferences);
                } else if (form.getString("encounter_type").equals("Birth Registration")) {
                    JsonFormUtils.editsave(this, getOpenSRPContext(), jsonString, allSharedPreferences.fetchRegisteredANM(), "Child_Photo", CHILD, "mother");
                } else if (form.getString("encounter_type").equals("AEFI")) {
                    JsonFormUtils.saveAdverseEvent(jsonString, location_name,
                            childDetails.entityId(), allSharedPreferences.fetchRegisteredANM());
                }
                childDataFragment.childDetails = childDetails;
                childDataFragment.loadData();
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

        } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            String imageLocation = currentfile.getAbsolutePath();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);

            JsonFormUtils.saveImage(this, allSharedPreferences.fetchRegisteredANM(), childDetails.entityId(), imageLocation);
            updateProfilePicture(gender);
        }
    }

    private void saveReportDeceasedJson(String jsonString, AllSharedPreferences allSharedPreferences) {

        JsonFormUtils.saveReportDeceased(this, getOpenSRPContext(), jsonString, allSharedPreferences.fetchRegisteredANM(), location_name, childDetails.entityId());

    }

    private void confirmReportDeceased(final String json, final AllSharedPreferences allSharedPreferences) {

        final AlertDialog builder = new AlertDialog.Builder(this).setCancelable(false).create();

        LayoutInflater inflater = getLayoutInflater();
        View notificationsLayout = inflater.inflate(R.layout.notification_base, null);
        notificationsLayout.setVisibility(View.VISIBLE);

        ImageView notificationIcon = (ImageView) notificationsLayout.findViewById(R.id.noti_icon);
        notificationIcon.setTag("confirm_deceased_icon");
        notificationIcon.setImageResource(R.drawable.ic_deceased);
        notificationIcon.getLayoutParams().height = 165;

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) notificationIcon.getLayoutParams();
        params.setMargins(55, params.topMargin, params.rightMargin, params.bottomMargin);
        notificationIcon.setLayoutParams(params);

        TextView notificationMessage = (TextView) notificationsLayout.findViewById(R.id.noti_message);
        notificationMessage.setText(childDetails.getColumnmaps().get("first_name") + " " + childDetails.getColumnmaps().get("last_name") + " marked as deceased");
        notificationMessage.setTextColor(getResources().getColor(R.color.black));
        notificationMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);

        Button positiveButton = (Button) notificationsLayout.findViewById(R.id.noti_positive_button);
        positiveButton.setVisibility(View.VISIBLE);
        positiveButton.setText(getResources().getString(R.string.undo));
        positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                builder.dismiss();

            }
        });

        Button negativeButton = (Button) notificationsLayout.findViewById(R.id.noti_negative_button);
        negativeButton.setVisibility(View.VISIBLE);
        negativeButton.setText(getResources().getString(R.string.confirm_button_label));
        negativeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveReportDeceasedJson(json, allSharedPreferences);
                builder.dismiss();

                Intent intent = new Intent(getApplicationContext(), ChildSmartRegisterActivity.class);
                intent.putExtra(BaseRegisterActivity.IS_REMOTE_LOGIN, false);
                startActivity(intent);
                finish();

            }
        });

        builder.setView(notificationsLayout);
        builder.show();
    }

    @Override
    protected int getContentView() {
        return R.layout.child_detail_activity_simple_tabs;
    }

    @Override
    protected int getDrawerLayoutId() {
        return R.id.drawer_layout;
    }

    @Override
    protected int getToolbarId() {
        return R.id.child_detail_toolbar;
    }

    @Override
    protected Class onBackActivity() {
        return ChildImmunizationActivity.class;
    }

    private void profileWidget() {
        TextView profilename = (TextView) findViewById(R.id.name);
        TextView profileZeirID = (TextView) findViewById(R.id.idforclient);
        TextView profileage = (TextView) findViewById(R.id.ageforclient);
        String name = "";
        String childId = "";
        String dobString = "";
        String formattedAge = "";
        if (isDataOk()) {
            name = getValue(childDetails.getColumnmaps(), "first_name", true)
                    + " " + getValue(childDetails.getColumnmaps(), "last_name", true);
            childId = getValue(childDetails.getColumnmaps(), "zeir_id", false);
            if (StringUtils.isNotBlank(childId)) {
                childId = childId.replace("-", "");
            }
            dobString = getValue(childDetails.getColumnmaps(), "dob", false);
            if (!TextUtils.isEmpty(dobString)) {
                DateTime dateTime = new DateTime(dobString);
                Date dob = dateTime.toDate();
                long timeDiff = Calendar.getInstance().getTimeInMillis() - dob.getTime();

                if (timeDiff >= 0) {
                    formattedAge = DateUtil.getDuration(timeDiff);
                }
            }
        }

        profileage.setText(String.format("%s: %s", getString(R.string.age), formattedAge));
        profileZeirID.setText(String.format("%s: %s", getString(R.string.label_zeir), childId));
        profilename.setText(name);
        updateGenderViews();
        Gender gender = Gender.UNKNOWN;
        if (isDataOk()) {
            String genderString = getValue(childDetails, "gender", false);
            if (genderString != null && genderString.equalsIgnoreCase(PathConstants.GENDER.FEMALE)) {
                gender = Gender.FEMALE;
            } else if (genderString != null && genderString.equalsIgnoreCase(PathConstants.GENDER.MALE)) {
                gender = Gender.MALE;
            }
        }
        updateProfilePicture(gender);
        updateStatus();
    }

    @Override
    public void updateStatus() {
        ImageView statusImage = (ImageView) findViewById(R.id.statusimage);
        TextView status_name = (TextView) findViewById(R.id.statusname);
        TextView status = (TextView) findViewById(R.id.status);
        if (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
            statusImage.clearColorFilter();
            statusImage.setColorFilter(Color.TRANSPARENT);
            statusImage.setImageResource(R.drawable.ic_icon_status_inactive);
            status_name.setText(R.string.inactive);
            status_name.setTextColor(getResources().getColor(R.color.dark_grey));
            status_name.setVisibility(View.VISIBLE);
            status.setText(R.string.status);
        } else if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
            statusImage.clearColorFilter();
            statusImage.setImageResource(R.drawable.ic_icon_status_losttofollowup);
            statusImage.setColorFilter(Color.TRANSPARENT);
            status_name.setVisibility(View.GONE);
            status.setText(R.string.lost_to_follow_up_with_nl);
        }
        if (!((details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) || (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())))) {
            statusImage.setImageResource(R.drawable.ic_icon_status_active);
            statusImage.setColorFilter(getResources().getColor(R.color.alert_completed));
            status_name.setText(R.string.active);
            status_name.setTextColor(getResources().getColor(R.color.alert_completed));
            status_name.setVisibility(View.VISIBLE);
            status.setText(R.string.status);
        }
    }

    private String updateActivityTitle() {
        String name = "";
        if (isDataOk()) {
            name = getValue(childDetails.getColumnmaps(), "first_name", true)
                    + " " + getValue(childDetails.getColumnmaps(), "last_name", true);
        }
        return String.format("%s's %s", name, "Health Details");
    }

    private void updateProfilePicture(Gender gender) {
        ChildDetailTabbedActivity.gender = gender;
        if (isDataOk()) {
            ImageView profileImageIV = (ImageView) findViewById(R.id.profile_image_iv);

            if (childDetails.entityId() != null) { //image already in local storage most likey ):
                //set profile image by passing the client id.If the image doesn't exist in the image repository then download and save locally
                profileImageIV.setTag(org.smartregister.R.id.entity_id, childDetails.entityId());
                DrishtiApplication.getCachedImageLoaderInstance().getImageByClientId(childDetails.entityId(), OpenSRPImageLoader.getStaticImageListener(profileImageIV, ImageUtils.profileImageResourceByGender(gender), ImageUtils.profileImageResourceByGender(gender)));

            }
            profileImageIV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent();
                }
            });
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.addFragment(childDataFragment, "Registration Data");
        adapter.addFragment(childUnderFiveFragment, "Under Five History");
        viewPager.setAdapter(adapter);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, Log.getStackTraceString(ex));
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                currentfile = photoFile;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
//        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
    }

    private void updateGenderViews() {
        Gender gender = Gender.UNKNOWN;
        if (isDataOk()) {
            String genderString = getValue(childDetails, "gender", false);
            if (genderString != null && genderString.toLowerCase().equals(PathConstants.GENDER.FEMALE)) {
                gender = Gender.FEMALE;
            } else if (genderString != null && genderString.toLowerCase().equals(PathConstants.GENDER.MALE)) {
                gender = Gender.MALE;
            }
        }
        int[] colors = updateGenderViews(gender);
        int darkShade = colors[0];
        int normalShade = colors[1];
        int lightSade = colors[2];
        detailtoolbar.setBackground(new ColorDrawable(getResources().getColor(normalShade)));
        tabLayout.setTabTextColors(getResources().getColor(R.color.dark_grey), getResources().getColor(normalShade));
//        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(normalShade));
        try {
            Field field = TabLayout.class.getDeclaredField("mTabStrip");
            field.setAccessible(true);
            Object ob = field.get(tabLayout);
            Class<?> c = Class.forName("android.support.design.widget.TabLayout$SlidingTabStrip");
            Method method = c.getDeclaredMethod("setSelectedIndicatorColor", int.class);
            method.setAccessible(true);
            method.invoke(ob, getResources().getColor(normalShade)); //now its ok
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private boolean isDataOk() {
        return childDetails != null && childDetails.getDetails() != null;
    }

    @Override
    public void onVaccinateToday(ArrayList<VaccineWrapper> tags, View view) {
        if (tags != null && !tags.isEmpty()) {
            saveVaccine(tags, view);
            Utils.startAsyncTask(new UpdateOfflineAlertsTask(), null);
        }
    }

    @Override
    public void onVaccinateEarlier(ArrayList<VaccineWrapper> tags, View view) {
        if (tags != null && !tags.isEmpty()) {
            saveVaccine(tags, view);
            Utils.startAsyncTask(new UpdateOfflineAlertsTask(), null);
        }
    }

    @Override
    public void onUndoVaccination(VaccineWrapper tag, View view) {
        if (tag != null && tag.getDbKey() != null) {
            final VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
            Long dbKey = tag.getDbKey();
            vaccineRepository.deleteVaccine(dbKey);

            tag.setUpdatedVaccineDate(null, false);
            tag.setDbKey(null);


            List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

            ArrayList<VaccineWrapper> wrappers = new ArrayList<>();
            wrappers.add(tag);
            updateVaccineGroupViews(view, wrappers, vaccineList, true);

            Utils.startAsyncTask(new UpdateOfflineAlertsTask(), null);
        }
    }

    @Override
    public void onWeightTaken(WeightWrapper tag) {
        if (tag != null) {
            WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();
            Weight weight = new Weight();
            if (tag.getDbKey() != null) {
                weight = weightRepository.find(tag.getDbKey());
            }
            weight.setBaseEntityId(childDetails.entityId());
            weight.setKg(tag.getWeight());
            weight.setDate(tag.getUpdatedWeightDate().toDate());
            weight.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
            if (StringUtils.isNotBlank(location_name)) {
                weight.setLocationId(location_name);
            }

            Gender gender = Gender.UNKNOWN;
            String genderString = getValue(childDetails, "gender", false);
            if (genderString != null && genderString.toLowerCase().equals(PathConstants.GENDER.FEMALE)) {
                gender = Gender.FEMALE;
            } else if (genderString != null && genderString.toLowerCase().equals(PathConstants.GENDER.MALE)) {
                gender = Gender.MALE;
            }

            Date dob = null;
            String dobString = getValue(childDetails.getColumnmaps(), "dob", false);
            if (!TextUtils.isEmpty(dobString)) {
                DateTime dateTime = new DateTime(dobString);
                dob = dateTime.toDate();
            }

            if (dob != null && gender != Gender.UNKNOWN) {
                weightRepository.add(dob, gender, weight);
            } else {
                weightRepository.add(weight);
            }

            tag.setDbKey(weight.getId());
            childUnderFiveFragment.loadView(false, false, true);
//            updateRecordWeightView(tag);
//            setLastModified(true);
        } else {
            childUnderFiveFragment.loadView(false, false, false);
        }
    }

    public void showWeightDialog(int i) {
        FragmentTransaction ft = this.getFragmentManager().beginTransaction();
        android.app.Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);


        String childName = constructChildName();
        String gender = getValue(childDetails.getColumnmaps(), "gender", true);
        String motherFirstName = getValue(childDetails.getColumnmaps(), "mother_first_name", true);
        if (StringUtils.isBlank(childName) && StringUtils.isNotBlank(motherFirstName)) {
            childName = "B/o " + motherFirstName.trim();
        }
        String zeirId = getValue(childDetails.getColumnmaps(), "zeir_id", false);
        String duration = "";
        String dobString = getValue(childDetails.getColumnmaps(), "dob", false);
        if (StringUtils.isNotBlank(dobString)) {
            DateTime dateTime = new DateTime(getValue(childDetails.getColumnmaps(), "dob", false));
            duration = DateUtil.getDuration(dateTime);
        }

        Photo photo = getProfilePhotoByClient();

        WeightWrapper weightWrapper = new WeightWrapper();
        weightWrapper.setId(childDetails.entityId());
        WeightRepository wp = getVaccinatorApplicationInstance().weightRepository();
        List<Weight> weightlist = wp.findLast5(childDetails.entityId());
        if (!weightlist.isEmpty()) {
            weightWrapper.setWeight(weightlist.get(i).getKg());
            weightWrapper.setUpdatedWeightDate(new DateTime(weightlist.get(i).getDate()), false);
            weightWrapper.setDbKey(weightlist.get(i).getId());
        }

        weightWrapper.setGender(gender);
        weightWrapper.setPatientName(childName);
        weightWrapper.setPatientNumber(zeirId);
        weightWrapper.setPatientAge(duration);
        weightWrapper.setPhoto(photo);
        weightWrapper.setPmtctStatus(getValue(childDetails.getColumnmaps(), PMTCT_STATUS_LOWER_CASE, false));
        weightWrapper.setDateOfBirth(dobString);

        EditWeightDialogFragment editWeightDialogFragment = EditWeightDialogFragment.newInstance(this, weightWrapper);
        editWeightDialogFragment.show(ft, DIALOG_TAG);

    }

    protected Photo getProfilePhotoByClient() {
        return ImageUtils.profilePhotoByClient(childDetails);
    }


    private String constructChildName() {
        String firstName = getValue(childDetails.getColumnmaps(), "first_name", true);
        String lastName = getValue(childDetails.getColumnmaps(), "last_name", true);
        return getName(firstName, lastName).trim();
    }

    private void saveVaccine(List<VaccineWrapper> tags, final View view) {
        if (tags != null && !tags.isEmpty()) {
            if (tags.size() == 1) {
                saveVaccine(tags.get(0));
                updateVaccineGroupViews(view);
            } else {
                VaccineWrapper[] arrayTags = tags.toArray(new VaccineWrapper[tags.size()]);
                SaveVaccinesTask backgroundTask = new SaveVaccinesTask();
                backgroundTask.setView(view);
                backgroundTask.execute(arrayTags);
            }
        }
    }

    private void updateVaccineGroupViews(View view) {
        if (view == null || !(view instanceof ImmunizationRowGroup)) {
            return;
        }
        final ImmunizationRowGroup vaccineGroup = (ImmunizationRowGroup) view;

        if (Looper.myLooper() == Looper.getMainLooper()) {
            vaccineGroup.updateViews();
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    vaccineGroup.updateViews();
                }
            });
        }
    }

    private void updateVaccineGroupViews(View view, final ArrayList<VaccineWrapper> wrappers, final List<Vaccine> vaccineList, final boolean undo) {
        if (view == null || !(view instanceof ImmunizationRowGroup)) {
            return;
        }
        final ImmunizationRowGroup vaccineGroup = (ImmunizationRowGroup) view;
        vaccineGroup.setModalOpen(false);

        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (undo) {
                vaccineGroup.setVaccineList(vaccineList);
                vaccineGroup.updateWrapperStatus(wrappers);
            }
            vaccineGroup.updateViews(wrappers);

        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (undo) {
                        vaccineGroup.setVaccineList(vaccineList);
                        vaccineGroup.updateWrapperStatus(wrappers);
                    }
                    vaccineGroup.updateViews(wrappers);
                }
            });
        }
    }

    private void saveVaccine(VaccineWrapper tag) {
        VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();

        Vaccine vaccine = new Vaccine();
        if (tag.getDbKey() != null) {
            vaccine = vaccineRepository.find(tag.getDbKey());
        }
        vaccine.setBaseEntityId(childDetails.entityId());
        vaccine.setName(tag.getName());
        vaccine.setDate(tag.getUpdatedVaccineDate().toDate());
        vaccine.setUpdatedAt(tag.getUpdatedVaccineDate().toDate().getTime());
        vaccine.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
        if (StringUtils.isNotBlank(location_name)) {
            vaccine.setLocationId(location_name);
        }

        String lastChar = vaccine.getName().substring(vaccine.getName().length() - 1);
        if (StringUtils.isNumeric(lastChar)) {
            vaccine.setCalculation(Integer.valueOf(lastChar));
        } else {
            vaccine.setCalculation(-1);
        }
        util.Utils.addVaccine(vaccineRepository, vaccine);
        tag.setDbKey(vaccine.getId());

        if (tag.getName().equalsIgnoreCase(VaccineRepo.Vaccine.bcg2.display())) {
            invalidateOptionsMenu();
            childUnderFiveFragment.loadView(false, false, false);
        }
    }

    private String getReportDeceasedMetadata() {
        try {
            JSONObject form = FormUtils.getInstance(getApplicationContext()).getFormJson("report_deceased");
            if (form != null) {
                //inject zeir id into the form
                JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.getString(JsonFormUtils.KEY).equalsIgnoreCase("Date_Birth")) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
                        DateTime dateTime = new DateTime(getValue(childDetails.getColumnmaps(), "dob", true));
                        Date dob = dateTime.toDate();
                        jsonObject.put(JsonFormUtils.VALUE, simpleDateFormat.format(dob));
                        break;
                    }
                }
            }
            return form == null ? null : form.toString();

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return "";
    }

    private boolean insertVaccinesGivenAsOptions(JSONObject question) throws JSONException {
        VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
        JSONObject omrsChoicesTemplate = question.getJSONObject("openmrs_choice_ids");
        JSONObject omrsChoices = new JSONObject();
        JSONArray choices = new JSONArray();
        List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

        boolean ok = false;
        if (vaccineList != null && vaccineList.size() > 0) {
            ok = true;
            for (int i = vaccineList.size() - 1; i >= 0; i--) {
                Vaccine curVaccine = vaccineList.get(i);
                String name = VaccinatorUtils.getVaccineDisplayName(this, curVaccine.getName())
                        + " (" + DATE_FORMAT.format(curVaccine.getDate()) + ")";
                choices.put(name);

                Iterator<String> vaccineGroupNames = omrsChoicesTemplate.keys();
                while (vaccineGroupNames.hasNext()) {
                    String curGroupName = vaccineGroupNames.next();
                    if (curVaccine.getName().toLowerCase().contains(curGroupName.toLowerCase())) {
                        omrsChoices.put(name, omrsChoicesTemplate.getString(curGroupName));
                        break;
                    }
                }
            }
        }

        question.put("values", choices);
        question.put("openmrs_choice_ids", omrsChoices);

        return ok;
    }

    @Override
    public void updateClientAttribute(String attributeName, Object attributeValue) {
        try {
            Date date = new Date();
            EventClientRepository db = getVaccinatorApplicationInstance().eventClientRepository();
            ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(this);

            JSONObject client = db.getClientByBaseEntityId(childDetails.entityId());
            JSONObject attributes = client.getJSONObject(JsonFormUtils.attributes);
            attributes.put(attributeName, attributeValue);
            client.remove(JsonFormUtils.attributes);
            client.put(JsonFormUtils.attributes, attributes);
            db.addorUpdateClient(childDetails.entityId(), client);


            detailsRepository.add(childDetails.entityId(), attributeName, attributeValue.toString(), new Date().getTime());
            ContentValues contentValues = new ContentValues();
            //Add the base_entity_id
            contentValues.put(attributeName.toLowerCase(), attributeValue.toString());
            db.getWritableDatabase().update(PathConstants.CHILD_TABLE_NAME, contentValues, "base_entity_id" + "=?", new String[]{childDetails.entityId()});


            Event event = (Event) new Event()
                    .withBaseEntityId(childDetails.entityId())
                    .withEventDate(new Date())
                    .withEventType(JsonFormUtils.encounterType)
                    .withLocationId(allSharedPreferences.fetchCurrentLocality())
                    .withProviderId(allSharedPreferences.fetchRegisteredANM())
                    .withEntityType(PathConstants.EntityType.CHILD)
                    .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                    .withDateCreated(new Date());

            JsonFormUtils.addMetaData(this, event, date);
            JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(event));
            db.addEvent(childDetails.entityId(), eventJson);
            long lastSyncTimeStamp = allSharedPreferences.fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            PathClientProcessor.getInstance(this).processClient(ecUpdater.getEvents(lastSyncDate, BaseRepository.TYPE_Unsynced));
            allSharedPreferences.saveLastUpdatedAtDate(lastSyncDate.getTime());

            //update details
            details = detailsRepository.getAllDetailsForClient(childDetails.entityId());
            if (childDetails.getColumnmaps().containsKey(attributeName)) {
                childDetails.getColumnmaps().put(attributeName, attributeValue.toString());
            }
            util.Utils.putAll(detailmaps, childDetails.getColumnmaps());


        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        details = detailsRepository.getAllDetailsForClient(childDetails.entityId());
        util.Utils.putAll(childDetails.getColumnmaps(), detailmaps);
        updateActivityTitle();
        initiallization();
    }

    //Recurring Service
    @Override
    public void onGiveToday(ServiceWrapper tag, View view) {
        if (tag != null) {
            saveService(tag, view);
        }
    }

    @Override
    public void onGiveEarlier(ServiceWrapper tag, View view) {
        if (tag != null) {
            saveService(tag, view);
        }
    }

    @Override
    public void onUndoService(ServiceWrapper tag, View view) {
        Utils.startAsyncTask(new UndoServiceTask(tag, view), null);
    }

    private void saveService(ServiceWrapper tag, final View view) {
        if (tag == null) {
            return;
        }

        ServiceWrapper[] arrayTags = {tag};
        SaveServiceTask backgroundTask = new SaveServiceTask();

        backgroundTask.setView(view);
        Utils.startAsyncTask(backgroundTask, arrayTags);
    }

    private boolean showVaccineListCheck(String eventId, String formSubmissionId) {
        ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(ChildDetailTabbedActivity.this);

        EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();
        org.smartregister.domain.db.Event event = null;
        if (eventId != null) {
            event = ecUpdater.convert(db.getEventsByEventId(eventId), org.smartregister.domain.db.Event.class);
        } else if (formSubmissionId != null) {
            event = ecUpdater.convert(db.getEventsByFormSubmissionId(formSubmissionId), org.smartregister.domain.db.Event.class);
        }
        if (event != null) {
            Date vaccine_create_date = event.getDateCreated().toDate();
            if (!DateUtil.checkIfDateThreeMonthsOlder(vaccine_create_date)) {
                return true;
            }
        } else {
            return true;
        }

        return false;
    }


    public VaccinatorApplication getVaccinatorApplicationInstance() {
        return VaccinatorApplication.getInstance();
    }

    public CommonPersonObjectClient getChildDetails() {
        return childDetails;
    }

    public ViewPagerAdapter getViewPagerAdapter() {
        return adapter;
    }

    public DetailsRepository getDetailsRepository() {
        return detailsRepository;
    }


    private boolean showRecordBcg2(List<Vaccine> vaccineList, List<Alert> alerts) {

        if (VaccinateActionUtils.hasVaccine(vaccineList, VaccineRepo.Vaccine.bcg2)) {
            return false;
        }

        Vaccine bcg = VaccinateActionUtils.getVaccine(vaccineList, VaccineRepo.Vaccine.bcg);
        if (bcg == null) {
            return false;
        }

        Alert alert = VaccinateActionUtils.getAlert(alerts, VaccineRepo.Vaccine.bcg2);
        if (alert == null || alert.isComplete()) {
            return false;
        }

        int bcgOffsetInWeeks = 12;
        Calendar twelveWeeksLaterDate = Calendar.getInstance();
        twelveWeeksLaterDate.setTime(bcg.getDate());
        twelveWeeksLaterDate.add(Calendar.WEEK_OF_YEAR, bcgOffsetInWeeks);

        Calendar today = Calendar.getInstance();

        return today.getTime().after(twelveWeeksLaterDate.getTime()) || DateUtils.isSameDay(twelveWeeksLaterDate, today);
    }

    private void showBcg2DialogFragment() {

        VaccineWrapper vaccineWrapper = new VaccineWrapper();
        vaccineWrapper.setId(childDetails.entityId());
        vaccineWrapper.setGender(childDetails.getDetails().get("gender"));
        vaccineWrapper.setName(VaccineRepo.Vaccine.bcg2.display());
        vaccineWrapper.setDefaultName(VaccineRepo.Vaccine.bcg2.display());

        String dobString = getValue(childDetails.getColumnmaps(), "dob", false);
        Date dob = Calendar.getInstance().getTime();
        if (!TextUtils.isEmpty(dobString)) {
            DateTime dateTime = new DateTime(dobString);
            dob = dateTime.toDate();
        }

        Photo photo = org.smartregister.immunization.util.ImageUtils.profilePhotoByClient(childDetails);
        vaccineWrapper.setPhoto(photo);

        String zeirId = getValue(childDetails.getColumnmaps(), "zeir_id", false);
        vaccineWrapper.setPatientNumber(zeirId);

        String firstName = getValue(childDetails.getColumnmaps(), "first_name", true);
        String lastName = getValue(childDetails.getColumnmaps(), "last_name", true);
        String childName = getName(firstName, lastName);
        vaccineWrapper.setPatientName(childName.trim());

        ArrayList<VaccineWrapper> vaccineWrappers = new ArrayList<>();
        vaccineWrappers.add(vaccineWrapper);

        List<Vaccine> vaccineList = VaccinatorApplication.getInstance().vaccineRepository()
                .findByEntityId(childDetails.entityId());
        if (vaccineList == null) {
            vaccineList = new ArrayList<>();
        }

        FragmentTransaction ft = this.getFragmentManager().beginTransaction();
        android.app.Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);

        VaccinationDialogFragment vaccinationDialogFragment = VaccinationDialogFragment.newInstance(dob, vaccineList, vaccineWrappers, true);
        vaccinationDialogFragment.show(ft, DIALOG_TAG);
    }


    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    public class SaveServiceTask extends AsyncTask<ServiceWrapper, Void, Triple<ArrayList<ServiceWrapper>, List<ServiceRecord>, List<Alert>>> {

        private View view;

        public void setView(View view) {
            this.view = view;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected void onPostExecute(Triple<ArrayList<ServiceWrapper>, List<ServiceRecord>, List<Alert>> triple) {
            hideProgressDialog();
            RecurringServiceUtils.updateServiceGroupViews(view, triple.getLeft(), triple.getMiddle(), triple.getRight());
        }

        @Override
        protected Triple<ArrayList<ServiceWrapper>, List<ServiceRecord>, List<Alert>> doInBackground(ServiceWrapper... params) {

            ArrayList<ServiceWrapper> list = new ArrayList<>();

            for (ServiceWrapper tag : params) {
                RecurringServiceUtils.saveService(tag, childDetails.entityId(), null, null);
                list.add(tag);

                ServiceSchedule.updateOfflineAlerts(tag.getType(), childDetails.entityId(), Utils.dobToDateTime(childDetails));
            }

            RecurringServiceRecordRepository recurringServiceRecordRepository = VaccinatorApplication.getInstance().recurringServiceRecordRepository();
            List<ServiceRecord> serviceRecordList = recurringServiceRecordRepository.findByEntityId(childDetails.entityId());

            RecurringServiceTypeRepository recurringServiceTypeRepository = VaccinatorApplication.getInstance().recurringServiceTypeRepository();
            List<ServiceType> serviceTypes = recurringServiceTypeRepository.fetchAll();
            String[] alertArray = VaccinateActionUtils.allAlertNames(serviceTypes);

            AlertService alertService = getOpenSRPContext().alertService();
            List<Alert> alertList = alertService.findByEntityIdAndAlertNames(childDetails.entityId(), alertArray);

            return Triple.of(list, serviceRecordList, alertList);

        }
    }

    private class SaveVaccinesTask extends AsyncTask<VaccineWrapper, Void, Void> {

        private View view;

        public void setView(View view) {
            this.view = view;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            hideProgressDialog();
            updateVaccineGroupViews(view);
        }

        @Override
        protected Void doInBackground(VaccineWrapper... vaccineWrappers) {
            for (VaccineWrapper tag : vaccineWrappers) {
                saveVaccine(tag);
            }
            return null;
        }

    }

    private class UpdateOfflineAlertsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            DateTime birthDateTime = Utils.dobToDateTime(childDetails);
            if (birthDateTime != null) {
                VaccineSchedule.updateOfflineAlerts(childDetails.entityId(), birthDateTime, CHILD);
            }
            return null;
        }

    }

    private class UndoServiceTask extends AsyncTask<Void, Void, Void> {

        private final View view;
        private final ServiceWrapper tag;
        private List<ServiceRecord> serviceRecordList;
        private ArrayList<ServiceWrapper> wrappers;
        private List<Alert> alertList;

        private UndoServiceTask(ServiceWrapper tag, View view) {
            this.tag = tag;
            this.view = view;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(getString(R.string.updating_dialog_title), null);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (tag != null) {

                if (tag.getDbKey() != null) {
                    RecurringServiceRecordRepository recurringServiceRecordRepository = VaccinatorApplication.getInstance().recurringServiceRecordRepository();
                    Long dbKey = tag.getDbKey();
                    recurringServiceRecordRepository.deleteServiceRecord(dbKey);

                    serviceRecordList = recurringServiceRecordRepository.findByEntityId(childDetails.entityId());

                    wrappers = new ArrayList<>();
                    wrappers.add(tag);

                    ServiceSchedule.updateOfflineAlerts(tag.getType(), childDetails.entityId(), Utils.dobToDateTime(childDetails));

                    RecurringServiceTypeRepository recurringServiceTypeRepository = VaccinatorApplication.getInstance().recurringServiceTypeRepository();
                    List<ServiceType> serviceTypes = recurringServiceTypeRepository.fetchAll();
                    String[] alertArray = VaccinateActionUtils.allAlertNames(serviceTypes);

                    AlertService alertService = getOpenSRPContext().alertService();
                    alertList = alertService.findByEntityIdAndAlertNames(childDetails.entityId(), alertArray);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            super.onPostExecute(params);
            hideProgressDialog();

            tag.setUpdatedVaccineDate(null, false);
            tag.setDbKey(null);

            RecurringServiceUtils.updateServiceGroupViews(view, wrappers, serviceRecordList, alertList, true);
        }
    }

    private class LaunchAdverseEventFormTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                JSONObject form = FormUtils.getInstance(getApplicationContext())
                        .getFormJson("adverse_event");
                if (form != null) {
                    JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        if (fields.getJSONObject(i).getString("key").equals("Reaction_Vaccine")) {
                            boolean result = insertVaccinesGivenAsOptions(fields.getJSONObject(i));
                            if (!result) {
                                return null;
                            }
                        }
                    }
                    return form.toString();
                }

            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return null;
        }

        @Override
        protected void onPostExecute(String metaData) {
            super.onPostExecute(metaData);
            if (metaData != null) {
                startFormActivity("adverse_event", childDetails.entityId(), metaData);
            } else {
                Toast.makeText(ChildDetailTabbedActivity.this, R.string.no_vaccine_record_found,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }


        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
