package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import util.PathConstants;
import util.Utils;


/**
 * Created by onamacuser on 18/03/2016.
 */
public class CoverageDropoutIntentService extends IntentService {
    private static final String TAG = CoverageDropoutIntentService.class.getCanonicalName();
    private CohortRepository cohortRepository;
    private CohortIndicatorRepository cohortIndicatorRepository;
    private ChildReportRepository childReportRepository;
    private EventClientRepository eventClientRepository;

    private static final String BIRTH_REGISTRATION_QUERY = "SELECT " +
            PathConstants.EC_CHILD_TABLE.BASE_ENTITY_ID +
            ", " + PathConstants.EC_CHILD_TABLE.DOB +
            ", " + PathConstants.EC_CHILD_TABLE.DATE +
            " FROM " + PathConstants.CHILD_TABLE_NAME +
            " WHERE (" + PathConstants.EC_CHILD_TABLE.DOD + " is NULL OR " + PathConstants.EC_CHILD_TABLE.DOD + " = '') ";

    private static final String VACCINE_QUERY = "SELECT " +
            "v." + VaccineRepository.BASE_ENTITY_ID +
            ", c." + PathConstants.EC_CHILD_TABLE.DOB +
            ", v." + VaccineRepository.NAME +
            ", v." + VaccineRepository.DATE +
            ", v." + VaccineRepository.UPDATED_AT_COLUMN +
            " FROM " + VaccineRepository.VACCINE_TABLE_NAME + " v  " +
            " LEFT JOIN " + PathConstants.CHILD_TABLE_NAME + " c  " +
            " ON v." + VaccineRepository.BASE_ENTITY_ID + " = c." + PathConstants.EC_CHILD_TABLE.BASE_ENTITY_ID +
            " WHERE (v."+VaccineRepository.OUT_OF_AREA + " is NULL OR v." + VaccineRepository.OUT_OF_AREA + " = 0) ";

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
        cohortRepository = VaccinatorApplication.getInstance().cohortRepository();
        cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();
        childReportRepository = VaccinatorApplication.getInstance().childReportRepository();
        eventClientRepository = VaccinatorApplication.getInstance().eventClientRepository();

        return super.onStartCommand(intent, flags, startId);
    }

    private void sendBroadcastMessage(String type) {
        Intent intent = new Intent();
        intent.setAction(CoverageDropoutBroadcastReceiver.ACTION_SERVICE_DONE);
        intent.putExtra(CoverageDropoutBroadcastReceiver.TYPE, type);
        sendBroadcast(intent);
    }

    private void generateBirthRegistrationIndicators() {
        try {
            final String dateColumn = PathConstants.EC_CHILD_TABLE.DATE;
            final String orderByClause = " ORDER BY " + dateColumn + " ASC ";

            SQLiteDatabase db = VaccinatorApplication.getInstance().getRepository().getWritableDatabase();
            String lastProcessedDate = VaccinatorApplication.getInstance().context().allSharedPreferences().getPreference(COVERAGE_DROPOUT_BIRTH_REGISTRATION_LAST_PROCESSED_DATE);

            ArrayList<HashMap<String, String>> results;
            if (StringUtils.isBlank(lastProcessedDate)) {
                results = eventClientRepository.rawQuery(db, BIRTH_REGISTRATION_QUERY.concat(orderByClause));

            } else {
                results = eventClientRepository.rawQuery(db, BIRTH_REGISTRATION_QUERY.concat(filterKey(BIRTH_REGISTRATION_QUERY) + dateColumn + " > '" + lastProcessedDate + "' ").concat(orderByClause));
            }

            for (Map<String, String> result : results) {
                String baseEntityId = result.get(PathConstants.EC_CHILD_TABLE.BASE_ENTITY_ID);
                String dobString = result.get(PathConstants.EC_CHILD_TABLE.DOB);
                String date = result.get(PathConstants.EC_CHILD_TABLE.DATE);

                updateRegistrations(baseEntityId, dobString);

                VaccinatorApplication.getInstance().context().allSharedPreferences().savePreference(COVERAGE_DROPOUT_BIRTH_REGISTRATION_LAST_PROCESSED_DATE, date);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void generateVaccineIndicators() {
        try {
            final String updatedAtColumn = " v." + VaccineRepository.UPDATED_AT_COLUMN;
            final String orderByClause = " ORDER BY " + updatedAtColumn + " ASC ";

            SQLiteDatabase db = VaccinatorApplication.getInstance().getRepository().getWritableDatabase();
            String lastProcessedDate = VaccinatorApplication.getInstance().context().allSharedPreferences().getPreference(COVERAGE_DROPOUT_VACCINATION_LAST_PROCESSED_DATE);

            ArrayList<HashMap<String, String>> results;
            if (StringUtils.isBlank(lastProcessedDate)) {
                results = eventClientRepository.rawQuery(db, VACCINE_QUERY.concat(orderByClause));

            } else {
                results = eventClientRepository.rawQuery(db, VACCINE_QUERY.concat(filterKey(VACCINE_QUERY) + updatedAtColumn + " > " + lastProcessedDate).concat(orderByClause));
            }

            for (Map<String, String> result : results) {
                String baseEntityId = result.get(VaccineRepository.BASE_ENTITY_ID);
                String dobString = result.get(PathConstants.EC_CHILD_TABLE.DOB);
                String vaccineName = result.get(VaccineRepository.NAME);
                String updatedAt = result.get(VaccineRepository.UPDATED_AT_COLUMN);
                String eventDate = result.get(VaccineRepository.DATE);

                ChildReport childReport = updateRegistrations(baseEntityId, dobString);
                if (childReport != null) {
                    updateIndicators(dobString, vaccineName, eventDate, childReport);
                } else {
                    throw new RuntimeException("WTF!!!");
                }

                VaccinatorApplication.getInstance().context().allSharedPreferences().savePreference(COVERAGE_DROPOUT_VACCINATION_LAST_PROCESSED_DATE, updatedAt);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            sendBroadcastMessage(CoverageDropoutBroadcastReceiver.TYPE_GENERATE_COHORT_INDICATORS);
        }
    }

    private ChildReport updateRegistrations(String baseEntityId, String dobString) {
        try {
            if (StringUtils.isBlank(baseEntityId) || StringUtils.isBlank(dobString)) {
                return null;
            }

            DateTime dateTime = new DateTime(dobString);
            Date dob = dateTime.toDate();

            Cohort cohort = cohortRepository.findByMonth(dob);
            if (cohort == null) {
                cohort = new Cohort();
                cohort.setMonth(dob);
                cohortRepository.add(cohort);

                // Break if the cohort record cannot be added to the db
                if (cohort.getId() == null || cohort.getId().equals(-1L)) {
                    return null;
                }
            }

            ChildReport childReport = childReportRepository.findByBaseEntityIdAndCohort(baseEntityId, cohort.getId());
            if (childReport == null) {
                childReport = new ChildReport();
                childReport.setBaseEntityId(baseEntityId);
                childReport.setCohortId(cohort.getId());
                childReportRepository.add(childReport);
            }
            return childReport;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    private void updateIndicators(String dobString, String vaccineName, String eventDate, ChildReport childReport) {
        try {
            if (StringUtils.isBlank(vaccineName) || StringUtils.isBlank(dobString) || childReport == null) {
                return;
            }

            if (StringUtils.isBlank(eventDate) || !StringUtils.isNumeric(eventDate)) {
                return;
            }

            long timeStamp = Long.valueOf(eventDate);
            Date vaccineDate = new Date(timeStamp);

            DateTime dateTime = new DateTime(dobString);
            Date dob = dateTime.toDate();

            // Don't add invalid vaccines to the indicator table
            boolean validVaccine = isValidVaccine(vaccineName, dob, vaccineDate);
            if (!validVaccine) {
                return;
            }

            String preVaccines = childReport.getValidVaccines();
            if (StringUtils.isNotBlank(preVaccines) && preVaccines.contains(vaccineName)) {
                // Vaccine already counted
                return;
            }

            String vaccines;
            if (StringUtils.isBlank(preVaccines)) {
                vaccines = vaccineName;
            } else {
                vaccines = preVaccines.concat(",").concat(vaccineName);
            }

            childReport.setValidVaccines(vaccines);
            childReportRepository.changeValidVaccines(vaccines, childReport.getId());

            CohortIndicator cohortIndicator = cohortIndicatorRepository.findByVaccineAndCohort(vaccineName, childReport.getCohortId());
            if (cohortIndicator == null) {
                cohortIndicator = new CohortIndicator();
                cohortIndicator.setCohortId(childReport.getCohortId());
                cohortIndicator.setValue(1L);
                cohortIndicator.setVaccine(vaccineName);
                cohortIndicatorRepository.add(cohortIndicator);
            } else {
                cohortIndicator.setValue(cohortIndicator.getValue() + 1L);
                cohortIndicatorRepository.changeValue(cohortIndicator.getValue(), cohortIndicator.getId());
            }


        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

    }

    private boolean isValidVaccine(String vaccine, Date dob, Date eventDate) {
        Date endDate = Utils.getCohortEndDate(vaccine, dob);
        return DateUtils.isSameDay(endDate, eventDate) || endDate.after(eventDate);
    }

    private String filterKey(String query) {
        if (StringUtils.containsIgnoreCase(query, "where")) {
            return " AND ";
        }
        return " WHERE ";
    }

}
