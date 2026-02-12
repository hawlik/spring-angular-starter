import { Routes } from '@angular/router';

import {
  authGuard,
  guestGuard,
  LoginComponent,
  ForgotPasswordComponent,
  ResetPasswordComponent,
} from '@app/shared';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent, canActivate: [guestGuard] },
  { path: 'reset-password', component: ResetPasswordComponent },
  {
    path: '',
    canActivate: [authGuard],
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
