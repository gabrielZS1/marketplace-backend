-- Controle dos e-mails de cobrança (a cobrança acontece pela página web /assinar, não pelo app).
-- Cada aviso é enviado uma vez por ciclo; os campos são zerados quando a assinatura fica ativa.
ALTER TABLE businesses
    ADD COLUMN trial_reminder_sent_at TIMESTAMPTZ,
    ADD COLUMN expired_notice_sent_at TIMESTAMPTZ;
