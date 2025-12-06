import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Api } from '../../api/api';
import { forgotPassword } from '../../api/fn/authentication/forgot-password';

@Component({
    selector: 'app-forgot-password',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslateModule],
    templateUrl: './forgot-password.html',
    styleUrl: './forgot-password.css'
})
export class ForgotPasswordComponent {
    private fb = inject(FormBuilder);
    private api = inject(Api);
    private router = inject(Router);
    private cdr = inject(ChangeDetectorRef);

    forgotPasswordForm = this.fb.group({
        identifier: ['', Validators.required]
    });

    error = '';
    success = false;
    isSubmitting = false;

    onSubmit() {
        if (this.forgotPasswordForm.valid && !this.isSubmitting) {
            this.error = '';
            this.success = false;
            this.isSubmitting = true;
            const { identifier } = this.forgotPasswordForm.value;

            if (identifier) {
                this.api.invoke(forgotPassword, { body: { identifier } })
                    .then(() => {
                        this.success = true;
                        this.isSubmitting = false;
                        this.forgotPasswordForm.reset();
                        this.cdr.detectChanges();

                        // Redirect to login after 3 seconds
                        setTimeout(() => {
                            this.router.navigate(['/login']);
                        }, 3000);
                    })
                    .catch((err: any) => {
                        this.error = 'AUTH.FORGOT_PASSWORD.ERROR';
                        this.isSubmitting = false;
                        console.error('Forgot password error', err);
                        this.cdr.detectChanges();
                    });
            }
        }
    }
}
