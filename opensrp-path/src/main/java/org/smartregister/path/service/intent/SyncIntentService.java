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
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.path.sync.PathClientProcessor;
import org.smartregister.path.view.LocationPickerView;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.service.HTTPAgent;
import org.smartregister.util.Utils;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Map;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import util.NetworkUtils;

public class SyncIntentService extends IntentService {
    private static final String EVENTS_SYNC_PATH = "/rest/event/add";

    private Context context;
    private HTTPAgent httpAgent;

    public static final int EVENT_FETCH_LIMIT = 50;

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

        if (fetchStatus.equals(FetchStatus.nothingFetched) || fetchStatus.equals(FetchStatus.fetched)) {
            ECSyncUpdater ecSyncUpdater = ECSyncUpdater.getInstance(context);
            ecSyncUpdater.updateLastCheckTimeStamp(Calendar.getInstance().getTimeInMillis());
        }

        sendSyncStatusBroadcastMessage(fetchStatus, true);
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
        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        while (true) {
            final long startSyncTimeStamp = ecUpdater.getLastSyncTimeStamp();
            Timer fetchTimer = new Timer();
            fetchTimer.start();
            int eCount = ecUpdater.fetchAllClientsAndEvents(AllConstants.SyncFilters.FILTER_LOCATION_ID, locations);
            fetchTimer.stop();
            fetchTimer.logDuration("Fetch clients and events");
            totalCount += eCount;
            if (eCount < 0) {
                return FetchStatus.fetchedFailed;
            } else if (eCount == 0) {
                break;
            }

            Log.i(getClass().getName(), "Sync count:  " + eCount);

            final long lastSyncTimeStamp = ecUpdater.getLastSyncTimeStamp();

            final Subscription subsc = Observable.
                    just("")
                    .subscribeOn(Schedulers.io())
                    .map(new Func1<String, Object>() {
                        @Override
                        public Object call(String s) {
                            processClients(ecUpdater, startSyncTimeStamp, lastSyncTimeStamp);
                            return null;
                        }
                    })
                    .subscribe(
                            new Action1<Object>() {
                                @Override
                                public void call(Object o) {
                                }
                            }
                    );
        }

        if (totalCount == 0) {
            return FetchStatus.nothingFetched;
        } else if (totalCount < 0) {
            return FetchStatus.fetchedFailed;
        } else {
            return FetchStatus.fetched;
        }
    }

    private void processClients(ECSyncUpdater ecUpdater, long startSyncTimeStamp, long lastSyncTimeStamp) {

        try {
            Timer processClients = new Timer();
            processClients.start();
            PathClientProcessor.getInstance(context).processClient(ecUpdater.allEvents(startSyncTimeStamp, lastSyncTimeStamp));
            processClients.stop();
            processClients.logDuration("Process clients");
            sendSyncStatusBroadcastMessage(FetchStatus.fetched);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void pushToServer() {
        pushECToServer();
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

    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus, boolean isComplete) {
        Intent intent = new Intent();
        intent.setAction(SyncStatusBroadcastReceiver.ACTION_SYNC_STATUS);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_FETCH_STATUS, fetchStatus);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_COMPLETE_STATUS, isComplete);
        sendBroadcast(intent);
    }

    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus) {
        sendSyncStatusBroadcastMessage(fetchStatus, false);
    }

    private void drishtiLogInfo(String message) {
        org.smartregister.util.Log.logInfo(message);
    }


    public static class Timer {
        private long startTime;
        private long stopTime = 0l;
        private boolean stopped = false;

        public void start() {
            startTime = System.currentTimeMillis();
        }

        public long stop() {
            stopTime = System.currentTimeMillis();
            stopped = true;
            return (stopTime - startTime);
        }

        public long getDuration() {
            return (stopped) ? (stopTime - startTime) : System.currentTimeMillis() - startTime;
        }

        public void logDuration(String durationName) {
            Log.e("TIMER ", durationName + ": " + getDuration());
        }
    }

}
