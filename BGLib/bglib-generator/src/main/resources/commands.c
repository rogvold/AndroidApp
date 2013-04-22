/*****************************************************************************
 * Copyright (C) 2000-2012 Bluegiga Technologies Inc. All rights reserved. All
 *  unauthorized copying and distribution prohibited.
 ****************************************************************************/
 
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

#include "cmd_def.h"

/**
 *
 */
void ble_default(const void*v)
{
}

/**
 *
 */
void ble_rsp_hardware_set_txpower(const void *nul)
{

}

/**
 *
 */
void ble_rsp_attclient_read_multiple(const struct ble_msg_attclient_read_multiple_rsp_t *msg)
{

}

/**
 *
 */
void ble_evt_attclient_read_multiple_response(const struct ble_msg_attclient_read_multiple_response_evt_t *msg)
{

}

/**
 *
 */
void ble_rsp_attclient_prepare_write(const struct ble_msg_attclient_prepare_write_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_attclient_execute_write(const struct ble_msg_attclient_execute_write_rsp_t *msg)
{

}

/**
 *
 */
void ble_evt_attributes_user_request(const struct ble_msg_attributes_user_request_evt_t *msg)
{

}

/**
 *
 */
void ble_rsp_attributes_user_response(const void * nul)
{

}

/**
 *
 */
void ble_rsp_system_whitelist_clear(const void *nul)
{

}

/**
 *
 */
void ble_rsp_attclient_write_command(const struct ble_msg_attclient_write_command_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_attclient_reserved(const void *nul)
{

}

/**
 *
 */
void ble_rsp_attclient_read_long(const struct ble_msg_attclient_read_long_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_system_whitelist_append(const struct ble_msg_system_whitelist_append_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_sm_set_parameters(const void *nul)
{

}

/**
 *
 */
void ble_rsp_sm_passkey_entry(const struct ble_msg_sm_passkey_entry_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_sm_get_bonds(const struct ble_msg_sm_get_bonds_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_sm_set_oob_data(const void *nul)
{

}

/**
 *
 */
void ble_rsp_sm_set_security_mode(const void *nul)
{

}

/**
 *
 */
void ble_rsp_gap_set_filtering(const struct ble_msg_gap_set_filtering_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_gap_set_adv_parameters(const struct ble_msg_gap_set_adv_parameters_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_hardware_io_port_write(const struct ble_msg_hardware_io_port_write_rsp_t *msg)
{

}

void ble_rsp_hardware_io_port_read(const struct ble_msg_hardware_io_port_read_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_hardware_spi_config(const struct ble_msg_hardware_spi_config_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_hardware_spi_transfer(const struct ble_msg_hardware_spi_transfer_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_hardware_i2c_read(const struct ble_msg_hardware_i2c_read_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_hardware_i2c_write(const struct ble_msg_hardware_i2c_write_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_test_get_channel_map(const struct ble_msg_test_get_channel_map_rsp_t *msg)
{

}

/**
 *
 */
void ble_evt_attributes_value(const struct ble_msg_attributes_value_evt_t *msg)
{

}

/**
 *
 */
void ble_evt_sm_bonding_fail(const struct ble_msg_sm_bonding_fail_evt_t *msg)
{

}

/**
 *
 */
void ble_evt_sm_passkey_display(const struct ble_msg_sm_passkey_display_evt_t *msg)
{

}

void ble_evt_sm_passkey_request(const struct ble_msg_sm_passkey_request_evt_t *msg)
{

}

/**
 *
 */
void ble_evt_sm_bond_status(const struct ble_msg_sm_bond_status_evt_t *msg)
{

}

/**
 *
 */
void ble_rsp_gap_set_adv_data(const struct ble_msg_gap_set_adv_data_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_gap_set_scan_parameters(const struct ble_msg_gap_set_scan_parameters_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_gap_set_directed_connectable_mode(const struct ble_msg_gap_set_directed_connectable_mode_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_hardware_io_port_config_direction(const struct ble_msg_hardware_io_port_config_direction_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_hardware_io_port_config_pull(const struct ble_msg_hardware_io_port_config_pull_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_hardware_io_port_config_function(const struct ble_msg_hardware_io_port_config_function_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_gap_set_privacy_flags(const void *nul)
{

}

/**
 *
 */
void ble_rsp_gap_connect_selective(const struct ble_msg_gap_connect_selective_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_system_whitelist_remove(const struct ble_msg_system_whitelist_remove_rsp_t *msg)
{

}

/**
 *
 */
void ble_rsp_system_reset(const void* nul)
{
}

/**
 *
 */
void ble_rsp_system_hello(const void* nul)
{
}

/**
 *
 */
void ble_rsp_system_address_get(const struct ble_msg_system_address_get_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_system_reg_write(const struct ble_msg_system_reg_write_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_system_reg_read(const struct ble_msg_system_reg_read_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_system_get_counters(const struct ble_msg_system_get_counters_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_system_get_connections(const struct ble_msg_system_get_connections_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_system_read_memory(const struct ble_msg_system_read_memory_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_system_get_info(const struct ble_msg_system_get_info_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_system_endpoint_tx(const void* nul)
{
}

/**
 *
 */
void ble_rsp_flash_ps_defrag(const void* nul)
{
}

/**
 *
 */
void ble_rsp_flash_ps_dump(const void* nul)
{
}

/**
 *
 */
void ble_rsp_flash_ps_erase_all(const void* nul)
{
}

/**
 *
 */
void ble_rsp_flash_ps_save(const struct ble_msg_flash_ps_save_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_flash_ps_load(const struct ble_msg_flash_ps_load_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_flash_ps_erase(const void* nul)
{
}

/**
 *
 */
void ble_rsp_flash_erase_page(const struct ble_msg_flash_erase_page_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_flash_write_words(const void* nul)
{
}

/**
 *
 */
void ble_rsp_attributes_write(const struct ble_msg_attributes_write_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_attributes_read(const struct ble_msg_attributes_read_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_attributes_read_type(const struct ble_msg_attributes_read_type_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_connection_disconnect(const struct ble_msg_connection_disconnect_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_connection_get_rssi(const struct ble_msg_connection_get_rssi_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_connection_update(const struct ble_msg_connection_update_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_connection_version_update(const struct ble_msg_connection_version_update_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_connection_channel_map_get(const struct ble_msg_connection_channel_map_get_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_connection_channel_map_set(const struct ble_msg_connection_channel_map_set_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_connection_features_get(const struct ble_msg_connection_features_get_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_connection_get_status(const struct ble_msg_connection_get_status_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_connection_raw_tx(const struct ble_msg_connection_raw_tx_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_attclient_find_by_type_value(const struct ble_msg_attclient_find_by_type_value_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_attclient_read_by_group_type(const struct ble_msg_attclient_read_by_group_type_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_attclient_read_by_type(const struct ble_msg_attclient_read_by_type_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_attclient_find_information(const struct ble_msg_attclient_find_information_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_attclient_read_by_handle(const struct ble_msg_attclient_read_by_handle_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_attclient_attribute_write(const struct ble_msg_attclient_attribute_write_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_sm_encrypt_start(const struct ble_msg_sm_encrypt_start_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_sm_set_bondable_mode(const void* nul)
{
}

/**
 *
 */
void ble_rsp_sm_delete_bonding(const struct ble_msg_sm_delete_bonding_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_gap_set_address_mode(const void* nul)
{
}

/**
 *
 */
void ble_rsp_gap_set_mode(const struct ble_msg_gap_set_mode_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_gap_discover(const struct ble_msg_gap_discover_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_gap_connect_direct(const struct ble_msg_gap_connect_direct_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_gap_end_procedure(const struct ble_msg_gap_end_procedure_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_hardware_io_port_config_irq(const struct ble_msg_hardware_io_port_config_irq_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_hardware_set_soft_timer(const struct ble_msg_hardware_set_soft_timer_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_hardware_adc_read(const struct ble_msg_hardware_adc_read_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_test_phy_tx(const void* nul)
{
}

/**
 *
 */
void ble_rsp_test_phy_rx(const void* nul)
{
}

/**
 *
 */
void ble_rsp_test_phy_end(const struct ble_msg_test_phy_end_rsp_t *msg)
{
}

/**
 *
 */
void ble_rsp_test_phy_reset(const void* nul)
{
}

/**
 *
 */
void ble_evt_system_boot(const struct ble_msg_system_boot_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_system_debug(const struct ble_msg_system_debug_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_system_endpoint_rx(const struct ble_msg_system_endpoint_rx_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_flash_ps_key(const struct ble_msg_flash_ps_key_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_connection_status(const struct ble_msg_connection_status_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_connection_version_ind(const struct ble_msg_connection_version_ind_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_connection_feature_ind(const struct ble_msg_connection_feature_ind_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_connection_raw_rx(const struct ble_msg_connection_raw_rx_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_connection_disconnected(const struct ble_msg_connection_disconnected_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_attclient_indicated(const struct ble_msg_attclient_indicated_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_attclient_procedure_completed(const struct ble_msg_attclient_procedure_completed_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_attclient_group_found(const struct ble_msg_attclient_group_found_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_attclient_attribute_found(const struct ble_msg_attclient_attribute_found_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_attclient_find_information_found(const struct ble_msg_attclient_find_information_found_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_attclient_attribute_value(const struct ble_msg_attclient_attribute_value_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_sm_smp_data(const struct ble_msg_sm_smp_data_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_gap_scan_response(const struct ble_msg_gap_scan_response_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_gap_mode_changed(const struct ble_msg_gap_mode_changed_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_hardware_io_port_status(const struct ble_msg_hardware_io_port_status_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_hardware_soft_timer(const struct ble_msg_hardware_soft_timer_evt_t *msg)
{
}

/**
 *
 */
void ble_evt_hardware_adc_result(const struct ble_msg_hardware_adc_result_evt_t *msg)
{
}

