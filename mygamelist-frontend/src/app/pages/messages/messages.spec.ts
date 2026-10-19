import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { Message, MessageThread } from '../../services/message';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Messages } from './messages';

describe('Messages', () => {
  let component: Messages;
  let fixture: ComponentFixture<Messages>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
      imports: [Messages]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Messages);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
  const message = (id: number): Message => ({id, senderId: 2, content: 'text', sentAt: '2026-10-14T12:00:00', mine: false});
  const page = (ids: number[], nextBefore: number | null): MessageThread => ({partner: {id: 2, name: 'Other', profilePicture: null}, messages: ids.map(message), canSend: true, nextBefore});

  it('merges earlier messages without duplicates and advances the cursor', () => {
    component.activeUserId = 2; component.thread = [message(51), message(52)]; component.nextBefore = 51;
    const get = vi.spyOn(component.messageService, 'getThread').mockReturnValue(of(page([49, 50, 51], 49)));
    component.carregarAnteriores();
    expect(get).toHaveBeenCalledWith(2, 51);
    expect(component.thread.map(m => m.id)).toEqual([49, 50, 51, 52]);
    expect(component.nextBefore).toBe(49);
    expect(component.loadingOlder).toBe(false);
  });

  it('ignores a late previous-page response after changing conversations', () => {
    const response = new Subject<MessageThread>();
    vi.spyOn(component.messageService, 'getThread').mockReturnValue(response);
    component.activeUserId = 2; component.nextBefore = 51;
    component.carregarAnteriores();
    component.activeUserId = 3; component.thread = [message(200)];
    response.next(page([50], null)); response.complete();
    expect(component.thread.map(m => m.id)).toEqual([200]);
  });

  it('keeps already loaded history when polling the latest page', () => {
    component.activeUserId = 2; component.thread = [message(1), message(51)];
    vi.spyOn(component.messageService, 'getThread').mockReturnValue(of(page([51, 52], 51)));
    component.atualizarThreadSilencioso(2);
    expect(component.thread.map(m => m.id)).toEqual([1, 51, 52]);
  });
});
