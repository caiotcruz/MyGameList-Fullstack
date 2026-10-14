import {readFileSync} from 'node:fs';
const front=readFileSync(new URL('../src/app/shared/text-limits.ts',import.meta.url),'utf8');
const back=readFileSync(new URL('../../mygamelist-backend/src/main/java/com/caiotcruz/mygamelist/dto/TextLimits.java',import.meta.url),'utf8');
for(const key of ['REVIEW','BIO','COMMENT','MESSAGE','NAME']){
 const a=Number(front.match(new RegExp(key+':\\s*(\\d+)'))?.[1]);
 const b=Number(back.match(new RegExp(key+'\\s*=\\s*(\\d+)'))?.[1]);
 if(!Number.isFinite(a)||a!==b)throw new Error(`Limites divergentes: ${key}`);
}
console.log('5 limites de texto iguais no frontend e backend.');
