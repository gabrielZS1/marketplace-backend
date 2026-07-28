ALTER TABLE reviews ADD COLUMN employee_id UUID REFERENCES employees(id);

UPDATE reviews r
SET employee_id = a.employee_id
    FROM appointments a
WHERE r.appointment_id = a.id;

ALTER TABLE reviews ALTER COLUMN employee_id SET NOT NULL;

CREATE INDEX idx_reviews_employee ON reviews(employee_id);