import { Component, inject } from '@angular/core';
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

  loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  error = '';

  onSubmit() {
    console.log('LoginComponent.onSubmit called');
    if (this.loginForm.valid) {
      this.error = '';
      const { username, password } = this.loginForm.value;
      console.log('Form is valid, attempting login for:', username);

      if (username && password) {
        this.authService.login({ username, password }).subscribe({
          next: (res) => {
            console.log('Login success, navigating to /', res);
            this.router.navigate(['/']).then(success => {
              console.log('Navigation result:', success);
            });
          },
          error: (err) => {
            this.error = 'AUTH.LOGIN.ERROR';
            console.error('Login error', err);
          }
        });
      }
    } else {
      console.log('Form is invalid', this.loginForm.errors);
    }
  }
}
