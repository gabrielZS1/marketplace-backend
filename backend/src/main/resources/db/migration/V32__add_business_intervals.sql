-- Intervalo recorrente do estabelecimento (ex.: almoço 12:00–13:00), todo dia.
-- Diferente de time_blocks (data/hora pontual): aqui é só a faixa de horário,
-- e vale para todos os dias em que o estabelecimento atende.
CREATE TABLE business_intervals (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    label       VARCHAR(60),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_business_intervals_business ON business_intervals(business_id);
