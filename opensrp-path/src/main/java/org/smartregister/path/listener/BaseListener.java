package org.smartregister.path.listener;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.util.Log;

import org.smartregister.path.R;
import org.smartregister.path.activity.BaseActivity;
import org.smartregister.path.activity.ChildSmartRegisterActivity;
import org.smartregister.path.fragment.ChildSmartRegisterFragment;
import org.smartregister.path.toolbar.BaseToolbar;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;
import org.smartregister.path.view.LocationActionView;

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
    private String location;

    public BaseListener(Activity context) {
        this.context = context;
    }

    public BaseListener(Activity context, BaseToolbar toolbar) {
        this.context = context;
        this.toolbar = toolbar;
    }

    public BaseListener(Activity context, String location) {
        this.context = context;
        this.location = location;
    }

    protected void startJsonForm(String formName, String entityId) {
        try {
            String location;
            if (toolbar instanceof LocationSwitcherToolbar) {
                LocationSwitcherToolbar locationSwitcherToolbar = (LocationSwitcherToolbar) toolbar;
                location = locationSwitcherToolbar.getCurrentLocation();
            } else
                location = this.location;
            if (location != null) {
                String locationId = JsonFormUtils.getOpenMrsLocationId(getOpenSRPContext(),
                        location);
                JsonFormUtils.startForm(context, getOpenSRPContext(), REQUEST_CODE_GET_JSON,
                        formName, entityId, null, locationId);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    protected String getCurrentLocation() {
        if (context instanceof BaseActivity) {
            LocationActionView actionView = (LocationActionView) ((BaseActivity) context).getMenu().findItem(R.id.location_switcher)
                    .getActionView();
            return actionView == null ? null : actionView.getSelectedItem();
        } else if (context instanceof ChildSmartRegisterActivity) {
            Fragment mBaseFragment = ((ChildSmartRegisterActivity) context).findFragmentByPosition(0);
            return ((ChildSmartRegisterFragment) mBaseFragment).getLocationPickerView().getSelectedItem();
        } else
            return null;

    }

}
