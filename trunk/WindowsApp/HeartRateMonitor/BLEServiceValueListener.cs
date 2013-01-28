﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HeartRateMonitor
{
    public interface BLEServiceValueListener
    {
        void receivedValue(BLEServiceInstance srv, byte[] value);
        void receivedInterval(BLEServiceInstance srv, int value);
    }
}
