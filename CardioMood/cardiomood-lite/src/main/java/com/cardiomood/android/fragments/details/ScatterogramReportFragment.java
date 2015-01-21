package com.cardiomood.android.fragments.details;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.lite.R;
import com.flurry.android.FlurryAgent;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.NumberAxis;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

/**
 * Created by Anton Danshin on 03/01/15.
 */
public class ScatterogramReportFragment extends AbstractSessionReportFragment {

    @InjectView(R.id.download_expert_version) @Optional
    Button getExpertVersionButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.inject(this, root);
        getExpertVersionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=com.cardiomood.android.expert"));
                    startActivity(intent);
                } catch (Exception ex) {
                    Log.w("FeedbackActivity", "failed to start google play", ex);
                }
                FlurryAgent.logEvent("download_expert_clicked");
            }
        });
        ViewGroup.LayoutParams params = topCustomSection.getLayoutParams();
        params.height = 600;
        topCustomSection.setLayoutParams(params);
        return root;
    }

    @Override
    protected Axis createXAxis() {
        return new NumberAxis();
    }

    @Override
    protected Axis createYAxis() {
        return new NumberAxis();
    }

    @Override
    protected void collectDataInBackground(SessionEntity session, double[] time, double[] rrFiltered) {
        // do nothing
    }

    @Override
    protected void displayData(double[] rr) {
        chartView.setVisibility(View.GONE);
    }

    @Override
    protected int getTopCustomLayoutId() {
        return R.layout.scatterogram_report_top;
    }
}
