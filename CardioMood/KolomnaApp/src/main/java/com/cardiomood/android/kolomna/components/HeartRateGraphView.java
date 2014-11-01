package com.cardiomood.android.kolomna.components;

import android.content.Context;

import com.cardiomood.android.kolomna.R;
import com.cardiomood.android.tools.CommonTools;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.LineGraphView;

/**
 * Created by antondanhsin on 24/10/14.
 */
public class HeartRateGraphView extends LineGraphView {

    public HeartRateGraphView(Context context, CharSequence title) {
        super(context, title == null ? null : title.toString());
        getGraphViewStyle().setTextSize(16);
        getGraphViewStyle().setNumVerticalLabels(10);
        getGraphViewStyle().setNumHorizontalLabels(7);

        setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    if (value < 0)
                        return "-";
                    if ((long) value == 0L)
                        return "0";
                    else return CommonTools.timeToHumanString(Math.round(value));
                } else {
                    return String.valueOf(Math.round(value));
                }
            }
        });

        setScrollable(true);
        setScalable(true);
        setViewPort(0, 60000);
    }

    public HeartRateGraphView(Context context) {
        this(context, context.getText(R.string.heart_rate_graph_title));
    }

    @Override
    protected double getMaxY() {
        double superMaxY = super.getMaxY();
        double superMinY = super.getMinY();

        if (superMaxY > 250) {
            return 250;
        } else if (superMaxY < 0) {
            return 0;
        }

        double h = superMaxY - superMinY;
        if (h < 20) {
            return superMaxY + 10;
        }
        double maxY = superMaxY + h*0.05;
        if (maxY < 75)
            return 75;
        return maxY;
    }


    @Override
    protected double getMinY() {
        double superMaxY = super.getMaxY();
        double superMinY = super.getMinY();

        if (superMinY > 250) {
            return 250;
        } else if (superMinY < 0) {
            return 0;
        }

        double h = superMaxY - superMinY;
        if (h < 20) {
            return superMinY - 10;
        }
        double minY = superMinY - h*0.05;
        if (minY > 120)
            return 120;
        return minY;
    }
}
