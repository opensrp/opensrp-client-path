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
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.path.R;
import org.smartregister.path.adapter.ExpandedListAdapter;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.Cumulative;
import org.smartregister.path.domain.CumulativeIndicator;
import org.smartregister.path.domain.NamedObject;
import org.smartregister.path.receiver.CoverageDropoutBroadcastReceiver;
import org.smartregister.path.repository.CumulativeIndicatorRepository;
import org.smartregister.path.repository.CumulativeRepository;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by keyman on 08/01/18.
 */
public class BcgMeaslesCumulativeDropoutReportActivity extends BaseReportActivity implements CoverageDropoutBroadcastReceiver.CoverageDropoutServiceListener {
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
                Intent intent = new Intent(BcgMeaslesCumulativeDropoutReportActivity.this, DropoutReportsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ((TextView) toolbar.findViewById(R.id.title)).setText(getString(R.string.bcg_measles_cumulative_dropout_report));

        expandableListView = (ExpandableListView) findViewById(R.id.expandable_list_view);
        expandableListView.setDivider(null);
        expandableListView.setDividerHeight(0);

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

        refresh(true);

        CoverageDropoutBroadcastReceiver.getInstance().addCoverageDropoutServiceListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CoverageDropoutBroadcastReceiver.getInstance().removeCoverageDropoutServiceListener(this);
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

    @Override
    public void onServiceFinish(String actionType) {
        if (CoverageDropoutBroadcastReceiver.TYPE_GENERATE_CUMULATIVE_INDICATORS.equals(actionType)) {
            refresh(false);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateExpandableList(final LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> map) {

        ExpandedListAdapter<Pair<String, String>, Triple<String, String, String>, Date> expandableListAdapter = new ExpandedListAdapter(BcgMeaslesCumulativeDropoutReportActivity.this, map, R.layout.dropout_report_cumulative_header, R.layout.dropout_report_item);
        expandableListAdapter.setChildSelectable(false);
        expandableListView.setAdapter(expandableListAdapter);
        expandableListAdapter.notifyDataSetChanged();
    }

    @Override
    protected Map<String, NamedObject<?>> generateReportBackground() {
        SimpleDateFormat monthDateFormat = new SimpleDateFormat("MMMM");
        CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
        CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();

        if (cumulativeRepository == null || cumulativeIndicatorRepository == null) {
            return null;
        }

        List<Cumulative> cumulatives = cumulativeRepository.fetchAllWithIndicators();
        if (cumulatives.isEmpty()) {
            return null;
        }

        Collections.sort(cumulatives, new Comparator<Cumulative>() {
            @Override
            public int compare(Cumulative lhs, Cumulative rhs) {
                if (lhs.getYearAsDate() == null) {
                    return 1;
                }
                if (rhs.getYearAsDate() == null) {
                    return 1;
                }
                return rhs.getYearAsDate().compareTo(lhs.getYearAsDate());
            }
        });

        LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> linkedHashMap = new LinkedHashMap<>();

        for (Cumulative cumulative : cumulatives) {

            List<Date> months = generateMonths(cumulative.getYearAsDate());
            List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>> itemDataList = new ArrayList<>();
            long totalDiff = 0L;
            long totalBcg = 0L;

            for (Date month : months) {

                String bcgVaccineName = generateVaccineName(VaccineRepo.Vaccine.bcg);
                CumulativeIndicator bcgCumulativeIndicator = cumulativeIndicatorRepository.findByVaccineMonthAndCumulativeId(bcgVaccineName, month, cumulative.getId());

                String measlesVaccineName = generateVaccineName(VaccineRepo.Vaccine.measles1);
                CumulativeIndicator measlesCumulativeIndicator = cumulativeIndicatorRepository.findByVaccineMonthAndCumulativeId(measlesVaccineName, month, cumulative.getId());

                long bcg = 0L;
                if (bcgCumulativeIndicator != null && bcgCumulativeIndicator.getValue() != null) {
                    bcg = bcgCumulativeIndicator.getValue();
                    totalBcg += bcg;
                }

                long measles = 0L;
                if (measlesCumulativeIndicator != null && measlesCumulativeIndicator.getValue() != null) {
                    measles = measlesCumulativeIndicator.getValue();
                }

                long diff = bcg - measles;
                totalDiff += diff;


                int percentage = 0;
                if (bcg > 0) {
                    percentage = (int) (diff * 100.0 / bcg + 0.5);
                }

                String monthString = monthDateFormat.format(month);
                ExpandedListAdapter.ItemData<Triple<String, String, String>, Date> itemData = new ExpandedListAdapter.ItemData<>(Triple.of(monthString, diff + " / " + bcg, String.format(getString(R.string.coverage_percentage),
                        percentage)), month);
                itemDataList.add(itemData);

            }

            int totalPercentage = 0;
            if (totalBcg > 0) {
                totalPercentage = (int) (totalDiff * 100.0 / totalBcg + 0.5);
            }
            linkedHashMap.put(Pair.create(String.valueOf(cumulative.getYear()), String.format(getString(R.string.coverage_percentage), totalPercentage)), itemDataList);
        }

        Map<String, NamedObject<?>> map = new HashMap<>();

        NamedObject<LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>>> linkedHashMapNamedObject = new NamedObject<>(LinkedHashMap.class.getName(), linkedHashMap);
        map.put(linkedHashMapNamedObject.name, linkedHashMapNamedObject);


        return map;
    }

    @Override
    protected void generateReportUI(Map<String, NamedObject<?>> map, boolean userAction) {
        LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> linkedHashMap = new LinkedHashMap<>();

        if (map.containsKey(LinkedHashMap.class.getName())) {
            NamedObject<?> namedObject = map.get(LinkedHashMap.class.getName());
            if (namedObject != null) {
                linkedHashMap = (LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>>) namedObject.object;
            }
        }

        updateExpandableList(linkedHashMap);

    }

    @Override
    protected Pair<List, Long> updateReportBackground(Long id) {
        return null;
    }

    @Override
    protected void updateReportUI(Pair<List, Long> pair, boolean userAction) {

    }
}

