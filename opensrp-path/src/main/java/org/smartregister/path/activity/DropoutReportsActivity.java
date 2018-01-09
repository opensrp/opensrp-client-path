package org.smartregister.path.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
public class DropoutReportsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBarDrawerToggle toggle = getDrawerToggle();
        toggle.setDrawerIndicatorEnabled(false);
        toggle.setHomeAsUpIndicator(null);

        LocationSwitcherToolbar toolbar = (LocationSwitcherToolbar) getToolbar();
        toolbar.setTitle(getString(R.string.side_nav_dropout));

        TextView initialsTV = (TextView) findViewById(R.id.name_inits);
        initialsTV.setText(getLoggedInUserInitials());
        initialsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDrawer();
            }
        });


        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setDivider(null);
        listView.setDividerHeight(0);

        List<String[]> list = new ArrayList<>();
        list.add(new String[]{getString(R.string.bcg_measles_cumulative), getString(R.string.bcg_measles_cohort)});
        list.add(new String[]{getString(R.string.penta_cumulative), getString(R.string.penta_cohort)});
        list.add(new String[]{getString(R.string.measles_cumulative)});

        DropoutArrayAdapter arrayAdapter = new DropoutArrayAdapter(DropoutReportsActivity.this, list);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

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

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    private class DropoutArrayAdapter extends ArrayAdapter<String[]> {


        public DropoutArrayAdapter(@NonNull Context context, @NonNull List<String[]> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String[] items = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.dropout_reports_item, parent, false);
            }

            if (items == null) {
                return convertView;
            }

            View rev1 = convertView.findViewById(R.id.rev1);
            View rev2 = convertView.findViewById(R.id.rev2);
            View divider = convertView.findViewById(R.id.adapter_divider_bottom);
            rev2.setVisibility(View.VISIBLE);

            if (items.length > 0) {
                String currentItem = items[0];
                TextView tvName = (TextView) convertView.findViewById(R.id.tv);
                tvName.setText(currentItem);

                if (currentItem.equals(getString(R.string.bcg_measles_cumulative))) {
                    rev1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(DropoutReportsActivity.this, BcgMeaslesCumulativeDropoutReportActivity.class);
                            startActivity(intent);
                        }
                    });
                } else if (currentItem.equals(getString(R.string.penta_cumulative))) {
                    rev1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(DropoutReportsActivity.this, PentaCumulativeDropoutReportActivity.class);
                            startActivity(intent);
                        }
                    });

                } else if (currentItem.equals(getString(R.string.measles_cumulative))) {
                    rev1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(DropoutReportsActivity.this, MeaslesCumulativeDropoutReportActivity.class);
                            startActivity(intent);
                        }
                    });

                }
            }


            if (items.length > 1) {
                String currentItem = items[1];
                TextView tvName = (TextView) convertView.findViewById(R.id.tv2);
                tvName.setText(currentItem);

                if (currentItem.equals(getString(R.string.bcg_measles_cohort))) {
                    rev2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(DropoutReportsActivity.this, BcgMeaslesCohortDropoutReportActivity.class);
                            startActivity(intent);
                        }
                    });

                } else if (currentItem.equals(getString(R.string.penta_cohort))) {
                    rev2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(DropoutReportsActivity.this, PentaCohortDropoutReportActivity.class);
                            startActivity(intent);
                        }
                    });
                }
            } else {
                rev2.setVisibility(View.GONE);
                divider.setVisibility(View.GONE);
            }
            // Lookup view for data population
            return convertView;
        }
    }

}
