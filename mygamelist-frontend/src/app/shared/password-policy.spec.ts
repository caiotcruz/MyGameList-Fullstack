import { passwordError } from './password-policy';
describe('password policy',()=>{
 it('accepts passphrases without arbitrary composition rules',()=>expect(passwordError('uma frase longa e memoravel')).toBe(''));
 it('rejects short passwords',()=>expect(passwordError('12345678901')).not.toBe(''));
 it('enforces bcrypt UTF-8 byte limit',()=>{expect(passwordError('é'.repeat(36))).toBe('');expect(passwordError('é'.repeat(37))).not.toBe('');});
});
