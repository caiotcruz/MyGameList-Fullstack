import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { authInterceptor } from './auth.interceptor';

describe('Authentication request boundary', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  const navigate = vi.fn().mockResolvedValue(true);
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('token', 'private-token');
    navigate.mockClear();
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(),
      { provide: Router, useValue: { navigate } }
    ] });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { requests.verify(); localStorage.clear(); });

  it('authenticates protected API calls', () => {
    const url = environment.apiUrl + '/users/1';
    http.get(url).subscribe();
    const request = requests.expectOne(url);
    expect(request.request.headers.get('Authorization')).toBe('Bearer private-token');
    request.flush({});
  });
  it('never attaches the session token to another origin', () => {
    http.get('https://external.example/image').subscribe();
    const request = requests.expectOne('https://external.example/image');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
  it('does not attach stale credentials to a public login call', () => {
    const url = environment.apiUrl + '/auth/login';
    http.post(url, {}).subscribe();
    const request = requests.expectOne(url);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
  it('clears only authentication state after a protected 401', () => {
    localStorage.setItem('userName', 'Player');
    localStorage.setItem('userId', '1');
    localStorage.setItem('theme', 'dark');
    const url = environment.apiUrl + '/my-games';
    http.get(url).subscribe({ error: () => {} });
    requests.expectOne(url).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('userName')).toBeNull();
    expect(localStorage.getItem('userId')).toBeNull();
    expect(localStorage.getItem('theme')).toBe('dark');
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
