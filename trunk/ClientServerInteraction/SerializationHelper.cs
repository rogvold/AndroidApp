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

        public static string CreateQueryString(IDictionary<object, object> parameters)
        {
            return parameters.Keys.Aggregate("?secret=" + Secret, (current, parameter) => current + String.Format(@"&{0}={1}", parameter, parameters[parameter]));
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
                throw new ServerResponseException(errorMessage);
        }
    }
}
