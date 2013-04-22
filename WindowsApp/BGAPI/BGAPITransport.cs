using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Ports;
using System.Threading;

namespace BGAPI
{
    public class BGAPITransport
    {
        private const int WAITING = 0;
        private const int HEADER = 1;
        private const int PAYLOAD = 2;

        private const int HEADER_SIZE = 4;
        protected internal List<IBGAPITransportListener> listeners = new List<IBGAPITransportListener>();

        protected internal SerialPort port;

        private long receivedBytes;
        private bool terminate;

        public BGAPITransport(SerialPort port)
        {
            this.port = port;
            var thr = new Thread(Run);
            try
            {
                port.Open();
                thr.Start();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        public virtual long ReceivedBytes
        {
            get { return receivedBytes; }
        }

        public virtual void addListener(IBGAPITransportListener l)
        {
            listeners.Add(l);
        }

        public virtual void removeListener(IBGAPITransportListener l)
        {
            listeners.Remove(l);
        }

        public virtual void Run()
        {
            var buffer = new byte[1024];
            var hdr = new byte[HEADER_SIZE];
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
                                    foreach (IBGAPITransportListener l in listeners)
                                        l.PacketReceived(p);
                                    p = null;
                                }
                            }
                        }
                        else if (state == PAYLOAD)
                        {
                            p.PayloadData.WriteByte(c);
                            idx++;
                            if (idx == p.PayloadLength) // We got a complete message
                            {
                                state = WAITING;
                                foreach (IBGAPITransportListener l in listeners)
                                    l.PacketReceived(p);
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
            try
            {
                port.Close();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        public virtual void sendPacket(BGAPIPacket p)
        {
            try
            {
                //port.Open();
                port.Write(p.PacketBytes, 0, p.PacketBytes.Length);
                foreach (IBGAPITransportListener l in listeners)
                    l.PacketSent(p);
            }
            catch (IOException ex)
            {
                //Logger.getLogger(typeof(BGAPITransport).Name).log(Level.SEVERE, null, ex);
            }
        }


        /// <summary>
        ///     ************************************************************************
        ///     * CODE OF THE RECEIVER THREAD
        /// </summary>
        /// ************************************************************************
        public virtual void stop()
        {
            terminate = true;
            port.Close();
        }
    }
}