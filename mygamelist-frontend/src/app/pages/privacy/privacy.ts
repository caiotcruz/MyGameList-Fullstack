import { finalize } from 'rxjs';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SafetyService, MessagePolicy, BlockedUser, Report } from '../../services/safety';
import { Feedback, errorMessage } from '../../services/feedback';
@Component({selector:'app-privacy',standalone:true,imports:[FormsModule,RouterModule],styleUrl:'./privacy.css',template:`
<main class="privacy-page">
 <a [routerLink]="['/profile',myId]">← Voltar ao perfil</a>
 <h1>Privacidade e segurança</h1><p>Escolha como você quer participar da comunidade.</p>
 @if(error){<p class="form-error" role="alert">{{error}}</p>}
 <section class="privacy-card"><h2>Quem pode me enviar mensagens?</h2>
 <p>Essa escolha vale para novos envios, inclusive em conversas existentes. Seu histórico continua disponível.</p>
 <label for="message-policy">Permitir mensagens de</label><select id="message-policy" [(ngModel)]="policy" [disabled]="!loaded||saving"><option value="PEOPLE_I_FOLLOW">Somente pessoas que eu sigo</option><option value="EVERYONE">Qualquer pessoa</option><option value="NOBODY">Ninguém</option></select>
 <button type="button" (click)="save()" [disabled]="!loaded||saving">{{saving?'Salvando…':'Salvar preferência'}}</button></section>
 <section class="privacy-card"><h2>Pessoas bloqueadas</h2>
 @for(user of blocks;track user.id){<div class="blocked-row"><span>{{user.name}}</span><button type="button" [disabled]="unblocking!==null" (click)="unblock(user.id)">Desbloquear</button></div>}@empty{<p>Nenhuma pessoa bloqueada.</p>}
 <p>Desbloquear não restaura os follows anteriores.</p></section>
 @if(moderator){<section class="privacy-card"><h2>Fila de moderação</h2><p>As denúncias não removem conteúdo automaticamente. Registre o resultado da análise e a ação tomada.</p>
 @for(report of reports;track report.id){<article class="moderation-report"><h3>#{{report.id}} · {{report.target_type}} #{{report.target_id}}</h3><p>{{report.reason}} · {{report.status}}</p><blockquote>{{report.snapshot}}</blockquote><p>{{report.details}}</p><label>Status<select [(ngModel)]="report.status"><option>OPEN</option><option>REVIEWING</option><option>RESOLVED</option><option>DISMISSED</option></select></label><label>Registro da análise<textarea [(ngModel)]="report.moderator_note" maxlength="2000"></textarea></label><button [disabled]="reviewing!==null||!report.moderator_note?.trim()" (click)="review(report)">Salvar análise</button></article>}@empty{<p>Nenhuma denúncia nesta página.</p>}
 <button [disabled]="page===0||loadingReports" (click)="loadReports(page-1)">Anterior</button><span> Página {{page+1}} </span><button [disabled]="reports.length<30||loadingReports" (click)="loadReports(page+1)">Próxima</button></section>}
</main>`})
export class Privacy implements OnInit {
 cdr=inject(ChangeDetectorRef);
 safety=inject(SafetyService);feedback=inject(Feedback);myId=Number(localStorage.getItem('userId'));
 policy:MessagePolicy='PEOPLE_I_FOLLOW';blocks:BlockedUser[]=[];reports:Report[]=[];moderator=false;saving=false;loaded=false;error='';page=0;loadingReports=false;reviewing:number|null=null;unblocking:number|null=null;
 ngOnInit(){this.safety.me().pipe(finalize(()=>this.cdr.markForCheck())).subscribe({next:r=>{this.policy=r.messagePolicy;this.moderator=r.moderator;this.loaded=true;if(r.moderator)this.loadReports(0);},error:e=>this.error=errorMessage(e)});this.loadBlocks();}
 loadBlocks(){this.safety.blocks().pipe(finalize(()=>this.cdr.markForCheck())).subscribe({next:r=>this.blocks=r,error:e=>this.error=errorMessage(e)});}
 save(){this.saving=true;this.error='';this.safety.preferences(this.policy).pipe(finalize(()=>this.cdr.markForCheck())).subscribe({next:()=>{this.saving=false;this.feedback.show('Preferência salva.');},error:e=>{this.saving=false;this.error=errorMessage(e);}});}
 unblock(id:number){this.unblocking=id;this.safety.unblock(id).pipe(finalize(()=>this.cdr.markForCheck())).subscribe({next:()=>{this.unblocking=null;this.blocks=this.blocks.filter(u=>u.id!==id);this.feedback.show('Usuário desbloqueado.');},error:e=>{this.unblocking=null;this.feedback.error(e);}});}
 loadReports(page:number){this.loadingReports=true;this.safety.reports(page).pipe(finalize(()=>this.cdr.markForCheck())).subscribe({next:r=>{this.page=page;this.reports=r;this.loadingReports=false;},error:e=>{this.loadingReports=false;this.feedback.error(e);}});}
 review(r:Report){this.reviewing=r.id;this.safety.review(r.id,r.status,r.moderator_note||'').pipe(finalize(()=>this.cdr.markForCheck())).subscribe({next:()=>{this.reviewing=null;this.feedback.show('Análise registrada.');},error:e=>{this.reviewing=null;this.feedback.error(e);}});}
}
