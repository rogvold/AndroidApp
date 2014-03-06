package com.cardiomood.android.fragments.monitoring;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cardiomood.android.R;
import com.cardiomood.android.heartrate.AbstractDataCollector;
import com.cardiomood.android.heartrate.CardioMoodHeartRateLeService;

/**
 * Created by danon on 04.03.14.
 */
public class HeartRateMonitoringFragment extends Fragment {

    private FragmentCallback mCallback = null;

    private Button startButton;
    private Button stopButton;


    public static HeartRateMonitoringFragment newInstance() {
        return new HeartRateMonitoringFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_heart_rate_monitoring, container, false);

        // todo: initialize view params

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof FragmentCallback) {
            mCallback = (FragmentCallback) activity;
        } else {
            throw new ClassCastException("Activity must implement " + FragmentCallback.class.getName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    private CardioMoodHeartRateLeService getService() {
        if (mCallback == null)
            return null;
        return mCallback.getService();
    }

    private AbstractDataCollector getDataCollector() {
        CardioMoodHeartRateLeService service = getService();
        if (service == null)
            return null;
        return (AbstractDataCollector) service.getDataCollector();
    }
}
