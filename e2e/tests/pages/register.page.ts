import { BasePage } from './base.page';

export class RegisterPage extends BasePage {
    async navigate() {
        await this.navigateTo('/register');
    }

    async register(user: {
        username: string,
        mail: string,
        password?: string,
        firstname: string,
        lastname: string,
        contact?: string,
        validationCode?: string
    }) {
        await this.page.fill('input[formControlName="username"]', user.username);
        await this.page.fill('input[formControlName="mail"]', user.mail);
        await this.page.fill('input[formControlName="password"]', user.password || 'testuser');
        await this.page.fill('input[formControlName="firstname"]', user.firstname);
        await this.page.fill('input[formControlName="lastname"]', user.lastname);
        if (user.contact) {
            await this.page.fill('input[formControlName="contact"]', user.contact);
        }
        await this.page.fill('input[formControlName="validationCode"]', user.validationCode || 'CODE-1111');

        await this.page.click('button[type="submit"]');

        // Check for error message
        try {
            const errorSelector = 'div.bg-red-50 h3';
            await this.page.waitForSelector(errorSelector, { timeout: 2000 });
            const errorMsg = await this.page.textContent(errorSelector);
            throw new Error(`Registration failed: ${errorMsg}`);
        } catch (e) {
            // If timeout, it means no error message appeared (hopefully)
            if (e instanceof Error && e.message.includes('Registration failed:')) {
                throw e;
            }
        }

        // Wait for successful registration (redirect to dashboard or login)
        // Based on AuthController, it returns a token, so frontend likely redirects to dashboard
        await this.page.waitForURL('**/dashboard');
    }
}
