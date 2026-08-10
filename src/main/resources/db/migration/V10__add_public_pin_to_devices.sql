-- Add public_pin_hash column to devices table.
-- Stores a BCrypt-hashed 4-digit PIN for protecting public playlist access.
-- Nullable: devices without a PIN remain publicly accessible (backward-compatible).
ALTER TABLE devices ADD COLUMN public_pin_hash VARCHAR(100);
