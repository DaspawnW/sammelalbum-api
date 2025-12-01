import { Page } from 'playwright';

export abstract class BasePage {
    protected page: Page;

    constructor(page: Page) {
        this.page = page;
    }

    async navigateTo(path: string) {
        await this.page.goto(`http://localhost:4200${path}`);
    }
}
