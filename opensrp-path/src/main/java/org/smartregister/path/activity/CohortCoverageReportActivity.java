package org.smartregister.path.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.Pair;
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
import org.smartregister.path.adapter.CohortSpinnerAdapter;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.Cohort;
import org.smartregister.path.domain.CohortHolder;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.path.helper.SpinnerHelper;
import org.smartregister.path.receiver.CoverageDropoutBroadcastReceiver;
import org.smartregister.path.repository.ChildReportRepository;
import org.smartregister.path.repository.CohortIndicatorRepository;
import org.smartregister.path.repository.CohortRepository;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;
import org.smartregister.util.Utils;

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
public class CohortCoverageReportActivity extends BaseActivity implements CoverageDropoutBroadcastReceiver.CoverageDropoutServiceListener {
    private static final String TAG = CohortCoverageReportActivity.class.getCanonicalName();

    //Global data variables
    private List<VaccineRepo.Vaccine> vaccineList = new ArrayList<>();
    private CohortHolder holder;


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

    private void refresh(boolean showProgressBar) {
        if (holder == null || holder.getCohortId() == null) {
            generateReport();
        } else {
            Utils.startAsyncTask(new UpdateReportTask(this, showProgressBar), new Long[]{holder.getCohortId()});
        }

    }

    private void generateReport() {
        holder = null;
        Utils.startAsyncTask(new GenerateReportTask(this), null);
    }

    private void updateListViewHeader() {
        // Add header
        ListView listView = (ListView) findViewById(R.id.list_view);
        View view = getLayoutInflater().inflate(R.layout.coverage_report_header, null);
        listView.addHeaderView(view);
    }

    private void updateReportList(final List<VaccineRepo.Vaccine> vaccineList, final List<CohortIndicator> indicators) {
        if (vaccineList == null) {
            return;
        }
        this.vaccineList = vaccineList;
        updateReportList(indicators);

    }

    private void updateReportList(final List<CohortIndicator> indicators) {

        if (indicators == null) {
            return;
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
                CohortIndicator cohortIndicator = retrieveIndicator(indicators, vaccine);

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
                Date endDate = util.Utils.getCohortEndDate(vaccine, util.Utils.getLastDayOfMonth(holder.getMonth()));
                if (endDate != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(endDate);
                    calendar.add(Calendar.DATE, 1);
                    endDate = calendar.getTime();

                    Date currentDate = new Date();
                    finalized = !(DateUtils.isSameDay(currentDate, endDate) || currentDate.before(endDate));
                }

                long value = 0;

                if (cohortIndicator != null) {
                    value = cohortIndicator.getValue();
                }

                TextView vaccinatedTextView = (TextView) view.findViewById(R.id.vaccinated);
                vaccinatedTextView.setText(String.valueOf(value));

                int percentage = 0;
                if (value > 0 && holder.getSize() > 0) {
                    percentage = (int) (value * 100.0 / holder.getSize() + 0.5);
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

    private void updateReportDates(List<Cohort> cohorts) {
        if (cohorts != null && !cohorts.isEmpty()) {

            View reportDateSpinnerView = findViewById(R.id.cohort_spinner);
            if (reportDateSpinnerView != null) {
                SpinnerHelper reportDateSpinner = new SpinnerHelper(reportDateSpinnerView);
                CohortSpinnerAdapter dataAdapter = new CohortSpinnerAdapter(this, R.layout.item_spinner, cohorts, new SimpleDateFormat("MMMM yyyy"));
                dataAdapter.setDropDownViewResource(R.layout.item_spinner_drop_down);
                reportDateSpinner.setAdapter(dataAdapter);

                reportDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Object tag = view.getTag();
                        if (tag != null && tag instanceof CohortHolder) {
                            holder = (CohortHolder) tag;
                            refresh(true);
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
        TextView textView = (TextView) findViewById(R.id.cohort_size_value);
        textView.setText(String.format(getString(R.string.cso_population_value), holder.getSize()));
    }

    private CohortIndicator retrieveIndicator(List<CohortIndicator> indicators, VaccineRepo.Vaccine vaccine) {
        final String vaccineString = VaccineRepository.addHyphen(vaccine.display().toLowerCase());
        for (CohortIndicator cohortIndicator : indicators) {
            if (cohortIndicator.getVaccine().equals(vaccineString)) {
                return cohortIndicator;
            }
        }
        return null;
    }

    @Override
    protected void showProgressDialog() {
        showProgressDialog(getString(R.string.updating_dialog_title), getString(R.string.please_wait_message));
    }

    @Override
    public void onServiceFinish(String actionType) {
        if (CoverageDropoutBroadcastReceiver.TYPE_GENERATE_COHORT_INDICATORS.equals(actionType)) {
            refresh(false);
        }
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    private class GenerateReportTask extends AsyncTask<Void, Void, Map<String, NamedObject<?>>> {

        BaseActivity baseActivity;

        private GenerateReportTask(BaseActivity baseActivity) {
            this.baseActivity = baseActivity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            baseActivity.showProgressDialog();
        }

        @Override
        protected Map<String, NamedObject<?>> doInBackground(Void... params) {
            try {

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


                ChildReportRepository childReportRepository = VaccinatorApplication.getInstance().childReportRepository();

                // Populate the default cohort
                Cohort cohort = cohorts.get(0);

                long cohortSize = childReportRepository.countCohort(cohort.getId());
                CohortHolder cohortHolder = new CohortHolder(cohort.getId(), cohort.getMonthAsDate(), cohortSize);


                CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();
                List<CohortIndicator> indicators = cohortIndicatorRepository.findByCohort(cohort.getId());

                List<VaccineRepo.Vaccine> vaccineList = VaccineRepo.getVaccines(PathConstants.EntityType.CHILD);
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


                Map<String, NamedObject<?>> map = new HashMap<>();
                NamedObject<List<Cohort>> cohortsNamedObject = new NamedObject<>(Cohort.class.getName(), cohorts);
                map.put(cohortsNamedObject.name, cohortsNamedObject);

                NamedObject<CohortHolder> cohortHolderNamedObject = new NamedObject<>(CohortHolder.class.getName(), cohortHolder);
                map.put(cohortHolderNamedObject.name, cohortHolderNamedObject);

                NamedObject<List<VaccineRepo.Vaccine>> vaccineNamedObject = new NamedObject<>(VaccineRepo.Vaccine.class.getName(), vaccineList);
                map.put(vaccineNamedObject.name, vaccineNamedObject);

                NamedObject<List<CohortIndicator>> indicatorMapNamedObject = new NamedObject<>(CohortIndicator.class.getName(), indicators);
                map.put(indicatorMapNamedObject.name, indicatorMapNamedObject);


                return map;

            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Map<String, NamedObject<?>> map) {
            super.onPostExecute(map);
            baseActivity.hideProgressDialog();

            if (map == null || map.isEmpty()) {
                return;
            }

            List<Cohort> cohorts = new ArrayList<>();
            List<VaccineRepo.Vaccine> vaccineList = new ArrayList<>();
            List<CohortIndicator> indicatorList = new ArrayList<>();

            if (map.containsKey(Cohort.class.getName())) {
                NamedObject<?> namedObject = map.get(Cohort.class.getName());
                if (namedObject != null) {
                    cohorts = (List<Cohort>) namedObject.object;
                }
            }

            if (map.containsKey(CohortHolder.class.getName())) {
                NamedObject<?> namedObject = map.get(CohortHolder.class.getName());
                if (namedObject != null) {
                    holder = (CohortHolder) namedObject.object;
                }
            }

            if (map.containsKey(VaccineRepo.Vaccine.class.getName())) {
                NamedObject<?> namedObject = map.get(VaccineRepo.Vaccine.class.getName());
                if (namedObject != null) {
                    vaccineList = (List<VaccineRepo.Vaccine>) namedObject.object;
                }
            }

            if (map.containsKey(CohortIndicator.class.getName())) {
                NamedObject<?> namedObject = map.get(CohortIndicator.class.getName());
                if (namedObject != null) {
                    indicatorList = (List<CohortIndicator>) namedObject.object;
                }
            }

            updateReportDates(cohorts);
            updateCohortSize();
            updateReportList(vaccineList, indicatorList);
        }
    }

    private class UpdateReportTask extends AsyncTask<Long, Void, Pair<List<CohortIndicator>, Long>> {

        private BaseActivity baseActivity;
        private boolean showProgressBar;

        private UpdateReportTask(BaseActivity baseActivity, boolean showProgressBar) {
            this.baseActivity = baseActivity;
            this.showProgressBar = showProgressBar;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (showProgressBar) {
                baseActivity.showProgressDialog();
            }
        }

        @Override
        protected Pair<List<CohortIndicator>, Long> doInBackground(Long... params) {

            if (params == null) {
                return null;
            }
            if (params.length == 1) {
                Long cohortId = params[0];

                CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();
                List<CohortIndicator> indicators = cohortIndicatorRepository.findByCohort(cohortId);

                ChildReportRepository childReportRepository = VaccinatorApplication.getInstance().childReportRepository();
                long cohortSize = childReportRepository.countCohort(cohortId);

                return Pair.create(indicators, cohortSize);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Pair<List<CohortIndicator>, Long> pair) {
            super.onPostExecute(pair);
            if (showProgressBar) {
                baseActivity.hideProgressDialog();
            }

            if (pair != null) {
                long cohortSize = pair.second;
                holder.setSize(cohortSize);
                updateCohortSize();

                List<CohortIndicator> indicators = pair.first;
                updateReportList(indicators);
            }
        }
    }

    private class NamedObject<T> {
        public final String name;
        public final T object;

        NamedObject(String name, T object) {
            this.name = name;
            this.object = object;
        }
    }

}
