using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace BGAPI
{
    public interface BGAPITransportListener
    {
        void packetSent(BGAPIPacket packet);
        void packetReceived(BGAPIPacket packet);
    }
}
