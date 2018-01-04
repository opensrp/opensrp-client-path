package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.domain.FetchStatus;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.path.R;
import org.smartregister.path.adapter.SpinnerAdapter;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.fragment.SetCsoDialogFragment;
import org.smartregister.path.helper.SpinnerHelper;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

import util.PathConstants;
import util.Utils;

/**
 * Created by keyman on 21/12/17.
 */
public class AnnualCoverageReportCsoActivity extends BaseActivity implements SetCsoDialogFragment.OnSetCsoListener {

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

        updateReportList(new Date());

        List<Date> dates = new ArrayList<>();

        Calendar c = Calendar.getInstance();
        dates.add(c.getTime());

        for (int i = 0; i < 5; i++) {
            c.add(Calendar.YEAR, -1);
            dates.add(c.getTime());
        }

        updateReportDates(dates);
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

    private void updateListViewHeader() {
        // Add header
        ListView listView = (ListView) findViewById(R.id.list_view);
        View view = getLayoutInflater().inflate(R.layout.coverage_report_header, null);
        listView.addHeaderView(view);
    }

    private void updateCsoUnder1Population(final int year, final Long value) {
        EditText csoValue = (EditText) findViewById(R.id.cso_value);
        csoValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetCsoDialogFragment.launchDialog(AnnualCoverageReportCsoActivity.this, BaseRegisterActivity.DIALOG_TAG, year, value);
            }
        });
        if (value == null) {
            csoValue.setText(getString(R.string.not_defined));
            csoValue.setTextColor(getResources().getColor(R.color.cso_error_red));
        } else {
            csoValue.setText(String.format(getString(R.string.cso_population_value), value));
            csoValue.setTextColor(getResources().getColor(R.color.text_black));
        }
    }

    private void updateReportList(Date date) {
        final Long csoValue = getCsoPopulation(date);
        final int year = Utils.yearFromDate(date);
        if (csoValue == null) {
            SetCsoDialogFragment.launchDialog(this, BaseRegisterActivity.DIALOG_TAG, year, null);
        }

        updateCsoUnder1Population(year, csoValue);

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

                final VaccineRepo.Vaccine vaccine = vaccineList.get(position);
                String display = vaccine.display();
                if (vaccine.equals(VaccineRepo.Vaccine.measles1)) {
                    display = VaccineRepo.Vaccine.measles1.display() + " / " + VaccineRepo.Vaccine.mr1.display();
                }

                if (vaccine.equals(VaccineRepo.Vaccine.measles2)) {
                    display = VaccineRepo.Vaccine.measles2.display() + " / " + VaccineRepo.Vaccine.mr2.display();
                }

                TextView vaccineTextView = (TextView) view.findViewById(R.id.vaccine);
                vaccineTextView.setText(display);

                Random r = new Random();
                int Low = 0;
                int High = 100;
                int result = r.nextInt(High - Low) + Low;

                TextView vaccinatedTextView = (TextView) view.findViewById(R.id.vaccinated);
                vaccinatedTextView.setText(String.valueOf(result));

                TextView coverageTextView = (TextView) view.findViewById(R.id.coverage);
                if (csoValue == null) {
                    coverageTextView.setText(getString(R.string.no_cso_target));
                    coverageTextView.setTextColor(getResources().getColor(R.color.cso_error_red));
                } else {
                    coverageTextView.setText(String.format(getString(R.string.coverage_percentage),
                            result));
                    coverageTextView.setTextColor(getResources().getColor(R.color.text_black));
                }

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AnnualCoverageReportCsoActivity.this, FacilityCumulativeCoverageReportActivity.class);
                        intent.putExtra(FacilityCumulativeCoverageReportActivity.YEAR, year);
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

    private void updateReportDates(List<Date> dates) {
        if (dates != null && !dates.isEmpty()) {
            View reportDateSpinnerView = findViewById(R.id.cohort_spinner);
            if (reportDateSpinnerView != null) {
                SpinnerHelper reportDateSpinner = new SpinnerHelper(reportDateSpinnerView);
                SpinnerAdapter dataAdapter = new SpinnerAdapter(this, R.layout.item_spinner, dates, new SimpleDateFormat("yyyy"));
                dataAdapter.setFirstSuffix(getString(R.string.in_progress));
                dataAdapter.setDropDownViewResource(R.layout.item_spinner_drop_down);
                reportDateSpinner.setAdapter(dataAdapter);

                reportDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Object tag = view.getTag();
                        if (tag != null && tag instanceof Date) {
                            updateReportList((Date) tag);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
        }
    }

    private Long getCsoPopulation(Date date) {
        int year = Utils.yearFromDate(date);
        String prefKey = PathConstants.CSO_UNDER_1_POPULATION + "_" + year;
        String csoUnder1Population = VaccinatorApplication.getInstance().context().allSharedPreferences().getPreference(prefKey);
        if (StringUtils.isBlank(csoUnder1Population) || !StringUtils.isNumeric(csoUnder1Population)) {
            return null;
        } else {
            return Long.valueOf(csoUnder1Population);
        }
    }

    @Override
    public void updateCsoTargetView(int year, Long csoValue) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        updateReportList(calendar.getTime());
    }
}
