package org.smartregister.path.activity;

import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.FetchStatus;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.Hia2Indicator;
import org.smartregister.path.domain.MonthlyTally;
import org.smartregister.path.fragment.DailyTalliesFragment;
import org.smartregister.path.fragment.DraftMonthlyFragment;
import org.smartregister.path.fragment.SendMonthlyDraftDialogFragment;
import org.smartregister.path.fragment.SentMonthlyFragment;
import org.smartregister.path.repository.DailyTalliesRepository;
import org.smartregister.path.repository.HIA2IndicatorsRepository;
import org.smartregister.path.repository.MonthlyTalliesRepository;
import org.smartregister.path.service.HIA2Service;
import org.smartregister.path.service.intent.HIA2IntentService;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;
import org.smartregister.util.FormUtils;
import org.smartregister.util.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import util.JsonFormUtils;
import util.PathConstants;

import static util.Utils.DuplicateDialogGuard.findDuplicateDialogFragment;

/**
 * Created by coder on 6/7/17.
 */
public class HIA2ReportsActivity extends BaseActivity {
    private static final String TAG = HIA2ReportsActivity.class.getCanonicalName();
    private static final int REQUEST_CODE_GET_JSON = 3432;
    public static final int MONTH_SUGGESTION_LIMIT = 3;
    private static final String FORM_KEY_CONFIRM = "confirm";
    private static final List<String> readOnlyList = new ArrayList<>(Arrays.asList(HIA2Service.CHN1_011, HIA2Service.CHN1_021, HIA2Service.CHN1_025, HIA2Service.CHN2_015, HIA2Service.CHN2_030, HIA2Service.CHN2_041, HIA2Service.CHN2_051, HIA2Service.CHN2_061));
    private HashMap<String, Long> lastOpenedDialog;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private TabLayout tabLayout;
    private ProgressDialog progressDialog;
    private boolean showFragment = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lastOpenedDialog = new HashMap<>();

        ActionBarDrawerToggle toggle = getDrawerToggle();
        toggle.setDrawerIndicatorEnabled(false);
        toggle.setHomeAsUpIndicator(null);

        LocationSwitcherToolbar toolbar = (LocationSwitcherToolbar) getToolbar();
        toolbar.setTitle(getString(R.string.side_nav_hia2));

        tabLayout = (TabLayout) findViewById(R.id.tabs);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout.setupWithViewPager(mViewPager);

        TextView initialsTV = (TextView) findViewById(R.id.name_inits);
        initialsTV.setText(getLoggedInUserInitials());
        initialsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDrawer();
            }
        });

        // Update Draft Monthly Title
        refreshDraftMonthlyTitle();
    }

    @Override
    public void onSyncStart() {
        super.onSyncStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        // TODO: This should go to the base class?
        LinearLayout hia2 = (LinearLayout) drawer.findViewById(R.id.hia2_reports);
        hia2.setBackgroundColor(getResources().getColor(R.color.tintcolor));
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        super.onSyncComplete(fetchStatus);
    }

    private Fragment currentFragment() {
        if (mViewPager == null || mSectionsPagerAdapter == null) {
            return null;
        }

        return mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());
    }

    public void startMonthlyReportForm(String formName, Date date, boolean firstTimeEdit) {
        try {
            Fragment currentFragment = currentFragment();
            if (currentFragment instanceof DraftMonthlyFragment) {
                Utils.startAsyncTask(
                        new StartDraftMonthlyFormTask(this, date, formName, firstTimeEdit), null);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            try {
                showFragment = true;
                String jsonString = data.getStringExtra("json");
                boolean skipValidationSet = data.getBooleanExtra(JsonFormConstants.SKIP_VALIDATION, false);
                JSONObject form = new JSONObject(jsonString);
                String monthString = form.getString("report_month");
                Date month = HIA2Service.dfyymmdd.parse(monthString);

                JSONObject monthlyDraftForm = new JSONObject(jsonString);
                Map<String, String> result = JsonFormUtils.sectionFields(monthlyDraftForm);
                boolean saveClicked;
                if (result.containsKey(FORM_KEY_CONFIRM)) {
                    saveClicked = Boolean.valueOf(result.get(FORM_KEY_CONFIRM));
                    result.remove(FORM_KEY_CONFIRM);
                    if (skipValidationSet) {
                        Snackbar.make(tabLayout, R.string.all_changes_saved, Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    saveClicked = false;
                }
                VaccinatorApplication.getInstance().monthlyTalliesRepository().save(result, month);
                if (saveClicked && !skipValidationSet) {
                    sendReport(month);
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            } catch (ParseException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        showFragment = false;
    }

    private void sendReport(final Date month) {
        if (month != null) {
            String dialogTag = SendMonthlyDraftDialogFragment.class.getName();
            int isDuplicateDialog = util.Utils.DuplicateDialogGuard.findDuplicateDialogFragment(this,
                    dialogTag, lastOpenedDialog);
            if (isDuplicateDialog == -1 || isDuplicateDialog == 1) {
                return;
            }

            String monthString = new SimpleDateFormat("MMM yyyy").format(month);
            // Create and show the dialog.
            SendMonthlyDraftDialogFragment newFragment = SendMonthlyDraftDialogFragment
                    .newInstance(monthString,
                            MonthlyTalliesRepository.DF_DDMMYY.format(Calendar.getInstance().getTime()),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(HIA2ReportsActivity.this,
                                            HIA2IntentService.class);
                                    intent.putExtra(HIA2IntentService.GENERATE_REPORT, true);
                                    intent.putExtra(HIA2IntentService.REPORT_MONTH,
                                            HIA2Service.dfyymm.format(month));
                                    startService(intent);
                                }
                            });

            FragmentTransaction ft = getFragmentManager()
                    .beginTransaction();
            ft.add(newFragment, dialogTag);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_hia2_reports;
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

    public void refreshDraftMonthlyTitle() {
        Utils.startAsyncTask(new FetchEditedMonthlyTalliesTask(new FetchEditedMonthlyTalliesTask.TaskListener() {
            @Override
            public void onPostExecute(final List<MonthlyTally> monthlyTallies) {
                tabLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
                            TabLayout.Tab tab = tabLayout.getTabAt(i);
                            if (tab != null && tab.getText() != null && tab.getText().toString()
                                    .contains(getString(R.string.hia2_draft_monthly))) {
                                tab.setText(String.format(
                                        getString(R.string.hia2_draft_monthly_with_count),
                                        monthlyTallies == null ? 0 : monthlyTallies.size()));
                            }
                        }
                    }
                });
            }
        }), null);
    }

    private static String retrieveValue(List<MonthlyTally> monthlyTallies, Hia2Indicator hia2Indicator) {
        String defaultValue = "0";
        if (hia2Indicator == null || monthlyTallies == null) {
            return defaultValue;
        }

        for (MonthlyTally monthlyTally : monthlyTallies) {
            if (monthlyTally.getIndicator() != null && monthlyTally.getIndicator().getIndicatorCode()
                    .equalsIgnoreCase(hia2Indicator.getIndicatorCode())) {
                return monthlyTally.getValue();
            }
        }

        return defaultValue;
    }


    private void initializeProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(getString(R.string.loading));
        progressDialog.setMessage(getString(R.string.please_wait_message));
    }

    protected void showProgressDialog() {
        if (progressDialog == null) {
            initializeProgressDialog();
        }

        progressDialog.show();
    }

    protected void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }


    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    public static class StartDraftMonthlyFormTask extends AsyncTask<Void, Void, Intent> {
        private final HIA2ReportsActivity baseActivity;
        private final Date date;
        private final String formName;
        private final boolean firstTimeEdit;

        public StartDraftMonthlyFormTask(HIA2ReportsActivity baseActivity,
                                         Date date, String formName, boolean firstTimeEdit) {
            this.baseActivity = baseActivity;
            this.date = date;
            this.formName = formName;
            this.firstTimeEdit = firstTimeEdit;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            baseActivity.showProgressDialog();
        }

        @Override
        protected Intent doInBackground(Void... params) {
            try {
                MonthlyTalliesRepository monthlyTalliesRepository = VaccinatorApplication.getInstance().monthlyTalliesRepository();
                List<MonthlyTally> monthlyTallies = monthlyTalliesRepository.findDrafts(MonthlyTalliesRepository.DF_YYYYMM.format(date));

                HIA2IndicatorsRepository hIA2IndicatorsRepository = VaccinatorApplication.getInstance().hIA2IndicatorsRepository();
                List<Hia2Indicator> hia2Indicators = hIA2IndicatorsRepository.fetchAll();
                if (hia2Indicators == null || hia2Indicators.isEmpty()) {
                    return null;
                }

                JSONObject form = FormUtils.getInstance(baseActivity).getFormJson(formName);
                JSONObject step1 = form.getJSONObject("step1");
                String title = MonthlyTalliesRepository.DF_YYYYMM.format(date).concat(" Draft");
                step1.put(PathConstants.KEY.TITLE, title);

                JSONArray sections = step1.getJSONArray(JsonFormConstants.SECTIONS);

                String indicatorCategory = "";
                // This map holds each category as key and all the fields for that category as the
                // value (jsonarray)
                LinkedHashMap<String, JSONArray> fieldsMap = new LinkedHashMap<>();
                for (Hia2Indicator hia2Indicator : hia2Indicators) {
                    JSONObject jsonObject = new JSONObject();
                    if (hia2Indicator.getLabel() == null) {
                        hia2Indicator.setLabel("");
                    }
                    String label = hia2Indicator.getIndicatorCode() + ": " + hia2Indicator.getLabel() + " *";

                    JSONObject vRequired = new JSONObject();
                    vRequired.put(JsonFormConstants.VALUE, "true");
                    vRequired.put(JsonFormConstants.ERR, "Specify: " + hia2Indicator.getLabel());
                    JSONObject vNumeric = new JSONObject();
                    vNumeric.put(JsonFormConstants.VALUE, "true");
                    vNumeric.put(JsonFormConstants.ERR, "Value should be numeric");

                    jsonObject.put(JsonFormConstants.KEY, hia2Indicator.getId());
                    jsonObject.put(JsonFormConstants.TYPE, "edit_text");
                    jsonObject.put(JsonFormConstants.READ_ONLY, readOnlyList.contains(hia2Indicator.getIndicatorCode()));
                    jsonObject.put(JsonFormConstants.HINT, label);
                    jsonObject.put(JsonFormConstants.VALUE, retrieveValue(monthlyTallies, hia2Indicator));
                    if (DailyTalliesRepository.IGNORED_INDICATOR_CODES
                            .contains(hia2Indicator.getIndicatorCode()) && firstTimeEdit) {
                        jsonObject.put(JsonFormConstants.VALUE, "");
                    }
                    jsonObject.put(JsonFormConstants.V_REQUIRED, vRequired);
                    jsonObject.put(JsonFormConstants.V_NUMERIC, vNumeric);
                    jsonObject.put(JsonFormConstants.OPENMRS_ENTITY_PARENT, "");
                    jsonObject.put(JsonFormConstants.OPENMRS_ENTITY, "");
                    jsonObject.put(JsonFormConstants.OPENMRS_ENTITY_ID, "");
                    jsonObject.put(PathConstants.KEY.HIA_2_INDICATOR, hia2Indicator.getIndicatorCode());
                    indicatorCategory = hia2Indicator.getCategory();
                    JSONArray fields = null;
                    if (fieldsMap.containsKey(indicatorCategory)) {
                        fields = fieldsMap.get(indicatorCategory);
                    } else {
                        fields = new JSONArray();
                    }
                    fields.put(jsonObject);
                    fieldsMap.put(indicatorCategory, fields);
                }

                // Build sections in the form based on categories, each key is a category
                for (String key : fieldsMap.keySet()) {
                    JSONObject section = new JSONObject();
                    section.put(JsonFormConstants.NAME, key);
                    section.put(JsonFormConstants.FIELDS, fieldsMap.get(key));
                    sections.put(section);
                }

                // Add the confirm button
                JSONObject buttonObject = new JSONObject();
                buttonObject.put(JsonFormConstants.KEY, FORM_KEY_CONFIRM);
                buttonObject.put(JsonFormConstants.VALUE, "false");
                buttonObject.put(JsonFormConstants.TYPE, "button");
                buttonObject.put(JsonFormConstants.HINT, "Confirm");
                buttonObject.put(JsonFormConstants.OPENMRS_ENTITY_PARENT, "");
                buttonObject.put(JsonFormConstants.OPENMRS_ENTITY, "");
                buttonObject.put(JsonFormConstants.OPENMRS_ENTITY_ID, "");
                JSONObject action = new JSONObject();
                action.put(JsonFormConstants.BEHAVIOUR, "finish_form");
                buttonObject.put(JsonFormConstants.ACTION, action);

                JSONArray confirmSectionFields = new JSONArray();
                confirmSectionFields.put(buttonObject);
                JSONObject confirmSection = new JSONObject();
                confirmSection.put(JsonFormConstants.FIELDS, confirmSectionFields);
                sections.put(confirmSection);

                form.put(JsonFormConstants.REPORT_MONTH, HIA2Service.dfyymmdd.format(date));
                form.put("identifier", "HIA2ReportForm");

                Intent intent = new Intent(baseActivity, PathJsonFormActivity.class);
                intent.putExtra("json", form.toString());
                intent.putExtra(JsonFormConstants.SKIP_VALIDATION, true);

                return intent;
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Intent intent) {
            super.onPostExecute(intent);
            baseActivity.hideProgressDialog();
            if (intent != null) {
                baseActivity.startActivityForResult(intent, REQUEST_CODE_GET_JSON);
            }
        }
    }

    public static class FetchEditedMonthlyTalliesTask extends AsyncTask<Void, Void, List<MonthlyTally>> {
        private final TaskListener taskListener;

        public FetchEditedMonthlyTalliesTask(TaskListener taskListener) {
            this.taskListener = taskListener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<MonthlyTally> doInBackground(Void... params) {
            MonthlyTalliesRepository monthlyTalliesRepository = VaccinatorApplication.getInstance().monthlyTalliesRepository();
            Calendar endDate = Calendar.getInstance();
            endDate.set(Calendar.DAY_OF_MONTH, 1); // Set date to first day of this month
            endDate.set(Calendar.HOUR_OF_DAY, 23);
            endDate.set(Calendar.MINUTE, 59);
            endDate.set(Calendar.SECOND, 59);
            endDate.set(Calendar.MILLISECOND, 999);
            endDate.add(Calendar.DATE, -1); // Move the date to last day of last month

            return monthlyTalliesRepository.findEditedDraftMonths(null, endDate.getTime());
        }

        @Override
        protected void onPostExecute(List<MonthlyTally> monthlyTallies) {
            super.onPostExecute(monthlyTallies);
            taskListener.onPostExecute(monthlyTallies);
        }

        public interface TaskListener {
            void onPostExecute(List<MonthlyTally> monthlyTallies);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return DailyTalliesFragment.newInstance();
                case 1:
                    return DraftMonthlyFragment.newInstance();
                case 2:
                    return SentMonthlyFragment.newInstance();
                default:
                    break;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.hia2_daily_tallies);
                case 1:
                    return getString(R.string.hia2_draft_monthly);
                case 2:
                    return getString(R.string.hia2_sent_monthly);
                default:
                    break;
            }
            return null;
        }
    }
}
