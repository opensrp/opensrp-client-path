package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.db.Event;
import org.smartregister.domain.db.Obs;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.ChildReport;
import org.smartregister.path.domain.Cohort;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.path.repository.ChildReportRepository;
import org.smartregister.path.repository.CohortIndicatorRepository;
import org.smartregister.path.repository.CohortRepository;
import org.smartregister.repository.EventClientRepository;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String QUERY = "SELECT " +
            "e." + EventClientRepository.event_column.baseEntityId.name() +
            ", c." + EventClientRepository.client_column.birthdate.name() +
            ", e." + EventClientRepository.event_column.eventType.name() +
            ", e." + EventClientRepository.event_column.json.name() +
            ", e." + EventClientRepository.event_column.eventDate.name() +
            ", e." + EventClientRepository.event_column.updatedAt.name() +
            " FROM " + EventClientRepository.Table.event.name() + " e " +
            " LEFT JOIN " + EventClientRepository.Table.client.name() + " c " +
            " ON e." + EventClientRepository.event_column.baseEntityId.name() + " = c." + EventClientRepository.client_column.baseEntityId.name() +
            " WHERE e." + EventClientRepository.event_column.eventType.name() + " = '" + PathConstants.EventType.VACCINATION + "' ";
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

    private void generateIndicators() {
        try {
            final String updatedAtColumn = " e." + EventClientRepository.event_column.updatedAt;
            final String orderByClause = " ORDER BY " + updatedAtColumn + " ASC ";
            final String limitClause = " limit 100 ";
            SQLiteDatabase db = VaccinatorApplication.getInstance().getRepository().getWritableDatabase();
            //get previous dates if shared preferences is null meaning reports for previous months haven't been generated
            String lastProcessedDate = VaccinatorApplication.getInstance().context().allSharedPreferences().getPreference(COVERAGE_DROPOUT_LAST_PROCESSED_DATE);
            ArrayList<HashMap<String, String>> results;
            if (lastProcessedDate == null || lastProcessedDate.isEmpty()) {
                results = eventClientRepository.rawQuery(db, QUERY.concat(orderByClause).concat(limitClause));

            } else {
                results = eventClientRepository.rawQuery(db, QUERY.concat(" AND " + updatedAtColumn + " > '" + lastProcessedDate + "'").concat(orderByClause).concat(limitClause));
            }
            for (Map<String, String> result : results) {
                String baseEntityId = result.get(EventClientRepository.event_column.baseEntityId.name());
                String birthDate = result.get(EventClientRepository.client_column.birthdate.name());
                String eventType = result.get(EventClientRepository.event_column.eventType.name());
                String json = result.get(EventClientRepository.event_column.json.name());
                String eventDate = result.get(EventClientRepository.event_column.eventDate.name());
                String updatedAt = result.get(EventClientRepository.event_column.updatedAt.name());

                processCohort(baseEntityId, birthDate, eventType, eventDate, json);

                VaccinatorApplication.getInstance().context().allSharedPreferences().savePreference(COVERAGE_DROPOUT_LAST_PROCESSED_DATE, updatedAt);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void processCohort(String baseEntityId, String birthDate, String eventType, String eventDate, String json) {
        try {
            if (StringUtils.isBlank(baseEntityId) || StringUtils.isBlank(birthDate) || StringUtils.isBlank(eventType) || StringUtils.isBlank(eventDate) || StringUtils.isBlank(json)) {
                return;
            }

            Date dob = dateFormat.parse(birthDate);
            Cohort cohort = cohortRepository.findByMonth(dob);
            if (cohort == null) {
                cohort = new Cohort();
                cohort.setMonth(dob);
                cohortRepository.add(cohort);

                if (cohort.getId() == null || cohort.getId().equals(-1L)) {
                    return;
                }
            }

            Event vaccineEvent;
            try {
                vaccineEvent = eventClientRepository.convert(new JSONObject(json), Event.class);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            Obs obs = vaccineEvent.findObs(null, true, PathConstants.CONCEPT.VACCINE_DATE);
            if (obs == null) {
                return;
            }
            String vaccine = obs.getFormSubmissionField();

            if (!isValidVaccine(vaccine, dob, dateFormat.parse(eventDate))) {
                return;
            }

            ChildReport childReport = childReportRepository.findByBaseEntityId(baseEntityId);
            if (childReport == null) {
                childReport = new ChildReport();
                childReport.setBaseEntityId(baseEntityId);
                childReport.setCohortId(cohort.getId());
                childReport.setValidVaccines(vaccine);
                childReportRepository.add(childReport);

                if (childReport.getId() == null || childReport.getId().equals(-1L)) {
                    return;
                }

            } else {
                String validVaccines = childReport.getValidVaccines().concat(",").concat(vaccine);
                childReportRepository.changeValidVaccines(validVaccines, childReport.getId());
                childReport.setValidVaccines(validVaccines);
            }

            CohortIndicator cohortIndicator = cohortIndicatorRepository.findByVaccineAndCohort(vaccine, cohort.getId());
            if (cohortIndicator == null) {
                cohortIndicator = new CohortIndicator();
                cohortIndicator.setCohortId(cohort.getId());
                cohortIndicator.setValue(1L);
                cohortIndicator.setVaccine(vaccine);

                Date endDate = getEndDate(vaccine, getLastDayOfMonth(cohort.getMonthAsDate()));
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endDate);
                calendar.add(Calendar.DATE, 1);
                cohortIndicator.setEndDate(calendar.getTime());
            } else {
                cohortIndicator.setValue(cohortIndicator.getValue() + 1L);
            }
            cohortIndicatorRepository.add(cohortIndicator);

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
