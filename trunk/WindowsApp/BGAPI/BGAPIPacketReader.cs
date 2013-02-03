namespace BGAPI
{
    public class BGAPIPacketReader
    {
        private readonly byte[] data;
        private int index;

        public BGAPIPacketReader(byte[] data)
        {
            this.data = data;
        }

        public virtual int Length
        {
            get { return data.Length; }
        }

        public virtual void reset()
        {
            index = 0;
        }

        public virtual int bytesLeft()
        {
            return data.Length - index;
        }

        private int next_uint()
        {
            return data[index++] & 0xFF;
        }

        public virtual int r_int8()
        {
            return data[index++] - 256;
        }

        public virtual int r_uint8()
        {
            return next_uint();
        }

        public virtual int r_uint16()
        {
            int result = next_uint();
            result += (next_uint() << 8);
            return result;
        }

        public virtual int r_int16()
        {
            int result = next_uint();
            result += (next_uint() << 8);
            return result;
        }


        public virtual int r_uint32()
        {
            int result = next_uint();
            result += (next_uint() << 8);
            result += (next_uint() << 16);
            result += (next_uint() << 24);
            return result;
        }

        public virtual byte[] r_uint8array()
        {
            var result = new byte[next_uint()];
            for (int i = 0; i < result.Length; i++)
            {
                result[i] = data[index++];
            }
            return result;
        }

        public virtual BDAddr r_bd_addr()
        {
            var addr = new byte[6];
            for (int i = 0; i < 6; i++)
            {
                addr[i] = data[index++];
            }
            return new BDAddr(addr);
        }
    }
}