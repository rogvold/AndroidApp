package com.mipt.razrhrmapp;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.text.InputFilter.LengthFilter;
import android.view.Menu;
import android.widget.Toast;

public class HrmActivity extends Activity {

    private Context mContext;
	private boolean ifPhoneSupportsBLE;
	private int[] data;
	private BluetoothAdapter mBluetoothAdapter;
	private static final String className = "android.bluetooth.BluetoothGattService";
	public static final ParcelUuid HRM = ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    

	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrm);
        
        mContext = this.getApplicationContext();
        
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_hrm, menu);
        return true;
    }
}
