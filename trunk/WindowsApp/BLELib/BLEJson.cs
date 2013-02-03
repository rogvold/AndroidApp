using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Net;
using System.Text;
using Newtonsoft.Json;

namespace BLELib
{
    public class BLEJson
    {
        public static string MakeIntervalsJson(BLESession session, DateTime packetTime, int create)
        {
            string date = packetTime.ToString("yyyy-MM-dd HH:mm:ss.fff", CultureInfo.InvariantCulture);
            string deviceName = session.ConnectedDevice.Name;
            string deviceId = session.ConnectedDevice.Address;
            var jsonDict = new Dictionary<string, object>
                {
                    {"start", date},
                    {"device_id", deviceId},
                    {"device_name", deviceName},
                    {"rates", session.Intervals.ToArray()},
                    {"email", session.User.Username},
                    {"password", session.User.Password},
                    {"create", create == 0 ? "0" : "1"}
                };
            session.Intervals.Clear();
            return "json=" + JsonConvert.SerializeObject(jsonDict);
        }

        public static HttpWebResponse SendJson(string json, string url)
        {
            byte[] data = Encoding.UTF8.GetBytes(json);
            var request = (HttpWebRequest) WebRequest.Create(url);
            request.Method = "POST";
            request.ContentType = "application/x-www-form-urlencoded";
            request.ContentLength = data.Length;
            request.KeepAlive = true;
            ServicePointManager.UseNagleAlgorithm = true;
            ServicePointManager.Expect100Continue = true;
            ServicePointManager.CheckCertificateRevocationList = true;
            ServicePointManager.DefaultConnectionLimit = Int32.MaxValue;

            try
            {
                using (Stream outputStream = request.GetRequestStream())
                    outputStream.Write(data, 0, data.Length);
                return request.GetResponse() as HttpWebResponse;
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
            return null;
        }


        public static int UserExists(string username)
        {
            var jsonDict = new Dictionary<string, object>
                {
                    {"purpose", "CheckUserExistence"},
                    {"email", username},
                    {"secret", "h7a7RaRtvAVwnMGq5BV6"}
                };
            string json = "json=" + JsonConvert.SerializeObject(jsonDict);
            HttpWebResponse resp = SendJson(json, "http://reshaka.ru:8080/BaseProjectWeb/mobileauth");
            Stream responseStream = resp.GetResponseStream();
            if (responseStream != null)
            {
                var sr = new StreamReader(responseStream);
                string response = sr.ReadToEnd();
                responseStream.Close();
                var respDict = JsonConvert.DeserializeObject<Dictionary<string, string>>(response);

                return Convert.ToInt32(respDict["response"]);
            }
            else
            {
                return 0;
            }
        }

        public static int CheckUser(string username, string password)
        {
            var jsonDict = new Dictionary<string, object>
                {
                    {"purpose", "CheckAuthorisationData"},
                    {"email", username},
                    {"password", password},
                    {"secret", "h7a7RaRtvAVwnMGq5BV6"}
                };
            string json = "json=" + JsonConvert.SerializeObject(jsonDict);
            HttpWebResponse resp = SendJson(json, "http://reshaka.ru:8080/BaseProjectWeb/mobileauth");
            Stream responseStream = resp.GetResponseStream();
            if (responseStream != null)
            {
                var sr = new StreamReader(responseStream);
                string response = sr.ReadToEnd();
                responseStream.Close();
                var respDict = JsonConvert.DeserializeObject<Dictionary<string, string>>(response);

                return Convert.ToInt32(respDict["response"]);
            }
            else
            {
                return 0;
            }
        }
    }
}