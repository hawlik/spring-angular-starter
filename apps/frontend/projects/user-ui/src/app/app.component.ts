import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';

import { AuthService, ChangePasswordDialogComponent } from '@app/shared';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen">
      @if (authService.isAuthenticated()) {
        <header class="bg-primary-600 text-white p-4">
          <div class="container mx-auto flex items-center justify-between">
            <h1 class="text-xl font-bold">App</h1>
            <div class="flex items-center gap-4">
              <span class="text-sm">{{ authService.userDisplayName() }}</span>
              <button
                type="button"
                (click)="openChangePasswordDialog()"
                class="rounded-md bg-primary-700 hover:bg-primary-800 px-3 py-1.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-primary-600"
              >
                Change Password
              </button>
              <button
                type="button"
                (click)="logout()"
                class="rounded-md bg-primary-700 px-3 py-1.5 text-sm font-medium hover:bg-primary-800 focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-primary-600"
              >
                Sign Out
              </button>
            </div>
          </div>
        </header>
      }
      <main>
        <router-outlet />
      </main>
    </div>
  `,
})
export class AppComponent {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly dialog = inject(Dialog);

  openChangePasswordDialog(): void {
    this.dialog.open(ChangePasswordDialogComponent, { panelClass: 'modal-panel' });
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigateByUrl('/login');
    });
  }
}
