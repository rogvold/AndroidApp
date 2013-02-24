﻿using System.Collections.Generic;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace ClientServerInteraction
{
    public class Session
    {
        private const int Sleep = 1;
        private const int Rest = 2;
        private const int Work = 3;
        private const int Training = 4;

        private ObjectId _id;

        [BsonId]
        public ObjectId Id
        {
            get { return _id; }
            set
            {
                _id = value;
                IdString = _id.ToString();
            }
        }

        public string IdString { get; private set; }

        public string UserId { get; set; }

        public long StartTimestamp { get; set; }

        public string Info { get; set; }

        public string /* int */ DeviceId { get; set; }

        public string DeviceName { get; set; }

        public int Activity { get; set; }

        public int HealthState { get; set; }

        public List<int> Rates { get; set; }

        // Evaluations section

        public int AMoPercents { get; set; }

        public double Mo { get; set; }

        public int[] RSAI { get; set; }

        public double HF { get; set; }

        public double HFPercents { get; set; }

        public double IC { get; set; }

        public double LF { get; set; }

        public double LFPercents { get; set; }

        public double TP { get; set; }

        public double ULF { get; set; }

        public double ULFPercents { get; set; }

        public double VLF { get; set; }

        public double VLFPercents { get; set; }

        public int Average { get; set; }

        public int RMSSD { get; set; }

        public int SDNN { get; set; }

        public int PNN50 { get; set; }
    }
}
