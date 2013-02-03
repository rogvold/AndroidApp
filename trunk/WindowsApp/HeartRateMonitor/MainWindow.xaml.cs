using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.IO;
using System.IO.Ports;
using System.Linq;
using System.Text;
using System.Threading;
using System.Windows;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Threading;
using BGAPI;
using BLELib;
using HeartRateMonitor.Properties;

namespace HeartRateMonitor
{
    public partial class MainWindow : Window, BGAPIListener
    {
        private const int IDLE = 0;
        private const int SERVICES = 1;
        private const int ATTRIBUTES = 2;
        private List<ushort> RRsToSend;
        private SerialPort _port;
        protected BGAPI.BGAPI bgapi;

        private BLEDevice _connectedDevice;
        private BLEDevice _bledevice;

        private BLEUser _connectedUser;
        protected internal int Connection = -1;
        private int _create;
        private BLESession _currentSession;

        protected internal BLEDeviceList DevList = new BLEDeviceList();

        private IEnumerator<BLEService> _discoveryIt;
        private BLEService _discoverySrv;
        private int _discoveryState = IDLE;
        private DateTime _startTime;

        public MainWindow()
        {
            InitializeComponent();
            DeviceList.ItemsSource = DevList.Devices;
            UsernameField.ItemsSource = Settings.Default.UserNames;
            if (!File.Exists("local.s3db"))
            {
                BLEOffline.CreateDB();
            }
            BLEOffline.SendSavedIntervals();
        }

        public SerialPort Port
        {
            get { return _port; }
            set
            {
                if (value != null)
                    _port = value;
            }
        }

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

        public virtual void receive_system_get_info(int major, int minor, int patch, int build, int ll_version,
                                                    int protocol_version, int hw)
        {
            var name = "BLED112:" + major + "." + minor + "." + patch + " (" + build + ") " + "ll=" + ll_version +
                          " hw=" + hw;
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart) (() => DongleName.Content = name));
            DevList.Clear();
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

        public virtual void receive_system_boot(int major, int minor, int patch, int build, int ll_version,
                                                int protocol_version, int hw)
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
            Console.WriteLine("Attribute Value att=" + handle.ToString("X") + " val = " + ByteUtils.BytesToString(value));
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

        public virtual void receive_connection_status(int conn, int flags, BDAddr address, int address_type,
                                                      int conn_interval, int timeout, int latency, int bonding)
        {
            if (flags != 0)
            {
                _bledevice = DevList.GetFromAddress(address.ToString());
                Connection = conn;
                _connectedDevice = _bledevice;
                _currentSession = new BLESession(_startTime, _connectedDevice, _connectedUser, new List<ushort>());
                Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                       (ThreadStart) (() => ConnectedSensor.Content = _bledevice.Name));
                bgapi.send_attclient_read_by_handle(Connection, 0x24);
            }
            else
            {
                Console.WriteLine("Connection lost!");
                Connection = -1;
                _bledevice = null;
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
            Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                   (ThreadStart)
                                   (() => DisconnectButton.RaiseEvent(new RoutedEventArgs(ButtonBase.ClickEvent))));

            DevList.Clear();
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart) (() => DeviceList.Items.Refresh()));

            bgapi.send_gap_set_scan_parameters(10, 250, 1);
            bgapi.send_gap_discover(1);
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
            if (_discoveryState != IDLE && _bledevice != null)
            {
                if (_discoveryState == SERVICES) // services have been discovered
                {
                    _discoveryIt = _bledevice.Services.Values.GetEnumerator();
                    _discoveryState = ATTRIBUTES;
                }
                if (_discoveryState == ATTRIBUTES)
                {
                    if (_discoveryIt.MoveNext())
                    {
                        _discoverySrv = _discoveryIt.Current;
                        bgapi.send_attclient_find_information(connection, _discoverySrv.Start, _discoverySrv.End);
                    }
                    else // Discovery is done
                    {
                        Console.WriteLine("Discovery completed:");
                        Console.WriteLine(_bledevice.GATTDescription);
                        _discoveryState = IDLE;
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
            if (_bledevice != null)
            {
                var srv = new BLEService(uuid, start, end);
                _bledevice.Services.Add(srv.UuidString, srv);
            }
        }

        public virtual void receive_attclient_attribute_found(int connection, int chrdecl, int value, int properties,
                                                              byte[] uuid)
        {
        }

        public virtual void receive_attclient_find_information_found(int connection, int chrhandle, byte[] uuid)
        {
            if (_discoveryState == ATTRIBUTES && _discoverySrv != null)
            {
                var att = new BLEAttribute(uuid, chrhandle);
                _discoverySrv.Attributes.Add(att);
            }
        }

        public virtual void receive_attclient_attribute_value(int connection, int atthandle, int type, byte[] value)
        {
            Console.WriteLine("Attclient Value att=" + atthandle.ToString("X") + " val = " +
                              ByteUtils.BytesToString(value));
            if (atthandle.ToString("X").Equals("24"))
                bgapi.send_attclient_attribute_write(connection, 18, new byte[] {1, 0});
            if (atthandle.ToString("X").Equals("11"))
            {
                UpdateUi(value);
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

        public virtual void receive_gap_scan_response(int rssi, int packet_type, BDAddr sender, int address_type,
                                                      int bond, byte[] data)
        {
            BLEDevice d = DevList.GetFromAddress(sender.ToString());
            if (d == null)
            {
                d = new BLEDevice(sender.ToString());
                DevList.Add(d);
                Console.WriteLine("Create device: " + d);
            }
            string name = Encoding.ASCII.GetString(data).Trim();
            name = name.Replace("\t", String.Empty);
            name = name.Replace("\n", String.Empty);
            name = name.Substring(1);
            if (!name.Contains('\0'))
                d.Name = name;
            d.Rssi = rssi;
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart) (() => DeviceList.Items.Refresh()));
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart) (() => ConnectButton.IsEnabled = true));
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

        private void ConnectDongle()
        {
            string[] ports = SerialPort.GetPortNames();
            foreach (string portName in ports)
            {
                try
                {
                    var port = new SerialPort(portName);
                    bgapi = new BGAPI.BGAPI(new BGAPITransport(port));
                    bgapi.addListener(this);
                    bgapi.send_system_get_info();
                    Port = port;
                    break;
                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex.Message);
                }
            }
        }

        private void ConnectButtonPressed(object sender, RoutedEventArgs e)
        {
            var d = (BLEDevice) DeviceList.SelectedItem;
            if (d == null) return;
            bgapi.send_gap_connect_direct(BDAddr.fromString(d.Address), 0, 0x3C, 0x3C, 0x64, 0);
            _startTime = DateTime.Now;
            RRsToSend = new List<ushort>();
            _create = 1;
            Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                   (ThreadStart) (() => DisconnectButton.IsEnabled = true));
            Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                   (ThreadStart) (() => ConnectButton.IsEnabled = false));
        }

        private void DisconnectButtonPressed(object sender, RoutedEventArgs e)
        {
            _bledevice = null;
            if (Connection >= 0)
            {
                bgapi.send_connection_disconnect(Connection);
            }
            if (RRsToSend.Count > 0)
            {
                _currentSession.Intervals.AddRange(BLEFilter.IntervalFilter(RRsToSend));
                if (BLEOffline.IsConnectedToInternet)
                {
                    BLEJson.SendJson(BLEJson.MakeIntervalsJson(_currentSession, _startTime, _create),
                                     "http://reshaka.ru:8080/BaseProjectWeb/faces/input");
                }
                else
                {
                    BLEOffline.SaveIntervals(_currentSession);
                }
                RRsToSend.Clear();
                _create = 0;
                _startTime = DateTime.Now;
            }
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart) (() => ConnectedSensor.Content = ""));
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart) (() => ConnectButton.IsEnabled = true));
            Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                   (ThreadStart) delegate { DisconnectButton.IsEnabled = false; });
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart) (() => HeartRate.Content = "0"));
        }

        private void UpdateUi(byte[] reportData)
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
                var bytes = new byte[2];
                bytes[0] = reportData[0];
                bytes[1] = reportData[1];
                if (!BitConverter.IsLittleEndian)
                    Array.Reverse(bytes);
                bpm = BitConverter.ToUInt16(bytes, 0);
                rrByte++;
            }
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart) delegate { HeartRate.Content = bpm; });
            Console.WriteLine("Heart rate: " + bpm);
            if ((reportData[0] & 0x04) == 1)
            {
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
                var rrs = new List<ushort>();
                var rrArray = new byte[2];
                rrArray[0] = reportData[rrByte];
                rrArray[1] = reportData[rrByte + 1];
                if (!BitConverter.IsLittleEndian)
                    Array.Reverse(rrArray);
                var rr = (ushort) ((BitConverter.ToUInt16(rrArray, 0)*1000)/1024);
                while (rr != 0)
                {
                    Console.WriteLine("RR: " + rr);
                    rrs.Add(rr);
                    rrByte += 2;
                    if (rrByte >= reportData.Length)
                        break;
                    rrArray[0] = reportData[rrByte];
                    rrArray[1] = reportData[rrByte + 1];
                    if (!BitConverter.IsLittleEndian)
                        Array.Reverse(rrArray);
                    rr = (ushort) ((BitConverter.ToUInt16(rrArray, 0)*1000)/1024);
                }
                RRsToSend.AddRange(rrs);
                if (RRsToSend.Count >= 50)
                {
                    _currentSession.Intervals.AddRange(BLEFilter.IntervalFilter(RRsToSend));
                    if (BLEOffline.IsConnectedToInternet)
                    {
                        BLEJson.SendJson(BLEJson.MakeIntervalsJson(_currentSession, _startTime, _create),
                                         "http://reshaka.ru:8080/BaseProjectWeb/faces/input");
                    }
                    else
                    {
                        BLEOffline.SaveIntervals(_currentSession);
                    }
                    RRsToSend.Clear();
                    _create = 0;
                    _startTime = DateTime.Now;
                }
            }
        }

        private void WindowClosed(object sender, EventArgs e)
        {
            try
            {
                Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                       (ThreadStart)
                                       (() => DisconnectButton.RaiseEvent(new RoutedEventArgs(ButtonBase.ClickEvent))));
                bgapi.send_connection_disconnect(Connection);
                bgapi.send_gap_end_procedure();
                bgapi.disconnect();
                Port.Close();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        private void SignInButtonPressed(object sender, RoutedEventArgs e)
        {
            try
            {
                if (BLEOffline.IsConnectedToInternet)
                {
                    var exists = BLEJson.UserExists(UsernameField.Text);
                    if (exists == 1)
                    {
                        var passwordIsCorrect = BLEJson.CheckUser(UsernameField.Text, PasswordField.Password);
                        if (passwordIsCorrect != 1)
                        {
                            Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                                   (ThreadStart)
                                                   (() => AuthStatus.Content = "Password is incorrect"));
                            Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                                   (ThreadStart) (() => PasswordField.BorderBrush = Brushes.Red));
                            throw new Exception("Incorrect password");
                        }
                    }
                    else
                    {
                        Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                               (ThreadStart) (() => AuthStatus.Content = "Username is incorrect"));

                        Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                               (ThreadStart) (() => UsernameField.BorderBrush = Brushes.Red));
                        throw new Exception("Incorrect username");
                    }
                }
                _connectedUser = new BLEUser(UsernameField.Text, PasswordField.Password);
                ConnectDongle();
                if (Settings.Default.UserNames != null && !Settings.Default.UserNames.Contains(UsernameField.Text))
                    Settings.Default.UserNames.Add(UsernameField.Text);
                else if (Settings.Default.UserNames == null)
                {
                    Settings.Default.UserNames = new StringCollection {UsernameField.Text};
                }
                Settings.Default.LastUserName = UsernameField.Text;
                Settings.Default.Save();
                Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                       (ThreadStart) (() => SignInButton.IsEnabled = false));
                Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                       (ThreadStart) (() => AuthStatus.Content = "Success"));
                Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                       (ThreadStart) (() => UsernameField.IsEnabled = false));
                Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                       (ThreadStart) (() => PasswordField.IsEnabled = false));
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        private void PasswordFieldKeyUp(object sender, KeyEventArgs e)
        {
            var val = (char) KeyInterop.VirtualKeyFromKey(e.Key);
            if (e.Key == Key.Enter)
            {
                if (UsernameField.Text != null && PasswordField.Password != null)
                {
                    Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                           (ThreadStart)
                                           (() => SignInButton.RaiseEvent(new RoutedEventArgs(ButtonBase.ClickEvent))));
                }
            }
            else if (char.IsLetterOrDigit((char) KeyInterop.VirtualKeyFromKey(e.Key)))
            {
                if (UsernameField.Text != null && PasswordField.Password != null)
                {
                    Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                           (ThreadStart) (() => SignInButton.IsEnabled = true));
                }
            }
        }
    }
}