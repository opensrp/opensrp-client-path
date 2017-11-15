package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Intent;

import org.smartregister.domain.FetchStatus;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.service.ActionService;

import util.NetworkUtils;

public class SyncAlertIntentService extends IntentService {

    private ActionService actionService;

    public SyncAlertIntentService() {
        super("SyncAlertIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        actionService = VaccinatorApplication.getInstance().context().actionService();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        if (VaccinatorApplication.getInstance().context().IsUserLoggedOut()) {
            drishtiLogInfo("Not updating from server as user is not logged in.");
            return;
        }
        doSync();
    }

    private FetchStatus doSync() {
        if (NetworkUtils.isNetworkAvailable()) {
            return actionService.fetchNewActions();
        }

        return FetchStatus.noConnection;
    }

    private void drishtiLogInfo(String message) {
        org.smartregister.util.Log.logInfo(message);
    }

}
