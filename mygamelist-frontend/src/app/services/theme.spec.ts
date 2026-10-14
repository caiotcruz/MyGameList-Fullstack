import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { THEMES, ThemeService, validTheme } from './theme';

describe('account themes',()=>{
 let events:Subject<NavigationEnd>;
 let router:{url:string;events:Subject<NavigationEnd>};
 beforeEach(()=>{
  localStorage.clear();events=new Subject();router={url:'/profile/1',events};
  TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),{provide:Router,useValue:router}]});
 });
 afterEach(()=>{TestBed.inject(HttpTestingController).verify();localStorage.clear();delete TestBed.inject(DOCUMENT).documentElement.dataset['theme'];});
 it('uses the server preference only in the signed-in area',()=>{
  localStorage.setItem('token','fixture');localStorage.setItem('userId','1');
  const service=TestBed.inject(ThemeService);const doc=TestBed.inject(DOCUMENT);
  TestBed.inject(HttpTestingController).expectOne(r=>r.url.endsWith('/users/1')).flush({theme:'TEAL'});
  expect(doc.documentElement.dataset['theme']).toBe('teal');
  for(const url of ['/landing','/login','/register','/verify?email=example']){
   router.url=url;events.next(new NavigationEnd(1,url,url));expect(doc.documentElement.dataset['theme']).toBe('teal');
  }
  router.url='/home';events.next(new NavigationEnd(2,'/home','/home'));
  expect(doc.documentElement.dataset['theme']).toBe('teal');
  expect(service.selected()).toBe('TEAL');
 });
 it('does not let an earlier load overwrite a newly saved choice',()=>{
  localStorage.setItem('token','fixture');localStorage.setItem('userId','1');
  const service=TestBed.inject(ThemeService);service.setSaved('AMBER');
  TestBed.inject(HttpTestingController).expectOne(r=>r.url.endsWith('/users/1')).flush({theme:'BLUE'});
  expect(service.selected()).toBe('AMBER');
 });
 it('resets to teal when switching accounts and ignores the previous response',()=>{
  localStorage.setItem('token','fixture');localStorage.setItem('userId','1');
  const service=TestBed.inject(ThemeService);const http=TestBed.inject(HttpTestingController);
  localStorage.setItem('userId','2');service.sync();
  http.expectOne(r=>r.url.endsWith('/users/1')).flush({theme:'ROSE'});
  expect(service.selected()).toBe('TEAL');
  http.expectOne(r=>r.url.endsWith('/users/2')).flush({theme:'TEAL'});
  expect(service.selected()).toBe('TEAL');
 });
 it('accepts every offered palette',()=>{expect(new Set(THEMES.map(t=>t.id)).size).toBe(9);expect(validTheme('BLUE')).toBe('BLUE');for(const theme of THEMES)expect(validTheme(theme.id)).toBe(theme.id);});
 it('rejects arbitrary theme values',()=>{expect(validTheme('purple')).toBe('TEAL');});
});
