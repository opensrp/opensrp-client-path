package org.smartregister.path.activity;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.smartregister.domain.FetchStatus;
import org.smartregister.path.R;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

/**
 * Created by coder on 6/7/17.
 */
public class DropoutReportsActivity extends BaseActivity {
    private static final String TAG = DropoutReportsActivity.class.getCanonicalName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBarDrawerToggle toggle = getDrawerToggle();
        toggle.setDrawerIndicatorEnabled(false);
        toggle.setHomeAsUpIndicator(null);

        LocationSwitcherToolbar toolbar = (LocationSwitcherToolbar) getToolbar();
        toolbar.setTitle(getString(R.string.side_nav_coverage));

        TextView initialsTV = (TextView) findViewById(R.id.name_inits);
        initialsTV.setText(getLoggedInUserInitials());
        initialsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDrawer();
            }
        });

    }

    @Override
    public void onSyncStart() {
        super.onSyncStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        // TODO: This should go to the base class?
        LinearLayout hia2 = (LinearLayout) drawer.findViewById(R.id.dropout_reports);
        hia2.setBackgroundColor(getResources().getColor(R.color.tintcolor));
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        super.onSyncComplete(fetchStatus);
    }


    @Override
    protected int getContentView() {
        return R.layout.activity_dropout_reports;
    }

    @Override
    protected int getDrawerLayoutId() {
        return R.id.drawer_layout;
    }

    @Override
    protected int getToolbarId() {
        return LocationSwitcherToolbar.TOOLBAR_ID;
    }

    @Override
    protected Class onBackActivity() {
        return null;
    }


}
