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
        private const string Token = @"token/";
        private const string Rates = @"rates/";

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

        public static Task<bool> UpdateUserInfo(User user)
        {
            const string url = ServerBase + Resources + Authorization + "info";
            var json = SerializationHelper.SerializeUser(user);
            var queryString =
                SerializationHelper.CreateQueryString(new Dictionary<object, object> { { "json", json } });
            return
                HttpHelper.PostAsync(url, queryString, null).ContinueWith(
                    resp => SerializationHelper.DeserializeValidationResponse(resp.Result));
        }

        public static Task<bool> Upload(string email, string password, long start, IEnumerable<int> rates)
        {
            const string url = ServerBase + Resources + Rates + "upload";
            var json = "json=" + SerializationHelper.SerializeRates(email, password, start, rates);
            return
                HttpHelper.PostAsync(url, null, json).ContinueWith(
                    resp => SerializationHelper.DeserializeValidationResponse(resp.Result));
        }

        public static Task<bool> SynchronizeRates(string email, string password, long start, IEnumerable<int> rates)
        {
            const string url = ServerBase + Resources + Rates + "sync";
            var json = "json=" + SerializationHelper.SerializeRates(email, password, start, rates);
            return
                HttpHelper.PostAsync(url, null, json).ContinueWith(
                    resp => SerializationHelper.DeserializeValidationResponse(resp.Result));
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
