using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using ClientServerInteraction;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace HeartRateMonitor.BusinessLayer
{
    public class SessionDB : Session
    {
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

        public SessionDB(Session session)
        {
            DeviceId = session.DeviceId;
            DeviceName = session.DeviceName;
            Info = session.Info;
            Rates = session.Rates;
            StartTimestamp = session.StartTimestamp;
            UserId = session.UserId;
        }



    }
}
