package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.smartregister.domain.FetchStatus;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.CoverageHolder;
import org.smartregister.path.domain.Cumulative;
import org.smartregister.path.domain.CumulativeIndicator;
import org.smartregister.path.domain.NamedObject;
import org.smartregister.path.receiver.CoverageDropoutBroadcastReceiver;
import org.smartregister.path.repository.CumulativeIndicatorRepository;
import org.smartregister.path.repository.CumulativeRepository;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Utils;

/**
 * Created by keyman on 21/12/17.
 */
public class AnnualCoverageReportZeirActivity extends BaseReportActivity implements CoverageDropoutBroadcastReceiver.CoverageDropoutServiceListener {

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
                Intent intent = new Intent(AnnualCoverageReportZeirActivity.this, CoverageReportsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ((TextView) toolbar.findViewById(R.id.title)).setText(getString(R.string.annual_coverage_report_zeir));

        updateListViewHeader(R.layout.coverage_report_header);

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
        return R.layout.activity_annual_coverage_report_zeir;
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

    private void updateZeirNumber() {
        Long size = getHolder().getSize();
        if (size == null) {
            size = 0L;
        }

        TextView zeirNumber = (TextView) findViewById(R.id.zeir_number);
        zeirNumber.setText(String.format(getString(R.string.cso_population_value), size));
    }

    @Override
    public void onServiceFinish(String actionType) {
        if (CoverageDropoutBroadcastReceiver.TYPE_GENERATE_CUMULATIVE_INDICATORS.equals(actionType)) {
            refresh(false);
        }
    }

    ////////////////////////////////////////////////////////////////
    // Reporting Methods
    ////////////////////////////////////////////////////////////////

    @Override
    protected <T> View generateView(final View view, final VaccineRepo.Vaccine vaccine, final List<T> indicators) {
        long value = 0;

        CumulativeIndicator cumulativeIndicator = retrieveCumulativeIndicator(indicators, vaccine);

        if (cumulativeIndicator != null) {
            value = cumulativeIndicator.getValue();
        }

        String display = vaccine.display();
        if (vaccine.equals(VaccineRepo.Vaccine.measles1)) {
            display = VaccineRepo.Vaccine.measles1.display() + " / " + VaccineRepo.Vaccine.mr1.display();
        }

        if (vaccine.equals(VaccineRepo.Vaccine.measles2)) {
            display = VaccineRepo.Vaccine.measles2.display() + " / " + VaccineRepo.Vaccine.mr2.display();
        }

        TextView vaccineTextView = (TextView) view.findViewById(R.id.vaccine);
        vaccineTextView.setText(display);

        TextView vaccinatedTextView = (TextView) view.findViewById(R.id.vaccinated);
        vaccinatedTextView.setText(String.valueOf(value));


        TextView coverageTextView = (TextView) view.findViewById(R.id.coverage);

        int percentage = 0;
        if (value > 0 && getHolder().getSize() != null && getHolder().getSize() > 0) {
            percentage = (int) (value * 100.0 / getHolder().getSize() + 0.5);
        }
        coverageTextView.setText(String.format(getString(R.string.coverage_percentage),
                percentage));

        if (Utils.isSameYear(getHolder().getDate(), new Date())) {
            coverageTextView.setTextColor(getResources().getColor(R.color.text_black));
        } else {
            coverageTextView.setTextColor(getResources().getColor(R.color.bluetext));
        }
        return view;
    }

    @Override
    protected Map<String, NamedObject<?>> generateReportBackground() {

        CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
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

        // Populate the default cumulative
        Cumulative cumulative = cumulatives.get(0);
        CoverageHolder coverageHolder = new CoverageHolder(cumulative.getId(), cumulative.getYearAsDate(), cumulative.getZeirNumber());

        CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();
        List<CumulativeIndicator> indicators = cumulativeIndicatorRepository.findByCumulativeId(cumulative.getId());

        Map<String, NamedObject<?>> map = new HashMap<>();
        NamedObject<List<Cumulative>> cumulativeNamedObject = new NamedObject<>(Cumulative.class.getName(), cumulatives);
        map.put(cumulativeNamedObject.name, cumulativeNamedObject);

        NamedObject<CoverageHolder> cumulativeHolderNamedObject = new NamedObject<>(CoverageHolder.class.getName(), coverageHolder);
        map.put(cumulativeHolderNamedObject.name, cumulativeHolderNamedObject);

        NamedObject<List<CumulativeIndicator>> indicatorMapNamedObject = new NamedObject<>(CumulativeIndicator.class.getName(), indicators);
        map.put(indicatorMapNamedObject.name, indicatorMapNamedObject);

        return map;
    }

    @Override
    protected void generateReportUI(Map<String, NamedObject<?>> map, boolean userAction) {
        List<Cumulative> cumulatives = new ArrayList<>();
        List<CumulativeIndicator> indicatorList = new ArrayList<>();

        if (map.containsKey(Cumulative.class.getName())) {
            NamedObject<?> namedObject = map.get(Cumulative.class.getName());
            if (namedObject != null) {
                cumulatives = (List<Cumulative>) namedObject.object;
            }
        }

        if (map.containsKey(CoverageHolder.class.getName())) {
            NamedObject<?> namedObject = map.get(CoverageHolder.class.getName());
            if (namedObject != null) {
                setHolder((CoverageHolder) namedObject.object);
            }
        }

        if (map.containsKey(CumulativeIndicator.class.getName())) {
            NamedObject<?> namedObject = map.get(CumulativeIndicator.class.getName());
            if (namedObject != null) {
                indicatorList = (List<CumulativeIndicator>) namedObject.object;
            }
        }

        updateZeirNumber();
        updateReportDates(cumulatives, CumulativeRepository.DF_YYYY, getString(R.string.in_progress));
        updateReportList(indicatorList);
    }

    @Override
    protected Pair<List, Long> updateReportBackground(Long id) {

        CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
        CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();

        Cumulative cumulative = cumulativeRepository.findById(id);
        if (cumulative == null) {
            return null;
        }

        List indicators = cumulativeIndicatorRepository.findByCumulativeId(id);
        return Pair.create(indicators, cumulative.getZeirNumber());
    }

    @Override
    protected void updateReportUI(Pair<List, Long> pair, boolean userAction) {
        setHolderSize(pair.second);
        updateZeirNumber();
        updateReportList(pair.first);
    }

}
