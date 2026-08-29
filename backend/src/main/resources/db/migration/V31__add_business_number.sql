-- Número do imóvel (casa / prédio / sala) do estabelecimento.
-- Separado de `address` porque a busca por geocoding nem sempre traz o número.
ALTER TABLE businesses ADD COLUMN number VARCHAR(20);
