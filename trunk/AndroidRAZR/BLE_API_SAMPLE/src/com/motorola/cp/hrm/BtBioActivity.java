/******************************************************************************
  Copyright (c) 2011-2012, Motorola Mobility, Inc. All rights reserved except as 
  otherwise explicitly indicated.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

        Redistributions of source code must retain the above copyright notice, 
 		this list of conditions and the following disclaimer.

        Redistributions in binary form must reproduce the above copyright notice, 
		this list of conditions and the following disclaimer in the documentation 
		and/or other materials provided with the distribution.

        Neither the name of the Motorola, Inc. nor the names of its contributors 
		may be used to endorse or promote products derived from this software 
		without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************/

package com.motorola.cp.hrm;

import android.os.ParcelUuid; 
import java.util.Set;
import java.util.regex.Pattern;

import com.motorola.bluetoothle.hrm.IBluetoothHrm;
import com.motorola.bluetoothle.hrm.IBluetoothHrmCallback;
import com.motorola.cp.hrm.R;
import com.motorola.cp.hrm.IMonitors; 
import com.motorola.bluetoothle.BluetoothGatt;
 
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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class BtBioActivity extends Activity  {
    // Debugging
    private static final String  TAG = "BtBioActivity";
    private static final String className = "android.bluetooth.BluetoothGattService";
   
    private static final boolean DEBUG = true;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    
    // Bluetooth Intent request codes
    private static final int REQUEST_ENABLE_BT = 2;

    private TextView        mTvDevice2;
    private TextView        mTvState2;
    private TextView        mTvHR2;
    boolean mBLEBound = false;
    boolean ifPhoneSupportsLE = false;
    AlertDialog.Builder alert_paired;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    BluetoothDevice device1 = null;
    ProgressDialog aDialog;
    
	// Array adapter for the conversation thread
	ArrayAdapter<String> mLogArrayAdapter;
	private ListView mLogView;
	boolean log_clicked = false;	
	String hrmUUID = "";
    private callback callback1;
	private Context mContext;
	private IntentFilter filter_scan;
    private Button mBtDev2;
	boolean flag_leRcvrReg = false;
	AlertDialog mDialog;
	boolean motoCompDevFound = false;
	
    Button mBtSenLoc;
    TextView mTvSenLoc;
    CheckBox mCbIndi;
    CheckBox mCbNoti;
    
    private byte[] data = new byte[2];
    
	private static int mSensorLocation = -1;
    private static int mEnergyExpended = 0;
    private static int mHeartBeatsPerMinute = 0;
    
    private static final int CONNECTED = 1;
    private static final int CONNECTING = 2;
    private static final int DISCONNECTED = 3;
    private static final int DISCONNECTING = 4;
    private static final int INIT = -1;

    public int mLeState = INIT;
    public static final ParcelUuid HRM = ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    
    int i = 0;
    int previous_mHeartBeatsPerMinute = 0,previous_mSensorLocation=0; 
    
	boolean leDisconn = true;

    private Handler mUIUpdateHandler = new Handler()
    {
		public void handleMessage(android.os.Message msg) 
		{
			updateUI();
		};
    };
 
    IBluetoothHrm mHrmService = null;
    ServiceConnection mConnection = new ServiceConnection() 
    {

    	public void onServiceConnected(ComponentName className,
    			IBinder service) 
    	{
    		mHrmService = IBluetoothHrm.Stub.asInterface(service);
    		mBLEBound = true;
    		mLogArrayAdapter.add("IBluetoothHrm service binded");
    	}

    	public void onServiceDisconnected(ComponentName arg0) 
    	{
    		mHrmService = null;
    		mBLEBound = false;    	
    		mLogArrayAdapter.add("IBluetoothHrm service un-binded");
    	}
    };

    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
    	mContext = this.getApplicationContext();
        /* Check if Bluetooth Low Energy is supported on phone */
        try 
        {
	        Class<?> object = Class.forName(className); 
	        ifPhoneSupportsLE = true;
        } catch (Exception e) 
        {
        	ifPhoneSupportsLE = false;
        } //End logic to check Low Energy support
        
      if (!ifPhoneSupportsLE) 
      {	
        	String message = "Bluetooth Low Energy is not supported on this phone!";
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show(); 
            finish();
            return;
      } 
      else 
      {
        data[0] = 0x00;
		data[1] = 0x00;
		
        // Set up the window layout
        setContentView(R.layout.main);
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) 
        {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        mTvDevice2 = (TextView)findViewById(R.id.m_device2);
        mTvState2 = (TextView)findViewById(R.id.m_state2);
        mTvHR2 = (TextView)findViewById(R.id.m_hr2);
        mBtDev2 = (Button)findViewById(R.id.m_bt_dev2);
               
        mBtSenLoc = (Button)findViewById(R.id.m_bt_senloc);
        mTvSenLoc = (TextView)findViewById(R.id.m_tv_senloc);
        
        mLogArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mLogView = (ListView) findViewById(R.id.in);
        mLogView.setAdapter(mLogArrayAdapter);
        
		hrmUUID = HRM.toString();
        
		
        findViewById(R.id.m_log).setOnClickListener(new OnClickListener() 
        {
            public void onClick(View v) 
            {
            	if(log_clicked ==false)
            	{
            		findViewById(R.id.m_ll2).setVisibility( View.GONE );
            		findViewById(R.id.m_tb2).setVisibility( View.GONE );

            		log_clicked = true;
            	}
            	else
            	{

            		findViewById(R.id.m_ll2).setVisibility( View.VISIBLE );
            		findViewById(R.id.m_tb2).setVisibility( View.VISIBLE );
            		log_clicked = false;
            	}
            }
        });

        mCbIndi = (CheckBox) findViewById(R.id.m_cb_indi);
        mCbIndi.setOnClickListener(new OnClickListener() 
        {
        	public void onClick(View v) 
        	{
        		Log.i(TAG, "mBtIndi");
        		if(mCbIndi.isChecked())
        		{
        			try 
        			{
                		Log.i(TAG, "mBtIndi set to be enabled");
        				mHrmService.setLeData(device1, hrmUUID,BluetoothGatt.OPERATION_ENABLE_INDICATION, data, 2);
        			} 
        			catch (RemoteException e) 
        			{
        				Log.e(TAG, "mBtIndi", e);
        			}
        		}
        		else
        		{
        			try 
        			{
                		Log.i(TAG, "mBtIndi set to be disabled");
        				mHrmService.setLeData(device1, hrmUUID, BluetoothGatt.OPERATION_DISABLE_INDICATION, data, 2);
        			} 
        			catch (RemoteException e) 
        			{
        				Log.e(TAG, "mBtIndi", e);
        			}                    
        		}
        	}
        });
        
        mCbNoti = (CheckBox) findViewById(R.id.m_cb_noti);
        mCbNoti.setOnClickListener(new OnClickListener() 
        {
        	public void onClick(View v) 
        	{
        		Log.i(TAG, "mBtNoti");
        		if(mCbNoti.isChecked())
        		{
        			mLogArrayAdapter.add("notification enabled");
        			
        			try 
        			{
        				Log.i(TAG, "mBtNoti set to be enabled");
        				mHrmService.setLeData(device1, hrmUUID,BluetoothGatt.OPERATION_ENABLE_NOTIFICATION, data, 2);
        			} 
        			catch (RemoteException e) 
        			{
        				Log.e(TAG, "mBtNoti", e);
        			}
        			
        		}
        		else
        		{
        			mLogArrayAdapter.add("notification disabled");

        			
        			try {
        				Log.i(TAG, "mBtNoti set to be disabled");
        				mHrmService.setLeData(device1, hrmUUID,BluetoothGatt.OPERATION_DISABLE_NOTIFICATION, data, 2);
        				}
        			catch (RemoteException e) 
        			{
        				Log.e(TAG, "mBtNoti", e);
        			}

        		}
        	}
        });
        
        mBtSenLoc.setOnClickListener(new View.OnClickListener() 
        {
            public void onClick(View view) 
            {
                Log.i(TAG, "read sensor location");

                try 
                {
                	mHrmService.getLeData(device1, hrmUUID,	BluetoothGatt.OPERATION_READ_SENSOR_LOCATION);
                } catch (RemoteException e) 
                {
                    Log.e(TAG, "pull sensor location ", e);
                }

            }

        });       
        
       filter_scan = new IntentFilter(BluetoothGatt.CONNECT_COMPLETE);
 	   filter_scan.addAction(BluetoothGatt.DISCONNECT_COMPLETE);
 	   filter_scan.addAction(BluetoothGatt.GET_COMPLETE);
 	   filter_scan.addAction(BluetoothGatt.SET_COMPLETE);

 	   registerReceiver(mConn_Receiver, filter_scan);  
 	   flag_leRcvrReg = true;
 	   
 	   Intent intent1 = new Intent(IBluetoothHrm.class.getName());
 	   getApplicationContext().bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);
 	   mLogArrayAdapter.add("Request IBluetoothHrm service binding...");
 	   Log.d(TAG, "Request IBluetoothHrm service binding...");

 	   Intent intent2 = new Intent(BluetoothGatt.ACTION_START_LE);
 	   intent2.putExtra(BluetoothGatt.EXTRA_PRIMARY_UUID,hrmUUID);//HRM service UUID
 	   sendBroadcast(intent2);
      
 	   callback1 = new callback(hrmUUID);
 	   mContext = this.getApplicationContext(); 
    }
   }

    
    @Override
    protected void onStart() 
    {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart()");
        
        if (!mBluetoothAdapter.isEnabled()) 
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } 
        else 
        {
            // TODO: Set up BT HR Monitor
        }        
    }
    
    @Override
    protected void onResume() 
    {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume()");
        // TODO: If BT HR Monitor is setup but not started, then start it.
    }
    
    @Override
    protected void onPause() 
    {
    	
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause()");
    }
    
    @Override
    protected void onStop() 
    {	
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop()");
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        if (DEBUG) Log.i(TAG, "onDestroy()");
    	
        if(!leDisconn){
        	if(mLeState == CONNECTED)
        	{
        		if (device1 != null) 
        		{
        			mLeState = DISCONNECTING;
        			Log.i(TAG, "disconnecting LE");
        			try 
        			{
        				mHrmService.disconnectLe(device1, hrmUUID);
        			} 
        			catch (RemoteException e) 
        			{
        				Log.e(TAG, "", e);
        				mLeState = DISCONNECTED;
        			}
        		}
        	}
        	leDisconn = true;
        }
        if(flag_leRcvrReg)
        {
        	flag_leRcvrReg = false;
        	unregisterReceiver(mConn_Receiver);
        }
    	
    	if (mHrmService != null) {
    		Log.i(TAG, "unbinding service");
    		getApplicationContext().unbindService(mConnection);
    	}
    	
    	if(mConnection != null)mConnection = null;
    	if(callback1 != null)callback1 = null;
    	if(mHrmService != null)mHrmService = null;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        if (DEBUG) Log.i(TAG, "onActivityResult() code: " + resultCode);
        switch (requestCode) 
        {
	        case REQUEST_ENABLE_BT :
	            if (resultCode == Activity.RESULT_OK) 
	            {
	               // TODO: Set up BT HR Monitor
	            }
	            else 
	            {
	                Log.e(TAG, "onActivityResult(): BT not enabled");
	                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
	                finish();
	            }
	            break;
        }
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_conn_dev2:

    		showPairedDeviceSelectDialog();
    		return true;

		case R.id.menu_exit:
			if(!leDisconn){
			   	if(mLeState == CONNECTED){
			   	     if (device1 != null) {
			   	    	 mLeState = DISCONNECTING;
			                Log.i(TAG, "disconnecting LE");
			                try 
			                {
			                	mHrmService.disconnectLe(device1, hrmUUID);			     
			                } 
			                catch (RemoteException e) 
			                {
			                    mLeState = DISCONNECTED;
			                }
			   	     }
			   	}

				leDisconn = true;
			}
			finish();			
			return true;
        }
        return false;
    }
 
	

    private void showPairedDeviceSelectDialog() 
    {
    	
    	   OnItemClickListener mPairedListClickListener = new OnItemClickListener() 
    	   {
               public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) 
               {
//       			audioAlert();
            	   mBluetoothAdapter.cancelDiscovery();
            	   //String devName = mPairedDevicesArrayAdapter.getItem(0);
            	   
                   String info = ((TextView) v).getText().toString();
                   Log.d("BTBioActivity", "the total length is " + info.length() );
                   
                   //String deviceAddress = info.substring(info.length() - 19);
                   String deviceAddress = getDeviceAddressFromDeviceInfo(info);
                   
                   device1 = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                   
                   //mLogArrayAdapter.add("device1 is "+device1.toString()+ " " + device1.getName().toString()+" "+device1.getAddress().toString());

                   if(device1!=null){
                       
                	   ((DialogInterface) mDialog).cancel();

                	   mLogArrayAdapter.add("device: "+device1);
                               	   
                	   mTvDevice2.setText(device1.getName().toString() );
                	
                	   try {
                	
                	       //int status = mHrmService.connectLe(device1, hrmUUID,  callback1);
                		   
                		   int status = mHrmService.connectLe(device1, "0000180d00001000800000805f9b34fb", callback1);
                		   if (status == BluetoothGatt.SUCCESS){
                			   mLogArrayAdapter.add("connectLe sent out succesfully.");
                        		mLeState = CONNECTING;

                		   }else{
                			   mLogArrayAdapter.add("connectLe sent out but failed.");
                		   }
                	   } catch (RemoteException e) {
                		   // TODO Auto-generated catch block
                		   e.printStackTrace();
                	   }
                   }
 
               }

			private String getDeviceAddressFromDeviceInfo(String info) {
				return info.split("\n")[1];
			}       
           };
    	
    	
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View layout_listview = inflater.inflate(R.layout.device_list1, (ViewGroup) findViewById(R.id.root_device_list));

		alert_paired = new AlertDialog.Builder(this);
		alert_paired.setView(layout_listview);
		
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        ListView pairedListView = (ListView)layout_listview.findViewById(R.id.paired_devices);
        mPairedDevicesArrayAdapter.clear();
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mPairedListClickListener);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
 
        if (pairedDevices.size() > 0) {
        	boolean foundDevices = false;
        	
        	if (pairedDevices.size() > 0) {
        	
            	//foundDevices = false;
            	for (BluetoothDevice device : pairedDevices) {
            		mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            		foundDevices = true;
            		/*
                	for (String s : IMonitors.MonitorNamesPatternLE) {
                		Pattern p_le = Pattern.compile(s);
                		if (device.getName().matches(p_le.pattern())) {
                			mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                			foundDevices = true;
                		}
                	} */
                }
        	}
            	
        	
            if (foundDevices) {
            	layout_listview.findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            } else {
            	layout_listview.findViewById(R.id.title_paired_devices).setVisibility(View.GONE);
                String noDevices = getResources().getText(R.string.none_paired).toString();
                mPairedDevicesArrayAdapter.add(noDevices);            	
            }
        } else {
        	layout_listview.findViewById(R.id.title_paired_devices).setVisibility(View.GONE);
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }

        alert_paired.setTitle("Select paired device for LE connect");
         mDialog = alert_paired.show();
    }

    public void connectDialog(){
		aDialog=ProgressDialog.show(this,"BT LE Connecting...","...");

    }
    
    private class callback extends IBluetoothHrmCallback.Stub {
    	private String service;
        callback(String serv) 
        {
        	 service = serv;
        }

        public void indicationLeCb(BluetoothDevice device, String service, int length, byte[] data) 
        {
        	Log.i(TAG,"indicationLeCb" );
        	parseData(length , data);
        }

        public void notificationLeCb(BluetoothDevice device, String service, int length,
        		byte[] data) 
        {
        	Log.i(TAG,"notificationLeCb" );
        	parseData(length , data);
        }
    }
    
    private final BroadcastReceiver mConn_Receiver = new BroadcastReceiver() 
    {

		@Override
		public void onReceive(Context context, Intent intent) 
		{
			// TODO Auto-generated method stub
			String action = intent.getAction();
			mLogArrayAdapter.add("some broadcast, action is: "+action);
			if (BluetoothGatt.CONNECT_COMPLETE.equals(action)) 
			{
				mLogArrayAdapter.add("LE connection complete - ");

				int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
				String service = intent.getStringExtra("uuid");// Todo
				if (status == BluetoothGatt.SUCCESS) 
				{
					mLogArrayAdapter.add("Connected successfully ! Service: "+service);	
					
					mTvState2.setText("  Connected  ");
					
					leDisconn = false;

					mLeState = CONNECTED;
							                
				} 
				else if (status != BluetoothGatt.SUCCESS) 
					 {	                    
					 	mLogArrayAdapter.add("Connection failed. Service: "+service);
						
						mTvState2.setText("  Not Connected  ");
						mLeState = DISCONNECTED;
					 }
			}
			else if (action.equals(BluetoothGatt.DISCONNECT_COMPLETE)) 
				{
	             	int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
	                intent.getStringExtra("uuid");
	                if (status == BluetoothGatt.SUCCESS) 
	                {
	                    mLeState = DISCONNECTED;
	                    device1 = null;
						mTvState2.setText("  Not Connected  ");
	                } 
	                else if (status != BluetoothGatt.SUCCESS) 
		                {
		                	mLeState = DISCONNECTED;
		                    device1 = null;
							mTvState2.setText("  Not Connected  ");
		                }
	
				}
				else if ((BluetoothGatt.GET_COMPLETE).equals(action)) 
					{
		                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
		                String service = intent.getStringExtra("uuid");// Todo
		                int length = intent.getIntExtra("length", 0);
		                if(status == BluetoothGatt.SUCCESS)
		                {
		                	if ((length >= 0) && service.equalsIgnoreCase(hrmUUID)) 
		                	{
		                		byte[] data = new byte[length];
		                		data = intent.getByteArrayExtra("data");
		                		mSensorLocation = data[0];
		                		Log.v(TAG, "onReceive GET_COMPLETE data first byte " + data[0]);
		                		
		                		mLogArrayAdapter.add("Sensor Location returned by GET_COMPLETE: "+getStringSensorLocation(mSensorLocation));
		                		mUIUpdateHandler.sendEmptyMessage(0);
		
		                		updateUI();                	
		            		}
		                }
		                else
		                {
		        			Toast.makeText(mContext, "Sensor query failed! ", Toast.LENGTH_LONG).show();
		                }
		            }
					else if (action.equals(BluetoothGatt.SET_COMPLETE)) 
						{
							mLogArrayAdapter.add("SET COMPLETE received, action is: "+action);
			        		Log.e(TAG, "SET COMPLETE received: " + action);
			
			                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
			                String service = intent.getStringExtra("uuid");// Todo
			                if (status == BluetoothGatt.SUCCESS && service.equalsIgnoreCase(hrmUUID)) 
			                {
			                }
			                else if(status != BluetoothGatt.SUCCESS && service.equalsIgnoreCase(hrmUUID))
				                {
				        			Toast.makeText(mContext, "Notification enabling failed! ", Toast.LENGTH_LONG).show();
				                } 
			            }
		}
	};
	
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
	
	private void updateUI()
	{
		Log.v(TAG, "updateUI parse han");
		if(mHeartBeatsPerMinute!= previous_mHeartBeatsPerMinute)
		{
			Log.d(TAG, mHeartBeatsPerMinute + " bpm");
			mTvHR2.setText( " "+mHeartBeatsPerMinute + " bpm");
			previous_mHeartBeatsPerMinute = mHeartBeatsPerMinute;
		}
			mTvSenLoc.setText(getStringSensorLocation(mSensorLocation));
	}
	
	public String getStringSensorLocation(int intLocation)
	{
		String strLocation = null;
		switch(intLocation)
		{
			case 0:
				strLocation = "Other";
				break;
			case 1:
				strLocation = "Chest";
				break;
			case 2:
				strLocation = "Wrist";
				break;
			case 3:
				strLocation = "Finger";
				break;
			case 4:
				strLocation = "Hand";
				break;
			case 5:
				strLocation = "Ear Lobe";
				break;
			case 6:
				strLocation = "Foot";
				break;
			case 100:
				strLocation = "No Skin Contact";
			default:
				strLocation = "Unknown";
				break;
		}
		Log.v(TAG,"getStringSensorLocation" + strLocation);
		return strLocation;
	}
}