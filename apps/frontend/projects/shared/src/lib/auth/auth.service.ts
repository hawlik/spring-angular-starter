import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, switchMap } from 'rxjs';

import {
  AuthUser,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  MessageResponse,
  ResetPasswordRequest,
  VerifyResetTokenRequest,
  VerifyResetTokenResponse,
} from './auth.models';

const AUTH_FLAG_KEY = 'authenticated';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly user = signal<AuthUser | null>(null);
  readonly initialized = signal(false);

  readonly isAuthenticated = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.roles.includes('ADMIN') ?? false);
  readonly userDisplayName = computed(() => {
    const u = this.user();
    if (!u) return '';
    return `${u.firstName} ${u.lastName}`.trim() || u.email;
  });

  init(): Observable<void> {
    if (!localStorage.getItem(AUTH_FLAG_KEY)) {
      this.initialized.set(true);
      return of(undefined);
    }

    return this.http.get<AuthUser>('/auth/me').pipe(
      map(user => {
        this.user.set(user);
      }),
      catchError(() => {
        localStorage.removeItem(AUTH_FLAG_KEY);
        return of(undefined);
      }),
      map(() => {
        this.initialized.set(true);
      }),
    );
  }

  login(req: LoginRequest): Observable<void> {
    return this.http.post<void>('/auth/login', req).pipe(
      switchMap(() => this.http.get<AuthUser>('/auth/me')),
      map(user => {
        this.user.set(user);
        localStorage.setItem(AUTH_FLAG_KEY, 'true');
      }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>('/auth/logout', {}).pipe(
      map(() => {
        this.user.set(null);
        localStorage.removeItem(AUTH_FLAG_KEY);
      }),
    );
  }

  forgotPassword(req: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/auth/forgot-password', req);
  }

  verifyResetToken(req: VerifyResetTokenRequest): Observable<VerifyResetTokenResponse> {
    return this.http.post<VerifyResetTokenResponse>('/auth/verify-reset-token', req);
  }

  resetPassword(req: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/auth/reset-password', req);
  }

  changePassword(req: ChangePasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/auth/change-password', req);
  }

  refresh(): Observable<void> {
    return this.http.post<void>('/auth/refresh', {});
  }

  clearAuth(): void {
    this.user.set(null);
    localStorage.removeItem(AUTH_FLAG_KEY);
  }
}
