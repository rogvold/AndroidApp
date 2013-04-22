using System.Linq;

namespace BLELib
{
    public class BLEAttribute
    {
        protected internal int handle;
        protected internal byte[] uuid;

        public BLEAttribute(byte[] uuid, int handle)
        {
            this.uuid = uuid;
            this.handle = handle;
        }

        public virtual byte[] Uuid
        {
            get { return uuid; }
        }

        public virtual string UuidString
        {
            get
            {
                string result = uuid.Aggregate("", (current, t) => string.Format("{0:X2}", t) + current);
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