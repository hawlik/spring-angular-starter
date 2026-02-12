import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

export function roleGuard(role: string): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const user = authService.user();
    if (user?.roles.includes(role)) {
      return true;
    }

    return router.createUrlTree(['/unauthorized']);
  };
}
