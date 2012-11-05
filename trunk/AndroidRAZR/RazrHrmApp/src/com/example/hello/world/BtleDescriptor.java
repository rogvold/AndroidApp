package com.example.hello.world;

import android.bluetooth.BluetoothDevice;

public class BtleDescriptor
{
  public BluetoothDevice device = null;
  public String name = null;
  public BluetoothState state = BluetoothState.UNKNOWN;

  /*public boolean equals(Object paramObject)
  {
    boolean bool = false;
    if (paramObject == null);
    while (true)
    {
      return bool;
      if (paramObject == this)
      {
        bool = true;
        continue;
      }
      if (paramObject.getClass() != getClass())
        continue;
      BtleDescriptor localBtleDescriptor = (BtleDescriptor)paramObject;
      if ((this.device == null) && (localBtleDescriptor.device == null))
      {
        bool = true;
        continue;
      }
      if ((this.device == null) || (localBtleDescriptor.device == null))
        continue;
      bool = this.device.getAddress().equalsIgnoreCase(localBtleDescriptor.device.getAddress());
    }
  }*/

  public int hashCode()
  {
    int i = 0;
    if (this.device != null)
      for (String str : this.device.getAddress().split(":"))
        i = i * 256 + Integer.parseInt(str, 16);
    return i;
  }

  public String toString()
  {
    String str;
    if ((this.name != null) && (this.device != null))
      str = this.name + " (" + this.device.getAddress() + ")";
    else return "Not initialized BT Device";
    return str;
  }


  public static enum BluetoothState
  {
	  BLUETOOTH_SETTINGS,
	  UNKNOWN,
	  PAIRED,
      NOT_PAIRED,
      NOT_FOUND,
      REFRESH_LIST 
  }
}