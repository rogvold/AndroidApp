package com.example.hello.world;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BtleAdapter
{
  public static final int ICS_VERSION = 14;
  private BluetoothAdapter btAdapter = null;
  private BluetoothConnectedListener connectedListener = null;
  private BroadcastReceiver connectionBroadcastReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context paramContext, Intent paramIntent)
    {
      switch (paramIntent.getIntExtra("android.bluetooth.adapter.extra.STATE", 10))
      {
      case 11:
      default:
      case 10:
      case 13:
      case 12:
      }
      while (true)
      {
        //return;
        if ((BtleAdapter.this.isConnected) && (BtleAdapter.this.connectedListener != null))
          BtleAdapter.this.connectedListener.connectionChange(false);
        //BtleAdapter.access$002(BtleAdapter.this, false);
        //continue;
        //if ((!BtleAdapter.this.isConnected) && (BtleAdapter.this.connectedListener != null))
        // BtleAdapter.this.connectedListener.connectionChange(true);
        //BtleAdapter.access$002(BtleAdapter.this, true);
      }
    }
  };
  private Context context = null;
  private boolean isConnected = false;
  private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context paramContext, Intent paramIntent)
    {
      String str = paramIntent.getAction();
      if ("android.bluetooth.device.action.FOUND".equals(str))
      {
        BluetoothDevice localBluetoothDevice = (BluetoothDevice)paramIntent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if (BtleAdapter.this.isBtleDevice(localBluetoothDevice))
        {
          BtleDescriptor localBtleDescriptor = new BtleDescriptor();
          localBtleDescriptor.device = localBluetoothDevice;
          localBtleDescriptor.state = BtleAdapter.this.getState(localBluetoothDevice);
          localBtleDescriptor.name = paramIntent.getStringExtra("android.bluetooth.device.extra.NAME");
          
          Log.i("scanBroadcastReceiver onReceive", "Device " + localBtleDescriptor.name + " found with address " + localBtleDescriptor.device.getAddress());
          if (BtleAdapter.this.scanListener != null)
            BtleAdapter.this.scanListener.deviceDetected(localBtleDescriptor);
        }
      }
      if ("android.bluetooth.adapter.action.DISCOVERY_STARTED".equals(str))
      {
    	  Log.i("scanBroadcastReceiver onReceive", "Scan started");
    	  BtleAdapter.this.scanListener.scanStarted();
      }
      if (!"android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(str))
      {
    	  Log.i("scanBroadcastReceiver onReceive", "Scan finished");
    	  if (BtleAdapter.this.scanListener != null)
			  BtleAdapter.this.scanListener.scanFinished();
    	  BtleAdapter.this.cancelScanForDevices();
      }   
    }
  };
  private ScanListener scanListener = null;

  public BtleAdapter(Context paramContext)
  {
    this.context = paramContext.getApplicationContext();
    this.btAdapter = BluetoothAdapter.getDefaultAdapter();
    this.isConnected = this.btAdapter.isEnabled();
    registerConnectionReceiver();
  }

  private String getName(BluetoothDevice paramBluetoothDevice)
  {
    String str = paramBluetoothDevice.getName();
    return str;
  }

  private BtleDescriptor.BluetoothState getState(BluetoothDevice paramBluetoothDevice)
  {
    BtleDescriptor.BluetoothState localBluetoothState;
    switch (paramBluetoothDevice.getBondState())
    {
    default:
      localBluetoothState = BtleDescriptor.BluetoothState.UNKNOWN;
    case 10:
      localBluetoothState = BtleDescriptor.BluetoothState.NOT_PAIRED;
    case 12:
      localBluetoothState = BtleDescriptor.BluetoothState.PAIRED;
    }
    return localBluetoothState;
  }

  private boolean isBtleDevice(BluetoothDevice paramBluetoothDevice)
  {
    //TODO разобраться с тем, что выводить в этом методе
    int i = 1;
    BluetoothClass localBluetoothClass = paramBluetoothDevice.getBluetoothClass();
    if (isIcsVersion())
      if (localBluetoothClass == null)
      	i = 0;
    return true;
  }

  public static final boolean isIcsVersion()
  {
    if (Build.VERSION.SDK_INT >= 14)
    	return true;
    return false;
  }

  private void registerConnectionReceiver()
  {
    this.context.registerReceiver(this.connectionBroadcastReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
  }

  private void unregisterConnectionReceiver()
  {
    try
    {
      this.context.unregisterReceiver(this.connectionBroadcastReceiver);
      return;
    }
    catch (Exception localException)
    {
      while (true)
        Log.w("unregisterConnectionReceiver", localException);
    }
  }

  public void cancelScanForDevices()
  {
    if (this.btAdapter.isDiscovering())
    {
      this.scanListener = null;
      this.btAdapter.cancelDiscovery();
      unregisterScanReceiver();
    }
  }

  public void destroy()
  {
    unregisterScanReceiver();
    unregisterConnectionReceiver();
  }

  public BtleDescriptor findDevice(String paramString)
  {
    BtleDescriptor localBtleDescriptor = null;
    Iterator localIterator = this.btAdapter.getBondedDevices().iterator();
    while (localIterator.hasNext())
    {
      BluetoothDevice localBluetoothDevice = (BluetoothDevice)localIterator.next();
      if ((!isBtleDevice(localBluetoothDevice)) || (!paramString.equalsIgnoreCase(localBluetoothDevice.getAddress())))
        continue;
      localBtleDescriptor = new BtleDescriptor();
      localBtleDescriptor.device = localBluetoothDevice;
      localBtleDescriptor.name = getName(localBluetoothDevice);
    }
    return localBtleDescriptor;
  }

  public List<BtleDescriptor> getPairedDevices()
  {
    ArrayList localArrayList = new ArrayList();
    Iterator localIterator = this.btAdapter.getBondedDevices().iterator();
    while (localIterator.hasNext())
    {
      BluetoothDevice localBluetoothDevice = (BluetoothDevice)localIterator.next();
      if (!isBtleDevice(localBluetoothDevice))
        continue;
      BtleDescriptor localBtleDescriptor = new BtleDescriptor();
      localBtleDescriptor.device = localBluetoothDevice;
      localBtleDescriptor.name = getName(localBluetoothDevice);
      if (localArrayList.contains(localBtleDescriptor))
        continue;
      localArrayList.add(localBtleDescriptor);
    }
    return localArrayList;
  }

  public boolean isConnected()
  {
    return this.isConnected;
  }

  public void listenerForBluetoothConnectionChanges(BluetoothConnectedListener paramBluetoothConnectedListener)
  {
    this.connectedListener = paramBluetoothConnectedListener;
  }

  public void registerScanReceiver()
  {
    this.context.registerReceiver(this.scanBroadcastReceiver, new IntentFilter("android.bluetooth.device.action.FOUND"));
    this.context.registerReceiver(this.scanBroadcastReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_STARTED"));
    this.context.registerReceiver(this.scanBroadcastReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED"));
  }

  public void scanForDevices(ScanListener paramScanListener)
  {
    if (!this.btAdapter.isDiscovering())
    {
      registerScanReceiver();
      this.btAdapter.startDiscovery();
      this.scanListener = paramScanListener;
    }
  }

  public void unregisterScanReceiver()
  {
    try
    {
      this.context.unregisterReceiver(this.scanBroadcastReceiver);
      return;
    }
    catch (Exception localException)
    {
      while (true)
        Log.w("UnregisteredScanReceiver", localException);
    }
  }

  public static abstract interface BluetoothConnectedListener
  {
    public abstract void connectionChange(boolean paramBoolean);
  }

  public static abstract interface ScanListener
  {
    public abstract void deviceDetected(BtleDescriptor paramBtleDescriptor);
    public abstract void scanFinished();
    public abstract void scanStarted();
  }
}