package org.smartregister.path.application;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.growthmonitoring.GrowthMonitoringLibrary;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.repository.ZScoreRepository;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineNameRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.repository.VaccineTypeRepository;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.path.BuildConfig;
import org.smartregister.path.R;
import org.smartregister.path.activity.LoginActivity;
import org.smartregister.path.receiver.Hia2ServiceBroadcastReceiver;
import org.smartregister.path.receiver.PathSyncBroadcastReceiver;
import org.smartregister.path.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.path.receiver.VaccinatorAlarmReceiver;
import org.smartregister.path.repository.DailyTalliesRepository;
import org.smartregister.path.repository.HIA2IndicatorsRepository;
import org.smartregister.path.repository.MonthlyTalliesRepository;
import org.smartregister.path.repository.PathRepository;
import org.smartregister.path.repository.StockRepository;
import org.smartregister.path.repository.UniqueIdRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Repository;
import org.smartregister.sync.DrishtiSyncScheduler;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.receiver.TimeChangedBroadcastReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.fabric.sdk.android.Fabric;
import util.PathConstants;

import static org.smartregister.util.Log.logError;
import static org.smartregister.util.Log.logInfo;

/**
 * Created by koros on 2/3/16.
 */
public class VaccinatorApplication extends DrishtiApplication
        implements TimeChangedBroadcastReceiver.OnTimeChangedListener {

    private static final String TAG = "VaccinatorApplication";
    private static CommonFtsObject commonFtsObject;
    private UniqueIdRepository uniqueIdRepository;
    private DailyTalliesRepository dailyTalliesRepository;
    private MonthlyTalliesRepository monthlyTalliesRepository;
    private HIA2IndicatorsRepository hIA2IndicatorsRepository;
    private EventClientRepository eventClientRepository;
    private StockRepository stockRepository;
    private boolean lastModified;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        context = Context.getInstance();
        context.updateApplicationContext(getApplicationContext());
        context.updateCommonFtsObject(createCommonFtsObject());

        //Initialize Modules
        CoreLibrary.init(context());
        GrowthMonitoringLibrary.init(context(), getRepository());
        ImmunizationLibrary.init(context(), getRepository(), createCommonFtsObject());

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        DrishtiSyncScheduler.setReceiverClass(PathSyncBroadcastReceiver.class);

        Hia2ServiceBroadcastReceiver.init(this);
        SyncStatusBroadcastReceiver.init(this);
        TimeChangedBroadcastReceiver.init(this);
        TimeChangedBroadcastReceiver.getInstance().addOnTimeChangedListener(this);

        applyUserLanguagePreference();
        cleanUpSyncState();
        initOfflineSchedules();
        setCrashlyticsUser(context);
        setAlarms(this);

    }

    public static synchronized VaccinatorApplication getInstance() {
        return (VaccinatorApplication) mInstance;
    }

    @Override
    public void logoutCurrentUser() {

        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getApplicationContext().startActivity(intent);
        context.userService().logoutSession();
    }

    protected void cleanUpSyncState() {
        DrishtiSyncScheduler.stop(getApplicationContext());
        context.allSharedPreferences().saveIsSyncInProgress(false);
    }


    @Override
    public void onTerminate() {
        logInfo("Application is terminating. Stopping Bidan Sync scheduler and resetting isSyncInProgress setting.");
        cleanUpSyncState();
        SyncStatusBroadcastReceiver.destroy(this);
        TimeChangedBroadcastReceiver.destroy(this);
        super.onTerminate();
    }

    protected void applyUserLanguagePreference() {
        Configuration config = getBaseContext().getResources().getConfiguration();

        String lang = context.allSharedPreferences().fetchLanguagePreference();
        if (!"".equals(lang) && !config.locale.getLanguage().equals(lang)) {
            locale = new Locale(lang);
            updateConfiguration(config);
        }
    }

    private void updateConfiguration(Configuration config) {
        config.locale = locale;
        Locale.setDefault(locale);
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    private static String[] getFtsSearchFields(String tableName) {
        if (tableName.equals(PathConstants.CHILD_TABLE_NAME)) {
            return new String[]{"zeir_id", "epi_card_number", "first_name", "last_name"};
        } else if (tableName.equals(PathConstants.MOTHER_TABLE_NAME)) {
            return new String[]{"zeir_id", "epi_card_number", "first_name", "last_name", "father_name", "husband_name", "contact_phone_number"};
        }
        return null;
    }

    private static String[] getFtsSortFields(String tableName) {


        if (tableName.equals(PathConstants.CHILD_TABLE_NAME)) {
            ArrayList<VaccineRepo.Vaccine> vaccines = VaccineRepo.getVaccines("child");
            List<String> names = new ArrayList<>();
            names.add("first_name");
            names.add("dob");
            names.add("zeir_id");
            names.add("last_interacted_with");
            names.add("inactive");
            names.add("lost_to_follow_up");
            names.add(PathConstants.EC_CHILD_TABLE.DOD);

            for (VaccineRepo.Vaccine vaccine : vaccines) {
                names.add("alerts." + VaccinateActionUtils.addHyphen(vaccine.display()));
            }

            return names.toArray(new String[names.size()]);
        } else if (tableName.equals(PathConstants.MOTHER_TABLE_NAME)) {
            return new String[]{"first_name", "dob", "zeir_id", "last_interacted_with"};
        }
        return null;
    }

    private static String[] getFtsTables() {
        return new String[]{PathConstants.CHILD_TABLE_NAME, PathConstants.MOTHER_TABLE_NAME};
    }

    private static Map<String, Pair<String, Boolean>> getAlertScheduleMap() {
        ArrayList<VaccineRepo.Vaccine> vaccines = VaccineRepo.getVaccines("child");
        Map<String, Pair<String, Boolean>> map = new HashMap<>();
        for (VaccineRepo.Vaccine vaccine : vaccines) {
            map.put(vaccine.display(), Pair.create(PathConstants.CHILD_TABLE_NAME, false));
        }
        return map;
    }

    public static CommonFtsObject createCommonFtsObject() {
        if (commonFtsObject == null) {
            commonFtsObject = new CommonFtsObject(getFtsTables());
            for (String ftsTable : commonFtsObject.getTables()) {
                commonFtsObject.updateSearchFields(ftsTable, getFtsSearchFields(ftsTable));
                commonFtsObject.updateSortFields(ftsTable, getFtsSortFields(ftsTable));
            }
        }
        commonFtsObject.updateAlertScheduleMap(getAlertScheduleMap());
        return commonFtsObject;
    }

    /**
     * This method sets the Crashlytics user to whichever username was used to log in last. It only
     * does so if the app is not built for debugging
     *
     * @param context The user's context
     */
    public static void setCrashlyticsUser(Context context) {
        if (!BuildConfig.DEBUG
                && context != null && context.userService() != null
                && context.userService().getAllSharedPreferences() != null) {
            Crashlytics.setUserName(context.userService().getAllSharedPreferences().fetchRegisteredANM());
        }
    }

    private void grantPhotoDirectoryAccess() {
        Uri uri = FileProvider.getUriForFile(this,
                "com.vijay.jsonwizard.fileprovider",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        grantUriPermission("com.vijay.jsonwizard", uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    @Override
    public Repository getRepository() {
        try {
            if (repository == null) {
                repository = new PathRepository(getInstance().getApplicationContext(), context());
                uniqueIdRepository();
                dailyTalliesRepository();
                monthlyTalliesRepository();
                hIA2IndicatorsRepository();
                eventClientRepository();
                stockRepository();
            }
        } catch (UnsatisfiedLinkError e) {
            logError("Error on getRepository: " + e);

        }
        return repository;
    }


    public WeightRepository weightRepository() {
        return GrowthMonitoringLibrary.getInstance().weightRepository();
    }

    public Context context() {
        return context;
    }

    public VaccineRepository vaccineRepository() {
        return ImmunizationLibrary.getInstance().vaccineRepository();
    }

    public ZScoreRepository zScoreRepository() {
        return GrowthMonitoringLibrary.getInstance().zScoreRepository();
    }

    public UniqueIdRepository uniqueIdRepository() {
        if (uniqueIdRepository == null) {
            uniqueIdRepository = new UniqueIdRepository((PathRepository) getRepository());
        }
        return uniqueIdRepository;
    }

    public DailyTalliesRepository dailyTalliesRepository() {
        if (dailyTalliesRepository == null) {
            dailyTalliesRepository = new DailyTalliesRepository((PathRepository) getRepository());
        }
        return dailyTalliesRepository;
    }

    public MonthlyTalliesRepository monthlyTalliesRepository() {
        if (monthlyTalliesRepository == null) {
            monthlyTalliesRepository = new MonthlyTalliesRepository((PathRepository) getRepository());
        }

        return monthlyTalliesRepository;
    }

    public HIA2IndicatorsRepository hIA2IndicatorsRepository() {
        if (hIA2IndicatorsRepository == null) {
            hIA2IndicatorsRepository = new HIA2IndicatorsRepository((PathRepository) getRepository());
        }
        return hIA2IndicatorsRepository;
    }

    public RecurringServiceTypeRepository recurringServiceTypeRepository() {
        return ImmunizationLibrary.getInstance().recurringServiceTypeRepository();
    }

    public RecurringServiceRecordRepository recurringServiceRecordRepository() {
        return ImmunizationLibrary.getInstance().recurringServiceRecordRepository();
    }

    public EventClientRepository eventClientRepository() {
        if (eventClientRepository == null) {
            eventClientRepository = new EventClientRepository(getRepository());
        }
        return eventClientRepository;
    }

    public StockRepository stockRepository() {
        if (stockRepository == null) {
            stockRepository = new StockRepository((PathRepository) getRepository());
        }
        return stockRepository;
    }

    public VaccineTypeRepository vaccineTypeRepository() {
        return ImmunizationLibrary.getInstance().vaccineTypeRepository();
    }

    public VaccineNameRepository vaccineNameRepository() {
        return ImmunizationLibrary.getInstance().vaccineNameRepository();
    }

    public boolean isLastModified() {
        return lastModified;
    }

    public void setLastModified(boolean lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public void onTimeChanged() {
        Toast.makeText(this, R.string.device_time_changed, Toast.LENGTH_LONG).show();
        context.userService().forceRemoteLogin();
        logoutCurrentUser();
    }

    @Override
    public void onTimeZoneChanged() {
        Toast.makeText(this, R.string.device_timezone_changed, Toast.LENGTH_LONG).show();
        context.userService().forceRemoteLogin();
        logoutCurrentUser();
    }

    private void initOfflineSchedules() {
        try {
            JSONArray childVaccines = new JSONArray(VaccinatorUtils.getSupportedVaccines(this));
            JSONArray specialVaccines = new JSONArray(VaccinatorUtils.getSpecialVaccines(this));
            VaccineSchedule.init(childVaccines, specialVaccines, "child");
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static void setAlarms(android.content.Context context) {
        VaccinatorAlarmReceiver.setAlarm(context, 2, PathConstants.ServiceType.DAILY_TALLIES_GENERATION);
        VaccinatorAlarmReceiver.setAlarm(context, 2, PathConstants.ServiceType.WEIGHT_SYNC_PROCESSING);
        VaccinatorAlarmReceiver.setAlarm(context, 2, PathConstants.ServiceType.VACCINE_SYNC_PROCESSING);
        VaccinatorAlarmReceiver.setAlarm(context, 2, PathConstants.ServiceType.RECURRING_SERVICES_SYNC_PROCESSING);
        VaccinatorAlarmReceiver.setAlarm(context, 2, PathConstants.ServiceType.IMAGE_UPLOAD);
        VaccinatorAlarmReceiver.setAlarm(context, 5, PathConstants.ServiceType.PULL_UNIQUE_IDS);

    }

}
