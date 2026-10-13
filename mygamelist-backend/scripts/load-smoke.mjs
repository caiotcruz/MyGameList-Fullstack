// Read-only smoke benchmark. Never runs against a remote host.
const base = 'http://localhost:8080';
const token = process.env.MGL_TEST_TOKEN;
if (!token) throw new Error('Defina MGL_TEST_TOKEN com o JWT de uma conta local de teste.');
const path = process.env.MGL_TEST_PATH;
if (!path || !path.startsWith('/') || path.startsWith('//')) throw new Error('Defina MGL_TEST_PATH com uma rota GET do projeto, por exemplo a rota de busca copiada da aba Network.');
const url = new URL(path, base);
if (url.origin !== base) throw new Error('Somente localhost:8080 permitido.');
const results=[];
await Promise.all(Array.from({length:5}, async()=>{
  for(let n=0;n<20;n++) {
    const start=performance.now();
    try {
      const r=await fetch(url,{headers:{Authorization:`Bearer ${token}`},signal:AbortSignal.timeout(10000),redirect:'error'});
      await r.arrayBuffer(); results.push({ms:performance.now()-start,status:r.status});
    }catch{results.push({ms:performance.now()-start,status:'network-error'});}
  }
}));
const times=results.map(x=>x.ms).sort((a,b)=>a-b);
const statuses=Object.groupBy(results,x=>x.status);
console.log({requests:results.length,concurrency:5,p50:times[49],p95:times[94],statuses:Object.fromEntries(Object.entries(statuses).map(([k,v])=>[k,v.length]))});
