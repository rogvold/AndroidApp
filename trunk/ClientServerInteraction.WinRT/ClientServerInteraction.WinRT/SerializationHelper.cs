using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace ClientServerInteraction.WinRT
{
    public sealed class SerializationHelper
    {

        public static string SerializeSession(Session session)
        {
            return JsonConvert.SerializeObject(session);
        }

        public static Session DeserializeSession(string json)
        {
            try 
            {
                return JsonConvert.DeserializeObject<Session>(json);
            }
            catch (Exception)
            {
                return null;
            }
        }

        public static User DeserializeUser(string json)
        {
            try
            {
                return JsonConvert.DeserializeObject<User>(json);
            } catch (Exception)
            {
                return null;
            }
        }

        public static string SerializeSessionIds(object strings)
        {
            return JsonConvert.SerializeObject(new
            {
                Sessions = strings
            });
        }

        public static IList<Session> DeserializeSessionList(string json)
        {
            try {
                return JsonConvert.DeserializeObject<List<Session>>(json);
            }
            catch (Exception)
            {
                return null;
            }
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
