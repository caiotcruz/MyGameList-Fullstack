export function passwordError(value:string):string {
  if(value.length<12)return 'Use uma senha com pelo menos 12 caracteres.';
  if(new TextEncoder().encode(value).length>72)return 'A senha deve ter no máximo 72 bytes; emojis e acentos podem ocupar mais de um byte.';
  return '';
}
