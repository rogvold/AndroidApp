using System.IO;
using System.Text;

namespace BGAPI
{
    public class BGAPIPacket
    {
        protected internal int classID;
        protected internal int commandID;
        protected internal MemoryStream data = new MemoryStream();
        protected internal int msgType;
        protected internal long payloadLength = -1;

        public BGAPIPacket(byte[] header)
        {
            msgType = (header[0] & 0xFF) >> 7;
            payloadLength = ((header[0] & 0x07) << 8) + header[1];
            classID = header[2];
            commandID = header[3];
        }

        public BGAPIPacket(int msg_type, int classID, int commandID)
        {
            msgType = msg_type;
            this.classID = classID;
            this.commandID = commandID;
        }

        public virtual int ClassID
        {
            get { return classID; }
        }

        public virtual int CommandID
        {
            get { return commandID; }
        }

        public virtual int MsgType
        {
            get { return msgType; }
        }

        public virtual long PayloadLength
        {
            get { return payloadLength; }
        }

        public virtual MemoryStream PayloadData
        {
            get { return data; }
        }

        public virtual BGAPIPacketReader PayloadReader
        {
            get { return new BGAPIPacketReader(data.ToArray()); }
        }

        public virtual byte[] PacketBytes
        {
            get
            {
                var res = new MemoryStream();
                var result = new BinaryWriter(res);
                payloadLength = data.Length;
                result.Write((byte) ((msgType << 7) + (payloadLength >> 8)));
                result.Write((byte) (payloadLength & 0xFF));
                result.Write((byte) (classID & 0xFF));
                result.Write((byte) (commandID & 0xFF));
                try
                {
                    result.Write(data.ToArray());
                }
                catch (IOException ex)
                {
                    //Logger.getLogger(typeof(BGAPIPacket).Name).log(Level.SEVERE, null, ex);
                }
                byte[] resArray = res.ToArray();
                res.Close();
                return resArray;
            }
        }

        public override string ToString()
        {
            var result = new StringBuilder();
            result.Append("< typ=" + msgType + " cla=" + classID + " cmd=" + commandID + " len=" + payloadLength + " ");
            if (data.Length > 0)
            {
                byte[] bytes = data.ToArray();
                result.Append("[ ");
                foreach (byte b in bytes)
                    result.Append(((b & 0xFF)).ToString("X") + " ");
                result.Append("] ");
            }
            result.Append(">");
            return result.ToString();
        }

        public virtual void w_uint8(int v)
        {
            data.WriteByte((byte) (v & 0xFF));
        }

        public virtual void w_int8(int v)
        {
            data.WriteByte((byte) v);
        }


        public virtual void w_uint16(int v)
        {
            data.WriteByte((byte) (v & 0xFF));
            data.WriteByte((byte) ((v >> 8) & 0xFF));
        }

        public virtual void w_int16(int v)
        {
            data.WriteByte((byte) (v & 0xFF));
            data.WriteByte((byte) ((v >> 8) & 0xFF));
        }

        public virtual void w_uint32(int v)
        {
            data.WriteByte((byte) (v & 0xFF));
            data.WriteByte((byte) ((v >> 8) & 0xFF));
            data.WriteByte((byte) ((v >> 16) & 0xFF));
            data.WriteByte((byte) ((v >> 24) & 0xFF));
        }

        public virtual void w_uint8array(byte[] bytes)
        {
            data.WriteByte((byte) bytes.Length);
            for (int i = 0; i < bytes.Length; i++)
            {
                data.WriteByte(bytes[i]);
            }
        }

        public virtual void w_bd_addr(BDAddr addr)
        {
            byte[] bytes = addr.ByteAddr;
            for (int i = 0; i < bytes.Length; i++)
            {
                data.WriteByte(bytes[i]);
            }
        }
    }
}