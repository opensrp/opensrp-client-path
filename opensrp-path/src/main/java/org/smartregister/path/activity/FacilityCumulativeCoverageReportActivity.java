package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.smartregister.domain.FetchStatus;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.path.R;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import util.Utils;

/**
 * Created by keyman on 03/01/17.
 */
public class FacilityCumulativeCoverageReportActivity extends BaseActivity {

    public static final String YEAR = "YEAR";
    public static final String VACCINE = "VACCINE";

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
                Intent intent = new Intent(FacilityCumulativeCoverageReportActivity.this, CoverageReportsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ((TextView) toolbar.findViewById(R.id.title)).setText(getString(R.string.facility_cumulative_coverage_report));


        int year = getIntent().getIntExtra(YEAR, Utils.yearFromDate(new Date()));
        VaccineRepo.Vaccine vaccine = (VaccineRepo.Vaccine) getIntent().getSerializableExtra(VACCINE);
        String vaccineName = vaccine.display();
        if (vaccine.equals(VaccineRepo.Vaccine.penta1) || vaccine.equals(VaccineRepo.Vaccine.penta3)) {
            vaccineName += " + 3 ";
        } else if (vaccine.equals(VaccineRepo.Vaccine.bcg) || vaccine.equals(VaccineRepo.Vaccine.measles1) || vaccine.equals(VaccineRepo.Vaccine.mr1)) {
            vaccineName += " + " + VaccineRepo.Vaccine.measles1.display() + "/" + VaccineRepo.Vaccine.mr1.display();
        }

        TextView textView = (TextView) findViewById(R.id.report_title);
        textView.setText(String.format(getString(R.string.facility_cumulative_title), year, vaccineName));

        refreshMonitoring();
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


    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        super.onSyncComplete(fetchStatus);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_facility_cumulative_coverage_report;
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


    private void refreshMonitoring() {

        String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        List<AxisValue> bottomAxisValues = new ArrayList<>();
        List<AxisValue> topAxisValues = new ArrayList<>();
        for (int i = 0; i < months.length; i++) {
            AxisValue curValue = new AxisValue((float) i);
            curValue.setLabel(months[i]);
            bottomAxisValues.add(curValue);

            topAxisValues.add(new AxisValue((float) i).setLabel(""));
        }

        List<Line> lines = new ArrayList<>();

        List<PointValue> values = new ArrayList<>();
        values.add(new PointValue((float) 0.5, (float) 17));
        values.add(new PointValue((float) 1.5, (float) 39));
        values.add(new PointValue((float) 2.5, (float) 51));

        Line blueLine = new Line(values);
        blueLine.setColor(getResources().getColor(R.color.cumulative_blue_line));
        blueLine.setHasPoints(true);
        blueLine.setHasLabels(false);
        blueLine.setShape(ValueShape.CIRCLE);
        blueLine.setHasLines(true);
        lines.add(blueLine);

        values = new ArrayList<>();
        values.add(new PointValue((float) 0.5, (float) 24));
        values.add(new PointValue((float) 1.5, (float) 58));
        values.add(new PointValue((float) 2.5, (float) 70));

        Line redline = new Line(values);
        redline.setColor(getResources().getColor(R.color.cumulative_red_line));
        redline.setHasPoints(true);
        redline.setHasLabels(false);
        redline.setShape(ValueShape.CIRCLE);
        redline.setHasLines(true);
        lines.add(redline);

        LineChartData data = new LineChartData();
        data.setLines(lines);

        List<AxisValue> topAxis = new ArrayList<>();

        data.setAxisXBottom(new Axis(bottomAxisValues).setMaxLabelChars(3).setHasLines(false).setHasTiltedLabels(false).setFormatter(new MonthValueFormatter()));
        data.setAxisYLeft(new Axis().setHasLines(true).setHasTiltedLabels(false).setAutoGenerated(true));
        data.setAxisXTop(new Axis(topAxisValues).setHasLines(true).setHasTiltedLabels(false));
        data.setAxisYRight(new Axis().setHasTiltedLabels(false).setAutoGenerated(false));

        LineChartView monitoringChart = (LineChartView) findViewById(R.id.monitoring_chart);
        monitoringChart.setLineChartData(data);
        monitoringChart.setViewportCalculationEnabled(false);
        resetViewport(monitoringChart);
    }

    private void resetViewport(LineChartView chart) {
        // Reset viewport height range to (0,100)
        final Viewport v = new Viewport(chart.getMaximumViewport());
        v.bottom = 0;
        v.top = 260;
        v.left = 0;
        v.right = 12;
        chart.setMaximumViewport(v);
        chart.setCurrentViewport(v);
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private static class MonthValueFormatter extends SimpleAxisValueFormatter {

        @Override
        public int formatValueForManualAxis(char[] formattedValue, AxisValue axisValue) {
            axisValue.setValue(axisValue.getValue() + 0.5f);
            return super.formatValueForManualAxis(formattedValue, axisValue);
        }

    }
}
