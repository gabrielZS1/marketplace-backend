ALTER TABLE working_hours
    ALTER COLUMN employee_id DROP NOT NULL,
    ADD COLUMN business_id UUID REFERENCES businesses(id);

ALTER TABLE working_hours
    ADD CONSTRAINT chk_working_hours_owner CHECK (
        (employee_id IS NOT NULL AND business_id IS NULL)
            OR
        (employee_id IS NULL AND business_id IS NOT NULL)
        );