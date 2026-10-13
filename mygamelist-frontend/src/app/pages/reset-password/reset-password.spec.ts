import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { Login } from '../login/login';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResetPassword } from './reset-password';

describe('ResetPassword', () => {
  let component: ResetPassword;
  let fixture: ComponentFixture<ResetPassword>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'login', component: Login }]), provideHttpClient(), provideHttpClientTesting()],
      imports: [ResetPassword]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ResetPassword);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('redirects when the required authentication parameters are missing', () => {
    expect(component).toBeTruthy();
    expect(TestBed.inject(Router).url).toBe('/login');
  });
});
