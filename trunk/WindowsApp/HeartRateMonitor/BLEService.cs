using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor
{
    public class BLEService
    {

        public static Dictionary<string, string> profiles = new Dictionary<string, string>();
        static BLEService()
        {
            profiles.Add("0x180A", "");
        }


        protected internal int start, end;
        protected internal byte[] uuid;

        protected internal List<BLEAttribute> attributes = new List<BLEAttribute>();

        public BLEService(byte[] uuid, int start, int end)
        {
            this.uuid = uuid;
            this.start = start;
            this.end = end;
        }

        public virtual int Start
        {
            get
            {
                return start;
            }
        }

        public virtual int End
        {
            get
            {
                return end;
            }
        }

        public virtual byte[] Uuid
        {
            get
            {
                return uuid;
            }
        }

        public virtual List<BLEAttribute> Attributes
        {
            get
            {
                return attributes;
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
            return "BLEService " + UuidString + " (" + start + ".." + end + ")";
        }

        public virtual string Description
        {
            get
            {
                string result = ToString();
                foreach (BLEAttribute a in attributes)
                {
                    result += "\n\t" + a.ToString();
                }
                return result;
            }
        }
    }
}
