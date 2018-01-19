package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.ChildReport;
import org.smartregister.path.domain.Cohort;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.path.receiver.CoverageDropoutBroadcastReceiver;
import org.smartregister.path.repository.ChildReportRepository;
import org.smartregister.path.repository.CohortIndicatorRepository;
import org.smartregister.path.repository.CohortRepository;
import org.smartregister.repository.EventClientRepository;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.PathConstants;
import util.Utils;


/**
 * Created by onamacuser on 18/03/2016.
 */
public class CoverageDropoutIntentService extends IntentService {
    private static final String TAG = CoverageDropoutIntentService.class.getCanonicalName();

    private static final String BIRTH_REGISTRATION_QUERY = "SELECT " +
            EventClientRepository.client_column.baseEntityId.name() +
            ", " + EventClientRepository.client_column.birthdate.name() +
            ", " + EventClientRepository.client_column.updatedAt.name() +
            " FROM " + EventClientRepository.Table.client.name() +
            " WHERE (" + EventClientRepository.client_column.deathdate.name() + " is NULL OR " + EventClientRepository.client_column.deathdate.name() + " = '') " +
            " AND " + EventClientRepository.client_column.identifiers.name() + " LIKE '%\"" + util.JsonFormUtils.ZEIR_ID + "\"%' ";
    ;

    private static final String VACCINE_QUERY = "SELECT " +
            "v." + VaccineRepository.BASE_ENTITY_ID +
            ", c." + PathConstants.EC_CHILD_TABLE.DOB +
            ", v." + VaccineRepository.NAME +
            ", v." + VaccineRepository.DATE +
            ", v." + VaccineRepository.UPDATED_AT_COLUMN +
            " FROM " + VaccineRepository.VACCINE_TABLE_NAME + " v  " +
            " INNER JOIN " + PathConstants.CHILD_TABLE_NAME + " c  " +
            " ON v." + VaccineRepository.BASE_ENTITY_ID + " = c." + PathConstants.EC_CHILD_TABLE.BASE_ENTITY_ID;

    private static final String COVERAGE_DROPOUT_BIRTH_REGISTRATION_LAST_PROCESSED_DATE = "COVERAGE_DROPOUT_BIRTH_REGISTRATION_LAST_PROCESSED_DATE";
    private static final String COVERAGE_DROPOUT_VACCINATION_LAST_PROCESSED_DATE = "COVERAGE_DROPOUT_VACCINATION_LAST_PROCESSED_DATE";

    public CoverageDropoutIntentService() {
        super("CoverageDropoutIntentService");
    }

    /**
     * Build indicators,save them to the db and generate report
     *
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Started Coverage Dropout service");
        try {

            generateBirthRegistrationIndicators();
            generateVaccineIndicators();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        Log.i(TAG, "Finishing  Coverage Dropout service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendBroadcastMessage(String type) {
        Intent intent = new Intent();
        intent.setAction(CoverageDropoutBroadcastReceiver.ACTION_SERVICE_DONE);
        intent.putExtra(CoverageDropoutBroadcastReceiver.TYPE, type);
        sendBroadcast(intent);
    }

    private static void sendBroadcastMessage(Context context, String type) {
        Intent intent = new Intent();
        intent.setAction(CoverageDropoutBroadcastReceiver.ACTION_SERVICE_DONE);
        intent.putExtra(CoverageDropoutBroadcastReceiver.TYPE, type);
        context.sendBroadcast(intent);
    }

    private void generateBirthRegistrationIndicators() {
        try {
            final String dateColumn = EventClientRepository.client_column.updatedAt.name();
            final String orderByClause = " ORDER BY " + dateColumn + " ASC ";

            EventClientRepository eventClientRepository = VaccinatorApplication.getInstance().eventClientRepository();
            SQLiteDatabase db = VaccinatorApplication.getInstance().getRepository().getWritableDatabase();
            String lastProcessedDate = VaccinatorApplication.getInstance().context().allSharedPreferences().getPreference(COVERAGE_DROPOUT_BIRTH_REGISTRATION_LAST_PROCESSED_DATE);

            ArrayList<HashMap<String, String>> results;
            if (StringUtils.isBlank(lastProcessedDate)) {
                results = eventClientRepository.rawQuery(db, BIRTH_REGISTRATION_QUERY.concat(orderByClause));

            } else {
                results = eventClientRepository.rawQuery(db, BIRTH_REGISTRATION_QUERY.concat(filterKey(BIRTH_REGISTRATION_QUERY) + dateColumn + " > '" + lastProcessedDate + "' ").concat(orderByClause));
            }

            for (Map<String, String> result : results) {
                String baseEntityId = result.get(EventClientRepository.client_column.baseEntityId.name());
                String dobString = result.get(EventClientRepository.client_column.birthdate.name());
                String updatedAt = result.get(EventClientRepository.client_column.updatedAt.name());

                if (StringUtils.isNotBlank(dobString)) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        Date dob = dateFormat.parse(dobString);
                        updateRegistrations(baseEntityId, dob);
                    } catch (ParseException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }

                VaccinatorApplication.getInstance().context().allSharedPreferences().savePreference(COVERAGE_DROPOUT_BIRTH_REGISTRATION_LAST_PROCESSED_DATE, updatedAt);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void generateVaccineIndicators() {
        try {
            final String updatedAtColumn = " v." + VaccineRepository.UPDATED_AT_COLUMN;
            final String orderByClause = " ORDER BY " + updatedAtColumn + " ASC ";

            EventClientRepository eventClientRepository = VaccinatorApplication.getInstance().eventClientRepository();
            SQLiteDatabase db = VaccinatorApplication.getInstance().getRepository().getWritableDatabase();
            String lastProcessedDate = VaccinatorApplication.getInstance().context().allSharedPreferences().getPreference(COVERAGE_DROPOUT_VACCINATION_LAST_PROCESSED_DATE);

            ArrayList<HashMap<String, String>> results;
            if (StringUtils.isBlank(lastProcessedDate)) {
                results = eventClientRepository.rawQuery(db, VACCINE_QUERY.concat(orderByClause));

            } else {
                results = eventClientRepository.rawQuery(db, VACCINE_QUERY.concat(filterKey(VACCINE_QUERY) + updatedAtColumn + " > " + lastProcessedDate).concat(orderByClause));
            }

            for (Map<String, String> result : results) {
                try {
                    String baseEntityId = result.get(VaccineRepository.BASE_ENTITY_ID);
                    String dobString = result.get(PathConstants.EC_CHILD_TABLE.DOB);
                    String vaccineName = result.get(VaccineRepository.NAME);
                    String updatedAt = result.get(VaccineRepository.UPDATED_AT_COLUMN);
                    String eventDate = result.get(VaccineRepository.DATE);

                    if (StringUtils.isNotBlank(dobString) && StringUtils.isNotBlank(baseEntityId)) {
                        DateTime dateTime = new DateTime(dobString);
                        Date dob = dateTime.toDate();

                        updateIndicators(getApplicationContext(), baseEntityId, dob, vaccineName, eventDate);
                    }

                    VaccinatorApplication.getInstance().context().allSharedPreferences().savePreference(COVERAGE_DROPOUT_VACCINATION_LAST_PROCESSED_DATE, updatedAt);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            sendBroadcastMessage(CoverageDropoutBroadcastReceiver.TYPE_GENERATE_COHORT_INDICATORS);
        }
    }

    private static void updateRegistrations(String baseEntityId, Date dob) {
        try {
            if (StringUtils.isBlank(baseEntityId) || dob == null) {
                return;
            }

            CohortRepository cohortRepository = VaccinatorApplication.getInstance().cohortRepository();
            ChildReportRepository childReportRepository = VaccinatorApplication.getInstance().childReportRepository();

            Cohort cohort = cohortRepository.findByMonth(dob);
            if (cohort == null) {
                cohort = new Cohort();
                cohort.setMonth(dob);
                cohortRepository.add(cohort);

                // Break if the cohort record cannot be added to the db
                if (cohort.getId() == null || cohort.getId().equals(-1L)) {
                    return;
                }
            }

            ChildReport childReport = childReportRepository.findByBaseEntityId(baseEntityId);
            if (childReport == null) {
                childReport = new ChildReport();
                childReport.setBaseEntityId(baseEntityId);
                childReport.setCohortId(cohort.getId());
                childReportRepository.add(childReport);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

    }

    public static void updateIndicators(Context context, String baseEntityId, Date dob, String vaccineName, String eventDate) {
        if (context == null || StringUtils.isBlank(baseEntityId) || dob == null || StringUtils.isBlank(vaccineName) || StringUtils.isBlank(eventDate)) {
            return;
        }
        try {
            List<String> vaccines = formatAndSplitVaccineName(vaccineName);

            for (String vaccine : vaccines) {
                ChildReportRepository childReportRepository = VaccinatorApplication.getInstance().childReportRepository();
                ChildReport childReport = childReportRepository.findByBaseEntityId(baseEntityId);
                if (childReport != null) {
                    updateIndicators(dob, vaccine, eventDate, childReport);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            sendBroadcastMessage(context, CoverageDropoutBroadcastReceiver.TYPE_GENERATE_COHORT_INDICATORS);
        }

    }

    private static void updateIndicators(Date dob, String vaccineName, String eventDate, ChildReport childReport) {
        try {
            if (StringUtils.isBlank(vaccineName) || dob == null || childReport == null) {
                return;
            }

            if (StringUtils.isBlank(eventDate) || !StringUtils.isNumeric(eventDate)) {
                return;
            }

            ChildReportRepository childReportRepository = VaccinatorApplication.getInstance().childReportRepository();
            CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();

            long timeStamp = Long.valueOf(eventDate);
            Date vaccineDate = new Date(timeStamp);

            // Don't add an already counted vaccine unless it's invalid now
            boolean alreadyCounted = false;
            String preVaccines = childReport.getValidVaccines();
            if (StringUtils.isNotBlank(preVaccines) && preVaccines.contains(vaccineName)) {
                alreadyCounted = true;
            }

            // Don't add invalid vaccines to the indicator table
            boolean isValid = isValidVaccine(vaccineName, dob, vaccineDate);

            boolean subtract = false;
            if (!isValid) {
                if (alreadyCounted) { // Already counted but now invalid - remove
                    subtract = true;
                } else {
                    return; // Not counted and invalid - no need to proceed
                }
            } else {
                if (alreadyCounted) { // Valid and Already counted - no need to proceed
                    return;
                }
            }

            String validVaccines = vaccineName;
            if (StringUtils.isNotBlank(preVaccines)) {
                List<String> vaccineList = validVaccinesAsList(preVaccines);

                if (subtract) {
                    vaccineList.remove(vaccineName);
                } else {
                    vaccineList.add(vaccineName);
                }

                validVaccines = StringUtils.join(vaccineList, ",");
            }
            childReportRepository.changeValidVaccines(validVaccines, childReport.getId());

            CohortIndicator cohortIndicator = cohortIndicatorRepository.findByVaccineAndCohort(vaccineName, childReport.getCohortId());
            if (cohortIndicator == null) {
                if (subtract) {
                    throw CoverageDropoutException.newInstance("Impossible!!! subract should only happen for already counted vaccines");
                }
                cohortIndicator = new CohortIndicator();
                cohortIndicator.setCohortId(childReport.getCohortId());
                cohortIndicator.setValue(1L);
                cohortIndicator.setVaccine(vaccineName);
                cohortIndicatorRepository.add(cohortIndicator);
            } else {
                Long value = cohortIndicator.getValue();
                if (subtract) {
                    if (value <= 0) {
                        return;
                    }
                    value -= 1L;
                } else {
                    value += 1L;
                }
                cohortIndicatorRepository.changeValue(value, cohortIndicator.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static void unregister(Context context, String baseEntityId, String vaccineName) {

        CohortRepository cohortRepository = VaccinatorApplication.getInstance().cohortRepository();
        ChildReportRepository childReportRepository = VaccinatorApplication.getInstance().childReportRepository();
        CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();

        if (cohortRepository == null || childReportRepository == null || cohortIndicatorRepository == null || StringUtils.isBlank(baseEntityId)) {
            return;
        }

        try {
            ChildReport childReport = childReportRepository.findByBaseEntityId(baseEntityId);
            if (childReport == null) {
                return;
            }

            Long cohortId = childReport.getCohortId();

            String validVaccines = childReport.getValidVaccines();
            if (StringUtils.isNotBlank(validVaccines)) {
                List<String> vaccineList = validVaccinesAsList(validVaccines);

                if (StringUtils.isBlank(vaccineName)) { // Child Centered
                    for (String vaccine : vaccineList) { // Un register all vaccines
                        unregister(vaccine, cohortId);
                    }
                } else { // Vaccine Centered
                    List<String> incomingVaccineList = formatAndSplitVaccineName(vaccineName);
                    for (String incomingVaccine : incomingVaccineList) { // Un register incoming vaccine only
                        if (vaccineList.contains(incomingVaccine)) {
                            unregister(incomingVaccine, cohortId);
                        }
                    }

                    // Remove the vaccine in childReport
                    boolean removed = vaccineList.removeAll(incomingVaccineList);
                    if (removed) {
                        childReportRepository.changeValidVaccines(StringUtils.join(vaccineList, ","), childReport.getId());
                    }
                }

            }

            if (StringUtils.isNotBlank(vaccineName)) { // Vaccine Centered - Return
                return;
            }

            childReportRepository.delete(childReport.getId());

            List<ChildReport> otherChildren = childReportRepository.findByCohort(cohortId);
            if (otherChildren == null || otherChildren.isEmpty()) {
                cohortRepository.delete(cohortId);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            sendBroadcastMessage(context, CoverageDropoutBroadcastReceiver.TYPE_GENERATE_COHORT_INDICATORS);
        }
    }

    public static void unregister(Context context, String baseEntityId) {
        unregister(context, baseEntityId, null);
    }

    private static void unregister(String vaccine, Long cohortId) {
        CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();

        CohortIndicator cohortIndicator = cohortIndicatorRepository.findByVaccineAndCohort(vaccine, cohortId);
        if (cohortIndicator != null) {
            if (cohortIndicator.getValue() > 1) {
                long value = cohortIndicator.getValue() - 1L;
                cohortIndicatorRepository.changeValue(value, cohortIndicator.getId());
            } else {
                cohortIndicatorRepository.delete(cohortIndicator.getId());
            }
        }
    }

    private static boolean isValidVaccine(String vaccine, Date dob, Date eventDate) {
        Date endDate = Utils.getCohortEndDate(vaccine, dob);
        return DateUtils.isSameDay(endDate, eventDate) || endDate.after(eventDate);
    }

    private String filterKey(String query) {
        if (StringUtils.containsIgnoreCase(query, "where")) {
            return " AND ";
        }
        return " WHERE ";
    }

    private static List<String> formatAndSplitVaccineName(String vaccineName) {
        if (StringUtils.isBlank(vaccineName)) {
            return new ArrayList<>();
        }

        List<String> vaccineList = new ArrayList<>();
        if (vaccineName.contains("/")) {
            String[] vaccines = vaccineName.split("/");
            for (String vaccine : vaccines) {
                vaccineList.add(formatVaccineName(vaccine));
            }
        } else {
            vaccineList.add(formatVaccineName(vaccineName));
        }
        return vaccineList;
    }

    private static String formatVaccineName(String vaccineName) {
        if (StringUtils.isBlank(vaccineName)) {
            return vaccineName;
        }

        String vaccine = VaccineRepository.removeHyphen(vaccineName).trim();
        vaccine = VaccineRepository.addHyphen(vaccine.toLowerCase());

        final String mr1 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.mr1.display().toLowerCase());
        final String mr2 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.mr2.display().toLowerCase());
        if (vaccine.equals(mr1)) {
            vaccine = VaccineRepository.addHyphen(VaccineRepo.Vaccine.measles1.display().toLowerCase());
        }

        if (vaccine.equals(mr2)) {
            vaccine = VaccineRepository.addHyphen(VaccineRepo.Vaccine.measles2.display().toLowerCase());
        }

        return vaccine;
    }

    private static List<String> validVaccinesAsList(String validVaccines) {
        if (StringUtils.isBlank(validVaccines)) {
            return new ArrayList<>();
        }
        String[] vaccines = validVaccines.split(",");
        return new ArrayList<>(Arrays.asList(vaccines));
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private static class CoverageDropoutException extends Exception {

        private CoverageDropoutException(String details) {
            super(details);
        }

        public static CoverageDropoutException newInstance(String details) {
            return new CoverageDropoutException(details);
        }
    }

}
