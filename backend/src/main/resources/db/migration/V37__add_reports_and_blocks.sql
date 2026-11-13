-- Denúncias de conteúdo (avaliações / estabelecimentos) e bloqueio de usuários.
-- Exigidos pelas lojas de apps para conteúdo gerado por usuários (Apple 1.2 / Google UGC).

CREATE TABLE reports (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID NOT NULL REFERENCES users (id),
    target_type VARCHAR(20) NOT NULL,
    target_id   UUID NOT NULL,
    reason      VARCHAR(30) NOT NULL,
    details     VARCHAR(500),
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_report_once UNIQUE (reporter_id, target_type, target_id)
);

CREATE INDEX idx_reports_status_created ON reports (status, created_at DESC);

CREATE TABLE user_blocks (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blocker_id UUID NOT NULL REFERENCES users (id),
    blocked_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_block UNIQUE (blocker_id, blocked_id),
    CONSTRAINT chk_user_block_not_self CHECK (blocker_id <> blocked_id)
);

CREATE INDEX idx_user_blocks_blocker ON user_blocks (blocker_id);
