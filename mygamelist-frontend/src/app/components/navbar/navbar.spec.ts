import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { vi } from 'vitest';
import { Navbar } from './navbar';

describe('Navbar', () => {
  let component: Navbar;
  let fixture: ComponentFixture<Navbar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
      imports: [Navbar]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Navbar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    fixture.destroy();
  });

  it('renders the response on the first opening without another click', async () => {
    vi.spyOn(component, 'shouldShowNavbar').mockReturnValue(true);
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('[aria-label="Notificações"]').click();
    await fixture.whenStable();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne(r=>r.url.endsWith('/notifications')).flush([
      {id:1,actor:{id:2,name:'Ana'},type:'FOLLOW',read:true,createdAt:'2026-10-13T10:00:00'}
    ]);
    // No detectChanges or second click: the HTTP response must schedule rendering.
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.dropdown-list').textContent).toContain('Ana');
    expect(fixture.nativeElement.querySelector('.dropdown-list').textContent).not.toContain('Carregando');
  });

  it('shows an error and allows retry without closing the dropdown', async () => {
    vi.spyOn(component, 'shouldShowNavbar').mockReturnValue(true);
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('[aria-label="Notificações"]').click();
    await fixture.whenStable();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne(r=>r.url.endsWith('/notifications')).flush({}, {status:503,statusText:'Unavailable'});
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
    fixture.nativeElement.querySelector('[role="alert"] button').click();
    http.expectOne(r=>r.url.endsWith('/notifications')).flush([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.dropdown-list').textContent).toContain('Silêncio no lobby');
  });

  it('should create' , () => {
    expect(component).toBeTruthy();
  });
});
