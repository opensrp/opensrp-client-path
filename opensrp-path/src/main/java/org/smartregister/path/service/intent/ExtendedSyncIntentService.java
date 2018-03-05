package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.smartregister.growthmonitoring.service.intent.ZScoreRefreshIntentService;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.service.ActionService;
import org.smartregister.stock.sync.StockSyncIntentService;

import util.NetworkUtils;

import static org.smartregister.util.Log.logInfo;

public class ExtendedSyncIntentService extends IntentService {

    private Context context;
    private ActionService actionService;

    public ExtendedSyncIntentService() {
        super("ExtendedSyncIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getBaseContext();
        actionService = VaccinatorApplication.getInstance().context().actionService();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        if (VaccinatorApplication.getInstance().context().IsUserLoggedOut()) {
            logInfo("Not updating from server as user is not logged in.");
            return;
        }

        if (NetworkUtils.isNetworkAvailable()) {

            startStockSync();

            actionService.fetchNewActions();

            startSyncValidation();
        }
        startZscoreRefresh();
    }

    private void startStockSync() {
        Intent intent = new Intent(context, StockSyncIntentService.class);
        startService(intent);
    }

    private void startSyncValidation() {
        Intent intent = new Intent(context, ValidateIntentService.class);
        startService(intent);
    }

    private void startZscoreRefresh() {
        Intent intent = new Intent(context, ZScoreRefreshIntentService.class);
        startService(intent);
    }

}
