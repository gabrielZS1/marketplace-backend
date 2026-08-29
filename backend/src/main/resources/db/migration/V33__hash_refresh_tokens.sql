-- Guardar só o hash SHA-256 do refresh token, não o valor em si.
-- Vazou o banco -> não dá pra usar os tokens roubados.
-- Os tokens atuais são de teste; os usuários simplesmente relogam.
DELETE FROM refresh_tokens;

ALTER TABLE refresh_tokens DROP COLUMN token;
ALTER TABLE refresh_tokens ADD COLUMN token_hash VARCHAR(64) NOT NULL;

CREATE UNIQUE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
