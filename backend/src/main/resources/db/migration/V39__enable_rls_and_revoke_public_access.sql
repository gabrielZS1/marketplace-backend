-- Segurança: o Supabase expõe o schema "public" por uma API REST pública (PostgREST) usando
-- os papéis anon / authenticated. Estas tabelas guardam dados sensíveis (usuários, tokens de
-- renovação, códigos de verificação, códigos promocionais) e eram legíveis por esses papéis.
--
-- O backend NÃO usa essa API: conecta direto como "postgres" (dono das tabelas, BYPASSRLS),
-- então nada muda para ele. Aqui:
--   1) liga RLS em todas as tabelas do schema public (sem policies = ninguém acessa pela API);
--   2) revoga os privilégios de anon / authenticated (e os padrões para tabelas futuras).
-- É idempotente e roda também em Postgres comum, onde esses papéis não existem.

DO $$
DECLARE
    t record;
BEGIN
    -- flyway_schema_history fica de fora: o próprio Flyway mantém um bloqueio nela enquanto esta
    -- migration roda, e um ALTER TABLE ali trava o deploy para sempre (deadlock com ele mesmo).
    -- No Supabase o RLS dessa tabela é ligado à parte, fora do Flyway.
    FOR t IN
        SELECT tablename FROM pg_tables
        WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.tablename);
    END LOOP;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL ON ALL TABLES    IN SCHEMA public FROM anon;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon;
        REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES    FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM anon;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON ALL TABLES    IN SCHEMA public FROM authenticated;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM authenticated;
        REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES    FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM authenticated;
    END IF;
END
$$;
