import { Component, inject, ViewChild, ElementRef, HostListener, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { TranslateModule } from '@ngx-translate/core';
import { EditProfileModalComponent } from '../../modals/edit-profile-modal/edit-profile-modal';
import { EditPasswordModalComponent } from '../../modals/edit-password-modal/edit-password-modal';
import { DeleteAccountModalComponent } from '../../modals/delete-account-modal/delete-account-modal';
import { UserService } from '../../../services/user.service';
import { UserDto } from '../../../api/models/user-dto';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, EditProfileModalComponent, EditPasswordModalComponent, DeleteAccountModalComponent],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css'
})
export class MainLayoutComponent implements OnInit {
  authService = inject(AuthService);
  userService = inject(UserService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  mobileMenuOpen = false;

  userMenuOpen = false;
  editProfileModalOpen = false;
  editPasswordModalOpen = false;
  deleteAccountModalOpen = false;
  currentUser: UserDto | null = null;

  @ViewChild('userMenuContainer') userMenuContainer!: ElementRef;

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (this.userMenuOpen && this.userMenuContainer && !this.userMenuContainer.nativeElement.contains(event.target)) {
      this.closeUserMenu();
    }
  }

  ngOnInit() {
    this.loadUserProfile();
  }

  loadUserProfile() {
    this.userService.getProfile().subscribe({
      next: (user) => {
        this.currentUser = user;
      },
      error: (err) => {
        console.error('[MainLayout] Failed to load user profile', err);
      }
    });
  }

  toggleUserMenu() {
    this.userMenuOpen = !this.userMenuOpen;
  }

  closeUserMenu() {
    this.userMenuOpen = false;
  }

  editProfile() {
    this.closeUserMenu();
    // Use setTimeout to ensure the modal opens after the menu closes and any click events have propagated
    setTimeout(() => {
      this.editProfileModalOpen = true;
      this.cdr.detectChanges();
    }, 100);
  }

  closeEditProfileModal() {
    this.editProfileModalOpen = false;
  }

  openEditPasswordModal() {
    this.userMenuOpen = false;
    this.editPasswordModalOpen = true;
  }

  closeEditPasswordModal() {
    this.editPasswordModalOpen = false;
  }

  onProfileSaved() {
    this.loadUserProfile();
  }

  editPassword() {
    this.closeUserMenu();
    this.openEditPasswordModal();
  }

  deleteAccount() {
    this.closeUserMenu();
    setTimeout(() => {
      this.deleteAccountModalOpen = true;
      this.cdr.detectChanges();
    }, 100);
  }

  closeDeleteAccountModal() {
    this.deleteAccountModalOpen = false;
  }

  onAccountDeleted() {
    this.closeDeleteAccountModal();
    // Logout user after account deletion
    this.authService.logout();
    this.router.navigate(['/welcome']);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/welcome']);
    this.closeUserMenu();
  }
}
