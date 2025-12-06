# Password Reset Workflow Test Suite

## Request Password Reset
* Register as new user with name "password-reset-user" and password "oldpassword123"
* Logout
* Navigate to forgot password page
* Request password reset for "password-reset-user"
* Verify password reset request success message
* Generate password reset token for user "password-reset-user"
* Navigate to reset password page with token "token"
* Reset password to "newpassword456"
* Verify password reset success message
* Wait for redirect to login page
* Login as "password-reset-user" with password "newpassword456"
* Navigate to "Dashboard" page
* Logout

## Password Validation Errors
* Register as new user with name "validation-test-user" and password "testpass"
* Logout
* Navigate to forgot password page
* Request password reset for "validation-test-user"
* Generate password reset token for user "validation-test-user"
* Navigate to reset password page with token "token"
* Enter password "short" and confirm password "different"
* Verify password too short error
* Verify password mismatch error
