package com.example.hello.world;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.broadcom.bt.le.api.BleAdapter;
import com.motorola.bluetoothle.BluetoothGatt;
import com.motorola.bluetoothle.IBluetoothGattCallback;
import com.motorola.bluetoothle.hrm.IBluetoothHrm;
import com.motorola.cp.hrm.BtBioActivity.callback;

public class MainActivity extends Activity {
	
	private class BtGattCallback extends IBluetoothGattCallback.Stub 
	{
		public void indicationGattCb(BluetoothDevice device, String service,
					String characterstic_handle, String[] data) 
		{
			// handle the returned data here
		}
		public void notificationGattCb(BluetoothDevice device, String service,
					String characterstic_handle, byte[] data) 
		{
			// handle the returned data here
		}
	}

	private BluetoothGatt mGattService;
    private BtGattCallback myCallback;
    private static BluetoothAdapter mAdapter;
    private IntentFilter filter_scan;
    BluetoothAdapter mBluetoothAdapter;
	private String hrmUUID;
    public static final ParcelUuid HRM = ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public Context mContext;
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	Log.i("onCreate", "got BTAdapter");
    	hrmUUID = HRM.toString();
    	
    	filter_scan = new IntentFilter(BluetoothGatt.CONNECT_COMPLETE);
  	    filter_scan.addAction(BluetoothGatt.DISCONNECT_COMPLETE);
  	    filter_scan.addAction(BluetoothGatt.GET_COMPLETE);
  	    filter_scan.addAction(BluetoothGatt.SET_COMPLETE);

  	    registerReceiver(mConn_Receiver, filter_scan);
  	    Log.e("onCreate, after adding action listeners", "called registerReceiver");
  	    
  	    Intent intent1 = new Intent(IBluetoothHrm.class.getName());
  	    getApplicationContext().bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);
	    mLogArrayAdapter.add("Request IBluetoothHrm service binding...YAHOO");
	    Log.d(TAG, "Request IBluetoothHrm service binding...");

	    Intent intent2 = new Intent(BluetoothGatt.ACTION_START_LE);
	    intent2.putExtra(BluetoothGatt.EXTRA_PRIMARY_UUID,hrmUUID);//HRM service UUID
	    sendBroadcast(intent2);
  
	    callback1 = new callback(hrmUUID);
	    mContext = this.getApplicationContext(); 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private final BroadcastReceiver mConn_Receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			mLogArrayAdapter.add("some broadcast, action is: "+action);
			if (BluetoothGatt.CONNECT_COMPLETE.equals(action)) {
				mLogArrayAdapter.add("LE connection complete - ");

				int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
				String service = intent.getStringExtra("uuid");// Todo
				if (status == BluetoothGatt.SUCCESS ) {
					mLogArrayAdapter.add("Connected successfully ! Service: "+service);	
					Log.e("onReceive", service);
					mTvState2.setText("  Connected  ");
					
					leDisconn = false;

					mLeState = CONNECTED;
							                
				} else if (status != BluetoothGatt.SUCCESS ) {	                    
					mLogArrayAdapter.add("Connection failed. Service: "+service);
					Log.e("onReceive", "NOT CONNECTED");
					mTvState2.setText("  Not Connected  ");
					mLeState = DISCONNECTED;
				}
			}
			else if (action.equals(BluetoothGatt.DISCONNECT_COMPLETE)) {
                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                intent.getStringExtra("uuid");
                if (status == BluetoothGatt.SUCCESS ) {
                    mLeState = DISCONNECTED;
                    device1 = null;
					mTvState2.setText("  Not Connected  ");
                } else if (status != BluetoothGatt.SUCCESS ) {
                	mLeState = DISCONNECTED;
                    device1 = null;
					mTvState2.setText("  Not Connected  ");
                }

            }
			
			else if ((BluetoothGatt.GET_COMPLETE).equals(action)) {
                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                String service = intent.getStringExtra("uuid");// Todo
                int length = intent.getIntExtra("length", 0);
                if(status == BluetoothGatt.SUCCESS ){
                	if ((length >= 0) && service.equalsIgnoreCase(hrmUUID)) {
                		byte[] data = new byte[length];
                		data = intent.getByteArrayExtra("data");
                		mSensorLocation = data[0];
                		Log.v(TAG, "onReceive GET_COMPLETE data first byte " + data[0]);
                		
                		mLogArrayAdapter.add("Sensor Location returned by GET_COMPLETE: "+getStringSensorLocation(mSensorLocation));
                		mUIUpdateHandler.sendEmptyMessage(0);
                		
                		updateUI();                	
                		}
                }else{
        			Toast.makeText(mContext, "Sensor query failed! ", Toast.LENGTH_LONG).show();
                }
            }
			else if (action.equals(BluetoothGatt.SET_COMPLETE)) {
				mLogArrayAdapter.add("SET COMPLETE received, action is: "+action);
        		Log.e(TAG, "SET COMPLETE received: " + action);

                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                String service = intent.getStringExtra("uuid");// Todo
                if (status == BluetoothGatt.SUCCESS && service.equalsIgnoreCase(hrmUUID)) {
                }else if(status != BluetoothGatt.SUCCESS && service.equalsIgnoreCase(hrmUUID)){
        			Toast.makeText(mContext, "Notification enabling failed! ", Toast.LENGTH_LONG).show();
                } 
            }
			
		}

	};
    
    // STEP 3: OnActivityResult
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        switch (requestCode) 
        {
	        case 1 :
		        if (resultCode == Activity.RESULT_OK) 
		        {
		        	Toast.makeText(getApplicationContext(), "Bluetooth found", Toast.LENGTH_SHORT).show();
		        }
	        break;
        }
    }
    
    @Override
    public void onDestroy()
    {
    	unregisterReceiver(mReceiver);
    }
}
