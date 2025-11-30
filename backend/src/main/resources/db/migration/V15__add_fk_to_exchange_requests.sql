-- Add foreign key columns to exchange_requests table
ALTER TABLE exchange_requests ADD COLUMN offerer_card_offer_id BIGINT;
ALTER TABLE exchange_requests ADD COLUMN requester_card_search_id BIGINT;
ALTER TABLE exchange_requests ADD COLUMN requester_card_offer_id BIGINT;
ALTER TABLE exchange_requests ADD COLUMN offerer_card_search_id BIGINT;

-- Add foreign key constraints
ALTER TABLE exchange_requests 
ADD CONSTRAINT fk_offerer_card_offer 
FOREIGN KEY (offerer_card_offer_id) 
REFERENCES card_offers(id) 
ON DELETE SET NULL;

ALTER TABLE exchange_requests 
ADD CONSTRAINT fk_requester_card_search 
FOREIGN KEY (requester_card_search_id) 
REFERENCES card_searches(id) 
ON DELETE SET NULL;

ALTER TABLE exchange_requests 
ADD CONSTRAINT fk_requester_card_offer 
FOREIGN KEY (requester_card_offer_id) 
REFERENCES card_offers(id) 
ON DELETE SET NULL;

ALTER TABLE exchange_requests 
ADD CONSTRAINT fk_offerer_card_search 
FOREIGN KEY (offerer_card_search_id) 
REFERENCES card_searches(id) 
ON DELETE SET NULL;
