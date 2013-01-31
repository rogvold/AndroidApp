using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data.SQLite;
using System.Linq;
using System.Text;
using System.Threading;

namespace HeartRateMonitor
{
    class BLEOffline
    {
        private static string dbConnection = "Data Source=local.s3db";

        private static readonly Object obj = new Object();

        public static void CreateDB()
        {
            SQLiteConnection cnn = new SQLiteConnection(dbConnection);
            cnn.Open();
            SQLiteCommand tableCreate = new SQLiteCommand(cnn);
            tableCreate.CommandText = "CREATE TABLE [Jsons] ([json_id] INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT, [json_string] TEXT  NOT NULL)";
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
                lock (obj)
                {
                    SQLiteConnection cnn = new SQLiteConnection(dbConnection);
                    cnn.Open();
                    SQLiteCommand jsonInsert = new SQLiteCommand(cnn);
                    jsonInsert.CommandText = String.Format("insert into Jsons (json_string) values(\"{0}\")",
                        ByteUtils.bytesToString(System.Text.Encoding.UTF8.GetBytes(
                        BLEJson.MakeIntervalsJSON(intervals, connectedDevice, startTime, Username, Password, create))));
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
                    SQLiteConnection cnn = new SQLiteConnection(dbConnection);
                    cnn.Open();
                    SQLiteCommand jsons = new SQLiteCommand(cnn);
                    jsons.CommandText = "select json_string from jsons";
                    SQLiteDataReader json_strings = jsons.ExecuteReader();
                    List<string> result = new List<string>();
                    while (json_strings.Read())
                    {
                        result.Add(json_strings.GetString(json_strings.GetOrdinal("json_string")));
                    }
                    json_strings.Close();
                    foreach (string json in result)
                    {
                        BLEJson.SendJSON(System.Text.Encoding.UTF8.GetString(ByteUtils.bytesFromString(json)), "http://reshaka.ru:8080/BaseProjectWeb/faces/input");
                        Thread.Sleep(100);
                    }

                    SQLiteCommand delete = new SQLiteCommand(cnn);
                    delete.CommandText = "delete from Jsons";
                    object deleted = delete.ExecuteNonQuery();

                    if ((int)deleted != result.Count)
                        throw new Exception("Something wrong");

                    delete.CommandText = "vacuum";
                    delete.ExecuteNonQuery();

                    cnn.Close();
                });
                bw.RunWorkerAsync();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }
    }
}
