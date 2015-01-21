package com.cardiomood.framework.fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cardiomood.framework.R;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.ChartView;
import com.shinobicontrols.charts.ShinobiChart;

import java.util.ArrayList;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by danon on 11.03.14.
 */
public abstract class SingleChartFragmentBase extends Fragment {

    private static final String TAG = SingleChartFragmentBase.class.getSimpleName();

    private Axis<? extends Comparable, ?> xAxis;
    private Axis<? extends Comparable, ?> yAxis;

    private ActionBarActivity mHostActivity;

    // Components in this fragment view:
    private ScrollView scrollView;
    private LinearLayout progressView;
    private ContentLoadingProgressBar progress;
    private TextView title;
    private TextView subtitle;
    private ChartView chartView;
    private ShinobiChart chart;
    private FrameLayout topCustomSection;
    private FrameLayout bottomCustomSection;

    private boolean refreshing = false;


    public SingleChartFragmentBase() {
        // Required empty public constructor
    }

    protected abstract String getShinobiLicenseKey();
    protected abstract Axis createXAxis();
    protected abstract Axis createYAxis();
    protected abstract Task<Void> refreshAsync();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_single_chart, container, false);
        progressView = (LinearLayout) v.findViewById(R.id.progress);
        progress = (ContentLoadingProgressBar) v.findViewById(R.id.content_loading);
        scrollView = (ScrollView) v.findViewById(R.id.scrollView);
        topCustomSection = (FrameLayout) v.findViewById(R.id.topCustomSection);
        bottomCustomSection = (FrameLayout) v.findViewById(R.id.bottomCustomSection);
        title = (TextView) v.findViewById(R.id.title);
        subtitle = (TextView) v.findViewById(R.id.subtitle);

        chartView = (ChartView) v.findViewById(R.id.chart);

        chartView.onCreate(savedInstanceState);
        chart = chartView.getShinobiChart();
        chart.setLicenseKey(getShinobiLicenseKey());

        int topCustomLayoutId = getTopCustomLayoutId();
        if (topCustomLayoutId != -1) {
            inflater.inflate(topCustomLayoutId, topCustomSection, true);
        }

        int bottomCustomLayoutId = getBottomCustomLayoutId();
        if (bottomCustomLayoutId != -1) {
            inflater.inflate(bottomCustomLayoutId, bottomCustomSection, true);
        }

        List<Axis<?,?>> allXAxises = new ArrayList<>(chart.getAllXAxes());
        for (Axis<?, ?> xAxis: allXAxises) {
            chart.removeXAxis(xAxis);
        }
        List<Axis<?,?>> allYAxises = new ArrayList<>(chart.getAllYAxes());
        for (Axis<?, ?> yAxis: allYAxises) {
            chart.removeYAxis(yAxis);
        }

        if (xAxis == null)
            xAxis = createXAxis();

        if (yAxis == null)
            yAxis = createYAxis();

        chart.setXAxis(xAxis);
        chart.setYAxis(yAxis);

        applyChartStyles();

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_single_chart, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem refreshItem = menu.findItem(R.id.menu_refresh);
        if (refreshing) {
            refreshItem.setEnabled(false);
            MenuItemCompat.setActionView(refreshItem, R.layout.abc_indeterminate_progress);
        } else {
            refreshItem.setEnabled(true);
            MenuItemCompat.setActionView(refreshItem, null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            refresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        chartView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        chartView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        chartView.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ActionBarActivity) {
            mHostActivity = (ActionBarActivity) activity;
        } else {
            throw new IllegalArgumentException("Host activity must be an instance of "
                    + ActionBarActivity.class.getName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mHostActivity = null;
    }

    protected void applyChartStyles() {
        // style the chart (default)
        chart.getStyle().setPlotAreaBackgroundColor(Color.TRANSPARENT);
        chart.getStyle().setBackgroundColor(Color.TRANSPARENT);
        chart.getStyle().setCanvasBackgroundColor(Color.TRANSPARENT);

        // style X-Axis (default)
        xAxis.getStyle().setLineColor(Color.BLACK);
        xAxis.getStyle().getTickStyle().setLabelTextSize(10);
        xAxis.getStyle().getTitleStyle().setTextSize(12);
        xAxis.getStyle().getGridlineStyle().setGridlinesShown(true);

        // style Y-Axis (default)
        yAxis.getStyle().setLineColor(Color.BLACK);
        yAxis.getStyle().getTickStyle().setLabelTextSize(10);
        yAxis.getStyle().getTitleStyle().setTextSize(12);
        yAxis.getStyle().getGridlineStyle().setGridlinesShown(true);
    }

    public void refresh() {
        if (!refreshing) {
            refreshing = true;
            mHostActivity.invalidateOptionsMenu();
            refreshAsync().continueWith(new Continuation<Void, Object>() {
                @Override
                public Object then(Task<Void> task) throws Exception {
                    refreshing = false;
                    if (mHostActivity != null) {
                        mHostActivity.invalidateOptionsMenu();
                    }
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        }
    }

    protected int getTopCustomLayoutId() {
        return -1;
    }

    protected int getBottomCustomLayoutId() {
        return -1;
    }

    public FrameLayout getTopCustomSection() {
        return topCustomSection;
    }

    public FrameLayout getBottomCustomSection() {
        return bottomCustomSection;
    }

    public void showProgress() {
        scrollView.setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);
        progress.show();
    }

    public void hideProgress() {
        progressView.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        progress.hide();
    }

    public ShinobiChart getChart() {
        return chart;
    }

    public ChartView getChartView() {
        return chartView;
    }

    public TextView getTitleView() {
        return title;
    }

    public TextView getSubtitleView() {
        return subtitle;
    }

    public Axis<? extends Comparable, ?> getXAxis() {
        return xAxis;
    }

    public Axis<? extends Comparable, ?> getYAxis() {
        return yAxis;
    }
}
