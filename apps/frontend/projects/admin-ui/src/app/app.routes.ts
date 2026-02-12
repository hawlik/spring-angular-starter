import { Routes } from '@angular/router';

import {
  authGuard,
  guestGuard,
  roleGuard,
  LoginComponent,
  ForgotPasswordComponent,
  ResetPasswordComponent,
  UnauthorizedComponent,
} from '@app/shared';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent, canActivate: [guestGuard] },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'unauthorized', component: UnauthorizedComponent, canActivate: [authGuard] },
  {
    path: '',
    canActivate: [authGuard, roleGuard('ADMIN')],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
