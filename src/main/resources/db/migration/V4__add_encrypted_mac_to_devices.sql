-- V4__add_encrypted_mac_to_devices.sql
ALTER TABLE devices ADD COLUMN encrypted_mac VARCHAR(255);
