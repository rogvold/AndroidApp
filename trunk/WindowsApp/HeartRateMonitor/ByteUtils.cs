using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor
{
    public class ByteUtils
    {

        public static byte[] bytesFromString(string bytes)
        {
            string[] bs = bytes.Split(' ');
            byte[] result = new byte[bs.Length];
            for (int i = 0; i < bs.Length; i++)
            {
                int b = Convert.ToInt32(bs[i]);
                result[i] = (byte)b;
            }
            return result;
        }

        public static string bytesToString(byte[] bytes)
        {
            StringBuilder result = new StringBuilder();
            foreach (byte b in bytes)
                result.Append((b & 0xFF).ToString("X") + " ");
            return result.ToString();
        }
    }
}
