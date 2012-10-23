using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace HeartRateMonitor.BusinessLayer
{
    public class Session
    {
        [BsonId]
        public ObjectId Id { get; set; }

        public string StartTime { get; set; }

        public long StartTimeStamp { get; set; }

        public string /* int */ DeviceId { get; set; }

        public string DeviceName { get; set; }

        public List<int> Rates { get; set; }

    }
}
