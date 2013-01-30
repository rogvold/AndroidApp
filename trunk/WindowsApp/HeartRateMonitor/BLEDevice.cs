using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor
{
    public class BLEDevice
    {

        protected internal string address;
        protected internal string name;
        protected internal int rssi;

        protected internal Dictionary<string, BLEService> services = new Dictionary<string, BLEService>();

        public virtual Dictionary<string, BLEService> Services
        {
            get
            {
                return services;
            }
        }

        public virtual string GATTDescription
        {
            get
            {
                string result = ToString();
                foreach (BLEService s in services.Values)
                {
                    result += "\n" + s.Description;
                }
                return result;
            }
        }

        public BLEDevice(string address)
        {
            this.address = address;
            name = "";
        }

        public virtual string Address
        {
            get
            {
                return address;
            }
        }

        public virtual string Name
        {
            get
            {
                return name;
            }
            set
            {
                this.name = value;
            }
        }

        public virtual int Rssi
        {
            get
            {
                return rssi;
            }
            set
            {
                this.rssi = value;
            }
        }

        public override string ToString()
        {
            return name + " [" + address + "]";
        }

        public virtual string bytesToString(byte[] bytes)
        {
            StringBuilder result = new StringBuilder();
            result.Append("[ ");
            foreach (byte b in bytes)
                result.Append((b & 0xFF).ToString("X") + " ");
            result.Append("]");
            return result.ToString();
        }
    }
}
