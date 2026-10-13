import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { FollowService } from './follow';

describe('Follow', () => {
  let service: FollowService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(FollowService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
