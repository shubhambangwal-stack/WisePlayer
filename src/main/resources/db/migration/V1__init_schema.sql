-- V1__init_schema.sql

-- Admins Table
CREATE TABLE admins (
    admin_id UUID PRIMARY KEY,
    email VARCHAR(100) UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    parent_id UUID,
    creator_id UUID,
    credits NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    partner_level VARCHAR(20) DEFAULT 'SILVER'
);
CREATE INDEX idx_admin_username ON admins (username);
CREATE INDEX idx_admin_email ON admins (email);

-- Devices Table
CREATE TABLE devices (
    device_id UUID PRIMARY KEY,
    fingerprint_hash VARCHAR(64) NOT NULL UNIQUE,
    device_secret_hash VARCHAR(64) NOT NULL,
    device_status VARCHAR(20) NOT NULL,
    subscription_type VARCHAR(20) NOT NULL,
    device_model VARCHAR(100),
    os_version VARCHAR(50),
    platform VARCHAR(50),
    registered_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITHOUT TIME ZONE,
    activated_at TIMESTAMP WITHOUT TIME ZONE,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    refresh_token VARCHAR(100),
    reseller_id UUID,
    is_active BOOLEAN DEFAULT TRUE
);
CREATE INDEX idx_fingerprint_hash ON devices (fingerprint_hash);

-- Payments Table
CREATE TABLE payments (
    payment_id UUID PRIMARY KEY,
    device_id UUID,
    reseller_id UUID,
    credit_amount INTEGER,
    status VARCHAR(20) NOT NULL,
    stripe_session_id VARCHAR(255),
    stripe_event_id VARCHAR(255) UNIQUE,
    paypal_order_id VARCHAR(255),
    paypal_capture_id VARCHAR(255),
    paypal_fee NUMERIC(10, 2),
    amount NUMERIC(10, 2),
    plan VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX idx_payment_session_id ON payments (stripe_session_id);
CREATE INDEX idx_payment_event_id ON payments (stripe_event_id);
CREATE INDEX idx_paypal_order_id ON payments (paypal_order_id);

-- Subscriptions Table
CREATE TABLE subscriptions (
    subscription_id UUID PRIMARY KEY,
    device_id UUID NOT NULL,
    plan VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    activation_source VARCHAR(20),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX idx_subscription_device_id ON subscriptions (device_id);

-- Subscription Plan Configs Table
CREATE TABLE subscription_plan_configs (
    plan_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    duration_days INTEGER NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    credits NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- Activation Requests Table
CREATE TABLE activation_requests (
    request_id UUID PRIMARY KEY,
    reseller_id UUID NOT NULL,
    device_id UUID NOT NULL,
    plan_name VARCHAR(50) NOT NULL,
    amount DOUBLE PRECISION,
    currency VARCHAR(10),
    status VARCHAR(20) NOT NULL,
    credits_used NUMERIC(10, 2),
    admin_notes VARCHAR(500),
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- Admin Audit Logs Table
CREATE TABLE admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    performed_by UUID NOT NULL,
    target_email VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- Admin Invites Table
CREATE TABLE admin_invites (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    token VARCHAR(255) NOT NULL UNIQUE,
    invited_by UUID NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- Credit Transactions Table
CREATE TABLE credit_transactions (
    transaction_id UUID PRIMARY KEY,
    admin_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    related_request_id UUID,
    notes VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX idx_credit_transaction_admin ON credit_transactions (admin_id);

-- Device Audit Logs Table
CREATE TABLE device_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    device_id UUID NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    action VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    ip_address VARCHAR(45),
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- Device Keys Table
CREATE TABLE device_keys (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_device_keys_device FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
);
CREATE INDEX idx_device_key_hash ON device_keys (key_hash);
CREATE INDEX idx_key_expires_at ON device_keys (expires_at);

-- Playlists Table
CREATE TABLE playlists (
    playlist_id UUID PRIMARY KEY,
    device_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL DEFAULT 'My Playlist',
    type VARCHAR(20) NOT NULL,
    server_url TEXT,
    username VARCHAR(512),
    password VARCHAR(512),
    m3u_url TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX idx_playlist_device_id ON playlists (device_id);

-- Super Admins Table
CREATE TABLE super_admins (
    super_admin_id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- Support Tickets Table
CREATE TABLE support_tickets (
    id UUID PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mac_address VARCHAR(255) NOT NULL,
    inquiry_type VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    attachment_url VARCHAR(1000),
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
