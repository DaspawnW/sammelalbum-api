CREATE TABLE exchange_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    offerer_id BIGINT NOT NULL,
    requested_sticker_id BIGINT NOT NULL,
    offered_sticker_id BIGINT,
    exchange_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES users(id),
    FOREIGN KEY (offerer_id) REFERENCES users(id),
    FOREIGN KEY (requested_sticker_id) REFERENCES stickers(id),
    FOREIGN KEY (offered_sticker_id) REFERENCES stickers(id)
);

CREATE INDEX idx_exchange_requests_requester ON exchange_requests(requester_id);
CREATE INDEX idx_exchange_requests_offerer ON exchange_requests(offerer_id);
