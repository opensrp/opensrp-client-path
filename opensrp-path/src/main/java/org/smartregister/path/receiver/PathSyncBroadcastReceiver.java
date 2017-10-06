package org.smartregister.path.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.service.intent.SyncIntentService;

import static org.smartregister.util.Log.logInfo;

public class PathSyncBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        logInfo("Sync alarm triggered. Trying to Sync.");

        context.startService(new Intent(context, SyncIntentService.class));
    }

    public org.smartregister.Context getOpenSRPContext() {
        return VaccinatorApplication.getInstance().context();
    }


}

