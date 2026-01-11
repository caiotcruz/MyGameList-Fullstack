import { TestBed } from '@angular/core/testing';

import { Follow } from './follow';

describe('Follow', () => {
  let service: Follow;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Follow);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
