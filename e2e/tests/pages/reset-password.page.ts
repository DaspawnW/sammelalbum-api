import { BasePage } from './base.page';

export class ResetPasswordPage extends BasePage {
    async navigateWithToken(token: string) {
        await this.navigateTo(`/password-reset?token=${token}`);
    }

    async resetPassword(newPassword: string) {
        await this.page.fill('input[formControlName="password"]', newPassword);
        await this.page.fill('input[formControlName="confirmPassword"]', newPassword);
        await this.page.click('button[type="submit"]');
        // Wait for success message to appear
        await this.page.waitForSelector('div.bg-green-50:has-text("erfolgreich")', { timeout: 5000 });
    }

    async verifySuccessMessage() {
        const successMessage = await this.page.locator('div.bg-green-50').first();
        await successMessage.waitFor({ state: 'visible' });
    }

    async verifyInvalidTokenError() {
        const errorMessage = await this.page.locator('div.bg-red-50:has-text("Ungültiger")').first();
        await errorMessage.waitFor({ state: 'visible' });
    }

    async verifyPasswordMismatchError() {
        const errorMessage = await this.page.locator('p.text-red-600:has-text("übereinstimmen")').first();
        await errorMessage.waitFor({ state: 'visible' });
    }

    async verifyPasswordTooShortError() {
        const errorMessage = await this.page.locator('p.text-red-600:has-text("mindestens 8")').first();
        await errorMessage.waitFor({ state: 'visible' });
    }

    async waitForRedirectToLogin() {
        // Wait for auto-redirect after 3 seconds
        await this.page.waitForURL('**/login', { timeout: 5000 });
    }
}
