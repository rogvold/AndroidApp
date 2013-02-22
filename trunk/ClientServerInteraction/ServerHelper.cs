using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace ClientServerInteraction
{
    public class ServerHelper
    {
        // hardcoded server url
        private const string ServerBase = @"http://omnihealth.azurewebsites.net/";
        private const string Account = @"Account/";
        private const string Evaluation = @"Evaluation/";

        // returns true on successful registration
        public static bool RegisterUser(string email, string password, string username)
        {
            var response = JSONRequestHelper.SendRequest(ServerBase + Account + @"RegisterUser", 
                                 SerializationHelper.SerializeRegistrationInfo(email, password, username));

            return SerializationHelper.DeserializeRegistrationResponse(response);
        }

        public static User AuthorizeUser(string email, string password)
        {
            var response = JSONRequestHelper.SendRequest(ServerBase + Account + @"AuthorizeUser",
                                            SerializationHelper.SerializeAuthorizationInfo(email, password));

            return SerializationHelper.DeserializeUser(response);
        }

        public static User GetUserInfo(string userId)
        {
            var json = JsonConvert.SerializeObject(new
            {
                UserId = userId
            });

            var response = JSONRequestHelper.SendRequest(ServerBase + Account + @"GetUserInfo", json);
            return SerializationHelper.DeserializeUser(response);
        }

        // returns evaluated session
        public static Session AddSession(Session session)
        {
            var response = JSONRequestHelper.SendRequest(ServerBase + Evaluation + @"AddSession",
                                                         SerializationHelper.SerializeSession(session));
            return SerializationHelper.DeserializeSession(response);
        }

        public static List<Session> GetSessions(List<string> sessionIds)
        {
            var response = JSONRequestHelper.SendRequest(ServerBase + Evaluation + @"GetSessions",
                                                         SerializationHelper.SerializeSessionIds(sessionIds));
            return SerializationHelper.DeserializeSessionList(response);
        } 
    }
}
