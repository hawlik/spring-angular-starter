import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let router: Router;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('passes through non-401 errors untouched', () => {
    const onError = vi.fn();
    http.get('/api/things').subscribe({ error: onError });

    httpTesting.expectOne('/api/things').flush(null, { status: 500, statusText: 'Server Error' });

    expect(onError).toHaveBeenCalled();
    httpTesting.expectNone('/auth/refresh');
  });

  it('refreshes and retries the original request on 401', () => {
    const onNext = vi.fn();
    http.get('/api/things').subscribe({ next: onNext });

    httpTesting.expectOne('/api/things').flush(null, { status: 401, statusText: 'Unauthorized' });
    httpTesting.expectOne('/auth/refresh').flush(null);

    const retried = httpTesting.expectOne('/api/things');
    retried.flush({ ok: true });

    expect(onNext).toHaveBeenCalledWith({ ok: true });
  });

  it('clears auth and redirects to /login when the refresh itself fails', () => {
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const clearAuth = vi.spyOn(auth, 'clearAuth');
    const onError = vi.fn();

    http.get('/api/things').subscribe({ error: onError });

    httpTesting.expectOne('/api/things').flush(null, { status: 401, statusText: 'Unauthorized' });
    httpTesting
      .expectOne('/auth/refresh')
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(clearAuth).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/login');
    expect(onError).toHaveBeenCalled();
  });

  it('issues a single refresh for concurrent 401s and replays both requests', () => {
    http.get('/api/one').subscribe();
    http.get('/api/two').subscribe();

    httpTesting.expectOne('/api/one').flush(null, { status: 401, statusText: 'Unauthorized' });
    httpTesting.expectOne('/api/two').flush(null, { status: 401, statusText: 'Unauthorized' });

    // Both failures share one in-flight refresh rather than each triggering their own.
    httpTesting.expectOne('/auth/refresh').flush(null);

    httpTesting.expectOne('/api/one').flush({});
    httpTesting.expectOne('/api/two').flush({});
  });

  it('does not attempt to refresh when the login request itself 401s', () => {
    const onError = vi.fn();
    http.post('/auth/login', {}).subscribe({ error: onError });

    httpTesting.expectOne('/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    httpTesting.expectNone('/auth/refresh');
    expect(onError).toHaveBeenCalled();
  });
});
