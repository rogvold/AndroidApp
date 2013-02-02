using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Net;
using System.Text;

namespace BLELib
{
    public class BLEJson
    {
        public static string MakeIntervalsJSON(List<ushort> intervals, 
            BLEDevice connectedDevice, 
            DateTime startTime, 
            string Username,
            string Password,
            int create)
        {
            string date = startTime.ToString("yyyy-MM-dd HH:mm:ss.fff", CultureInfo.InvariantCulture);
            string deviceName = connectedDevice.Name;
            string deviceId = connectedDevice.Address;
            Dictionary<string, object> jsonDict = new Dictionary<string, object>();
            jsonDict.Add("start", date);
            jsonDict.Add("device_id", deviceId);
            jsonDict.Add("device_name", deviceName);
            jsonDict.Add("rates", intervals.ToArray());
            jsonDict.Add("email", Username);
            jsonDict.Add("password", Password);
            jsonDict.Add("create", create == 0 ? "0" : "1");
            return "json=" + JsonConvert.SerializeObject(jsonDict);
        }

        public static HttpWebResponse SendJSON(string json, string url)
        {
            byte[] data = Encoding.UTF8.GetBytes(json);
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
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
            Dictionary<string, object> jsonDict = new Dictionary<string, object>();
            jsonDict.Add("purpose", "CheckUserExistence");
            jsonDict.Add("email", username);
            jsonDict.Add("secret", "h7a7RaRtvAVwnMGq5BV6");
            string json = "json=" + JsonConvert.SerializeObject(jsonDict);
            HttpWebResponse resp = SendJSON(json, "http://reshaka.ru:8080/BaseProjectWeb/mobileauth");
            Stream responseStream = resp.GetResponseStream();
            StreamReader sr = new StreamReader(responseStream);
            string response = sr.ReadToEnd();
            responseStream.Close();
            Dictionary<string, string> respDict = JsonConvert.DeserializeObject<Dictionary<string, string>>(response);

            return Convert.ToInt32(respDict["response"]);
        }

        public static int CheckUser(string username, string password)
        {
            Dictionary<string, object> jsonDict = new Dictionary<string, object>();
            jsonDict.Add("purpose", "CheckAuthorisationData");
            jsonDict.Add("email", username);
            jsonDict.Add("password", password);
            jsonDict.Add("secret", "h7a7RaRtvAVwnMGq5BV6");
            string json = "json=" + JsonConvert.SerializeObject(jsonDict);
            HttpWebResponse resp = SendJSON(json, "http://reshaka.ru:8080/BaseProjectWeb/mobileauth");
            Stream responseStream = resp.GetResponseStream();
            StreamReader sr = new StreamReader(responseStream);
            string response = sr.ReadToEnd();
            responseStream.Close();
            Dictionary<string, string> respDict = JsonConvert.DeserializeObject<Dictionary<string, string>>(response);

            return Convert.ToInt32(respDict["response"]);
        }
    }
}
