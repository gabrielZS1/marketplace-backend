-- Trava por conta: depois de várias senhas erradas, o e-mail fica bloqueado
-- por alguns minutos (complementa o rate limit por IP).
ALTER TABLE users ADD COLUMN failed_login_attempts SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN lockout_until TIMESTAMPTZ;
