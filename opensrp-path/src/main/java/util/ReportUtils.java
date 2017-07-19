package util;

import android.content.Context;
import android.util.Log;

import org.opensrp.path.domain.Report;
import org.opensrp.path.domain.ReportHia2Indicator;
import org.opensrp.path.sync.ECSyncUpdater;
import org.joda.time.DateTime;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 08/02/2017.
 */
public class ReportUtils {
    private static final String TAG = ReportUtils.class.getCanonicalName();


    public static void createReport(Context context, List<ReportHia2Indicator> hia2Indicators, Date month, String reportType) {
        try {
            ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

            String providerId = org.opensrp.Context.getInstance().allSharedPreferences().fetchRegisteredANM();
            String locationId = org.opensrp.Context.getInstance().allSharedPreferences().getPreference(PathConstants.CURRENT_LOCATION_ID);
            Report report = new Report();
            report.setFormSubmissionId(JsonFormUtils.generateRandomUUIDString());
            report.setHia2Indicators(hia2Indicators);
            report.setLocationId(locationId);
            report.setProviderId(providerId);

            // Get the second last day of the month
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(month);
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - 2);

            report.setReportDate(new DateTime(calendar.getTime()));
            report.setReportType(reportType);
            JSONObject reportJson = new JSONObject(JsonFormUtils.gson.toJson(report));
            ecUpdater.addReport(reportJson);

        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
    }


}
