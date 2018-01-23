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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.smartregister.domain.FetchStatus;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.R;
import org.smartregister.path.adapter.CoverageSpinnerAdapter;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.CoverageHolder;
import org.smartregister.path.domain.Cumulative;
import org.smartregister.path.domain.CumulativeIndicator;
import org.smartregister.path.domain.NamedObject;
import org.smartregister.path.fragment.SetCsoDialogFragment;
import org.smartregister.path.helper.SpinnerHelper;
import org.smartregister.path.receiver.CoverageDropoutBroadcastReceiver;
import org.smartregister.path.repository.CumulativeIndicatorRepository;
import org.smartregister.path.repository.CumulativeRepository;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.PathConstants;
import util.Utils;

/**
 * Created by keyman on 21/12/17.
 */
public class AnnualCoverageReportCsoActivity extends BaseActivity implements SetCsoDialogFragment.OnSetCsoListener, CoverageDropoutBroadcastReceiver.CoverageDropoutServiceListener {
    private static final String TAG = AnnualCoverageReportCsoActivity.class.getCanonicalName();

    //Global data variables
    private List<VaccineRepo.Vaccine> vaccineList = new ArrayList<>();
    private CoverageHolder holder;

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
                Intent intent = new Intent(AnnualCoverageReportCsoActivity.this, CoverageReportsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ((TextView) toolbar.findViewById(R.id.title)).setText(getString(R.string.annual_coverage_report_cso));

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
        return R.layout.activity_annual_coverage_report_cso;
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
        if (holder == null || holder.getId() == null) {
            generateReport();
        } else {
            org.smartregister.util.Utils.startAsyncTask(new UpdateReportTask(this, showProgressBar), new Long[]{holder.getId()});
        }
    }

    private void generateReport() {
        holder = null;
        org.smartregister.util.Utils.startAsyncTask(new GenerateReportTask(this), null);
    }

    private void updateListViewHeader() {
        // Add header
        ListView listView = (ListView) findViewById(R.id.list_view);
        View view = getLayoutInflater().inflate(R.layout.coverage_report_header, null);
        listView.addHeaderView(view);
    }

    private void updateCsoUnder1Population() {
        showSetCsoDialog();

        EditText csoValue = (EditText) findViewById(R.id.cso_value);
        csoValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetCsoDialogFragment.launchDialog(AnnualCoverageReportCsoActivity.this, BaseRegisterActivity.DIALOG_TAG, holder);
            }
        });
        if (holder.getSize() == null) {
            csoValue.setText(getString(R.string.not_defined));
            csoValue.setTextColor(getResources().getColor(R.color.cso_error_red));
        } else {
            csoValue.setText(String.format(getString(R.string.cso_population_value), holder.getSize()));
            csoValue.setTextColor(getResources().getColor(R.color.text_black));
        }
    }

    private void updateReportList(final List<VaccineRepo.Vaccine> vaccineList, final List<CumulativeIndicator> indicators) {
        if (vaccineList == null) {
            return;
        }
        this.vaccineList = vaccineList;
        updateReportList(indicators);

    }

    private void updateReportList(final List<CumulativeIndicator> indicators) {

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
                final LayoutInflater inflater =
                        AnnualCoverageReportCsoActivity.this.getLayoutInflater();
                if (convertView == null) {
                    view = inflater.inflate(R.layout.coverage_report_item, null);
                } else {
                    view = convertView;
                }

                long value = 0;
                final VaccineRepo.Vaccine vaccine = vaccineList.get(position);
                CumulativeIndicator cumulativeIndicator = retrieveIndicator(indicators, vaccine);

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
                if (holder.getSize() == null) {
                    coverageTextView.setText(getString(R.string.no_cso_target));
                    coverageTextView.setTextColor(getResources().getColor(R.color.cso_error_red));
                } else {
                    int percentage = 0;
                    if (value > 0 && holder.getSize() > 0) {
                        percentage = (int) (value * 100.0 / holder.getSize() + 0.5);
                    }
                    coverageTextView.setText(String.format(getString(R.string.coverage_percentage),
                            percentage));
                    coverageTextView.setTextColor(getResources().getColor(R.color.text_black));
                }

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AnnualCoverageReportCsoActivity.this, FacilityCumulativeCoverageReportActivity.class);
                        intent.putExtra(FacilityCumulativeCoverageReportActivity.HOLDER, holder);
                        intent.putExtra(FacilityCumulativeCoverageReportActivity.VACCINE, vaccine);
                        startActivity(intent);
                    }
                });

                return view;
            }
        };

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(baseAdapter);
    }

    private void updateReportDates(List<Cumulative> cumulatives) {
        if (cumulatives != null && !cumulatives.isEmpty()) {

            boolean firstSuffix = false;
            List<CoverageHolder> coverageHolders = new ArrayList<>();
            for (int i = 0; i < cumulatives.size(); i++) {
                Cumulative cumulative = cumulatives.get(i);
                if (i == 0 && Utils.isSameYear(new Date(), cumulative.getYearAsDate())) {
                    firstSuffix = true;
                }
                coverageHolders.add(new CoverageHolder(cumulative.getId(), cumulative.getYearAsDate()));
            }

            View reportDateSpinnerView = findViewById(R.id.cumulative_spinner);
            if (reportDateSpinnerView != null) {
                SpinnerHelper reportDateSpinner = new SpinnerHelper(reportDateSpinnerView);
                CoverageSpinnerAdapter dataAdapter = new CoverageSpinnerAdapter(this, R.layout.item_spinner, coverageHolders, new SimpleDateFormat("yyyy"));

                if (firstSuffix) {
                    dataAdapter.setFirstSuffix(getString(R.string.in_progress));
                }

                dataAdapter.setDropDownViewResource(R.layout.item_spinner_drop_down);
                reportDateSpinner.setAdapter(dataAdapter);

                reportDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Object tag = view.getTag();
                        if (tag != null && tag instanceof CoverageHolder) {
                            holder = (CoverageHolder) tag;
                            refresh(true);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do nothing
                    }
                });
            }
        }
    }

    @Override
    public void updateCsoTargetView(CoverageHolder holder, Long newCsoValue) {
        if (holder != null && holder.getId() != null) {

            CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
            cumulativeRepository.changeCsoNumber(newCsoValue, holder.getId());

            refresh(true);
        }
    }

    private void showSetCsoDialog() {
        if (holder != null && holder.getSize() == null) {
            SetCsoDialogFragment.launchDialog(this, BaseRegisterActivity.DIALOG_TAG, holder);
        }
    }

    private CumulativeIndicator retrieveIndicator(List<CumulativeIndicator> indicators, VaccineRepo.Vaccine vaccine) {
        final String vaccineString = VaccineRepository.addHyphen(vaccine.display().toLowerCase());
        for (CumulativeIndicator cumulativeIndicator : indicators) {
            if (cumulativeIndicator.getVaccine().equals(vaccineString)) {
                return cumulativeIndicator;
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
        if (CoverageDropoutBroadcastReceiver.TYPE_GENERATE_CUMULATIVE_INDICATORS.equals(actionType)) {
            refresh(false);
        }
    }

    public static int getYear(Date date) {
        return Integer.valueOf(CumulativeRepository.DF_YYYY.format(date));
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    private class GenerateReportTask extends AsyncTask<Void, Void, Map<String, NamedObject<?>>> {

        private BaseActivity baseActivity;

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

                CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
                List<Cumulative> cumulatives = cumulativeRepository.fetchAll();
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
                CoverageHolder coverageHolder = new CoverageHolder(cumulative.getId(), cumulative.getYearAsDate(), cumulative.getCsoNumber());

                CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();
                List<CumulativeIndicator> indicators = cumulativeIndicatorRepository.findByCumulativeId(cumulative.getId());

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
                NamedObject<List<Cumulative>> cumulativeNamedObject = new NamedObject<>(Cumulative.class.getName(), cumulatives);
                map.put(cumulativeNamedObject.name, cumulativeNamedObject);

                NamedObject<CoverageHolder> cumulativeHolderNamedObject = new NamedObject<>(CoverageHolder.class.getName(), coverageHolder);
                map.put(cumulativeHolderNamedObject.name, cumulativeHolderNamedObject);

                NamedObject<List<VaccineRepo.Vaccine>> vaccineNamedObject = new NamedObject<>(VaccineRepo.Vaccine.class.getName(), vaccineList);
                map.put(vaccineNamedObject.name, vaccineNamedObject);

                NamedObject<List<CumulativeIndicator>> indicatorMapNamedObject = new NamedObject<>(CumulativeIndicator.class.getName(), indicators);
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

            List<Cumulative> cumulatives = new ArrayList<>();
            List<VaccineRepo.Vaccine> vaccineList = new ArrayList<>();
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
                    holder = (CoverageHolder) namedObject.object;
                }
            }

            if (map.containsKey(VaccineRepo.Vaccine.class.getName())) {
                NamedObject<?> namedObject = map.get(VaccineRepo.Vaccine.class.getName());
                if (namedObject != null) {
                    vaccineList = (List<VaccineRepo.Vaccine>) namedObject.object;
                }
            }

            if (map.containsKey(CumulativeIndicator.class.getName())) {
                NamedObject<?> namedObject = map.get(CumulativeIndicator.class.getName());
                if (namedObject != null) {
                    indicatorList = (List<CumulativeIndicator>) namedObject.object;
                }
            }

            updateCsoUnder1Population();
            updateReportDates(cumulatives);
            updateReportList(vaccineList, indicatorList);
        }
    }

    private class UpdateReportTask extends AsyncTask<Long, Void, Pair<List<CumulativeIndicator>, Long>> {

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
        protected Pair<List<CumulativeIndicator>, Long> doInBackground(Long... params) {

            if (params == null) {
                return null;
            }
            if (params.length == 1) {
                Long cumulativeId = params[0];

                CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
                CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();


                Cumulative cumulative = cumulativeRepository.findById(cumulativeId);
                if (cumulative == null) {
                    return null;
                }

                List<CumulativeIndicator> indicators = cumulativeIndicatorRepository.findByCumulativeId(cumulativeId);

                return Pair.create(indicators, cumulative.getCsoNumber());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Pair<List<CumulativeIndicator>, Long> pair) {
            super.onPostExecute(pair);
            if (showProgressBar) {
                baseActivity.hideProgressDialog();
            }

            if (pair != null) {
                Long size = pair.second;
                holder.setSize(size);
                updateCsoUnder1Population();

                List<CumulativeIndicator> indicators = pair.first;
                updateReportList(indicators);
            }
        }
    }
}
