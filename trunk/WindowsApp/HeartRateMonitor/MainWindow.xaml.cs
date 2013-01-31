using BGAPI;
using System;
using System.Collections.Generic;
using System.IO.Ports;
using System.Linq;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Threading;
using System.IO;
using System.ComponentModel;
using System.Windows.Threading;
using System.Globalization;
using Newtonsoft.Json;
using System.Net;
using System.Windows.Forms;
using System.Net.NetworkInformation;
using System.Data.SQLite;

namespace HeartRateMonitor
{
    /// <summary>
    /// Логика взаимодействия для MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window, BGAPIListener
    {
        private SerialPort _port;

        public SerialPort Port
        {
            get
            {
                return _port;
            }
            set
            {
                if (value != null)
                    _port = value;
            }
        }

        private DateTime startTime;

        private List<ushort> RRsToSend;

        private int create;

        private BLEDevice connectedDevice;

        private string dbConnection;

        private string Username;
        private string Password;

        protected BGAPI.BGAPI bgapi;
        protected internal BLEDeviceList devList = new BLEDeviceList();

        public MainWindow()
        {
            InitializeComponent();
            deviceList.ItemsSource = devList.Devices;
            //if (Properties.Settings.Default.LastUserName != null)
            //    usernameField.Text = Properties.Settings.Default.LastUserName;
            usernameField.ItemsSource = Properties.Settings.Default.UserNames;
            dbConnection = "Data Source=local.s3db";
            if (IsConnectedToInternet)
            {
                SendSavedIntervals();
            }
        }

        private void OpenPortButtonClick(object sender, RoutedEventArgs e)
        {
            //var window = new ChoosePortWindow();
            //window.Closed += ChoosePortWindowClosed;
            //window.Show();
        }

        private void ConnectDongle()
        {
            string[] ports = SerialPort.GetPortNames();
            foreach (string portName in ports)
            {
                try
                {
                    SerialPort port = new SerialPort(portName);
                    bgapi = new BGAPI.BGAPI(new BGAPITransport(port));
                    bgapi.addListener(this);
                    bgapi.send_system_get_info();
                    this.Port = port;
                    startTime = DateTime.Now;
                    RRsToSend = new List<ushort>();
                    create = 1;
                    break;
                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex.Message);
                }
            }
        }

        private const int IDLE = 0;
        private const int SERVICES = 1;
        private const int ATTRIBUTES = 2;
        private IEnumerator<BLEService> discovery_it = null;
        private BLEService discovery_srv = null;
        private int discovery_state = IDLE;

        // Callbacks for class system (index = 0)
        public virtual void receive_system_reset()
        {
        }
        public virtual void receive_system_hello()
        {
        }
        public virtual void receive_system_address_get(BDAddr address)
        {
        }
        public virtual void receive_system_reg_write(int result)
        {
        }
        public virtual void receive_system_reg_read(int address, int value)
        {
        }
        public virtual void receive_system_get_counters(int txok, int txretry, int rxok, int rxfail)
        {
        }
        public virtual void receive_system_get_connections(int maxconn)
        {
        }
        public virtual void receive_system_read_memory(int address, byte[] data)
        {
        }
        public virtual void receive_system_get_info(int major, int minor, int patch, int build, int ll_version, int protocol_version, int hw)
        {
            string name = "BLED112:" + major + "." + minor + "." + patch + " (" + build + ") " + "ll=" + ll_version + " hw=" + hw;
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart)delegate { dongleName.Content = name; });
            devList.Clear();
            bgapi.send_gap_set_scan_parameters(10, 250, 1);
            bgapi.send_gap_discover(1);
        }
        public virtual void receive_system_endpoint_tx()
        {
        }
        public virtual void receive_system_whitelist_append(int result)
        {
        }
        public virtual void receive_system_whitelist_remove(int result)
        {
        }
        public virtual void receive_system_whitelist_clear()
        {
        }
        public virtual void receive_system_boot(int major, int minor, int patch, int build, int ll_version, int protocol_version, int hw)
        {
        }
        public virtual void receive_system_debug(byte[] data)
        {
        }
        public virtual void receive_system_endpoint_rx(int endpoint, byte[] data)
        {
        }

        // Callbacks for class flash (index = 1)
        public virtual void receive_flash_ps_defrag()
        {
        }
        public virtual void receive_flash_ps_dump()
        {
        }
        public virtual void receive_flash_ps_erase_all()
        {
        }
        public virtual void receive_flash_ps_save(int result)
        {
        }
        public virtual void receive_flash_ps_load(int result, byte[] value)
        {
        }
        public virtual void receive_flash_ps_erase()
        {
        }
        public virtual void receive_flash_erase_page(int result)
        {
        }
        public virtual void receive_flash_write_words()
        {
        }
        public virtual void receive_flash_ps_key(int key, byte[] value)
        {
        }

        // Callbacks for class attributes (index = 2)
        public virtual void receive_attributes_write(int result)
        {
        }
        public virtual void receive_attributes_read(int handle, int offset, int result, byte[] value)
        {
        }
        public virtual void receive_attributes_read_type(int handle, int result, byte[] value)
        {
        }
        public virtual void receive_attributes_user_response()
        {
        }
        public virtual void receive_attributes_value(int connection, int reason, int handle, int offset, byte[] value)
        {
            Console.WriteLine("Attribute Value att=" + handle.ToString("X") + " val = " + bytesToString(value));
        }
        public virtual void receive_attributes_user_request(int connection, int handle, int offset)
        {
        }

        // Callbacks for class connection (index = 3)
        public virtual void receive_connection_disconnect(int connection, int result)
        {
        }
        public virtual void receive_connection_get_rssi(int connection, int rssi)
        {
        }
        public virtual void receive_connection_update(int connection, int result)
        {
        }
        public virtual void receive_connection_version_update(int connection, int result)
        {
        }
        public virtual void receive_connection_channel_map_get(int connection, byte[] map)
        {
        }
        public virtual void receive_connection_channel_map_set(int connection, int result)
        {
        }
        public virtual void receive_connection_features_get(int connection, int result)
        {
        }
        public virtual void receive_connection_get_status(int connection)
        {
        }
        public virtual void receive_connection_raw_tx(int connection)
        {
        }

        protected internal int connection = -1;
        protected internal BLEDevice bledevice = null;
        public virtual void receive_connection_status(int conn, int flags, BDAddr address, int address_type, int conn_interval, int timeout, int latency, int bonding)
        {
            if (flags != 0)
            {
                bledevice = devList.GetFromAddress(address.ToString());
                this.connection = conn;
                connectedDevice = bledevice;
                Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart)delegate { connectedSensor.Content = bledevice.Name; });
                bgapi.send_attclient_read_by_handle(connection, 0x24);
            }
            else
            {
                Console.WriteLine("Connection lost!");
                connection = -1;
                bledevice = null;
                //jButtonConnect.Enabled = true;
                //jButtonDisconnect.Enabled = false;
            }
        }
        public virtual void receive_connection_version_ind(int connection, int vers_nr, int comp_id, int sub_vers_nr)
        {
        }
        public virtual void receive_connection_feature_ind(int connection, byte[] features)
        {
        }
        public virtual void receive_connection_raw_rx(int connection, byte[] data)
        {
        }
        public virtual void receive_connection_disconnected(int connection, int reason)
        {
        }

        // Callbacks for class attclient (index = 4)
        public virtual void receive_attclient_find_by_type_value(int connection, int result)
        {
        }
        public virtual void receive_attclient_read_by_group_type(int connection, int result)
        {
        }
        public virtual void receive_attclient_read_by_type(int connection, int result)
        {
        }
        public virtual void receive_attclient_find_information(int connection, int result)
        {
        }
        public virtual void receive_attclient_read_by_handle(int connection, int result)
        {
        }
        public virtual void receive_attclient_attribute_write(int connection, int result)
        {
        }
        public virtual void receive_attclient_write_command(int connection, int result)
        {
        }
        public virtual void receive_attclient_reserved()
        {
        }
        public virtual void receive_attclient_read_long(int connection, int result)
        {
        }
        public virtual void receive_attclient_prepare_write(int connection, int result)
        {
        }
        public virtual void receive_attclient_execute_write(int connection, int result)
        {
        }
        public virtual void receive_attclient_read_multiple(int connection, int result)
        {
        }
        public virtual void receive_attclient_indicated(int connection, int attrhandle)
        {
        }
        public virtual void receive_attclient_procedure_completed(int connection, int result, int chrhandle)
        {
            if (discovery_state != IDLE && bledevice != null)
            {
                if (discovery_state == SERVICES) // services have been discovered
                {
                    discovery_it = bledevice.Services.Values.GetEnumerator();
                    discovery_state = ATTRIBUTES;
                }
                if (discovery_state == ATTRIBUTES)
                {
                    if (discovery_it.MoveNext())
                    {
                        discovery_srv = discovery_it.Current;
                        bgapi.send_attclient_find_information(connection, discovery_srv.Start, discovery_srv.End);
                    }
                    else // Discovery is done
                    {
                        Console.WriteLine("Discovery completed:");
                        Console.WriteLine(bledevice.GATTDescription);
                        discovery_state = IDLE;
                    }
                }
            }
            if (result != 0)
            {
                Console.WriteLine("ERROR: Attribute Procedure Completed with error code 0x" + result.ToString("X"));
            }
        }
        public virtual void receive_attclient_group_found(int connection, int start, int end, byte[] uuid)
        {
            if (bledevice != null)
            {
                BLEService srv = new BLEService(uuid, start, end);
                bledevice.Services.Add(srv.UuidString, srv);
            }
        }
        public virtual void receive_attclient_attribute_found(int connection, int chrdecl, int value, int properties, byte[] uuid)
        {
        }
        public virtual void receive_attclient_find_information_found(int connection, int chrhandle, byte[] uuid)
        {
            if (discovery_state == ATTRIBUTES && discovery_srv != null)
            {
                BLEAttribute att = new BLEAttribute(uuid, chrhandle);
                discovery_srv.Attributes.Add(att);
            }
        }
        public virtual void receive_attclient_attribute_value(int connection, int atthandle, int type, byte[] value)
        {
            Console.WriteLine("Attclient Value att=" + atthandle.ToString("X") + " val = " + bytesToString(value));
            if (atthandle.ToString("X").Equals("24"))
                bgapi.send_attclient_attribute_write(connection, 18, new byte[] {1, 0});
            if (atthandle.ToString("X").Equals("11"))
            {
                updateUI(value);
            }
        }
        public virtual void receive_attclient_read_multiple_response(int connection, byte[] handles)
        {
        }

        // Callbacks for class sm (index = 5)
        public virtual void receive_sm_encrypt_start(int handle, int result)
        {
        }
        public virtual void receive_sm_set_bondable_mode()
        {
        }
        public virtual void receive_sm_delete_bonding(int result)
        {
        }
        public virtual void receive_sm_set_parameters()
        {
        }
        public virtual void receive_sm_passkey_entry(int result)
        {
        }
        public virtual void receive_sm_get_bonds(int bonds)
        {
        }
        public virtual void receive_sm_set_oob_data()
        {
        }
        public virtual void receive_sm_smp_data(int handle, int packet, byte[] data)
        {
        }
        public virtual void receive_sm_bonding_fail(int handle, int result)
        {
        }
        public virtual void receive_sm_passkey_display(int handle, int passkey)
        {
        }
        public virtual void receive_sm_passkey_request(int handle)
        {
        }
        public virtual void receive_sm_bond_status(int bond, int keysize, int mitm, int keys)
        {
        }

        // Callbacks for class gap (index = 6)
        public virtual void receive_gap_set_privacy_flags()
        {
        }
        public virtual void receive_gap_set_mode(int result)
        {
        }
        public virtual void receive_gap_discover(int result)
        {

        }
        public virtual void receive_gap_connect_direct(int result, int connection_handle)
        {
        }
        public virtual void receive_gap_end_procedure(int result)
        {
        }
        public virtual void receive_gap_connect_selective(int result, int connection_handle)
        {
        }
        public virtual void receive_gap_set_filtering(int result)
        {
        }
        public virtual void receive_gap_set_scan_parameters(int result)
        {
        }
        public virtual void receive_gap_set_adv_parameters(int result)
        {
        }
        public virtual void receive_gap_set_adv_data(int result)
        {
        }
        public virtual void receive_gap_set_directed_connectable_mode(int result)
        {
        }
        public virtual void receive_gap_scan_response(int rssi, int packet_type, BDAddr sender, int address_type, int bond, byte[] data)
        {
            BLEDevice d = devList.GetFromAddress(sender.ToString());
            if (d == null)
            {
                d = new BLEDevice(sender.ToString());
                devList.Add(d);
                Console.WriteLine("Create device: " + d.ToString());
            }
            string name = System.Text.Encoding.ASCII.GetString(data).Trim();
            name = name.Replace("\t", String.Empty);
            name = name.Replace("\n", String.Empty);
            name = name.Substring(1);
            if (!name.Contains('\0'))
                d.Name = name;
            d.Rssi = rssi;
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart)delegate{ deviceList.Items.Refresh(); });
        }
        public virtual void receive_gap_mode_changed(int discover, int connect)
        {
        }

        // Callbacks for class hardware (index = 7)
        public virtual void receive_hardware_io_port_config_irq(int result)
        {
        }
        public virtual void receive_hardware_set_soft_timer(int result)
        {
        }
        public virtual void receive_hardware_adc_read(int result)
        {
        }
        public virtual void receive_hardware_io_port_config_direction(int result)
        {
        }
        public virtual void receive_hardware_io_port_config_function(int result)
        {
        }
        public virtual void receive_hardware_io_port_config_pull(int result)
        {
        }
        public virtual void receive_hardware_io_port_write(int result)
        {
        }
        public virtual void receive_hardware_io_port_read(int result, int port, int data)
        {
        }
        public virtual void receive_hardware_spi_config(int result)
        {
        }
        public virtual void receive_hardware_spi_transfer(int result, int channel, byte[] data)
        {
        }
        public virtual void receive_hardware_i2c_read(int result, byte[] data)
        {
        }
        public virtual void receive_hardware_i2c_write(int written)
        {
        }
        public virtual void receive_hardware_set_txpower()
        {
        }
        public virtual void receive_hardware_io_port_status(int timestamp, int port, int irq, int state)
        {
        }
        public virtual void receive_hardware_soft_timer(int handle)
        {
        }
        public virtual void receive_hardware_adc_result(int input, int value)
        {
        }

        // Callbacks for class test (index = 8)
        public virtual void receive_test_phy_tx()
        {
        }
        public virtual void receive_test_phy_rx()
        {
        }
        public virtual void receive_test_phy_end(int counter)
        {
        }
        public virtual void receive_test_phy_reset()
        {
        }
        public virtual void receive_test_get_channel_map(byte[] channel_map)
        {
        }

        public virtual string bytesToString(byte[] bytes)
        {
            StringBuilder result = new StringBuilder();
            result.Append("[ ");
            foreach (byte b in bytes)
                result.Append((b & 0xFF).ToString("X") + " ");
            result.Append("]");
            return result.ToString();
        }

        private void DiscoverButtonPressed(object sender, RoutedEventArgs e)
        {
            devList.Clear();
            bgapi.send_gap_set_scan_parameters(10, 250, 1);
            bgapi.send_gap_discover(1);
        }

        private void StopDiscoverButtonPressed(object sender, RoutedEventArgs e)
        {
            bgapi.send_gap_end_procedure();
        }

        private void ConnectButtonPressed(object sender, RoutedEventArgs e)
        {
            BLEDevice d = (BLEDevice) deviceList.SelectedItem;
            if (d == null) return;
            bgapi.send_gap_connect_direct(BDAddr.fromString(d.Address), 0, 0x3C, 0x3C, 0x64, 0);
        }

        private void DisconnectButtonPressed(object sender, RoutedEventArgs e)
        {
            bledevice = null;
            if (connection >= 0)
            {
                bgapi.send_connection_disconnect(connection);
            }
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart)delegate { connectedSensor.Content = ""; });
        }

        private void updateUI(byte[] reportData)
        {
            ushort bpm = 0;
            // n of byte containing rr
            int rrByte = 2;
            if ((reportData[0] & 0x01) == 0) 
            {
                /* uint8 bpm */
                bpm = reportData[1];
            } 
            else 
            {
                /* uint16 bpm */
                byte[] bytes = new byte[2];
                bytes[0] = reportData[0];
                bytes[1] = reportData[1];
                /*if (BitConverter.IsLittleEndian)
                    Array.Reverse(bytes);*/
                bpm = BitConverter.ToUInt16(bytes, 0);
                rrByte++;
            }
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart)delegate { heartRate.Content = bpm; });
            Console.WriteLine("Heart rate: " + bpm);
            if ((reportData[0] & 0x04) == 1){
                // Energy field is present
                rrByte += 2;
            }
    
            if ((reportData[0] & 0x05) == 0 || reportData.Length <= rrByte) 
            {
                Console.WriteLine("RR intervals aren't present");
            } 
            else
            {
                Console.WriteLine("RR intervals are present");
                List<ushort> rrs = new List<ushort>();
                byte[] rrArray = new byte[2];
                rrArray[0] = reportData[rrByte];
                rrArray[1] = reportData[rrByte + 1];
                /*if (BitConverter.IsLittleEndian)
                    Array.Reverse(rrArray);*/
                ushort rr = (ushort) ((BitConverter.ToUInt16(rrArray, 0) * 1000) / 1024);
                while (rr != 0) {
                    Console.WriteLine("RR: " + rr);
                    rrs.Add(rr);
                    rrByte += 2;
                    if (rrByte >= reportData.Length)
                        break;
                    rrArray[0] = reportData[rrByte];
                    rrArray[1] = reportData[rrByte + 1];
                    /*if (BitConverter.IsLittleEndian)
                        Array.Reverse(rrArray);*/
                    rr = (ushort) ((BitConverter.ToUInt16(rrArray, 0) * 1000) / 1024);
                }
                rrs.Add(rr);
                RRsToSend.AddRange(rrs);
                if (RRsToSend.Count >= 10)
                {
                    if (IsConnectedToInternet)
                    {
                        SendJSON(MakeIntervalsJSON(RRsToSend), "http://reshaka.ru:8080/BaseProjectWeb/faces/input");
                    }
                    else
                    {
                        SaveIntervals(RRsToSend);
                    }
                    RRsToSend.Clear();
                    startTime = DateTime.Now;
                    create = 0;
                }
            }
        }

        private void SaveIntervals(List<ushort> intervals)
        {
            try
            {
                SQLiteConnection cnn = new SQLiteConnection(dbConnection);
                cnn.Open();
                SQLiteCommand jsonInsert = new SQLiteCommand(cnn);
                jsonInsert.CommandText = String.Format("insert into Jsons (json_string) values(\"{0}\")", ByteUtils.bytesToString(System.Text.Encoding.UTF8.GetBytes(MakeIntervalsJSON(intervals))));
                object user_id = jsonInsert.ExecuteScalar();
                cnn.Close();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        private void SendSavedIntervals()
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
                        SendJSON(System.Text.Encoding.UTF8.GetString(ByteUtils.bytesFromString(json)), "http://reshaka.ru:8080/BaseProjectWeb/faces/input");
                        Thread.Sleep(100);
                    }

                    SQLiteCommand delete = new SQLiteCommand(cnn);
                    delete.CommandText = "delete from Jsons";
                    object deleted = delete.ExecuteNonQuery();

                    if ((int)deleted != result.Count)
                        throw new Exception("Something wrong");

                    cnn.Close();
                });
                bw.RunWorkerAsync();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        private HttpWebResponse SendJSON(string json, string url)
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

        private string MakeIntervalsJSON(List<ushort> intervals)
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

        private void WindowClosed(object sender, EventArgs e)
        {
            try
            {
                bgapi.send_connection_disconnect(connection);
                bgapi.send_gap_end_procedure();
                bgapi.disconnect();
                this.Port.Close();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        private void SignInButtonPressed(object sender, RoutedEventArgs e)
        {
            this.Username = this.usernameField.Text;
            this.Password = this.passwordField.Password;
            int exists = 1;
            int passwordIsCorrect = 1;
            if (IsConnectedToInternet)
            {
                exists = UserExists(Username);
                if (exists == 1)
                    passwordIsCorrect = CheckUser(Username, Password);
            }
            if (Properties.Settings.Default.UserNames != null && !Properties.Settings.Default.UserNames.Contains(Username))
                Properties.Settings.Default.UserNames.Add(Username);
            else if (Properties.Settings.Default.UserNames == null)
            {
                Properties.Settings.Default.UserNames = new System.Collections.Specialized.StringCollection();
                Properties.Settings.Default.UserNames.Add(Username);
            }
            Properties.Settings.Default.LastUserName = Username;
            Properties.Settings.Default.Save();
            usernameField.ItemsSource = Properties.Settings.Default.UserNames;
            usernameField.UpdateLayout();
            ConnectDongle();
        }

        private int UserExists(string username)
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

        private int CheckUser(string username, string password)
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
                    PingReply reply = p.Send(host, 3000);
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
