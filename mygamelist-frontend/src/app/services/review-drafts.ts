import { Injectable } from '@angular/core';
@Injectable({providedIn:'root'})
export class ReviewDrafts {
 private key(id:unknown){return `mgl:review-draft:${localStorage.getItem('userId')}:${id}`;}
 save(entry:any){if(!entry?.rawgId||!localStorage.getItem('userId'))return;try{sessionStorage.setItem(this.key(entry.rawgId),JSON.stringify({savedAt:Date.now(),entry}));}catch{/* Form remains in memory if storage is unavailable. */}}
 restore(id:unknown):any|null {try{const raw=sessionStorage.getItem(this.key(id));if(!raw)return null;const draft=JSON.parse(raw);if(Date.now()-draft.savedAt>86400000){this.clear(id);return null;}return draft.entry?.rawgId==id?draft.entry:null;}catch{return null;}}
 clear(id:unknown){try{sessionStorage.removeItem(this.key(id));}catch{}}
}
