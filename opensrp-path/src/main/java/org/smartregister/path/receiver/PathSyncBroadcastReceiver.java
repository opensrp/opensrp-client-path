package org.smartregister.path.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.sync.PathAfterFetchListener;
import org.smartregister.path.sync.PathUpdateActionsTask;
import org.smartregister.sync.SyncProgressIndicator;

import static org.smartregister.util.Log.logInfo;

public class PathSyncBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        logInfo("Sync alarm triggered. Trying to Sync.");

        PathUpdateActionsTask pathUpdateActionsTask = new PathUpdateActionsTask(
                context,
                getOpenSRPContext().actionService(),
                new SyncProgressIndicator(),
                getOpenSRPContext().allFormVersionSyncService());

        pathUpdateActionsTask.updateFromServer(new PathAfterFetchListener());

    }

    public org.smartregister.Context getOpenSRPContext() {
        return VaccinatorApplication.getInstance().context();
    }


}

