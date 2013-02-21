﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace ClientServerInteraction
{
    public class SerializationHelper
    {

        public static string SerializeSession(Session session)
        {
            return JsonConvert.SerializeObject(session);
        }

        public static Session DeserializeSession(string json)
        {
            return JsonConvert.DeserializeObject<Session>(json);
        }

        public static User DeserializeUser(string json)
        {
            return JsonConvert.DeserializeObject<User>(json);
        }

        public static string SerializeSessionIds(List<string> strings)
        {
            return JsonConvert.SerializeObject(new
                {
                    Sessions = strings
                });
        }
 
        public static List<Session> DeserializeSessionList(string json)
        {
            return JsonConvert.DeserializeObject<List<Session>>(json);
        } 

        public static string SerializeRegistrationInfo(string email, string password, string username)
        {
            return JsonConvert.SerializeObject(new
                {
                    Email = email,
                    Password = password,
                    Username = username
                });
        }

        public static string SerializeAuthorizationInfo(string email, string password)
        {
            return JsonConvert.SerializeObject(new
            {
                Email = email,
                Password = password
            });
        }

        public static bool DeserializeRegistrationResponse(string json)
        {
            return JToken.Parse(json).Value<int>("success") == 1;
        }
    }
}