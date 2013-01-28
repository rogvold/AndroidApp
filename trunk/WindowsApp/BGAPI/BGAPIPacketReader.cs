using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace BGAPI
{
    public class BGAPIPacketReader
    {
        private byte[] data;
		private int index;

		public BGAPIPacketReader(byte[] data)
		{
			this.data = data;
		}

		public virtual void reset()
		{
			index = 0;
		}

		public virtual int Length
		{
            get 
            {
                return data.Length;
            }
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
			return (int)data[index++] - 256;
		}

		public virtual int r_uint8()
		{
			return (int)next_uint();
		}

		public virtual int r_uint16()
		{
			int result = (int)next_uint();
			result += (int)(next_uint()<<8);
			return result;
		}
		 public virtual int r_int16()
		 {
			int result = (int)next_uint();
			result += (int)(next_uint()<<8);
			return result;
		}


		public virtual int r_uint32()
		{
			int result = (int)next_uint();
			result += (int)(next_uint()<<8);
			result += (int)(next_uint()<<16);
			result += (int)(next_uint()<<24);
			return result;
		}

		public virtual byte[] r_uint8array()
		{
			byte[] result = new byte[next_uint()];
			for (int i=0; i<result.Length; i++)
			{
				result[i] = data[index++];
			}
			return result;
		}

		public virtual BDAddr r_bd_addr()
		{
			byte[] addr = new byte[6];
			for (int i=0; i<6; i++)
			{
				addr[i] = data[index++];
			}
			return new BDAddr(addr);
		}
    }
}
