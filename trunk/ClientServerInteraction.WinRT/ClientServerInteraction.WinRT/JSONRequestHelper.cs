﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using Windows.Foundation;

namespace ClientServerInteraction.WinRT
{
    public sealed class JSONRequestHelper
    {
        private static readonly HttpClient Client = new HttpClient()
                                                        {
                                                            Timeout = new TimeSpan(0, 0, 0, 5)
                                                        };


        public static IAsyncOperation<string> SendRequest(string url, string json)
        {
            return SendRequestInternal(url, json).AsAsyncOperation();
        }

        internal static async Task<string> SendRequestInternal(string url, string json)
        {
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Post, url);
                var array = Encoding.UTF8.GetBytes(json);

                var httpContent = new ByteArrayContent(array);
                httpContent.Headers.ContentType = MediaTypeHeaderValue.Parse("text/json");
                var response = await Client.PostAsync(request.RequestUri, httpContent);
                return await response.Content.ReadAsStringAsync();
            }
            catch (Exception ex)
            {
                return null;
            }
        }
    }
}
