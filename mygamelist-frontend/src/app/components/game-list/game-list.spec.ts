import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GameList } from './game-list';

describe('GameList', () => {
  let component: GameList;
  let fixture: ComponentFixture<GameList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
      imports: [GameList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GameList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    localStorage.removeItem('userId');
    sessionStorage.clear();
  });

  it('preserves a failed review and clears its draft only after a successful retry', () => {
    localStorage.setItem('userId', '991');
    component.abrirEdicao({game:{rawgId:123,title:'Fixture'}, status:'COMPLETED',score:9,review:''}, new Event('click'));
    component.editingGame.review = 'Minha review que não pode sumir';
    component.salvarAlteracoes();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne(r=>r.method==='POST').flush({message:'Serviço indisponível'}, {status:503,statusText:'Unavailable'});
    expect(component.isModalOpen).toBe(true);
    expect(component.isSaving).toBe(false);
    expect(component.saveError).toBeTruthy();
    expect(component.drafts.restore(123).review).toBe('Minha review que não pode sumir');
    component.fecharModal();
    component.abrirEdicao({game:{rawgId:123,title:'Fixture'},status:'COMPLETED',score:9,review:''},new Event('click'));
    expect(component.editingGame.review).toBe('Minha review que não pode sumir');
    component.salvarAlteracoes();
    http.expectOne(r=>r.method==='POST').flush({});
    expect(component.isModalOpen).toBe(false);
    expect(component.drafts.restore(123)).toBeNull();
  });

  it('should create' , () => {
    expect(component).toBeTruthy();
  });
});
