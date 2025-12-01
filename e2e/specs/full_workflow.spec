# Full Workflow Test Suite

## Full Exchange Lifecycle with Two Users
* Register as new user with name "full-flow-1" and password "testuser"
* Navigate to "Offers" page
* Create offer for "Sticker 110" with types "exchange"
* Create offer for "Sticker 111" with types "freebie"
* Create offer for "Sticker 112" with types "payed"
* Create offer for "Sticker 113" with types "exchange, freebie"
* Navigate to "Searches" page
* Create search for "Sticker 120"
* Create search for "Sticker 121"
* Logout

* Register as new user with name "full-flow-2" and password "testuser"
* Navigate to "Offers" page
* Create offer for "Sticker 120" with types "exchange"
* Create offer for "Sticker 121" with types "freebie"
* Navigate to "Searches" page
* Create search for "Sticker 110"
* Create search for "Sticker 111"
* Logout

* Login as "full-flow-1"
* Navigate to "Matches" page
* Select "Exchange" tab
* Verify "Sticker 120" is listed as a match (requested by Alice)
* Initiate exchange request for "Sticker 120" offering "Sticker 110"
* Navigate to "Exchanges" page
* Verify sent request for "Sticker 120"
* Logout

* Login as "full-flow-2"
* Navigate to "Exchanges" page
* Verify incoming request for "Sticker 120"
* Accept the exchange request for "Sticker 120"
* Verify status changes to "Interesse" for "Sticker 120"
* Complete the exchange request for "Sticker 120"
* Logout

* Login as "full-flow-1"
* Navigate to "Exchanges" page
* Verify status changes to "Interesse" for "Sticker 120"
* Complete the exchange request for "Sticker 120"
* Verify status changes to "Abgeschlossen" for "Sticker 120"
* Navigate to "Offers" page
* Verify "Sticker 110" is removed from the offers list
* Navigate to "Searches" page
* Verify "Sticker 120" is removed from the searches list
* Logout

## Cancelled Exchange Scenario
* Login as "full-flow-1"
* Navigate to "Matches" page
* Select "Freebie" tab
* Verify "Sticker 121" is listed as a match (requested by Alice)
* Initiate freebie request for "Sticker 121"
* Logout

* Login as "full-flow-2"
* Navigate to "Exchanges" page
* Verify incoming request for "Sticker 121"
* Accept the exchange request for "Sticker 121"
* Cancel the exchange request for "Sticker 121"
* Verify status changes to "Abgebrochen" for "Sticker 121"
* Logout

* Login as "full-flow-1"
* Navigate to "Exchanges" page
* Verify status changes to "Abgebrochen" for "Sticker 121"
* Navigate to "Offers" page
* Verify "Sticker 111" appears in the offers list
* Navigate to "Searches" page
* Verify "Sticker 121" appears in the searches list
* Logout
