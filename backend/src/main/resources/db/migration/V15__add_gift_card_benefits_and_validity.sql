ALTER TABLE gift_cards
    ADD COLUMN benefits TEXT[],
    ADD COLUMN validity_label VARCHAR(50);