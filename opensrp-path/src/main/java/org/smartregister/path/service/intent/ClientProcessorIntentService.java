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
            PathClientProcessor.getInstance(context).processClient(events);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Process Client Exception: " + e.getMessage(), e);
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
