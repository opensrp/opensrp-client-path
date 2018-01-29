package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Pair;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.domain.FetchStatus;
import org.smartregister.path.R;
import org.smartregister.path.adapter.ExpandedListAdapter;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by keyman on 08/01/18.
 */
public class PentaCumulativeDropoutReportActivity extends BaseActivity {
    private ExpandableListView expandableListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setTitle("");

        LocationSwitcherToolbar toolbar = (LocationSwitcherToolbar) getToolbar();
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PentaCumulativeDropoutReportActivity.this, DropoutReportsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ((TextView) toolbar.findViewById(R.id.title)).setText(getString(R.string.penta_cumulative_dropout_report));

        expandableListView = (ExpandableListView) findViewById(R.id.expandable_list_view);
        expandableListView.setDivider(null);
        expandableListView.setDividerHeight(0);

        updateExpandableList(formatListData());
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
        return R.layout.activity_dropout_report_template;
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

    @SuppressWarnings("unchecked")
    private void updateExpandableList(final LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> map) {


        ExpandedListAdapter<Pair<String, String>, Triple<String, String, String>, Date> expandableListAdapter = new ExpandedListAdapter(PentaCumulativeDropoutReportActivity.this, map, R.layout.dropout_report_cumulative_header, R.layout.dropout_report_item);
        expandableListView.setAdapter(expandableListAdapter);
        expandableListAdapter.notifyDataSetChanged();
    }

    private LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> formatListData() {
        LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> map = new LinkedHashMap<>();

        List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>> itemDataList = new ArrayList<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (String month : months) {
            ExpandedListAdapter.ItemData<Triple<String, String, String>, Date> itemData = new ExpandedListAdapter.ItemData<>(Triple.of(month, "14 / 100", "14%"), new Date());
            itemDataList.add(itemData);
        }


        map.put(Pair.create("2017", "14%"), itemDataList);
        map.put(Pair.create("2016", "14%"), itemDataList);

        return map;

    }
}

