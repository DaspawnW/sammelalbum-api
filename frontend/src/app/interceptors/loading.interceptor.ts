import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LoadingService } from '../services/loading.service';
import { finalize } from 'rxjs/operators';

/**
 * HTTP Interceptor that automatically shows/hides the loading spinner
 * for all HTTP requests. Uses finalize operator to ensure spinner is ALWAYS
 * hidden regardless of success or error.
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
    const loadingService = inject(LoadingService);
    // Generate a unique ID for this request
    const requestId = `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

    // Show loading spinner
    try {
        loadingService.show(requestId);
    } catch (error) {
        console.error('Error showing loading spinner:', error);
    }

    return next(req).pipe(
        finalize(() => {
            try {
                loadingService.hide(requestId);
            } catch (error) {
                console.error('Error hiding loading spinner:', error);
                try {
                    loadingService.reset();
                } catch (e) {
                    // Suppress critical error
                }
            }
        })
    );
};
