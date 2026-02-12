import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../auth.service';

@Component({
  selector: 'lib-forgot-password',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center px-4">
      <div class="w-full max-w-md">
        <div class="bg-white rounded-lg shadow-md p-8">
          <h1 class="text-2xl font-bold text-center mb-2">Forgot Password</h1>
          <p class="text-gray-600 text-center text-sm mb-6">
            Enter your email address and we'll send you a link to reset your password.
          </p>

          @if (submitted()) {
            <div
              role="status"
              class="rounded-md bg-green-50 p-4 text-sm text-green-700"
            >
              If an account exists with that email, you will receive a password reset link shortly.
            </div>
          } @else {
            <form [formGroup]="form" (ngSubmit)="onSubmit()">
              <div class="mb-6">
                <label for="email" class="block text-sm font-medium text-gray-700 mb-1">
                  Email
                </label>
                <input
                  id="email"
                  type="email"
                  formControlName="email"
                  autocomplete="email"
                  class="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                  [attr.aria-invalid]="form.controls.email.invalid && form.controls.email.touched"
                />
              </div>

              <button
                type="submit"
                [disabled]="form.invalid || submitting()"
                class="w-full rounded-md bg-primary-600 px-4 py-2 text-white font-medium hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {{ submitting() ? 'Sending...' : 'Send Reset Link' }}
              </button>
            </form>
          }

          <p class="mt-4 text-center text-sm text-gray-600">
            <a routerLink="/login" class="text-primary-600 hover:text-primary-500">
              Back to Sign In
            </a>
          </p>
        </div>
      </div>
    </div>
  `,
})
export class ForgotPasswordComponent {
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly submitted = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  onSubmit(): void {
    if (this.form.invalid) return;

    this.submitting.set(true);

    this.authService.forgotPassword(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitted.set(true);
      },
      error: () => {
        // Always show success to prevent email enumeration
        this.submitted.set(true);
      },
    });
  }
}
