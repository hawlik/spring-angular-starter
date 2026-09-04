import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../auth.service';

@Component({
  selector: 'lib-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center px-4">
      <div class="w-full max-w-md">
        <div class="bg-white rounded-lg shadow-md p-8">
          <h1 class="text-2xl font-bold text-center mb-6">Sign In</h1>

          @if (errorMessage()) {
            <div
              role="alert"
              class="mb-4 rounded-md bg-danger-50 p-4 text-sm text-danger-700"
            >
              {{ errorMessage() }}
            </div>
          }

          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="mb-4">
              <label for="username" class="block text-sm font-medium text-gray-700 mb-1">
                Email
              </label>
              <input
                id="username"
                type="email"
                formControlName="username"
                autocomplete="username"
                class="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                [attr.aria-invalid]="form.controls.username.invalid && form.controls.username.touched"
              />
            </div>

            <div class="mb-4">
              <label for="password" class="block text-sm font-medium text-gray-700 mb-1">
                Password
              </label>
              <input
                id="password"
                type="password"
                formControlName="password"
                autocomplete="current-password"
                class="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                [attr.aria-invalid]="form.controls.password.invalid && form.controls.password.touched"
              />
            </div>

            <div class="mb-6 flex items-center gap-2">
              <input
                id="rememberMe"
                type="checkbox"
                formControlName="rememberMe"
                class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
              />
              <label for="rememberMe" class="text-sm text-gray-700">Keep me logged in</label>
            </div>

            <button
              type="submit"
              [disabled]="form.invalid || submitting()"
              class="w-full rounded-md bg-primary-600 px-4 py-2 text-white font-medium hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ submitting() ? 'Signing in...' : 'Sign In' }}
            </button>
          </form>

          <p class="mt-4 text-center text-sm text-gray-600">
            <a routerLink="/forgot-password" class="text-primary-600 hover:text-primary-500">
              Forgot your password?
            </a>
          </p>
        </div>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    rememberMe: [false],
  });

  onSubmit(): void {
    if (this.form.invalid) return;

    this.submitting.set(true);
    this.errorMessage.set('');

    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.router.navigateByUrl('/');
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(
          err.status === 401
            ? 'Invalid email or password.'
            : 'An unexpected error occurred. Please try again.',
        );
      },
    });
  }
}
