import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const noAuthGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const authService = inject(AuthService);

    if (authService.isAuthenticated()) {
        router.navigate(['/dashboard']);
        return false;
    } else {
        return true;
    }
};
