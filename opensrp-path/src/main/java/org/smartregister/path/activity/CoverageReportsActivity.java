package org.smartregister.path.activity;

import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.FetchStatus;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.Hia2Indicator;
import org.smartregister.path.domain.MonthlyTally;
import org.smartregister.path.fragment.DailyTalliesFragment;
import org.smartregister.path.fragment.DraftMonthlyFragment;
import org.smartregister.path.fragment.SendMonthlyDraftDialogFragment;
import org.smartregister.path.fragment.SentMonthlyFragment;
import org.smartregister.path.repository.DailyTalliesRepository;
import org.smartregister.path.repository.HIA2IndicatorsRepository;
import org.smartregister.path.repository.MonthlyTalliesRepository;
import org.smartregister.path.service.HIA2Service;
import org.smartregister.path.service.intent.HIA2IntentService;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;
import org.smartregister.util.FormUtils;
import org.smartregister.util.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import util.JsonFormUtils;
import util.PathConstants;

/**
 * Created by coder on 6/7/17.
 */
public class CoverageReportsActivity extends BaseActivity {
    private static final String TAG = CoverageReportsActivity.class.getCanonicalName();

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
        LinearLayout hia2 = (LinearLayout) drawer.findViewById(R.id.coverage_reports);
        hia2.setBackgroundColor(getResources().getColor(R.color.tintcolor));
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        super.onSyncComplete(fetchStatus);
    }


    @Override
    protected int getContentView() {
        return R.layout.activity_coverage_reports;
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
