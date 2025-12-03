import { BasePage } from './base.page';

export class LoginPage extends BasePage {
    async login(username: string, password: string = 'testuser') {
        await this.navigateTo('/login');
        await this.page.fill('input[formControlName="username"]', username);
        await this.page.fill('input[formControlName="password"]', password);
        await this.page.click('button[type="submit"]');
        await this.page.waitForURL('**/dashboard');
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
