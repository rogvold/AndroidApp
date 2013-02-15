using System;
using System.ComponentModel;
using System.IO.Ports;
using System.Linq;
using System.Text;
using BGAPI;

namespace BLELib
{
    public class BLEListener : BGAPIListener, INotifyPropertyChanged
    {
        public event PropertyChangedEventHandler PropertyChanged;
        public event EventHandler Disconnected;
        public event EventHandler DataUpdated;
        public event EventHandler DevicesDiscovered;

        private SerialPort _port;
        private BGAPI.BGAPI bgapi;
        private int _connection = -1;

        private BLEDevice _bledevice;

        public BLEDevice ConnectedDevice
        {
            get { return _bledevice; }
        }

        public BLEDeviceList DevList = new BLEDeviceList();

        private SerialPort Port
        {
            get { return _port; }
            set
            {
                if (value != null)
                    _port = value;
            }
        }

        private string _dongleName;

        public string DongleName
        {
            get { return _dongleName; }
            set
            {
                _dongleName = value;
                OnPropertyChanged("DongleName");
            }
        }

        private string _connectedDeviceName;

        public string ConnectedDeviceName
        {
            get { return _connectedDeviceName; }
            set
            {
                _connectedDeviceName = value;
                OnPropertyChanged("ConnectedDeviceName");
            }
        }

        protected void OnPropertyChanged(string name)
        {
            PropertyChangedEventHandler handler = PropertyChanged;
            if (handler != null)
            {
                handler(this, new PropertyChangedEventArgs(name));
            }
        }

        protected void OnDisconnected()
        {
            EventHandler handler = Disconnected;
            if (handler != null)
            {
                handler(this, new EventArgs());
            }
        }

        protected void OnUpdateData(byte[] value)
        {
            EventHandler handler = DataUpdated;
            if (handler != null)
            {
                handler(this, new UpdateEventArgs(value));
            }
        }

        protected void OnDevicesDiscover()
        {
            EventHandler handler = DevicesDiscovered;
            if (handler != null)
            {
                handler(this, new EventArgs());
            }
        }

        public void receive_system_reset()
        {
            
        }

        public void receive_system_hello()
        {
            
        }

        public void receive_system_address_get(BDAddr address)
        {
            
        }

        public void receive_system_reg_write(int result)
        {
            
        }

        public void receive_system_reg_read(int address, int value)
        {
            
        }

        public void receive_system_get_counters(int txok, int txretry, int rxok, int rxfail)
        {
            
        }

        public void receive_system_get_connections(int maxconn)
        {
            
        }

        public void receive_system_read_memory(int address, byte[] data)
        {
            
        }

        public void receive_system_get_info(int major, int minor, int patch, int build, int ll_version, int protocol_version, int hw)
        {
            DongleName = "BLED112:" + major + "." + minor + "." + patch + " (" + build + ") " + "ll=" + ll_version +
                          " hw=" + hw;
            DevList.Clear();
            bgapi.send_gap_set_scan_parameters(10, 250, 1);
            bgapi.send_gap_discover(1);
        }

        public void receive_system_endpoint_tx()
        {
            
        }

        public void receive_system_whitelist_append(int result)
        {
            
        }

        public void receive_system_whitelist_remove(int result)
        {
            
        }

        public void receive_system_whitelist_clear()
        {
            
        }

        public void receive_system_boot(int major, int minor, int patch, int build, int ll_version, int protocol_version, int hw)
        {
            
        }

        public void receive_system_debug(byte[] data)
        {
            
        }

        public void receive_system_endpoint_rx(int endpoint, byte[] data)
        {
            
        }

        public void receive_flash_ps_defrag()
        {
            
        }

        public void receive_flash_ps_dump()
        {
            
        }

        public void receive_flash_ps_erase_all()
        {
            
        }

        public void receive_flash_ps_save(int result)
        {
            
        }

        public void receive_flash_ps_load(int result, byte[] value)
        {
            
        }

        public void receive_flash_ps_erase()
        {
            
        }

        public void receive_flash_erase_page(int result)
        {
            
        }

        public void receive_flash_write_words()
        {
            
        }

        public void receive_flash_ps_key(int key, byte[] value)
        {
            
        }

        public void receive_attributes_write(int result)
        {
            
        }

        public void receive_attributes_read(int handle, int offset, int result, byte[] value)
        {
            
        }

        public void receive_attributes_read_type(int handle, int result, byte[] value)
        {
            
        }

        public void receive_attributes_user_response()
        {
            
        }

        public void receive_attributes_value(int connection, int reason, int handle, int offset, byte[] value)
        {
            Console.WriteLine("Attribute Value att=" + handle.ToString("X") + " val = " + ByteUtils.BytesToString(value));   
        }

        public void receive_attributes_user_request(int connection, int handle, int offset)
        {
            
        }

        public void receive_connection_disconnect(int connection, int result)
        {
            
        }

        public void receive_connection_get_rssi(int connection, int rssi)
        {
            
        }

        public void receive_connection_update(int connection, int result)
        {
            
        }

        public void receive_connection_version_update(int connection, int result)
        {
            
        }

        public void receive_connection_channel_map_get(int connection, byte[] map)
        {
            
        }

        public void receive_connection_channel_map_set(int connection, int result)
        {
            
        }

        public void receive_connection_features_get(int connection, int result)
        {
            
        }

        public void receive_connection_get_status(int connection)
        {
            
        }

        public void receive_connection_raw_tx(int connection)
        {
            
        }

        public void receive_connection_status(int connection, int flags, BDAddr address, int address_type, int conn_interval, int timeout, int latency, int bonding)
        {
            if (flags != 0)
            {
                _bledevice = DevList.GetFromAddress(address.ToString());
                _connection = connection;
                ConnectedDeviceName = _bledevice.Name;
                bgapi.send_attclient_read_by_handle(_connection, 0x24);
            }
            else
            {
                Console.WriteLine("Connection lost!");
                _connection = -1;
                _bledevice = null;
            }
        }

        public void receive_connection_version_ind(int connection, int vers_nr, int comp_id, int sub_vers_nr)
        {
            
        }

        public void receive_connection_feature_ind(int connection, byte[] features)
        {
            
        }

        public void receive_connection_raw_rx(int connection, byte[] data)
        {
            
        }

        public void receive_connection_disconnected(int connection, int reason)
        {
            DevList.Clear();
            
            bgapi.send_gap_set_scan_parameters(10, 250, 1);
            bgapi.send_gap_discover(1);
            OnDisconnected();
        }

        public void receive_attclient_find_by_type_value(int connection, int result)
        {
            
        }

        public void receive_attclient_read_by_group_type(int connection, int result)
        {
            
        }

        public void receive_attclient_read_by_type(int connection, int result)
        {
            
        }

        public void receive_attclient_find_information(int connection, int result)
        {
            
        }

        public void receive_attclient_read_by_handle(int connection, int result)
        {
            
        }

        public void receive_attclient_attribute_write(int connection, int result)
        {
            
        }

        public void receive_attclient_write_command(int connection, int result)
        {
            
        }

        public void receive_attclient_reserved()
        {
            
        }

        public void receive_attclient_read_long(int connection, int result)
        {
            
        }

        public void receive_attclient_prepare_write(int connection, int result)
        {
            
        }

        public void receive_attclient_execute_write(int connection, int result)
        {
            
        }

        public void receive_attclient_read_multiple(int connection, int result)
        {
            
        }

        public void receive_attclient_indicated(int connection, int attrhandle)
        {
            
        }

        public void receive_attclient_procedure_completed(int connection, int result, int chrhandle)
        {
            
        }

        public void receive_attclient_group_found(int connection, int start, int end, byte[] uuid)
        {
            
        }

        public void receive_attclient_attribute_found(int connection, int chrdecl, int value, int properties, byte[] uuid)
        {
            
        }

        public void receive_attclient_find_information_found(int connection, int chrhandle, byte[] uuid)
        {
            
        }

        public void receive_attclient_attribute_value(int connection, int atthandle, int type, byte[] value)
        {
            Console.WriteLine("Attclient Value att=" + atthandle.ToString("X") + " val = " +
                              ByteUtils.BytesToString(value));
            if (atthandle.ToString("X").Equals("24"))
                bgapi.send_attclient_attribute_write(connection, 18, new byte[] { 1, 0 });
            if (atthandle.ToString("X").Equals("11"))
            {
                OnUpdateData(value);
            }
        }

        public void receive_attclient_read_multiple_response(int connection, byte[] handles)
        {
            
        }

        public void receive_sm_encrypt_start(int handle, int result)
        {
            
        }

        public void receive_sm_set_bondable_mode()
        {
            
        }

        public void receive_sm_delete_bonding(int result)
        {
            
        }

        public void receive_sm_set_parameters()
        {
            
        }

        public void receive_sm_passkey_entry(int result)
        {
            
        }

        public void receive_sm_get_bonds(int bonds)
        {
            
        }

        public void receive_sm_set_oob_data()
        {
            
        }

        public void receive_sm_smp_data(int handle, int packet, byte[] data)
        {
            
        }

        public void receive_sm_bonding_fail(int handle, int result)
        {
            
        }

        public void receive_sm_passkey_display(int handle, int passkey)
        {
            
        }

        public void receive_sm_passkey_request(int handle)
        {
            
        }

        public void receive_sm_bond_status(int bond, int keysize, int mitm, int keys)
        {
            
        }

        public void receive_gap_set_privacy_flags()
        {
            
        }

        public void receive_gap_set_mode(int result)
        {
            
        }

        public void receive_gap_discover(int result)
        {
            
        }

        public void receive_gap_connect_direct(int result, int connection_handle)
        {
            
        }

        public void receive_gap_end_procedure(int result)
        {
            
        }

        public void receive_gap_connect_selective(int result, int connection_handle)
        {
            
        }

        public void receive_gap_set_filtering(int result)
        {
            
        }

        public void receive_gap_set_scan_parameters(int result)
        {
            
        }

        public void receive_gap_set_adv_parameters(int result)
        {
            
        }

        public void receive_gap_set_adv_data(int result)
        {
            
        }

        public void receive_gap_set_directed_connectable_mode(int result)
        {
            
        }

        public void receive_gap_scan_response(int rssi, int packet_type, BDAddr sender, int address_type, int bond, byte[] data)
        {
            var d = DevList.GetFromAddress(sender.ToString());
            if (d == null)
            {
                d = new BLEDevice(sender.ToString());
                DevList.Add(d);
                Console.WriteLine("Create device: " + d);
            }
            var name = Encoding.ASCII.GetString(data).Trim();
            name = name.Replace("\t", String.Empty);
            name = name.Replace("\n", String.Empty);
            name = name.Substring(1);
            if (!name.Contains('\0'))
                d.Name = name;
            d.Rssi = rssi;
            OnDevicesDiscover();
        }

        public void receive_gap_mode_changed(int discover, int connect)
        {
            
        }

        public void receive_hardware_io_port_config_irq(int result)
        {
            
        }

        public void receive_hardware_set_soft_timer(int result)
        {
            
        }

        public void receive_hardware_adc_read(int result)
        {
            
        }

        public void receive_hardware_io_port_config_direction(int result)
        {
            
        }

        public void receive_hardware_io_port_config_function(int result)
        {
            
        }

        public void receive_hardware_io_port_config_pull(int result)
        {
            
        }

        public void receive_hardware_io_port_write(int result)
        {
            
        }

        public void receive_hardware_io_port_read(int result, int port, int data)
        {
            
        }

        public void receive_hardware_spi_config(int result)
        {
            
        }

        public void receive_hardware_spi_transfer(int result, int channel, byte[] data)
        {
            
        }

        public void receive_hardware_i2c_read(int result, byte[] data)
        {
            
        }

        public void receive_hardware_i2c_write(int written)
        {
            
        }

        public void receive_hardware_set_txpower()
        {
            
        }

        public void receive_hardware_io_port_status(int timestamp, int port, int irq, int state)
        {
            
        }

        public void receive_hardware_soft_timer(int handle)
        {
            
        }

        public void receive_hardware_adc_result(int input, int value)
        {
            
        }

        public void receive_test_phy_tx()
        {
            
        }

        public void receive_test_phy_rx()
        {
            
        }

        public void receive_test_phy_end(int counter)
        {
            
        }

        public void receive_test_phy_reset()
        {
            
        }

        public void receive_test_get_channel_map(byte[] channel_map)
        {
            
        }

        public void ConnectDongle()
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

        public void ConnectDevice(BLEDevice device)
        {
            bgapi.send_gap_connect_direct(BDAddr.fromString(device.Address), 0, 0x3C, 0x3C, 0x64, 0);
        }

        public void DisconnectDevice()
        {
            _bledevice = null;
            if (_connection >= 0)
            {
                bgapi.send_connection_disconnect(_connection);
            }
        }

        public void Stop()
        {
            bgapi.send_connection_disconnect(_connection);
            bgapi.send_gap_end_procedure();
            bgapi.disconnect();
            Port.Close();
        }
    }

    public class UpdateEventArgs : EventArgs
    {

        private byte[] _data;

        public UpdateEventArgs(byte[] data)
        {
            this._data = data;
        }

        public byte[] Data
        {
            get { return _data; }
            set { this._data = value; }
        }
    }
}
