import { BasePage } from './base.page';

export class ExchangesPage extends BasePage {
    async navigate() {
        await this.navigateTo('/exchanges');
    }

    async verifyIncomingRequest(stickerName: string, partnerName?: string) {
        // Ensure "Offen" filter is checked
        await this.toggleFilter("Offen", true);

        try {
            if (partnerName) {
                await this.page.waitForSelector(`tr:has-text("${stickerName}"):has-text("${partnerName}")`, { timeout: 5000 });
            } else {
                await this.page.waitForSelector(`tr:has-text("${stickerName}")`, { timeout: 5000 });
            }
        } catch (e) {
            throw e;
        }
    }

    async acceptRequest(stickerName: string) {
        const row = this.page.locator(`tr:has-text("${stickerName}")`);
        await row.locator('button:has-text("Ansehen")').click(); // "View"
        await this.page.waitForSelector('h3:has-text("Tauschdetails")'); // Modal title
        await this.page.click('button:has-text("Tausch abstimmen")'); // Accept

        // Wait for status update (might be in modal or table)
        await this.page.waitForSelector(':text("Interesse")');

        // Close modal by clicking backdrop (assuming it's the div with rgba background)
        // Or better, click the "X" button if we can target it.
        // Let's try clicking the backdrop which is the first child of the fixed container.
        // We can target the fixed container and click top-left.
        await this.page.mouse.click(10, 10);

        // Wait for modal to close
        await this.page.waitForSelector('h3:has-text("Tauschdetails")', { state: 'hidden' });
    }

    async completeExchange(stickerName: string) {
        const row = this.page.locator(`tr:has-text("${stickerName}")`);
        await row.locator('button:has-text("Ansehen")').click();
        await this.page.waitForSelector('h3:has-text("Tauschdetails")');

        await this.page.click('button:has-text("Transaktion abschließen")'); // Close
        // Confirm warning modal
        await this.page.waitForSelector('h3:has-text("Transaktion abschließen?")');
        await this.page.click('button:has-text("Abschließen bestätigen")');

        // Wait for modals to close
        await this.page.waitForSelector('h3:has-text("Transaktion abschließen?")', { state: 'hidden' });
        await this.page.waitForSelector('h3:has-text("Tauschdetails")', { state: 'hidden' });

        // Reload to ensure fresh state and default filters
        await this.page.reload();
        await this.page.waitForLoadState('networkidle');
    }

    async cancelExchange(stickerName: string) {
        const row = this.page.locator(`tr:has-text("${stickerName}")`);
        await row.locator('button:has-text("Ansehen")').click();
        await this.page.waitForSelector('h3:has-text("Tauschdetails")');

        await this.page.click('button:has-text("Tausch ablehnen")'); // Decline

        // Wait for modal to close
        await this.page.waitForSelector('h3:has-text("Tauschdetails")', { state: 'hidden' });

        // Wait for modal to close
        await this.page.waitForSelector('h3:has-text("Tauschdetails")', { state: 'hidden' });

        // Removed force reload to speed up test. Assuming UI updates correctly.
        // If filter needs reset, we might need to handle it, but let's try without reload first.

        await this.selectFilter("Abgebrochen");
        await this.verifyStatus(stickerName, "EXCHANGE_CANCELED");
    }

    async verifyStatus(stickerName: string, status: string) {
        // Translate status key to text (simplified map for test)
        let translatedStatus = status;
        let filterStatus = status;
        let alternativeStatus = "";

        if (status === "EXCHANGE_INTERREST" || status === "Interesse") {
            translatedStatus = "Interesse";
            filterStatus = "Interesse";
            alternativeStatus = "Bereits"; // Allow "Bereits erhalten" or "Bereits abgegeben"
        }
        if (status === "EXCHANGE_COMPLETED" || status === "Abgeschlossen") {
            translatedStatus = "Abgeschlossen";
            filterStatus = "Abgeschlossen";
        }
        if (status === "EXCHANGE_CANCELED" || status === "Abgebrochen") {
            translatedStatus = "Abgebrochen";
            filterStatus = "Abgebrochen";
        }
        if (status === "INITIAL" || status === "Offen") {
            translatedStatus = "Offen";
            filterStatus = "Offen";
        }
        if (status === "STATUS_RECEIVED" || status === "Bereits erhalten") {
            translatedStatus = "Bereits";
            filterStatus = "Interesse";
        }

        // For CANCELED, we must ensure "Interesse" and "Offen" are NOT checked, 
        // because the backend might fail to update status and we want to verify that.
        if (status === "EXCHANGE_CANCELED" || status === "Abgebrochen") {
            await this.toggleFilter("Interesse", false);
            await this.toggleFilter("Offen", false);
        }

        // Ensure the status is visible in the filter
        await this.toggleFilter(filterStatus, true);

        const row = this.page.locator(`tr:has-text("${stickerName}")`).first();
        try {
            await row.waitFor({ state: 'visible', timeout: 10000 });
        } catch (e) {
            throw new Error(`Row for sticker "${stickerName}" not found in table.`);
        }

        const text = await row.innerText();
        if (!text.includes(translatedStatus) && (!alternativeStatus || !text.includes(alternativeStatus))) {
            throw new Error(`Row for "${stickerName}" found but status "${translatedStatus}" ${alternativeStatus ? 'or "' + alternativeStatus + '"' : ''} not found. Actual text: "${text}"`);
        }
    }

    async selectFilter(status: string) {
        await this.toggleFilter(status, true);
    }

    async toggleFilter(status: string, checked: boolean) {
        await this.page.click('button:has-text("Status")');
        // Wait for dropdown to appear
        const filterItem = this.page.locator('div[role="menu"] div.flex.items-center').filter({ hasText: status }).first();
        const checkbox = filterItem.locator('input[type="checkbox"]');

        await checkbox.waitFor({ state: 'visible' });

        const isChecked = await checkbox.isChecked();
        if (isChecked !== checked) {
            await checkbox.click();
        }
        // Close dropdown by clicking outside to avoid accidental clicks on items
        await this.page.mouse.click(0, 0);
    }

    async verifySentRequest(stickerName: string) {
        await this.verifyIncomingRequest(stickerName);
    }
}
