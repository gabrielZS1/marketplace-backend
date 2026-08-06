CREATE TYPE work_location_type AS ENUM ('AT_BUSINESS', 'AT_CLIENT_HOME', 'BOTH');

ALTER TABLE businesses
    ADD COLUMN team_size INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN work_location_type work_location_type NOT NULL DEFAULT 'AT_BUSINESS',
    ADD COLUMN trial_ends_at TIMESTAMPTZ,
    ADD COLUMN current_period_end TIMESTAMPTZ,
    ADD COLUMN external_customer_id VARCHAR(255),
    ADD COLUMN plan_price NUMERIC(10, 2);