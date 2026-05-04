-- V2__add_currency_to_payments.sql
ALTER TABLE payments ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'EUR';
