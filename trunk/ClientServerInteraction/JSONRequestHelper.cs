using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;

namespace ClientServerInteraction
{
    public class JSONRequestHelper
    {
        public static string SendRequest(string url, string json)
        {
            var httpWebRequest = (HttpWebRequest) WebRequest.Create(url);
            httpWebRequest.ContentType = "text/json";
            httpWebRequest.Method = "POST";
            httpWebRequest.KeepAlive = false;

            using (var streamWriter = new StreamWriter(httpWebRequest.GetRequestStream()))
            {
                streamWriter.Write(json);
                streamWriter.Flush();
                streamWriter.Close();

                var httpResponse = (HttpWebResponse) httpWebRequest.GetResponse();
                using (var streamReader = new StreamReader(httpResponse.GetResponseStream()))
                {
                    var result = streamReader.ReadToEnd();
                    return result;
                }
            }
        }
    }
}
