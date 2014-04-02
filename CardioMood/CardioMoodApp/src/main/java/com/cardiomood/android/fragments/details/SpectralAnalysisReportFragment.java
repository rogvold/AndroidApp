package com.cardiomood.android.fragments.details;

import android.os.Bundle;

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.math.HeartRateMath;
import com.cardiomood.math.spectrum.SpectralAnalysis;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.NumberRange;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.List;


public class SpectralAnalysisReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = SpectralAnalysisReportFragment.class.getSimpleName();

    private HeartRateDataItemDAO hrDAO;

    public SpectralAnalysisReportFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hrDAO = new HeartRateDataItemDAO();
    }

    @Override
    protected Axis getXAxis() {
        return new NumberAxis();
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
        chart.setTitle("Spectral Power");
        Axis xAxis = chart.getXAxis();
        xAxis.setTitle("Frequency, Hz");
        xAxis.getStyle().getTickStyle().setLabelTextSize(10);
        xAxis.getStyle().getTitleStyle().setTextSize(12);

        Axis yAxis = chart.getYAxis();
        yAxis.setTitle("Power, ms2/Hz");
        yAxis.getStyle().getTickStyle().setLabelTextSize(10);
        yAxis.getStyle().getTitleStyle().setTextSize(12);

        // prepare source data
        double rr[] = hrm.getRrIntervals();
        double time[] = hrm.getTime();
        double bpm[] = new double[rr.length];
        for (int i=0; i<rr.length; i++)
            bpm[i] = 1000*60/rr[i];

        // Spectrum Chart
        SpectralAnalysis sa = new SpectralAnalysis(time, rr);
        double[] power = sa.getPower();
        PolynomialSplineFunction spectrum = sa.getSplinePower();


        xAxis.enableGesturePanning(true);
        xAxis.enableGestureZooming(true);
        xAxis.setDefaultRange(new NumberRange(0.0, 0.45));

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s: series)
            chart.removeSeries(s);

        SimpleDataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
        double maxFreq = sa.toFrequency(power.length-1), freq = 0;
        while(freq < maxFreq) {
            dataAdapter2.add(new DataPoint<Double, Double>(freq, spectrum.value(freq)));
            freq += 0.0005;
        }

        LineSeries series2 = new LineSeries();
        series2.setDataAdapter(dataAdapter2);
        chart.addSeries(series2);
        chart.redrawChart();
    }

    @Override
    protected int getBottomCustomLayoutId() {
        return R.layout.fragment_spectral_analysis_report_bottom;
    }
}
