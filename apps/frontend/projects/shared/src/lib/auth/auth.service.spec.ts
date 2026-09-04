import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthUser } from './auth.models';
import { AuthService } from './auth.service';

const testUser: AuthUser = {
  id: 1,
  email: 'jane@example.com',
  firstName: 'Jane',
  lastName: 'Doe',
  avatarUrl: null,
  roles: ['USER'],
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('starts signed out', () => {
    expect(service.user()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.isAdmin()).toBe(false);
    expect(service.userDisplayName()).toBe('');
  });

  it('derives the display name from the user name, falling back to the email', () => {
    service.user.set(testUser);
    expect(service.userDisplayName()).toBe('Jane Doe');

    service.user.set({ ...testUser, firstName: '', lastName: '' });
    expect(service.userDisplayName()).toBe('jane@example.com');
  });

  it('flags admins by role', () => {
    service.user.set({ ...testUser, roles: ['USER', 'ADMIN'] });

    expect(service.isAdmin()).toBe(true);
  });

  it('login stores the user and the auth flag', () => {
    service.login({ username: 'jane@example.com', password: 'secret', rememberMe: false }).subscribe();

    httpMock.expectOne('/auth/login').flush(null);
    httpMock.expectOne('/auth/me').flush(testUser);

    expect(service.user()).toEqual(testUser);
    expect(localStorage.getItem('authenticated')).toBe('true');
  });

  it('logout clears the user and the auth flag', () => {
    service.user.set(testUser);
    localStorage.setItem('authenticated', 'true');

    service.logout().subscribe();
    httpMock.expectOne('/auth/logout').flush(null);

    expect(service.user()).toBeNull();
    expect(localStorage.getItem('authenticated')).toBeNull();
  });

  it('init skips the /auth/me probe when no auth flag is present', () => {
    service.init().subscribe();

    httpMock.expectNone('/auth/me');
    expect(service.initialized()).toBe(true);
  });

  it('init restores the session when the auth flag is present', () => {
    localStorage.setItem('authenticated', 'true');

    service.init().subscribe();
    httpMock.expectOne('/auth/me').flush(testUser);

    expect(service.user()).toEqual(testUser);
    expect(service.initialized()).toBe(true);
  });

  it('init drops a stale auth flag when the probe fails', () => {
    localStorage.setItem('authenticated', 'true');

    service.init().subscribe();
    httpMock.expectOne('/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(service.user()).toBeNull();
    expect(localStorage.getItem('authenticated')).toBeNull();
    expect(service.initialized()).toBe(true);
  });

  it('changePassword posts the current and new password', () => {
    service.changePassword({ currentPassword: 'old', newPassword: 'NewPassw0rd' }).subscribe();

    const req = httpMock.expectOne('/auth/change-password');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ currentPassword: 'old', newPassword: 'NewPassw0rd' });
    req.flush({ message: 'Password changed successfully.' });
  });

  it('refresh posts to /auth/refresh', () => {
    service.refresh().subscribe();

    const req = httpMock.expectOne('/auth/refresh');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('login sends the rememberMe flag', () => {
    service.login({ username: 'jane@example.com', password: 'secret', rememberMe: true }).subscribe();

    const req = httpMock.expectOne('/auth/login');
    expect(req.request.body).toEqual({
      username: 'jane@example.com',
      password: 'secret',
      rememberMe: true,
    });
    req.flush(null);
    httpMock.expectOne('/auth/me').flush(testUser);
  });
});
