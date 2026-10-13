import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { Register } from '../register/register';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Verify } from './verify';

describe('Verify', () => {
  let component: Verify;
  let fixture: ComponentFixture<Verify>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'register', component: Register }]), provideHttpClient(), provideHttpClientTesting()],
      imports: [Verify]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Verify);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('redirects when the required authentication parameters are missing', () => {
    expect(component).toBeTruthy();
    expect(TestBed.inject(Router).url).toBe('/register');
  });
});
