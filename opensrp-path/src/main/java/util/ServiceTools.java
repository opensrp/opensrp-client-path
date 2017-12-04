package util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import org.smartregister.path.application.VaccinatorApplication;

import java.util.List;

/**
 * Created by keyman on 12/4/17.
 */

public class ServiceTools {

    public static boolean isServiceRunning(Class<?> serviceClass) {
        final ActivityManager activityManager = (ActivityManager) VaccinatorApplication.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo service : services) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    public static void startService(Context context, Class<?> serviceClass) {
        if (context == null || serviceClass == null) {
            return;
        }

        if (!isServiceRunning(serviceClass)) {
            Intent intent = new Intent(context, serviceClass);
            VaccinatorApplication.getInstance().startService(intent);
        }

    }
}