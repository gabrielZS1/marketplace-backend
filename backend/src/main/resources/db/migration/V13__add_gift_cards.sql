CREATE TABLE gift_cards (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            business_id UUID NOT NULL REFERENCES businesses(id),
                            category VARCHAR(20) NOT NULL,
                            title VARCHAR(150) NOT NULL,
                            description VARCHAR(255),
                            price NUMERIC(10, 2) NOT NULL,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);