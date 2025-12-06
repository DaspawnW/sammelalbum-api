import { Component, inject, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Api } from '../../api/api';
import { resetPassword } from '../../api/fn/authentication/reset-password';

// Custom validator for password match
function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (!password || !confirmPassword) {
        return null;
    }

    return password.value === confirmPassword.value ? null : { passwordMismatch: true };
}

@Component({
    selector: 'app-reset-password',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslateModule],
    templateUrl: './reset-password.html',
    styleUrl: './reset-password.css'
})
export class ResetPasswordComponent implements OnInit {
    private fb = inject(FormBuilder);
    private api = inject(Api);
    private router = inject(Router);
    private route = inject(ActivatedRoute);
    private cdr = inject(ChangeDetectorRef);

    token: string | null = null;

    resetPasswordForm = this.fb.group({
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });

    error = '';
    success = false;
    isSubmitting = false;
    invalidToken = false;

    ngOnInit() {
        // Extract token from query parameters
        this.route.queryParams.subscribe(params => {
            this.token = params['token'];
            if (!this.token) {
                this.invalidToken = true;
                this.error = 'AUTH.RESET_PASSWORD.INVALID_TOKEN';
            }
        });
    }

    get passwordMismatch(): boolean {
        return this.resetPasswordForm.hasError('passwordMismatch') &&
            (this.resetPasswordForm.get('confirmPassword')?.touched || false);
    }

    get passwordTooShort(): boolean {
        const passwordControl = this.resetPasswordForm.get('password');
        return (passwordControl?.hasError('minlength') && (passwordControl?.touched || false)) || false;
    }

    onSubmit() {
        if (this.resetPasswordForm.valid && !this.isSubmitting && this.token) {
            this.error = '';
            this.success = false;
            this.isSubmitting = true;
            const { password } = this.resetPasswordForm.value;

            if (password) {
                this.api.invoke(resetPassword, {
                    body: {
                        token: this.token,
                        newPassword: password
                    }
                })
                    .then(() => {
                        this.success = true;
                        this.isSubmitting = false;
                        this.resetPasswordForm.reset();
                        this.cdr.detectChanges();

                        // Redirect to login after 3 seconds
                        setTimeout(() => {
                            this.router.navigate(['/login']);
                        }, 3000);
                    })
                    .catch((err: any) => {
                        this.error = 'AUTH.RESET_PASSWORD.ERROR';
                        this.isSubmitting = false;
                        console.error('Reset password error', err);
                        this.cdr.detectChanges();
                    });
            }
        }
    }
}
