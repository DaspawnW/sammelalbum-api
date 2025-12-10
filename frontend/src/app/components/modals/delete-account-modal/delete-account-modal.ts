import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { deleteMe } from '../../../api/fn/user/delete-me';
import { ApiConfiguration } from '../../../api/api-configuration';

@Component({
  selector: 'app-delete-account-modal',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './delete-account-modal.html'
})
export class DeleteAccountModalComponent {
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();
  @Output() confirm = new EventEmitter<void>();

  private http = inject(HttpClient);
  private apiConfig = inject(ApiConfiguration);
  isDeleting = false;

  onClose() {
    if (!this.isDeleting) {
      this.close.emit();
    }
  }

  onConfirm() {
    this.isDeleting = true;

    deleteMe(this.http, this.apiConfig.rootUrl).subscribe({
      next: () => {
        this.isDeleting = false;
        this.confirm.emit();
      },
      error: (err: any) => {
        console.error('[DeleteAccountModal] Failed to delete account', err);
        this.isDeleting = false;
        // TODO: Show error message to user
        alert('Fehler beim Löschen des Accounts. Bitte versuchen Sie es später erneut.');
      }
    });
  }
}
