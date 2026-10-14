import { TEXT_LIMITS, reviewLimitError } from './text-limits';
describe('long reviews',()=>{
 it('accepts exactly 50k UTF-16 units including emojis',()=>{
  expect(reviewLimitError('🎮'.repeat(TEXT_LIMITS.REVIEW/2))).toBe('');
 });
 it('rejects overflow without truncating the supplied text',()=>{
  const review='a'.repeat(TEXT_LIMITS.REVIEW+1);
  expect(reviewLimitError(review)).toContain('50.000');
  expect(review.length).toBe(TEXT_LIMITS.REVIEW+1);
 });
});
