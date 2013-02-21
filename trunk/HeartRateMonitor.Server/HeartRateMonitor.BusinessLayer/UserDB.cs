using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Web.Security;
using ClientServerInteraction;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace HeartRateMonitor.BusinessLayer
{
    public class UserDB : User
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

    }
}
