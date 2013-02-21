using System.Collections.Generic;

namespace ClientServerInteraction
{
    public class Session
    {

        public string IdString { get; set; }

        public string UserId { get; set; }

        public long StartTimestamp { get; set; }

        public string Info { get; set; }

        public string /* int */ DeviceId { get; set; }

        public string DeviceName { get; set; }

        public List<int> Rates { get; set; }

    }
}
