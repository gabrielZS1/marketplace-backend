-- Códigos de uso único enviados por e-mail: redefinição de senha e verificação de e-mail.
-- Guardamos só o hash do código (é uma credencial), com expiração e limite de tentativas.
CREATE TABLE one_time_codes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose     VARCHAR(32) NOT NULL,          -- PASSWORD_RESET | EMAIL_VERIFICATION
    code_hash   VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    attempts    SMALLINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_one_time_codes_user_purpose ON one_time_codes(user_id, purpose);
