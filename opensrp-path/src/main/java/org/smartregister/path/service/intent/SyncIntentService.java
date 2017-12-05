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
import android.util.Pair;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import util.NetworkUtils;

public class SyncIntentService extends Service {
    private static final String EVENTS_SYNC_PATH = "/rest/event/add";

    private Context context;
    private HTTPAgent httpAgent;

    public static final int EVENT_PULL_LIMIT = 100;
    private static final int EVENT_PUSH_LIMIT = 50;

    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private List<Observable<?>> processObservables;
    private List<Observable<?>> saveObservables;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            saveObservables = new ArrayList<>();
            processObservables = new ArrayList<>();
            handleSync();
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

    private void pullECFromServer() {
        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        // Fetch locations
        String locations = Utils.getPreference(context, LocationPickerView.PREF_TEAM_LOCATIONS, "");
        if (StringUtils.isBlank(locations)) {
            sendSyncStatusBroadcastMessage(FetchStatus.fetchedFailed);
        }

        Observable.just(locations)
                .observeOn(AndroidSchedulers.from(mHandlerThread.getLooper()))
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(@NonNull String locations) throws Exception {

                        JSONObject jsonObject = fetchRetry(locations, 0);
                        if (jsonObject == null) {
                            return Observable.just(FetchStatus.fetchedFailed);
                        } else {
                            int eCount = jsonObject.has("no_of_events") ? jsonObject.getInt("no_of_events") : 0;
                            if (eCount < 0) {
                                return Observable.just(FetchStatus.fetchedFailed);
                            } else if (eCount == 0) {
                                return Observable.just(FetchStatus.nothingFetched);
                            } else {
                                Pair<Long, Long> serverVersionPair = getMinMaxServerVersions(jsonObject);
                                long lastServerVersion = serverVersionPair.second - 1;
                                if (eCount < EVENT_PULL_LIMIT) {
                                    lastServerVersion = serverVersionPair.second;
                                }
                                ecUpdater.updateLastSyncTimeStamp(lastServerVersion);
                                return Observable.just(new ResponseParcel(jsonObject, serverVersionPair));
                            }
                        }
                    }
                })
                .subscribe(new Consumer<Object>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void accept(Object o) throws Exception {
                        if (o != null) {
                            if (o instanceof ResponseParcel) {
                                ResponseParcel responseParcel = (ResponseParcel) o;
                                saveToSyncTables(responseParcel);
                            } else if (o instanceof FetchStatus) {
                                final FetchStatus fetchStatus = (FetchStatus) o;
                                if (saveObservables != null && !saveObservables.isEmpty()) {
                                    Observable.zip(saveObservables, new Function<Object[], Object>() {
                                        @Override
                                        public Object apply(@NonNull Object[] objects) throws Exception {
                                            return FetchStatus.fetched;
                                        }
                                    }).subscribe(new Consumer<Object>() {
                                        @Override
                                        public void accept(Object o) throws Exception {
                                            if (processObservables != null && !processObservables.isEmpty()) {
                                                Observable.zip(processObservables, new Function<Object[], Object>() {
                                                    @Override
                                                    public Object apply(@NonNull Object[] objects) throws Exception {
                                                        return FetchStatus.fetched;
                                                    }
                                                }).subscribe(new Consumer<Object>() {
                                                    @Override
                                                    public void accept(Object o) throws Exception {
                                                        complete(fetchStatus);
                                                    }
                                                });
                                            } else {
                                                complete(fetchStatus);
                                            }
                                        }
                                    });
                                } else if (processObservables != null && !processObservables.isEmpty()) {
                                    Observable.zip(processObservables, new Function<Object[], Object>() {
                                        @Override
                                        public Object apply(@NonNull Object[] objects) throws Exception {
                                            return FetchStatus.fetched;
                                        }
                                    }).subscribe(new Consumer<Object>() {
                                        @Override
                                        public void accept(Object o) throws Exception {
                                            complete(fetchStatus);
                                        }
                                    });
                                } else {
                                    complete(fetchStatus);
                                }

                            }
                        }
                    }
                });
    }

    private void saveToSyncTables(final ResponseParcel responseParcel) {
        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);
        final Observable<Pair<Long, Long>> observable = Observable.just(responseParcel)
                .observeOn(AndroidSchedulers.from(mHandlerThread.getLooper()))
                .subscribeOn(Schedulers.io()).
                        map(new Function<ResponseParcel, Pair<Long, Long>>() {
                            @Override
                            public Pair<Long, Long> apply(@NonNull ResponseParcel responseParcel) throws Exception {
                                JSONObject jsonObject = responseParcel.getJsonObject();
                                ecUpdater.saveAllClientsAndEvents(jsonObject);
                                return responseParcel.getServerVersionPair();

                            }
                        });

        observable.subscribe(new Consumer<Pair<Long, Long>>() {
            @Override
            public void accept(Pair<Long, Long> longLongPair) throws Exception {
                clientProcessor(longLongPair);
                saveObservables.remove(observable);
            }
        });

        saveObservables.add(observable);

        pullECFromServer();
    }

    private void clientProcessor(final Pair<Long, Long> serverVersionPair) {
        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        final Observable<FetchStatus> observable = Observable.
                just(serverVersionPair)
                .observeOn(AndroidSchedulers.from(mHandlerThread.getLooper()))
                .subscribeOn(Schedulers.io())
                .map(new Function<Pair<Long, Long>, FetchStatus>() {
                    @Override
                    public FetchStatus apply(@NonNull Pair<Long, Long> serverVersionPair) throws Exception {
                        PathClientProcessor.getInstance(context).processClient(ecUpdater.allEvents(serverVersionPair.first, serverVersionPair.second));
                        return FetchStatus.fetched;
                    }
                });


        observable.subscribe(new Consumer<FetchStatus>() {
            @Override
            public void accept(FetchStatus fetchStatus) throws Exception {
                sendSyncStatusBroadcastMessage(FetchStatus.fetched);
                processObservables.remove(observable);
            }
        });

        processObservables.add(observable);
    }

    private JSONObject fetchRetry(String locations, int count) throws Exception {
        // Request spacing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Log.e(getClass().getName(), ie.getMessage());
        }

        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        try {
            return ecUpdater.fetchAsJsonObject(AllConstants.SyncFilters.FILTER_LOCATION_ID, locations);

        } catch (Exception e) {
            Log.e(getClass().getName(), e.getMessage());
            if (count >= 2) {
                return null;
            } else {
                return fetchRetry(locations, ++count);
            }

        }
    }

    private void complete(FetchStatus fetchStatus) {
        if (fetchStatus.equals(FetchStatus.nothingFetched)) {
            ECSyncUpdater ecSyncUpdater = ECSyncUpdater.getInstance(context);
            ecSyncUpdater.updateLastCheckTimeStamp(Calendar.getInstance().getTimeInMillis());
        }

        sendSyncStatusBroadcastMessage(fetchStatus, true);
    }

// PUSH TO SERVER

    private void pushToServer() {
        pushECToServer();
    }

    private void pushECToServer() {
        EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();
        boolean keepSyncing = true;

        while (keepSyncing) {
            try {
                Map<String, Object> pendingEvents = db.getUnSyncedEvents(EVENT_PUSH_LIMIT);

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
            stopSelf();
        }
    }

    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus) {
        sendSyncStatusBroadcastMessage(fetchStatus, false);
    }

    private void drishtiLogInfo(String message) {
        org.smartregister.util.Log.logInfo(message);
    }

    private Pair<Long, Long> getMinMaxServerVersions(JSONObject jsonObject) {
        final String EVENTS = "events";
        final String SERVER_VERSION = "serverVersion";
        try {
            if (jsonObject != null && jsonObject.has(EVENTS)) {
                JSONArray events = jsonObject.getJSONArray(EVENTS);

                long maxServerVersion = Long.MIN_VALUE;
                long minServerVersion = Long.MAX_VALUE;

                for (int i = 0; i < events.length(); i++) {
                    Object o = events.get(i);
                    if (o instanceof JSONObject) {
                        JSONObject jo = (JSONObject) o;
                        if (jo.has(SERVER_VERSION)) {
                            long serverVersion = jo.getLong(SERVER_VERSION);
                            if (serverVersion > maxServerVersion) {
                                maxServerVersion = serverVersion;
                            }

                            if (serverVersion < minServerVersion) {
                                minServerVersion = serverVersion;
                            }
                        }
                    }
                }
                return Pair.create(minServerVersion, maxServerVersion);
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), e.getMessage());
        }
        return Pair.create(0L, 0L);
    }

    private class ResponseParcel {
        private JSONObject jsonObject;
        private Pair<Long, Long> serverVersionPair;

        public ResponseParcel(JSONObject jsonObject, Pair<Long, Long> serverVersionPair) {
            this.jsonObject = jsonObject;
            this.serverVersionPair = serverVersionPair;
        }

        public JSONObject getJsonObject() {
            return jsonObject;
        }

        public Pair<Long, Long> getServerVersionPair() {
            return serverVersionPair;
        }
    }

}
