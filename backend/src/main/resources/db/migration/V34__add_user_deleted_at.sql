-- Exclusão de conta (LGPD): a linha do usuário é anonimizada e marcada aqui,
-- em vez de apagada de vez (os agendamentos/avaliações de outras partes ficam).
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;
