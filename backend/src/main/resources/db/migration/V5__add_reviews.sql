ALTER TABLE reviews ADD COLUMN business_id UUID REFERENCES businesses(id) ON DELETE CASCADE;
ALTER TABLE reviews ADD COLUMN client_id UUID REFERENCES users(id);

UPDATE reviews r
SET business_id = a.business_id,
    client_id = a.client_id
    FROM appointments a
WHERE r.appointment_id = a.id;

ALTER TABLE reviews ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN client_id SET NOT NULL;

CREATE INDEX idx_reviews_business ON reviews(business_id);