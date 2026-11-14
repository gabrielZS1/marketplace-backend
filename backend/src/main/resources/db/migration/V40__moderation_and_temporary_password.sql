-- 1) Senha temporária: funcionário criado (ou com senha redefinida pelo dono) precisa trocar
--    a senha no primeiro acesso. Contas existentes ficam como estão (false).
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- 2) Moderação: estabelecimento suspenso por denúncia. É separado de "active" (que também
--    muda com a assinatura) para que um pagamento aprovado não reative quem foi suspenso.
ALTER TABLE businesses
    ADD COLUMN moderation_suspended BOOLEAN NOT NULL DEFAULT FALSE;

-- 3) Aviso de segurança do Supabase: função com search_path mutável.
ALTER FUNCTION public.set_updated_at() SET search_path = public;
