# Profile Management Test Suite

## Update Profile Information
* Register as new user with name "profile-test-user" and password "testuser"
* Navigate to "Dashboard" page
* Open edit profile modal
* Update profile with firstname "UpdatedFirst" and lastname "UpdatedLast"
* Verify profile data shows firstname "UpdatedFirst" and lastname "UpdatedLast"
* Logout

## Update Contact Information
* Login as "profile-test-user"
* Open edit profile modal
* Update profile with contact "updated-contact@example.com"
* Verify profile data shows contact "updated-contact@example.com"
* Logout

## Change Password
* Login as "profile-test-user"
* Open change password modal
* Change password from "testuser" to "newpassword123"
* Verify password change success
* Logout
* Login as "profile-test-user" with password "newpassword123"
* Navigate to "Dashboard" page
* Logout
