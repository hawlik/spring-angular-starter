import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { LoginComponent } from './login.component';
import { AuthUser } from '../../auth.models';

const mockUser: AuthUser = {
  id: 1,
  email: 'test@example.com',
  firstName: 'John',
  lastName: 'Doe',
  avatarUrl: null,
  roles: ['USER'],
};

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have an invalid form initially', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('should require email and password', () => {
    const { username, password } = component.form.controls;
    expect(username.hasError('required')).toBe(true);
    expect(password.hasError('required')).toBe(true);
  });

  it('should validate email format', () => {
    component.form.controls.username.setValue('not-email');
    expect(component.form.controls.username.hasError('email')).toBe(true);

    component.form.controls.username.setValue('test@example.com');
    expect(component.form.controls.username.hasError('email')).toBe(false);
  });

  it('should not submit when form is invalid', () => {
    component.onSubmit();
    httpTesting.expectNone('/auth/login');
  });

  it('should submit and navigate on success', () => {
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    component.form.setValue({ username: 'test@example.com', password: 'password', rememberMe: false });
    component.onSubmit();

    expect(component.submitting()).toBe(true);

    const loginReq = httpTesting.expectOne('/auth/login');
    loginReq.flush(null);

    const meReq = httpTesting.expectOne('/auth/me');
    meReq.flush(mockUser);

    expect(navigateSpy).toHaveBeenCalledWith('/');
  });

  it('should show error on 401', () => {
    component.form.setValue({ username: 'test@example.com', password: 'wrong', rememberMe: false });
    component.onSubmit();

    const loginReq = httpTesting.expectOne('/auth/login');
    loginReq.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(component.submitting()).toBe(false);
    expect(component.errorMessage()).toBe('Invalid email or password.');
  });

  it('should show generic error on server error', () => {
    component.form.setValue({ username: 'test@example.com', password: 'password', rememberMe: false });
    component.onSubmit();

    const loginReq = httpTesting.expectOne('/auth/login');
    loginReq.flush(null, { status: 500, statusText: 'Server Error' });

    expect(component.submitting()).toBe(false);
    expect(component.errorMessage()).toBe('An unexpected error occurred. Please try again.');
  });

  it('should default rememberMe to false and send it on submit', () => {
    expect(component.form.controls.rememberMe.value).toBe(false);

    component.form.setValue({ username: 'test@example.com', password: 'password', rememberMe: true });
    component.onSubmit();

    const loginReq = httpTesting.expectOne('/auth/login');
    expect(loginReq.request.body).toEqual({
      username: 'test@example.com',
      password: 'password',
      rememberMe: true,
    });
    loginReq.flush(null);
    httpTesting.expectOne('/auth/me').flush(mockUser);
  });
});
