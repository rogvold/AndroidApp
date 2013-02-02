using System;
using System.Collections.Generic;

namespace BLELib
{
    public class BLEDeviceList
    {

        private List<BLEDevice> devices = new List<BLEDevice>();

        public List<BLEDevice> Devices
        {
            get
            {
                return this.devices;
            }
            set
            {
                this.devices = value;
            }
        }

        public virtual void Clear()
        {
            int idx = devices.Count - 1;
            if (idx < 0)
                return;
            devices.Clear();
        }

        public virtual void Add(BLEDevice d)
        {
            devices.Add(d);
        }

        public virtual void Changed(BLEDevice d)
        {
            if (devices.Count == 0)
                return;
        }

        public virtual BLEDevice GetFromAddress(string address)
        {
            foreach (BLEDevice d in devices)
            {
                if (d.address.Equals(address))
                    return d;
            }
            return null;
        }

        public virtual int Count
        {
            get
            {
                return devices.Count;
            }
        }

        public virtual Object this[int index]
        {
            get
            {
                return devices[index];
            }
        }
    }
}
