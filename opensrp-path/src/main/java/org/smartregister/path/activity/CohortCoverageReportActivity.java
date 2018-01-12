package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang3.time.DateUtils;
import org.smartregister.domain.FetchStatus;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.R;
import org.smartregister.path.adapter.SpinnerAdapter;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.ChildReport;
import org.smartregister.path.domain.Cohort;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.path.helper.SpinnerHelper;
import org.smartregister.path.repository.ChildReportRepository;
import org.smartregister.path.repository.CohortIndicatorRepository;
import org.smartregister.path.repository.CohortRepository;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import util.PathConstants;

/**
 * Created by keyman on 21/12/17.
 */
public class CohortCoverageReportActivity extends BaseActivity {

    private Date currentDate = null;
    private long cohortSize;
    private Map<String, Cohort> map;

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

        updateListViewHeader();
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

        CohortRepository cohortRepository = VaccinatorApplication.getInstance().cohortRepository();
        List<Cohort> cohorts = cohortRepository.fetchAll();
        Collections.reverse(cohorts);

        List<Date> dates = new ArrayList<>();
        map = new HashMap<>();

        for (Cohort cohort : cohorts) {
            dates.add(cohort.getMonthAsDate());
            map.put(cohort.getMonth(), cohort);
        }
        currentDate = dates.get(0);

        updateReportDates(dates);
        updateReportList();

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

    private void updateListViewHeader() {
        // Add header
        ListView listView = (ListView) findViewById(R.id.list_view);
        View view = getLayoutInflater().inflate(R.layout.coverage_report_header, null);
        listView.addHeaderView(view);
    }

    private void updateReportList() {

        final Cohort cohort = getCurrentCohort();
        if (cohort == null) {
            return;
        }

        updateCohortSize();

        CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();
        final List<CohortIndicator> indicators = cohortIndicatorRepository.findByCohort(cohort.getId());

        final List<VaccineRepo.Vaccine> vaccineList = VaccineRepo.getVaccines(PathConstants.EntityType.CHILD);
        Collections.sort(vaccineList, new Comparator<VaccineRepo.Vaccine>() {
            @Override
            public int compare(VaccineRepo.Vaccine lhs, VaccineRepo.Vaccine rhs) {
                return lhs.display().compareToIgnoreCase(rhs.display());
            }
        });

        vaccineList.remove(VaccineRepo.Vaccine.bcg2);
        vaccineList.remove(VaccineRepo.Vaccine.ipv);
        vaccineList.remove(VaccineRepo.Vaccine.measles1);
        vaccineList.remove(VaccineRepo.Vaccine.measles2);
        vaccineList.remove(VaccineRepo.Vaccine.mr1);
        vaccineList.remove(VaccineRepo.Vaccine.mr2);


        vaccineList.add(VaccineRepo.Vaccine.measles1);
        vaccineList.add(VaccineRepo.Vaccine.measles2);

        final Map<VaccineRepo.Vaccine, CohortIndicator> map = new LinkedHashMap<>();
        for (VaccineRepo.Vaccine vaccine : vaccineList) {
            final String vaccineString = VaccineRepository.addHyphen(vaccine.display().toLowerCase());
            for (CohortIndicator cohortIndicator : indicators) {
                if (cohortIndicator.getVaccine().equals(vaccineString)) {
                    map.put(vaccine, cohortIndicator);
                }
            }

        }

        BaseAdapter baseAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return vaccineList.size();
            }

            @Override
            public Object getItem(int position) {
                return vaccineList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view;
                LayoutInflater inflater =
                        CohortCoverageReportActivity.this.getLayoutInflater();
                if (convertView == null) {
                    view = inflater.inflate(R.layout.coverage_report_item, null);
                } else {
                    view = convertView;
                }

                VaccineRepo.Vaccine vaccine = vaccineList.get(position);
                String display = vaccine.display();
                if (vaccine.equals(VaccineRepo.Vaccine.measles1)) {
                    display = VaccineRepo.Vaccine.measles1.display() + " / " + VaccineRepo.Vaccine.mr1.display();
                }

                if (vaccine.equals(VaccineRepo.Vaccine.measles2)) {
                    display = VaccineRepo.Vaccine.measles2.display() + " / " + VaccineRepo.Vaccine.mr2.display();
                }

                TextView vaccineTextView = (TextView) view.findViewById(R.id.vaccine);
                vaccineTextView.setText(display);

                CohortIndicator cohortIndicator = map.get(vaccine);

                long value = 0;
                boolean finalized = false;
                if (cohortIndicator != null) {
                    Date currentDate = new Date();
                    finalized = !(DateUtils.isSameDay(currentDate, cohortIndicator.getEndDate()) || currentDate.before(cohortIndicator.getEndDate()));
                    value = cohortIndicator.getValue();
                }

                TextView vaccinatedTextView = (TextView) view.findViewById(R.id.vaccinated);
                vaccinatedTextView.setText(String.valueOf(value));

                int percentage = 0;
                if (value > 0 && cohortSize > 0) {
                    percentage = (int) (value * 100.0 / cohortSize + 0.5);
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
        };

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(baseAdapter);
    }

    private void updateReportDates(List<Date> dates) {
        if (dates != null && !dates.isEmpty()) {
            View reportDateSpinnerView = findViewById(R.id.cohort_spinner);
            if (reportDateSpinnerView != null) {
                SpinnerHelper reportDateSpinner = new SpinnerHelper(reportDateSpinnerView);
                SpinnerAdapter dataAdapter = new SpinnerAdapter(this, R.layout.item_spinner, dates, new SimpleDateFormat("MMMM yyyy"));
                dataAdapter.setDropDownViewResource(R.layout.item_spinner_drop_down);
                reportDateSpinner.setAdapter(dataAdapter);

                reportDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Object tag = view.getTag();
                        if (tag != null && tag instanceof Date) {
                            currentDate = (Date) tag;
                            updateReportList();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do Nothing
                    }
                });
            }
        }
    }

    private void updateCohortSize() {
        ChildReportRepository childReportRepository = VaccinatorApplication.getInstance().childReportRepository();
        Cohort cohort = getCurrentCohort();
        cohortSize = childReportRepository.countCohort(cohort.getId());
        TextView textView = (TextView) findViewById(R.id.cohort_size_value);
        textView.setText(String.format(getString(R.string.cso_population_value), cohortSize));

    }

    private Cohort getCurrentCohort() {
        if (currentDate == null) {
            return null;
        }
        String month = CohortRepository.DF_YYYYMM.format(currentDate);
        return map.get(month);
    }

}
