package org.smartregister.path.activity;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.R;
import org.smartregister.path.adapter.CoverageSpinnerAdapter;
import org.smartregister.path.domain.Cohort;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.path.domain.CoverageHolder;
import org.smartregister.path.domain.Cumulative;
import org.smartregister.path.domain.CumulativeIndicator;
import org.smartregister.path.domain.NamedObject;
import org.smartregister.path.helper.SpinnerHelper;
import org.smartregister.path.repository.CumulativeRepository;
import org.smartregister.util.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by keyman on 1/24/18.
 */

public abstract class BaseReportActivity extends BaseActivity {

    private static final String TAG = BaseReportActivity.class.getCanonicalName();

    //Global data variables
    private List<VaccineRepo.Vaccine> vaccineList = new ArrayList<>();
    private CoverageHolder holder;

    protected void refresh(boolean showProgressBar) {
        if (holder == null || holder.getId() == null) {
            generateReport();
        } else {
            Utils.startAsyncTask(new UpdateReportTask(this, showProgressBar), new Long[]{holder.getId()});
        }
    }

    private void generateReport() {
        holder = null;
        Utils.startAsyncTask(new GenerateReportTask(this), null);
    }

    protected void updateListViewHeader(Integer headerLayout) {
        if (headerLayout == null) {
            return;
        }

        // Add header
        ListView listView = (ListView) findViewById(R.id.list_view);
        View view = getLayoutInflater().inflate(headerLayout, null);
        listView.addHeaderView(view);
    }

    protected <T> void updateReportDates(List list, SimpleDateFormat dateFormat, String suffix) {
        if (list != null && !list.isEmpty()) {

            boolean firstSuffix = false;
            List<CoverageHolder> coverageHolders = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Object object = list.get(i);
                if (object instanceof Cumulative) {
                    Cumulative cumulative = (Cumulative) object;
                    if (i == 0 && util.Utils.isSameYear(new Date(), cumulative.getYearAsDate())) {
                        firstSuffix = true;
                    }
                    coverageHolders.add(new CoverageHolder(cumulative.getId(), cumulative.getYearAsDate()));
                } else if (object instanceof Cohort) {
                    Cohort cohort = (Cohort) object;
                    coverageHolders.add(new CoverageHolder(cohort.getId(), cohort.getMonthAsDate()));
                }
            }

            View reportDateSpinnerView = findViewById(R.id.report_spinner);
            if (reportDateSpinnerView != null) {
                SpinnerHelper reportDateSpinner = new SpinnerHelper(reportDateSpinnerView);
                CoverageSpinnerAdapter dataAdapter = new CoverageSpinnerAdapter(this, R.layout.item_spinner, coverageHolders, dateFormat);

                if (StringUtils.isNotBlank(suffix) && firstSuffix) {
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

    protected <T> void updateReportList(final List<VaccineRepo.Vaccine> vaccineList, final List<T> indicators) {
        if (vaccineList == null) {
            return;
        }
        this.vaccineList = vaccineList;
        updateReportList(indicators);

    }

    protected <T> void updateReportList(final List<T> indicators) {

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
                        BaseReportActivity.this.getLayoutInflater();
                if (convertView == null) {
                    view = inflater.inflate(R.layout.coverage_report_item, null);
                } else {
                    view = convertView;
                }

                final VaccineRepo.Vaccine vaccine = vaccineList.get(position);

                return generateView(view, vaccine, indicators);
            }
        };


        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(baseAdapter);
    }

    protected abstract <T> View generateView(final View view, final VaccineRepo.Vaccine vaccine, final List<T> indicators);

    protected <T> CumulativeIndicator retrieveCumulativeIndicator(List<T> indicators, VaccineRepo.Vaccine vaccine) {
        final String vaccineString = VaccineRepository.addHyphen(vaccine.display().toLowerCase());
        for (T t : indicators) {
            if (t instanceof CumulativeIndicator) {
                CumulativeIndicator cumulativeIndicator = (CumulativeIndicator) t;
                if (cumulativeIndicator.getVaccine().equals(vaccineString)) {
                    return cumulativeIndicator;
                }
            }
        }
        return null;
    }

    protected <T> CohortIndicator retrieveCohortIndicator(List<T> indicators, VaccineRepo.Vaccine vaccine) {
        final String vaccineString = VaccineRepository.addHyphen(vaccine.display().toLowerCase());
        for (T t : indicators) {
            if (t instanceof CohortIndicator) {
                CohortIndicator cohortIndicator = (CohortIndicator) t;
                if (cohortIndicator.getVaccine().equals(vaccineString)) {
                    return cohortIndicator;
                }
            }
        }
        return null;
    }

    @Override
    protected void showProgressDialog() {
        showProgressDialog(getString(R.string.updating_dialog_title), getString(R.string.please_wait_message));
    }

    protected abstract Map<String, NamedObject<?>> generateReportBackground();

    protected abstract void generateReportUI(Map<String, NamedObject<?>> map);

    protected abstract Pair<List, Long> updateReportBackground(Long id);

    protected abstract void updateReportUI(Pair<List, Long> pair);

    public void setHolder(CoverageHolder holder) {
        this.holder = holder;
    }

    public CoverageHolder getHolder() {
        return holder;
    }

    public void setHolderSize(Long size) {
        if (holder != null) {
            holder.setSize(size);
        }
    }

    public static int getYear(Date date) {
        return Integer.valueOf(CumulativeRepository.DF_YYYY.format(date));
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    protected class GenerateReportTask extends AsyncTask<Void, Void, Map<String, NamedObject<?>>> {

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
                return generateReportBackground();
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

            generateReportUI(map);
        }
    }

    protected class UpdateReportTask extends AsyncTask<Long, Void, Pair<List, Long>> {

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
        protected Pair<List, Long> doInBackground(Long... params) {

            if (params == null) {
                return null;
            }
            if (params.length == 1) {
                try {
                    return updateReportBackground(params[0]);
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Pair<List, Long> pair) {
            super.onPostExecute(pair);
            if (showProgressBar) {
                baseActivity.hideProgressDialog();
            }

            if (pair != null) {
                updateReportUI(pair);
            }
        }
    }
}
