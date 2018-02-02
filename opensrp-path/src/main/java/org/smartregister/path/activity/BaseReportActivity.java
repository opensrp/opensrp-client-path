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
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.R;
import org.smartregister.path.adapter.CoverageSpinnerAdapter;
import org.smartregister.path.adapter.ExpandedListAdapter;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.Cohort;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.path.domain.CoverageHolder;
import org.smartregister.path.domain.Cumulative;
import org.smartregister.path.domain.CumulativeIndicator;
import org.smartregister.path.domain.NamedObject;
import org.smartregister.path.helper.SpinnerHelper;
import org.smartregister.path.repository.CohortIndicatorRepository;
import org.smartregister.path.repository.CohortPatientRepository;
import org.smartregister.path.repository.CohortRepository;
import org.smartregister.path.repository.CumulativeIndicatorRepository;
import org.smartregister.path.repository.CumulativeRepository;
import org.smartregister.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import util.PathConstants;

/**
 * Created by keyman on 1/24/18.
 */

public abstract class BaseReportActivity extends BaseActivity {

    private static final String TAG = BaseReportActivity.class.getCanonicalName();
    public static final String DIALOG_TAG = "report_dialog";

    //Global data variables
    private List<VaccineRepo.Vaccine> vaccineList = new ArrayList<>();
    private CoverageHolder holder;

    protected void refresh(boolean userAction) {
        if (holder == null || holder.getId() == null) {
            generateReport(userAction);
        } else {
            Utils.startAsyncTask(new UpdateReportTask(this, userAction), new Long[]{holder.getId()});
        }
    }

    protected void generateReport(boolean userAction) {
        holder = null;
        Utils.startAsyncTask(new GenerateReportTask(this, userAction), null);
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

    protected <T> void updateReportDates(List list, SimpleDateFormat dateFormat, String suffix, boolean toUpperCase) {
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

                if (toUpperCase) {
                    dataAdapter.setToUpperCase(true);
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

    protected <T> void updateReportDates(List list, SimpleDateFormat dateFormat, String suffix) {
        updateReportDates(list, dateFormat, suffix, false);

    }

    protected <T> void updateReportList(final List<T> indicators) {
        if (vaccineList == null || vaccineList.isEmpty()) {
            vaccineList = generateVaccineList();
        }

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

    protected <T> View generateView(final View view, final VaccineRepo.Vaccine vaccine, final List<T> indicators) {
        return view;
    }

    protected <T> Long retrieveCumulativeIndicatorValue(List<T> indicators, VaccineRepo.Vaccine vaccine) {
        long value = 0L;
        final String vaccineString = VaccineRepository.addHyphen(vaccine.display().toLowerCase());
        for (T t : indicators) {
            if (t instanceof CumulativeIndicator) {
                CumulativeIndicator cumulativeIndicator = (CumulativeIndicator) t;
                if (cumulativeIndicator.getVaccine().equals(vaccineString)) {
                    value += cumulativeIndicator.getValue();
                }
            }
        }
        return value;
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

    protected LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> generateCumulativeDropoutMap(VaccineRepo.Vaccine started, VaccineRepo.Vaccine completed) {
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

        LinkedHashMap<Pair<String, String>, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> linkedHashMap = new LinkedHashMap<>();
        for (Cumulative cumulative : cumulatives) {
            long totalDiff = 0L;
            long totalStarted = 0L;

            List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>> itemDataList = new ArrayList<>();

            String startedVaccineName = generateVaccineName(started);
            List<CumulativeIndicator> cumulativeIndicators = cumulativeIndicatorRepository.findByVaccineAndCumulativeId(startedVaccineName, cumulative.getId(), CumulativeIndicatorRepository.COLUMN_MONTH + " DESC ");
            for (CumulativeIndicator startedCumulativeIndicator : cumulativeIndicators) {

                long startCount = 0L;
                if (startedCumulativeIndicator != null && startedCumulativeIndicator.getValue() != null) {
                    startCount = startedCumulativeIndicator.getValue();
                }

                // If denominator is zero, skip
                if (startCount == 0L) {
                    continue;
                }

                Date month = startedCumulativeIndicator.getMonthAsDate();
                String completedVaccineName = generateVaccineName(completed);
                CumulativeIndicator completedCumulativeIndicator = cumulativeIndicatorRepository.findByVaccineMonthAndCumulativeId(completedVaccineName, month, cumulative.getId());

                long completeCount = 0L;
                if (completedCumulativeIndicator != null && completedCumulativeIndicator.getValue() != null) {
                    completeCount = completedCumulativeIndicator.getValue();
                }

                long diff = startCount - completeCount;
                int percentage = (int) (diff * 100.0 / startCount + 0.5);

                String monthString = monthDateFormat.format(month);
                ExpandedListAdapter.ItemData<Triple<String, String, String>, Date> itemData = new ExpandedListAdapter.ItemData<>(Triple.of(monthString, diff + " / " + startCount, String.format(getString(R.string.coverage_percentage),
                        percentage)), month);
                itemDataList.add(itemData);

                totalStarted += startCount;
                totalDiff += diff;
            }

            if (totalStarted > 0) {
                int totalPercentage = (int) (totalDiff * 100.0 / totalStarted + 0.5);
                linkedHashMap.put(Pair.create(String.valueOf(cumulative.getYear()), String.format(getString(R.string.coverage_percentage), totalPercentage)), itemDataList);
            }

        }
        return linkedHashMap;
    }

    protected LinkedHashMap<String, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> generateCohortDropoutMap(VaccineRepo.Vaccine started, VaccineRepo.Vaccine completed) {
        SimpleDateFormat monthDateFormat = new SimpleDateFormat("MMMM");
        CohortRepository cohortRepository = VaccinatorApplication.getInstance().cohortRepository();
        CohortPatientRepository cohortPatientRepository = VaccinatorApplication.getInstance().cohortPatientRepository();
        CohortIndicatorRepository cohortIndicatorRepository = VaccinatorApplication.getInstance().cohortIndicatorRepository();

        if (cohortRepository == null || cohortPatientRepository == null || cohortIndicatorRepository == null) {
            return null;
        }

        List<Cohort> cohorts = cohortRepository.fetchAll();
        LinkedHashMap<String, List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>>> linkedHashMap = new LinkedHashMap<>();

        for (Cohort cohort : cohorts) {
            String startedVaccineName = generateVaccineName(started);
            CohortIndicator startedCohortIndicator = cohortIndicatorRepository.findByVaccineAndCohort(startedVaccineName, cohort.getId());

            long startCount = 0L;
            if (startedCohortIndicator != null && startedCohortIndicator.getValue() != null) {
                startCount = startedCohortIndicator.getValue();
            }

            // If denominator is zero, skip
            if (startCount == 0L) {
                continue;
            }

            String completedVaccineName = generateVaccineName(completed);
            CohortIndicator completedCohortIndicator = cohortIndicatorRepository.findByVaccineAndCohort(completedVaccineName, cohort.getId());

            long completeCount = 0L;
            if (completedCohortIndicator != null && completedCohortIndicator.getValue() != null) {
                completeCount = completedCohortIndicator.getValue();
            }

            long diff = startCount - completeCount;
            int percentage = (int) (diff * 100.0 / startCount + 0.5);

            boolean isFinalized = isFinalized(completed, cohort.getMonthAsDate());
            String monthString = monthDateFormat.format(cohort.getMonthAsDate());
            ExpandedListAdapter.ItemData<Triple<String, String, String>, Date> itemData = new ExpandedListAdapter.ItemData<>(Triple.of(monthString, diff + " / " + startCount, String.format(getString(R.string.coverage_percentage),
                    percentage)), cohort.getMonthAsDate());
            itemData.setFinalized(isFinalized);

            Integer year = util.Utils.yearFromDate(cohort.getMonthAsDate());
            List<ExpandedListAdapter.ItemData<Triple<String, String, String>, Date>> itemDataList = linkedHashMap.get(year.toString());
            if (itemDataList == null) {
                itemDataList = new ArrayList<>();
                linkedHashMap.put(year.toString(), itemDataList);
            }
            itemDataList.add(itemData);
        }
        return linkedHashMap;
    }

    protected Long changeZeirNumberFor2017(long zeirNumber, int year) {
        // Zeir number will be zero the first year (2017)
        // get the next year's zeir number

        if (zeirNumber == 0L && year == 2017) {
            CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
            if (cumulativeRepository != null) {
                Cumulative nextCumulative = cumulativeRepository.findByYear((2017 + 1));
                if (nextCumulative != null && nextCumulative.getZeirNumber() != null) {
                    return nextCumulative.getZeirNumber();
                }
            }
        }
        return zeirNumber;
    }

    @Override
    protected void showProgressDialog() {
        showProgressDialog(getString(R.string.updating_dialog_title), getString(R.string.please_wait_message));
    }

    protected abstract Map<String, NamedObject<?>> generateReportBackground();

    protected abstract void generateReportUI(Map<String, NamedObject<?>> map, boolean userAction);

    protected Pair<List, Long> updateReportBackground(Long id) {
        return null;
    }

    protected void updateReportUI(Pair<List, Long> pair, boolean userAction) {
        // Override to implement this
    }

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

    private List<VaccineRepo.Vaccine> generateVaccineList() {
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

        return vaccineList;
    }

    protected String generateVaccineName(VaccineRepo.Vaccine vaccine) {
        if (vaccine == null) {
            return null;
        }

        return VaccineRepository.addHyphen(vaccine.display().toLowerCase());
    }

    protected boolean isFinalized(VaccineRepo.Vaccine vaccine, Date date) {
        boolean finalized = false;
        Date endDate = util.Utils.getCohortEndDate(vaccine, util.Utils.getLastDayOfMonth(date));
        if (endDate != null) {
            Date currentDate = new Date();
            finalized = !(DateUtils.isSameDay(currentDate, endDate) || endDate.after(currentDate));
        }

        return finalized;
    }

    ////////////////////////////////////////////////////////////////
// Inner classes
////////////////////////////////////////////////////////////////
    protected class GenerateReportTask extends AsyncTask<Void, Void, Map<String, NamedObject<?>>> {

        private BaseActivity baseActivity;
        private boolean userAction;

        private GenerateReportTask(BaseActivity baseActivity, boolean userAction) {
            this.baseActivity = baseActivity;
            this.userAction = userAction;
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

            generateReportUI(map, userAction);
        }
    }

    protected class UpdateReportTask extends AsyncTask<Long, Void, Pair<List, Long>> {

        private BaseActivity baseActivity;
        private boolean userAction;

        private UpdateReportTask(BaseActivity baseActivity, boolean userAction) {
            this.baseActivity = baseActivity;
            this.userAction = userAction;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (userAction) {
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
            if (userAction) {
                baseActivity.hideProgressDialog();
            }

            if (pair != null) {
                updateReportUI(pair, userAction);
            }
        }
    }
}
