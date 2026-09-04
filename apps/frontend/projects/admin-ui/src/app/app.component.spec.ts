import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService, AuthUser } from '@app/shared';

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
    expect(header.textContent).toContain('Admin Panel');
    expect(header.textContent).toContain('Jane Doe');
  });

  it('logs out and navigates to /login when Sign Out is clicked', async () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    auth.user.set(testUser);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('header button').click();
    httpMock.expectOne('/auth/logout').flush(null);
    await fixture.whenStable();

    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/login');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('header')).toBeNull();
  });
});
