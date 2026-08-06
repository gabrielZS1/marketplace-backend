ALTER TYPE subscription_status ADD VALUE IF NOT EXISTS 'PAST_DUE';

ALTER TABLE businesses
    ADD COLUMN subscription_grace_ends_at TIMESTAMPTZ,
    ADD COLUMN mp_preapproval_id VARCHAR(255);