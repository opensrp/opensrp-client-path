package org.smartregister.path.renderer;

import android.content.Context;
import android.graphics.Canvas;

import java.util.List;

import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.provider.LineChartDataProvider;
import lecho.lib.hellocharts.renderer.LineChartRenderer;
import lecho.lib.hellocharts.view.Chart;

/**
 * Created by keyman on 2/15/18.
 */

public class CustomLineChartRenderer extends LineChartRenderer {

    private LineChartDataProvider dataProvider;

    public CustomLineChartRenderer(Context context, Chart chart, LineChartDataProvider dataProvider) {
        super(context, chart, dataProvider);

        this.dataProvider = dataProvider;
    }

    @Override
    public void drawUnclipped(Canvas canvas) {
        final LineChartData data = dataProvider.getLineChartData();
        for (Line line : data.getLines()) {
            List<PointValue> pointValueList = line.getValues();
            if (pointValueList != null && !pointValueList.isEmpty()) {
                PointValue pointValue = pointValueList.get(0);
                if (pointValue.getX() == 0f && pointValue.getY() == 0f) {
                    pointValueList.remove(pointValue);
                }
            }
        }
        super.drawUnclipped(canvas);
    }
}