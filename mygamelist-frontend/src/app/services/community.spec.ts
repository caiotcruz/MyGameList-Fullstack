import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CommunityService } from './community';

describe('Community', () => {
  let service: CommunityService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CommunityService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
