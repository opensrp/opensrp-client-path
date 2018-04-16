package org.smartregister.path.service.intent.path;

import android.content.Intent;

import org.smartregister.path.receiver.VaccinatorAlarmReceiver;
import org.smartregister.service.ImageUploadSyncService;

/**
 * Created by keyman on 4/16/2018.
 */

public class PathImageUploadSyncService extends ImageUploadSyncService {

    public PathImageUploadSyncService() {
        super();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        VaccinatorAlarmReceiver.completeWakefulIntent(intent);
    }
}

