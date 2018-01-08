package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.smartregister.domain.FetchStatus;
import org.smartregister.path.R;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by keyman on 18/12/17.
 */
public class CoverageReportsActivity extends BaseActivity {

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

        ListView listView = (ListView) findViewById(R.id.list_view);

        List<String> list = new ArrayList<>();
        list.add(getString(R.string.cohort_coverage_report));
        list.add(getString(R.string.annual_coverage_report_cso));
        list.add(getString(R.string.annual_coverage_report_zeir));

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(CoverageReportsActivity.this, R.layout.coverage_reports_item, R.id.tv, list);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        Intent intent = new Intent(CoverageReportsActivity.this, CohortCoverageReportActivity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        intent = new Intent(CoverageReportsActivity.this, AnnualCoverageReportCsoActivity.class);
                        startActivity(intent);
                        break;
                    case 2:
                        intent = new Intent(CoverageReportsActivity.this, AnnualCoverageReportZeirActivity.class);
                        startActivity(intent);
                        break;
                    default:
                        break;
                }
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
