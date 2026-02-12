import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    tap({
      error: err => {
        if (err.status === 401 && !req.url.includes('/auth/')) {
          authService.clearAuth();
          router.navigateByUrl('/login');
        }
      },
    }),
  );
};
