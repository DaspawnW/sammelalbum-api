-- Clean up and reset sequences (Postgres compatible)
TRUNCATE TABLE exchange_requests, card_offers, card_searches, stickers, credentials, users RESTART IDENTITY CASCADE;

-- Users
INSERT INTO users (id, firstname, lastname, mail, contact) VALUES
(1, 'Alice', 'Test', 'testuser-1@example.com', 'alice@example.com'),
(2, 'Bob', 'Test', 'testuser-2@example.com', 'bob@example.com'),
(3, 'Charlie', 'Test', 'testuser-3@example.com', 'charlie@example.com'),
(4, 'David', 'Test', 'testuser-4@example.com', 'david@example.com'),
(5, 'Eve', 'Test', 'testuser-5@example.com', 'eve@example.com'),
(6, 'Frank', 'Test', 'testuser-6@example.com', 'frank@example.com');

-- Credentials
-- Password is 'testuser' ($2a$10$6oHAZ5zyAS3Zudz9hchbKuwPYi5GwegB91wz/qzF6VVDCulUU0VRy)
INSERT INTO credentials (id, username, password_hash, user_id) VALUES
(1, 'testuser-1', '$2a$10$6oHAZ5zyAS3Zudz9hchbKuwPYi5GwegB91wz/qzF6VVDCulUU0VRy', 1),
(2, 'testuser-2', '$2a$10$6oHAZ5zyAS3Zudz9hchbKuwPYi5GwegB91wz/qzF6VVDCulUU0VRy', 2),
(3, 'testuser-3', '$2a$10$6oHAZ5zyAS3Zudz9hchbKuwPYi5GwegB91wz/qzF6VVDCulUU0VRy', 3),
(4, 'testuser-4', '$2a$10$6oHAZ5zyAS3Zudz9hchbKuwPYi5GwegB91wz/qzF6VVDCulUU0VRy', 4),
(5, 'testuser-5', '$2a$10$6oHAZ5zyAS3Zudz9hchbKuwPYi5GwegB91wz/qzF6VVDCulUU0VRy', 5),
(6, 'testuser-6', '$2a$10$6oHAZ5zyAS3Zudz9hchbKuwPYi5GwegB91wz/qzF6VVDCulUU0VRy', 6);

-- Stickers (1-50)
INSERT INTO stickers (id, name) VALUES
(1, 'Sticker 1'), (2, 'Sticker 2'), (3, 'Sticker 3'), (4, 'Sticker 4'), (5, 'Sticker 5'),
(6, 'Sticker 6'), (7, 'Sticker 7'), (8, 'Sticker 8'), (9, 'Sticker 9'), (10, 'Sticker 10'),
(11, 'Sticker 11'), (12, 'Sticker 12'), (13, 'Sticker 13'), (14, 'Sticker 14'), (15, 'Sticker 15'),
(16, 'Sticker 16'), (17, 'Sticker 17'), (18, 'Sticker 18'), (19, 'Sticker 19'), (20, 'Sticker 20'),
(21, 'Sticker 21'), (22, 'Sticker 22'), (23, 'Sticker 23'), (24, 'Sticker 24'), (25, 'Sticker 25'),
(26, 'Sticker 26'), (27, 'Sticker 27'), (28, 'Sticker 28'), (29, 'Sticker 29'), (30, 'Sticker 30'),
(31, 'Sticker 31'), (32, 'Sticker 32'), (33, 'Sticker 33'), (34, 'Sticker 34'), (35, 'Sticker 35'),
(36, 'Sticker 36'), (37, 'Sticker 37'), (38, 'Sticker 38'), (39, 'Sticker 39'), (40, 'Sticker 40'),
(41, 'Sticker 41'), (42, 'Sticker 42'), (43, 'Sticker 43'), (44, 'Sticker 44'), (45, 'Sticker 45'),
(46, 'Sticker 46'), (47, 'Sticker 47'), (48, 'Sticker 48'), (49, 'Sticker 49'), (50, 'Sticker 50');

-- ==========================================
-- 1. MATCH SCENARIOS (Available for new requests)
-- ==========================================

-- 1.1 Freebie Match (Bob offers 1, Alice searches 1)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(1, 2, 1, false, true, false, false); -- Bob
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(1, 1, 1, false); -- Alice

-- 1.2 Exchange Match (Charlie offers 2 needs 3, Alice offers 3 needs 2)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(2, 3, 2, false, false, true, false), -- Charlie offers 2
(3, 1, 3, false, false, true, false); -- Alice offers 3
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(2, 3, 3, false), -- Charlie needs 3
(3, 1, 2, false); -- Alice needs 2

-- 1.3 Payed Match (David offers 4, Alice searches 4)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(4, 4, 4, true, false, false, false); -- David
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(4, 1, 4, false); -- Alice

-- ==========================================
-- 2. FREEBIE REQUESTS (Various Statuses)
-- ==========================================

-- 2.1 INITIAL (Eve -> Bob, Sticker 5)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(5, 2, 5, false, true, false, false);
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(5, 5, 5, false);
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, requester_closed, offerer_closed) VALUES
(1, 5, 2, 5, NULL, 'FREEBIE', 'INITIAL', false, false);

-- 2.2 MAIL_SEND (Eve -> Bob, Sticker 6)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(6, 2, 6, false, true, false, false);
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(6, 5, 6, false);
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, requester_closed, offerer_closed) VALUES
(2, 5, 2, 6, NULL, 'FREEBIE', 'MAIL_SEND', false, false);

-- 2.3 EXCHANGE_INTERREST (Accepted/Reserved) (Frank -> Bob, Sticker 7)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(7, 2, 7, false, true, false, true); -- Reserved
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(7, 6, 7, true); -- Reserved
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, offerer_card_offer_id, requester_card_search_id, requester_closed, offerer_closed) VALUES
(3, 6, 2, 7, NULL, 'FREEBIE', 'EXCHANGE_INTERREST', 7, 7, false, false);

-- 2.4 EXCHANGE_COMPLETED (Frank -> Bob, Sticker 8)
-- Cards deleted
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, requester_closed, offerer_closed) VALUES
(4, 6, 2, 8, NULL, 'FREEBIE', 'EXCHANGE_COMPLETED', true, true);

-- 2.5 EXCHANGE_CANCELED (Requester Cancelled) (Eve -> Bob, Sticker 9)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(8, 2, 9, false, true, false, false);
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(8, 5, 9, false);
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, cancellation_reason, requester_closed, offerer_closed) VALUES
(5, 5, 2, 9, NULL, 'FREEBIE', 'EXCHANGE_CANCELED', 'REQUESTER_CANCELED', false, false);

-- ==========================================
-- 3. EXCHANGE REQUESTS (Various Statuses)
-- ==========================================

-- 3.1 INITIAL (Eve -> Charlie, Sticker 10 for 11)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(9, 3, 10, false, false, true, false), -- Charlie offers 10
(10, 5, 11, false, false, true, false); -- Eve offers 11
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(9, 3, 11, false), -- Charlie needs 11
(10, 5, 10, false); -- Eve needs 10
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, requester_closed, offerer_closed) VALUES
(6, 5, 3, 10, 11, 'EXCHANGE', 'INITIAL', false, false);

-- 3.2 EXCHANGE_INTERREST (Accepted/Reserved) (Frank -> Charlie, Sticker 12 for 13)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(11, 3, 12, false, false, true, true), -- Charlie offers 12 (Reserved)
(12, 6, 13, false, false, true, true); -- Frank offers 13 (Reserved)
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(11, 3, 13, true), -- Charlie needs 13 (Reserved)
(12, 6, 12, true); -- Frank needs 12 (Reserved)
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, offerer_card_offer_id, requester_card_offer_id, offerer_card_search_id, requester_card_search_id, requester_closed, offerer_closed) VALUES
(7, 6, 3, 12, 13, 'EXCHANGE', 'EXCHANGE_INTERREST', 11, 12, 11, 12, false, false);

-- 3.3 EXCHANGE_CANCELED (Offerer Cancelled) (Eve -> Charlie, Sticker 14 for 15)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(13, 3, 14, false, false, true, false),
(14, 5, 15, false, false, true, false);
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(13, 3, 15, false),
(14, 5, 14, false);
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, cancellation_reason, requester_closed, offerer_closed) VALUES
(8, 5, 3, 14, 15, 'EXCHANGE', 'EXCHANGE_CANCELED', 'OFFERER_CANCELED', false, false);

-- ==========================================
-- 4. PAYED REQUESTS (Various Statuses)
-- ==========================================

-- 4.1 MAIL_SEND (Eve -> David, Sticker 16)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(15, 4, 16, true, false, false, false);
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(15, 5, 16, false);
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, requester_closed, offerer_closed) VALUES
(9, 5, 4, 16, NULL, 'PAYED', 'MAIL_SEND', false, false);

-- 4.2 EXCHANGE_INTERREST (Accepted/Reserved) (Frank -> David, Sticker 17)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(16, 4, 17, true, false, false, true); -- Reserved
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(16, 6, 17, true); -- Reserved
INSERT INTO exchange_requests (id, requester_id, offerer_id, requested_sticker_id, offered_sticker_id, exchange_type, status, offerer_card_offer_id, requester_card_search_id, requester_closed, offerer_closed) VALUES
(10, 6, 4, 17, NULL, 'PAYED', 'EXCHANGE_INTERREST', 16, 16, false, false);

-- ==========================================
-- 5. BULK DATA (Alice's Market)
-- ==========================================

-- Alice offers stickers 20-30 (Freebie)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(17, 1, 20, false, true, false, false),
(18, 1, 21, false, true, false, false),
(19, 1, 22, false, true, false, false),
(20, 1, 23, false, true, false, false),
(21, 1, 24, false, true, false, false),
(22, 1, 25, false, true, false, false),
(23, 1, 26, false, true, false, false),
(24, 1, 27, false, true, false, false),
(25, 1, 28, false, true, false, false),
(26, 1, 29, false, true, false, false),
(27, 1, 30, false, true, false, false);

-- Alice searches stickers 31-40
INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(17, 1, 31, false),
(18, 1, 32, false),
(19, 1, 33, false),
(20, 1, 34, false),
(21, 1, 35, false),
(22, 1, 36, false),
(23, 1, 37, false),
(24, 1, 38, false),
(25, 1, 39, false),
(26, 1, 40, false);

-- Bob offers 31-35 (Payed) - Matches for Alice
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(28, 2, 31, true, false, false, false),
(29, 2, 32, true, false, false, false),
(30, 2, 33, true, false, false, false),
(31, 2, 34, true, false, false, false),
(32, 2, 35, true, false, false, false);

-- Charlie offers 36-40 (Exchange) - Matches for Alice (if Alice has what Charlie wants)
-- Charlie wants 20-24 (which Alice has!)
INSERT INTO card_offers (id, user_id, sticker_id, offer_payed, offer_freebie, offer_exchange, is_reserved) VALUES
(33, 3, 36, false, false, true, false),
(34, 3, 37, false, false, true, false),
(35, 3, 38, false, false, true, false),
(36, 3, 39, false, false, true, false),
(37, 3, 40, false, false, true, false);

INSERT INTO card_searches (id, user_id, sticker_id, is_reserved) VALUES
(27, 3, 20, false),
(28, 3, 21, false),
(29, 3, 22, false),
(30, 3, 23, false),
(31, 3, 24, false);

-- Reset sequences to max id + 1 (Postgres compatible)
-- For SERIAL columns (users, credentials, stickers, card_offers)
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('credentials_id_seq', (SELECT MAX(id) FROM credentials));
SELECT setval('card_offers_id_seq', (SELECT MAX(id) FROM card_offers));

-- For IDENTITY columns (card_searches, exchange_requests)
ALTER TABLE card_searches ALTER COLUMN id RESTART WITH 32;
ALTER TABLE exchange_requests ALTER COLUMN id RESTART WITH 11;
