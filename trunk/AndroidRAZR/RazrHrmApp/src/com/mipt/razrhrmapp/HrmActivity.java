package com.mipt.razrhrmapp;

import com.motorola.bluetoothle.BluetoothGatt;
import com.motorola.bluetoothle.hrm.IBluetoothHrm;
import com.motorola.bluetoothle.hrm.IBluetoothHrmCallback;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class HrmActivity extends Activity {

    private Context mContext;
	private boolean ifPhoneSupportsBLE;
	private int[] data;
	private BluetoothAdapter mBluetoothAdapter;
	private static final String className = "android.bluetooth.BluetoothGattService";
	public static final ParcelUuid HRM = ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb");
	String hrmUUID = "";
	private IntentFilter filter_scan;
	private boolean flag_BleRcvrReg;
	private boolean mBLEBound = false;
    private IndNotCallback mIndNotCallback;
    
    private static final int CONNECTED = 1;
    private static final int CONNECTING = 2;
    private static final int DISCONNECTED = 3;
    private static final int DISCONNECTING = 4;
    private static final int INIT = -1;

    public int mBleState = INIT;

	protected boolean BleDisconn;
	
	private BluetoothDevice mBleDevice = null;
	
	private ListView bondedDevicesList;
	private Button connectButton;
	private TextView bpmTv;
	private ArrayAdapter mBondedDevicesListAdapter;
	
	private boolean deviceConnected = false;
	
	private int mHeartBeatsPerMinute;
	private int mEnergyExpended;
	private int previous_mHeartBeatsPerMinute;
	
	IBluetoothHrm mHrmService = null;
    ServiceConnection mConnection = new ServiceConnection() 
    {
		public void onServiceConnected(ComponentName className,
    			IBinder service) 
    	{
    		mHrmService = IBluetoothHrm.Stub.asInterface(service);
    		mBLEBound = true;
    		//mLogArrayAdapter.add("IBluetoothHrm service binded");
    	}

    	public void onServiceDisconnected(ComponentName arg0) 
    	{
    		mHrmService = null;
    		mBLEBound = false;    	
    		//mLogArrayAdapter.add("IBluetoothHrm service un-binded");
    	}
    };
    
    private Handler mUIUpdateHandler = new Handler()
    {
		public void handleMessage(android.os.Message msg) 
		{
			updateUI();
		};
    };
    
    private void updateUI()
	{
    	Log.v("Update GUI", "updateUI parse han");
    	if(mHeartBeatsPerMinute!= previous_mHeartBeatsPerMinute)
		{
			Log.d("onUpdateGui", mHeartBeatsPerMinute + " bpm");
			bpmTv.setText( " "+mHeartBeatsPerMinute + " bpm");
			previous_mHeartBeatsPerMinute = mHeartBeatsPerMinute;
		}
		//mTvSenLoc.setText(getStringSensorLocation(mSensorLocation));
	}

	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_hrm);
        
        /* Check if Bluetooth Low Energy is supported on phone */
        try 
        {
	        Class<?> object = Class.forName(className); 
	        ifPhoneSupportsBLE = true;
        } catch (Exception e) 
        {
        	ifPhoneSupportsBLE = false;
        	Toast.makeText(mContext, "Your phone is not compatible with BLE", Toast.LENGTH_SHORT).show();
        } //End logic to check Low Energy support
        
        if (!ifPhoneSupportsBLE) 
        {	
        	finish();
        	return;
        } 
        else 
        {
        	//data[0] = 0x00;
  			//data[1] = 0x00;
  		
  			// Set up the window layout
  			setContentView(R.layout.activity_hrm);
  			
  			mBondedDevicesListAdapter = new ArrayAdapter<String>(this, R.layout.message);
  	        mContext = this.getApplicationContext();
  			bondedDevicesList = (ListView)findViewById(R.id.bondedDevicesList);
  			bondedDevicesList.setAdapter(mBondedDevicesListAdapter);
  			bpmTv = (TextView)findViewById(R.id.bpmTv);
  			bondedDevicesList.setOnItemClickListener(new OnItemClickListener()
  			{
  				public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
  				{
  					if (deviceConnected)
  					{
  						//TODO disconnect device
  					}
  					else
  					{
  						mBluetoothAdapter.cancelDiscovery();
  						String info = ((TextView) v).getText().toString();
  						Log.i("OnClick", "trying to connect..." + info.length());
  						String deviceAddress = getDeviceAddressFromDeviceInfo(info);
  						
  						mBleDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
  						
  						if (mBleDevice != null)
  						{
  							try 
  							{
  								int status = mHrmService.connectLe(mBleDevice, "0000180d00001000800000805f9b34fb", mIndNotCallback);
  								if (status == BluetoothGatt.SUCCESS)
  								{
									Log.i("OnConnect", "connectLe sent out succesfully.");
									mBleState = CONNECTING;
  								}
  								else
  								{
									Log.e("onConnect", "ConnectLe sent out but failed");
								}
  							} catch (RemoteException e) {
  								// TODO Auto-generated catch block
  								Log.e("OnConnect", e.getMessage());
  							}
						}
  					}
  				}
  				
  				private String getDeviceAddressFromDeviceInfo(String info) {
  					return info.split("\n")[1];
  				}
  			}
  			);
  			
  			// Get local Bluetooth adapter
  			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
  			if (mBluetoothAdapter == null) 
  			{
  				Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
  				finish();
  				return;
  			}
          
  			hrmUUID = HRM.toString();
          
  			
  			filter_scan = new IntentFilter(BluetoothGatt.CONNECT_COMPLETE);
  			filter_scan.addAction(BluetoothGatt.DISCONNECT_COMPLETE);
  			filter_scan.addAction(BluetoothGatt.GET_COMPLETE);
  			filter_scan.addAction(BluetoothGatt.SET_COMPLETE);
  			
  			registerReceiver(mConn_Receiver, filter_scan);  
  			flag_BleRcvrReg = true;
  			
  			Intent intent1 = new Intent(IBluetoothHrm.class.getName());
  			getApplicationContext().bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);
  			//mLogArrayAdapter.add("Request IBluetoothHrm service binding...");
  			Log.d("OnCreate", "Request IBluetoothHrm service binding...");
  			
  			Intent intent2 = new Intent(BluetoothGatt.ACTION_START_LE);
  			intent2.putExtra(BluetoothGatt.EXTRA_PRIMARY_UUID,hrmUUID);//HRM service UUID
  			sendBroadcast(intent2);
  			
  			mIndNotCallback = new IndNotCallback(hrmUUID);
  			mContext = this.getApplicationContext();

  			for(BluetoothDevice bd: mBluetoothAdapter.getBondedDevices())
  			{
  				mBondedDevicesListAdapter.add(bd.getName() + "\n" + bd.getAddress());
  			}
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
    	getMenuInflater().inflate(R.menu.activity_hrm, menu);
        return true;
    }
	
	private final BroadcastReceiver mConn_Receiver = new BroadcastReceiver() 
    {
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			// TODO Auto-generated method stub
			String action = intent.getAction();
			Log.i("onReceive", "some broadcast, action is: "+action);
			if (BluetoothGatt.CONNECT_COMPLETE.equals(action)) 
			{
				Log.i("onReceive", "LE connection complete - ");
				String service = intent.getStringExtra("uuid");// Todo
				int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
				switch (status)
				{
					case BluetoothGatt.SUCCESS :
						Log.i("onReceive", "Connected successfully ! Service: "+service);	
						BleDisconn = false;
						mBleState = CONNECTED;
						deviceConnected = true;
						break;
					default : 
						Log.i("onReceive", "Connection failed. Service: "+service);
						mBleState = DISCONNECTED;
						break;
				}
			}//endif connect_complete
			
			if (action.equals(BluetoothGatt.DISCONNECT_COMPLETE)) 
			{
             	int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                intent.getStringExtra("uuid");
                switch (status)
				{
					case BluetoothGatt.SUCCESS :
						mBleState = DISCONNECTED;
	                    mBleDevice = null;
						break;
					default : 
						mBleState = DISCONNECTED;
	                    mBleDevice = null;
	                    break;
				}
			}//endif disconnect_complete
			
			if ((BluetoothGatt.GET_COMPLETE).equals(action)) 
			{
                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                String service = intent.getStringExtra("uuid");// Todo
                int length = intent.getIntExtra("length", 0);
                
                switch (status)
				{
					case BluetoothGatt.SUCCESS :
						if ((length >= 0) && service.equalsIgnoreCase(hrmUUID)) 
	                	{
	                		byte[] data = new byte[length];
	                		data = intent.getByteArrayExtra("data");
	                		Log.v("GET_COMPLETE", "onReceive GET_COMPLETE data first byte " + data[0]);
	                		
	                		//Log.i("onReceive", "Sensor Location returned by GET_COMPLETE: "+getStringSensorLocation(mSensorLocation));
	                		mUIUpdateHandler.sendEmptyMessage(0);

	                		updateUI();                	
	            		}
						break;
					default : 
						Toast.makeText(mContext, "Sensor query failed! ", Toast.LENGTH_LONG).show();
						break;
				}
            }//endif get_complete
			
			if (action.equals(BluetoothGatt.SET_COMPLETE)) 
			{
				Log.i("onReceive", "SET COMPLETE received, action is: "+action);
        		Log.e("SET COMPLETE", "SET COMPLETE received: " + action);

                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                String service = intent.getStringExtra("uuid");// Todo
                if (status == BluetoothGatt.SUCCESS && service.equalsIgnoreCase(hrmUUID)) 
                {}
                else if(status != BluetoothGatt.SUCCESS && service.equalsIgnoreCase(hrmUUID))
    					Toast.makeText(mContext, "Notification enabling failed! ", Toast.LENGTH_LONG).show();
            
			}//endif set_complete
		}
	};
	
	private class IndNotCallback extends IBluetoothHrmCallback.Stub 
	{
		private String service;
    	IndNotCallback(String serv) 
        {
    		service = serv;
        }

        public void indicationLeCb(BluetoothDevice device, String service, int length, byte[] data) 
        {
        	Log.i("indicationLeCb", "indicationLeCb");
        	parseData(length , data);
        }

        public void notificationLeCb(BluetoothDevice device, String service, int length,
        		byte[] data) 
        {
        	Log.i("notificationLeCb", "notificationLeCb");
        	parseData(length , data);
        }
    }
	
	private void parseData(int length , byte[] data)
	{
		mHeartBeatsPerMinute = 0;
		if (data[1] != 0) 
		{
			mHeartBeatsPerMinute = ((data[0] & 0xFF) << 8) + (data[1] & 0xFF);
		} 
		else 
		{
			mHeartBeatsPerMinute = data[0] & 0xFF;
		}
		//correct the raw data from LE call back that if>128, it becomes negative
		mHeartBeatsPerMinute = (data[0] < 0) ? (128+(128+mHeartBeatsPerMinute)):(mHeartBeatsPerMinute);

		mEnergyExpended |= data[2] & 0xFF;
		mEnergyExpended |= ((data[3] & 0xFF)<<8);

		mUIUpdateHandler.sendEmptyMessage(0);
	}
}