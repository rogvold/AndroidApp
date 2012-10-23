using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace HeartRateMonitor.BusinessLayer
{
    public class User
    {
        [BsonId]
        public ObjectId Id { get; set; }

        public List<string> Sessions { get; set; }

        public string Username { get; set; }


    }
}
