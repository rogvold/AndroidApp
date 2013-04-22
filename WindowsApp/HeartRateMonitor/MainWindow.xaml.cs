using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.ComponentModel;
using System.IO;
using System.Threading;
using System.Windows;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Threading;
using BLELib;
using HeartRateMonitor.Properties;

namespace HeartRateMonitor
{
    public partial class MainWindow
    {
        private List<ushort> RRsToSend;

        private int _create;
        private BLESession _currentSession;
        private DateTime _startTime;

        private BLEListener _listener;
        private BLEUser _connectedUser;

        public MainWindow()
        {
            InitializeComponent();
            UsernameField.ItemsSource = Settings.Default.UserNames;
            if (!File.Exists("local.s3db"))
            {
                BLEOffline.CreateDB();
            }
            BLEOffline.SendSavedIntervals();
            _listener = new BLEListener();
            DeviceList.ItemsSource = _listener.DevList.Devices;
            _listener.PropertyChanged += ListenerPropertyChanged;
            _listener.DataUpdated += UpdateUi;
            _listener.DevicesDiscovered += DevicesDiscovered;
            _listener.Disconnected += Disconnected;
        }

        private void Disconnected(object sender, EventArgs e)
        {
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart)(() => DeviceList.Items.Refresh()));
            Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                       (ThreadStart)
                                       (() => DisconnectButton.RaiseEvent(new RoutedEventArgs(ButtonBase.ClickEvent))));
        }

        private void DevicesDiscovered(object sender, EventArgs e)
        {
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart)(() => DeviceList.Items.Refresh()));
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart)(() => ConnectButton.IsEnabled = true));
        }

        private void ListenerPropertyChanged(object sender, PropertyChangedEventArgs eventArgs)
        {
            if (eventArgs.PropertyName.Equals("DongleName"))
            {
                Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart)(() => DongleName.Content = _listener.DongleName));
            }
            if (eventArgs.PropertyName.Equals("ConnectedDeviceName"))
            {
                Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                      (ThreadStart)(() => ConnectedSensor.Content = _listener.ConnectedDeviceName));
                _currentSession = new BLESession(_startTime, _listener.ConnectedDevice, _connectedUser, new List<ushort>());
            }
        }

        private void ConnectButtonPressed(object sender, RoutedEventArgs e)
        {
            var d = (BLEDevice) DeviceList.SelectedItem;
            if (d == null) return;
            _startTime = DateTime.Now;
            _listener.ConnectDevice(d);
            RRsToSend = new List<ushort>();
            _create = 1;
            Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                   (ThreadStart) (() => DisconnectButton.IsEnabled = true));
            Dispatcher.BeginInvoke(DispatcherPriority.Normal,
                                   (ThreadStart) (() => ConnectButton.IsEnabled = false));
        }

        private void DisconnectButtonPressed(object sender, RoutedEventArgs e)
        {
            _listener.DisconnectDevice();
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

        private void UpdateUi(object sender, EventArgs eventArgs)
        {
            byte[] reportData = ((UpdateEventArgs)eventArgs).Data;
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
            Dispatcher.BeginInvoke(DispatcherPriority.Normal, (ThreadStart) (() => HeartRate.Content = bpm));
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
                _listener.Stop();
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
                _listener.ConnectDongle();
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