import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { ResetPasswordComponent } from './reset-password.component';

function createActivatedRoute(token: string | null) {
  return {
    snapshot: {
      queryParamMap: {
        get: (key: string) => (key === 'token' ? token : null),
      },
    },
  };
}

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let httpTesting: HttpTestingController;

  function setup(token: string | null) {
    TestBed.configureTestingModule({
      imports: [ResetPasswordComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: createActivatedRoute(token) },
      ],
    });

    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTesting.verify();
  });

  describe('without token', () => {
    beforeEach(() => setup(null));

    it('should show error state when no token in URL', () => {
      expect(component.state()).toBe('error');
    });
  });

  describe('with valid token', () => {
    beforeEach(() => setup('valid-token'));

    it('should verify token on init', () => {
      const req = httpTesting.expectOne('/auth/verify-reset-token');
      expect(req.request.body).toEqual({ token: 'valid-token' });
      req.flush({ token: 'exchange-token' });

      expect(component.state()).toBe('form');
    });

    it('should show error state on invalid token', () => {
      const req = httpTesting.expectOne('/auth/verify-reset-token');
      req.flush(null, { status: 400, statusText: 'Bad Request' });

      expect(component.state()).toBe('error');
    });

    it('should validate password match', () => {
      const req = httpTesting.expectOne('/auth/verify-reset-token');
      req.flush({ token: 'exchange-token' });

      component.form.controls.newPassword.setValue('password1');
      component.form.controls.confirmPassword.setValue('password2');
      component.form.controls.confirmPassword.markAsTouched();

      expect(component.form.hasError('passwordMismatch')).toBe(true);
    });

    it('should validate minimum password length', () => {
      const req = httpTesting.expectOne('/auth/verify-reset-token');
      req.flush({ token: 'exchange-token' });

      component.form.controls.newPassword.setValue('short');
      expect(component.form.controls.newPassword.hasError('minlength')).toBe(true);
    });

    it('should submit and show success', () => {
      const verifyReq = httpTesting.expectOne('/auth/verify-reset-token');
      verifyReq.flush({ token: 'exchange-token' });

      component.form.setValue({ newPassword: 'newpass12', confirmPassword: 'newpass12' });
      component.onSubmit();

      expect(component.submitting()).toBe(true);

      const resetReq = httpTesting.expectOne('/auth/reset-password');
      expect(resetReq.request.body).toEqual({ token: 'exchange-token', newPassword: 'newpass12' });
      resetReq.flush({ message: 'ok' });

      expect(component.state()).toBe('success');
    });

    it('should show error on submit failure', () => {
      const verifyReq = httpTesting.expectOne('/auth/verify-reset-token');
      verifyReq.flush({ token: 'exchange-token' });

      component.form.setValue({ newPassword: 'newpass12', confirmPassword: 'newpass12' });
      component.onSubmit();

      const resetReq = httpTesting.expectOne('/auth/reset-password');
      resetReq.flush(null, { status: 500, statusText: 'Error' });

      expect(component.submitting()).toBe(false);
      expect(component.errorMessage()).toBe('Failed to reset password. Please try again.');
    });
  });
});
