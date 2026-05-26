-- V5__change_encrypted_mac_to_plaintext_mac.sql
ALTER TABLE devices DROP COLUMN IF EXISTS encrypted_mac;
ALTER TABLE devices ADD COLUMN mac_address VARCHAR(100);
