/** Future provider data stays separate from manually curated review/status/score.
 * steam app IDs must never be substituted for RAWG IDs.
 */
export interface ExternalGameProgress {
  provider: 'steam';
  externalGameId: string;
  playtimeMinutes: number | null;
  unlockedAchievements: number | null;
  totalAchievements: number | null;
  syncedAt: string;
}
export interface LibraryEntry {
  id: number;
  game: { title: string; rawgId: number; coverUrl?: string; background_image?: string };
  status: string;
  score?: number | null;
  review?: string | null;
  favorite?: boolean;
  spoiler?: boolean;
  updatedAt?: string;
  externalProgress?: ExternalGameProgress[];
}
export type LibrarySort = 'default'|'title'|'status'|'score'|'updatedAt'|'playtime';
export interface LibraryViewOptions {
  query: string; status: string; favoritesOnly: boolean;
  sort: LibrarySort; direction: 'asc'|'desc';
}
const statusOrder = ['PLAYING','PLAN_TO_PLAY','COMPLETED','PLATINUM','DROPPED'];
const collator = new Intl.Collator('pt-BR', {numeric:true,sensitivity:'base'});
export function normalizeGameTitle(value: string): string {
  return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR').trim();
}
function time(entry: LibraryEntry): number { const t=Date.parse(entry.updatedAt || ''); return Number.isFinite(t)?t:0; }
function playtime(entry: LibraryEntry): number | null {
  return entry.externalProgress?.find(p=>p.provider==='steam')?.playtimeMinutes ?? null;
}
export function selectLibrary(entries: readonly LibraryEntry[], options: LibraryViewOptions): LibraryEntry[] {
  const query=normalizeGameTitle(options.query);
  const filtered=entries.filter(e => (!query || normalizeGameTitle(e.game.title).includes(query))
    && (!options.status || e.status===options.status) && (!options.favoritesOnly || e.favorite));
  const direction=options.direction==='asc'?1:-1;
  return filtered.sort((a,b)=>{
    let result=0;
    switch(options.sort){
      case 'default': result=Number(!!b.favorite)-Number(!!a.favorite) || time(b)-time(a); break;
      case 'title': result=collator.compare(a.game.title,b.game.title)*direction; break;
      case 'status': result=(statusOrder.indexOf(a.status)-statusOrder.indexOf(b.status))*direction; break;
      case 'updatedAt': result=(time(a)-time(b))*direction; break;
      case 'score':
      case 'playtime': {
        const av=options.sort==='score'?(a.score && a.score>0?a.score:null):playtime(a);
        const bv=options.sort==='score'?(b.score && b.score>0?b.score:null):playtime(b);
        // Unknown values always remain last, including when the direction changes.
        if(av===null || bv===null) result=av===bv?0:av===null?1:-1;
        else result=(av-bv)*direction;
        break;
      }
    }
    return result || collator.compare(a.game.title,b.game.title) || a.id-b.id;
  });
}
