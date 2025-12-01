import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslateModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  registerForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
    mail: ['', [Validators.required, Validators.email]],
    firstname: ['', Validators.required],
    lastname: ['', Validators.required],
    contact: [''],
    validationCode: ['', Validators.required]
  });

  error = '';

  onSubmit() {

    if (this.registerForm.valid) {
      this.error = '';
      const val = this.registerForm.value;


      // Ensure all required fields are present and not null/undefined
      if (val.username && val.password && val.mail && val.firstname && val.lastname && val.validationCode) {
        this.authService.register({
          username: val.username,
          password: val.password,
          mail: val.mail,
          firstname: val.firstname,
          lastname: val.lastname,
          contact: val.contact || '',
          validationCode: val.validationCode
        }).subscribe({
          next: (res) => {

            this.router.navigate(['/']).then(success => {

            });
          },
          error: (err) => {
            this.error = 'AUTH.REGISTER.ERROR';
            console.error('Register error', err);
          }
        });
      }
    } else {

    }
  }
}
