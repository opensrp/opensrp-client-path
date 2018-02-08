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
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
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

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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

public class SyncService extends Service {
    private static final String EVENTS_SYNC_PATH = "/rest/event/add";

    private Context context;
    private HTTPAgent httpAgent;

    public static final int EVENT_PULL_LIMIT = 100;
    private static final int EVENT_PUSH_LIMIT = 50;

    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private List<Observable<?>> observables;
    private boolean fetchFinished;
    private List<String> nullClientIds;
    RequestQueue requestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread = new HandlerThread("SyncService.HandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
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
            sendSyncStatusBroadcastMessage(FetchStatus.noConnection, true);
            return;
        }

        try {
            pushToServer();
            pullECFromServer();

        } catch (Exception e) {
            Log.e(getClass().getName(), e.getMessage(), e);
            sendSyncStatusBroadcastMessage(FetchStatus.fetchedFailed, true);
        }
    }

    private void pullECFromServer() {
        fetchRetry();
    }

    private void saveResponseParcel(final ResponseParcel responseParcel) {
        fetchRetry();
        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);
        final Observable<FetchStatus> observable = Observable.just(responseParcel)
                .observeOn(AndroidSchedulers.from(mHandlerThread.getLooper()))
                .subscribeOn(Schedulers.io()).
                        flatMap(new Function<ResponseParcel, ObservableSource<FetchStatus>>() {
                            @Override
                            public ObservableSource<FetchStatus> apply(@NonNull ResponseParcel responseParcel) throws Exception {
                                JSONObject jsonObject = responseParcel.getJsonObject();
                                ecUpdater.saveAllClientsAndEvents(jsonObject);
                                return Observable.
                                        just(responseParcel.getServerVersionPair())
                                        .observeOn(AndroidSchedulers.from(mHandlerThread.getLooper()))
                                        .subscribeOn(Schedulers.io())
                                        .map(new Function<Pair<Long, Long>, FetchStatus>() {
                                            @Override
                                            public FetchStatus apply(@NonNull Pair<Long, Long> serverVersionPair) throws Exception {
                                                PathClientProcessor.getInstance(context).processClient(ecUpdater.allEvents(serverVersionPair.first - 1, serverVersionPair.second));
                                                return FetchStatus.fetched;
                                            }
                                        });

                            }
                        });

        observable.subscribe(new Consumer<FetchStatus>()

        {
            @Override
            public void accept(FetchStatus fetchStatus) throws Exception {
                // Remove observable from list
                observables.remove(observable);
                Log.i(getClass().getName(), "Deleted: one observable, new count:" + observables.size());

                if ((observables == null || observables.isEmpty()) && fetchFinished) {
                    complete(FetchStatus.fetched);
                } else {
                    sendSyncStatusBroadcastMessage(FetchStatus.fetched);

                }
            }
        });

        // Add observable to list
        observables.add(observable);

        Long observableSize = observables == null ? 0L : observables.size();
        Log.i(getClass().getName(), "Added: one observable, new count: " + observableSize);

    }

    private void saveFetched(Object o) {
        if (o != null) {
            if (o instanceof ResponseParcel) {
                ResponseParcel responseParcel = (ResponseParcel) o;
                saveResponseParcel(responseParcel);
            } else if (o instanceof FetchStatus) {
                final FetchStatus fetchStatus = (FetchStatus) o;
                if (observables == null || observables.isEmpty()) {
                    complete(fetchStatus);
                } else {
                    fetchFinished = true;
                }
            }
        }
    }

    private void fetchRetry() {
        fetchRetry(0);
    }

    private void fetchRetry(final int count) {
        try {
            // Request spacing
            try {
                final int MILLISECONDS = 100;
                Thread.sleep(MILLISECONDS);
            } catch (InterruptedException ie) {
                Log.e(getClass().getName(), ie.getMessage(), ie);
            }

            // Fetch locations
            final String locations = Utils.getPreference(context, LocationPickerView.PREF_TEAM_LOCATIONS, "");
            if (StringUtils.isBlank(locations)) {
                sendSyncStatusBroadcastMessage(FetchStatus.fetchedFailed, true);
                return;
            }

            final ECSyncUpdater ecSyncUpdater = ECSyncUpdater.getInstance(context);
            String baseUrl = VaccinatorApplication.getInstance().context().
                    configuration().dristhiBaseURL();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
            }

            Long lastSyncDatetime = ecSyncUpdater.getLastSyncTimeStamp();
            Log.i(SyncService.class.getName(), "LAST SYNC DT :" + new DateTime(lastSyncDatetime));

            String url = baseUrl + ECSyncUpdater.SEARCH_URL + "?" + AllConstants.SyncFilters.FILTER_LOCATION_ID + "=" + locations + "&serverVersion=" + lastSyncDatetime + "&limit=" + SyncService.EVENT_PULL_LIMIT;
            Log.i(SyncService.class.getName(), "URL: " + url);


            JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, new com.android.volley.Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    // Successful response
                }
            }, new com.android.volley.Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(getClass().getName(), "Fetch Retry Error Response Exception: " + error.getMessage(), error.getCause());
                    if (count < 2) {
                        int newCount = count + 1;
                        fetchRetry(newCount);
                    }
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return authHeaders();
                }

                @Override
                protected com.android.volley.Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    try {
                        String jsonString = new String(response.data,
                                HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));

                        if (StringUtils.isBlank(jsonString)) {
                            saveFetched(FetchStatus.fetchedFailed);
                        }
                        JSONObject jsonObject = new JSONObject(jsonString);

                        final String NO_OF_EVENTS = "no_of_events";
                        int eCount = jsonObject.has(NO_OF_EVENTS) ? jsonObject.getInt(NO_OF_EVENTS) : 0;
                        Log.i(getClass().getName(), "event count: " + eCount);
                        if (eCount < 0) {
                            saveFetched(FetchStatus.fetchedFailed);
                        } else if (eCount == 0) {
                            saveFetched(FetchStatus.nothingFetched);
                        } else {
                            Pair<Long, Long> serverVersionPair = getMinMaxServerVersions(jsonObject);
                            long lastServerVersion = serverVersionPair.second - 1;
                            if (eCount < EVENT_PULL_LIMIT) {
                                lastServerVersion = serverVersionPair.second;
                            }

                            JSONArray events = jsonObject.has("events") ? jsonObject.getJSONArray("events") : new JSONArray();
                            JSONArray clients = jsonObject.has("clients") ? jsonObject.getJSONArray("clients") : new JSONArray();

                            final String BASE_ENTITY_ID = "baseEntityId";
                            for (int i = 0; i < events.length(); i++) {
                                JSONObject event = events.getJSONObject(i);
                                if (event.has(BASE_ENTITY_ID)) {
                                    String baseEntityId = event.getString(BASE_ENTITY_ID);
                                    boolean found = false;
                                    for (int j = 0; j < clients.length(); j++) {
                                        JSONObject client = clients.getJSONObject(j);
                                        if (client.has(BASE_ENTITY_ID)) {
                                            if (baseEntityId.equals(client.getString(BASE_ENTITY_ID))) {
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!found && !nullClientIds.contains(baseEntityId)) {
                                        final String ID = "id";
                                        final String FORM_SUBMISSION_ID = "formSubmissionId";
                                        String eventId = event.has(ID) ? event.getString(ID) : null;
                                        String formSubmissionId = event.has(FORM_SUBMISSION_ID) ? event.getString(FORM_SUBMISSION_ID) : null;
                                        fetchClientRetry(baseEntityId, eventId, formSubmissionId);
                                    }
                                }
                            }

                            ecSyncUpdater.updateLastSyncTimeStamp(lastServerVersion);
                            saveFetched(new ResponseParcel(jsonObject, serverVersionPair));
                        }

                        return com.android.volley.Response.success(new JSONObject(jsonString),
                                HttpHeaderParser.parseCacheHeaders(response));
                    } catch (UnsupportedEncodingException e) {
                        return com.android.volley.Response.error(new ParseError(e));
                    } catch (JSONException je) {
                        return com.android.volley.Response.error(new ParseError(je));
                    }
                }
            };

            requestQueue.add(jsonRequest);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Fetch Retry Exception: " + e.getMessage(), e.getCause());
        }
    }

    public void fetchClientRetry(String baseEntityId, String eventId, String formSubmissionId) {
        fetchClientRetry(baseEntityId, eventId, formSubmissionId, 0);
    }

    public void fetchClientRetry(final String baseEntityId, final String eventId, final String formSubmissionId, final int count) {
        // Request spacing
        try {
            final int MILLISECONDS = 10;
            Thread.sleep(MILLISECONDS);
        } catch (InterruptedException ie) {
            Log.e(getClass().getName(), ie.getMessage(), ie);
        }

        try {
            final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);
            if (StringUtils.isBlank(baseEntityId)) {
                return;
            }

            JSONObject client = ecUpdater.getClient(baseEntityId);
            if (client != null) {
                return;
            }

            String baseUrl = VaccinatorApplication.getInstance().context().
                    configuration().dristhiBaseURL();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
            }

            String url = baseUrl + ECSyncUpdater.CLIENT_URL + "/" + baseEntityId;
            Log.i(SyncService.class.getName(), "URL: " + url);


            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new com.android.volley.Response.Listener<String>() {
                @Override
                public void onResponse(String result) {
                    // Successful response
                }
            }, new com.android.volley.Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(getClass().getName(), "Fetch Retry Client Error Exception: " + error.getMessage(), error.getCause());
                    if (count < 2) {
                        int newCount = count + 1;
                        fetchClientRetry(baseEntityId, eventId, formSubmissionId, newCount);
                    }
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return authHeaders();
                }

                @Override
                protected com.android.volley.Response<String> parseNetworkResponse(NetworkResponse response) {
                    String parsed;
                    try {
                        parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                    } catch (UnsupportedEncodingException e) {
                        parsed = new String(response.data);
                    }

                    if (StringUtils.isBlank(parsed)) {
                        nullClientIds.add(baseEntityId);
                    } else {
                        try {
                            JSONObject client = new JSONObject(parsed);
                            updateClientDateTime(client, "birthdate");
                            updateClientDateTime(client, "deathdate");
                            updateClientDateTime(client, "dateCreated");
                            updateClientDateTime(client, "dateEdited");
                            updateClientDateTime(client, "dateVoided");

                            JSONArray jsonArray = new JSONArray();
                            jsonArray.put(client);
                            ecUpdater.batchInsertClients(jsonArray);

                            JSONObject event = null;
                            if (StringUtils.isNotBlank(eventId)) {
                                event = ecUpdater.getEventsByEventId(eventId);
                            }

                            if (event == null) {
                                event = ecUpdater.getEventsByFormSubmissionId(formSubmissionId);
                            }

                            if (event != null) {
                                List<JSONObject> jsonObjects = new ArrayList<>();
                                jsonObjects.add(event);

                                PathClientProcessor.getInstance(context).processClient(jsonObjects);
                            }
                        } catch (Exception e) {
                            Log.e(getClass().getName(), "Fetch Client Retry Parse Network Exception: " + e.getMessage(), e.getCause());
                            nullClientIds.add(baseEntityId);
                        }
                    }
                    return com.android.volley.Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            requestQueue.add(stringRequest);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Fetch Client Retry: " + e.getMessage(), e.getCause());

        }
    }

    private void updateClientDateTime(JSONObject client, String field) {
        try {
            if (client.has(field) && client.get(field) != null) {
                Long timestamp = client.getLong(field);
                DateTime dateTime = new DateTime(timestamp);
                client.put(field, dateTime.toString());
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), e.getMessage(), e);
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
                Log.e(getClass().getName(), e.getMessage(), e);
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
            Log.e(getClass().getName(), e.getMessage(), e);
        }
        return Pair.create(0L, 0L);
    }

    private Map<String, String> authHeaders() {
        final String USERNAME = VaccinatorApplication.getInstance().context().allSharedPreferences().fetchRegisteredANM();
        final String PASSWORD = VaccinatorApplication.getInstance().context().allSettings().fetchANMPassword();
        Map<String, String> headers = new HashMap<>();
        // add headers <key,value>
        String credentials = USERNAME + ":" + PASSWORD;
        String auth = "Basic "
                + Base64.encodeToString(credentials.getBytes(),
                Base64.NO_WRAP);
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", auth);
        return headers;
    }

////////////////////////////////////////////////////////////////
// Inner classes
////////////////////////////////////////////////////////////////

    private final class ServiceHandler extends Handler {
        private ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            observables = new ArrayList<>();
            fetchFinished = false;
            nullClientIds = new ArrayList<>();
            requestQueue = Volley.newRequestQueue(context);
            handleSync();
        }
    }

    private class ResponseParcel {
        private JSONObject jsonObject;
        private Pair<Long, Long> serverVersionPair;

        private ResponseParcel(JSONObject jsonObject, Pair<Long, Long> serverVersionPair) {
            this.jsonObject = jsonObject;
            this.serverVersionPair = serverVersionPair;
        }

        private JSONObject getJsonObject() {
            return jsonObject;
        }

        private Pair<Long, Long> getServerVersionPair() {
            return serverVersionPair;
        }
    }
}
