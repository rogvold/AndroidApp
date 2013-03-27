using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using ClientServerInteraction.Error;

namespace ClientServerInteraction
{
    public class HttpHelper
    {

        public static Task<string> PostAsync(string url, string queryString, string json)
        {
            if (queryString != null)
                url += queryString;
            var request = (HttpWebRequest)WebRequest.Create(url);
            request.ContentType = "text/json";
            request.Method = "POST";

            if (json == null)
            {
                // If there's nothing to be written to the request then just get the response
                return GetHttpResponseAsync(request).ContinueWith(resp => ReadAsString(resp.Result));
            }

            // Write the post data to the request stream
            return GetHttpRequestStreamAsync(request).ContinueWith(stream =>
                {
                    using (var streamWriter = new StreamWriter(stream.Result))
                    {
                        streamWriter.Write(json);
                        streamWriter.Flush();
                        streamWriter.Close();
                    }
                    return request;
                }).ContinueWith(req => (HttpWebResponse)req.Result.GetResponse()).ContinueWith(resp => ReadAsString(resp.Result));

        }

        private static Task<HttpWebResponse> GetHttpResponseAsync(HttpWebRequest request)
        {
            try
            {
                return Task.Factory.FromAsync(request.BeginGetResponse, ar => (HttpWebResponse)request.EndGetResponse(ar), null);
            }
            catch (Exception ex)
            {
                return TaskErrorHelper.ErrorAsync<HttpWebResponse>(ex);
            }
        }

        private static Task<Stream> GetHttpRequestStreamAsync(HttpWebRequest request)
        {
            try
            {
                return Task.Factory.FromAsync<Stream>(request.BeginGetRequestStream, request.EndGetRequestStream, null);
            }
            catch (Exception ex)
            {
                return TaskErrorHelper.ErrorAsync<Stream>(ex);
            }
        }

        private static string ReadAsString(HttpWebResponse response)
        {
            try
            {
                using (response)
                {
                    using (var stream = response.GetResponseStream())
                    {
                        var reader = new StreamReader(stream);

                        return reader.ReadToEnd();
                    }
                }
            }
            catch (Exception ex)
            {
                return null;
            }
        }
    }
}
