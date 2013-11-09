package com.cardiomood.android.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;

import com.cardiomood.android.R;
import com.cardiomood.android.SessionDetailsActivity;
import com.cardiomood.android.bluetooth.HeartRateMonitor;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Project: CardioMood
 * User: danon
 * Date: 23.05.13
 * Time: 23:50
 */
public class MonitorFragment extends Fragment {

   private static final String TAG = "CardioMood.MonitorFragment";
    // Bluetooth Intent request codes
    private static final int REQUEST_ENABLE_BT = 2;

    private static final int HR_DATA_EVENT = 1;
    private static final int CONNECTION_STATUS_CHANGE_EVENT = 2;

    private View container;
    private WebView webView;
    private View initialView;
    private Button connectDeviceButton;
    private ScrollView scrollView;

    private HeartRateMonitor hrMonitor;
    private List<HeartRateDataItem> collectedData;

    private ProgressDialog sessionSavingDialog;
    public static boolean isMonitoring = false;

    private Handler mUIUpdateHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            try {
                if (msg.what == HR_DATA_EVENT) {
                    saveHeartRateData(msg.arg1, 0, (short[])msg.obj);
                    execJS("setPulse(" + msg.arg1 + ")");
                }

                if (msg.what == CONNECTION_STATUS_CHANGE_EVENT) {
                    final int newStatus = msg.arg2;

                    if (getActivity() != null) {
                        getActivity().invalidateOptionsMenu();
                    }

                    // update button ui state
                    connectDeviceButton.setEnabled(newStatus == HeartRateMonitor.DISCONNECTED || newStatus == HeartRateMonitor.INIT);
                    if (newStatus == HeartRateMonitor.CONNECTING) {
                        connectDeviceButton.setText(R.string.connecting);
                    } else if (newStatus == HeartRateMonitor.CONNECTED) {
                       connectDeviceButton.setText(R.string.connected);
                    } else {
                        connectDeviceButton.setText(R.string.connect);
                    }

                    // update timer
                    if (newStatus == HeartRateMonitor.CONNECTED) {
                        monitorTime = 0;
                        if (timer != null) {
                            timer.cancel();
                            timer.purge();
                        }
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                monitorTime += 1;
                                if (monitorTime > 120) {
                                    performDisconnect();
                                    saveAndOpenSessionView();
                                } else {
                                    execJS("setProgress(" + (float)monitorTime/120*100 + ");");
                                    execJS("setPulse(" + hrMonitor.getHeartBeatsPerMinute() + ");");
                                }

                            }
                        }, 0, 1000);
                        if (collectedData!= null) {
                            collectedData.clear();
                        }

                        collectedData = Collections.synchronizedList(new ArrayList<HeartRateDataItem>());

                        setConnectedView();
                    } else if (newStatus == HeartRateMonitor.DISCONNECTED) {
                        if (timer != null) {
                            timer.cancel();
                            timer.purge();
                            timer = null;
                        }
                        if (monitorTime <=120) {
                            Toast.makeText(getActivity(), R.string.device_was_disconnected, Toast.LENGTH_SHORT).show();
                        }
                        setDisconnectedView();
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "handle message failed", ex);
            }
            return true;
        }
    });

    private long monitorTime = 0;
    private Timer timer = new Timer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void initWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setBuiltInZoomControls(false);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        webView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        this.container = inflater.inflate(R.layout.fragment_monitor, container, false);
        webView = (WebView) this.container.findViewById(R.id.webView1);
        initialView = this.container.findViewById(R.id.initial_view);
        scrollView = (ScrollView) this.container.findViewById(R.id.connected_view);
        connectDeviceButton = (Button) this.container.findViewById(R.id.btn_connect_device);

        connectDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performConnect();
            }
        });

        setDisconnectedView();
        return this.container;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_disconnect:
                performDisconnect();
                break;
        }
        return false;
    }

    private void setConnectedView() {
        isMonitoring = true;
        initWebView();
        scrollView.setVisibility(View.VISIBLE);
        webView.loadUrl(getString(R.string.asset_countdown_html));
        connectDeviceButton.setEnabled(false);
//        execJS("setSliderText(1, \"" + getString(R.string.monitor_slider_text1) + "\")");
//        execJS("setSliderText(2, \"" + getString(R.string.monitor_slider_text2) + "\")");
//        execJS("setSliderText(3, \"" + getString(R.string.monitor_slider_text3) + "\")");
//        execJS("setSliderText(4, \"" + getString(R.string.monitor_slider_text4) + "\")");
    }

    private void setDisconnectedView() {
        scrollView.setVisibility(View.GONE);
        webView.stopLoading();
        connectDeviceButton.setEnabled(true);
        initialView.setVisibility(View.VISIBLE);
        isMonitoring = false;
    }

    public void performConnect() {
        try {
            try {
                hrMonitor.initBLEService();
            } catch (Exception ex) {
                Toast.makeText(getActivity(), "Cannot start Bluetooth.", Toast.LENGTH_SHORT);
                return;
            }
            hrMonitor.attemptConnection();
        } catch (Exception ex) {
            Log.d(TAG, "performConnect(): "+ex);
            ex.printStackTrace();
        }
    }

    public void performDisconnect() {
        hrMonitor.disconnect();
        hrMonitor.cleanup();
    }

    private void execJS(final String js) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "execJS(): js = " + js);
                webView.loadUrl("javascript:"+js);
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        this.hrMonitor = new HeartRateMonitor(getActivity());
        this.hrMonitor.setCallback(new HeartRateMonitor.Callback() {

            @Override
            public void onSensorLocationChange(int oldLocation, int newLocation) {
                //execJS("updateDeviceLocation(" + oldLocation + "," + newLocation +")");
                //mUIUpdateHandler.sendEmptyMessage(0);
            }

            @Override
            public void onHRDataRecieved(int heartBeatsPerMinute, int energyExpended, short[] rrIntervals) {
                mUIUpdateHandler.sendMessage(mUIUpdateHandler.obtainMessage(HR_DATA_EVENT, heartBeatsPerMinute, 0, rrIntervals));
            }

            @Override
            public void onConnectionStateChange(int oldState, int newState) {
                Log.d(TAG, "connectionStateChanged(): oldState= " + oldState + "; newState = " + newState);
                mUIUpdateHandler.sendMessage(mUIUpdateHandler.obtainMessage(CONNECTION_STATUS_CHANGE_EVENT, oldState, newState));
            }
        });
    }

    private void showSavingSessionDialog() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sessionSavingDialog = ProgressDialog.show(getActivity(), getString(R.string.please_wait), getString(R.string.saving_data), true, false);
            }
        });
    }

    private void removeSavingSessionDialog() {
        if (sessionSavingDialog != null) {
            sessionSavingDialog.dismiss();
            sessionSavingDialog = null;
        }
    }

    private void saveAndOpenSessionView() {
        final List<HeartRateDataItem> data = new ArrayList<HeartRateDataItem>(collectedData);
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected void onPreExecute() {
                showSavingSessionDialog();
            }

            @Override
            protected Long doInBackground(Void... params) {
                if (data.isEmpty())
                    return null;
                HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO();
                HeartRateSession session = new HeartRateSession();
                session.setDateStarted(data.get(0).getTimeStamp());
                session.setDateEnded(new Date());
                session.setStatus(SessionStatus.COMPLETED);
                session = sessionDAO.insert(session, data);
                Long sessionId = session.getId();
                return sessionId;
            }

            @Override
            protected void onPostExecute(Long sessionId) {
                if (sessionId == null) {
                    return;
                }
                Intent intent = new Intent(getActivity(), SessionDetailsActivity.class);
                intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, sessionId);
                intent.putExtra(SessionDetailsActivity.POST_RENDER_ACTION_EXTRA, SessionDetailsActivity.RENAME_ACTION);
                startActivity(intent);

                removeSavingSessionDialog();
            }
        }.execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    // TODO: Set up BT HR Monitor
                } else {
                    Log.e(TAG, "onActivityResult(): BT not enabled");
                    Toast.makeText(getActivity(), "Bluetooth is not enabled. Leaving...",
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        performDisconnect();
    }

    private void saveHeartRateData(int heartBeatsPerMinute, int energyExpended, short[] rrIntervals) {
        try {
            for (short rr: rrIntervals) {
                collectedData.add(new HeartRateDataItem(heartBeatsPerMinute, (int) (rr * (1.0 / 1024 * 1000))));
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG, "onCreateOptionsMenu()");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_monitor, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.menu_disconnect);
        if (item != null && hrMonitor != null) {
            int c = hrMonitor.getConnectionStatus();
            if (c == HeartRateMonitor.CONNECTED) {
                item.setEnabled(true);
                //item.setVisible(true);
            } else {
                item.setEnabled(false);
                //item.setVisible(false);
            }
        }
    }

}
