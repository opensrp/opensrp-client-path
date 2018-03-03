package org.smartregister.path.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.api.constants.Gender;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.domain.Photo;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.domain.WeightWrapper;
import org.smartregister.growthmonitoring.fragment.GrowthDialogFragment;
import org.smartregister.growthmonitoring.fragment.RecordWeightDialogFragment;
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
import org.smartregister.immunization.fragment.ServiceDialogFragment;
import org.smartregister.immunization.fragment.UndoServiceDialogFragment;
import org.smartregister.immunization.fragment.UndoVaccinationDialogFragment;
import org.smartregister.immunization.fragment.VaccinationDialogFragment;
import org.smartregister.immunization.listener.ServiceActionListener;
import org.smartregister.immunization.listener.VaccinationActionListener;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.RecurringServiceUtils;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.immunization.view.ServiceGroup;
import org.smartregister.immunization.view.VaccineGroup;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.NamedObject;
import org.smartregister.path.domain.RegisterClickables;
import org.smartregister.path.service.intent.CoverageDropoutIntentService;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;
import org.smartregister.path.view.SiblingPicturesGroup;
import org.smartregister.repository.DetailsRepository;
import org.smartregister.service.AlertService;
import org.smartregister.util.DateUtil;
import org.smartregister.util.OpenSRPImageLoader;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.DrishtiApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import util.ImageUtils;
import util.JsonFormUtils;
import util.PathConstants;


/**
 * Created by Jason Rogena - jrogena@ona.io on 16/02/2017.
 */

public class ChildImmunizationActivity extends BaseActivity
        implements LocationSwitcherToolbar.OnLocationChangeListener, WeightActionListener, VaccinationActionListener, ServiceActionListener {

    private static final String TAG = "ChildImmunoActivity";
    private static final String EXTRA_CHILD_DETAILS = "child_details";
    private static final String EXTRA_REGISTER_CLICKABLES = "register_clickables";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final String DIALOG_TAG = "ChildImmunoActivity_DIALOG_TAG";
    private ArrayList<VaccineGroup> vaccineGroups;
    private ArrayList<ServiceGroup> serviceGroups;
    private static final ArrayList<String> COMBINED_VACCINES;
    private static final HashMap<String, String> COMBINED_VACCINES_MAP;
    private boolean bcgScarNotificationShown;
    private boolean weightNotificationShown;
    private final String BCG2_NOTIFICATION_DONE = "bcg2_not_done";
    private static final int RANDOM_MAX_RANGE = 4232;
    private static final int RANDOM_MIN_RANGE = 213;
    private static final int RECORD_WEIGHT_BUTTON_ACTIVE_MIN = 12;

    static {
        COMBINED_VACCINES = new ArrayList<>();
        COMBINED_VACCINES_MAP = new HashMap<>();
        COMBINED_VACCINES.add("Measles 1");
        COMBINED_VACCINES_MAP.put("Measles 1", "Measles 1 / MR 1");
        COMBINED_VACCINES.add("MR 1");
        COMBINED_VACCINES_MAP.put("MR 1", "Measles 1 / MR 1");
        COMBINED_VACCINES.add("Measles 2");
        COMBINED_VACCINES_MAP.put("Measles 2", "Measles 2 / MR 2");
        COMBINED_VACCINES.add("MR 2");
        COMBINED_VACCINES_MAP.put("MR 2", "Measles 2 / MR 2");
    }

    // Views
    private LocationSwitcherToolbar toolbar;

    // Data
    private CommonPersonObjectClient childDetails;
    private RegisterClickables registerClickables;
    private DetailsRepository detailsRepository;
    private boolean dialogOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        detailsRepository = getOpenSRPContext().detailsRepository();

        toolbar = (LocationSwitcherToolbar) getToolbar();
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildImmunizationActivity.this, ChildSmartRegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
        toolbar.setOnLocationChangeListener(this);
//       View view= toolbar.findViewById(R.id.immunization_separator);
//        view.setBackground(R.drawable.vertical_seperator_female);

        // Get child details from bundled data
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            Serializable serializable = extras.getSerializable(EXTRA_CHILD_DETAILS);
            if (serializable != null && serializable instanceof CommonPersonObjectClient) {
                childDetails = (CommonPersonObjectClient) serializable;
            }

            serializable = extras.getSerializable(EXTRA_REGISTER_CLICKABLES);
            if (serializable != null && serializable instanceof RegisterClickables) {
                registerClickables = (RegisterClickables) serializable;
            }
        }

        bcgScarNotificationShown = false;
        weightNotificationShown = false;

        toolbar.init(this);
        setLastModified(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_CHILD_DETAILS, childDetails);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Serializable serializable = savedInstanceState.getSerializable(EXTRA_CHILD_DETAILS);
        if (serializable != null && serializable instanceof CommonPersonObjectClient) {
            childDetails = (CommonPersonObjectClient) serializable;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vaccineGroups != null) {
            LinearLayout vaccineGroupCanvasLL = (LinearLayout) findViewById(R.id.vaccine_group_canvas_ll);
            vaccineGroupCanvasLL.removeAllViews();
            vaccineGroups = null;
        }

        if (serviceGroups != null) {
            LinearLayout serviceGroupCanvasLL = (LinearLayout) findViewById(R.id.service_group_canvas_ll);
            serviceGroupCanvasLL.removeAllViews();
            serviceGroups = null;
        }
        updateViews();
    }

    private boolean isDataOk() {
        return childDetails != null && childDetails.getDetails() != null;
    }

    private void updateViews() {
        findViewById(R.id.profile_name_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDetailActivity(ChildImmunizationActivity.this, childDetails, null);
            }
        });

        // TODO: update all views using child data
        Map<String, String> details = detailsRepository.getAllDetailsForClient(childDetails.entityId());

        util.Utils.putAll(childDetails.getColumnmaps(), details);

        updateGenderViews();
        toolbar.setTitle(updateActivityTitle());
        updateAgeViews();
        updateChildIdViews();

        WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();

        VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();

        RecurringServiceTypeRepository recurringServiceTypeRepository = VaccinatorApplication.getInstance().recurringServiceTypeRepository();

        RecurringServiceRecordRepository recurringServiceRecordRepository = VaccinatorApplication.getInstance().recurringServiceRecordRepository();

        AlertService alertService = getOpenSRPContext().alertService();

        UpdateViewTask updateViewTask = new UpdateViewTask();
        updateViewTask.setWeightRepository(weightRepository);
        updateViewTask.setVaccineRepository(vaccineRepository);
        updateViewTask.setRecurringServiceTypeRepository(recurringServiceTypeRepository);
        updateViewTask.setRecurringServiceRecordRepository(recurringServiceRecordRepository);
        updateViewTask.setAlertService(alertService);
        Utils.startAsyncTask(updateViewTask, null);
    }

    private void updateProfilePicture(Gender gender) {
        if (isDataOk()) {
            ImageView profileImageIV = (ImageView) findViewById(R.id.profile_image_iv);

            if (childDetails.entityId() != null) { //image already in local storage most likey ):
                //set profile image by passing the client id.If the image doesn't exist in the image repository then download and save locally
                profileImageIV.setTag(org.smartregister.R.id.entity_id, childDetails.entityId());
                DrishtiApplication.getCachedImageLoaderInstance().getImageByClientId(childDetails.entityId(), OpenSRPImageLoader.getStaticImageListener(profileImageIV, ImageUtils.profileImageResourceByGender(gender), ImageUtils.profileImageResourceByGender(gender)));

            }
        }
    }

    private void updateChildIdViews() {
        String name = "";
        String childId = "";
        if (isDataOk()) {
            name = constructChildName();
            childId = Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.ZEIR_ID, false);
        }

        TextView nameTV = (TextView) findViewById(R.id.name_tv);
        nameTV.setText(name);
        TextView childIdTV = (TextView) findViewById(R.id.child_id_tv);
        childIdTV.setText(String.format("%s: %s", getString(R.string.label_zeir), childId));

        Utils.startAsyncTask(new GetSiblingsTask(), null);
    }

    private void updateAgeViews() {
        String dobString = "";
        String formattedAge = "";
        String formattedDob = "";
        if (isDataOk()) {
            dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
            Date dob = util.Utils.dobStringToDate(dobString);
            if (dob != null) {
                formattedDob = DATE_FORMAT.format(dob);
                long timeDiff = Calendar.getInstance().getTimeInMillis() - dob.getTime();

                if (timeDiff >= 0) {
                    formattedAge = DateUtil.getDuration(timeDiff);
                }
            }
        }
        TextView dobTV = (TextView) findViewById(R.id.dob_tv);
        dobTV.setText(String.format("%s: %s", getString(R.string.birthdate), formattedDob));
        TextView ageTV = (TextView) findViewById(R.id.age_tv);
        ageTV.setText(String.format("%s: %s", getString(R.string.age), formattedAge));
    }

    private void updateGenderViews() {
        Gender gender = Gender.UNKNOWN;
        if (isDataOk()) {
            String genderString = Utils.getValue(childDetails, PathConstants.KEY.GENDER, false);
            if (genderString != null && genderString.equalsIgnoreCase(PathConstants.GENDER.FEMALE)) {
                gender = Gender.FEMALE;
            } else if (genderString != null && genderString.equalsIgnoreCase(PathConstants.GENDER.MALE)) {
                gender = Gender.MALE;
            }
        }
        updateGenderViews(gender);
    }

    @Override
    protected int[] updateGenderViews(Gender gender) {
        int[] selectedColor = super.updateGenderViews(gender);

        String identifier = getString(R.string.neutral_sex_id);
        int toolbarResource = 0;
        if (gender.equals(Gender.FEMALE)) {
            toolbarResource = R.drawable.vertical_separator_female;
            identifier = getString(R.string.female_sex_id);
        } else if (gender.equals(Gender.MALE)) {
            toolbarResource = R.drawable.vertical_separator_male;
            identifier = getString(R.string.male_sex_id);
        }
        toolbar.updateSeparatorView(toolbarResource);

        TextView childSiblingsTV = (TextView) findViewById(R.id.child_siblings_tv);
        childSiblingsTV.setText(
                String.format(getString(R.string.child_siblings), identifier).toUpperCase());
        updateProfilePicture(gender);

        return selectedColor;
    }

    private void updateServiceViews(Map<String, List<ServiceType>> serviceTypeMap, List<ServiceRecord> serviceRecordList, List<Alert> alerts) {

        Map<String, List<ServiceType>> foundServiceTypeMap = new LinkedHashMap<>();
        if (serviceGroups == null) {
            for (String type : serviceTypeMap.keySet()) {
                if (foundServiceTypeMap.containsKey(type)) {
                    continue;
                }

                for (ServiceRecord serviceRecord : serviceRecordList) {
                    if (serviceRecord.getSyncStatus().equals(RecurringServiceTypeRepository.TYPE_Unsynced) && serviceRecord.getType().equals(type)) {
                        foundServiceTypeMap.put(type, serviceTypeMap.get(type));
                        break;
                    }
                }

                if (foundServiceTypeMap.containsKey(type)) {
                    continue;
                }

                for (Alert a : alerts) {
                    if (StringUtils.containsIgnoreCase(a.scheduleName(), type)
                            || StringUtils.containsIgnoreCase(a.visitCode(), type)) {
                        foundServiceTypeMap.put(type, serviceTypeMap.get(type));
                        break;
                    }
                }

            }

            if (foundServiceTypeMap.isEmpty()) {
                return;
            }


            serviceGroups = new ArrayList<>();
            LinearLayout serviceGroupCanvasLL = (LinearLayout) findViewById(R.id.service_group_canvas_ll);

            ServiceGroup curGroup = new ServiceGroup(this);
            curGroup.setData(childDetails, foundServiceTypeMap, serviceRecordList, alerts);
            curGroup.setOnServiceClickedListener(new ServiceGroup.OnServiceClickedListener() {
                @Override
                public void onClick(ServiceGroup serviceGroup, ServiceWrapper
                        serviceWrapper) {
                    if (dialogOpen) {
                        return;
                    }
                    dialogOpen = true;
                    addServiceDialogFragment(serviceWrapper, serviceGroup);
                }
            });
            curGroup.setOnServiceUndoClickListener(new ServiceGroup.OnServiceUndoClickListener() {
                @Override
                public void onUndoClick(ServiceGroup serviceGroup, ServiceWrapper serviceWrapper) {
                    if (dialogOpen) {
                        return;
                    }
                    dialogOpen = true;
                    addServiceUndoDialogFragment(serviceGroup, serviceWrapper);
                }
            });
            serviceGroupCanvasLL.addView(curGroup);
            serviceGroups.add(curGroup);
        }

    }

    private void updateVaccinationViews(List<Vaccine> vaccineList, List<Alert> alerts) {

        if (vaccineGroups == null) {
            vaccineGroups = new ArrayList<>();
            String supportedVaccinesString = VaccinatorUtils.getSupportedVaccines(this);

            try {
                JSONArray supportedVaccines = new JSONArray(supportedVaccinesString);

                for (int i = 0; i < supportedVaccines.length(); i++) {
                    JSONObject vaccineGroupObject = supportedVaccines.getJSONObject(i);
                    addVaccineGroup(-1, vaccineGroupObject, vaccineList, alerts);
                }
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        showVaccineNotifications(vaccineList, alerts);
    }

    private void showVaccineNotifications(List<Vaccine> vaccineList, List<Alert> alerts) {

        DetailsRepository detailsRepository = VaccinatorApplication.getInstance().context().detailsRepository();
        Map<String, String> details = detailsRepository.getAllDetailsForClient(childDetails.entityId());

        if (details.containsKey(BCG2_NOTIFICATION_DONE)) {
            return;
        }

        if (VaccinateActionUtils.hasVaccine(vaccineList, VaccineRepo.Vaccine.bcg2)) {
            return;
        }

        Vaccine bcg = VaccinateActionUtils.getVaccine(vaccineList, VaccineRepo.Vaccine.bcg);
        if (bcg == null) {
            return;
        }

        Alert alert = VaccinateActionUtils.getAlert(alerts, VaccineRepo.Vaccine.bcg2);
        if (alert == null || alert.isComplete()) {
            return;
        }

        int bcgOffsetInWeeks = 12;
        Calendar twelveWeeksLaterDate = Calendar.getInstance();
        twelveWeeksLaterDate.setTime(bcg.getDate());
        twelveWeeksLaterDate.add(Calendar.WEEK_OF_YEAR, bcgOffsetInWeeks);

        Calendar today = Calendar.getInstance();

        if (today.getTime().after(twelveWeeksLaterDate.getTime()) || DateUtils.isSameDay(twelveWeeksLaterDate, today)) {
            showCheckBcgScarNotification(alert);
        }
    }

    private void addVaccineGroup(int canvasId, JSONObject vaccineGroupData, List<Vaccine> vaccineList, List<Alert> alerts) {
        LinearLayout vaccineGroupCanvasLL = (LinearLayout) findViewById(R.id.vaccine_group_canvas_ll);
        VaccineGroup curGroup = new VaccineGroup(this);
        curGroup.setData(vaccineGroupData, childDetails, vaccineList, alerts, PathConstants.KEY.CHILD);
        curGroup.setOnRecordAllClickListener(new VaccineGroup.OnRecordAllClickListener() {
            @Override
            public void onClick(VaccineGroup vaccineGroup, ArrayList<VaccineWrapper> dueVaccines) {
                if (dialogOpen) {
                    return;
                }
                dialogOpen = true;
                addVaccinationDialogFragment(dueVaccines, vaccineGroup);
            }
        });
        curGroup.setOnVaccineClickedListener(new VaccineGroup.OnVaccineClickedListener() {
            @Override
            public void onClick(VaccineGroup vaccineGroup, VaccineWrapper vaccine) {
                if (dialogOpen) {
                    return;
                }
                dialogOpen = true;
                ArrayList<VaccineWrapper> vaccineWrappers = new ArrayList<>();
                vaccineWrappers.add(vaccine);
                addVaccinationDialogFragment(vaccineWrappers, vaccineGroup);
            }
        });
        curGroup.setOnVaccineUndoClickListener(new VaccineGroup.OnVaccineUndoClickListener() {
            @Override
            public void onUndoClick(VaccineGroup vaccineGroup, VaccineWrapper vaccine) {
                if (dialogOpen) {
                    return;
                }

                dialogOpen = true;
                addVaccineUndoDialogFragment(vaccineGroup, vaccine);
            }
        });

        LinearLayout parent;
        int groupParentId = canvasId;
        if (groupParentId == -1) {
            Random r = new Random();
            groupParentId = r.nextInt(RANDOM_MAX_RANGE - RANDOM_MIN_RANGE) + RANDOM_MIN_RANGE;
            parent = new LinearLayout(this);
            parent.setId(groupParentId);
            vaccineGroupCanvasLL.addView(parent);
        } else {
            parent = (LinearLayout) findViewById(groupParentId);
            parent.removeAllViews();
        }
        parent.addView(curGroup);
        curGroup.setTag(R.id.vaccine_group_vaccine_data, vaccineGroupData.toString());
        curGroup.setTag(R.id.vaccine_group_parent_id, String.valueOf(groupParentId));
        vaccineGroups.add(curGroup);
    }

    private void addVaccineUndoDialogFragment(VaccineGroup vaccineGroup, VaccineWrapper vaccineWrapper) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);
        vaccineGroup.setModalOpen(true);

        UndoVaccinationDialogFragment undoVaccinationDialogFragment = UndoVaccinationDialogFragment.newInstance(vaccineWrapper);
        undoVaccinationDialogFragment.show(ft, DIALOG_TAG);
        undoVaccinationDialogFragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogOpen = false;
            }
        });
    }

    private void addServiceUndoDialogFragment(ServiceGroup serviceGroup, ServiceWrapper serviceWrapper) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);
        serviceGroup.setModalOpen(true);

        UndoServiceDialogFragment undoServiceDialogFragment = UndoServiceDialogFragment.newInstance(serviceWrapper);
        undoServiceDialogFragment.show(ft, DIALOG_TAG);
        undoServiceDialogFragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogOpen = false;
            }
        });
    }

    private void updateWeightViews(Weight lastUnsyncedWeight) {

        String childName = constructChildName();
        String gender = Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.GENDER, true);
        String motherFirstName = Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.MOTHER_FIRST_NAME, true);
        if (StringUtils.isBlank(childName) && StringUtils.isNotBlank(motherFirstName)) {
            childName = "B/o " + motherFirstName.trim();
        }

        String zeirId = Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.ZEIR_ID, false);
        String duration = "";
        String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
        DateTime dateTime = util.Utils.dobStringToDateTime(dobString);
        if (dateTime != null) {
            duration = DateUtil.getDuration(dateTime);
        }

        Photo photo = ImageUtils.profilePhotoByClient(childDetails);

        WeightWrapper weightWrapper = new WeightWrapper();
        weightWrapper.setId(childDetails.entityId());
        weightWrapper.setGender(gender);
        weightWrapper.setPatientName(childName);
        weightWrapper.setPatientNumber(zeirId);
        weightWrapper.setPatientAge(duration);
        weightWrapper.setPhoto(photo);
        weightWrapper.setPmtctStatus(Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.PMTCT_STATUS, false));

        if (lastUnsyncedWeight != null) {
            weightWrapper.setWeight(lastUnsyncedWeight.getKg());
            weightWrapper.setDbKey(lastUnsyncedWeight.getId());
            weightWrapper.setUpdatedWeightDate(new DateTime(lastUnsyncedWeight.getDate()), false);
        }

        updateRecordWeightViews(weightWrapper);

        ImageButton growthChartButton = (ImageButton) findViewById(R.id.growth_chart_button);
        growthChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.startAsyncTask(new ShowGrowthChartTask(), null);
            }
        });
    }

    private void updateRecordWeightViews(WeightWrapper weightWrapper) {
        View recordWeight = findViewById(R.id.record_weight);
        recordWeight.setClickable(true);
        recordWeight.setBackground(getResources().getDrawable(R.drawable.record_weight_bg));

        TextView recordWeightText = (TextView) findViewById(R.id.record_weight_text);
        recordWeightText.setText(R.string.record_weight);

        ImageView recordWeightCheck = (ImageView) findViewById(R.id.record_weight_check);
        recordWeightCheck.setVisibility(View.GONE);
        recordWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWeightDialog(view);
            }
        });

        if (weightWrapper.getDbKey() != null && weightWrapper.getWeight() != null) {
            recordWeightText.setText(Utils.kgStringSuffix(weightWrapper.getWeight()));
            recordWeightCheck.setVisibility(View.VISIBLE);

            if (weightWrapper.getUpdatedWeightDate() != null) {
                long timeDiff = Calendar.getInstance().getTimeInMillis() - weightWrapper.getUpdatedWeightDate().getMillis();

                if (timeDiff <= TimeUnit.MILLISECONDS.convert(RECORD_WEIGHT_BUTTON_ACTIVE_MIN, TimeUnit.HOURS)) {
                    //disable the button
                    recordWeight.setClickable(false);
                    recordWeight.setBackground(new ColorDrawable(getResources()
                            .getColor(android.R.color.transparent)));
                } else {
                    //reset state
                    weightWrapper.setWeight(null);
                    weightWrapper.setDbKey(null);
                    recordWeight.setClickable(true);
                    recordWeight.setBackground(getResources().getDrawable(R.drawable.record_weight_bg));
                    recordWeightText.setText(R.string.record_weight);
                    recordWeightCheck.setVisibility(View.GONE);
                }
            }
        }

        recordWeight.setTag(weightWrapper);

    }

    private void showWeightDialog(View view) {
        FragmentTransaction ft = this.getFragmentManager().beginTransaction();
        Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            Log.d("duplicate fragments", "duplicate weight fragments exist!");
            return;
        }
        ft.addToBackStack(null);

        String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
        Date dob = util.Utils.dobStringToDate(dobString);
        if (dob == null) {
            dob = Calendar.getInstance().getTime();
        }

        WeightWrapper weightWrapper = (WeightWrapper) view.getTag();
        RecordWeightDialogFragment recordWeightDialogFragment = RecordWeightDialogFragment.newInstance(dob, weightWrapper);
        recordWeightDialogFragment.show(ft, DIALOG_TAG);

    }

    private String readAssetContents(String path) {
        String fileContents = null;
        try {
            InputStream is = getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            fileContents = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e(TAG, ex.toString(), ex);
        }

        return fileContents;
    }

    public static void launchActivity(Context fromContext, CommonPersonObjectClient childDetails, RegisterClickables registerClickables) {
        Intent intent = new Intent(fromContext, ChildImmunizationActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_CHILD_DETAILS, childDetails);
        bundle.putSerializable(EXTRA_REGISTER_CLICKABLES, registerClickables);
        intent.putExtras(bundle);

        fromContext.startActivity(intent);
    }

    private void launchDetailActivity(Context fromContext, CommonPersonObjectClient childDetails, RegisterClickables registerClickables) {
        Intent intent = new Intent(fromContext, ChildDetailTabbedActivity.class);
        Bundle bundle = new Bundle();
        try {
            bundle.putString(PathConstants.KEY.LOCATION_NAME, JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(), toolbar.getCurrentLocation()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        bundle.putSerializable(EXTRA_CHILD_DETAILS, childDetails);
        bundle.putSerializable(EXTRA_REGISTER_CLICKABLES, registerClickables);
        intent.putExtras(bundle);

        fromContext.startActivity(intent);
    }

    private String updateActivityTitle() {
        String name = "";
        if (isDataOk()) {
            name = constructChildName();
        }
        return String.format("%s > %s", getString(R.string.app_name), name.trim());
    }

    @Override
    public void onLocationChanged(final String newLocation) {
        // TODO: Do whatever needs to be done when the location is changed
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_child_immunization;
    }

    @Override
    protected int getDrawerLayoutId() {
        return R.id.drawer_layout;
    }

    @Override
    protected int getToolbarId() {
        return LocationSwitcherToolbar.TOOLBAR_ID;
    }

    @Override
    protected Class onBackActivity() {
        return ChildSmartRegisterActivity.class;
    }

    @Override
    public void onWeightTaken(WeightWrapper tag) {
        if (tag != null) {
            final WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();
            Weight weight = new Weight();
            if (tag.getDbKey() != null) {
                weight = weightRepository.find(tag.getDbKey());
            }
            weight.setBaseEntityId(childDetails.entityId());
            weight.setKg(tag.getWeight());
            weight.setDate(tag.getUpdatedWeightDate().toDate());
            weight.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
            try {
                weight.setLocationId(JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(),
                        toolbar.getCurrentLocation()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Gender gender = Gender.UNKNOWN;
            String genderString = Utils.getValue(childDetails, PathConstants.KEY.GENDER, false);
            if (genderString != null && genderString.toLowerCase().equals(PathConstants.GENDER.FEMALE)) {
                gender = Gender.FEMALE;
            } else if (genderString != null && genderString.toLowerCase().equals(PathConstants.GENDER.MALE)) {
                gender = Gender.MALE;
            }

            String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
            Date dob = util.Utils.dobStringToDate(dobString);

            if (dob != null && gender != Gender.UNKNOWN) {
                weightRepository.add(dob, gender, weight);
            } else {
                weightRepository.add(weight);
            }

            tag.setDbKey(weight.getId());
            updateRecordWeightViews(tag);
            setLastModified(true);
        }
    }

    @Override
    public void onVaccinateToday(ArrayList<VaccineWrapper> tags, View v) {
        if (tags != null && !tags.isEmpty()) {
            View view = getLastOpenedView();
            saveVaccine(tags, view);
        }
    }

    @Override
    public void onVaccinateEarlier(ArrayList<VaccineWrapper> tags, View v) {
        if (tags != null && !tags.isEmpty()) {
            View view = getLastOpenedView();
            saveVaccine(tags, view);
        }
    }

    @Override
    public void onUndoVaccination(VaccineWrapper tag, View v) {
        Utils.startAsyncTask(new UndoVaccineTask(tag, v), null);
    }

    private void addVaccinationDialogFragment(ArrayList<VaccineWrapper> vaccineWrappers, VaccineGroup vaccineGroup) {

        FragmentTransaction ft = this.getFragmentManager().beginTransaction();
        Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);
        vaccineGroup.setModalOpen(true);
        String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
        Date dob = util.Utils.dobStringToDate(dobString);
        if (dob == null) {
            dob = Calendar.getInstance().getTime();
        }

        List<Vaccine> vaccineList = VaccinatorApplication.getInstance().vaccineRepository()
                .findByEntityId(childDetails.entityId());
        if (vaccineList == null) vaccineList = new ArrayList<>();

        VaccinationDialogFragment vaccinationDialogFragment = VaccinationDialogFragment.newInstance(dob, vaccineList, vaccineWrappers, true);
        vaccinationDialogFragment.show(ft, DIALOG_TAG);
        vaccinationDialogFragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogOpen = false;
            }
        });

    }

    private void addServiceDialogFragment(ServiceWrapper serviceWrapper, ServiceGroup serviceGroup) {

        FragmentTransaction ft = this.getFragmentManager().beginTransaction();
        Fragment prev = this.getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);
        serviceGroup.setModalOpen(true);

        String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
        DateTime dob = util.Utils.dobStringToDateTime(dobString);
        if (dob == null) {
            dob = DateTime.now();
        }

        List<ServiceRecord> serviceRecordList = VaccinatorApplication.getInstance().recurringServiceRecordRepository()
                .findByEntityId(childDetails.entityId());

        ServiceDialogFragment serviceDialogFragment = ServiceDialogFragment.newInstance(dob, serviceRecordList, serviceWrapper, true);
        serviceDialogFragment.show(ft, DIALOG_TAG);
        serviceDialogFragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogOpen = false;
            }
        });
    }

    private void performRegisterActions() {
        if (registerClickables != null) {
            if (registerClickables.isRecordWeight()) {
                final View recordWeight = findViewById(R.id.record_weight);
                recordWeight.post(new Runnable() {
                    @Override
                    public void run() {
                        recordWeight.performClick();
                    }
                });
            } else if (registerClickables.isRecordAll()) {
                performRecordAllClick(0);
            }

            //Reset register actions
            registerClickables.setRecordAll(false);
            registerClickables.setRecordWeight(false);
        }
    }

    private void performRecordAllClick(final int index) {
        if (vaccineGroups != null && vaccineGroups.size() > index) {
            final VaccineGroup vaccineGroup = vaccineGroups.get(index);
            vaccineGroup.post(new Runnable() {
                @Override
                public void run() {
                    ArrayList<VaccineWrapper> vaccineWrappers = vaccineGroup.getDueVaccines();
                    if (!vaccineWrappers.isEmpty()) {
                        final TextView recordAllTV = (TextView) vaccineGroup.findViewById(R.id.record_all_tv);
                        recordAllTV.post(new Runnable() {
                            @Override
                            public void run() {
                                recordAllTV.performClick();
                            }
                        });
                    } else {
                        performRecordAllClick(index + 1);
                    }
                }
            });
        }
    }

    private void saveVaccine(ArrayList<VaccineWrapper> tags, final View view) {
        if (tags.isEmpty()) {
            return;
        }

        VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();

        VaccineWrapper[] arrayTags = tags.toArray(new VaccineWrapper[tags.size()]);
        SaveVaccinesTask backgroundTask = new SaveVaccinesTask();
        backgroundTask.setVaccineRepository(vaccineRepository);
        backgroundTask.setView(view);
        Utils.startAsyncTask(backgroundTask, arrayTags);

    }

    private void saveVaccine(VaccineRepository vaccineRepository, VaccineWrapper tag) {
        if (tag.getUpdatedVaccineDate() == null) {
            return;
        }


        Vaccine vaccine = new Vaccine();
        if (tag.getDbKey() != null) {
            vaccine = vaccineRepository.find(tag.getDbKey());
        }
        vaccine.setBaseEntityId(childDetails.entityId());
        vaccine.setName(tag.getName());
        vaccine.setDate(tag.getUpdatedVaccineDate().toDate());
        vaccine.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
        try {
            vaccine.setLocationId(JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(),
                    toolbar.getCurrentLocation()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String lastChar = vaccine.getName().substring(vaccine.getName().length() - 1);
        if (StringUtils.isNumeric(lastChar)) {
            vaccine.setCalculation(Integer.valueOf(lastChar));
        } else {
            vaccine.setCalculation(-1);
        }
        util.Utils.addVaccine(vaccineRepository, vaccine);
        tag.setDbKey(vaccine.getId());
        setLastModified(true);
    }

    private void updateVaccineGroupViews(View view, final ArrayList<VaccineWrapper> wrappers, List<Vaccine> vaccineList) {
        updateVaccineGroupViews(view, wrappers, vaccineList, false);
    }

    private void updateVaccineGroupViews(View view, final ArrayList<VaccineWrapper> wrappers, final List<Vaccine> vaccineList, final boolean undo) {
        if (view == null || !(view instanceof VaccineGroup)) {
            return;
        }
        final VaccineGroup vaccineGroup = (VaccineGroup) view;
        vaccineGroup.setModalOpen(false);

        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (undo) {
                vaccineGroup.setVaccineList(vaccineList);
                vaccineGroup.updateWrapperStatus(wrappers, PathConstants.KEY.CHILD);
            }
            vaccineGroup.updateViews(wrappers);

        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (undo) {
                        vaccineGroup.setVaccineList(vaccineList);
                        vaccineGroup.updateWrapperStatus(wrappers, PathConstants.KEY.CHILD);
                    }
                    vaccineGroup.updateViews(wrappers);
                }
            });
        }
    }

    private void showRecordWeightNotification() {
        if (!weightNotificationShown) {
            weightNotificationShown = true;
            showNotification(R.string.record_weight_notification, R.drawable.ic_weight_notification,
                    R.string.record_weight,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            View recordWeight = findViewById(R.id.record_weight);
                            showWeightDialog(recordWeight);
                            hideNotification();
                        }
                    }, R.string.cancel, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideNotification();
                        }
                    }, null);
        }
    }

    private void showCheckBcgScarNotification(Alert alert) {
        if (!bcgScarNotificationShown) {
            bcgScarNotificationShown = true;
            showNotification(R.string.check_child_bcg_scar, R.drawable.ic_check_bcg_scar,
                    R.string.ok_button_label, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideNotification();
                            Alert alert = (Alert) v.getTag();
                            if (alert != null) {
                                Utils.startAsyncTask(new MarkBcgTwoAsDoneTask(), null);
                            }
                        }
                    }, 0, null, alert);
        }
    }

    private String constructChildName() {
        String firstName = Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.FIRST_NAME, true);
        String lastName = Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.LAST_NAME, true);
        return Utils.getName(firstName, lastName).trim();
    }

    @Override
    public void finish() {
        if (isLastModified()) {
            String tableName = PathConstants.CHILD_TABLE_NAME;
            AllCommonsRepository allCommonsRepository = getOpenSRPContext().allCommonsRepositoryobjects(tableName);
            ContentValues contentValues = new ContentValues();
            contentValues.put(PathConstants.KEY.LAST_INTERACTED_WITH, (new Date()).getTime());
            allCommonsRepository.update(tableName, contentValues, childDetails.entityId());
            allCommonsRepository.updateSearch(childDetails.entityId());
        }
        super.finish();
    }

    private boolean isLastModified() {
        VaccinatorApplication application = (VaccinatorApplication) getApplication();
        return application.isLastModified();
    }

    private void setLastModified(boolean lastModified) {
        VaccinatorApplication application = (VaccinatorApplication) getApplication();
        if (lastModified != application.isLastModified()) {
            application.setLastModified(lastModified);
        }
    }

    private VaccineGroup getLastOpenedView() {
        if (vaccineGroups == null) {
            return null;
        }

        for (VaccineGroup vaccineGroup : vaccineGroups) {
            if (vaccineGroup.isModalOpen()) {
                return vaccineGroup;
            }
        }

        return null;
    }

    private void updateVaccineGroupsUsingAlerts(List<String> affectedVaccines, List<Vaccine> vaccineList, List<Alert> alerts) {
        if (affectedVaccines != null && vaccineList != null) {
            // Update all other affected vaccine groups
            HashMap<VaccineGroup, ArrayList<VaccineWrapper>> affectedGroups = new HashMap<>();
            for (String curAffectedVaccineName : affectedVaccines) {
                boolean viewFound = false;
                // Check what group it is in
                for (VaccineGroup curGroup : vaccineGroups) {
                    ArrayList<VaccineWrapper> groupWrappers = curGroup.getAllVaccineWrappers();
                    if (groupWrappers == null) groupWrappers = new ArrayList<>();
                    for (VaccineWrapper curWrapper : groupWrappers) {
                        String curWrapperName = curWrapper.getName();

                        // Check if current wrapper is one of the combined vaccines
                        if (COMBINED_VACCINES.contains(curWrapperName)) {
                            // Check if any of the sister vaccines is currAffectedVaccineName
                            String[] allSisters = COMBINED_VACCINES_MAP.get(curWrapperName).split(" / ");
                            for (String allSister : allSisters) {
                                if (allSister.replace(" ", "").equalsIgnoreCase(curAffectedVaccineName.replace(" ", ""))) {
                                    curWrapperName = allSister;
                                    break;
                                }
                            }
                        }

                        if (curWrapperName.replace(" ", "").toLowerCase()
                                .contains(curAffectedVaccineName.replace(" ", "").toLowerCase())) {
                            if (!affectedGroups.containsKey(curGroup)) {
                                affectedGroups.put(curGroup, new ArrayList<VaccineWrapper>());
                            }

                            affectedGroups.get(curGroup).add(curWrapper);
                            viewFound = true;
                        }

                        if (viewFound) break;
                    }

                    if (viewFound) break;
                }
            }

            for (VaccineGroup curGroup : affectedGroups.keySet()) {
                try {
                    vaccineGroups.remove(curGroup);
                    addVaccineGroup(Integer.valueOf((String) curGroup.getTag(R.id.vaccine_group_parent_id)),
                            new JSONObject((String) curGroup.getTag(R.id.vaccine_group_vaccine_data)),
                            vaccineList, alerts);
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }
    }

    //Recurring Service
    @Override
    public void onGiveToday(ServiceWrapper tag, View v) {
        if (tag != null) {
            View view = RecurringServiceUtils.getLastOpenedServiceView(serviceGroups);
            saveService(tag, view);
        }
    }

    @Override
    public void onGiveEarlier(ServiceWrapper tag, View v) {
        if (tag != null) {
            View view = RecurringServiceUtils.getLastOpenedServiceView(serviceGroups);
            saveService(tag, view);
        }
    }

    @Override
    public void onUndoService(ServiceWrapper tag, View v) {
        Utils.startAsyncTask(new UndoServiceTask(tag), null);
    }

    private void saveService(ServiceWrapper tag, final View view) {
        if (tag == null) {
            return;
        }

        ServiceWrapper[] arrayTags = {tag};
        SaveServiceTask backgroundTask = new SaveServiceTask();
        String providerId = getOpenSRPContext().allSharedPreferences().fetchRegisteredANM();
        String locationId = null;

        try {
            locationId = JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(),
                    toolbar.getCurrentLocation());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        backgroundTask.setProviderId(providerId);
        backgroundTask.setLocationId(locationId);
        backgroundTask.setView(view);
        Utils.startAsyncTask(backgroundTask, arrayTags);
    }


    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private class UpdateViewTask extends AsyncTask<Void, Void, Map<String, NamedObject<?>>> {

        private VaccineRepository vaccineRepository;
        private WeightRepository weightRepository;
        private RecurringServiceTypeRepository recurringServiceTypeRepository;
        private RecurringServiceRecordRepository recurringServiceRecordRepository;
        private AlertService alertService;

        public void setVaccineRepository(VaccineRepository vaccineRepository) {
            this.vaccineRepository = vaccineRepository;
        }

        public void setWeightRepository(WeightRepository weightRepository) {
            this.weightRepository = weightRepository;
        }

        public void setRecurringServiceTypeRepository(RecurringServiceTypeRepository recurringServiceTypeRepository) {
            this.recurringServiceTypeRepository = recurringServiceTypeRepository;
        }

        public void setRecurringServiceRecordRepository(RecurringServiceRecordRepository recurringServiceRecordRepository) {
            this.recurringServiceRecordRepository = recurringServiceRecordRepository;
        }

        public void setAlertService(AlertService alertService) {
            this.alertService = alertService;
        }


        @Override
        protected void onPreExecute() {
            showProgressDialog(getString(R.string.updating_dialog_title), null);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Map<String, NamedObject<?>> map) {
            hideProgressDialog();

            List<Vaccine> vaccineList = new ArrayList<>();
            Weight weight = null;

            Map<String, List<ServiceType>> serviceTypeMap = new LinkedHashMap<>();
            List<ServiceRecord> serviceRecords = new ArrayList<>();

            List<Alert> alertList = new ArrayList<>();

            if (map.containsKey(Weight.class.getName())) {
                NamedObject<?> namedObject = map.get(Weight.class.getName());
                if (namedObject != null) {
                    weight = (Weight) namedObject.object;
                }

            }

            if (map.containsKey(Vaccine.class.getName())) {
                NamedObject<?> namedObject = map.get(Vaccine.class.getName());
                if (namedObject != null) {
                    vaccineList = (List<Vaccine>) namedObject.object;
                }

            }

            if (map.containsKey(ServiceType.class.getName())) {
                NamedObject<?> namedObject = map.get(ServiceType.class.getName());
                if (namedObject != null) {
                    serviceTypeMap = (Map<String, List<ServiceType>>) namedObject.object;
                }

            }

            if (map.containsKey(ServiceRecord.class.getName())) {
                NamedObject<?> namedObject = map.get(ServiceRecord.class.getName());
                if (namedObject != null) {
                    serviceRecords = (List<ServiceRecord>) namedObject.object;
                }

            }

            if (map.containsKey(Alert.class.getName())) {
                NamedObject<?> namedObject = map.get(Alert.class.getName());
                if (namedObject != null) {
                    alertList = (List<Alert>) namedObject.object;
                }

            }

            updateWeightViews(weight);
            updateServiceViews(serviceTypeMap, serviceRecords, alertList);
            updateVaccinationViews(vaccineList, alertList);
            performRegisterActions();
        }

        @Override
        protected Map<String, NamedObject<?>> doInBackground(Void... voids) {
            String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
            DateTime dateTime = util.Utils.dobStringToDateTime(dobString);
            if (dateTime != null) {
                VaccineSchedule.updateOfflineAlerts(childDetails.entityId(), dateTime, PathConstants.KEY.CHILD);
            }

            List<Vaccine> vaccineList = new ArrayList<>();
            Weight weight = null;

            Map<String, List<ServiceType>> serviceTypeMap = new LinkedHashMap<>();
            List<ServiceRecord> serviceRecords = new ArrayList<>();

            List<Alert> alertList = new ArrayList<>();
            if (vaccineRepository != null) {
                vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

            }
            if (weightRepository != null) {
                weight = weightRepository.findUnSyncedByEntityId(childDetails.entityId());
            }

            if (recurringServiceRecordRepository != null) {
                serviceRecords = recurringServiceRecordRepository.findByEntityId(childDetails.entityId());
            }

            if (recurringServiceTypeRepository != null) {
                List<String> types = recurringServiceTypeRepository.fetchTypes();
                for (String type : types) {
                    List<ServiceType> subTypes = recurringServiceTypeRepository.findByType(type);
                    serviceTypeMap.put(type, subTypes);
                }
            }

            if (alertService != null) {
                alertList = alertService.findByEntityId(childDetails.entityId());
            }

            Map<String, NamedObject<?>> map = new HashMap<>();

            NamedObject<List<Vaccine>> vaccineNamedObject = new NamedObject<>(Vaccine.class.getName(), vaccineList);
            map.put(vaccineNamedObject.name, vaccineNamedObject);

            NamedObject<Weight> weightNamedObject = new NamedObject<>(Weight.class.getName(), weight);
            map.put(weightNamedObject.name, weightNamedObject);

            NamedObject<Map<String, List<ServiceType>>> serviceTypeNamedObject = new NamedObject<>(ServiceType.class.getName(), serviceTypeMap);
            map.put(serviceTypeNamedObject.name, serviceTypeNamedObject);

            NamedObject<List<ServiceRecord>> serviceRecordNamedObject = new NamedObject<>(ServiceRecord.class.getName(), serviceRecords);
            map.put(serviceRecordNamedObject.name, serviceRecordNamedObject);

            NamedObject<List<Alert>> alertsNamedObject = new NamedObject<>(Alert.class.getName(), alertList);
            map.put(alertsNamedObject.name, alertsNamedObject);

            return map;
        }
    }

    public class SaveServiceTask extends AsyncTask<ServiceWrapper, Void, Triple<ArrayList<ServiceWrapper>, List<ServiceRecord>, List<Alert>>> {

        private View view;
        private String providerId;
        private String locationId;

        public void setView(View view) {
            this.view = view;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public void setLocationId(String locationId) {
            this.locationId = locationId;
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
                RecurringServiceUtils.saveService(tag, childDetails.entityId(), providerId, locationId);
                setLastModified(true);
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

    private class UndoServiceTask extends AsyncTask<Void, Void, Void> {

        private final View view;
        private final ServiceWrapper tag;
        private List<ServiceRecord> serviceRecordList;
        private ArrayList<ServiceWrapper> wrappers;
        private List<Alert> alertList;

        public UndoServiceTask(ServiceWrapper tag) {
            this.tag = tag;
            this.view = RecurringServiceUtils.getLastOpenedServiceView(serviceGroups);
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(getString(R.string.updating_dialog_title), null);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (tag != null && tag.getDbKey() != null) {
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

    private class ShowGrowthChartTask extends AsyncTask<Void, Void, List<Weight>> {
        FragmentTransaction ft;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            FragmentManager fragmentManager = ChildImmunizationActivity.this.getFragmentManager();
            Fragment prev = fragmentManager.findFragmentByTag(DIALOG_TAG);
            ft = fragmentManager.beginTransaction();
            if (prev != null) {
                Log.d("duplicate fragment", "found duplicate growth fragment"); // remove this
                this.cancel(true);
                return;
            }
             // remove this comment
            showProgressDialog();
        }

        @Override
        protected List<Weight> doInBackground(Void... params) {
            WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();
            List<Weight> allWeights = weightRepository.findByEntityId(childDetails.entityId());
            try {
                String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
                Date dob = util.Utils.dobStringToDate(dobString);
                if (!TextUtils.isEmpty(Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.BIRTH_WEIGHT, false))
                        && dob != null) {
                    Double birthWeight = Double.valueOf(Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.BIRTH_WEIGHT, false));

                    Weight weight = new Weight(-1l, null, (float) birthWeight.doubleValue(), dob, null, null, null, Calendar.getInstance().getTimeInMillis(), null, null, 0);
                    allWeights.add(weight);
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return allWeights;
        }

        @Override
        protected void onPostExecute(List<Weight> allWeights) {
            super.onPostExecute(allWeights);
            hideProgressDialog();

            ft.addToBackStack(null);
            GrowthDialogFragment growthDialogFragment = GrowthDialogFragment.newInstance(childDetails, allWeights);
            growthDialogFragment.show(ft, DIALOG_TAG);
        }
    }

    private class MarkBcgTwoAsDoneTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            DetailsRepository detailsRepository = VaccinatorApplication.getInstance().context().detailsRepository();
            detailsRepository.add(childDetails.entityId(), BCG2_NOTIFICATION_DONE, Boolean.TRUE.toString(), new Date().getTime());
            return null;
        }

    }

    private class SaveVaccinesTask extends AsyncTask<VaccineWrapper, Void, Pair<ArrayList<VaccineWrapper>, List<Vaccine>>> {

        private View view;
        private VaccineRepository vaccineRepository;
        private AlertService alertService;
        private List<String> affectedVaccines;
        private List<Vaccine> vaccineList;
        private List<Alert> alertList;

        public void setView(View view) {
            this.view = view;
        }

        public void setVaccineRepository(VaccineRepository vaccineRepository) {
            this.vaccineRepository = vaccineRepository;
            alertService = getOpenSRPContext().alertService();
            affectedVaccines = new ArrayList<>();
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected void onPostExecute(Pair<ArrayList<VaccineWrapper>, List<Vaccine>> pair) {
            hideProgressDialog();
            updateVaccineGroupViews(view, pair.first, pair.second);
            View recordWeight = findViewById(R.id.record_weight);
            WeightWrapper weightWrapper = (WeightWrapper) recordWeight.getTag();
            if (weightWrapper == null || weightWrapper.getWeight() == null) {
                showRecordWeightNotification();
            }

            updateVaccineGroupsUsingAlerts(affectedVaccines, vaccineList, alertList);
            showVaccineNotifications(vaccineList, alertList);
        }

        @Override
        protected Pair<ArrayList<VaccineWrapper>, List<Vaccine>> doInBackground(VaccineWrapper... vaccineWrappers) {

            ArrayList<VaccineWrapper> list = new ArrayList<>();
            if (vaccineRepository != null) {
                for (VaccineWrapper tag : vaccineWrappers) {
                    saveVaccine(vaccineRepository, tag);
                    list.add(tag);
                }
            }

            Pair<ArrayList<VaccineWrapper>, List<Vaccine>> pair = new Pair<>(list, vaccineList);
            String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
            DateTime dateTime = util.Utils.dobStringToDateTime(dobString);
            if (dateTime != null) {
                affectedVaccines = VaccineSchedule.updateOfflineAlerts(childDetails.entityId(), dateTime, PathConstants.KEY.CHILD);
            }
            vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());
            alertList = alertService.findByEntityIdAndAlertNames(childDetails.entityId(),
                    VaccinateActionUtils.allAlertNames(PathConstants.KEY.CHILD));

            return pair;
        }
    }


    private class UndoVaccineTask extends AsyncTask<Void, Void, Void> {

        private final VaccineWrapper tag;
        private final View v;
        private final VaccineRepository vaccineRepository;
        private final AlertService alertService;
        private List<Vaccine> vaccineList;
        private List<Alert> alertList;
        private List<String> affectedVaccines;

        public UndoVaccineTask(VaccineWrapper tag, View v) {
            this.tag = tag;
            this.v = v;
            vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
            alertService = getOpenSRPContext().alertService();
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(getString(R.string.updating_dialog_title), null);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (tag != null) {

                if (tag.getDbKey() != null) {
                    Long dbKey = tag.getDbKey();
                    vaccineRepository.deleteVaccine(dbKey);

                    // Update coverage reports
                    CoverageDropoutIntentService.unregister(ChildImmunizationActivity.this, childDetails.entityId(), tag.getName());

                    String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
                    DateTime dateTime = util.Utils.dobStringToDateTime(dobString);
                    if (dateTime != null) {
                        affectedVaccines = VaccineSchedule.updateOfflineAlerts(childDetails.entityId(), dateTime, PathConstants.KEY.CHILD);
                        vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());
                        alertList = alertService.findByEntityIdAndAlertNames(childDetails.entityId(),
                                VaccinateActionUtils.allAlertNames(PathConstants.KEY.CHILD));
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            hideProgressDialog();
            super.onPostExecute(params);

            // Refresh the vaccine group with the updated vaccine
            tag.setUpdatedVaccineDate(null, false);
            tag.setDbKey(null);

            View view = getLastOpenedView();

            ArrayList<VaccineWrapper> wrappers = new ArrayList<>();
            wrappers.add(tag);
            updateVaccineGroupViews(view, wrappers, vaccineList, true);
            updateVaccineGroupsUsingAlerts(affectedVaccines, vaccineList, alertList);
            showVaccineNotifications(vaccineList, alertList);
        }
    }

    private class GetSiblingsTask extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            String baseEntityId = childDetails.entityId();
            String motherBaseEntityId = Utils.getValue(childDetails.getColumnmaps(), PathConstants.KEY.RELATIONAL_ID, false);
            if (!TextUtils.isEmpty(motherBaseEntityId) && !TextUtils.isEmpty(baseEntityId)) {
                List<CommonPersonObject> children = getOpenSRPContext().commonrepository(PathConstants.CHILD_TABLE_NAME)
                        .findByRelational_IDs(motherBaseEntityId);

                if (children != null) {
                    ArrayList<String> baseEntityIds = new ArrayList<>();
                    for (CommonPersonObject curChild : children) {
                        if (!baseEntityId.equals(curChild.getCaseId())) {
                            baseEntityIds.add(curChild.getCaseId());
                        }
                    }

                    return baseEntityIds;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> baseEntityIds) {
            super.onPostExecute(baseEntityIds);
            ArrayList<String> ids = new ArrayList<>();
            if (baseEntityIds != null) {
                ids = baseEntityIds;
            }

            Collections.reverse(ids);

            SiblingPicturesGroup siblingPicturesGroup = (SiblingPicturesGroup) ChildImmunizationActivity.this.findViewById(R.id.sibling_pictures);
            siblingPicturesGroup.setSiblingBaseEntityIds(ChildImmunizationActivity.this, ids);
        }
    }

}
