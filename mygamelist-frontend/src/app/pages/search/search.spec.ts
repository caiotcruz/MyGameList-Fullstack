import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Search } from './search';

describe('Search', () => {
  let component: Search;
  let fixture: ComponentFixture<Search>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
      imports: [Search]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Search);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(()=>localStorage.clear());

  it('filters a game added locally while recommendations load',()=>{
    localStorage.setItem('token','fixture');component.carregarRecomendacoes();
    component.userGameIds.add(22);
    TestBed.inject(HttpTestingController).expectOne(r=>r.url.endsWith('/games/recommendations')).flush({state:'READY',pendingGenres:0,genres:[],games:[
      {rawgId:22,title:'Already added',matchedGenres:[],affinity:.8},
      {rawgId:23,title:'New',matchedGenres:['puzzle'],affinity:.8}
    ]});
    expect(component.recommendations?.games.map(g=>g.rawgId)).toEqual([23]);
    expect(component.recommendationsLoading).toBe(false);
  });

  it('allows retry without breaking search when recommendations fail',()=>{
    localStorage.setItem('token','fixture');component.games=[{id:5,name:'Search result'}];
    component.carregarRecomendacoes();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne(r=>r.url.endsWith('/games/recommendations')).flush({}, {status:503,statusText:'Unavailable'});
    expect(component.recommendationsError).toContain('Não foi possível');
    expect(component.games[0].id).toBe(5);
    component.carregarRecomendacoes();
    http.expectOne(r=>r.url.endsWith('/games/recommendations')).flush({state:'EMPTY_HISTORY',pendingGenres:0,genres:[],games:[]});
    expect(component.recommendationsError).toBe('');
    expect(component.recommendations?.state).toBe('EMPTY_HISTORY');
  });

  it('ignores an obsolete recommendation response',()=>{
    localStorage.setItem('token','fixture');const http=TestBed.inject(HttpTestingController);
    component.carregarRecomendacoes();const older=http.expectOne(r=>r.url.endsWith('/games/recommendations'));
    component.carregarRecomendacoes();const newer=http.expectOne(r=>r.url.endsWith('/games/recommendations'));
    newer.flush({state:'READY',pendingGenres:0,genres:[],games:[]});
    older.flush({state:'EMPTY_HISTORY',pendingGenres:0,genres:[],games:[]});
    expect(component.recommendations?.state).toBe('READY');
  });

  it('should create' , () => {
    expect(component).toBeTruthy();
  });
});
