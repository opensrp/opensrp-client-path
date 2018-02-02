package org.smartregister.path.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.ona.kujaku.services.MapboxOfflineDownloaderService;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 02/02/2018.
 */

public class OfflineMapDownloadUpdatesReceiver extends BroadcastReceiver {

    public static final String TAG = OfflineMapDownloadUpdatesReceiver.class.getName();
    private boolean isDownloading = false;
    private boolean isWaitingForDownloadToStart = false;
    private DisplayToastInterface displayToastInterface;

    public OfflineMapDownloadUpdatesReceiver(DisplayToastInterface displayToastInterface) {
        this.displayToastInterface = displayToastInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Offline Map Download Updates received intent with extras : " + intent.getExtras().toString());

        if (intent.hasExtra(MapboxOfflineDownloaderService.KEY_RESULT_STATUS)) {
            String resultStatus = intent.getStringExtra(MapboxOfflineDownloaderService.KEY_RESULT_STATUS);
            MapboxOfflineDownloaderService.SERVICE_ACTION serviceAction = (MapboxOfflineDownloaderService.SERVICE_ACTION) intent.getExtras().get(MapboxOfflineDownloaderService.KEY_RESULTS_PARENT_ACTION);

            String message = (intent.hasExtra(MapboxOfflineDownloaderService.KEY_RESULT_MESSAGE)) ?
                    intent.getStringExtra(MapboxOfflineDownloaderService.KEY_RESULT_MESSAGE) : null;

            if (serviceAction == MapboxOfflineDownloaderService.SERVICE_ACTION.DOWNLOAD_MAP) {
                if (resultStatus.equals(MapboxOfflineDownloaderService.SERVICE_ACTION_RESULT.SUCCESSFUL.name())) {
                    isDownloading = true;
                    if (isWaitingForDownloadToStart) {
                        displayToastInterface.showInfoToast("Offline Map has started downloading");
                        isWaitingForDownloadToStart = false;
                    }
                } else if (resultStatus.equals(MapboxOfflineDownloaderService.SERVICE_ACTION_RESULT.FAILED.name())) {
                    //An error occurred trying to download the map
                    message = (message == null) ? "Oops! Offline map cannot be downloaded" : message;
                    isDownloading = false;
                    displayToastInterface.showInfoToast(message);
                }
            } else if (serviceAction == MapboxOfflineDownloaderService.SERVICE_ACTION.STOP_CURRENT_DOWNLOAD) {
                isDownloading = false;

                Log.i(TAG, "Offline Map Download: STATUS - " + resultStatus + ((message == null) ? "" : " with message - "  + message));
            }
        } else {
            Log.e(TAG, "Unknown OfflineMapDownloadService Message : \n" + intent.getExtras().toString());
        }
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }

    public boolean isWaitingForDownloadToStart() {
        return isWaitingForDownloadToStart;
    }

    public void setWaitingForDownloadToStart(boolean waitingForDownloadToStart) {
        this.isWaitingForDownloadToStart = waitingForDownloadToStart;
    }
}
