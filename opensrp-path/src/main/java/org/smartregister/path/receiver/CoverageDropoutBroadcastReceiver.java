package org.smartregister.path.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens for broadcast responses from {@link org.smartregister.path.service.intent.CoverageDropoutIntentService}
 * service
 * Created by keyman on 1/15/18.
 */

public class CoverageDropoutBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = CoverageDropoutBroadcastReceiver.CoverageDropoutServiceListener.class.getCanonicalName();
    public static final String TYPE = "TYPE";
    public static final String ACTION_SERVICE_DONE = "COVERAGE_DROPOUT_DONE";
    public static final String TYPE_GENERATE_COHORT_INDICATORS = "GENERATE_COHORT_INDICATORS";
    public static final String TYPE_GENERATE_CUMULATIVE_INDICATORS = "GENERATE_CUMULATIVE_INDICATORS";

    private static CoverageDropoutBroadcastReceiver singleton;
    private final List<CoverageDropoutServiceListener> listeners;

    public static void init(Context context) {
        if (singleton != null) {
            destroy(context);
        }

        singleton = new CoverageDropoutBroadcastReceiver();
        context.registerReceiver(singleton, new IntentFilter(ACTION_SERVICE_DONE));
    }

    private static void destroy(Context context) {
        try {
            if (singleton != null) {
                context.unregisterReceiver(singleton);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static CoverageDropoutBroadcastReceiver getInstance() {
        return singleton;
    }

    public CoverageDropoutBroadcastReceiver() {
        this.listeners = new ArrayList<>();
    }

    public void addCoverageDropoutServiceListener(CoverageDropoutServiceListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeCoverageDropoutServiceListener(CoverageDropoutServiceListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra(TYPE);
        for (CoverageDropoutServiceListener curListener : listeners) {
            curListener.onServiceFinish(type);
        }
    }

    public interface CoverageDropoutServiceListener {
        void onServiceFinish(String actionType);
    }
}
