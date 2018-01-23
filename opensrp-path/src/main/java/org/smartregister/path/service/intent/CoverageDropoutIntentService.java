package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.Cohort;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.path.domain.CohortPatient;
import org.smartregister.path.domain.Cumulative;
import org.smartregister.path.domain.CumulativeIndicator;
import org.smartregister.path.domain.CumulativePatient;
import org.smartregister.path.receiver.CoverageDropoutBroadcastReceiver;
import org.smartregister.path.repository.CohortIndicatorRepository;
import org.smartregister.path.repository.CohortPatientRepository;
import org.smartregister.path.repository.CohortRepository;
import org.smartregister.path.repository.CumulativeIndicatorRepository;
import org.smartregister.path.repository.CumulativePatientRepository;
import org.smartregister.path.repository.CumulativeRepository;
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

    private static final String COLON = ":";
    private static final String COMMA = ",";

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
        if (type != null) {
            Intent intent = new Intent();
            intent.setAction(CoverageDropoutBroadcastReceiver.ACTION_SERVICE_DONE);
            intent.putExtra(CoverageDropoutBroadcastReceiver.TYPE, type);
            sendBroadcast(intent);
        }
    }

    private static void sendBroadcastMessage(Context context, String type) {
        if (context != null && type != null) {
            Intent intent = new Intent();
            intent.setAction(CoverageDropoutBroadcastReceiver.ACTION_SERVICE_DONE);
            intent.putExtra(CoverageDropoutBroadcastReceiver.TYPE, type);
            context.sendBroadcast(intent);
        }
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

                        updateCohortRegistrations(baseEntityId, dob);
                        updateCumulativeRegistrations(baseEntityId, dob);

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

                    if (StringUtils.isNotBlank(dobString) && StringUtils.isNotBlank(baseEntityId) && StringUtils.isNotBlank(eventDate) && StringUtils.isNumeric(eventDate)) {
                        DateTime dateTime = new DateTime(dobString);
                        Date dob = dateTime.toDate();

                        long timeStamp = Long.valueOf(eventDate);
                        Date vaccineDate = new Date(timeStamp);

                        updateIndicators(getApplicationContext(), baseEntityId, dob, vaccineName, vaccineDate);
                    }

                    VaccinatorApplication.getInstance().context().allSharedPreferences().savePreference(COVERAGE_DROPOUT_VACCINATION_LAST_PROCESSED_DATE, updatedAt);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    public static void updateIndicators(Context context, String baseEntityId, Date dob, String vaccineName, Date vaccineDate) {
        if (context == null || StringUtils.isBlank(baseEntityId) || dob == null || StringUtils.isBlank(vaccineName) || vaccineDate == null) {
            return;
        }
        try {
            List<String> vaccines = formatAndSplitVaccineName(vaccineName);

            for (String vaccine : vaccines) {
                updateCohortIndicators(dob, baseEntityId, vaccine, vaccineDate);
                updateCumulativeIndicators(dob, baseEntityId, vaccine, vaccineDate);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            sendBroadcastMessage(context, CoverageDropoutBroadcastReceiver.TYPE_GENERATE_COHORT_INDICATORS);
            sendBroadcastMessage(context, CoverageDropoutBroadcastReceiver.TYPE_GENERATE_CUMULATIVE_INDICATORS);
        }
    }

    public static void unregister(Context context, String baseEntityId) {
        unregisterCohort(context, baseEntityId, null);
    }

    public static void unregister(Context context, String baseEntityId, String vaccineName) {
        unregisterCohort(context, baseEntityId, vaccineName);
        unregisterCumulative(context, baseEntityId, vaccineName);
    }

    private static void deleteCohortIndicator(String vaccine, Long cohortId) {
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

    ////////////////////////////////////////////////////////////////
    // COHORT
    ////////////////////////////////////////////////////////////////
    private static void updateCohortRegistrations(String baseEntityId, Date dob) {
        try {
            if (StringUtils.isBlank(baseEntityId) || dob == null) {
                return;
            }

            CohortRepository cohortRepository = VaccinatorApplication.getInstance().cohortRepository();
            CohortPatientRepository cohortPatientRepository = VaccinatorApplication.getInstance().cohortPatientRepository();

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

            CohortPatient cohortPatient = cohortPatientRepository.findByBaseEntityId(baseEntityId);
            if (cohortPatient == null) {
                cohortPatient = new CohortPatient();
                cohortPatient.setBaseEntityId(baseEntityId);
                cohortPatient.setCohortId(cohort.getId());
                cohortPatientRepository.add(cohortPatient);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

    }

    private static void updateCohortIndicators(Date dob, String baseEntityId, String vaccineName, Date vaccineDate) {
        try {
            if (StringUtils.isBlank(baseEntityId) || StringUtils.isBlank(vaccineName) || dob == null || vaccineDate == null) {
                return;
            }


            CohortPatientRepository cohortPatientRepository = VaccinatorApplication.getInstance().cohortPatientRepository();
            CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();

            CohortPatient cohortPatient = cohortPatientRepository.findByBaseEntityId(baseEntityId);
            if (cohortPatient == null) {
                return;
            }

            // Don't add an already counted vaccine unless it's invalid now
            boolean alreadyCounted = false;
            List<String> vaccineList = validVaccinesAsList(cohortPatient.getValidVaccines());
            if (!vaccineList.isEmpty() && vaccineList.contains(vaccineName)) {
                alreadyCounted = true;
            }

            // Don't add invalid vaccines to the indicator table
            boolean isValid = isVaccineValidForCohort(vaccineName, dob, vaccineDate);

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

            if (subtract) {
                vaccineList.remove(vaccineName);
            } else {
                vaccineList.add(vaccineName);
            }

            cohortPatientRepository.changeValidVaccines(StringUtils.join(vaccineList, COMMA), cohortPatient.getId());

            CohortIndicator cohortIndicator = cohortIndicatorRepository.findByVaccineAndCohort(vaccineName, cohortPatient.getCohortId());
            if (cohortIndicator == null) {
                if (subtract) {
                    throw CoverageDropoutException.newInstance("Impossible!!! subtract should only happen for already counted vaccines");
                }
                cohortIndicator = new CohortIndicator();
                cohortIndicator.setCohortId(cohortPatient.getCohortId());
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

    private static void unregisterCohort(Context context, String baseEntityId, String vaccineName) {

        CohortRepository cohortRepository = VaccinatorApplication.getInstance().cohortRepository();
        CohortPatientRepository cohortPatientRepository = VaccinatorApplication.getInstance().cohortPatientRepository();
        CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();

        if (cohortRepository == null || cohortPatientRepository == null || cohortIndicatorRepository == null || StringUtils.isBlank(baseEntityId) || StringUtils.isBlank(vaccineName)) {
            return;
        }

        try {
            CohortPatient cohortPatient = cohortPatientRepository.findByBaseEntityId(baseEntityId);
            if (cohortPatient == null) {
                return;
            }

            Long cohortId = cohortPatient.getCohortId();

            String validVaccines = cohortPatient.getValidVaccines();
            if (StringUtils.isNotBlank(validVaccines)) {
                List<String> vaccineList = validVaccinesAsList(validVaccines);

                if (StringUtils.isBlank(vaccineName)) { // Child Centered
                    for (String vaccine : vaccineList) { // Un register all vaccines
                        deleteCohortIndicator(vaccine, cohortId);
                    }
                } else { // Vaccine Centered
                    List<String> incomingVaccineList = formatAndSplitVaccineName(vaccineName);
                    for (String incomingVaccine : incomingVaccineList) { // Un register incoming vaccine only
                        if (vaccineList.contains(incomingVaccine)) {
                            deleteCohortIndicator(incomingVaccine, cohortId);
                        }
                    }

                    // Remove the vaccine in cohortPatient
                    boolean removed = vaccineList.removeAll(incomingVaccineList);
                    if (removed) {
                        cohortPatientRepository.changeValidVaccines(StringUtils.join(vaccineList, COMMA), cohortPatient.getId());
                    }
                }
            }

            if (StringUtils.isNotBlank(vaccineName)) { // Vaccine Centered - Return
                return;
            }

            cohortPatientRepository.delete(cohortPatient.getId());

            List<CohortPatient> otherChildren = cohortPatientRepository.findByCohort(cohortId);
            if (otherChildren == null || otherChildren.isEmpty()) {
                cohortRepository.delete(cohortId);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            sendBroadcastMessage(context, CoverageDropoutBroadcastReceiver.TYPE_GENERATE_COHORT_INDICATORS);
        }
    }

    ////////////////////////////////////////////////////////////////
    // CUMULATIVE
    ////////////////////////////////////////////////////////////////

    private static void updateCumulativeRegistrations(String baseEntityId, Date vaccineDate) {
        try {
            if (StringUtils.isBlank(baseEntityId) || vaccineDate == null) {
                return;
            }

            CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
            CumulativePatientRepository cumulativePatientRepository = VaccinatorApplication.getInstance().cumulativePatientRepository();

            Cumulative cumulative = cumulativeRepository.findByYear(vaccineDate);
            if (cumulative == null) {
                cumulative = new Cumulative();
                cumulative.setYear(vaccineDate);
                //TODO
                cumulative.setZeirNumber(0L);
                cumulativeRepository.add(cumulative);
                // Break if the cohort record cannot be added to the db
                if (cumulative.getId() == null || cumulative.getId().equals(-1L)) {
                    return;
                }
            }

            CumulativePatient cumulativePatient = cumulativePatientRepository.findByBaseEntityId(baseEntityId);
            if (cumulativePatient == null) {
                cumulativePatient = new CumulativePatient();
                cumulativePatient.setBaseEntityId(baseEntityId);
                cumulativePatientRepository.add(cumulativePatient);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static void updateCumulativeIndicators(Date dob, String baseEntityId, String vaccineName, Date vaccineDate) {
        try {
            if (StringUtils.isBlank(vaccineName) || dob == null || vaccineDate == null) {
                return;
            }

            CumulativePatientRepository cumulativePatientRepository = VaccinatorApplication.getInstance().cumulativePatientRepository();
            CumulativePatient cumulativePatient = cumulativePatientRepository.findByBaseEntityId(baseEntityId);
            Date oldDate = null;

            // Don't add an already counted vaccine unless it's invalid now
            boolean alreadyCounted = false;
            List<String> vaccineList = validVaccinesAsList(cumulativePatient.getValidVaccines());
            for (String validVaccine : vaccineList) {
                if (validVaccine.contains(vaccineName)) {
                    alreadyCounted = true;
                    oldDate = getDateFromValidVaccine(validVaccine);
                }
            }

            // Don't add invalid vaccines to the indicator table
            boolean isValid = isValidVaccineForCumulative(vaccineName, dob);

            boolean replace = false;
            boolean subtract = false;
            if (!isValid) {
                if (alreadyCounted) { // Already counted but now invalid - remove
                    subtract = true;
                } else {
                    return; // Not counted and invalid - no need to proceed
                }
            } else {
                if (alreadyCounted) { // Valid and Already counted - no need to proceed
                    replace = true;
                }
            }

            if (replace) { // Move from one culumative to another -- is valid and already counted
                if (oldDate != null && !Utils.isSameMonthAndYear(oldDate, vaccineDate)) { // If same month year, ignore
                    updateCumulativeIndicator(cumulativePatient, vaccineName, oldDate, true);
                    updateCumulativeIndicator(cumulativePatient, vaccineName, vaccineDate, false);
                }
            } else {
                updateCumulativeIndicator(cumulativePatient, vaccineName, oldDate != null ? oldDate : vaccineDate, subtract);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static void updateCumulativeIndicator(CumulativePatient cumulativePatient, String vaccineName, Date date, boolean subtract) {

        try {
            CumulativePatientRepository cumulativePatientRepository = VaccinatorApplication.getInstance().cumulativePatientRepository();
            CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();

            List<String> vaccineList = validVaccinesAsList(cumulativePatient.getValidVaccines());
            String validVaccine = vaccineName + COLON + date.getTime();
            if (subtract) {
                vaccineList.remove(validVaccine);
            } else {
                vaccineList.add(validVaccine);
            }

            cumulativePatientRepository.changeValidVaccines(StringUtils.join(vaccineList, COMMA), cumulativePatient.getId());

            CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
            Cumulative cumulative = cumulativeRepository.findByYear(date);

            if (cumulative == null) {
                return;
            }

            CumulativeIndicator cumulativeIndicator = cumulativeIndicatorRepository.findByVaccineMonthAndCumulativeId(vaccineName, date, cumulative.getId());
            if (cumulativeIndicator == null) {
                if (subtract) {
                    throw CoverageDropoutException.newInstance("Impossible!!! subtract should only happen for already counted vaccines");
                }
                cumulativeIndicator = new CumulativeIndicator();
                cumulativeIndicator.setCumulativeId(cumulative.getId());
                cumulativeIndicator.setValue(1L);
                cumulativeIndicator.setVaccine(vaccineName);
                cumulativeIndicator.setMonth(date);
                cumulativeIndicatorRepository.add(cumulativeIndicator);
            } else {
                Long value = cumulativeIndicator.getValue();
                if (subtract) {
                    if (value <= 0) {
                        return;
                    }
                    value -= 1L;
                } else {
                    value += 1L;
                }
                cumulativeIndicatorRepository.changeValue(value, cumulativeIndicator.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static void unregisterCumulative(Context context, String baseEntityId, String vaccineName) {

        CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
        CumulativePatientRepository cumulativePatientRepository = VaccinatorApplication.getInstance().cumulativePatientRepository();
        CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();

        if (cumulativeRepository == null || cumulativePatientRepository == null || cumulativeIndicatorRepository == null
                || StringUtils.isBlank(baseEntityId) || StringUtils.isBlank(vaccineName)) {
            return;
        }

        try {
            CumulativePatient cumulativePatient = cumulativePatientRepository.findByBaseEntityId(baseEntityId);
            if (cumulativePatient == null) {
                return;
            }

            String validVaccines = cumulativePatient.getValidVaccines();
            if (StringUtils.isNotBlank(validVaccines)) {
                List<String> vaccineList = validVaccinesAsList(validVaccines);

                // Vaccine Centered
                List<String> incomingVaccineList = formatAndSplitVaccineName(vaccineName);
                for (String validVaccine : vaccineList) {
                    for (String incomingVaccine : incomingVaccineList) { // Un register incoming vaccine only
                        if (validVaccine.contains(incomingVaccine)) {
                            Date oldEventDate = getDateFromValidVaccine(validVaccine);
                            deleteCumulativeIndicator(incomingVaccine, oldEventDate);
                        }
                    }
                }

                // Remove the vaccine in cohortPatient
                boolean removed = vaccineList.removeAll(incomingVaccineList);
                if (removed) {
                    cumulativePatientRepository.changeValidVaccines(StringUtils.join(vaccineList, COMMA), cumulativePatient.getId());
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            sendBroadcastMessage(context, CoverageDropoutBroadcastReceiver.TYPE_GENERATE_CUMULATIVE_INDICATORS);
        }
    }

    private static void deleteCumulativeIndicator(String vaccine, Date month) {

        CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
        CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();

        Cumulative cumulative = cumulativeRepository.findByYear(month);
        if (cumulative == null) {
            return;
        }

        CumulativeIndicator cumulativeIndicator = cumulativeIndicatorRepository.findByVaccineMonthAndCumulativeId(vaccine, month, cumulative.getId());
        if (cumulativeIndicator != null) {
            if (cumulativeIndicator.getValue() > 1) {
                long value = cumulativeIndicator.getValue() - 1L;
                cumulativeIndicatorRepository.changeValue(value, cumulativeIndicator.getId());
            } else {
                cumulativeIndicatorRepository.delete(cumulativeIndicator.getId());
            }
        }
    }

    ////////////////////////////////////////////////////////////////
    // UTILITY
    ////////////////////////////////////////////////////////////////

    private static boolean isVaccineValidForCohort(String vaccine, Date dob, Date eventDate) {
        Date endDate = Utils.getCohortEndDate(vaccine, dob);
        return DateUtils.isSameDay(endDate, eventDate) || endDate.after(eventDate);
    }

    private static boolean isValidVaccineForCumulative(String vaccine, Date dob) {
        if (StringUtils.isBlank(vaccine) || dob == null) {
            return false;
        }
        LocalDate birthdate = LocalDate.fromDateFields(dob);
        LocalDate now = new LocalDate();
        Months months = Months.monthsBetween(birthdate, now);
        int ageInMonths = months.getMonths();

        final String measles2 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.measles2.display().toLowerCase());

        if (vaccine.equals(measles2)) {
            if (ageInMonths <= 24) {
                return true;
            }
        } else {
            if (ageInMonths <= 12) {
                return true;
            }
        }
        return false;
    }

    private String filterKey(String query) {
        if (StringUtils.containsIgnoreCase(query, "where")) {
            return " AND ";
        }
        return " WHERE ";
    }

    private static List<String> formatAndSplitVaccineName(String vaccineName) {
        final String forwardSlash = "/";
        if (StringUtils.isBlank(vaccineName)) {
            return new ArrayList<>();
        }

        List<String> vaccineList = new ArrayList<>();
        if (vaccineName.contains(forwardSlash)) {
            String[] vaccines = vaccineName.split(forwardSlash);
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
        String[] vaccines = validVaccines.split(COMMA);
        return new ArrayList<>(Arrays.asList(vaccines));
    }

    private static Date getDateFromValidVaccine(String validVaccine) {
        if (StringUtils.isNotBlank(validVaccine) && validVaccine.contains(COLON)) {
            String timeStamp = validVaccine.split(COLON)[1];
            if (StringUtils.isNumeric(timeStamp)) {
                return new Date(Long.valueOf(timeStamp));
            }
        }
        return null;
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
