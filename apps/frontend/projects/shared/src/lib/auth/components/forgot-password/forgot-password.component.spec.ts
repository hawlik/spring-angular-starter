import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ForgotPasswordComponent } from './forgot-password.component';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
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

  it('should validate email format', () => {
    component.form.controls.email.setValue('bad');
    expect(component.form.controls.email.hasError('email')).toBe(true);

    component.form.controls.email.setValue('test@example.com');
    expect(component.form.invalid).toBe(false);
  });

  it('should not submit when form is invalid', () => {
    component.onSubmit();
    httpTesting.expectNone('/auth/forgot-password');
  });

  it('should show success message after submit', () => {
    component.form.controls.email.setValue('test@example.com');
    component.onSubmit();

    expect(component.submitting()).toBe(true);

    const req = httpTesting.expectOne('/auth/forgot-password');
    expect(req.request.body).toEqual({ email: 'test@example.com' });
    req.flush({ message: 'ok' });

    expect(component.submitted()).toBe(true);
  });

  it('should show success even on error to prevent email enumeration', () => {
    component.form.controls.email.setValue('test@example.com');
    component.onSubmit();

    const req = httpTesting.expectOne('/auth/forgot-password');
    req.flush(null, { status: 500, statusText: 'Error' });

    expect(component.submitted()).toBe(true);
  });
});
