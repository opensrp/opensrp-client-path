package org.opensrp.path.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.opensrp.path.sync.PathAfterFetchListener;
import org.opensrp.path.sync.PathUpdateActionsTask;
import org.opensrp.sync.SyncProgressIndicator;

import static org.opensrp.util.Log.logInfo;

public class PathSyncBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        logInfo("Sync alarm triggered. Trying to Sync.");

        PathUpdateActionsTask pathUpdateActionsTask = new PathUpdateActionsTask(
                context,
                org.opensrp.Context.getInstance().actionService(),
                org.opensrp.Context.getInstance().formSubmissionSyncService(),
                new SyncProgressIndicator(),
                org.opensrp.Context.getInstance().allFormVersionSyncService());

        pathUpdateActionsTask.updateFromServer(new PathAfterFetchListener());

    }

}

