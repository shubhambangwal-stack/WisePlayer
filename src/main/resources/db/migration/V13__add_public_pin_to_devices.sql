-- Add public_pin_hash column to devices table.
-- Stores a BCrypt-hashed 4-digit PIN for protecting public playlist access.
-- Nullable: devices without a PIN remain publicly accessible (backward-compatible).
-- IF NOT EXISTS: safe to re-run if column was added in a previous attempt.
ALTER TABLE devices ADD COLUMN IF NOT EXISTS public_pin_hash VARCHAR(100);
