using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HeartRateMonitor
{
    public class BLEAttribute
    {
        protected internal byte[] uuid;
        protected internal int handle;

        public BLEAttribute(byte[] uuid, int handle)
        {
            this.uuid = uuid;
            this.handle = handle;
        }

        public virtual byte[] Uuid
        {
            get
            {
                return uuid;
            }
        }

        public virtual string UuidString
        {
            get
            {
                string result = "";
                for (int i = 0; i < uuid.Length; i++)
                {
                    result = string.Format("{0:X2}", uuid[i]) + result;
                }
                result = "0x" + result;
                return result;
            }
        }

        public override string ToString()
        {
            return "ATT " + UuidString + " => 0x" + handle.ToString("X").ToUpper();
        }
    }
}
