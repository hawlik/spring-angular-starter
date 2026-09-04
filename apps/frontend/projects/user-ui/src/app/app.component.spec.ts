import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Dialog } from '@angular/cdk/dialog';
import { of } from 'rxjs';

import { AuthService, AuthUser, ChangePasswordDialogComponent } from '@app/shared';

import { AppComponent } from './app.component';

const testUser: AuthUser = {
  id: 1,
  email: 'jane@example.com',
  firstName: 'Jane',
  lastName: 'Doe',
  avatarUrl: null,
  roles: ['USER'],
};

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let auth: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    auth = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('creates the component', () => {
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('hides the header while signed out', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('header')).toBeNull();
  });

  it('shows the header and display name once signed in', () => {
    auth.user.set(testUser);
    fixture.detectChanges();

    const header: HTMLElement = fixture.nativeElement.querySelector('header');
    expect(header).not.toBeNull();
    expect(header.textContent).toContain('App');
    expect(header.textContent).toContain('Jane Doe');
  });

  it('logs out and navigates to /login when Sign Out is clicked', async () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    auth.user.set(testUser);
    fixture.detectChanges();

    const signOut = Array.from<HTMLButtonElement>(
      fixture.nativeElement.querySelectorAll('header button'),
    ).find(b => b.textContent?.includes('Sign Out'));
    signOut!.click();
    httpMock.expectOne('/auth/logout').flush(null);
    await fixture.whenStable();

    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/login');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('header')).toBeNull();
  });

  it('opens the change-password dialog from the header', () => {
    const dialog = TestBed.inject(Dialog);
    const open = vi.spyOn(dialog, 'open').mockReturnValue({ closed: of(undefined) } as never);
    auth.user.set(testUser);
    fixture.detectChanges();

    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('header button'),
    );
    const changePassword = buttons.find(b => b.textContent?.includes('Change Password'));
    expect(changePassword).toBeDefined();

    changePassword!.click();

    expect(open).toHaveBeenCalledWith(ChangePasswordDialogComponent, { panelClass: 'modal-panel' });
  });

  it('does not show the change-password button while signed out', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('header')).toBeNull();
  });
});
