using System.Collections.Generic;
using System.Linq;

namespace BLELib
{
    public class BLEDevice
    {
        protected internal string address;
        protected internal string name;
        protected internal int rssi;

        protected internal Dictionary<string, BLEService> services = new Dictionary<string, BLEService>();

        public BLEDevice(string address)
        {
            this.address = address;
            name = "";
        }

        public virtual Dictionary<string, BLEService> Services
        {
            get { return services; }
        }

        public virtual string GATTDescription
        {
            get
            {
                string result = ToString();
                return services.Values.Aggregate(result, (current, s) => current + ("\n" + s.Description));
            }
        }

        public virtual string Address
        {
            get { return address; }
        }

        public virtual string Name
        {
            get { return name; }
            set { name = value; }
        }

        public virtual int Rssi
        {
            get { return rssi; }
            set { rssi = value; }
        }

        public override string ToString()
        {
            return name + " [" + address + "]";
        }
    }
}