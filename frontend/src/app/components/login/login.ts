import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslateModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  error = '';

  onSubmit() {
    if (this.loginForm.valid) {
      this.error = '';
      const { username, password } = this.loginForm.value;

      if (username && password) {
        this.authService.login({ username, password }).subscribe({
          next: (res) => {
            this.router.navigate(['/']);
          },
          error: (err) => {
            this.error = 'AUTH.LOGIN.ERROR';
            console.error('Login error', err);
            this.cdr.detectChanges(); // Force view update to show error
          }
        });
      }
    }
  }
}
