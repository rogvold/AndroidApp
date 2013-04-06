using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using ClientServerInteraction.Error;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace ClientServerInteraction
{
    public class SerializationHelper
    {
        private const string Secret = @"h7a7RaRtvAVwnMGq5BV6";
        // response constants
        private const int NormalCode = 1;
        private const int ErrorCode = 0;
        // error constants

        public static string CreateQueryString(IDictionary<object, object> parameters)
        {
            var str = "";
            foreach (var parameter in parameters.Keys)
            {
                if (!str.Equals(""))
                    str += "&";
                str += String.Format(@"{0}={1}", parameter, parameters[parameter]);
            }
            return str;
        }

        public static bool DeserializeValidationResponse(string json)
        {
            CheckError(json);
            return JToken.Parse(json).Value<int>("response") != 0;
        }

        public static string SerializeUser(User user)
        {
            return JsonConvert.SerializeObject(user, new JsonSerializerSettings {ContractResolver = new LowercaseContractResolver()});
        }

        public static string SerializeRates(string email, string password, long start, IEnumerable<int> rates)
        {
            return JsonConvert.SerializeObject(new
                {
                    email,
                    password,
                    start,
                    rates,
                    create = 1
                });
        }

        public static User DeserializeUser(string json)
        {
            return JsonConvert.DeserializeObject<User>(json);
        }

        private static void CheckError(string json)
        {
            var errorMessage = JToken.Parse(json).Value<string>("error");
            if (errorMessage != null)
                throw new ServerResponseException(errorMessage, ServerResponseException.OtherError);
        }

        public static JToken ParseResponse(string json)
        {
            var response = JsonConvert.DeserializeObject<ServerResponse>(json);
            switch (response.ResponseCode)
            {
                case NormalCode:
                    return response.Data;
                case ErrorCode:
                    int errorCode;
                    var errorMessage = DeserializeError(response.Error, out errorCode);
                    throw new ServerResponseException(errorMessage, errorCode);
            }
            return null;
        }

        public static AccessToken DeserializeAccessToken(JToken response)
        {
            return JsonConvert.DeserializeObject<AccessToken>(response.ToString());
        }

        private static string DeserializeError(JToken error, out int errorCode)
        {
            errorCode = error.Value<int>("code");
            return error.Value<string>("message");
        }

        public static T Deserialize<T>(string json)
        {
            return JsonConvert.DeserializeObject<T>(json);
        }

        public static string Serialize(Object obj)
        {
            return JsonConvert.SerializeObject(obj);
        }

        private class ServerResponse
        {
            public int ResponseCode { get; set; }
            public JToken Error { get; set; }
            public JToken Data { get; set; }
        }
    }
}
