using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace BGAPI
{
    public class BGAPIDefaultListener : BGAPIListener
    {
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
        public virtual void receive_connection_status(int connection, int flags, BDAddr address, int address_type, int conn_interval, int timeout, int latency, int bonding)
        {
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
        }
        public virtual void receive_attclient_group_found(int connection, int start, int end, byte[] uuid)
        {
        }
        public virtual void receive_attclient_attribute_found(int connection, int chrdecl, int value, int properties, byte[] uuid)
        {
        }
        public virtual void receive_attclient_find_information_found(int connection, int chrhandle, byte[] uuid)
        {
        }
        public virtual void receive_attclient_attribute_value(int connection, int atthandle, int type, byte[] value)
        {


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
    }
}
