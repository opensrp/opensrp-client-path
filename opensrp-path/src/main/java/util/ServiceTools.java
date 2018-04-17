package util;

import android.content.Context;
import android.content.Intent;

import org.smartregister.path.receiver.VaccinatorAlarmReceiver;
import org.smartregister.path.service.intent.SyncIntentService;

/**
 * Created by keyman on 12/4/17.
 */

public class ServiceTools {


    public static void startSyncService(Context context) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, SyncIntentService.class);
        context.startService(intent);

    }

    public static void startService(Context context, Class serviceClass, boolean wakeup) {
        if (context == null || serviceClass == null) {
            return;
        }

        Intent intent = new Intent(context, serviceClass);
        if (wakeup) {
            VaccinatorAlarmReceiver.startWakefulService(context, intent);
        } else {
            context.startService(intent);
        }
    }
}
