import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, shareReplay, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

// Shared across interceptor invocations so that concurrent 401s trigger a single
// refresh call; every waiting request then replays on the same result.
let refreshInFlight$: Observable<void> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError(err => {
      const noRetryUrls = ['/auth/refresh', '/auth/login', '/auth/logout'];
      if (err.status !== 401 || noRetryUrls.some(url => req.url.includes(url))) {
        return throwError(() => err);
      }

      if (!refreshInFlight$) {
        refreshInFlight$ = authService.refresh().pipe(
          shareReplay(1),
          finalize(() => {
            refreshInFlight$ = null;
          }),
        );
      }

      return refreshInFlight$.pipe(
        switchMap(() => next(req)),
        catchError(() => {
          authService.clearAuth();
          router.navigateByUrl('/login');
          return throwError(() => err);
        }),
      );
    }),
  );
};
