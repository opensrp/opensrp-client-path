package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.smartregister.growthmonitoring.service.intent.ZScoreRefreshIntentService;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.receiver.VaccinatorAlarmReceiver;
import org.smartregister.path.service.intent.path.PathStockSyncIntentService;
import org.smartregister.path.service.intent.path.PathZScoreRefreshIntentService;
import org.smartregister.service.ActionService;
import org.smartregister.stock.sync.StockSyncIntentService;

import util.NetworkUtils;
import util.ServiceTools;

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

        boolean wakeup = workIntent.getBooleanExtra(SyncIntentService.WAKE_UP, false);

        if (NetworkUtils.isNetworkAvailable()) {

            startStockSync(wakeup);

            actionService.fetchNewActions();

            startSyncValidation(wakeup);
        }
        startZscoreRefresh(wakeup);

        VaccinatorAlarmReceiver.completeWakefulIntent(workIntent);
    }

    private void startStockSync(boolean wakeup) {
        ServiceTools.startService(context, PathStockSyncIntentService.class, wakeup);
    }

    private void startSyncValidation(boolean wakeup) {
        ServiceTools.startService(context, ValidateIntentService.class, wakeup);
    }

    private void startZscoreRefresh(boolean wakeup) {
        ServiceTools.startService(context, PathZScoreRefreshIntentService.class, wakeup);
    }

}
