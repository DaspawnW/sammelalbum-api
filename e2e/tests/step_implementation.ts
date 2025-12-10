import { Step, BeforeSuite, AfterSuite, BeforeScenario, AfterScenario, BeforeSpec } from "gauge-ts";
import { chromium, Browser, Page, BrowserContext } from "playwright";
import { LoginPage } from "./pages/login.page";
import { OffersPage } from "./pages/offers.page";
import { SearchPage } from "./pages/search.page";
import { MatchesPage } from "./pages/matches.page";
import { ExchangesPage } from "./pages/exchanges.page";
import { ProfilePage } from "./pages/profile.page";
import { ForgotPasswordPage } from "./pages/forgot-password.page";
import { ResetPasswordPage } from "./pages/reset-password.page";
import { RegisterPage } from "./pages/register.page";

export default class StepImplementation {
    private static browser: Browser;
    private static context: BrowserContext;
    private static page: Page;

    private static loginPage: LoginPage;
    private static registerPage: RegisterPage;
    private static offersPage: OffersPage;
    private static searchPage: SearchPage;
    private static matchesPage: MatchesPage;
    private static exchangesPage: ExchangesPage;
    private static profilePage: ProfilePage;
    private static forgotPasswordPage: ForgotPasswordPage;
    private static resetPasswordPage: ResetPasswordPage;

    // Store token for password reset tests
    private static passwordResetToken: string = '';
    // Track last registered username for dynamic token generation
    private static lastRegisteredUsername: string = '';

    @BeforeSuite()
    public async beforeSuite() {
        StepImplementation.browser = await chromium.launch({ headless: true }); // Set to false for debugging
    }

    @AfterSuite()
    public async afterSuite() {
        await StepImplementation.browser.close();
    }

    @BeforeScenario()
    public async beforeScenario() {
        StepImplementation.context = await StepImplementation.browser.newContext();
        StepImplementation.page = await StepImplementation.context.newPage();

        StepImplementation.loginPage = new LoginPage(StepImplementation.page);
        StepImplementation.registerPage = new RegisterPage(StepImplementation.page);
        StepImplementation.offersPage = new OffersPage(StepImplementation.page);
        StepImplementation.searchPage = new SearchPage(StepImplementation.page);
        StepImplementation.matchesPage = new MatchesPage(StepImplementation.page);
        StepImplementation.exchangesPage = new ExchangesPage(StepImplementation.page);
        StepImplementation.profilePage = new ProfilePage(StepImplementation.page);
        StepImplementation.forgotPasswordPage = new ForgotPasswordPage(StepImplementation.page);
        StepImplementation.resetPasswordPage = new ResetPasswordPage(StepImplementation.page);

        StepImplementation.page.on('console', msg => console.log(`BROWSER LOG: ${msg.text()}`));
    }

    @AfterScenario()
    public async afterScenario() {
        await StepImplementation.context.close();
    }

    @BeforeSpec()
    public async beforeSpec() {
        // Reset database before each spec to ensure clean state
        console.log("Resetting database before spec...");
        const { exec } = require('child_process');
        await new Promise<void>((resolve, reject) => {
            exec('./reset-db.sh', (error, stdout, stderr) => {
                if (error) {
                    console.error(`exec error: ${error}`);
                    reject(error);
                    return;
                }
                console.log(`stdout: ${stdout}`);
                resolve();
            });
        });
    }

    // --- Login/Register Steps ---
    @Step("Login as <username>")
    public async login(username: string) {
        await StepImplementation.loginPage.login(username);
    }

    @Step("Login as <username> with password <password>")
    public async loginWithPassword(username: string, password: string) {
        await StepImplementation.loginPage.login(username, password);
    }

    @Step("Register as new user with name <username> and password <password>")
    public async register(username: string, password: string) {
        await StepImplementation.registerPage.navigate();
        await StepImplementation.registerPage.register({
            username: username,
            mail: `${username}@example.com`,
            password: password,
            firstname: username,
            lastname: 'User',
            contact: `${username}@example.com`
        });
        // Track the last registered username for password reset token generation
        StepImplementation.lastRegisteredUsername = username;
    }

    @Step("Logout")
    public async logout() {
        await StepImplementation.loginPage.logout();
    }

    // --- Navigation Steps ---
    @Step("Navigate to <pageName> page")
    public async navigateTo(pageName: string) {
        switch (pageName) {
            case "Dashboard":
                await StepImplementation.page.goto('http://localhost:4200/dashboard');
                break;
            case "Offers": await StepImplementation.offersPage.navigate(); break;
            case "Searches": await StepImplementation.searchPage.navigate(); break;
            case "Matches": await StepImplementation.matchesPage.navigate(); break;
            case "Exchanges": await StepImplementation.exchangesPage.navigate(); break;
        }
    }

    // --- Offers Steps ---
    @Step("Create a new offer for sticker <stickerName>")
    public async createOffer(stickerName: string) {
        await StepImplementation.offersPage.createOffer(stickerName);
    }

    @Step("Verify <stickerName> appears in the offers list")
    public async verifyOfferExists(stickerName: string) {
        await StepImplementation.offersPage.verifyOfferExists(stickerName);
    }

    @Step("Verify <stickerName> is removed from the offers list")
    public async verifyOfferRemoved(stickerName: string) {
        await StepImplementation.offersPage.verifyOfferNotExists(stickerName);
    }



    @Step("Create offer for <stickerName> with types <types>")
    public async createOfferWithTypes(stickerName: string, types: string) {
        // Parse types from comma-separated string (e.g., "exchange, freebie")
        const typeList = types.split(',').map(t => t.trim().toLowerCase());
        await StepImplementation.offersPage.createOffer(stickerName, typeList);
    }

    private selectedStickerForDeletion: string = "";

    @Step("Select offer for <stickerName>")
    public async selectOfferForDeletion(stickerName: string) {
        this.selectedStickerForDeletion = stickerName;
        // In reality, we'd click the checkbox here
        const row = StepImplementation.page.locator(`tr:has-text("${stickerName}")`).first();
        await row.locator('input[type="checkbox"]').check();
    }

    @Step("Click delete button")
    public async clickDeleteButton() {
        await StepImplementation.page.click('button:has-text("Ausgewählte löschen")');
    }

    @Step("Confirm deletion")
    public async confirmDelete() {
        // Wait a moment for the modal to fully render and overlay to clear
        await StepImplementation.page.waitForTimeout(1000);
        // Click the delete confirmation button (without force to allow Angular event handling)
        await StepImplementation.page.locator('button:has-text("Löschen")').last().click();
        // Wait for the API call to complete and table to refresh
        await StepImplementation.page.waitForTimeout(5000);
    }

    // --- Search Steps ---
    @Step("Create a new search for sticker <stickerName>")
    public async createSearch(stickerName: string) {
        await StepImplementation.searchPage.createSearch(stickerName);
    }

    @Step("Create search for <stickerName>")
    public async createSearchShort(stickerName: string) {
        await StepImplementation.searchPage.createSearch(stickerName);
    }

    @Step("Verify <stickerName> appears in the searches list")
    public async verifySearchExists(stickerName: string) {
        await StepImplementation.searchPage.verifySearchExists(stickerName);
    }

    @Step("Select search for <stickerName>")
    public async selectSearchForDeletion(stickerName: string) {
        this.selectedStickerForDeletion = stickerName;
        const row = StepImplementation.page.locator(`tr:has-text("${stickerName}")`).first();
        await row.locator('input[type="checkbox"]').check();
    }

    @Step("Verify <stickerName> is removed from the searches list")
    public async verifySearchRemoved(stickerName: string) {
        await StepImplementation.searchPage.verifySearchNotExists(stickerName);
    }

    // --- Matches Steps ---
    @Step("Select <tabName> tab")
    public async selectTab(tabName: 'Exchange' | 'Freebie' | 'Payed') {
        await StepImplementation.matchesPage.selectTab(tabName);
    }

    @Step("Verify <stickerName> is listed as a match (requested by Alice)")
    public async verifyMatchRequestedByAlice(stickerName: string) {
        await StepImplementation.matchesPage.verifyMatch(stickerName);
    }

    @Step("Verify <stickerName> is listed as a match (offered by Alice)")
    public async verifyMatchOfferedByAlice(stickerName: string) {
        await StepImplementation.matchesPage.verifyMatch(stickerName);
    }

    @Step("Initiate exchange request for <requestedSticker> offering <offeredSticker>")
    public async initiateExchange(requestedSticker: string, offeredSticker: string) {
        await StepImplementation.matchesPage.initiateExchange(requestedSticker, offeredSticker);
    }

    @Step("Initiate freebie request for <requestedSticker>")
    public async initiateFreebieRequest(requestedSticker: string) {
        await StepImplementation.matchesPage.initiateExchange(requestedSticker);
    }

    // --- Exchange Steps ---
    @Step("Verify incoming request for <stickerName> from <partnerName>")
    public async verifyIncomingRequest(stickerName: string, partnerName: string) {
        await StepImplementation.exchangesPage.verifyIncomingRequest(stickerName, partnerName);
    }

    @Step("Verify sent request for <stickerName>")
    public async verifySentRequest(stickerName: string) {
        await StepImplementation.exchangesPage.verifySentRequest(stickerName);
    }

    @Step("Verify incoming request for <stickerName>")
    public async verifyIncomingRequestAnonymous(stickerName: string) {
        await StepImplementation.exchangesPage.verifyIncomingRequest(stickerName);
    }

    @Step("Accept the exchange request for <stickerName>")
    public async acceptRequest(stickerName: string) {
        await StepImplementation.exchangesPage.acceptRequest(stickerName);
    }

    @Step("Verify status changes to <status> for <stickerName>")
    public async verifyStatus(status: string, stickerName: string) {
        await StepImplementation.exchangesPage.verifyStatus(stickerName, status);
    }

    @Step("Complete the exchange request for <stickerName>")
    public async completeExchange(stickerName: string) {
        await StepImplementation.exchangesPage.completeExchange(stickerName);
    }

    @Step("Cancel the exchange request for <stickerName>")
    public async cancelExchange(stickerName: string) {
        await StepImplementation.exchangesPage.cancelExchange(stickerName);
    }

    @Step("Verify offer for <stickerName> is reserved")
    public async verifyOfferIsReserved(stickerName: string) {
        await StepImplementation.offersPage.verifyOfferIsReserved(stickerName);
    }

    @Step("Verify offer for <stickerName> is not reserved")
    public async verifyOfferIsNotReserved(stickerName: string) {
        await StepImplementation.offersPage.verifyOfferIsNotReserved(stickerName);
    }

    @Step("Verify search for <stickerName> is reserved")
    public async verifySearchIsReserved(stickerName: string) {
        await StepImplementation.searchPage.verifySearchIsReserved(stickerName);
    }

    @Step("Verify search for <stickerName> is not reserved")
    public async verifySearchIsNotReserved(stickerName: string) {
        await StepImplementation.searchPage.verifySearchIsNotReserved(stickerName);
    }

    @Step("Verify offer for <stickerName> is deleted")
    public async verifyOfferIsDeleted(stickerName: string) {
        await StepImplementation.offersPage.verifyOfferNotExists(stickerName);
    }

    @Step("Verify search for <stickerName> is deleted")
    public async verifySearchIsDeleted(stickerName: string) {
        await StepImplementation.searchPage.verifySearchNotExists(stickerName);
    }

    // --- Profile Management Steps ---
    @Step("Open edit profile modal")
    public async openEditProfileModal() {
        await StepImplementation.profilePage.openEditProfileModal();
    }

    @Step("Update profile with firstname <firstname> and lastname <lastname>")
    public async updateProfileName(firstname: string, lastname: string) {
        await StepImplementation.profilePage.updateProfile({ firstname, lastname });
    }

    @Step("Update profile with contact <contact>")
    public async updateProfileContact(contact: string) {
        await StepImplementation.profilePage.updateProfile({ contact });
    }

    @Step("Verify profile data shows firstname <firstname> and lastname <lastname>")
    public async verifyProfileName(firstname: string, lastname: string) {
        await StepImplementation.profilePage.verifyProfileData({ firstname, lastname });
    }

    @Step("Verify profile data shows contact <contact>")
    public async verifyProfileContact(contact: string) {
        await StepImplementation.profilePage.verifyProfileData({ contact });
    }

    @Step("Open change password modal")
    public async openChangePasswordModal() {
        await StepImplementation.profilePage.openChangePasswordModal();
    }

    @Step("Change password from <currentPassword> to <newPassword>")
    public async changePassword(currentPassword: string, newPassword: string) {
        await StepImplementation.profilePage.changePassword(currentPassword, newPassword);
    }

    @Step("Verify password change success")
    public async verifyPasswordChangeSuccess() {
        await StepImplementation.profilePage.verifyPasswordChangeSuccess();
    }

    // --- Password Reset Steps ---
    @Step("Navigate to forgot password page")
    public async navigateToForgotPassword() {
        await StepImplementation.forgotPasswordPage.navigate();
    }

    @Step("Request password reset for <identifier>")
    public async requestPasswordReset(identifier: string) {
        await StepImplementation.forgotPasswordPage.requestPasswordReset(identifier);
    }

    @Step("Verify password reset request success message")
    public async verifyPasswordResetRequestSuccess() {
        await StepImplementation.forgotPasswordPage.verifySuccessMessage();
    }

    @Step("Generate password reset token for user <username>")
    public async generatePasswordResetToken(username: string) {
        // Generate a password reset token using the same JWT secret as the backend
        // This matches the implementation in PasswordResetTokenService.java
        const jwt = require('jsonwebtoken');

        // Secret from application.yaml: app.jwt.password-reset-secret
        const passwordResetSecret = 'R9mPX2vYq8wB5nZt7cKjH4fL6gD3sA1eU0iO9pMxN8y=';

        // Expiration: 2 hours (7200000 ms) from application.yaml
        const expirationTime = 7200000;

        const token = jwt.sign(
            {}, // empty claims, just like the Java implementation
            Buffer.from(passwordResetSecret, 'base64'), // decode base64 secret
            {
                algorithm: 'HS256',
                subject: username,
                expiresIn: Math.floor(expirationTime / 1000) // convert ms to seconds
            }
        );

        StepImplementation.passwordResetToken = token;
        console.log(`Generated password reset token for user: ${username}`);
    }

    @Step("Navigate to reset password page with token <token>")
    public async navigateToResetPasswordWithToken(token: string) {
        const actualToken = token === 'token' ? StepImplementation.passwordResetToken : token;
        await StepImplementation.resetPasswordPage.navigateWithToken(actualToken);
    }

    @Step("Reset password to <newPassword>")
    public async resetPassword(newPassword: string) {
        await StepImplementation.resetPasswordPage.resetPassword(newPassword);
    }

    @Step("Verify password reset success message")
    public async verifyPasswordResetSuccess() {
        await StepImplementation.resetPasswordPage.verifySuccessMessage();
    }

    @Step("Wait for redirect to login page")
    public async waitForRedirectToLogin() {
        await StepImplementation.resetPasswordPage.waitForRedirectToLogin();
    }

    @Step("Verify invalid token error is displayed")
    public async verifyInvalidTokenError() {
        await StepImplementation.resetPasswordPage.verifyInvalidTokenError();
    }

    @Step("Enter password <password> and confirm password <confirmPassword>")
    public async enterPasswords(password: string, confirmPassword: string) {
        await StepImplementation.page.fill('input[formControlName="password"]', password);
        await StepImplementation.page.fill('input[formControlName="confirmPassword"]', confirmPassword);
        // Blur the fields to trigger validation
        await StepImplementation.page.locator('input[formControlName="confirmPassword"]').blur();
    }

    @Step("Verify password too short error")
    public async verifyPasswordTooShortError() {
        await StepImplementation.resetPasswordPage.verifyPasswordTooShortError();
    }

    @Step("Verify password mismatch error")
    public async verifyPasswordMismatchError() {
        await StepImplementation.resetPasswordPage.verifyPasswordMismatchError();
    }

    // --- User Deletion Steps ---
    @Step("Open delete account modal")
    public async openDeleteAccountModal() {
        await StepImplementation.profilePage.openDeleteAccountModal();
    }

    @Step("Confirm account deletion")
    public async confirmAccountDeletion() {
        await StepImplementation.profilePage.confirmAccountDeletion();
    }

    @Step("Verify user is logged out")
    public async verifyUserIsLoggedOut() {
        await StepImplementation.loginPage.verifyLoggedOut();
    }

    @Step("Attempt to login as <username> with password <password>")
    public async attemptLogin(username: string, password: string) {
        await StepImplementation.loginPage.attemptLogin(username, password);
    }

    @Step("Verify login fails with error message")
    public async verifyLoginFails() {
        await StepImplementation.loginPage.verifyLoginError();
    }
}
