using System;
using System.Collections.Generic;
using System.Linq;

namespace BLELib
{
    public class BLEDeviceList
    {
        private readonly List<BLEDevice> _devices = new List<BLEDevice>();

        public IEnumerable<BLEDevice> Devices
        {
            get { return _devices; }
        }

        public Object this[int index]
        {
            get { return _devices[index]; }
        }

        public void Clear()
        {
            int idx = _devices.Count - 1;
            if (idx < 0)
                return;
            _devices.Clear();
        }

        public void Add(BLEDevice d)
        {
            _devices.Add(d);
        }

        public BLEDevice GetFromAddress(string address)
        {
            return _devices.FirstOrDefault(d => d.address.Equals(address));
        }
    }
}