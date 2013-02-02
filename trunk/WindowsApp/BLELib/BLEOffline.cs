using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data.SQLite;
using System.Net.NetworkInformation;
using System.Threading;

namespace BLELib
{
    public class BLEOffline
    {
        private static string dbConnection = "Data Source=local.s3db";

        private static readonly SQLiteConnection cnn = new SQLiteConnection(dbConnection);

        public static void CreateDB()
        {
            SQLiteCommand tableCreate = new SQLiteCommand(cnn);
            tableCreate.CommandText = "CREATE TABLE [Jsons] ([json_id] INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT, [json_string] TEXT  NOT NULL)";
            cnn.Open();
            object user_id = tableCreate.ExecuteScalar();
            cnn.Close();
        }

        public static void SaveIntervals(List<ushort> intervals, 
            BLEDevice connectedDevice, 
            DateTime startTime, 
            string Username,
            string Password, 
            int create)
        {
            try
            {
                SQLiteCommand jsonInsert = new SQLiteCommand(cnn);
                jsonInsert.CommandText = String.Format("insert into Jsons (json_string) values(\"{0}\")",
                ByteUtils.BytesToString(System.Text.Encoding.UTF8.GetBytes(
                BLEJson.MakeIntervalsJSON(intervals, connectedDevice, startTime, Username, Password, create))));
                lock (cnn)
                {
                    cnn.Open();
                    object user_id = jsonInsert.ExecuteScalar();
                    cnn.Close();
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        public static void SendSavedIntervals()
        {
            try
            {
                BackgroundWorker bw = new BackgroundWorker();
                bw.DoWork += new DoWorkEventHandler(
                delegate(object o, DoWorkEventArgs args)
                {
                    while (true)
                    {
                        if (IsConnectedToInternet)
                        {
                            SQLiteCommand jsons = new SQLiteCommand(cnn);
                            jsons.CommandText = "select json_string from jsons";
                            List<string> result = new List<string>();
                            lock (cnn)
                            {
                                cnn.Open();
                                SQLiteDataReader json_strings = jsons.ExecuteReader();
                                while (json_strings.Read())
                                {
                                    result.Add(json_strings.GetString(json_strings.GetOrdinal("json_string")));
                                }
                                json_strings.Close();
                                cnn.Close();
                            }
                            if (result.Count == 0)
                                continue;
                            foreach (string json in result)
                            {
                                BLEJson.SendJSON(System.Text.Encoding.UTF8.GetString(ByteUtils.BytesFromString(json)), "http://reshaka.ru:8080/BaseProjectWeb/faces/input");
                                Thread.Sleep(100);
                            }

                            SQLiteCommand delete = new SQLiteCommand(cnn);
                            delete.CommandText = "delete from Jsons";
                            lock (cnn)
                            {
                                cnn.Open();
                                object deleted = delete.ExecuteNonQuery();
                                cnn.Close();
                                if ((int)deleted != result.Count)
                                    throw new Exception("Something wrong");
                            }

                            lock (cnn)
                            {
                                cnn.Open();
                                delete.CommandText = "vacuum";
                                delete.ExecuteNonQuery();
                                cnn.Close();
                            }
                        }
                    }
                });
                bw.RunWorkerAsync();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }


        public static bool IsConnectedToInternet
        {
            get
            {
                Uri url = new Uri("http://www.reshaka.ru/");
                string pingurl = string.Format("{0}", url.Host);
                string host = pingurl;
                bool result = false;
                Ping p = new Ping();
                try
                {
                    PingReply reply = p.Send(host, 1000);
                    if (reply.Status == IPStatus.Success)
                        return true;
                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex.Message);
                    return false;
                }
                return result;
            }
        }
    }
}
