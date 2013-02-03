using System.Globalization;
using System.Text;

namespace BLELib
{
    public class ByteUtils
    {
        public static byte[] BytesFromString(string bytes)
        {
            string[] bs = bytes.Split(' ');
            var result = new byte[bs.Length];
            for (int i = 0; i < bs.Length - 1; i++)
            {
                int b = int.Parse(bs[i], NumberStyles.HexNumber);
                result[i] = (byte) b;
            }
            return result;
        }

        public static string BytesToString(byte[] bytes)
        {
            var result = new StringBuilder();
            foreach (byte b in bytes)
                result.Append((b & 0xFF).ToString("X") + " ");
            return result.ToString();
        }
    }
}