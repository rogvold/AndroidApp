using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data.SQLite;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Text;
using System.Threading;

namespace BLELib
{
    public class BLEOffline
    {
        private static string dbConnection = "Data Source=local.s3db";

        private static readonly SQLiteConnection cnn = new SQLiteConnection(dbConnection);

        public static bool IsConnectedToInternet
        {
            get
            {
                var url = new Uri("http://www.reshaka.ru/");
                string pingurl = string.Format("{0}", url.Host);
                string host = pingurl;
                bool result = false;
                var p = new Ping();
                try
                {
                    PingReply reply = p.Send(host, 1000);
                    if (reply.Status == IPStatus.Success)
                        return true;
                }
                catch (Exception ex)
                {
                    //Console.WriteLine(ex.Message);
                    return false;
                }
                return result;
            }
        }

        public static void CreateDB()
        {
            var tableCreate = new SQLiteCommand(cnn);
            tableCreate.CommandText =
                "CREATE TABLE [Sessions] ([session_id] INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT, [username] TEXT  NOT NULL, [password] TEXT  NOT NULL, [start_time] TEXT  NOT NULL, [device_name] TEXT  NULL, [device_id] TEXT  NULL)";
            lock (cnn)
            {
                cnn.Open();
                object user_id = tableCreate.ExecuteScalar();
                cnn.Close();
                tableCreate.CommandText =
                    "CREATE TABLE [Intervals] ([session_id] INTEGER  NOT NULL, [value] TEXT  NOT NULL)";
                cnn.Open();
                user_id = tableCreate.ExecuteScalar();
                cnn.Close();
            }
        }

        public static void SaveIntervals(BLESession session)
        {
            try
            {
                string date = session.StartTime.ToString("yyyy-MM-dd HH:mm:ss.fff", CultureInfo.InvariantCulture);
                var findSession = new SQLiteCommand(cnn);
                findSession.CommandText = String.Format("select session_id from sessions where start_time=\"{0}\"", date);
                object sessionId;
                lock (cnn)
                {
                    cnn.Open();
                    sessionId = findSession.ExecuteScalar();
                    cnn.Close();
                }

                if (sessionId == null)
                {
                    var createSession = new SQLiteCommand(cnn);
                    createSession.CommandText =
                        String.Format(
                            "insert into sessions(username, password, start_time, device_name, device_id) values(\"{0}\", \"{1}\", \"{2}\", \"{3}\", \"{4}\")",
                            session.User.Username, session.User.Password, date, session.ConnectedDevice.Name,
                            session.ConnectedDevice.Address);
                    lock (cnn)
                    {
                        cnn.Open();
                        createSession.ExecuteScalar();
                        cnn.Close();
                    }
                    createSession.CommandText = String.Format(
                        "select session_id from sessions where start_time=\"{0}\"", date);
                    lock (cnn)
                    {
                        cnn.Open();
                        sessionId = createSession.ExecuteScalar();
                        cnn.Close();
                    }
                }

                var intervalsInsert = new SQLiteCommand(cnn);
                var intervalString = new StringBuilder();
                foreach (ushort interval in session.Intervals)
                {
                    intervalString.Append(interval + " ");
                }

                intervalsInsert.CommandText =
                    String.Format("insert into intervals(session_id, value) values(\"{0}\", \"{1}\")", sessionId,
                                  intervalString);

                lock (cnn)
                {
                    cnn.Open();
                    intervalsInsert.ExecuteScalar();
                    cnn.Close();
                }
                session.Intervals.Clear();
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
                var bw = new BackgroundWorker();
                bw.DoWork += (sender, args) =>
                    {
                        try
                        {
                            while (true)
                            {
                                if (IsConnectedToInternet)
                                {
                                    var getSessions = new SQLiteCommand(cnn);
                                    getSessions.CommandText = "select session_id from sessions";
                                    var sessionIds = new List<int>();
                                    lock (cnn)
                                    {
                                        cnn.Open();
                                        SQLiteDataReader session_ids = getSessions.ExecuteReader();
                                        while (session_ids.Read())
                                        {
                                            sessionIds.Add(session_ids.GetInt32(session_ids.GetOrdinal("session_id")));
                                        }
                                        session_ids.Close();
                                        cnn.Close();
                                    }

                                    if (sessionIds.Count == 0)
                                        continue;

                                    var sessions = new List<BLESession>();

                                    foreach (int sessionId in sessionIds)
                                    {
                                        var getSessionTime = new SQLiteCommand(cnn);
                                        getSessionTime.CommandText =
                                            String.Format("select start_time from sessions where session_id = \"{0}\"",
                                                          sessionId);
                                        string startTime;
                                        var getSessionUsername = new SQLiteCommand(cnn);
                                        getSessionUsername.CommandText =
                                            String.Format("select username from sessions where session_id = \"{0}\"",
                                                          sessionId);
                                        string username;
                                        var getSessionPassword = new SQLiteCommand(cnn);
                                        getSessionPassword.CommandText =
                                            String.Format("select password from sessions where session_id = \"{0}\"",
                                                          sessionId);
                                        string password;
                                        var getSessionDeviceName = new SQLiteCommand(cnn);
                                        getSessionDeviceName.CommandText =
                                            String.Format(
                                                "select device_name from sessions where session_id = \"{0}\"", sessionId);
                                        string deviceName;
                                        var getSessionDeviceId = new SQLiteCommand(cnn);
                                        getSessionDeviceId.CommandText =
                                            String.Format("select device_id from sessions where session_id = \"{0}\"",
                                                          sessionId);
                                        string deviceId;
                                        lock (cnn)
                                        {
                                            cnn.Open();
                                            startTime = getSessionTime.ExecuteScalar().ToString();
                                            username = getSessionUsername.ExecuteScalar().ToString();
                                            password = getSessionPassword.ExecuteScalar().ToString();
                                            deviceName = getSessionDeviceName.ExecuteScalar().ToString();
                                            deviceId = getSessionDeviceId.ExecuteScalar().ToString();
                                            cnn.Close();
                                        }
                                        var getIntervals = new SQLiteCommand(cnn);
                                        getIntervals.CommandText =
                                            String.Format("select value from intervals where session_id = \"{0}\"",
                                                          sessionId);
                                        var result = new List<string>();
                                        var intervals = new List<ushort>();
                                        lock (cnn)
                                        {
                                            cnn.Open();
                                            SQLiteDataReader values = getIntervals.ExecuteReader();
                                            while (values.Read())
                                            {
                                                result.Add(values.GetString(values.GetOrdinal("value")));
                                            }
                                            values.Close();
                                            cnn.Close();
                                        }
                                        if (result.Count != 0)
                                        {
                                            foreach (string value in result)
                                            {
                                                intervals.AddRange(
                                                    value.Split(' ').Where(x => x != "").Select(UInt16.Parse).ToList());
                                            }
                                        }
                                        DateTime time = DateTime.ParseExact(startTime, "yyyy-MM-dd HH:mm:ss.fff",
                                                                            CultureInfo.InvariantCulture);
                                        var device = new BLEDevice(deviceId);
                                        device.Name = deviceName;
                                        var session = new BLESession(time, device, new BLEUser(username, password),
                                                                     intervals);
                                        HttpWebResponse resp =
                                            BLEJson.SendJson(BLEJson.MakeIntervalsJson(session, session.StartTime, 1),
                                                             "http://reshaka.ru:8080/BaseProjectWeb/faces/sync");
                                        Stream responseStream = resp.GetResponseStream();
                                        string response = new StreamReader(responseStream).ReadToEnd();
                                        responseStream.Close();
                                        if (response.Equals("ok"))
                                        {
                                            var deleteSession = new SQLiteCommand(cnn);
                                            deleteSession.CommandText =
                                                String.Format("delete from sessions where session_id = \"{0}\"",
                                                              sessionId);
                                            lock (cnn)
                                            {
                                                cnn.Open();
                                                deleteSession.ExecuteNonQuery();
                                                cnn.Close();
                                            }
                                            deleteSession.CommandText =
                                                String.Format("delete from intervals where session_id = \"{0}\"",
                                                              sessionId);
                                            lock (cnn)
                                            {
                                                cnn.Open();
                                                deleteSession.ExecuteNonQuery();
                                                cnn.Close();
                                            }
                                        }
                                    }

                                    lock (cnn)
                                    {
                                        var clear = new SQLiteCommand(cnn);
                                        cnn.Open();
                                        clear.CommandText = "vacuum";
                                        clear.ExecuteNonQuery();
                                        cnn.Close();
                                    }
                                }
                                Thread.Sleep(500);
                            }
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine(ex.Message);
                        }
                    };
                bw.RunWorkerAsync();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }
    }
}