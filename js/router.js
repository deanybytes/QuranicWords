// URL <-> app state. Supports the clean paths that vercel.json rewrites to index.html
// (/dictionary, /flashcards, /roots, /quiz, …) as well as the legacy ?view= / ?mode= parameters.

export const VIEWS = ['learn', 'review', 'cards', 'table', 'flashcards', 'quiz', 'roots', 'progress'];
export const DEFAULT_VIEW = 'cards';

const PATH_TO_VIEW = {
  '/learn': 'learn',
  '/review': 'review',
  '/dictionary': 'cards',
  '/table': 'table',
  '/flashcards': 'flashcards',
  '/quiz': 'quiz',
  '/roots': 'roots',
  '/progress': 'progress',
};

export const VIEW_TO_PATH = {
  learn: '/learn',
  review: '/review',
  cards: '/dictionary',
  table: '/table',
  flashcards: '/flashcards',
  quiz: '/quiz',
  roots: '/roots',
  progress: '/progress',
};

const CATS = ['NOUN', 'VERB', 'PARTICLE'];
const LEGACY_POS = { noun: 'NOUN', verb: 'VERB', particle: 'PARTICLE', proper_noun: 'NOUN', pronoun: 'PARTICLE' };
export const SORTS = ['curriculum', 'freq_desc', 'freq_asc', 'alpha_ar', 'alpha_meaning'];
const LEGACY_SORT = { default: 'curriculum', occ_desc: 'freq_desc', occ_asc: 'freq_asc', alpha_en: 'alpha_meaning' };

function cleanPath(pathname) {
  let p = (pathname || '/').toLowerCase();
  if (p.length > 1) p = p.replace(/\/+$/, '');
  return p || '/';
}

/**
 * Parses a location into app state.
 * @returns {{view:string, q:string, ch:number|null, cat:string|null, root:string|null, sort:string, saved:boolean, lang:string|null}}
 */
export function parseRoute(pathname, search = '') {
  const params = new URLSearchParams(search || '');
  const fromPath = PATH_TO_VIEW[cleanPath(pathname)] || null;
  const qv = (params.get('view') || params.get('mode') || '').toLowerCase();
  const fromQuery = qv === 'dictionary' ? 'cards' : (VIEWS.includes(qv) ? qv : null);
  const view = fromQuery || fromPath || DEFAULT_VIEW;

  const chRaw = params.get('ch') || params.get('chapter');
  let ch = null;
  if (chRaw) {
    const n = parseInt(String(chRaw).replace(/^ch_0?/i, ''), 10);
    if (Number.isInteger(n) && n >= 1 && n <= 99) ch = n;
  }

  let cat = (params.get('cat') || '').toUpperCase();
  if (!CATS.includes(cat)) cat = LEGACY_POS[(params.get('pos') || '').toLowerCase()] || null;

  let sort = params.get('sort') || 'curriculum';
  sort = LEGACY_SORT[sort] || sort;
  if (!SORTS.includes(sort)) sort = 'curriculum';

  const q = (params.get('q') || params.get('search') || '').slice(0, 100);
  const root = (params.get('root') || '').slice(0, 20) || null;
  const saved = params.get('saved') === '1';
  const lang = params.get('lang') || null;
  return { view, q, ch, cat: cat || null, root, sort, saved, lang };
}

/** Builds a shareable URL (path + query) for a view and filter state. */
export function buildUrl(view, state = {}) {
  const path = VIEW_TO_PATH[view] || '/';
  const p = new URLSearchParams();
  const filterable = view === 'cards' || view === 'table' || view === 'flashcards' || view === 'quiz';
  if (filterable) {
    if (state.q) p.set('q', state.q);
    if (state.ch) p.set('ch', String(state.ch));
    if (state.cat) p.set('cat', state.cat);
    if (state.root) p.set('root', state.root);
    if (state.sort && state.sort !== 'curriculum') p.set('sort', state.sort);
    if (state.saved) p.set('saved', '1');
  }
  const qs = p.toString();
  return qs ? `${path}?${qs}` : path;
}
