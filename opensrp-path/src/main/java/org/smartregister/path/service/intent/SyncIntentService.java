package org.smartregister.path.service.intent;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import util.NetworkUtils;

public class SyncIntentService extends Service {
    private static final String EVENTS_SYNC_PATH = "/rest/event/add";

    private Context context;
    private HTTPAgent httpAgent;

    public static final int EVENT_FETCH_LIMIT = 50;

    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private static boolean started = false;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (!started) {
                handleSync();
            }

            stopSelf(message.arg1);

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread = new HandlerThread("SyncIntentService.HandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

        context = getBaseContext();
        httpAgent = VaccinatorApplication.getInstance().context().getHttpAgent();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mHandlerThread.quit();
    }


    protected void handleSync() {

        sendSyncStatusBroadcastMessage(FetchStatus.fetchStarted);
        if (VaccinatorApplication.getInstance().context().IsUserLoggedOut()) {
            drishtiLogInfo("Not updating from server as user is not logged in.");
            return;
        }

        doSync();
    }

    private void doSync() {
        if (!NetworkUtils.isNetworkAvailable()) {
            sendSyncStatusBroadcastMessage(FetchStatus.noConnection);
        }

        try {
            pushToServer();
            pullECFromServer();

        } catch (Exception e) {
            Log.e(getClass().getName(), "", e);
            sendSyncStatusBroadcastMessage(FetchStatus.fetchedFailed);
        }
    }

    private void pullECFromServer() throws Exception {
        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        // Fetch locations
        String locations = Utils.getPreference(context, LocationPickerView.PREF_TEAM_LOCATIONS, "");
        if (StringUtils.isBlank(locations)) {
            sendSyncStatusBroadcastMessage(FetchStatus.fetchedFailed);
        }

        Observable.just(locations)
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(@NonNull String locations) throws Exception {

                        JSONObject jsonObject = ecUpdater.fetchAsJsonObject(AllConstants.SyncFilters.FILTER_LOCATION_ID, locations);
                        if (jsonObject == null) {
                            return Observable.just(FetchStatus.fetchedFailed);
                        } else {
                            int eCount = jsonObject.has("no_of_events") ? jsonObject.getInt("no_of_events") : 0;
                            if (eCount < 0) {
                                return Observable.just(FetchStatus.fetchedFailed);
                            } else if (eCount == 0) {
                                return Observable.just(FetchStatus.nothingFetched);
                            } else {
                                return Observable.just(jsonObject)
                                        .subscribeOn(Schedulers.io())
                                        .map(new Function<JSONObject, Object>() {
                                            @Override
                                            public Object apply(@NonNull JSONObject jsonObject) throws Exception {
                                                long startSyncTimeStamp = ecUpdater.getLastSyncTimeStamp();
                                                boolean success = ecUpdater.saveAllClientsAndEvents(jsonObject);
                                                if (!success) {
                                                    return FetchStatus.fetchedFailed;
                                                } else {
                                                    long lastSyncTimeStamp = ecUpdater.getLastSyncTimeStamp();
                                                    return Pair.of(startSyncTimeStamp, lastSyncTimeStamp);
                                                }
                                            }
                                        });
                            }
                        }
                    }
                })
                .subscribe(new Consumer<Object>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void accept(Object o) throws Exception {
                        if (o != null) {
                            if (o instanceof FetchStatus) {
                                FetchStatus fetchStatus = (FetchStatus) o;

                                if (fetchStatus.equals(FetchStatus.nothingFetched)) {
                                    ECSyncUpdater ecSyncUpdater = ECSyncUpdater.getInstance(context);
                                    ecSyncUpdater.updateLastCheckTimeStamp(Calendar.getInstance().getTimeInMillis());
                                }

                                sendSyncStatusBroadcastMessage(fetchStatus, true);
                            } else if (o instanceof Pair) {
                                Pair<Long, Long> pair = (Pair<Long, Long>) o;
                                processECFromServer(pair);
                                pullECFromServer();
                            }

                        }
                    }
                });
    }

    private void processECFromServer(Pair<Long, Long> pair) {
        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        Observable.
                just(pair)
                .subscribeOn(Schedulers.io())
                .map(new Function<Pair<Long, Long>, Object>() {
                    @Override
                    public Object apply(@NonNull Pair<Long, Long> longLongPair) throws Exception {
                        PathClientProcessor.getInstance(context).processClient(ecUpdater.allEvents(longLongPair.getLeft(), longLongPair.getRight()));
                        return FetchStatus.fetched;
                    }
                })
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        if (o != null && o instanceof FetchStatus) {
                            FetchStatus fetchStatus = (FetchStatus) o;
                            sendSyncStatusBroadcastMessage(fetchStatus);
                        }
                    }
                });
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

        if (isComplete) {
            started = false;
        } else if (fetchStatus.equals(FetchStatus.fetchStarted)) {
            started = true;
        }
    }

    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus) {
        sendSyncStatusBroadcastMessage(fetchStatus, false);
    }

    private void drishtiLogInfo(String message) {
        org.smartregister.util.Log.logInfo(message);
    }

}
