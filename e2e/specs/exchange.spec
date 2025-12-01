# Exchange Lifecycle

## Complete an Exchange
* Login as "testuser-1"
* Navigate to "Matches" page
* Select "Exchange" tab
* Initiate exchange request for "Sticker 2" offering "Sticker 3"
* Logout

* Login as "testuser-3"
* Navigate to "Exchanges" page
* Accept the exchange request for "Sticker 3"
* Verify status changes to "EXCHANGE_INTERREST" for "Sticker 3"

* Navigate to "Offers" page
* Verify offer for "Sticker 2" is reserved
* Navigate to "Searches" page
* Verify search for "Sticker 3" is reserved
* Logout

* Login as "testuser-1"
* Navigate to "Offers" page
* Verify offer for "Sticker 3" is reserved
* Navigate to "Searches" page
* Verify search for "Sticker 2" is reserved
* Logout

* Login as "testuser-3"
* Navigate to "Exchanges" page
* Complete the exchange request for "Sticker 3"
* Verify status changes to "STATUS_RECEIVED" for "Sticker 3"

* Navigate to "Offers" page
* Verify offer for "Sticker 2" is deleted
* Navigate to "Searches" page
* Verify search for "Sticker 3" is deleted
* Logout

* Login as "testuser-1"
* Navigate to "Exchanges" page
* Complete the exchange request for "Sticker 3"
* Verify status changes to "EXCHANGE_COMPLETED" for "Sticker 3"
* Navigate to "Offers" page
* Verify offer for "Sticker 3" is deleted
* Navigate to "Searches" page
* Verify search for "Sticker 2" is deleted

## Cancel an Exchange
* Login as "testuser-3"
* Navigate to "Exchanges" page
* Accept the exchange request for "Sticker 10"
* Logout

* Login as "testuser-5"
* Navigate to "Offers" page
* Verify offer for "Sticker 11" is reserved
* Navigate to "Searches" page
* Verify search for "Sticker 10" is reserved
* Logout

* Login as "testuser-3"
* Navigate to "Searches" page
* Verify search for "Sticker 11" is reserved
* Navigate to "Offers" page
* Verify offer for "Sticker 10" is reserved
* Logout

* Login as "testuser-5"
* Navigate to "Exchanges" page
* Cancel the exchange request for "Sticker 10"
* Verify status changes to "EXCHANGE_CANCELED" for "Sticker 10"
* Navigate to "Offers" page
* Verify offer for "Sticker 11" is not reserved
* Navigate to "Searches" page
* Verify search for "Sticker 10" is not reserved
* Logout

* Login as "testuser-3"
* Navigate to "Searches" page
* Verify search for "Sticker 11" is not reserved
* Navigate to "Offers" page
* Verify offer for "Sticker 10" is not reserved

## Delete Reserved Offer
* Login as "testuser-3"
* Navigate to "Offers" page
* Verify offer for "Sticker 12" is reserved
* Delete offer for "Sticker 12"
* Verify "Sticker 12" is removed from the offers list
* Logout

* Login as "testuser-6"
* Navigate to "Exchanges" page
* Verify status changes to "EXCHANGE_CANCELED" for "Sticker 13"
* Navigate to "Offers" page
* Verify offer for "Sticker 13" is not reserved
* Navigate to "Searches" page
* Verify search for "Sticker 12" is not reserved

