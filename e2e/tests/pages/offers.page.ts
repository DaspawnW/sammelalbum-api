import { BasePage } from './base.page';

export class OffersPage extends BasePage {
    async navigate() {
        await this.navigateTo('/offers');
    }

    async createOffer(stickerName: string, types: string[] = ['exchange']) {
        await this.page.click('button:has-text("Angebot erstellen")');
        // Wait for modal to be visible
        await this.page.waitForSelector('h3:has-text("Neue Angebote erstellen")', { state: 'visible' });
        // Extract the ID from the sticker name (e.g., "Sticker 45" -> "45")
        const stickerId = stickerName.replace(/\D/g, '');
        await this.page.fill('textarea', stickerId);

        // Uncheck default "Tausch" first if it's checked by default (it is in the code I saw earlier)
        // Actually, let's just check/uncheck based on input.
        // The UI might have Tausch checked by default.
        // Let's uncheck all first to be safe, or just check what we need.
        // The previous code clicked 'Tausch' to select it (assuming it was unchecked?).
        // Let's assume they start unchecked or we need to ensure the state.
        // Best way: check if checked, if not match desired, click.

        const typeMap: { [key: string]: string } = {
            'exchange': 'Tausch',
            'freebie': 'Geschenk',
            'payed': 'Verkauf'
        };

        // Helper to set checkbox state
        const setCheckbox = async (labelText: string, shouldBeChecked: boolean) => {
            // Scope to modal to avoid matching table content
            const modal = this.page.locator('div').filter({ has: this.page.locator('h3:has-text("Neue Angebote erstellen")') }).last();
            const checkbox = modal.locator(`label:has-text("${labelText}") input[type="checkbox"]`);

            const isChecked = await checkbox.isChecked();
            if (isChecked !== shouldBeChecked) {
                await checkbox.click({ force: true });
            }
        };

        await setCheckbox('Tausch', types.includes('exchange'));
        await setCheckbox('Geschenk', types.includes('freebie'));
        await setCheckbox('Verkauf', types.includes('payed'));

        // Click the submit button with force to bypass any overlay issues
        const submitBtn = this.page.locator('button:has-text("Erstellen")').last();
        await submitBtn.waitFor({ state: 'visible' });

        // Wait for button to be enabled
        if (await submitBtn.isDisabled()) {
            throw new Error("Submit button is disabled");
        }

        await submitBtn.click({ force: true });

        // Wait for modal to close
        await this.page.locator('h3:has-text("Neue Angebote erstellen")').waitFor({ state: 'hidden' });

        // Wait for the API call to complete and table to refresh
        await this.page.waitForTimeout(2000);
        // Wait for the offer to appear in the table (check for the sticker name)
        await this.page.waitForSelector(`td:has-text("${stickerName}")`, { timeout: 10000 });
    }

    async deleteOffer(stickerName: string) {
        // Find row with sticker name
        const row = this.page.locator(`tr:has-text("${stickerName}")`);
        await row.locator('a:has-text("Löschen")').click();
        // Wait for modal and click the specific delete button inside it
        // Use exact match to avoid matching "Ausgewählte löschen"
        await this.page.locator('button').filter({ hasText: /^Löschen$/ }).click();
        await this.page.waitForSelector(`td:has-text("${stickerName}")`, { state: 'hidden' });
    }

    async verifyOfferExists(stickerName: string) {
        await this.page.waitForSelector(`td:has-text("${stickerName}")`);
    }

    async verifyOfferNotExists(stickerName: string) {
        // Wait a bit for the table to refresh after deletion
        await this.page.waitForTimeout(2000);
        // Check that no elements with this sticker name exist (exact match)
        const count = await this.page.locator(`td`, { hasText: new RegExp(`^${stickerName}$`) }).count();
        if (count !== 0) {
            throw new Error(`Expected no offers for "${stickerName}", but found ${count}`);
        }
    }

    async verifyOfferIsReserved(stickerName: string) {
        const row = this.page.locator(`tr:has-text("${stickerName}")`);
        await row.locator('span:has-text("Reserviert")').waitFor({ state: 'visible' });
    }

    async verifyOfferIsNotReserved(stickerName: string) {
        const row = this.page.locator(`tr:has-text("${stickerName}")`);
        await row.locator('span:has-text("Reserviert")').waitFor({ state: 'hidden' });
    }
}
