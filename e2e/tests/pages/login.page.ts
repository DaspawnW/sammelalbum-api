import { BasePage } from './base.page';

export class LoginPage extends BasePage {
    async login(username: string, password: string = 'testuser') {
        await this.navigateTo('/login');
        await this.page.fill('input[formControlName="username"]', username);
        await this.page.fill('input[formControlName="password"]', password);
        await this.page.click('button[type="submit"]');
        await this.page.waitForURL('**/dashboard');
    }

    async attemptLogin(username: string, password: string) {
        await this.navigateTo('/login');
        await this.page.fill('input[formControlName="username"]', username);
        await this.page.fill('input[formControlName="password"]', password);
        await this.page.click('button[type="submit"]');
        // Wait for error to appear (don't wait for URL change)
        await this.page.waitForTimeout(1000);
    }

    async verifyLoginError() {
        // Check for error message
        const errorVisible = await this.page.isVisible('text=Ungültiger Benutzername oder Passwort');
        if (!errorVisible) {
            throw new Error('Expected login error message to be visible');
        }
    }

    async verifyLoggedOut() {
        // Verify we're on the welcome page
        await this.page.waitForURL('http://localhost:4200/welcome');
        const currentUrl = this.page.url();
        if (!currentUrl.includes('/welcome')) {
            throw new Error(`Expected to be on welcome page but got: ${currentUrl}`);
        }
    }

    async logout() {
        // Click the user menu button to open dropdown
        await this.page.click('#user-menu-button');
        // Wait for menu animation
        await this.page.waitForTimeout(300);
        // Click the logout link in the dropdown
        await this.page.click('a:has-text("Abmelden")');
        // Wait for redirect to welcome page
        await this.page.waitForURL('http://localhost:4200/welcome');
    }
}
