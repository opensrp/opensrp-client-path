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

import net.vidageek.mirror.dsl.Mirror;

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
import org.smartregister.path.view.LocationPickerView;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.service.HTTPAgent;
import org.smartregister.util.Utils;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.NetworkUtils;

public class SyncService extends Service {
    private static final String ADD_URL = "/rest/event/add";
    public static final String SYNC_URL = "/rest/event/sync";
    private static final String CLIENT_URL = "/rest/client";

    private Context context;
    private HTTPAgent httpAgent;

    public static final int EVENT_PULL_LIMIT = 100;
    private static final int EVENT_PUSH_LIMIT = 50;

    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private RequestQueue mainRequestQueue;
    private List<RequestQueue> clientRequestQueues;
    private List<String> nullClientIds;

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

    private void fetchRetry() {
        fetchRetry(0);
    }

    private synchronized void fetchRetry(final int count) {
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

            String url = baseUrl + SYNC_URL + "?" + AllConstants.SyncFilters.FILTER_LOCATION_ID + "=" + locations + "&serverVersion=" + lastSyncDatetime + "&limit=" + SyncService.EVENT_PULL_LIMIT;
            Log.i(SyncService.class.getName(), "URL: " + url);

            JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, new com.android.volley.Response.Listener<JSONObject>() {
                @Override
                @SuppressWarnings("unchecked")
                public void onResponse(JSONObject jsonObject) {
                    int eCount = fetchNumberOfEvents(jsonObject);
                    if (eCount == 0) {
                        sendSyncStatusBroadcastMessage(FetchStatus.nothingFetched, true);
                    } else {
                        fetchRetry();
                    }
                }
            }, new com.android.volley.Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error != null) {
                        Log.e(getClass().getName(), "Fetch Retry Error Response Exception: " + error.getMessage(), error.getCause());
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null) {
                            Log.e(getClass().getName(), "Status Code: " + networkResponse.statusCode);
                        }
                    }

                    if (count < 2) {
                        int newCount = count + 1;
                        fetchRetry(newCount);
                    } else {
                        sendSyncStatusBroadcastMessage(FetchStatus.fetchedFailed, true);
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
                        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

                        String jsonString = new String(response.data,
                                HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));

                        JSONObject jsonObject = new JSONObject(jsonString);

                        int eCount = fetchNumberOfEvents(jsonObject);
                        if (eCount < 0) {
                            return com.android.volley.Response.error(new ParseError(new Exception("Error")));
                        } else if (eCount > 0) {
                            final Pair<Long, Long> serverVersionPair = getMinMaxServerVersions(jsonObject);
                            long lastServerVersion = serverVersionPair.second - 1;
                            if (eCount < EVENT_PULL_LIMIT) {
                                lastServerVersion = serverVersionPair.second;
                            }

                            ecSyncUpdater.updateLastSyncTimeStamp(lastServerVersion);
                            ecSyncUpdater.saveAllClientsAndEvents(jsonObject);


                            final RequestQueue clientRequestQueue = Volley.newRequestQueue(context);
                            List<JSONObject> events = ecUpdater.allEvents(serverVersionPair.first - 1, serverVersionPair.second);
                            for (int i = 0; i < events.size(); i++) {
                                JSONObject event = events.get(i);
                                fetchClientRetry(clientRequestQueue, event);
                            }

                            clientRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {
                                @Override
                                public void onRequestFinished(Request<Object> request) {
                                    if (isEmptyReuestQueue(clientRequestQueue)) {
                                        startClientProcessorService(serverVersionPair);
                                        stopSyncService();
                                    }
                                }
                            });
                            clientRequestQueues.add(clientRequestQueue);

                            if (isEmptyReuestQueue(clientRequestQueue)) {
                                startClientProcessorService(serverVersionPair);
                            }

                        }
                        return com.android.volley.Response.success(jsonObject,
                                HttpHeaderParser.parseCacheHeaders(response));
                    } catch (UnsupportedEncodingException e) {
                        return com.android.volley.Response.error(new ParseError(e));
                    } catch (JSONException je) {
                        return com.android.volley.Response.error(new ParseError(je));
                    } catch (Exception e) {
                        return com.android.volley.Response.error(new ParseError(e));
                    }
                }
            };

            mainRequestQueue.add(jsonRequest);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Fetch Retry Exception: " + e.getMessage(), e.getCause());
        }
    }

    private void fetchClientRetry(RequestQueue requestQueue, JSONObject event) {
        final String BASE_ENTITY_ID = "baseEntityId";
        final String CLIENT = "client";
        try {
            if (!event.has(CLIENT) && event.has(BASE_ENTITY_ID)) {
                String baseEntityId = event.getString(BASE_ENTITY_ID);
                synchronized (nullClientIds) {
                    if (!nullClientIds.contains(baseEntityId)) {
                        fetchClientRetry(requestQueue, baseEntityId);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), "Fetch Client Exception: " + e.getMessage(), e.getCause());
        }
    }

    private void fetchClientRetry(RequestQueue requestQueue, String basEntityId) {
        fetchClientRetry(requestQueue, basEntityId, 0);
    }

    private synchronized void fetchClientRetry(final RequestQueue requestQueue, final String baseEntityId, final int count) {
        // Request spacing
        try {
            final int MILLISECONDS = 250;
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

            String url = baseUrl + CLIENT_URL + "/" + baseEntityId;
            Log.i(SyncService.class.getName(), "URL: " + url);

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new com.android.volley.Response.Listener<String>() {
                @Override
                public void onResponse(String result) {
                    // Response Success
                }
            }, new com.android.volley.Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error != null) {
                        Log.e(getClass().getName(), "Fetch Retry Client Error Exception: " + error.getMessage(), error.getCause());
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null) {
                            Log.e(getClass().getName(), "Status Code: " + networkResponse.statusCode);
                        }
                    }
                    if (count < 2) {
                        int newCount = count + 1;
                        fetchClientRetry(requestQueue, baseEntityId, newCount);
                    }
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return authHeaders();
                }

                @Override
                protected com.android.volley.Response<String> parseNetworkResponse
                        (NetworkResponse response) {
                    String parsed;
                    try {
                        parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                    } catch (UnsupportedEncodingException e) {
                        parsed = new String(response.data);
                    }

                    try {
                        final int SC_OK = 200;
                        if (StringUtils.isBlank(parsed) && response.statusCode == SC_OK) {
                            nullClientIds.add(baseEntityId);
                        }
                        if (StringUtils.isNotBlank(parsed)) {
                            JSONObject client = new JSONObject(parsed);
                            updateClientDateTime(client, "birthdate");
                            updateClientDateTime(client, "deathdate");
                            updateClientDateTime(client, "dateCreated");
                            updateClientDateTime(client, "dateEdited");
                            updateClientDateTime(client, "dateVoided");

                            JSONArray jsonArray = new JSONArray();
                            jsonArray.put(client);
                            ecUpdater.batchInsertClients(jsonArray);
                        }
                    } catch (Exception e) {
                        Log.e(getClass().getName(), "Fetch Client Retry Parse Network Exception: " + e.getMessage(), e.getCause());
                    }
                    return com.android.volley.Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            requestQueue.add(stringRequest);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Fetch Client Retry: " + e.getMessage(), e.getCause());
        }
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
                                ADD_URL),
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


    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus) {
        sendSyncStatusBroadcastMessage(fetchStatus, false);
    }

    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus, boolean isComplete) {
        Intent intent = new Intent();
        intent.setAction(SyncStatusBroadcastReceiver.ACTION_SYNC_STATUS);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_FETCH_STATUS, fetchStatus);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_COMPLETE_STATUS, isComplete);
        sendBroadcast(intent);

        stopSyncService();
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

    private int fetchNumberOfEvents(JSONObject jsonObject) {
        int count = 0;
        final String NO_OF_EVENTS = "no_of_events";
        try {
            if (jsonObject != null && jsonObject.has(NO_OF_EVENTS)) {
                count = jsonObject.getInt(NO_OF_EVENTS);
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), e.getMessage(), e);
        }
        Log.i(getClass().getName(), "event count: " + count);
        return count;
    }

    private void startClientProcessorService(Pair<Long, Long> serverVersionPair) {
        Intent intent = new Intent(context, ClientProcessorIntentService.class);
        intent.putExtra(ClientProcessorIntentService.START, serverVersionPair.first - 1);
        intent.putExtra(ClientProcessorIntentService.END, serverVersionPair.second);
        startService(intent);
    }

    @SuppressWarnings("unchecked")
    public boolean isEmptyReuestQueue(RequestQueue requestQueue) {
        final Object mObject = new Mirror().on(requestQueue).get().field("mCurrentRequests");
        final Set<Request<?>> mCurrentRequests = (Set<Request<?>>) mObject;
        return mCurrentRequests.isEmpty();
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

    private void stopSyncService() {
        boolean emptyClientQueue = true;
        synchronized (clientRequestQueues) {
            for (RequestQueue clientRequestQueue : clientRequestQueues) {
                if (!isEmptyReuestQueue(clientRequestQueue)) {
                    emptyClientQueue = false;
                    break;
                }
            }
        }

        synchronized (mainRequestQueue) {
            if (emptyClientQueue && isEmptyReuestQueue(mainRequestQueue)) {
                stopSelf();
            }
        }
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
            mainRequestQueue = Volley.newRequestQueue(context);
            clientRequestQueues = Collections.synchronizedList(new ArrayList<RequestQueue>());
            nullClientIds = Collections.synchronizedList(new ArrayList<String>());
            mainRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {
                @Override
                public void onRequestFinished(Request<Object> request) {
                    synchronized (mainRequestQueue) {
                        if (isEmptyReuestQueue(mainRequestQueue)) {
                            stopSyncService();
                        }
                    }
                }
            });
            handleSync();
        }
    }
}
