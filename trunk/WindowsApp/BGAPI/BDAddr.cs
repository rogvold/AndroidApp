﻿using System;
using System.Text;

namespace BGAPI
{
    public class BDAddr
    {
        protected internal byte[] byte_addr;

        public BDAddr(byte[] addr)
        {
            byte_addr = addr;
        }

        public virtual byte[] ByteAddr
        {
            get { return byte_addr; }
        }

        public static BDAddr fromString(string addr)
        {
            string[] bytes = addr.Split(':');
            if (bytes.Length != 6)
            {
                throw new Exception("Invalid Bluetooth address format.");
            }
            var byte_addr = new byte[6];
            for (int i = 0; i < 6; i++)
            {
                byte_addr[5 - i] = (byte) Convert.ToInt32(bytes[i], 16);
            }
            return new BDAddr(byte_addr);
        }

        public override string ToString()
        {
            var result = new StringBuilder();
            for (int i = 0; i < byte_addr.Length; i++)
            {
                result.Append((byte_addr[5 - i] & 0xFF).ToString("X"));
                if (i < byte_addr.Length - 1)
                    result.Append(":");
            }
            return result.ToString();
        }
    }
}