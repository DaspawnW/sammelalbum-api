import { BasePage } from './base.page';

export class SearchPage extends BasePage {
    async navigate() {
        await this.navigateTo('/searches');
    }

    async createSearch(stickerName: string) {
        await this.page.click('button:has-text("Suchanfrage erstellen")');
        // Wait for modal to be visible
        await this.page.waitForSelector('h3:has-text("Neue Suchanfragen erstellen")', { state: 'visible' });
        // Extract the ID from the sticker name (e.g., "Sticker 46" -> "46")
        const stickerId = stickerName.replace(/\D/g, '');
        // Wait a moment for overlay to stabilize
        await this.page.waitForTimeout(500);
        await this.page.fill('textarea', stickerId);
        // Wait a moment for the modal to fully render and overlay to clear
        await this.page.waitForTimeout(1000);
        // Click the submit button (without force to allow Angular event handling)
        await this.page.locator('button:has-text("Erstellen")').last().click();
        // Wait for the API call to complete and table to refresh
        await this.page.waitForTimeout(5000);
        // Wait for the search to appear in the table
        await this.page.waitForSelector(`td:has-text("${stickerName}")`, { timeout: 10000 });
    }

    async deleteSearch(stickerName: string) {
        const row = this.page.locator(`tr:has-text("${stickerName}")`);
        await row.locator('a:has-text("Löschen")').click();
        await this.page.click('button:has-text("Löschen")'); // Confirm in modal
        await this.page.waitForSelector(`td:has-text("${stickerName}")`, { state: 'hidden' });
    }

    async verifySearchExists(stickerName: string) {
        await this.page.waitForSelector(`td:has-text("${stickerName}")`);
    }

    async verifySearchNotExists(stickerName: string) {
        // Wait a bit for the table to refresh after deletion
        await this.page.waitForTimeout(2000);
        // Check that no elements with this sticker name exist
        const count = await this.page.locator(`td:has-text("${stickerName}")`).count();
        if (count !== 0) {
            throw new Error(`Expected no searches for "${stickerName}", but found ${count}`);
        }
    }

    async verifySearchIsReserved(stickerName: string) {
        const row = this.page.locator(`tr:has-text("${stickerName}")`);
        await row.locator('span:has-text("Reserviert")').waitFor({ state: 'visible' });
    }

    async verifySearchIsNotReserved(stickerName: string) {
        const row = this.page.locator(`tr:has-text("${stickerName}")`);
        await row.locator('span:has-text("Reserviert")').waitFor({ state: 'hidden' });
    }
}
