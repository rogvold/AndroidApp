using System;
using System.Collections.Generic;

namespace BLELib
{
    public class BLESession
    {
        private BLEDevice _ConnectedDevice;
        private List<ushort> _Intervals;

        private DateTime _StartTime;

        private BLEUser _User;

        public BLESession(DateTime startTime, BLEDevice connectedDevice, BLEUser user, List<ushort> intervals)
        {
            ConnectedDevice = connectedDevice;
            StartTime = startTime;
            User = user;
            Intervals = intervals;
        }

        public BLEDevice ConnectedDevice
        {
            get { return _ConnectedDevice; }
            set
            {
                if (value != null)
                    _ConnectedDevice = value;
            }
        }

        public DateTime StartTime
        {
            get { return _StartTime; }
            set
            {
                if (value != null)
                    _StartTime = value;
            }
        }

        public BLEUser User
        {
            get { return _User; }
            set
            {
                if (value != null)
                    _User = value;
            }
        }

        public List<ushort> Intervals
        {
            get { return _Intervals; }
            set
            {
                if (value != null)
                    _Intervals = value;
            }
        }
    }
}