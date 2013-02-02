using System;
using System.Collections.Generic;
using BGAPI;

namespace BLELib
{
    public class BLEServiceInstance : BGAPIDefaultListener
    {

        public List<BLEServiceValueListener> listeners = new List<BLEServiceValueListener>();

        public virtual void addBLEServiceValueListener(BLEServiceValueListener l)
        {
            listeners.Add(l);
        }

        public virtual void removeBLEServiceValueListener(BLEServiceValueListener l)
        {
            listeners.Remove(l);
        }

        protected internal BGAPI.BGAPI bgapi;
        protected internal int connection;
        protected internal int value_handle;
        protected internal int interval_handle;
        protected internal int config_handle;

        public virtual BGAPI.BGAPI Bgapi
        {
            get
            {
                return bgapi;
            }
        }

        public virtual int Connection
        {
            get
            {
                return connection;
            }
        }

        public virtual int Value_handle
        {
            get
            {
                return value_handle;
            }
        }

        public virtual int Interval_handle
        {
            get
            {
                return interval_handle;
            }
        }

        public virtual int Config_handle
        {
            get
            {
                return config_handle;
            }
        }

        public BLEServiceInstance(BGAPI.BGAPI bgapi, int connection, int value_handle, int interval_handle, int config_handle)
        {
            this.bgapi = bgapi;
            this.connection = connection;
            this.value_handle = value_handle;
            this.interval_handle = interval_handle;
            this.config_handle = config_handle;
            bgapi.addListener(this);
        }

        public virtual void disconnect()
        {
            bgapi.removeListener(this);
        }

        public BLEServiceInstance(BGAPI.BGAPI bgapi, int connection, BLEService srv)
        {
            // TODO: Impelment
            throw new Exception("Not Implemented");
        }

        public virtual void subscribeIndications()
        {
            bgapi.send_attclient_write_command(connection, config_handle, new byte[] { 0x02, 0x00 });
        }

        public virtual void subscribeNotifications()
        {
            bgapi.send_attclient_write_command(connection, config_handle, new byte[] { 0x01, 0x00 });
        }
        public virtual void unsubscribe()
        {
            bgapi.send_attclient_write_command(connection, config_handle, new byte[] { 0x00, 0x00 });
        }

        public virtual void writeInterval(int value)
        {
            byte[] i = new byte[2];
            i[0] = (byte)((value >> 8) & 0xFF);
            i[1] = (byte)(value & 0xFF);
            bgapi.send_attclient_write_command(connection, interval_handle, i);
        }

        public virtual void readInterval()
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
                        l.receivedValue(this, value);
                    }
                }
                else if (atthandle == interval_handle)
                {
                    foreach (BLEServiceValueListener l in listeners)
                    {
                        l.receivedInterval(this, (value[0] << 8) + (value[1] & 0xFF));
                    }
                }
            }
        }
    }
}
