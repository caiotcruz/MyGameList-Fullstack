import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
export type MessagePolicy='EVERYONE'|'PEOPLE_I_FOLLOW'|'NOBODY';
export type ReportTarget='USER'|'ACTIVITY'|'COMMENT'|'REVIEW'|'MESSAGE';
export interface BlockedUser { id:number;name:string; }
export interface Report {id:number;target_type:string;target_id:number;reason:string;details:string;snapshot:string;status:string;moderator_note:string|null;created_at:string;}
@Injectable({providedIn:'root'})
export class SafetyService {
 private http=inject(HttpClient);private url=environment.apiUrl+'/safety';
 me(){return this.http.get<{messagePolicy:MessagePolicy;moderator:boolean}>(this.url+'/me');}
 preferences(messagePolicy:MessagePolicy){return this.http.put<void>(this.url+'/preferences',{messagePolicy});}
 blocks(){return this.http.get<BlockedUser[]>(this.url+'/blocks');}
 relationship(id:number){return this.http.get<{blockedByMe:boolean;interactionAllowed:boolean}>(this.url+'/relationship/'+id);}
 block(id:number){return this.http.put<void>(this.url+'/blocks/'+id,{});}
 unblock(id:number){return this.http.delete<void>(this.url+'/blocks/'+id);}
 report(targetType:ReportTarget,targetId:number,reason:string,details:string){return this.http.post<{id:number}>(this.url+'/reports',{targetType,targetId,reason,details});}
 reports(page:number){return this.http.get<Report[]>(this.url+'/moderation/reports',{params:{page}});}
 review(id:number,status:string,note:string){return this.http.put<void>(this.url+'/moderation/reports/'+id,{status,note});}
}
