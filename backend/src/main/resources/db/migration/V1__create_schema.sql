-- =========================================================
-- Marketplace de Serviços (Barbearia, Manicure, Spa, etc)
-- Modelagem do Banco de Dados (PostgreSQL) — Multi-tenant
-- =========================================================
-- Baseado nas decisões do projeto Duarte Cortes, adaptado para
-- suportar múltiplas empresas na mesma plataforma.
-- =========================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gist"; -- necessário para constraint de exclusão (anti double-booking)
CREATE EXTENSION IF NOT EXISTS "cube";
CREATE EXTENSION IF NOT EXISTS "earthdistance"; -- permite calcular distância entre coordenadas (busca por proximidade)

-- =========================================================
-- ENUMS
-- =========================================================

-- CLIENT: usuário final que agenda serviços
-- BUSINESS_OWNER: dono de uma empresa cadastrada na plataforma
-- EMPLOYEE: funcionário de uma empresa (ex: barbeiro, manicure)
-- ADMIN: administrador da plataforma (você)
CREATE TYPE user_role AS ENUM ('CLIENT', 'BUSINESS_OWNER', 'EMPLOYEE', 'ADMIN');

-- Categoria fixa da empresa. Pode crescer com ALTER TYPE ... ADD VALUE no futuro.
CREATE TYPE business_category AS ENUM ('BARBERSHOP', 'MANICURE', 'SPA');

-- Onde o serviço é realizado
-- ON_SITE: só no estabelecimento
-- HOME_SERVICE: só a domicílio (no endereço do cliente)
-- BOTH: o cliente escolhe entre os dois
CREATE TYPE service_location_type AS ENUM ('ON_SITE', 'HOME_SERVICE', 'BOTH');

-- Situação da empresa em relação ao acesso à plataforma
-- FREE: acesso liberado sem cobrança (ex: parceiros fundadores)
-- TRIAL: período de teste gratuito
-- ACTIVE: assinatura paga em dia (para quando o modelo de cobrança for implementado)
-- SUSPENDED: acesso bloqueado (inadimplência ou moderação)
CREATE TYPE subscription_status AS ENUM ('FREE', 'TRIAL', 'ACTIVE', 'SUSPENDED');

CREATE TYPE appointment_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED',
    'NO_SHOW'
);

-- =========================================================
-- USERS — base de autenticação para todos os perfis
-- =========================================================

CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       name            VARCHAR(150)  NOT NULL,
                       email           VARCHAR(150)  NOT NULL UNIQUE,
                       phone           VARCHAR(20)   NOT NULL UNIQUE,
                       password_hash   VARCHAR(255)  NOT NULL,
                       role            user_role     NOT NULL DEFAULT 'CLIENT',
                       photo_url       VARCHAR(500),
                       active          BOOLEAN       NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_role ON users(role);

-- =========================================================
-- BUSINESSES — cada empresa cadastrada na plataforma
-- =========================================================

CREATE TABLE businesses (
                            id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                            owner_id            UUID NOT NULL REFERENCES users(id),
                            name                VARCHAR(150) NOT NULL,
                            category            business_category NOT NULL,
                            description         TEXT,
                            logo_url            VARCHAR(500),

    -- Endereço e localização (essencial para a busca por proximidade)
                            address             VARCHAR(255) NOT NULL,
                            city                VARCHAR(100) NOT NULL,
                            state               VARCHAR(2)   NOT NULL,
                            latitude            DOUBLE PRECISION NOT NULL,
                            longitude           DOUBLE PRECISION NOT NULL,

                            subscription_status subscription_status NOT NULL DEFAULT 'TRIAL',
                            active              BOOLEAN NOT NULL DEFAULT TRUE, -- aprovado pelo admin e visível na busca

                            created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                            updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_businesses_category ON businesses(category);
CREATE INDEX idx_businesses_active ON businesses(active);

-- Índice espacial: acelera MUITO buscas por "empresas num raio de X km"
CREATE INDEX idx_businesses_location ON businesses
    USING gist (ll_to_earth(latitude, longitude));

-- =========================================================
-- EMPLOYEES — funcionários de uma empresa (extensão de users)
-- =========================================================

CREATE TABLE employees (
                           id              UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                           business_id     UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
                           bio             TEXT,
                           specialty       VARCHAR(150),
                           active          BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_employees_business ON employees(business_id);

-- =========================================================
-- SERVICES — catálogo de serviços, um por empresa
-- =========================================================

CREATE TABLE services (
                          id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          business_id         UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
                          name                VARCHAR(100) NOT NULL,
                          description         TEXT,
                          price               NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                          duration_minutes    INTEGER NOT NULL CHECK (duration_minutes > 0),
                          location_type       service_location_type NOT NULL DEFAULT 'ON_SITE',
                          active              BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_services_business ON services(business_id);

-- =========================================================
-- EMPLOYEE_SERVICES — quais serviços cada funcionário realiza
-- =========================================================

CREATE TABLE employee_services (
                                   employee_id     UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                                   service_id      UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
                                   PRIMARY KEY (employee_id, service_id)
);

-- =========================================================
-- WORKING_HOURS — expediente padrão de cada funcionário
-- =========================================================

CREATE TABLE working_hours (
                               id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               employee_id     UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                               day_of_week     SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
                               start_time      TIME NOT NULL,
                               end_time        TIME NOT NULL,
                               CHECK (end_time > start_time),
                               UNIQUE (employee_id, day_of_week)
);

-- =========================================================
-- TIME_OFF — bloqueios pontuais (folga, imprevisto)
-- =========================================================

CREATE TABLE time_off (
                          id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          employee_id     UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                          starts_at       TIMESTAMPTZ NOT NULL,
                          ends_at         TIMESTAMPTZ NOT NULL,
                          reason          VARCHAR(255),
                          CHECK (ends_at > starts_at)
);

-- =========================================================
-- APPOINTMENTS — agendamentos
-- =========================================================

CREATE TABLE appointments (
                              id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              business_id     UUID NOT NULL REFERENCES businesses(id),
                              client_id       UUID NOT NULL REFERENCES users(id),
                              employee_id     UUID NOT NULL REFERENCES employees(id),
                              service_id      UUID NOT NULL REFERENCES services(id),

                              starts_at       TIMESTAMPTZ NOT NULL,
                              ends_at         TIMESTAMPTZ NOT NULL,
                              status          appointment_status NOT NULL DEFAULT 'PENDING',

    -- Preenchido apenas quando o serviço é HOME_SERVICE (atendimento no endereço do cliente)
                              is_home_service BOOLEAN NOT NULL DEFAULT FALSE,
                              client_address  VARCHAR(255),

                              notes           TEXT,
                              created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                              updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                              CHECK (ends_at > starts_at),

    -- Anti double-booking: mesmo funcionário não pode ter dois agendamentos
    -- com horários sobrepostos, entre os status considerados "ativos"
                              EXCLUDE USING gist (
        employee_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS'))
);

CREATE INDEX idx_appointments_business ON appointments(business_id);
CREATE INDEX idx_appointments_employee_date ON appointments(employee_id, starts_at);
CREATE INDEX idx_appointments_client ON appointments(client_id);

-- =========================================================
-- REVIEWS — avaliação da empresa após o atendimento (v2, já modelado)
-- =========================================================

CREATE TABLE reviews (
                         id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         appointment_id  UUID NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
                         rating          SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         comment         TEXT,
                         created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =========================================================
-- PUSH_TOKENS — notificações
-- =========================================================

CREATE TABLE push_tokens (
                             id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             token           VARCHAR(255) NOT NULL,
                             platform        VARCHAR(10) NOT NULL CHECK (platform IN ('ios', 'android')),
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                             UNIQUE (user_id, token)
);

-- =========================================================
-- TRIGGER genérico para manter updated_at sempre atualizado
-- =========================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_businesses_updated_at
    BEFORE UPDATE ON businesses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_appointments_updated_at
    BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =========================================================
-- EXEMPLO: busca de empresas num raio de X km (referência para o backend)
-- =========================================================
-- SELECT *, earth_distance(ll_to_earth(latitude, longitude), ll_to_earth(:lat, :lng)) / 1000 AS distance_km
-- FROM businesses
-- WHERE active = TRUE
--   AND earth_box(ll_to_earth(:lat, :lng), :radius_meters) @> ll_to_earth(latitude, longitude)
-- ORDER BY distance_km ASC;