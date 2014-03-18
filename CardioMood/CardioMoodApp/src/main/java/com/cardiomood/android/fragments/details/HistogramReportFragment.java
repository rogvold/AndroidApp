package com.cardiomood.android.fragments.details;

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.math.HeartRateMath;
import com.cardiomood.math.histogram.Histogram;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.CategoryAxis;
import com.shinobicontrols.charts.ColumnSeries;
import com.shinobicontrols.charts.DataAdapter;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;


public class HistogramReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = HistogramReportFragment.class.getSimpleName();

    private static final double[] GOOD_RR = new double[] {
            695,710,710,703,710,679,695,664,687,656,671,679,710,703,726,718,710,718,734,695,710,726,718,710,703,710,703,726,703,710,703,726,695,718,710,718,726,710,710,718,695,695,703,695,718,726,734,718,750,734,734,710,718,726,710,718,703,710,687,687,679,679,679,671,664,640,648,632,640,656,656,664,703,703,726,757,757,750,726,734,687,695,679,679,664,679,695,695,679,671,656,648,664,671,679,718,710,742,726,710,679,664,664,640,617,617,617,625,625,664,687,695,695,718,718,718,718,742,726,710,679,687,687,726,718,710,703,710,695,679,671,671,656,632,640,617,632,656,687,718,757,773,789,765,757,742,757,734,742,742,710,726,687,671,640,632,640,664,664,656,671,679,656,656,625,640,625,609,609,601,609,585,585,562,578,570,539,554,539,562,554,546,554,554,585,585,617,617,640,656,671,656,664,664,640,648,671,664,648,656,625,632,617,617,632,617,625,601,609,593,593,570,593,570,578,585,585,609,648,664,671,679,679,687,664,679,656,671,664,679,656,656,640,640,609,617,601,593,570,593,609,625,679,687,718,726,742,734,726,726,718,726,679,703,718,710,710,687,664,664,648,648,648,656,671,687,687,671,664,679,664,671,671,687,671,656,656,648,648,632,632,609,640,648,648,640,679,687,710,695,718,734,734,710,718,718,718,703,664,664,648,671,664,710,734,750,742,734,710,710,679,679,648,656,625,625,617,632,625,617,625,601,593,593,593,601,632,664,687,703,703,695,710,695,687,656,664,648,656,648,664,640,656,640,640,632,617,625,617,640,640,648,656,851,828,781,757,710,710,679,687,664,695,671,671,664,687,656,656,648,640,632,632,609,625,601,601,601,601,617    };


    private HeartRateDataItemDAO hrDAO = new HeartRateDataItemDAO();

    public HistogramReportFragment() {
        // Required empty public constructor
    }

    @Override
    protected Axis getXAxis() {
        return new CategoryAxis();
    }

    @Override
    protected Axis getYAxis() {
        return new NumberAxis();
    }

    @Override
    protected HeartRateMath collectDataInBackground(HeartRateSession session) {
        List<HeartRateDataItem> items = hrDAO.getItemsBySessionId(session.getId());

        double[] rr = new double[items.size()];
        for (int i=0; i<items.size(); i++) {
            rr[i] = items.get(i).getRrTime();
        }
        return new HeartRateMath(rr);
    }

    @Override
    protected void displayData(HeartRateMath hrm) {
        ShinobiChart chart = getChart();
        chart.setTitle("Histogram");
        Axis xAxis = chart.getXAxis();
        Axis yAxis = chart.getYAxis();
        // prepare source data
        double rr[] = hrm.getRrIntervals();

        // Histogram Chart
        xAxis.getStyle().setInterSeriesSetPadding(2.0f);
        xAxis.enableGesturePanning(true);
        xAxis.enableGestureZooming(true);
        xAxis.setMajorTickFrequency(100.0);
        xAxis.getStyle().getTickStyle().setMinorTicksShown(false);
        xAxis.getStyle().getTickStyle().setMajorTicksShown(true);
        xAxis.getStyle().getTickStyle().setLabelTextSize(10);

        yAxis.getStyle().getTickStyle().setLabelTextSize(10);

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s: series)
            chart.removeSeries(s);


        chart.addSeries(getSeriesForIntervals(rr));
        chart.redrawChart();
    }

    private ColumnSeries getSeriesForIntervals(double rr[]) {
        DataAdapter<Integer, Integer> dataAdapter2 = new SimpleDataAdapter<Integer, Integer>();
        double maxRR = StatUtils.max(rr);
        double minRR = StatUtils.min(rr);
        Histogram histogram = new Histogram(rr, 50);
        if (minRR < 100)
            minRR = 100;
        for (double x=Math.floor((minRR-100)/50)*50; x<=Math.ceil((maxRR+50)/50)*50; x+=50) {
            if (x <= maxRR)
                dataAdapter2.add(new DataPoint<Integer, Integer>((int) x, histogram.getCountFor(x)));
            else
                dataAdapter2.add(new DataPoint<Integer, Integer>((int) x, 0));
        }

        ColumnSeries series2 = new ColumnSeries();
        series2.setDataAdapter(dataAdapter2);
        return series2;
    }

    @Override
    protected int getBottomCustomLayoutId() {
        return R.layout.fragment_histogram_report_bottom;
    }
}
