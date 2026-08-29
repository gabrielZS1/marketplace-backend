-- Login com Google e verificação de e-mail.

-- Identificador do usuário no Google (claim "sub"). Nulo para contas só de e-mail/senha.
ALTER TABLE users ADD COLUMN google_id VARCHAR(64) UNIQUE;

-- E-mail confirmado? Contas criadas via Google já entram verificadas.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Conta criada via Google pode não ter senha...
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- ...nem telefone (o Google não fornece). O app pede depois, no perfil.
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;

-- Usuários que já existiam entraram antes da verificação existir: não trancar ninguém.
UPDATE users SET email_verified = TRUE;
