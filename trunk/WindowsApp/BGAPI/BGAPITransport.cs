using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Threading;
using System.IO;
using System.IO.Ports;

namespace BGAPI
{
    public class BGAPITransport
    {

        protected internal List<BGAPITransportListener> listeners = new List<BGAPITransportListener>();
        public virtual void addListener(BGAPITransportListener l)
        {
            listeners.Add(l);
        }
        public virtual void removeListener(BGAPITransportListener l)
        {
            listeners.Remove(l);
        }

        protected internal SerialPort port;

        private long receivedBytes = 0;

        public virtual long ReceivedBytes
        {
            get
            {
                return receivedBytes;
            }
        }

        public BGAPITransport(SerialPort port)
        {
            this.port = port;
            Thread thr = new Thread(new ThreadStart(Run));
            port.Open();
            thr.Start();
        }

        public virtual void Run()
        {

            byte[] buffer = new byte[1024];
            byte[] hdr = new byte[HEADER_SIZE];
            int len = -1;
            int idx = 0;
            int state = WAITING;
            BGAPIPacket p = null;


            try
            {
                while (!terminate && ((len = port.Read(buffer, 0, 1024)) > -1))
                {
                    receivedBytes += len;

                    for (int i = 0; i < len; i++)
                    {
                        byte c = buffer[i];
                        if (state == WAITING)
                        {
                            idx = 0;
                            state = HEADER;
                            hdr[idx++] = c;
                        }
                        else if (state == HEADER)
                        {
                            hdr[idx++] = c;
                            if (idx == HEADER_SIZE) // We got the whole header
                            {
                                p = new BGAPIPacket(hdr);

                                if (p.PayloadLength > 0) // there is a payload
                                {
                                    state = PAYLOAD;
                                    idx = 0;
                                }
                                else // There is no payload
                                {
                                    state = WAITING;
                                    foreach (BGAPITransportListener l in listeners)
                                        l.packetReceived(p);
                                    p = null;
                                }
                            }
                        }
                        else if (state == PAYLOAD)
                        {
                            p.PayloadData.WriteByte((byte)c);
                            idx++;
                            if (idx == p.PayloadLength) // We got a complete message
                            {
                                state = WAITING;
                                foreach (BGAPITransportListener l in listeners)
                                    l.packetReceived(p);
                                p = null;
                            }
                        }
                    }
                }
            }
            catch (IOException e)
            {
                //e.printStackTrace();
            }
            port.Close();
        }

        public virtual void sendPacket(BGAPIPacket p)
        {
            try
            {
                //port.Open();
                port.Write(p.PacketBytes, 0, p.PacketBytes.Length);
                foreach (BGAPITransportListener l in listeners)
                    l.packetSent(p);
            }
            catch (IOException ex)
            {
                //Logger.getLogger(typeof(BGAPITransport).Name).log(Level.SEVERE, null, ex);
            }
        }



        ///    <summary> ************************************************************************
        ///     * CODE OF THE RECEIVER THREAD </summary>
        ///     ************************************************************************

        public virtual void stop()
        {
            terminate = true;
            this.port.Close();
        }

        private bool terminate = false;

        private const int WAITING = 0;
        private const int HEADER = 1;
        private const int PAYLOAD = 2;

        private const int HEADER_SIZE = 4;
    }
}
