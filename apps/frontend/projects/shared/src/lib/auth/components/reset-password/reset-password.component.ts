import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../auth.service';

type ResetState = 'verifying' | 'form' | 'success' | 'error';

@Component({
  selector: 'lib-reset-password',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center px-4">
      <div class="w-full max-w-md">
        <div class="bg-white rounded-lg shadow-md p-8">
          @switch (state()) {
            @case ('verifying') {
              <p class="text-center text-gray-600">Verifying your reset link...</p>
            }

            @case ('error') {
              <div class="text-center">
                <h1 class="text-2xl font-bold mb-4 text-danger-600">Invalid Link</h1>
                <p class="text-gray-600 mb-6">
                  This password reset link is invalid or has expired.
                </p>
                <a
                  routerLink="/forgot-password"
                  class="text-primary-600 hover:text-primary-500 font-medium"
                >
                  Request a new reset link
                </a>
              </div>
            }

            @case ('form') {
              <h1 class="text-2xl font-bold text-center mb-6">Reset Password</h1>

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
                  <label for="newPassword" class="block text-sm font-medium text-gray-700 mb-1">
                    New Password
                  </label>
                  <input
                    id="newPassword"
                    type="password"
                    formControlName="newPassword"
                    autocomplete="new-password"
                    class="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                    [attr.aria-invalid]="form.controls.newPassword.invalid && form.controls.newPassword.touched"
                  />
                </div>

                <div class="mb-6">
                  <label for="confirmPassword" class="block text-sm font-medium text-gray-700 mb-1">
                    Confirm Password
                  </label>
                  <input
                    id="confirmPassword"
                    type="password"
                    formControlName="confirmPassword"
                    autocomplete="new-password"
                    class="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                    [attr.aria-invalid]="form.controls.confirmPassword.invalid && form.controls.confirmPassword.touched"
                  />
                  @if (form.hasError('passwordMismatch') && form.controls.confirmPassword.touched) {
                    <p class="mt-1 text-sm text-danger-600">Passwords do not match.</p>
                  }
                </div>

                <button
                  type="submit"
                  [disabled]="form.invalid || submitting()"
                  class="w-full rounded-md bg-primary-600 px-4 py-2 text-white font-medium hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {{ submitting() ? 'Resetting...' : 'Reset Password' }}
                </button>
              </form>
            }

            @case ('success') {
              <div class="text-center">
                <h1 class="text-2xl font-bold mb-4 text-green-600">Password Reset</h1>
                <p class="text-gray-600 mb-6">
                  Your password has been successfully reset.
                </p>
                <a
                  routerLink="/login"
                  class="inline-block rounded-md bg-primary-600 px-6 py-2 text-white font-medium hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
                >
                  Sign In
                </a>
              </div>
            }
          }
        </div>
      </div>
    </div>
  `,
})
export class ResetPasswordComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly state = signal<ResetState>('verifying');
  readonly submitting = signal(false);
  readonly errorMessage = signal('');

  private exchangeToken = '';

  readonly form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: [passwordMatchValidator] },
  );

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.state.set('error');
      return;
    }

    this.authService.verifyResetToken({ token }).subscribe({
      next: res => {
        this.exchangeToken = res.token;
        this.state.set('form');
      },
      error: () => {
        this.state.set('error');
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.submitting.set(true);
    this.errorMessage.set('');

    this.authService
      .resetPassword({
        token: this.exchangeToken,
        newPassword: this.form.getRawValue().newPassword,
      })
      .subscribe({
        next: () => {
          this.state.set('success');
        },
        error: () => {
          this.submitting.set(false);
          this.errorMessage.set('Failed to reset password. Please try again.');
        },
      });
  }
}

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('newPassword');
  const confirm = control.get('confirmPassword');

  if (password && confirm && password.value !== confirm.value) {
    return { passwordMismatch: true };
  }

  return null;
}
