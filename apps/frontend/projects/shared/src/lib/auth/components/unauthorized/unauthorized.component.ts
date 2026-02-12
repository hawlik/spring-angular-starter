import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../auth.service';

@Component({
  selector: 'lib-unauthorized',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen flex items-center justify-center px-4">
      <div class="w-full max-w-md">
        <div class="bg-white rounded-lg shadow-md p-8 text-center">
          <h1 class="text-2xl font-bold mb-4 text-danger-600">Access Denied</h1>
          <p class="text-gray-600 mb-6">
            You do not have permission to access this page.
          </p>
          <button
            type="button"
            (click)="logout()"
            class="rounded-md bg-primary-600 px-6 py-2 text-white font-medium hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
          >
            Sign Out
          </button>
        </div>
      </div>
    </div>
  `,
})
export class UnauthorizedComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigateByUrl('/login');
    });
  }
}
