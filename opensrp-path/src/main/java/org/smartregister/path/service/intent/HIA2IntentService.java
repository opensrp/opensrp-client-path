package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Response;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.MonthlyTally;
import org.smartregister.path.domain.ReportHia2Indicator;
import org.smartregister.path.receiver.Hia2ServiceBroadcastReceiver;
import org.smartregister.path.repository.DailyTalliesRepository;
import org.smartregister.path.repository.MonthlyTalliesRepository;
import org.smartregister.path.service.HIA2Service;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.service.HTTPAgent;
import org.smartregister.util.Utils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import util.PathConstants;
import util.ReportUtils;


/**
 * Created by onamacuser on 18/03/2016.
 */
public class HIA2IntentService extends IntentService {
    private static final String TAG = HIA2IntentService.class.getCanonicalName();
    public static final String GENERATE_REPORT = "GENERATE_REPORT";
    public static final String REPORT_MONTH = "REPORT_MONTH";
    private DailyTalliesRepository dailyTalliesRepository;
    private MonthlyTalliesRepository monthlyTalliesRepository;
    private EventClientRepository eventClientRepository;
    private HIA2Service hia2Service;

    //HIA2 Status
    private VaccineRepository vaccineRepository;
    private static final int DAYS_BEFORE_OVERDUE = 10;
    private Context context;

    public HIA2IntentService() {
        super("HIA2IntentService");
    }

    /**
     * Build indicators,save them to the db and generate report
     *
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Started HIA2 service");
        try {
            boolean generateReport = intent.getBooleanExtra(GENERATE_REPORT, false);
            if (!generateReport) {
                //Update H1A2 status (Within or Overdue)
                updateVaccineHIA2Status();

                // Generate daily HIA2 indicators
                generateDailyHia2Indicators();

                // Send broadcast message
                sendBroadcastMessage(Hia2ServiceBroadcastReceiver.TYPE_GENERATE_DAILY_INDICATORS);
            } else {
                String monthString = intent.getStringExtra(REPORT_MONTH);
                if (!TextUtils.isEmpty(monthString)) {
                    Date month = HIA2Service.dfyymm.parse(monthString);
                    generateMonthlyReport(month);
                    sendBroadcastMessage(Hia2ServiceBroadcastReceiver.TYPE_GENERATE_MONTHLY_REPORT);
                }
            }

            // Push all reports to server
            pushReportsToServer();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        Log.i(TAG, "Finishing HIA2 service");
    }

    private void sendBroadcastMessage(String type) {
        Intent intent = new Intent();
        intent.setAction(Hia2ServiceBroadcastReceiver.ACTION_SERVICE_DONE);
        intent.putExtra(Hia2ServiceBroadcastReceiver.TYPE, type);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dailyTalliesRepository = VaccinatorApplication.getInstance().dailyTalliesRepository();
        monthlyTalliesRepository = VaccinatorApplication.getInstance().monthlyTalliesRepository();
        eventClientRepository = VaccinatorApplication.getInstance().eventClientRepository();
        hia2Service = new HIA2Service();

        vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
        context = getBaseContext();

        return super.onStartCommand(intent, flags, startId);
    }

    private void generateDailyHia2Indicators() {
        try {

            SQLiteDatabase db = VaccinatorApplication.getInstance().getRepository().getWritableDatabase();
            //get previous dates if shared preferences is null meaning reports for previous months haven't been generated
            String lastProcessedDate = VaccinatorApplication.getInstance().context().allSharedPreferences().getPreference(HIA2Service.HIA2_LAST_PROCESSED_DATE);
            ArrayList<HashMap<String, String>> reportDates;
            if (lastProcessedDate == null || lastProcessedDate.isEmpty()) {
                reportDates = eventClientRepository.rawQuery(db, HIA2Service.PREVIOUS_REPORT_DATES_QUERY.concat(" order by eventDate asc"));

            } else {
                reportDates = eventClientRepository.rawQuery(db, HIA2Service.PREVIOUS_REPORT_DATES_QUERY.concat(" where " + EventClientRepository.event_column.updatedAt + " >'" + lastProcessedDate + "'" + " order by eventDate asc"));
            }
            String userName = VaccinatorApplication.getInstance().context().allSharedPreferences().fetchRegisteredANM();
            for (Map<String, String> dates : reportDates) {
                String date = dates.get(EventClientRepository.event_column.eventDate.name());
                String updatedAt = dates.get(EventClientRepository.event_column.updatedAt.name());

                Map<String, Object> hia2Report = hia2Service.generateIndicators(db, date);
                dailyTalliesRepository.save(date, hia2Report);
                VaccinatorApplication.getInstance().context().allSharedPreferences().savePreference(HIA2Service.HIA2_LAST_PROCESSED_DATE, updatedAt);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

    }

    private void generateMonthlyReport(Date month) {
        try {
            if (month != null) {
                List<MonthlyTally> tallies = monthlyTalliesRepository
                        .find(MonthlyTalliesRepository.DF_YYYYMM.format(month));
                if (tallies != null) {
                    List<ReportHia2Indicator> tallyReports = new ArrayList<>();
                    for (MonthlyTally curTally : tallies) {
                        tallyReports.add(curTally.getReportHia2Indicator());
                    }

                    ReportUtils.createReport(this, tallyReports, month, HIA2Service.REPORT_NAME);

                    for (MonthlyTally curTally : tallies) {
                        curTally.setDateSent(Calendar.getInstance().getTime());
                        monthlyTalliesRepository.save(curTally);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void updateVaccineHIA2Status() {
        try {
            List<Vaccine> vaccines = vaccineRepository.findWithNullHia2Status();
            if (!vaccines.isEmpty()) {
                for (Vaccine vaccine : vaccines) {
                    Long daysAfter = countDaysAfterDueDate(vaccine);
                    if (daysAfter == null) {
                        continue;
                    }
                    String hia2Status;
                    if (daysAfter <= DAYS_BEFORE_OVERDUE) {
                        hia2Status = VaccineRepository.HIA2_Within;
                    } else {
                        hia2Status = VaccineRepository.HIA2_Overdue;
                    }
                    vaccineRepository.updateHia2Status(vaccine, hia2Status);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private Long countDaysAfterDueDate(Vaccine vaccine) {

        CommonRepository commonRepository = VaccinatorApplication.getInstance().context().commonrepository(PathConstants.CHILD_TABLE_NAME);
        if (vaccine == null || vaccine.getBaseEntityId() == null || vaccine.getDate() == null) {
            return null;
        }

        if (vaccineRepository == null || commonRepository == null) {
            return null;
        }

        CommonPersonObject rawDetails = commonRepository.findByBaseEntityId(vaccine.getBaseEntityId());
        if (rawDetails == null) {
            return null;
        }

        CommonPersonObjectClient childDetails = org.smartregister.util.Utils.convert(rawDetails);
        List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

        DateTime dateTime = Utils.dobToDateTime(childDetails);
        if (dateTime == null) {
            return null;
        }

        Date dob = dateTime.toDate();

        Date dueDate = getDueVaccineDate(vaccine, vaccineList, dob);
        Date doneDate = vaccine.getDate();

        if (dueDate == null) {
            return null;
        }

        long diff = doneDate.getTime() - dueDate.getTime();
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        Log.i(TAG, "Days: " + TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
        return days;

    }

    private Date getDueVaccineDate(Vaccine vaccine, List<Vaccine> issuedVaccines, Date dateOfBirth) {
        VaccineSchedule curVaccineSchedule = VaccineSchedule.getVaccineSchedule("child",
                vaccine.getName());
        Date minDate = null;

        if (curVaccineSchedule != null) {
            minDate = curVaccineSchedule.getDueDate(issuedVaccines, dateOfBirth);
        }

        return minDate;
    }

    private void pushReportsToServer() {
        final String REPORTS_SYNC_PATH = "/rest/report/add";
        HTTPAgent httpAgent = VaccinatorApplication.getInstance().context().getHttpAgent();
        try {
            boolean keepSyncing = true;
            int limit = 50;
            while (keepSyncing) {
                List<JSONObject> pendingReports = eventClientRepository.getUnSyncedReports(limit);

                if (pendingReports.isEmpty()) {
                    return;
                }

                String baseUrl = VaccinatorApplication.getInstance().context().configuration().dristhiBaseURL();
                if (baseUrl.endsWith(context.getString(R.string.url_separator))) {
                    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(context.getString(R.string.url_separator)));
                }
                // create request body
                JSONObject request = new JSONObject();

                request.put("reports", pendingReports);
                String jsonPayload = request.toString();
                Response<String> response = httpAgent.post(
                        MessageFormat.format("{0}/{1}",
                                baseUrl,
                                REPORTS_SYNC_PATH),
                        jsonPayload);
                if (response.isFailure()) {
                    Log.e(getClass().getName(), "Reports sync failed.");
                    return;
                }
                eventClientRepository.markReportsAsSynced(pendingReports);
                Log.i(getClass().getName(), "Reports synced successfully.");
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), e.getMessage());
        }
    }

}
