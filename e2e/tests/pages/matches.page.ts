import { BasePage } from './base.page';

export class MatchesPage extends BasePage {
    async navigate() {
        await this.navigateTo('/matches');
    }

    async selectTab(tabName: 'Exchange' | 'Freebie' | 'Payed') {
        let index = 1;
        switch (tabName) {
            case 'Exchange': index = 1; break;
            case 'Freebie': index = 2; break;
            case 'Payed': index = 3; break;
        }
        const tabSelector = `nav[aria-label="Tabs"] a:nth-of-type(${index})`;
        await this.page.click(tabSelector);
        // Wait for tab to become active (border-psv-green class)
        await this.page.waitForSelector(`${tabSelector}.border-psv-green`);
        // Wait for the API call to complete and table to refresh
        await this.page.waitForTimeout(5000);
    }

    async verifyMatch(stickerName: string) {
        // Wait for table to be visible
        await this.page.waitForSelector('table', { state: 'visible' });
        await this.page.waitForSelector(`span:has-text("${stickerName}")`, { state: 'visible' });
    }

    async initiateExchange(requestedSticker: string, offeredSticker?: string) {
        // Ensure modal is closed before starting
        const modalTitle = this.page.locator('h3:has-text("Tauschanfrage erstellen")');
        if (await modalTitle.isVisible()) {
            await this.page.click('button:has-text("Abbrechen")');
            await modalTitle.waitFor({ state: 'hidden' });
        }

        // Find row with requested sticker
        const row = this.page.locator(`tr:has-text("${requestedSticker}")`);
        await row.locator('button:has-text("Tauschanfrage anlegen")').click({ force: true });

        // Wait for modal to open
        await modalTitle.waitFor({ state: 'visible' });

        // Modal interaction
        // Exchange logic: Select Give/Get
        // This assumes the modal structure. Adjust selectors as needed.
        // For simplicity, we might need to select by text content in dropdowns or similar.
        // This part is tricky without seeing the exact HTML structure of the modal.
        // Assuming we just click "Add Pair" and "Create" for now if pre-filled, 
        // Select requested sticker (First dropdown - Get/Requested)
        // Check if we are in Exchange mode (dropdowns) or Freebie/Payed mode (checkboxes)
        const isExchangeMode = await this.page.locator('select').count() > 0;

        if (isExchangeMode) {
            // Exchange logic: Select Give/Get
            const firstSelect = this.page.locator('select').first();
            await firstSelect.selectOption({ label: requestedSticker });
            await firstSelect.dispatchEvent('change');

            if (offeredSticker) {
                // Select offered sticker (Second dropdown - Give/Offered)
                const secondSelect = this.page.locator('select').nth(1);
                await secondSelect.selectOption({ label: offeredSticker });
                await secondSelect.dispatchEvent('change');
            }

            // Add to exchange
            const addBtn = this.page.locator('button:has-text("+")');
            // Wait for button to be enabled
            await this.page.waitForTimeout(500);
            if (await addBtn.isDisabled()) {
                throw new Error("Add button is disabled");
            }
            await addBtn.click();

        } else {
            // Freebie/Payed logic: Checkbox list
            // Find the item in the list
            const itemRow = this.page.locator(`li:has-text("${requestedSticker}")`);
            if (await itemRow.count() === 0) {
                throw new Error(`Item "${requestedSticker}" not found in the list.`);
            }

            // Check if already selected (optional, but good practice)
            const checkbox = itemRow.locator('input[type="checkbox"]');
            if (!(await checkbox.isChecked())) {
                await itemRow.click();
            }
        }

        // Verify pair is added (for Exchange) or items selected (for Freebie)
        // Log modal content to debug
        const modalBody = this.page.locator('.modal-body, mat-dialog-content, .dialog-content, div[role="dialog"]');
        if (await modalBody.count() > 0) {
            // console.log("Modal content:", await modalBody.innerText());
        } else {
            console.log("Modal body not found, logging all text:", await this.page.locator('body').innerText());
        }

        // Wait for submit button to be enabled
        const submitBtn = this.page.locator('button:has-text("Anfrage erstellen")');
        await submitBtn.waitFor({ state: 'visible' });

        // Wait a bit longer for Angular to update validity
        await this.page.waitForTimeout(1000);

        // Check if enabled
        if (await submitBtn.isDisabled()) {
            console.error("Submit button is disabled!");
            throw new Error("Submit button is disabled");
        }

        // Wait a bit for Angular to update validity
        await this.page.waitForTimeout(1000);

        await submitBtn.click(); // Remove force to ensure it's clickable

        // Wait for modal to close
        await modalTitle.waitFor({ state: 'hidden' });
    }
}
