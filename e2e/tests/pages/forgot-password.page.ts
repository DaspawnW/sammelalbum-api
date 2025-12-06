import { BasePage } from './base.page';

export class ForgotPasswordPage extends BasePage {
    async navigate() {
        await this.navigateTo('/forgot-password');
    }

    async requestPasswordReset(identifier: string) {
        await this.page.fill('input[formControlName="identifier"]', identifier);
        await this.page.click('button[type="submit"]');
        // Wait for success message to appear
        await this.page.waitForSelector('div.bg-green-50:has-text("Falls ein Konto")', { timeout: 5000 });
    }

    async verifySuccessMessage() {
        const successMessage = await this.page.locator('div.bg-green-50').first();
        await successMessage.waitFor({ state: 'visible' });
    }

    async clickBackToLogin() {
        await this.page.click('a:has-text("Zurück zur Anmeldung")');
        await this.page.waitForURL('**/login');
    }
}
