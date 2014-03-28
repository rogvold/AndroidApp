package com.cardiomood.sport.android.system.bluetooth;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.heartrate.bluetooth.HeartRateLeService;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.cardiomood.sport.android.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class HeartRateMonitor {
	
	public static final ParcelUuid HRM = ParcelUuid.fromString("0000180D-0000-1000-8000-00805f9b34fb");
	//public static final ParcelUuid BATTERY = ParcelUuid.fromString("0000180F-0000-1000-8000-00805f9b34fb");
	public static final int CONNECTED = 2;
	public static final int CONNECTING = 1;
	public static final int DISCONNECTED = 0;
	public static final int DISCONNECTING = 3;
	public static final int INIT = -1;
	
	private static final String TAG = "HeartRateMonitor";

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;
	
	// Bluetooth Intent request codes
    private static final int REQUEST_ENABLE_BT = 2;
	
	private Activity mActivity;
	private Context mContext;
	private Callback mCallback;

    private Handler mHandler;
    private boolean mScanning = false;

    private AlertDialog alertSelectDevice;
	ProgressDialog aDialog;
	
	// sensor parameters
	private int energyExpended = 0;
	private int heartBeatsPerMinute = 0;
	
	private BluetoothDevice device;
	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			hrmService = ((HeartRateLeService.LocalBinder) service).getService();
			bleBound = true;
		}

		public void onServiceDisconnected(ComponentName arg0) {
			hrmService = null;
			bleBound = false;
		}
	};
	private boolean bleBound = false;
	private boolean leDisconnected = true;
	boolean flag_leRcvrReg = false;
	private int leState = INIT;
	private HeartRateLeService hrmService;
	
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
            if (LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED.equals(action)) {
                if (mCallback != null) {
                    mCallback.onHRDataRecieved(intent.getIntExtra(LeHRMonitor.EXTRA_BPM, 0), 0, intent.getShortArrayExtra(LeHRMonitor.EXTRA_INTERVALS));
                }
                return;
            }
            if (LeHRMonitor.ACTION_BPM_CHANGED.equals(action)) {
                heartBeatsPerMinute = intent.getIntExtra(LeHRMonitor.EXTRA_NEW_BPM, 0);
                return;
            }
            if (LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED.equals(action)) {
                int oldStatus = intent.getIntExtra(LeHRMonitor.EXTRA_OLD_STATUS, 0);
                int newStatus = intent.getIntExtra(LeHRMonitor.EXTRA_NEW_STATUS, 0);
                switch (newStatus) {
                    case LeHRMonitor.CONNECTING_STATUS:
                        setConnectionStatus(CONNECTING);
                        break;
                    case LeHRMonitor.DISCONNECTING_STATUS:
                        setConnectionStatus(DISCONNECTING);
                        break;
                    case LeHRMonitor.CONNECTED_STATUS:
                        setConnectionStatus(CONNECTED);
                        break;
                    default:
                        setConnectionStatus(DISCONNECTED);
                }
            }

		}

	};
	
	/** Constructor */
	public HeartRateMonitor(Activity activity) {
		mActivity = activity;
		mContext = activity.getApplicationContext();
	}
	
	public void initBLEService() {
        mHandler = new Handler();

        mActivity.registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());
        flag_leRcvrReg = true;

        Intent intent1 = new Intent(CardioMoodHeartRateLeService.class.getName());
        mActivity.bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);
	}

    public boolean requestEnableBluetooth() {
        LeHRMonitor monitor = hrmService.getMonitor();
        if (monitor == null)
            return false;
        BluetoothAdapter bluetoothAdapter = monitor.getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(mActivity, "Bluetooth adapter is not available.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            return false;
        } return true;
    }

    public void attemptConnection() {
        attemptConnection(null);
    }

    public void attemptConnection(String deviceAddress) {
        try {
            try {
                if (!requestEnableBluetooth())
                    return;

                // Check if already initialized
                LeHRMonitor monitor = hrmService.getMonitor();

                if (monitor != null && monitor.getConnectionStatus() != LeHRMonitor.INITIAL_STATUS) {
                    hrmService.disconnect();
                    hrmService.close();
                }

                if (!hrmService.initialize(mActivity)) {
                    Toast.makeText(mActivity, "Failed to initialize service. Make sure your device is supported.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    return;
                }

                if (deviceAddress != null && !deviceAddress.isEmpty()) {
                    hrmService.connect(deviceAddress);
                    return;
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed to initialize service.", ex);
                Toast.makeText(mActivity, "Cannot start Bluetooth.", Toast.LENGTH_SHORT);
                return;
            }
            showConnectionDialog();
        } catch (Exception ex) {
            Log.d(TAG, "performConnect(): "+ex);
            ex.printStackTrace();
        }
    }

    public boolean isConnected() {
        return leState == CONNECTED;
    }
	
	public int getConnectionStatus() {
		return leState;
	}

	public void setConnectionStatus(int leState) {
		int oldState = this.leState;
		if (leState != oldState) {
			this.leState = leState;
			leStateChanged(oldState, leState);
		}
	}
	
	public int getHeartBeatsPerMinute() {
		return heartBeatsPerMinute;
	}
	
	public int getEnergyExpended() {
		return energyExpended;
	}
	
	public void disconnect() {
		if (!leDisconnected) {
			if (leState == CONNECTED) {
				if (device != null) {
					setConnectionStatus(DISCONNECTING);
				    hrmService.disconnect();
				}
			}
			leDisconnected = true;
		}
	}
	
	public void cleanup() {
		if (flag_leRcvrReg) {
			flag_leRcvrReg = false;
			mContext.unregisterReceiver(broadcastReceiver);
		}

		if (bleBound && hrmService != null) {
            bleBound = false;
			mContext.unbindService(mConnection);
		}
		
		if (mConnection != null)
			mConnection = null;
		if (hrmService != null)
			hrmService = null;
	}
	
	public void setCallback(Callback callback) {
		mCallback = callback;
	}
	
	public BluetoothDevice getDevice() {
		return device;
	}
	
	@Override
	public void finalize() {
        try {
            super.finalize();
        } catch (Throwable ex) {
            // nothing to do here
        } finally {
            disconnect();
            cleanup();
        }
    }

    @SuppressWarnings("NewApi")
    private void showConnectionDialog() {
        Log.d(TAG, "showConnectionDialog()");
        final BluetoothAdapter bluetoothAdapter = hrmService.getMonitor().getCurrentBluetoothAdapter();

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout_listview = inflater.inflate(R.layout.device_list, (ViewGroup) mActivity.findViewById(R.id.root_device_list));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity);
        dialogBuilder.setView(layout_listview);

        final ArrayList<String> discoveredDeviceNames = new ArrayList<String>();
        final ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<String>(mActivity, R.layout.device_name, discoveredDeviceNames);
        final ListView pairedListView = (ListView) layout_listview.findViewById(R.id.paired_devices);
        mPairedDevicesArrayAdapter.clear();
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        final Set<BluetoothDevice> discoveredDevices = new HashSet<BluetoothDevice>();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        discoveredDevices.addAll(pairedDevices);
        boolean foundDevices = false;
        if (pairedDevices.size() > 0) {

            if (pairedDevices.size() > 0) {

                // foundDevices = false;
                for (BluetoothDevice device : pairedDevices) {
                    for (String s : IMonitors.MonitorNamesPatternLE) {
                        Pattern p_le = Pattern.compile(s);
                        if (device.getName().matches(p_le.pattern())) {
                            mPairedDevicesArrayAdapter.add(device.getName()  + "\n" + device.getAddress());
                            foundDevices = true;
                        }
                    }
                }
            }

            if (foundDevices) {
                layout_listview.findViewById(R.id.title_paired_devices)
                        .setVisibility(View.VISIBLE);
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    layout_listview.findViewById(R.id.title_paired_devices)
                            .setVisibility(View.GONE);
                    String noDevices = "None devices have been paired";
                    mPairedDevicesArrayAdapter.add(noDevices);
                }
            }
        }

        dialogBuilder.setTitle(mContext.getText(R.string.select_device_to_connect));
        alertSelectDevice = dialogBuilder.create();
        final Object leScanCallback = (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
                ? null : new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
                Log.i(TAG, "onLeScan(): bluetoothDevice.address="+bluetoothDevice.getAddress());
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String deviceString = bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress();
                        for (BluetoothDevice device : discoveredDevices) {
                            if (device.getAddress().equals(bluetoothDevice.getAddress()))
                                return;
                        }
                        discoveredDevices.add(bluetoothDevice);
                        mPairedDevicesArrayAdapter.add(deviceString);
                        mPairedDevicesArrayAdapter.notifyDataSetChanged();
                    }
                });

            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            alertSelectDevice.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    scanLeDevice(bluetoothAdapter, true, (BluetoothAdapter.LeScanCallback) leScanCallback);
                }
            });
        }
        alertSelectDevice.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    scanLeDevice(bluetoothAdapter, false, (BluetoothAdapter.LeScanCallback) leScanCallback);
                }
                if (hrmService == null)
                    return;
                LeHRMonitor monitor = hrmService.getMonitor();
                if (monitor == null)
                    return;
                int status = monitor.getConnectionStatus();
                if (status == LeHRMonitor.READY_STATUS || status == LeHRMonitor.INITIAL_STATUS) {
                    hrmService.close();
                }
            }
        });
        AdapterView.OnItemClickListener mPairedListClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
                bluetoothAdapter.cancelDiscovery();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    scanLeDevice(bluetoothAdapter, false, (BluetoothAdapter.LeScanCallback) leScanCallback);
                }

                String info = ((TextView) v).getText().toString();
                //Log.d(TAG, "mPairedListClickListener.onItemClick(): the total length is " + info.length());
                String deviceAddress = info.substring(info.lastIndexOf("\n")+1);

                try {
                    hrmService.connect(deviceAddress);
                } catch (Exception ex) {
                    Log.e(TAG, "mBluetoothLeService.connect() failed to connect to the device '" + deviceAddress+"'", ex);
                }
                alertSelectDevice.dismiss();
                alertSelectDevice = null;
            }
        };
        pairedListView.setOnItemClickListener(mPairedListClickListener);
        alertSelectDevice.show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final BluetoothAdapter bluetoothAdapter, final boolean enable, final BluetoothAdapter.LeScanCallback leScanCallback) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanning) {
                        mScanning = false;
                        bluetoothAdapter.stopLeScan(leScanCallback);
                        Log.d(TAG, "LeScan stopped.");
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothAdapter.startLeScan(new UUID[]{UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")}, leScanCallback);
            Log.d(TAG, "LeScan started.");
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
            Log.d(TAG, "LeScan stopped.");
        }
    }
	
	private void leStateChanged(int oldState, int newState) {
		if (mCallback != null) {
			mCallback.onConnectionStateChange(oldState, newState);
		}
	}
	
	public static String getStringSensorLocation(int intLocation) {
		switch (intLocation) {
		case 0:
			return "Other";
		case 1:
			return "Chest";
		case 2:
			return "Wrist";
		case 3:
			return "Finger";
		case 4:
			return "Hand";
		case 5:
			return "Ear Lobe";
		case 6:
            return "Foot";
		case 100:
            return "No Skin Contact";
		default:
			return "Unknown";
		}
	}
	
	public static String getStatusAsText(int leState) {
		String strStatus = "N/A";
		switch (leState) {
		case INIT:
			strStatus = "No device";
			break;
		case CONNECTED:
			strStatus = "Connected";
			break;
		case CONNECTING:
			strStatus = "Connecting...";
			break;
		case DISCONNECTING:
			strStatus = "Disconnecting...";
			break;
		case DISCONNECTED:
			strStatus = "Disconnected";
			break;
		default:
			strStatus = "N/A";
			break;
		}
		return strStatus;
	}

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED);
        intentFilter.addAction(LeHRMonitor.ACTION_BPM_CHANGED);
        intentFilter.addAction(LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED);
        return intentFilter;
    }

	public static interface Callback {

        void onConnectionStateChange(int oldState, int newState);

        void onHRDataRecieved(int heartBeatsPerMinute, int energyExpended, short[] rrIntervals);

    }
	
}
