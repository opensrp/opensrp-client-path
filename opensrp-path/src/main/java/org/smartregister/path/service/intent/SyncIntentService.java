package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.Response;
import org.smartregister.growthmonitoring.service.intent.ZScoreRefreshIntentService;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.path.view.LocationPickerView;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.service.HTTPAgent;
import org.smartregister.util.Utils;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import util.NetworkUtils;

public class SyncIntentService extends IntentService {
    private static final String EVENTS_SYNC_PATH = "/rest/event/add";
    private static final String REPORTS_SYNC_PATH = "/rest/report/add";

    private Context context;
    private HTTPAgent httpAgent;

    public static final int EVENT_FETCH_LIMIT = 50;
    public static final String START_SYNC_TIMESTAMP = "startSyncTimeStamp";
    public static final String LAST_SYNC_TIMESTAMP = "lastSyncTimeStamp";
    public static final String FETCH_STATUS = "fetchStatus";


    public SyncIntentService() {
        super("SyncIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getBaseContext();
        httpAgent = VaccinatorApplication.getInstance().context().getHttpAgent();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        sendSyncStatusBroadcastMessage(FetchStatus.fetchStarted);
        if (VaccinatorApplication.getInstance().context().IsUserLoggedOut()) {
            drishtiLogInfo("Not updating from server as user is not logged in.");
            return;
        }

        FetchStatus fetchStatus = doSync();

        startZscoreRefresh();

        if (fetchStatus.equals(FetchStatus.nothingFetched) || fetchStatus.equals(FetchStatus.fetched)) {
            ECSyncUpdater ecSyncUpdater = ECSyncUpdater.getInstance(context);
            ecSyncUpdater.updateLastCheckTimeStamp(Calendar.getInstance().getTimeInMillis());
        }

        startProcessClient(fetchStatus);
    }

    private FetchStatus doSync() {
        if (!NetworkUtils.isNetworkAvailable()) {
            return FetchStatus.noConnection;
        }

        try {
            // Fetch locations
            String locations = Utils.getPreference(context, LocationPickerView.PREF_TEAM_LOCATIONS, "");
            if (StringUtils.isBlank(locations)) {
                return FetchStatus.fetchedFailed;
            }

            pushToServer();
            return pullECFromServer(locations);

        } catch (Exception e) {
            Log.e(getClass().getName(), "", e);
            return FetchStatus.fetchedFailed;
        }

    }

    private FetchStatus pullECFromServer(String locations) throws Exception {
        int totalCount = 0;
        ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        while (true) {
            long startSyncTimeStamp = ecUpdater.getLastSyncTimeStamp();
            int eCount = ecUpdater.fetchAllClientsAndEvents(AllConstants.SyncFilters.FILTER_LOCATION_ID, locations);
            totalCount += eCount;
            if (eCount < 0) {
                return FetchStatus.fetchedFailed;
            } else if (eCount == 0) {
                break;
            }

            Log.i(getClass().getName(), "Sync count:  " + eCount);

            long lastSyncTimeStamp = ecUpdater.getLastSyncTimeStamp();
            startProcessClient(startSyncTimeStamp, lastSyncTimeStamp);

            Thread.sleep(1000);
        }


        if (totalCount == 0) {
            return FetchStatus.nothingFetched;
        } else if (totalCount < 0) {
            return FetchStatus.fetchedFailed;
        } else {
            return FetchStatus.fetched;
        }
    }

    private void pushToServer() {
        pushECToServer();
        pushReportsToServer();
        startSyncValidation();
    }

    private void pushECToServer() {
        EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();
        boolean keepSyncing = true;

        while (keepSyncing) {
            try {
                Map<String, Object> pendingEvents = db.getUnSyncedEvents(EVENT_FETCH_LIMIT);

                if (pendingEvents.isEmpty()) {
                    return;
                }

                String baseUrl = VaccinatorApplication.getInstance().context().configuration().dristhiBaseURL();
                if (baseUrl.endsWith(context.getString(R.string.url_separator))) {
                    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(context.getString(R.string.url_separator)));
                }
                // create request body
                JSONObject request = new JSONObject();
                if (pendingEvents.containsKey(context.getString(R.string.clients_key))) {
                    request.put(context.getString(R.string.clients_key), pendingEvents.get(context.getString(R.string.clients_key)));
                }
                if (pendingEvents.containsKey(context.getString(R.string.events_key))) {
                    request.put(context.getString(R.string.events_key), pendingEvents.get(context.getString(R.string.events_key)));
                }
                String jsonPayload = request.toString();
                Response<String> response = httpAgent.post(
                        MessageFormat.format("{0}/{1}",
                                baseUrl,
                                EVENTS_SYNC_PATH),
                        jsonPayload);
                if (response.isFailure()) {
                    Log.e(getClass().getName(), "Events sync failed.");
                    return;
                }
                db.markEventsAsSynced(pendingEvents);
                Log.i(getClass().getName(), "Events synced successfully.");
            } catch (Exception e) {
                Log.e(getClass().getName(), e.getMessage());
            }
        }


    }

    private void pushReportsToServer() {
        EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();
        try {
            boolean keepSyncing = true;
            int limit = 50;
            while (keepSyncing) {
                List<JSONObject> pendingReports = db.getUnSyncedReports(limit);

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
                db.markReportsAsSynced(pendingReports);
                Log.i(getClass().getName(), "Reports synced successfully.");
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), e.getMessage());
        }
    }

    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus) {
        Intent intent = new Intent();
        intent.setAction(SyncStatusBroadcastReceiver.ACTION_SYNC_STATUS);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_FETCH_STATUS, fetchStatus);
        sendBroadcast(intent);
    }

    private void startProcessClient(long start, long end) {
        Intent intent = new Intent(context, ProcessClientIntentService.class);
        intent.putExtra(START_SYNC_TIMESTAMP, start);
        intent.putExtra(LAST_SYNC_TIMESTAMP, end);
        startService(intent);
    }

    private void startProcessClient(FetchStatus fetchStatus) {
        Intent intent = new Intent(context, ProcessClientIntentService.class);
        intent.putExtra(FETCH_STATUS, fetchStatus);
        startService(intent);
    }

    private void startSyncValidation() {
        Intent intent = new Intent(context, ValidateIntentService.class);
        startService(intent);
    }

    private void startZscoreRefresh() {
        Intent intent = new Intent(context, ZScoreRefreshIntentService.class);
        startService(intent);
    }

    private void drishtiLogInfo(String message) {
        org.smartregister.util.Log.logInfo(message);
    }


}
