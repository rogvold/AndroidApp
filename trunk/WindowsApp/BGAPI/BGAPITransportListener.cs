using System;
using System.Collections.Generic;
using System.Text;

namespace BGAPI
{
    public interface BGAPITransportListener
    {
        void packetSent(BGAPIPacket packet);
        void packetReceived(BGAPIPacket packet);
    }
}
