using System;
using System.Collections.Generic;
using System.Linq;

namespace BLELib
{
    public class BLEDeviceList
    {
        private List<BLEDevice> _devices = new List<BLEDevice>();

        public List<BLEDevice> Devices
        {
            get { return _devices; }
            set { _devices = value; }
        }

        public virtual int Count
        {
            get { return _devices.Count; }
        }

        public virtual Object this[int index]
        {
            get { return _devices[index]; }
        }

        public virtual void Clear()
        {
            int idx = _devices.Count - 1;
            if (idx < 0)
                return;
            _devices.Clear();
        }

        public virtual void Add(BLEDevice d)
        {
            _devices.Add(d);
        }

        public virtual void Changed(BLEDevice d)
        {
            if (_devices.Count == 0)
                return;
        }

        public virtual BLEDevice GetFromAddress(string address)
        {
            return _devices.FirstOrDefault(d => d.address.Equals(address));
        }
    }
}