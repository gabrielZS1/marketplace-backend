-- O código promocional agora concede DIAS de teste (15 ou 30), não meses.
ALTER TABLE promo_codes RENAME COLUMN free_months TO free_days;

-- Rótulo opcional pra identificar o lote no painel / no Kiwify (ex: "LANCAMENTO-15", "LANCAMENTO-30").
ALTER TABLE promo_codes ADD COLUMN IF NOT EXISTS label VARCHAR(60);
