CREATE TABLE reseller_email_otps (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     admin_id UUID NOT NULL,
                                     otp_hash VARCHAR(64) NOT NULL,
                                     expires_at TIMESTAMP NOT NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT now()
);