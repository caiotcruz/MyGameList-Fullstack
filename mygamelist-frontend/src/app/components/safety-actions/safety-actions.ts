import { Observable, finalize } from 'rxjs';
import { ChangeDetectorRef, Component, Input, Output, EventEmitter, inject, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SafetyService, ReportTarget } from '../../services/safety';
import { Feedback, errorMessage } from '../../services/feedback';
@Component({selector:'app-safety-actions',standalone:true,imports:[FormsModule],template:`
@if(ownerId !== myId){
 <details #actions class="safety-actions" (click)="$event.stopPropagation()">
  <summary class="safety-more" aria-label="Mais opções deste conteúdo" title="Mais opções">···</summary>
  <span class="safety-actions-menu">
  <button type="button" class="safety-link" (click)="actions.open=false; open('report')" aria-label="Denunciar este conteúdo">Denunciar</button>
  @if(allowBlock){<button type="button" class="safety-link" (click)="actions.open=false; open('block')">Bloquear</button>}
  </span>
 </details>
}
<dialog #dialog class="safety-dialog" aria-label="Proteção da comunidade" (cancel)="onCancel($event)">
 <form (ngSubmit)="submit()">
  <h2>{{mode==='block'?'Bloquear usuário':'Denunciar conteúdo'}}</h2>
  @if(mode==='block'){
   <p>Vocês deixarão de se seguir e não poderão interagir ou ver os perfis e conversas um do outro. O histórico será preservado. Você pode desbloquear em Privacidade.</p>
  }@else{
   <p>Sua denúncia será enviada à moderação. Ela não é exibida ao usuário denunciado.</p>
   <label>Motivo<select name="reason" [(ngModel)]="reason" required><option value="SPAM">Spam</option><option value="HARASSMENT">Assédio</option><option value="HATE">Discurso de ódio</option><option value="INAPPROPRIATE">Conteúdo inadequado</option><option value="OTHER">Outro motivo</option></select></label>
   <label>Detalhes (opcional)<textarea name="details" [(ngModel)]="details" maxlength="2000" rows="4"></textarea></label>
  }
  @if(error){<p class="form-error" role="alert">{{error}}</p>}
  <div class="safety-dialog-actions"><button type="button" [disabled]="busy" (click)="dialog.close()">Cancelar</button><button type="submit" [disabled]="busy">{{busy?'Enviando…':mode==='block'?'Confirmar bloqueio':'Enviar denúncia'}}</button></div>
 </form>
</dialog>`})
export class SafetyActions {
 @Input({required:true}) targetType!:ReportTarget;
 @Input({required:true}) targetId!:number;
 @Input({required:true}) ownerId!:number;
 @Input() allowBlock=false;
 @Output() blocked=new EventEmitter<void>();
 @ViewChild('dialog') dialog!:ElementRef<HTMLDialogElement>;
 cdr=inject(ChangeDetectorRef);
 safety=inject(SafetyService);feedback=inject(Feedback);
 get myId(){return Number(localStorage.getItem('userId'));}
 mode:'report'|'block'='report';reason='SPAM';details='';error='';busy=false;
 open(mode:'report'|'block'){this.mode=mode;this.error='';this.dialog.nativeElement.showModal();}
 onCancel(event:Event){if(this.busy)event.preventDefault();}
 submit(){if(this.busy)return;this.busy=true;this.error='';
 const request:Observable<unknown>=this.mode==='block'?this.safety.block(this.ownerId):this.safety.report(this.targetType,this.targetId,this.reason,this.details);
 request.pipe(finalize(()=>this.cdr.markForCheck())).subscribe({next:()=>{this.busy=false;this.dialog.nativeElement.close();this.feedback.show(this.mode==='block'?'Usuário bloqueado.':'Denúncia recebida para análise.');if(this.mode==='block')this.blocked.emit();this.details='';},error:(e)=>{this.busy=false;this.error=errorMessage(e);}});
 }
}
