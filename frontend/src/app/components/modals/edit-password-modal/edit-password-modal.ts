import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { UserService } from '../../../services/user.service';

@Component({
    selector: 'app-edit-password-modal',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, TranslateModule],
    templateUrl: './edit-password-modal.html',
    styleUrl: './edit-password-modal.css'
})
export class EditPasswordModalComponent implements OnInit {
    @Input() isOpen = false;
    @Output() close = new EventEmitter<void>();

    passwordForm: FormGroup;
    isSubmitting = false;
    errorMessage = '';
    successMessage = '';

    constructor(
        private fb: FormBuilder,
        private userService: UserService,
        private cdr: ChangeDetectorRef
    ) {
        this.passwordForm = this.fb.group({
            currentPassword: ['', Validators.required],
            newPassword: ['', [Validators.required, Validators.minLength(6)]],
            confirmPassword: ['', Validators.required]
        }, { validators: this.passwordMatchValidator });
    }

    ngOnInit(): void { }

    // Custom validator to check if passwords match
    passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
        const newPassword = control.get('newPassword');
        const confirmPassword = control.get('confirmPassword');

        if (!newPassword || !confirmPassword) {
            return null;
        }

        return newPassword.value === confirmPassword.value ? null : { passwordMismatch: true };
    }

    onSubmit(): void {
        if (this.passwordForm.invalid || this.isSubmitting) {
            return;
        }

        this.isSubmitting = true;
        this.errorMessage = '';
        this.successMessage = '';

        const { currentPassword, newPassword } = this.passwordForm.value;

        this.userService.changePassword({ currentPassword, newPassword }).subscribe({
            next: () => {
                this.successMessage = 'PASSWORD.SUCCESS';
                this.isSubmitting = false;
                this.passwordForm.reset();

                // Close modal after short delay to show success message
                setTimeout(() => {
                    this.close.emit();
                    this.successMessage = '';
                }, 1500);
            },
            error: (err) => {
                console.error('Error changing password:', err);
                this.errorMessage = err.status === 400 ? 'PASSWORD.INVALID_CURRENT' : 'PASSWORD.ERROR';
                this.isSubmitting = false;

                // Force change detection to immediately show error
                this.cdr.detectChanges();
            }
        });
    }

    onCancel(): void {
        this.passwordForm.reset();
        this.errorMessage = '';
        this.successMessage = '';
        this.close.emit();
    }
}
