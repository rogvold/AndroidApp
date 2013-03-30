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
        private const string Authorization = @"auth/";
        private const string Rates = @"rates/";

        public static Task<bool> ValidateEmail(string email)
        {
            const string url = ServerBase + Resources + Authorization + "check_existence";
            var queryString = SerializationHelper.CreateQueryString(new Dictionary<object, object> {{"email", email}});
            return
                HttpHelper.PostAsync(url, queryString, null).ContinueWith(json => SerializationHelper.DeserializeValidationResponse(json.Result));
        }   
        
        public static Task<bool> Register(string email, string password)
        {
            const string url = ServerBase + Resources + Authorization + "register";
            var queryString = SerializationHelper.CreateQueryString(new Dictionary<object, object> {{"email", email}, {"password", password}});
            return
                HttpHelper.PostAsync(url, queryString, null).ContinueWith(
                    json => SerializationHelper.DeserializeValidationResponse(json.Result));
        }

        public static Task<bool> CheckAuthorizationData(string email, string password)
        {
            const string url = ServerBase + Resources + Authorization + "check_data";
            var queryString = SerializationHelper.CreateQueryString(new Dictionary<object, object> { { "email", email }, { "password", password } });
            return
                HttpHelper.PostAsync(url, queryString, null).ContinueWith(
                    json => SerializationHelper.DeserializeValidationResponse(json.Result));
        }

        public static Task<User> GetUserInfo(string email, string password)
        {
            const string url = ServerBase + Resources + Authorization + "info";
            var queryString =
                SerializationHelper.CreateQueryString(new Dictionary<object, object>
                    {{"email", email}, {"password", password}});
            return
                HttpHelper.PostAsync(url, queryString, null).ContinueWith(
                    json => SerializationHelper.DeserializeUser(json.Result));
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
        

      
    }
}
