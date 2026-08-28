-- Novo status: período de teste terminou sem o cliente nunca ter assinado.
-- Diferente de SUSPENDED (que é falha de pagamento / cancelamento de quem já pagava).
ALTER TYPE subscription_status ADD VALUE IF NOT EXISTS 'EXPIRED';
