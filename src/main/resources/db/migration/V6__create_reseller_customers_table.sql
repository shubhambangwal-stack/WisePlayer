-- V6__create_reseller_customers_table.sql
CREATE TABLE reseller_customers (
    id UUID PRIMARY KEY,
    reseller_id UUID NOT NULL,
    mac_address VARCHAR(100) NOT NULL,
    customer_name VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_reseller_customers_mac ON reseller_customers (mac_address);
CREATE INDEX idx_reseller_customers_reseller ON reseller_customers (reseller_id);
