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

        // response constants
        private const int NormalCode = 1;
        private const int ErrorCode = 0;

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
