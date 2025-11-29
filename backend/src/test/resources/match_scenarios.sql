-- Clean up existing data
DELETE FROM exchange_requests;
DELETE FROM card_searches;
DELETE FROM card_offers;
DELETE FROM credentials;
DELETE FROM users;
DELETE FROM stickers;

-- Create Stickers (1-20)
INSERT INTO stickers (id, name) VALUES (1, 'Sticker 1');
INSERT INTO stickers (id, name) VALUES (2, 'Sticker 2');
INSERT INTO stickers (id, name) VALUES (3, 'Sticker 3');
INSERT INTO stickers (id, name) VALUES (4, 'Sticker 4');
INSERT INTO stickers (id, name) VALUES (5, 'Sticker 5');
INSERT INTO stickers (id, name) VALUES (6, 'Sticker 6');
INSERT INTO stickers (id, name) VALUES (7, 'Sticker 7');
INSERT INTO stickers (id, name) VALUES (8, 'Sticker 8');
INSERT INTO stickers (id, name) VALUES (9, 'Sticker 9');
INSERT INTO stickers (id, name) VALUES (10, 'Sticker 10');
INSERT INTO stickers (id, name) VALUES (11, 'Sticker 11');
INSERT INTO stickers (id, name) VALUES (12, 'Sticker 12');
INSERT INTO stickers (id, name) VALUES (13, 'Sticker 13');
INSERT INTO stickers (id, name) VALUES (14, 'Sticker 14');
INSERT INTO stickers (id, name) VALUES (15, 'Sticker 15');
INSERT INTO stickers (id, name) VALUES (16, 'Sticker 16');
INSERT INTO stickers (id, name) VALUES (17, 'Sticker 17');
INSERT INTO stickers (id, name) VALUES (18, 'Sticker 18');
INSERT INTO stickers (id, name) VALUES (19, 'Sticker 19');
INSERT INTO stickers (id, name) VALUES (20, 'Sticker 20');

-- Create Users
-- User 1: Main Test User (The "Searcher")
INSERT INTO users (id, firstname, lastname, mail) VALUES (1, 'Main', 'User', 'main@example.com');
INSERT INTO credentials (user_id, username, password_hash) VALUES (1, 'mainuser', '$2a$10$xn3LI/RushrySPM.ueO8COs/j.a/c.d.e.f.g.h.i.j.k.l.m'); -- password

-- User 2: The "Freebie King" (Offers many freebies that Main needs)
INSERT INTO users (id, firstname, lastname, mail) VALUES (2, 'Freebie', 'King', 'freebie@example.com');

-- User 3: The "Payed Merchant" (Offers many payed cards that Main needs)
INSERT INTO users (id, firstname, lastname, mail) VALUES (3, 'Payed', 'Merchant', 'payed@example.com');

-- User 4: The "Perfect Match" (Has perfect 1:1 exchange with Main)
INSERT INTO users (id, firstname, lastname, mail) VALUES (4, 'Perfect', 'Match', 'perfect@example.com');
INSERT INTO credentials (user_id, username, password_hash) VALUES (4, 'perfectmatch', '$2a$10$xn3LI/RushrySPM.ueO8COs/j.a/c.d.e.f.g.h.i.j.k.l.m');

-- User 5: The "Partial Match" (Has some 1:1 exchange with Main)
INSERT INTO users (id, firstname, lastname, mail) VALUES (5, 'Partial', 'Match', 'partial@example.com');
INSERT INTO credentials (user_id, username, password_hash) VALUES (5, 'partialmatch', '$2a$10$xn3LI/RushrySPM.ueO8COs/j.a/c.d.e.f.g.h.i.j.k.l.m');

-- User 6: The "No Match" (Offers nothing Main needs, Needs nothing Main offers)
INSERT INTO users (id, firstname, lastname, mail) VALUES (6, 'No', 'Match', 'nomatch@example.com');
INSERT INTO credentials (user_id, username, password_hash) VALUES (6, 'nomatch', '$2a$10$xn3LI/RushrySPM.ueO8COs/j.a/c.d.e.f.g.h.i.j.k.l.m');

-- User 7: The "One Way" (Offers what Main needs, but needs nothing from Main - for Exchange test)
-- Needs: 11 (Main does NOT offer)
-- Offers: 
-- Sticker 6 as PAYED
-- Sticker 7 as FREEBIE
-- Sticker 8 as EXCHANGE (but Main offers 8 as EXCHANGE, so this might be a mutual match if Main needs 8? No, Main offers 8. Main needs 1,2,3,4,5. Wait.)
-- Main Needs: 1, 2, 3, 4, 5.
-- Main Offers: 6, 7, 8, 9, 10.

-- Let's adjust User 7 to offer things Main NEEDS (1, 2, 3).
-- User 7 offers Sticker 1 as PAYED.
-- User 7 offers Sticker 2 as FREEBIE.
-- User 7 offers Sticker 3 as EXCHANGE.

INSERT INTO users (id, firstname, lastname, mail) VALUES (7, 'One', 'Way', 'oneway@example.com');
INSERT INTO credentials (user_id, username, password_hash) VALUES (7, 'oneway', '$2a$10$xn3LI/RushrySPM.ueO8COs/j.a/c.d.e.f.g.h.i.j.k.l.m');

-- User 1 (Main)
-- Needs: 1, 2, 3, 4, 5
INSERT INTO card_searches (user_id, sticker_id) VALUES (1, 1);
INSERT INTO card_searches (user_id, sticker_id) VALUES (1, 2);
INSERT INTO card_searches (user_id, sticker_id) VALUES (1, 3);
INSERT INTO card_searches (user_id, sticker_id) VALUES (1, 4);
INSERT INTO card_searches (user_id, sticker_id) VALUES (1, 5);
-- Offers: 6, 7, 8, 9, 10 (Exchange)
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (1, 6, true);
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (1, 7, true);
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (1, 8, true);
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (1, 9, true);
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (1, 10, true);

-- User 2 (Freebie King)
-- Offers: 1, 2, 3, 4, 5 (Freebie)
INSERT INTO card_offers (user_id, sticker_id, offer_freebie) VALUES (2, 1, true);
INSERT INTO card_offers (user_id, sticker_id, offer_freebie) VALUES (2, 2, true);
INSERT INTO card_offers (user_id, sticker_id, offer_freebie) VALUES (2, 3, true);
INSERT INTO card_offers (user_id, sticker_id, offer_freebie) VALUES (2, 4, true);
INSERT INTO card_offers (user_id, sticker_id, offer_freebie) VALUES (2, 5, true);

-- User 3 (Payed Merchant)
-- Offers: 1, 2, 3, 4, 5 (Payed)
INSERT INTO card_offers (user_id, sticker_id, offer_payed) VALUES (3, 1, true);
INSERT INTO card_offers (user_id, sticker_id, offer_payed) VALUES (3, 2, true);
INSERT INTO card_offers (user_id, sticker_id, offer_payed) VALUES (3, 3, true);
INSERT INTO card_offers (user_id, sticker_id, offer_payed) VALUES (3, 4, true);
INSERT INTO card_offers (user_id, sticker_id, offer_payed) VALUES (3, 5, true);

-- User 4 (Perfect Match)
-- Offers: 1, 2, 3 (Exchange)
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (4, 1, true);
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (4, 2, true);
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (4, 3, true);
-- Needs: 6, 7, 8
INSERT INTO card_searches (user_id, sticker_id) VALUES (4, 6);
INSERT INTO card_searches (user_id, sticker_id) VALUES (4, 7);
INSERT INTO card_searches (user_id, sticker_id) VALUES (4, 8);

-- User 5 (Partial Match)
-- Offers: 1, 2 (Exchange)
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (5, 1, true);
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (5, 2, true);
-- Needs: 6
INSERT INTO card_searches (user_id, sticker_id) VALUES (5, 6);

-- User 6 (No Match)
-- Offers: 11
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (6, 11, true);
-- Needs: 12
INSERT INTO card_searches (user_id, sticker_id) VALUES (6, 12);

-- User 7 (One Way)

INSERT INTO card_searches (user_id, sticker_id) VALUES (7, 11); -- Needs something Main doesn't have

-- Offer 1 as PAYED
INSERT INTO card_offers (user_id, sticker_id, offer_payed) VALUES (7, 1, true);

-- Offer 2 as FREEBIE
INSERT INTO card_offers (user_id, sticker_id, offer_freebie) VALUES (7, 2, true);

-- Offer 3 as EXCHANGE
INSERT INTO card_offers (user_id, sticker_id, offer_exchange) VALUES (7, 3, true);
