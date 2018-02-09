package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.FetchStatus;
import org.smartregister.path.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.path.sync.PathClientProcessor;

import java.util.Calendar;
import java.util.List;

/**
 * Created by keyman on 2/9/18.
 */

public class ClientProcessorIntentService extends IntentService {
    public static final String START = "start";
    public static final String END = "end";

    private int waitingIntentCount = 0;
    private Context context;

    public ClientProcessorIntentService() {
        super("ClientProcessorIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        waitingIntentCount++;
        context = getBaseContext();
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onHandleIntent(Intent intent) {
        long min = intent.getLongExtra(START, 0);
        long max = intent.getLongExtra(END, 0);
        processClient(min, max);

        waitingIntentCount--;

        if (waitingIntentCount == 0) {
            complete(FetchStatus.fetched);
        } else {
            sendSyncStatusBroadcastMessage(FetchStatus.fetched);
        }
    }

    private void processClient(long start, long end) {
        try {
            final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);
            List<JSONObject> events = ecUpdater.allEvents(start, end);
            for (int i = 0; i < events.size(); i++) {
                fetchAndInsertClient(events.get(i));
            }

            PathClientProcessor.getInstance(context).processClient(events);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Process Client Exception: " + e.getMessage(), e);
        }

    }

    private void fetchAndInsertClient(JSONObject event) {
        final String BASE_ENTITY_ID = "baseEntityId";
        final String CLIENT = "client";
        try {
            if (!event.has(CLIENT) && event.has(BASE_ENTITY_ID)) {
                String baseEntityId = event.getString(BASE_ENTITY_ID);
                JSONObject client = fetchClientRetry(baseEntityId);
                if (client != null) {
                    event.put(CLIENT, client);
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), "Fetch Client Exception: " + e.getMessage(), e.getCause());
        }
    }

    private JSONObject fetchClientRetry(String baseEntityId) {
        return fetchClientRetry(baseEntityId, 0);
    }

    private JSONObject fetchClientRetry(String baseEntityId, int count) {
        // Request spacing
        try {
            final int MILLISECONDS = 10;
            Thread.sleep(MILLISECONDS);
        } catch (InterruptedException ie) {
            Log.e(getClass().getName(), ie.getMessage());
        }

        final ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);
        try {
            if (StringUtils.isBlank(baseEntityId)) {
                return null;
            }

            JSONObject client = ecUpdater.getClient(baseEntityId);
            if (client != null) {
                return client;
            }

            client = ecUpdater.fetchClientAsJsonObject(baseEntityId);
            if (client == null) {
                return null;
            }

            updateClientDateTime(client, "birthdate");
            updateClientDateTime(client, "deathdate");
            updateClientDateTime(client, "dateCreated");
            updateClientDateTime(client, "dateEdited");
            updateClientDateTime(client, "dateVoided");

            JSONArray jsonArray = new JSONArray();
            jsonArray.put(client);
            ecUpdater.batchInsertClients(jsonArray);
            return client;
        } catch (Exception e) {
            if (count >= 2) {
                return null;
            } else {
                int newCount = count + 1;
                return fetchClientRetry(baseEntityId, newCount);
            }
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

    //Broadcast Receiver

    private void complete(FetchStatus fetchStatus) {
        if (fetchStatus.equals(FetchStatus.nothingFetched)) {
            ECSyncUpdater ecSyncUpdater = ECSyncUpdater.getInstance(context);
            ecSyncUpdater.updateLastCheckTimeStamp(Calendar.getInstance().getTimeInMillis());
        }

        sendSyncStatusBroadcastMessage(fetchStatus, true);
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

}
