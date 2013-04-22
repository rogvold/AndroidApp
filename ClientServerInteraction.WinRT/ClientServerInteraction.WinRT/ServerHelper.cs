using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Windows.Foundation;
using System.Collections;
using System.Reflection;

namespace ClientServerInteraction.WinRT
{
    public sealed class ServerHelper
    {
        // hardcoded server url
        private const string ServerBase = @"http://omnihealth.azurewebsites.net/";
        private const string Account = @"Account/";
        private const string Evaluation = @"Evaluation/";

        // returns true on successful registration
        public static IAsyncOperation<bool> RegisterUser(string email, string password, string username)
        {
                return RegisterUserInternal(email, password, username).AsAsyncOperation();
        }

        internal static async Task<bool> RegisterUserInternal(string email, string password, string username)
        {
            var response = await JSONRequestHelper.SendRequest(ServerBase + Account + @"RegisterUser",
                                 SerializationHelper.SerializeRegistrationInfo(email, password, username));

            return SerializationHelper.DeserializeRegistrationResponse(response);
        }

        public static IAsyncOperation<IList<object>> AuthorizeUser(string email, string password)
        {
            return AuthorizeUserInternal(email, password).AsAsyncOperation();
        }

        internal static async Task<IList<object>> AuthorizeUserInternal(string email, string password)
        {
            var response = await JSONRequestHelper.SendRequest(ServerBase + Account + @"AuthorizeUser",
                                            SerializationHelper.SerializeAuthorizationInfo(email, password));

            return SerializationHelper.DeserializeUser(response);
        }

        public static IAsyncOperation<IList<object>> GetUserInfo(string userId)
        {
            return GetUserInfoInternal(userId).AsAsyncOperation();
        }

        internal static async Task<IList<object>> GetUserInfoInternal(string userId)
        {
            var json = JsonConvert.SerializeObject(new
            {
                UserId = userId
            });

            var response = await JSONRequestHelper.SendRequest(ServerBase + Account + @"GetUserInfo", json);
            return SerializationHelper.DeserializeUser(response);
        }

        // returns evaluated session
        public static IAsyncOperation<Session> AddSession(Session session, string userId)
        {
			session.UserId = userId;
            session.Rates = ((IEnumerable)session.Rates).Cast<int>().ToList();
            session.Intervals = ((IEnumerable)session.Intervals).Cast<int>().ToList(); 
            return AddSessionInternal(session).AsAsyncOperation();
        }

        internal static async Task<Session> AddSessionInternal(Session session)
        {
            var response = await JSONRequestHelper.SendRequest(ServerBase + Evaluation + @"AddSession",
                                                         SerializationHelper.SerializeSession(session));
            return SerializationHelper.DeserializeSession(response);
        }

        public static IAsyncOperation<IList<Session>> GetSessions(object sessionIds)
        {
            return GetSessionsInternal(sessionIds).AsAsyncOperation();
        }

        internal static async Task<IList<Session>> GetSessionsInternal(object sessionIds)
        {
            var response = await JSONRequestHelper.SendRequest(ServerBase + Evaluation + @"GetSessions",
                                                         SerializationHelper.SerializeSessionIds(sessionIds));
            return SerializationHelper.DeserializeSessionList(response);
        }
    }
}
