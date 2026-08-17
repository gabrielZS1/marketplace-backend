CREATE TABLE promo_codes (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             code VARCHAR(20) NOT NULL UNIQUE,
                             free_months INTEGER NOT NULL,
                             redeemed BOOLEAN NOT NULL DEFAULT false,
                             redeemed_by_business_id UUID REFERENCES businesses(id),
                             redeemed_at TIMESTAMPTZ,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);