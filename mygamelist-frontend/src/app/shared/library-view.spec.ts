import { LibraryEntry, LibraryViewOptions, selectLibrary } from './library-view';
const options:LibraryViewOptions={query:'',status:'',favoritesOnly:false,sort:'default',direction:'asc'};
const games:LibraryEntry[]=[
 {id:1,game:{title:'Pokémon 10',rawgId:10},status:'DROPPED',score:null,updatedAt:'2026-01-01'},
 {id:2,game:{title:'Pokémon 2',rawgId:20},status:'COMPLETED',score:9,updatedAt:'2026-02-01'},
 {id:3,game:{title:'Portal',rawgId:30},status:'PLAYING',score:8,favorite:true,updatedAt:'2025-01-01'}
];
describe('library selection',()=>{
 it('searches by substring ignoring accents/case and never mutates the source',()=>{
  const before=JSON.stringify(games);
  expect(selectLibrary(games,{...options,query:'KEMON',sort:'title'}).map(e=>e.id)).toEqual([2,1]);
  expect(JSON.stringify(games)).toBe(before);
 });
 it('combines filters and keeps the favorite first in default ordering',()=>{
  expect(selectLibrary(games,options).map(e=>e.id)).toEqual([3,2,1]);
  expect(selectLibrary(games,{...options,status:'DROPPED',query:'poke'}).map(e=>e.id)).toEqual([1]);
  expect(selectLibrary(games,{...options,favoritesOnly:true}).map(e=>e.id)).toEqual([3]);
 });
 it('puts unrated games last in either direction',()=>{
  expect(selectLibrary(games,{...options,sort:'score',direction:'desc'}).map(e=>e.id)).toEqual([2,3,1]);
  expect(selectLibrary(games,{...options,sort:'score',direction:'asc'}).map(e=>e.id)).toEqual([3,2,1]);
 });
 it('keeps future provider playtime separate from ratings and missing playtime last',()=>{
  const entry={...games[0],externalProgress:[{provider:'steam' as const,externalGameId:'123',playtimeMinutes:0,unlockedAchievements:0,totalAchievements:10,syncedAt:'2026-10-13'}]};
  expect(selectLibrary([games[1],entry],{...options,sort:'playtime',direction:'desc'}).map(e=>e.id)).toEqual([1,2]);
  expect(entry.score).toBeNull();
 });
});
