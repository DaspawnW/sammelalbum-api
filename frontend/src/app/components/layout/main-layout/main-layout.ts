import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css'
})
export class MainLayoutComponent {
  authService = inject(AuthService);
  private router = inject(Router);
  mobileMenuOpen = false;

  logout() {
    this.authService.logout();
    this.router.navigate(['/welcome']);
  }
}
