using System;
using System.Collections.Generic;
using BGAPI;

namespace BLELib
{
    public class BLEServiceInstance : BGAPIDefaultListener
    {
        protected internal BGAPI.BGAPI bgapi;
        protected internal int config_handle;
        protected internal int connection;
        protected internal int interval_handle;
        public List<BLEServiceValueListener> listeners = new List<BLEServiceValueListener>();
        protected internal int value_handle;

        public BLEServiceInstance(BGAPI.BGAPI bgapi, int connection, int valueHandle, int intervalHandle,
                                  int configHandle)
        {
            this.bgapi = bgapi;
            this.connection = connection;
            value_handle = valueHandle;
            interval_handle = intervalHandle;
            config_handle = configHandle;
            bgapi.addListener(this);
        }

        public BLEServiceInstance(BGAPI.BGAPI bgapi, int connection, BLEService srv)
        {
            // TODO: Impelment
            throw new Exception("Not Implemented");
        }

        public virtual BGAPI.BGAPI Bgapi
        {
            get { return bgapi; }
        }

        public virtual int Connection
        {
            get { return connection; }
        }

        public virtual int ValueHandle
        {
            get { return value_handle; }
        }

        public virtual int IntervalHandle
        {
            get { return interval_handle; }
        }

        public virtual int ConfigHandle
        {
            get { return config_handle; }
        }

        public virtual void AddBLEServiceValueListener(BLEServiceValueListener l)
        {
            listeners.Add(l);
        }

        public virtual void RemoveBLEServiceValueListener(BLEServiceValueListener l)
        {
            listeners.Remove(l);
        }

        public virtual void Disconnect()
        {
            bgapi.removeListener(this);
        }

        public virtual void SubscribeIndications()
        {
            bgapi.send_attclient_write_command(connection, config_handle, new byte[] {0x02, 0x00});
        }

        public virtual void SubscribeNotifications()
        {
            bgapi.send_attclient_write_command(connection, config_handle, new byte[] {0x01, 0x00});
        }

        public virtual void Unsubscribe()
        {
            bgapi.send_attclient_write_command(connection, config_handle, new byte[] {0x00, 0x00});
        }

        public virtual void WriteInterval(int value)
        {
            var i = new byte[2];
            i[0] = (byte) ((value >> 8) & 0xFF);
            i[1] = (byte) (value & 0xFF);
            bgapi.send_attclient_write_command(connection, interval_handle, i);
        }

        public virtual void ReadInterval()
        {
            bgapi.send_attclient_read_by_handle(connection, interval_handle);
        }


        public override void receive_attclient_attribute_value(int conn, int atthandle, int type, byte[] value)
        {
            if (connection == conn)
            {
                if (atthandle == value_handle)
                {
                    foreach (BLEServiceValueListener l in listeners)
                    {
                        l.ReceivedValue(this, value);
                    }
                }
                else if (atthandle == interval_handle)
                {
                    foreach (BLEServiceValueListener l in listeners)
                    {
                        l.ReceivedInterval(this, (value[0] << 8) + (value[1] & 0xFF));
                    }
                }
            }
        }
    }
}