CREATE TABLE IF NOT EXISTS working_hours (
                                             id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    day_of_week     SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    CHECK (end_time > start_time),
    UNIQUE (employee_id, day_of_week)
    );