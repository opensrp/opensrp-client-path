package org.smartregister.path.listener;

import android.app.Activity;
import android.util.Log;

import org.smartregister.path.toolbar.BaseToolbar;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import util.JsonFormUtils;

import static org.smartregister.path.activity.BaseActivity.REQUEST_CODE_GET_JSON;
import static org.smartregister.path.activity.LoginActivity.getOpenSRPContext;

/**
 * Created by samuelgithengi on 2/14/18.
 */

public class BaseListener {

    private final static String TAG = BaseListener.class.getName();

    protected Activity context;
    private BaseToolbar toolbar;

    public BaseListener(Activity context) {
        this.context = context;
    }

    public BaseListener(Activity context, BaseToolbar toolbar) {
        this.context = context;
        this.toolbar = toolbar;
    }

    protected void startJsonForm(String formName, String entityId) {
        try {
            String locationId = null;
            if (toolbar instanceof LocationSwitcherToolbar) {
                LocationSwitcherToolbar locationSwitcherToolbar = (LocationSwitcherToolbar) toolbar;
                locationId = JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(),
                        locationSwitcherToolbar.getCurrentLocation());
            }
            JsonFormUtils.startForm(context, getOpenSRPContext(), REQUEST_CODE_GET_JSON,
                    formName, entityId, null, locationId);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

}
