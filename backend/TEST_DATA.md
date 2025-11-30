# Test Data Documentation

This document describes the comprehensive test data set provided in `test-data.sql`. This data covers all major use cases, exchange types, statuses, and edge cases.

## Users

All users have the password: `testuser`

| Username | Role / Persona | Description |
| :--- | :--- | :--- |
| `testuser-1` | **Alice** (The Active User) | Has many matches, offers (20-30), and searches (31-40). |
| `testuser-2` | **Bob** (The Giver) | Focuses on freebie offers. |
| `testuser-3` | **Charlie** (The Trader) | Focuses on exchange offers. |
| `testuser-4` | **David** (The Seller) | Focuses on payed offers. |
| `testuser-5` | **Eve** (The Requester) | Has pending and cancelled requests. |
| `testuser-6` | **Frank** (The Completer) | Has accepted and completed exchanges. |

## Scenarios & Use Cases

### 1. Match Discovery (New User Experience)
**Objective**: Verify that a new user (Alice) immediately sees relevant matches of different types.
*   **Freebie Match**: Bob offers **Sticker 1**, Alice searches **Sticker 1**. Tests simple 1-way matching.
*   **Exchange Match**: Charlie offers **Sticker 2** (needs 3), Alice offers **Sticker 3** (needs 2). Tests complex 2-way mutual matching logic.
*   **Payed Match**: David offers **Sticker 4**, Alice searches **Sticker 4**. Tests payed offer matching.

### 2. Request Lifecycle: Freebie
**Objective**: Validate the full state machine of a Freebie request.
*   **Creation (`INITIAL`)**: Eve -> Bob (**Sticker 5**). Tests request creation before mail processing.
*   **Notification (`MAIL_SEND`)**: Eve -> Bob (**Sticker 6**). Tests that system has processed the request and "sent mail".
*   **Acceptance & Reservation (`EXCHANGE_INTERREST`)**: Frank -> Bob (**Sticker 7**).
    *   **Key Test**: Verify that Bob's Offer and Frank's Search are marked `is_reserved=true`.
    *   **Key Test**: Verify these cards do **not** appear in other match results.
*   **Completion (`EXCHANGE_COMPLETED`)**: Frank -> Bob (**Sticker 8**).
    *   **Key Test**: Verify that the cards are **deleted** from the database.
*   **Cancellation (`EXCHANGE_CANCELED`)**: Eve -> Bob (**Sticker 9**).
    *   **Reason**: `REQUESTER_CANCELED`.
    *   **Key Test**: Verify cards are **not** reserved (released).

### 3. Request Lifecycle: Exchange (Mutual)
**Objective**: Validate the complex state machine of a Mutual Exchange (2 cards involved).
*   **Creation (`INITIAL`)**: Eve -> Charlie (**Sticker 10** for **11**).
*   **Acceptance & Reservation (`EXCHANGE_INTERREST`)**: Frank -> Charlie (**Sticker 12** for **13**).
    *   **Key Test**: Verify **ALL 4** cards (Frank's Offer/Search, Charlie's Offer/Search) are reserved.
*   **Cancellation (`EXCHANGE_CANCELED`)**: Eve -> Charlie (**Sticker 14** for **15**).
    *   **Reason**: `OFFERER_CANCELED`. Tests cancellation by the other party.

### 4. Request Lifecycle: Payed
**Objective**: Validate Payed request flow.
*   **Notification (`MAIL_SEND`)**: Eve -> David (**Sticker 16**).
*   **Acceptance (`EXCHANGE_INTERREST`)**: Frank -> David (**Sticker 17**). Tests reservation of payed offer.

### 5. Bulk Data & Filtering (Alice's Market)
**Objective**: Test UI performance, pagination, and filtering with larger datasets.
*   **Alice's Inventory**: 11 Offers (Stickers 20-30), 10 Searches (Stickers 31-40).
*   **Mixed Matches**:
    *   Bob provides **Payed** matches for Alice's searches (31-35).
    *   Charlie provides **Exchange** matches for Alice's searches (36-40) wanting her offers (20-24).
*   **Key Tests**:
    *   **Pagination**: Verify lists handle >10 items.
    *   **Filtering**: Filter matches by "Payed Only" vs "Exchange Only".
    *   **Sorting**: Sort matches by sticker name or date.
