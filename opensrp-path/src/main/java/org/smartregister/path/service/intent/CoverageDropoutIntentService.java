package org.smartregister.path.service.intent;

import android.app.IntentService;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import util.PathConstants;


/**
 * Created by onamacuser on 18/03/2016.
 */
public class CoverageDropoutIntentService extends IntentService {
    private static final String TAG = CoverageDropoutIntentService.class.getCanonicalName();
    private CohortRepository cohortRepository;
    private CohortIndicatorRepository cohortIndicatorRepository;
    private ChildReportRepository childReportRepository;
    private EventClientRepository eventClientRepository;

    private static final String QUERY = "SELECT " +
            "v." + VaccineRepository.BASE_ENTITY_ID +
            ", c." + PathConstants.EC_CHILD_TABLE.DOB +
            ", v." + VaccineRepository.NAME +
            ", v." + VaccineRepository.DATE +
            ", v." + VaccineRepository.UPDATED_AT_COLUMN +
            " FROM " + VaccineRepository.VACCINE_TABLE_NAME + " v  " +
            " LEFT JOIN " + PathConstants.CHILD_TABLE_NAME + " c  " +
            " ON v." + VaccineRepository.BASE_ENTITY_ID + " = c." + PathConstants.EC_CHILD_TABLE.BASE_ENTITY_ID;

    public static final String COVERAGE_DROPOUT_LAST_PROCESSED_DATE = "COVERAGE_DROPOUT_LAST_PROCESSED_DATE";

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
            generateIndicators();
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

    private void generateIndicators() {
        try {
            final String updatedAtColumn = " v." + VaccineRepository.UPDATED_AT_COLUMN;
            final String orderByClause = " ORDER BY " + updatedAtColumn + " ASC ";
            SQLiteDatabase db = VaccinatorApplication.getInstance().getRepository().getWritableDatabase();
            //get previous dates if shared preferences is null meaning reports for previous months haven't been generated
            String lastProcessedDate = VaccinatorApplication.getInstance().context().allSharedPreferences().getPreference(COVERAGE_DROPOUT_LAST_PROCESSED_DATE);
            ArrayList<HashMap<String, String>> results;
            if (StringUtils.isBlank(lastProcessedDate)) {
                results = eventClientRepository.rawQuery(db, QUERY.concat(orderByClause));

            } else {
                results = eventClientRepository.rawQuery(db, QUERY.concat(" AND " + updatedAtColumn + " > " + lastProcessedDate).concat(orderByClause));
            }
            for (Map<String, String> result : results) {
                String baseEntityId = result.get(VaccineRepository.BASE_ENTITY_ID);
                String dobString = result.get(PathConstants.EC_CHILD_TABLE.DOB);
                String vaccineName = result.get(VaccineRepository.NAME);
                String updatedAt = result.get(VaccineRepository.UPDATED_AT_COLUMN);

                String eventDate = result.get(VaccineRepository.DATE);
                if (StringUtils.isNotBlank(eventDate) && StringUtils.isNumeric(eventDate)) {
                    long timeStamp = Long.valueOf(eventDate);
                    processCohort(baseEntityId, dobString, vaccineName, new Date(timeStamp));
                }


                VaccinatorApplication.getInstance().context().allSharedPreferences().savePreference(COVERAGE_DROPOUT_LAST_PROCESSED_DATE, updatedAt);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            sendBroadcastMessage(CoverageDropoutBroadcastReceiver.TYPE_GENERATE_COHORT_INDICATORS);
        }
    }

    private void processCohort(String baseEntityId, String dobString, String vaccineName, Date vaccineDate) {
        try {
            if (StringUtils.isBlank(baseEntityId) || StringUtils.isBlank(vaccineName) || StringUtils.isBlank(dobString)) {
                return;
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
                    return;
                }
            }

            ChildReport childReport = childReportRepository.findByBaseEntityIdAndCohort(baseEntityId, cohort.getId());
            if (childReport == null) {
                childReport = new ChildReport();
                childReport.setBaseEntityId(baseEntityId);
                childReport.setCohortId(cohort.getId());
                childReportRepository.add(childReport);

                // Break if the child report record cannot be added to the db
                if (childReport.getId() == null || childReport.getId().equals(-1L)) {
                    return;
                }
            }

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

            CohortIndicator cohortIndicator = cohortIndicatorRepository.findByVaccineAndCohort(vaccineName, cohort.getId());
            if (cohortIndicator == null) {
                cohortIndicator = new CohortIndicator();
                cohortIndicator.setCohortId(cohort.getId());
                cohortIndicator.setValue(1L);
                cohortIndicator.setVaccine(vaccineName);

                Date endDate = getEndDate(vaccineName, getLastDayOfMonth(cohort.getMonthAsDate()));
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endDate);
                calendar.add(Calendar.DATE, 1);
                cohortIndicator.setEndDate(calendar.getTime());
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
        Date endDate = getEndDate(vaccine, dob);
        return DateUtils.isSameDay(endDate, eventDate) || endDate.after(eventDate);
    }

    private Date getEndDate(String vaccine, Date startDate) {

        Calendar endDateCalendar = Calendar.getInstance();
        endDateCalendar.setTime(startDate);

        final String opv0 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.opv0.display().toLowerCase());

        final String rota1 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.rota1.display().toLowerCase());
        final String rota2 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.rota2.display().toLowerCase());

        final String measles2 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.measles2.display().toLowerCase());
        final String mr2 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.mr2.display().toLowerCase());

        if (vaccine.equals(opv0)) {
            endDateCalendar.add(Calendar.DATE, 13);
        } else if (vaccine.equals(rota1) || vaccine.equals(rota2)) {
            endDateCalendar.add(Calendar.MONTH, 8);
        } else if (vaccine.equals(measles2) || vaccine.equals(mr2)) {
            endDateCalendar.add(Calendar.YEAR, 2);
        } else {
            endDateCalendar.add(Calendar.YEAR, 1);
        }

        return endDateCalendar.getTime();
    }

    private Date getLastDayOfMonth(Date month) {
        Calendar c = Calendar.getInstance();
        c.setTime(month);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return c.getTime();
    }
}
