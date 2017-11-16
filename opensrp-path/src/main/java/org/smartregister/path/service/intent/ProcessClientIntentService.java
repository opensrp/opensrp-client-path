package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.smartregister.domain.FetchStatus;
import org.smartregister.path.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.path.sync.PathAfterFetchListener;
import org.smartregister.path.sync.PathClientProcessor;

import java.io.Serializable;

public class ProcessClientIntentService extends IntentService {

    private Context context;
    private PathAfterFetchListener pathAfterFetchListener;

    public ProcessClientIntentService() {
        super("ProcessClientIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getBaseContext();
        pathAfterFetchListener = new PathAfterFetchListener();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        try {
            long start = workIntent.getLongExtra(SyncIntentService.START_SYNC_TIMESTAMP, -1);
            long end = workIntent.getLongExtra(SyncIntentService.LAST_SYNC_TIMESTAMP, -1);
            Serializable fetchStatusSerializable = workIntent.getSerializableExtra(SyncIntentService.FETCH_STATUS);

            if (fetchStatusSerializable != null && fetchStatusSerializable instanceof FetchStatus) {
                FetchStatus fetchStatus = (FetchStatus) fetchStatusSerializable;
                pathAfterFetchListener.afterFetch(fetchStatus);
                sendSyncStatusBroadcastMessage(fetchStatus);
            } else if (start != -1 && end != -1) {
                ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);
                PathClientProcessor.getInstance(context).processClient(ecUpdater.allEvents(start, end));
                pathAfterFetchListener.partialFetch(FetchStatus.fetched);
            }

        } catch (
                Exception e)

        {
            Log.e(getClass().getName(), "", e);
        }
    }

    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus) {
        Intent intent = new Intent();
        intent.setAction(SyncStatusBroadcastReceiver.ACTION_SYNC_STATUS);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_FETCH_STATUS, fetchStatus);
        sendBroadcast(intent);
    }
}
