package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.time.DateUtils;
import org.smartregister.domain.FetchStatus;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.Cohort;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.path.domain.CoverageHolder;
import org.smartregister.path.domain.NamedObject;
import org.smartregister.path.receiver.CoverageDropoutBroadcastReceiver;
import org.smartregister.path.repository.CohortIndicatorRepository;
import org.smartregister.path.repository.CohortPatientRepository;
import org.smartregister.path.repository.CohortRepository;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.PathConstants;

/**
 * Created by keyman on 21/12/17.
 */
public class CohortCoverageReportActivity extends BaseReportActivity implements CoverageDropoutBroadcastReceiver.CoverageDropoutServiceListener {

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
                Intent intent = new Intent(CohortCoverageReportActivity.this, CoverageReportsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ((TextView) toolbar.findViewById(R.id.title)).setText(getString(R.string.cohort_coverage_report));

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
        return R.layout.activity_cohort_coverage_reports;
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

    private void updateCohortSize() {
        Long size = getHolder().getSize();
        if (size == null) {
            size = 0L;
        }

        TextView textView = (TextView) findViewById(R.id.cohort_size_value);
        textView.setText(String.format(getString(R.string.cso_population_value), size));
    }

    @Override
    public void onServiceFinish(String actionType) {
        if (CoverageDropoutBroadcastReceiver.TYPE_GENERATE_COHORT_INDICATORS.equals(actionType)) {
            refresh(false);
        }
    }

    ////////////////////////////////////////////////////////////////
    // Reporting Methods
    ////////////////////////////////////////////////////////////////

    @Override
    protected <T> View generateView(final View view, final VaccineRepo.Vaccine vaccine, final List<T> indicators) {
        long value = 0;

        CohortIndicator cohortIndicator = retrieveCohortIndicator(indicators, vaccine);

        if (cohortIndicator != null) {
            value = cohortIndicator.getValue();
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

        boolean finalized = false;
        Date endDate = util.Utils.getCohortEndDate(vaccine, util.Utils.getLastDayOfMonth(getHolder().getDate()));
        if (endDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endDate);
            calendar.add(Calendar.DATE, 1);
            endDate = calendar.getTime();

            Date currentDate = new Date();
            finalized = !(DateUtils.isSameDay(currentDate, endDate) || currentDate.before(endDate));
        }

        TextView vaccinatedTextView = (TextView) view.findViewById(R.id.vaccinated);
        vaccinatedTextView.setText(String.valueOf(value));

        int percentage = 0;
        if (value > 0 && getHolder().getSize() != null && getHolder().getSize() > 0) {
            percentage = (int) (value * 100.0 / getHolder().getSize() + 0.5);
        }

        TextView coverageTextView = (TextView) view.findViewById(R.id.coverage);
        coverageTextView.setText(String.format(getString(R.string.coverage_percentage),
                percentage));

        vaccinatedTextView.setTextColor(getResources().getColor(R.color.black));
        coverageTextView.setTextColor(getResources().getColor(R.color.black));

        if (finalized) {
            vaccinatedTextView.setTextColor(getResources().getColor(R.color.bluetext));
            coverageTextView.setTextColor(getResources().getColor(R.color.bluetext));
        }
        return view;
    }

    @Override
    protected Map<String, NamedObject<?>> generateReportBackground() {

        CohortRepository cohortRepository = VaccinatorApplication.getInstance().cohortRepository();
        List<Cohort> cohorts = cohortRepository.fetchAll();
        if (cohorts.isEmpty()) {
            return null;
        }

        Collections.sort(cohorts, new Comparator<Cohort>() {
            @Override
            public int compare(Cohort lhs, Cohort rhs) {
                if (lhs.getMonthAsDate() == null) {
                    return 1;
                }
                if (rhs.getMonthAsDate() == null) {
                    return 1;
                }
                return rhs.getMonthAsDate().compareTo(lhs.getMonthAsDate());
            }
        });


        CohortPatientRepository cohortPatientRepository = VaccinatorApplication.getInstance().cohortPatientRepository();

        // Populate the default cohort
        Cohort cohort = cohorts.get(0);

        long cohortSize = cohortPatientRepository.countCohort(cohort.getId());
        CoverageHolder coverageHolder = new CoverageHolder(cohort.getId(), cohort.getMonthAsDate(), cohortSize);

        CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();
        List<CohortIndicator> indicators = cohortIndicatorRepository.findByCohort(cohort.getId());

        Map<String, NamedObject<?>> map = new HashMap<>();
        NamedObject<List<Cohort>> cohortsNamedObject = new NamedObject<>(Cohort.class.getName(), cohorts);
        map.put(cohortsNamedObject.name, cohortsNamedObject);

        NamedObject<CoverageHolder> cohortHolderNamedObject = new NamedObject<>(CoverageHolder.class.getName(), coverageHolder);
        map.put(cohortHolderNamedObject.name, cohortHolderNamedObject);

        NamedObject<List<CohortIndicator>> indicatorMapNamedObject = new NamedObject<>(CohortIndicator.class.getName(), indicators);
        map.put(indicatorMapNamedObject.name, indicatorMapNamedObject);

        return map;
    }

    @Override
    protected void generateReportUI(Map<String, NamedObject<?>> map, boolean userAction) {
        List<Cohort> cohorts = new ArrayList<>();
        List<CohortIndicator> indicatorList = new ArrayList<>();

        if (map.containsKey(Cohort.class.getName())) {
            NamedObject<?> namedObject = map.get(Cohort.class.getName());
            if (namedObject != null) {
                cohorts = (List<Cohort>) namedObject.object;
            }
        }

        if (map.containsKey(CoverageHolder.class.getName())) {
            NamedObject<?> namedObject = map.get(CoverageHolder.class.getName());
            if (namedObject != null) {
                setHolder((CoverageHolder) namedObject.object);
            }
        }

        if (map.containsKey(CohortIndicator.class.getName())) {
            NamedObject<?> namedObject = map.get(CohortIndicator.class.getName());
            if (namedObject != null) {
                indicatorList = (List<CohortIndicator>) namedObject.object;
            }
        }

        updateReportDates(cohorts, new SimpleDateFormat("MMMM yyyy"), null);
        updateCohortSize();
        updateReportList(indicatorList);
    }

    @Override
    protected Pair<List, Long> updateReportBackground(Long id) {

        CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();
        List indicators = cohortIndicatorRepository.findByCohort(id);

        CohortPatientRepository cohortPatientRepository = VaccinatorApplication.getInstance().cohortPatientRepository();
        long cohortSize = cohortPatientRepository.countCohort(id);

        return Pair.create(indicators, cohortSize);
    }

    @Override
    protected void updateReportUI(Pair<List, Long> pair, boolean userAction) {
        setHolderSize(pair.second);
        updateCohortSize();
        updateReportList(pair.first);
    }
}
