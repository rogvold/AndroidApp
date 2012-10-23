using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Web;
using System.IO;
using HeartRateMonitor.Server.Models.JSON;


namespace HeartRateMonitor.Server.Helpers
{
    public static class StreamHelper
    {
        public static dynamic ReadJsonFromStream(Stream stream)
        {
            var json = new StringBuilder();

            using (var reader = new StreamReader(stream))
            {
                while (!reader.EndOfStream)
                {
                    json.Append(reader.ReadLine());
                }
            }

            return json.ToString().ToDynamic();
        }
    }
}