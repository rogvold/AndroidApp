using System;
using System.Web.Script.Serialization;

namespace HeartRateMonitor.Server.Models.JSON
{
    public static class JSONHelper
    {
        public static string ToJSON(this object obj)
        {
            var serializer = new JavaScriptSerializer();
            return serializer.Serialize(obj);
        }

        public static string ToJSON(this object obj, int depth)
        {
            var serializer = new JavaScriptSerializer {RecursionLimit = depth};
            return serializer.Serialize(obj);
        }

        public static object ToObject(this string json, Type targetType)
        {
            var serializer = new JavaScriptSerializer {MaxJsonLength = int.MaxValue};
            
            return serializer.Deserialize(json, targetType);
        }

        public static object ToObject(this string json)
        {
            var serializer = new JavaScriptSerializer();
            return serializer.DeserializeObject(json);
        }

        public static dynamic ToDynamic(this string json)
        {
            var serializer = new JavaScriptSerializer();
            serializer.RegisterConverters(new[] {new DynamicJsonConverter()});

            return serializer.Deserialize(json, typeof (object));
            //return Json.Decode(json);


            //var jss = new JavaScriptSerializer();
            //var d = jss.Deserialize<dynamic>(json);
            //return d as DynamicObject;
        }
    }
}