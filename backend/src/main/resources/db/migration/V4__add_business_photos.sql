CREATE TABLE business_photos (
                                 id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
                                 url         VARCHAR(500) NOT NULL,
                                 position    INTEGER NOT NULL DEFAULT 0,
                                 created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_business_photos_business ON business_photos(business_id);