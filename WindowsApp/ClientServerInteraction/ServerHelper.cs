using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ClientServerInteraction.Error;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace ClientServerInteraction
{
    public class ServerHelper
    {
        // server url and controller suffixes
        private const string ServerBase = @"http://www.cardiomood.com/BaseProjectWeb/";
        private const string Resources = @"resources/";
        private const string Authorization = @"SecureAuth/";
        private const string Sessions = @"SecureSessions/";
        private const string Indicators = @"SecureIndicators/";
        private const string Token = @"token/";
        private const string Rates = @"SecureRatesUploading/";

        private static Task BaseRequest<T>(string urlSuffix, Dictionary<object, object> queryContent,
            Object json, ResponseCallback<T> callback)
        {
            var url = ServerBase + Resources + urlSuffix;
            var jsonString = json == null ? null : SerializationHelper.Serialize(json);
            if (queryContent == null)
                queryContent = new Dictionary<object, object>();
            if (jsonString != null)
                queryContent.Add("json", jsonString);
            var content = SerializationHelper.CreateQueryString(queryContent);
            return HttpHelper.PostAndParseResponseAsync<T>(url, null, content).
                ContinueWith(task => CommonCallbackRoutine(task, callback));
        }

        public static Task LogIn(string email, string password, string deviceId, 
            ResponseCallback<AccessToken> callback)
        {
            return BaseRequest(Token + "authorize",
                               new Dictionary<object, object>
                                   {{"email", email}, {"password", password}, {"deviceId", deviceId}}, null, callback);
        }

        public static Task ValidateEmail(string email, ResponseCallback<bool> callback)
        {
            return BaseRequest(Authorization + "check_existence", new Dictionary<object, object> {{"email", email}},
                               null, callback);
        }   
        
        public static Task Register(string email, string password, ResponseCallback<bool> callback)
        {
            return BaseRequest(Authorization + "register",
                               new Dictionary<object, object> {{"email", email}, {"password", password}}, null, callback);

        }

        public static Task CheckAuthorizationData(string email, string password, ResponseCallback<bool> callback)
        {
            return BaseRequest(Authorization + "register",
                               new Dictionary<object, object> { { "email", email }, { "password", password } }, null, callback);
        }

        public static Task GetUserInfo(string accessToken, ResponseCallback<User> callback)
        {
            return BaseRequest(Authorization + "info", new Dictionary<object, object> {{"token", accessToken}},
                               null, callback);
        }

        public static Task UpdateUserInfo(string accessToken, User user, ResponseCallback<User> callback)
        {
            return BaseRequest(Authorization + "update_info", new Dictionary<object, object> {{"token", accessToken}},
                               user, callback);
        }

        public static Task Upload(string accessToken, long start, IEnumerable<int> rates, bool create, ResponseCallback<bool> callback)
        {
            return BaseRequest(Rates + "upload", new Dictionary<object, object> {{"token", accessToken}}, new
                {
                    start,
                    rates,
                    create = create ? 1 : 0
                }, callback);
        }

        public static Task SynchronizeRates(string accessToken, long start, IEnumerable<int> rates, bool create, ResponseCallback<bool> callback)
        {
            return BaseRequest(Rates + "sync", new Dictionary<object, object> { { "token", accessToken } }, new
            {
                start,
                rates,
                create = create ? 1 : 0
            }, callback);
        }

        public static Task GetAllSessions(string accessToken, ResponseCallback<IEnumerable<Session>> callback)
        {
            return BaseRequest(Sessions + "all", new Dictionary<object, object> {{"token", accessToken}}, null, callback);
        }

        public static Task GetTension(string accessToken, long sessionId, ResponseCallback<IEnumerable<double[]>> callback)
        {
            return BaseRequest(Indicators + sessionId + "/tension", new Dictionary<object, object> {{"token", accessToken}},
                               null, callback);
        }

        public static Task GetRates(string accessToken, long sessionId, ResponseCallback<IEnumerable<int>> callback)
        {
            return BaseRequest(Sessions + "rates",
                               new Dictionary<object, object> {{"sessionId", sessionId}, {"token", accessToken}}, null,
                               callback);
        }

        private static void CommonCallbackRoutine<T>(Task<T> task, ResponseCallback<T> callback)
        {
            var exception = task.Exception == null ? null : task.Exception.InnerException;
            if (exception == null)
            {
                callback.Success.Invoke(task.Result);
            }
            else if (exception is ServerResponseException)
            {
                callback.ServerError.Invoke((ServerResponseException)exception);
            }
            else
            {
                callback.ClientError.Invoke(exception);
            }
        }

      
    }
}
