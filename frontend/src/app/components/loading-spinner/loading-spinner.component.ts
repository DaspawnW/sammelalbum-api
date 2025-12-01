import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoadingService } from '../../services/loading.service';
import { Subscription } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    <div *ngIf="isLoading" class="loading-overlay">
      <div class="spinner-container">
        <div class="spinner"></div>
        <p class="loading-text">{{ 'LOADING.TEXT' | translate }}</p>
      </div>
    </div>
  `,
  styles: [`
    .loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 9999;
      pointer-events: all;
    }

    .spinner-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }

    .spinner {
      width: 50px;
      height: 50px;
      border: 5px solid rgba(255, 255, 255, 0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    .loading-text {
      color: white;
      font-size: 1rem;
      font-weight: 500;
      margin: 0;
    }
  `]
})
export class LoadingSpinnerComponent implements OnInit, OnDestroy {
  isLoading = false;
  private subscription?: Subscription;

  constructor(
    private loadingService: LoadingService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    try {
      this.subscription = this.loadingService.loading$.subscribe({
        next: (loading) => {
          this.ngZone.run(() => {
            // Wrap in setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
            // and allow other components (like Login) to update their view first.
            setTimeout(() => {
              this.isLoading = loading;
              this.cdr.detectChanges(); // Force view update after the tick
            });
          });
        },
        error: (error) => {
          console.error('Error in loading subscription:', error);
          this.ngZone.run(() => {
            setTimeout(() => {
              this.isLoading = false;
              this.cdr.detectChanges();
            });
          });
        }
      });
    } catch (error) {
      console.error('Error initializing LoadingSpinnerComponent:', error);
      this.isLoading = false;
    }
  }

  ngOnDestroy(): void {
    try {
      if (this.subscription) {
        this.subscription.unsubscribe();
      }
    } catch (error) {
      console.error('Error destroying LoadingSpinnerComponent:', error);
    }
  }
}
