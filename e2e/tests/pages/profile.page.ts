import { BasePage } from './base.page';

export class ProfilePage extends BasePage {
    async openEditProfileModal() {
        // Click user menu button
        await this.page.click('#user-menu-button');
        // Wait for menu to open
        await this.page.waitForTimeout(300);
        // Click "Edit Profile" option
        await this.page.click('a:has-text("Profil bearbeiten")');
        // Wait for modal to appear
        await this.page.waitForTimeout(500);
    }

    async updateProfile(data: { firstname?: string; lastname?: string; mail?: string; contact?: string }) {
        // Fill in the form fields if provided
        if (data.firstname !== undefined) {
            await this.page.fill('input[formControlName="firstname"]', data.firstname);
        }
        if (data.lastname !== undefined) {
            await this.page.fill('input[formControlName="lastname"]', data.lastname);
        }
        if (data.mail !== undefined) {
            await this.page.fill('input[formControlName="mail"]', data.mail);
        }
        if (data.contact !== undefined) {
            await this.page.fill('input[formControlName="contact"]', data.contact);
        }

        // Click save button
        await this.page.click('button:has-text("Speichern")');
        // Wait for save to complete
        await this.page.waitForTimeout(1000);
    }

    async verifyProfileData(data: { firstname?: string; lastname?: string; mail?: string; contact?: string }) {
        // Open the modal again to verify
        await this.openEditProfileModal();

        // Verify the values
        if (data.firstname !== undefined) {
            const value = await this.page.inputValue('input[formControlName="firstname"]');
            if (value !== data.firstname) {
                throw new Error(`Expected firstname to be "${data.firstname}" but got "${value}"`);
            }
        }
        if (data.lastname !== undefined) {
            const value = await this.page.inputValue('input[formControlName="lastname"]');
            if (value !== data.lastname) {
                throw new Error(`Expected lastname to be "${data.lastname}" but got "${value}"`);
            }
        }
        if (data.mail !== undefined) {
            const value = await this.page.inputValue('input[formControlName="mail"]');
            if (value !== data.mail) {
                throw new Error(`Expected mail to be "${data.mail}" but got "${value}"`);
            }
        }
        if (data.contact !== undefined) {
            const value = await this.page.inputValue('input[formControlName="contact"]');
            if (value !== data.contact) {
                throw new Error(`Expected contact to be "${data.contact}" but got "${value}"`);
            }
        }

        // Close the modal
        await this.page.click('button:has-text("Abbrechen")');
        await this.page.waitForTimeout(300);
    }

    async openChangePasswordModal() {
        // Click user menu button
        await this.page.click('#user-menu-button');
        // Wait for menu to open
        await this.page.waitForTimeout(300);
        // Click "Change Password" option
        await this.page.click('a:has-text("Passwort ändern")');
        // Wait for modal to appear
        await this.page.waitForTimeout(500);
    }

    async changePassword(currentPassword: string, newPassword: string) {
        // Fill in the password fields
        await this.page.fill('input[formControlName="currentPassword"]', currentPassword);
        await this.page.fill('input[formControlName="newPassword"]', newPassword);
        await this.page.fill('input[formControlName="confirmPassword"]', newPassword);

        // Click save button
        await this.page.click('button:has-text("Speichern")');
        // Wait for save to complete and modal to close
        await this.page.waitForTimeout(2000);
    }

    async verifyPasswordChangeSuccess() {
        // Check that success message appeared (it auto-closes after 1.5s)
        // We just verify the modal closed successfully
        const modalVisible = await this.page.isVisible('h3:has-text("Passwort ändern")');
        if (modalVisible) {
            throw new Error('Password change modal is still visible - change may have failed');
        }
    }

    async openDeleteAccountModal() {
        // Click user menu button
        await this.page.click('#user-menu-button');
        // Wait for menu to open
        await this.page.waitForTimeout(300);
        // Click "Account löschen" option
        await this.page.click('a:has-text("Account löschen")');
        // Wait for modal to appear
        await this.page.waitForTimeout(500);
    }

    async confirmAccountDeletion() {
        // Click the confirmation button in the delete account modal
        await this.page.click('button:has-text("Ja, Account löschen")');
        // Wait for deletion to complete and redirect
        await this.page.waitForTimeout(2000);
    }
}
