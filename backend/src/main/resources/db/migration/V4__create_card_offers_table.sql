CREATE TABLE card_offers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    sticker_id BIGINT NOT NULL REFERENCES stickers(id),
    offer_type VARCHAR(50) NOT NULL
);

CREATE INDEX idx_card_offers_user_id ON card_offers(user_id);
CREATE INDEX idx_card_offers_sticker_id ON card_offers(sticker_id);
