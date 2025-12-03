import { Component, EventEmitter, Input, OnInit, OnChanges, Output, inject, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { UserDto } from '../../../api/models/user-dto';
import { UserService } from '../../../services/user.service';
import { UpdateProfileRequest } from '../../../api/models/update-profile-request';

@Component({
    selector: 'app-edit-profile-modal',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, TranslateModule],
    templateUrl: './edit-profile-modal.html',
    styleUrl: './edit-profile-modal.css'
})
export class EditProfileModalComponent implements OnInit, OnChanges {
    @Input() isOpen = false;
    @Input() user: UserDto | null = null;
    @Output() close = new EventEmitter<void>();
    @Output() save = new EventEmitter<void>();

    profileForm: FormGroup;
    userService = inject(UserService);
    fb = inject(FormBuilder);
    translate = inject(TranslateService);

    loading = false;

    constructor() {
        this.profileForm = this.fb.group({
            firstname: ['', Validators.required],
            lastname: ['', Validators.required],
            mail: ['', [Validators.required, Validators.email]],
            contact: ['']
        });
    }

    ngOnInit(): void {
        this.updateForm();
    }

    ngOnChanges(): void {
        if (this.isOpen && this.user) {
            this.updateForm();
        }
    }

    private updateForm(): void {
        if (this.user) {
            this.profileForm.patchValue({
                firstname: this.user.firstname,
                lastname: this.user.lastname,
                mail: this.user.mail,
                contact: this.user.contact
            });
        }
    }

    onClose() {
        this.close.emit();
    }

    onSubmit() {
        if (this.profileForm.valid) {
            this.loading = true;
            const request: UpdateProfileRequest = this.profileForm.value;
            this.userService.updateProfile(request).subscribe({
                next: () => {
                    this.save.emit();
                    this.close.emit();
                    this.loading = false;
                },
                error: (err) => {
                    this.loading = false;
                }
            });
        }
    }
}
