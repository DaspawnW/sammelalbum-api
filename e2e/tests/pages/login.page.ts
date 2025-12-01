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
        await this.page.click('button:has-text("Abmelden")');
        await this.page.waitForURL('http://localhost:4200/welcome');
    }
}
