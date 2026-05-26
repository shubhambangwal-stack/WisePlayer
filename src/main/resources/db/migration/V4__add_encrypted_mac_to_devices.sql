-- V4__add_mac_address_to_devices.sql
ALTER TABLE devices ADD COLUMN mac_address VARCHAR(100);
