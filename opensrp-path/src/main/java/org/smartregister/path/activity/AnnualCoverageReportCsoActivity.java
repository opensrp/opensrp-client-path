package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.CoverageHolder;
import org.smartregister.path.domain.Cumulative;
import org.smartregister.path.domain.CumulativeIndicator;
import org.smartregister.path.domain.NamedObject;
import org.smartregister.path.fragment.SetCsoDialogFragment;
import org.smartregister.path.receiver.CoverageDropoutBroadcastReceiver;
import org.smartregister.path.repository.CumulativeIndicatorRepository;
import org.smartregister.path.repository.CumulativeRepository;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Utils;

/**
 * Created by keyman on 21/12/17.
 */
public class AnnualCoverageReportCsoActivity extends BaseReportActivity implements SetCsoDialogFragment.OnSetCsoListener {

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

        updateListViewHeader(R.layout.coverage_report_header);
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


    private void updateCsoUnder1Population(boolean userAction) {
        if (userAction && getHolder() != null && getHolder().getSize() == null) {
            SetCsoDialogFragment.launchDialog(this, BaseReportActivity.DIALOG_TAG, getHolder());
        }

        EditText csoValue = (EditText) findViewById(R.id.cso_value);
        csoValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                SetCsoDialogFragment.launchDialog(AnnualCoverageReportCsoActivity.this, BaseReportActivity.DIALOG_TAG, getHolder());
                v.setEnabled(true);
            }
        });
        if (getHolder().getSize() == null) {
            csoValue.setText(getString(R.string.not_defined));
            csoValue.setTextColor(getResources().getColor(R.color.cso_error_red));
        } else {
            csoValue.setText(String.format(getString(R.string.cso_population_value), getHolder().getSize()));
            csoValue.setTextColor(getResources().getColor(R.color.text_black));
        }
    }

    @Override
    public void updateCsoTargetView(CoverageHolder holder, Long newCsoValue) {
        if (holder != null && getHolder().getId() != null) {

            CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
            cumulativeRepository.changeCsoNumber(newCsoValue, getHolder().getId());

            refresh(true);
        }
    }

    @Override
    protected String getActionType() {
        return CoverageDropoutBroadcastReceiver.TYPE_GENERATE_CUMULATIVE_INDICATORS;
    }

    @Override
    protected int getParentNav() {
        return R.id.coverage_reports;
    }

    ////////////////////////////////////////////////////////////////
    // Reporting Methods
    ////////////////////////////////////////////////////////////////

    @Override
    protected <T> View generateView(final View view, final VaccineRepo.Vaccine vaccine, final List<T> indicators) {
        long value = retrieveCumulativeIndicatorValue(indicators, vaccine);

        TextView vaccineTextView = (TextView) view.findViewById(R.id.vaccine);

        TextView vaccinatedTextView = (TextView) view.findViewById(R.id.vaccinated);
        vaccinatedTextView.setText(String.valueOf(value));
        vaccineTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_open_in_new, 0, 0, 0);
        if (vaccine.equals(VaccineRepo.Vaccine.bcg) || vaccine.equals(VaccineRepo.Vaccine.penta1) || vaccine.equals(VaccineRepo.Vaccine.penta3) || vaccine.equals(VaccineRepo.Vaccine.measles1)) {
            vaccineTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pageview, 0, 0, 0);
        }

        float drawablePadding = getResources().getDimension(R.dimen.register_drawable_padding);
        int paddingInt = Float.valueOf(drawablePadding).intValue();
        vaccineTextView.setCompoundDrawablePadding(paddingInt);


        TextView coverageTextView = (TextView) view.findViewById(R.id.coverage);
        if (getHolder().getSize() == null) {
            coverageTextView.setText(getString(R.string.no_cso_target));
            coverageTextView.setTextColor(getResources().getColor(R.color.cso_error_red));
        } else {
            int percentage = 0;
            if (value > 0 && getHolder().getSize() > 0) {
                percentage = (int) (value * 100.0 / getHolder().getSize() + 0.5);
            }
            coverageTextView.setText(String.format(getString(R.string.coverage_percentage),
                    percentage));

            if (Utils.isSameYear(getHolder().getDate(), new Date())) {
                vaccinatedTextView.setTextColor(getResources().getColor(R.color.text_black));
                coverageTextView.setTextColor(getResources().getColor(R.color.text_black));
            } else {
                vaccinatedTextView.setTextColor(getResources().getColor(R.color.bluetext));
                coverageTextView.setTextColor(getResources().getColor(R.color.bluetext));
            }
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getHolder().getSize() == null || getHolder().getSize() == 0L) {
                    Toast.makeText(AnnualCoverageReportCsoActivity.this, getString(R.string.pls_set_cso_target), Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(AnnualCoverageReportCsoActivity.this, FacilityCumulativeCoverageReportActivity.class);
                    intent.putExtra(FacilityCumulativeCoverageReportActivity.HOLDER, getHolder());
                    intent.putExtra(FacilityCumulativeCoverageReportActivity.VACCINE, vaccine);
                    startActivity(intent);
                }
            }
        });

        return view;
    }

    @Override
    protected Map<String, NamedObject<?>> generateReportBackground() {

        CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
        CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();

        if (cumulativeRepository == null || cumulativeIndicatorRepository == null) {
            return null;
        }

        List<Cumulative> cumulatives = cumulativeRepository.fetchAllWithIndicators();
        if (cumulatives.isEmpty()) {
            return null;
        }

        // Populate the default cumulative
        Cumulative cumulative = cumulatives.get(0);
        CoverageHolder coverageHolder = new CoverageHolder(cumulative.getId(), cumulative.getYearAsDate(), cumulative.getCsoNumber());

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

        updateCsoUnder1Population(userAction);
        updateReportDates(cumulatives, CumulativeRepository.DF_YYYY, getString(R.string.in_progress));
        updateReportList(indicatorList);
    }

    @Override
    protected Pair<List, Long> updateReportBackground(Long id) {

        CumulativeRepository cumulativeRepository = VaccinatorApplication.getInstance().cumulativeRepository();
        CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();

        if (cumulativeRepository == null || cumulativeIndicatorRepository == null) {
            return null;
        }

        Cumulative cumulative = cumulativeRepository.findById(id);
        if (cumulative == null) {
            return null;
        }

        List indicators = cumulativeIndicatorRepository.findByCumulativeId(id);

        return Pair.create(indicators, cumulative.getCsoNumber());
    }

    @Override
    protected void updateReportUI(Pair<List, Long> pair, boolean userAction) {
        setHolderSize(pair.second);
        updateCsoUnder1Population(userAction);
        updateReportList(pair.first);
    }
}
