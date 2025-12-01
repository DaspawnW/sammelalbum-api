import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Router, NavigationStart } from '@angular/router';

@Injectable({
    providedIn: 'root'
})
export class LoadingService {
    private loadingSubject = new BehaviorSubject<boolean>(false);
    private activeRequests = new Set<string>();
    private timeoutId: any;
    private router = inject(Router);

    public loading$: Observable<boolean> = this.loadingSubject.asObservable();

    constructor() {
        // Automatically clear loading state on navigation
        this.router.events.subscribe(event => {
            if (event instanceof NavigationStart) {
                this.reset();
            }
        });
    }

    /**
     * Show the loading spinner for a specific request
     * @param requestId Unique identifier for the request
     */
    show(requestId: string): void {
        try {
            this.activeRequests.add(requestId);

            if (this.activeRequests.size > 0) {
                this.loadingSubject.next(true);

                // Clear existing timeout if any
                if (this.timeoutId) {
                    clearTimeout(this.timeoutId);
                }

                // Set safety timeout (10 seconds)
                this.timeoutId = setTimeout(() => {
                    this.reset();
                }, 10000);
            }
        } catch (error) {
            console.error('Error in LoadingService.show():', error);
            this.reset();
        }
    }

    /**
     * Hide the loading spinner for a specific request
     * @param requestId Unique identifier for the request
     */
    hide(requestId: string): void {
        try {
            if (this.activeRequests.has(requestId)) {
                this.activeRequests.delete(requestId);
            }

            if (this.activeRequests.size === 0) {
                this.loadingSubject.next(false);
                if (this.timeoutId) {
                    clearTimeout(this.timeoutId);
                    this.timeoutId = null;
                }
            }
        } catch (error) {
            console.error('Error in LoadingService.hide():', error);
            this.reset();
        }
    }

    /**
     * Force reset the loading state
     */
    reset(): void {
        try {
            this.activeRequests.clear();
            this.loadingSubject.next(false);
            if (this.timeoutId) {
                clearTimeout(this.timeoutId);
                this.timeoutId = null;
            }
        } catch (error) {
            console.error('Error in LoadingService.reset():', error);
            try {
                this.loadingSubject.next(false);
            } catch (e) {
                // Critical error, suppress to avoid loop
            }
        }
    }

    /**
     * Get current request count (for debugging)
     */
    getRequestCount(): number {
        return this.activeRequests.size;
    }
}
