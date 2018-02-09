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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import util.NetworkUtils;

public class SyncService extends Service {
    private static final String EVENTS_SYNC_PATH = "/rest/event/add";

    private Context context;
    private HTTPAgent httpAgent;

    public static final int EVENT_PULL_LIMIT = 100;
    private static final int EVENT_PUSH_LIMIT = 50;

    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
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
                    int eCount = fetchNumberOfEvents(jsonObject);
                    if (eCount == 0) {
                        sendSyncStatusBroadcastMessage(FetchStatus.nothingFetched);
                    } else {
                        fetchRetry();
                    }
                }
            }, new com.android.volley.Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(getClass().getName(), "Fetch Retry Error Response Exception: " + error.getMessage(), error.getCause());
                    if (count < 2) {
                        int newCount = count + 1;
                        fetchRetry(newCount);
                    } else {
                        sendSyncStatusBroadcastMessage(FetchStatus.fetchedFailed);
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

                        JSONObject jsonObject = new JSONObject(jsonString);

                        int eCount = fetchNumberOfEvents(jsonObject);
                        if (eCount < 0) {
                            return com.android.volley.Response.error(new ParseError(new Exception("Error")));
                        } else if (eCount > 0) {
                            Pair<Long, Long> serverVersionPair = getMinMaxServerVersions(jsonObject);
                            long lastServerVersion = serverVersionPair.second - 1;
                            if (eCount < EVENT_PULL_LIMIT) {
                                lastServerVersion = serverVersionPair.second;
                            }

                            ecSyncUpdater.updateLastSyncTimeStamp(lastServerVersion);
                            ecSyncUpdater.saveAllClientsAndEvents(jsonObject);

                            startClientProcessorService(serverVersionPair);
                        }
                        return com.android.volley.Response.success(new JSONObject(jsonString),
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

            requestQueue.add(jsonRequest);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Fetch Retry Exception: " + e.getMessage(), e.getCause());
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


    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus) {
        sendSyncStatusBroadcastMessage(fetchStatus, false);
    }

    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus, boolean isComplete) {
        Intent intent = new Intent();
        intent.setAction(SyncStatusBroadcastReceiver.ACTION_SYNC_STATUS);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_FETCH_STATUS, fetchStatus);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_COMPLETE_STATUS, isComplete);
        sendBroadcast(intent);

        if (isEmptyReuestQueue()) {
            stopSelf();
        }
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
    public boolean isEmptyReuestQueue() {
        final Object mObject = new Mirror().on(this.requestQueue).get().field("mCurrentRequests");
        final Set<Request<?>> mCurrentRequests = (Set<Request<?>>) mObject;
        return mCurrentRequests.isEmpty();
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
            requestQueue = Volley.newRequestQueue(context);
            requestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {
                @Override
                public void onRequestFinished(Request<Object> request) {
                    if (isEmptyReuestQueue()) {
                        stopSelf();
                    }
                }
            });
            handleSync();
        }
    }
}
