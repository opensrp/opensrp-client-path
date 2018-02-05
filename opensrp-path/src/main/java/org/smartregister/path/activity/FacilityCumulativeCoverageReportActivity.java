package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.CoverageHolder;
import org.smartregister.path.domain.CumulativeIndicator;
import org.smartregister.path.domain.NamedObject;
import org.smartregister.path.receiver.CoverageDropoutBroadcastReceiver;
import org.smartregister.path.repository.CumulativeIndicatorRepository;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by keyman on 03/01/17.
 */
public class FacilityCumulativeCoverageReportActivity extends BaseReportActivity {

    public static final String HOLDER = "HOLDER";
    public static final String VACCINE = "VACCINE";
    private static final String START = "start";
    private static final String END = "end";

    private CoverageHolder holder = null;
    private VaccineRepo.Vaccine vaccine = null;


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

        Serializable serializable = getIntent().getSerializableExtra(HOLDER);
        if (serializable != null && serializable instanceof CoverageHolder) {
            holder = (CoverageHolder) serializable;
        }

        vaccine = (VaccineRepo.Vaccine) getIntent().getSerializableExtra(VACCINE);
        String vaccineName = vaccine.display();
        if (vaccine.equals(VaccineRepo.Vaccine.penta1) || vaccine.equals(VaccineRepo.Vaccine.penta3)) {
            vaccineName = VaccineRepo.Vaccine.penta1.display() + " + 3 ";
        } else if (vaccine.equals(VaccineRepo.Vaccine.bcg) || vaccine.equals(VaccineRepo.Vaccine.measles1) || vaccine.equals(VaccineRepo.Vaccine.mr1)) {
            vaccineName = VaccineRepo.Vaccine.bcg.display() + " + " + VaccineRepo.Vaccine.measles1.display() + "/" + VaccineRepo.Vaccine.mr1.display();
        }

        TextView textView = (TextView) findViewById(R.id.report_title);
        textView.setText(String.format(getString(R.string.facility_cumulative_title), BaseReportActivity.getYear(holder.getDate()), vaccineName));

        TextView csoTargetView = (TextView) findViewById(R.id.cso_target_value);
        TextView csoTargetMonthlyView = (TextView) findViewById(R.id.cso_target_monthly_value);

        if (holder.getSize() != null) {
            Long csoTargetMonthly = holder.getSize() / 12;
            csoTargetView.setText(String.valueOf(holder.getSize()));
            csoTargetMonthlyView.setText(String.valueOf(csoTargetMonthly));
        } else {
            csoTargetView.setText("0");
            csoTargetMonthlyView.setText("0");
        }
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

    @Override
    protected String getActionType() {
        return CoverageDropoutBroadcastReceiver.TYPE_GENERATE_CUMULATIVE_INDICATORS;
    }

    @Override
    protected int getParentNav() {
        return R.id.coverage_reports;
    }

    private void refreshMonitoring(List<CumulativeIndicator> startCumulativeIndicators, List<CumulativeIndicator> endCumulativeIndicators) {
        boolean isComparison = endCumulativeIndicators != null;

        long leftPartitions = 15;
        long csoTarget = 0L;
        long csoTargetMonthly = 0L;
        if (holder.getSize() != null) {
            csoTarget = holder.getSize();
            csoTargetMonthly = csoTarget / 12;
        }

        String[] months = new DateFormatSymbols().getShortMonths();
        Map<String, Long> startValueMap = new LinkedHashMap<>();
        Map<String, Long> endValueMap = new LinkedHashMap<>();

        // Axis
        List<AxisValue> bottomAxisValues = new ArrayList<>();
        List<AxisValue> topAxisValues = new ArrayList<>();
        List<AxisValue> leftAxisValues = new ArrayList<>();
        List<AxisValue> rightAxisValues = new ArrayList<>();

        for (int i = 0; i < leftPartitions; i++) {
            float currentMonlthyTarget = csoTargetMonthly * i;
            AxisValue leftValue = new AxisValue(currentMonlthyTarget);
            leftValue.setLabel(String.valueOf((int) currentMonlthyTarget));
            leftAxisValues.add(leftValue);

            if (i < months.length) {
                AxisValue curValue = new AxisValue((float) i);
                curValue.setLabel(months[i].toUpperCase());
                bottomAxisValues.add(curValue);

                topAxisValues.add(new AxisValue((float) i).setLabel(""));
            }

            if (i >= 1 && i <= 5) {
                float value = csoTarget * (0.25f * i);
                AxisValue rightValue = new AxisValue(value);
                rightValue.setLabel(String.format(getString(R.string.coverage_percentage), 25 * i));
                rightAxisValues.add(rightValue);
            }
        }

        // Lines
        List<Line> lines = new ArrayList<>();
        lines.add(generateLine(0.25f, csoTargetMonthly));
        lines.add(generateLine(0.5f, csoTargetMonthly));
        lines.add(generateLine(0.75f, csoTargetMonthly));
        lines.add(generateLine(1f, csoTargetMonthly));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM");
        for (CumulativeIndicator cumulativeIndicator : startCumulativeIndicators) {
            Date month = cumulativeIndicator.getMonthAsDate();
            String monthString = simpleDateFormat.format(month);
            startValueMap.put(monthString, cumulativeIndicator.getValue());
        }

        if (isComparison) {
            for (CumulativeIndicator cumulativeIndicator : endCumulativeIndicators) {
                Date month = cumulativeIndicator.getMonthAsDate();
                String monthString = simpleDateFormat.format(month);
                endValueMap.put(monthString, cumulativeIndicator.getValue());
            }
        }

        List<PointValue> startValues = new ArrayList<>();
        List<PointValue> endValues = new ArrayList<>();

        startValues.add(new PointValue(0f, 0f));
        endValues.add(new PointValue(0f, 0f));

        boolean checkCurrentTime = false;
        Calendar calendar = null;
        Date currentDate = null;

        int year = BaseReportActivity.getYear(holder.getDate());
        int currentYear = BaseReportActivity.getYear(new Date());
        if (year >= currentYear) {
            checkCurrentTime = true;
            calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.DAY_OF_YEAR, 1);

            currentDate = new Date();
        }

        for (int i = 0; i < 12; i++) {
            if (checkCurrentTime) {
                if (currentDate.before(calendar.getTime())) {
                    break;
                }
                calendar.add(Calendar.MONTH, 1);
            }

            float x = 0.5f + i;
            float y = 0L;
            float z = 0L;
            for (int j = i; j >= 0; j--) {
                Long startValue = startValueMap.get(months[j]);
                if (startValue != null) {
                    y += startValue;
                }
                if (isComparison) {
                    Long endValue = endValueMap.get(months[j]);
                    if (endValue != null) {
                        z += endValue;
                    }
                }
            }
            startValues.add(new PointValue(x, y));
            if (isComparison) {
                endValues.add(new PointValue(x, z));
            }

        }

        lines.add(new Line(startValues).
                setColor(getResources().getColor(R.color.cumulative_blue_line)).
                setHasPoints(true).
                setHasLabels(false).
                setShape(ValueShape.CIRCLE).
                setHasLines(true).
                setStrokeWidth(2));

        if (isComparison) {
            lines.add(new Line(endValues).
                    setColor(getResources().getColor(R.color.cumulative_red_line)).
                    setHasPoints(true).
                    setHasLabels(false).
                    setShape(ValueShape.CIRCLE).
                    setHasLines(true).
                    setStrokeWidth(2));
        }

        LineChartData data = new LineChartData();
        data.setLines(lines);

        data.setAxisXBottom(new Axis(bottomAxisValues).setMaxLabelChars(3).setHasLines(false).setHasTiltedLabels(false).setFormatter(new MonthValueFormatter()));
        data.setAxisYLeft(new Axis(leftAxisValues).setHasLines(true).setHasTiltedLabels(false));
        data.setAxisXTop(new Axis(topAxisValues).setHasLines(true).setHasTiltedLabels(false));
        data.setAxisYRight(new Axis(rightAxisValues).setMaxLabelChars(5).setHasLines(false).setHasTiltedLabels(false));

        // Chart
        LineChartView monitoringChart = (LineChartView) findViewById(R.id.monitoring_chart);
        monitoringChart.setLineChartData(data);
        monitoringChart.setViewportCalculationEnabled(false);
        resetViewport(monitoringChart, csoTargetMonthly, leftPartitions);

        updateTableLayout(startValueMap, endValueMap, months, isComparison, checkCurrentTime);
    }

    private void resetViewport(LineChartView chart, long csoTargetMonthly, long leftPartitions) {
        // Reset viewport height range to (0,100)
        Viewport v = chart.getMaximumViewport();
        v.set(v.left, csoTargetMonthly * leftPartitions, v.right, 0);
        chart.setMaximumViewport(v);
        chart.setCurrentViewport(v);
    }

    private Line generateLine(float percentageDecimal, long csoTargetMonthly) {
        List<PointValue> values = new ArrayList<>();
        for (int i = 0; i <= 12; i++) {
            float y = csoTargetMonthly * i * percentageDecimal;
            PointValue pointValue = new PointValue();
            pointValue.set(i, y);
            values.add(pointValue);
        }

        Line line = new Line(values);
        line.setHasLines(true);
        line.setHasPoints(false);
        line.setStrokeWidth(1);
        return line;
    }

    private void updateTableLayout(Map<String, Long> startValueMap, Map<String, Long> endValueMap, String[] months, boolean isComparison, boolean checkCurrentTime) {

        boolean isCheckCurrentTime = checkCurrentTime;

        TableRow titleRow = (TableRow) findViewById(R.id.title_1);
        for (int i = 0; i < titleRow.getChildCount(); i++) {
            if (i > 0) {
                TextView titleTextView = (TextView) titleRow.getChildAt(i);
                titleTextView.setTextColor(getResources().getColor(R.color.silver));
                titleTextView.setText(months[i - 1].toUpperCase());
            }
        }

        TableRow startTotalValueRow = (TableRow) findViewById(R.id.total_1);
        TableRow startCumValueRow = (TableRow) findViewById(R.id.cum_1);

        TableRow endTotalValueRow = (TableRow) findViewById(R.id.total_2);
        TableRow endCumValueRow = (TableRow) findViewById(R.id.cum_2);

        TableRow dropoutValueRow = (TableRow) findViewById(R.id.dropout_1);
        TableRow dropoutPercentageValueRow = (TableRow) findViewById(R.id.drop_percent_1);

        TableRow cumDropoutValueRow = (TableRow) findViewById(R.id.cum_dropout_1);
        TableRow cumDropoutPercentageValueRow = (TableRow) findViewById(R.id.cum_drop_percent_1);

        if (isComparison) {
            endTotalValueRow.setVisibility(View.VISIBLE);
            endCumValueRow.setVisibility(View.VISIBLE);

            dropoutValueRow.setVisibility(View.VISIBLE);
            dropoutPercentageValueRow.setVisibility(View.VISIBLE);

            cumDropoutValueRow.setVisibility(View.VISIBLE);
            cumDropoutPercentageValueRow.setVisibility(View.VISIBLE);
        }


        Calendar calendar = null;
        Date currentDate = null;

        if (isCheckCurrentTime) {
            isCheckCurrentTime = true;
            int year = BaseReportActivity.getYear(holder.getDate());
            calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.DAY_OF_YEAR, 1);

            currentDate = new Date();
        }

        for (int i = 0; i < startTotalValueRow.getChildCount(); i++) {

            if (i > 0 && isCheckCurrentTime) {
                if (currentDate.before(calendar.getTime())) {
                    break;
                }
                calendar.add(Calendar.MONTH, 1);
            }

            TextView startTotalTextView = (TextView) startTotalValueRow.getChildAt(i);
            TextView startCumTextView = (TextView) startCumValueRow.getChildAt(i);

            TextView endTotalTextView = (TextView) endTotalValueRow.getChildAt(i);
            TextView endCumTextView = (TextView) endCumValueRow.getChildAt(i);

            TextView dropoutTextView = (TextView) dropoutValueRow.getChildAt(i);
            TextView dropoutPercentageTextView = (TextView) dropoutPercentageValueRow.getChildAt(i);

            TextView cumDropoutTextView = (TextView) cumDropoutValueRow.getChildAt(i);
            TextView cumDropoutPercentageTextView = (TextView) cumDropoutPercentageValueRow.getChildAt(i);


            startTotalTextView.setTextColor(getResources().getColor(R.color.cumulative_blue_line));
            startCumTextView.setTextColor(getResources().getColor(R.color.cumulative_blue_line));

            if (isComparison) {
                endTotalTextView.setTextColor(getResources().getColor(R.color.cumulative_red_line));
                endCumTextView.setTextColor(getResources().getColor(R.color.cumulative_red_line));

                dropoutTextView.setTextColor(getResources().getColor(R.color.client_list_grey));
                dropoutPercentageTextView.setTextColor(getResources().getColor(R.color.client_list_grey));

                cumDropoutTextView.setTextColor(getResources().getColor(R.color.client_list_grey));
                cumDropoutPercentageTextView.setTextColor(getResources().getColor(R.color.client_list_grey));
            }

            if (i == 0) {
                if (isComparison) {
                    VaccineRepo.Vaccine startVaccine = vaccine;
                    VaccineRepo.Vaccine endVaccine = vaccine;
                    if (vaccine.equals(VaccineRepo.Vaccine.penta1) || vaccine.equals(VaccineRepo.Vaccine.penta3)) {
                        startVaccine = VaccineRepo.Vaccine.penta1;
                        endVaccine = VaccineRepo.Vaccine.penta3;
                    }
                    if (vaccine.equals(VaccineRepo.Vaccine.bcg) || vaccine.equals(VaccineRepo.Vaccine.measles1)) {
                        startVaccine = VaccineRepo.Vaccine.bcg;
                        endVaccine = VaccineRepo.Vaccine.measles1;
                    }

                    startTotalTextView.setText(String.format(getString(R.string.total_vaccine), StringUtils.capitalize(startVaccine.display().toLowerCase())));
                    startCumTextView.setText(String.format(getString(R.string.total_cum), StringUtils.capitalize(startVaccine.display().toLowerCase())));

                    endTotalTextView.setText(String.format(getString(R.string.total_vaccine), StringUtils.capitalize(endVaccine.display().toLowerCase())));
                    endCumTextView.setText(String.format(getString(R.string.total_cum), StringUtils.capitalize(endVaccine.display().toLowerCase())));

                    dropoutTextView.setText(getString(R.string.total_dropout));
                    if (startVaccine.equals(VaccineRepo.Vaccine.penta1)) {
                        dropoutTextView.setText(getString(R.string.total_dropout_penta));
                    }
                    dropoutPercentageTextView.setText(getString(R.string.total_dropout_percentage));

                    dropoutTextView.setTextColor(getResources().getColor(R.color.text_black));
                    dropoutPercentageTextView.setTextColor(getResources().getColor(R.color.text_black));

                    cumDropoutTextView.setText(getString(R.string.total_cum_dropout));
                    cumDropoutPercentageTextView.setText(getString(R.string.total_cum_dropout_percentage));

                    cumDropoutTextView.setTextColor(getResources().getColor(R.color.text_black));
                    cumDropoutPercentageTextView.setTextColor(getResources().getColor(R.color.text_black));
                } else {
                    startTotalTextView.setText(String.format(getString(R.string.total_vaccine), StringUtils.capitalize(vaccine.display().toLowerCase())));
                    startCumTextView.setText(String.format(getString(R.string.total_cum), StringUtils.capitalize(vaccine.display().toLowerCase())));
                }
            } else {

                // Total
                Long startValue = startValueMap.get(months[i - 1]);
                if (startValue == null) {
                    startValue = 0L;
                }
                startTotalTextView.setText(String.valueOf(startValue));

                Long endValue = 0L;
                if (isComparison) {
                    endValue = endValueMap.get(months[i - 1]);
                    if (endValue == null) {
                        endValue = 0L;
                    }
                    endTotalTextView.setText(String.valueOf(endValue));
                }

                // Cumulative
                Long cumStartValue = 0L;
                Long cumEndValue = 0L;
                for (int j = i; j >= 1; j--) {
                    Long currentStartValue = startValueMap.get(months[j - 1]);
                    if (currentStartValue != null) {
                        cumStartValue += currentStartValue;
                    }

                    if (isComparison) {
                        Long currentEndValue = endValueMap.get(months[j - 1]);
                        if (currentEndValue != null) {
                            cumEndValue += currentEndValue;
                        }

                    }
                }
                startCumTextView.setText(String.valueOf(cumStartValue));

                if (isComparison) {
                    endCumTextView.setText(String.valueOf(cumEndValue));

                    Long dropoutValue = startValue - endValue;
                    Long cumDropoutValue = cumStartValue - cumEndValue;

                    dropoutTextView.setText(String.valueOf(dropoutValue));
                    cumDropoutTextView.setText(String.valueOf(cumDropoutValue));

                    int dropoutPercentage = 0;
                    if (dropoutValue > 0 && startValue > 0) {
                        dropoutPercentage = (int) (dropoutValue * 100.0 / startValue + 0.5);
                    }

                    dropoutPercentageTextView.setText(String.format(getString(R.string.coverage_percentage), dropoutPercentage));

                    int cumDropoutPercentage = 0;
                    if (cumDropoutValue > 0 && cumStartValue > 0) {
                        cumDropoutPercentage = (int) (cumDropoutValue * 100.0 / cumStartValue + 0.5);
                    }

                    cumDropoutPercentageTextView.setText(String.format(getString(R.string.coverage_percentage), cumDropoutPercentage));
                }
            }
        }
    }

    @Override
    protected Map<String, NamedObject<?>> generateReportBackground() {
        VaccineRepo.Vaccine startVaccine = vaccine;
        VaccineRepo.Vaccine endVaccine = null;
        if (vaccine.equals(VaccineRepo.Vaccine.penta1) || vaccine.equals(VaccineRepo.Vaccine.penta3)) {
            startVaccine = VaccineRepo.Vaccine.penta1;
            endVaccine = VaccineRepo.Vaccine.penta3;
        } else if (vaccine.equals(VaccineRepo.Vaccine.bcg) || vaccine.equals(VaccineRepo.Vaccine.measles1)) {
            startVaccine = VaccineRepo.Vaccine.bcg;
            endVaccine = VaccineRepo.Vaccine.measles1;
        }

        String orderBy = CumulativeIndicatorRepository.COLUMN_MONTH + " ASC ";

        CumulativeIndicatorRepository cumulativeIndicatorRepository = VaccinatorApplication.getInstance().cumulativeIndicatorRepository();
        List<CumulativeIndicator> startCumulativeIndicators = cumulativeIndicatorRepository.findByVaccineAndCumulativeId(generateVaccineName(startVaccine), holder.getId(), orderBy);
        List<CumulativeIndicator> endCumulativeIndicators = null;
        if (endVaccine != null) {
            endCumulativeIndicators = cumulativeIndicatorRepository.findByVaccineAndCumulativeId(generateVaccineName(endVaccine), holder.getId(), orderBy);
        }

        Map<String, NamedObject<?>> map = new HashMap<>();
        NamedObject<List<CumulativeIndicator>> startedNamedObject = new NamedObject<>(START, startCumulativeIndicators);
        map.put(startedNamedObject.name, startedNamedObject);

        NamedObject<List<CumulativeIndicator>> endNamedObject = new NamedObject<>(END, endCumulativeIndicators);
        map.put(endNamedObject.name, endNamedObject);

        return map;

    }

    @Override
    protected void generateReportUI(Map<String, NamedObject<?>> map, boolean userAction) {
        List<CumulativeIndicator> startCumulativeIndicators = new ArrayList<>();
        List<CumulativeIndicator> endCumulativeIndicators = null;

        if (map.containsKey(START)) {
            NamedObject<?> namedObject = map.get(START);
            if (namedObject != null) {
                startCumulativeIndicators = (List<CumulativeIndicator>) namedObject.object;
            }
        }

        if (map.containsKey(END)) {
            NamedObject<?> namedObject = map.get(END);
            if (namedObject != null) {
                endCumulativeIndicators = (List<CumulativeIndicator>) namedObject.object;
            }
        }

        refreshMonitoring(startCumulativeIndicators, endCumulativeIndicators);
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
