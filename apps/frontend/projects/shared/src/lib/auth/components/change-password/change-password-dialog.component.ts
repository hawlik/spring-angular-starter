import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { DialogRef } from '@angular/cdk/dialog';

import { AuthService } from '../../auth.service';

function passwordComplexityValidator(control: AbstractControl): ValidationErrors | null {
  const value: string = control.value ?? '';
  if (!value) return null;
  const hasUpper = /[A-Z]/.test(value);
  const hasLower = /[a-z]/.test(value);
  const hasDigit = /\d/.test(value);
  if (!hasUpper || !hasLower || !hasDigit) {
    return { complexity: true };
  }
  return null;
}

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const newPassword = control.get('newPassword');
  const confirm = control.get('confirmPassword');
  if (newPassword && confirm && newPassword.value !== confirm.value) {
    return { passwordMismatch: true };
  }
  return null;
}

@Component({
  selector: 'lib-change-password-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './change-password-dialog.component.html',
})
export class ChangePasswordDialogComponent {
  private readonly dialogRef = inject<DialogRef<void>>(DialogRef);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  protected readonly submitting = signal(false);
  protected readonly serverError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8), passwordComplexityValidator]],
      confirmPassword: ['', Validators.required],
    },
    { validators: [passwordMatchValidator] },
  );

  protected isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && (ctrl.dirty || ctrl.touched));
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const { currentPassword, newPassword } = this.form.getRawValue();
    this.submitting.set(true);
    this.serverError.set(null);

    this.authService.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => this.dialogRef.close(),
      error: err => {
        this.submitting.set(false);
        if (err.status === 400) {
          this.serverError.set('Current password is incorrect.');
        } else {
          this.serverError.set('An unexpected error occurred. Please try again.');
        }
      },
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
