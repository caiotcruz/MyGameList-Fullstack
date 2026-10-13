import { Component, inject } from '@angular/core';
import { Feedback } from '../services/feedback';
@Component({selector:'app-feedback',standalone:true,template:`@if(feedback.message()){<div class="feedback-toast" role="status" aria-live="polite"><span>{{feedback.message()}}</span><button type="button" aria-label="Fechar aviso" (click)="feedback.message.set('')">×</button></div>}`})
export class FeedbackHost { feedback=inject(Feedback); }
