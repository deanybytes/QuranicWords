// Pure search / filter / sort over the word index.

const ALEF_MAP = { 'أ': 'ا', 'إ': 'ا', 'آ': 'ا', 'ٱ': 'ا', 'ى': 'ي', 'ة': 'ه', 'ؤ': 'و', 'ئ': 'ي' };

/**
 * Arabic skeleton: identical to tools/export/build_web_data.py `skeleton()` so that a query is
 * compared with the precomputed `sk` field — diacritic- and hamza-seat-insensitive.
 */
export function skeleton(text) {
  let out = '';
  for (const ch of String(text || '').normalize('NFC')) {
    const c = ALEF_MAP[ch] || ch;
    if (c >= 'ء' && c <= 'ي') out += c;
  }
  return out;
}

/** Latin folding for transliteration / meaning search: lowercase, no diacritics or ʿ ʾ ' marks. */
export function latinFold(text) {
  return String(text || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[ʿʾ'`’‘]/g, '');
}

const HAS_ARABIC = /[؀-ۿ]/;

/** Returns true when the word matches the free-text query. */
export function matchesQuery(w, query, lang) {
  const q = String(query || '').trim();
  if (!q) return true;
  if (HAS_ARABIC.test(q)) {
    const sq = skeleton(q);
    if (!sq) return false;
    if (w.sk && w.sk.includes(sq)) return true;
    if (w.rt && skeleton(w.rt) === sq) return true;
    return false;
  }
  const fq = latinFold(q);
  if (!fq) return true;
  if (w.tl && latinFold(w.tl).includes(fq)) return true;
  if (w.m) {
    if (w.m[lang] && latinFold(w.m[lang]).includes(fq)) return true;
    if (lang !== 'en' && w.m.en && latinFold(w.m.en).includes(fq)) return true;
  }
  if (/\d+:\d+/.test(q) && w.ref && w.ref.includes(q)) return true;
  return false;
}

/**
 * Filters and sorts the word list.
 * @param {Array} words
 * @param {{q?:string, ch?:number|null, cat?:string|null, root?:string|null, saved?:boolean,
 *          savedIds?:Set<string>, sort?:string, lang?:string}} opts
 */
export function filterWords(words, opts = {}) {
  const { q = '', ch = null, cat = null, root = null, saved = false, savedIds = new Set(), sort = 'curriculum', lang = 'en' } = opts;
  const out = words.filter((w) =>
    (!saved || savedIds.has(w.id)) &&
    (!ch || w.ch === ch) &&
    (!cat || w.cat === cat) &&
    (!root || w.rt === root) &&
    matchesQuery(w, q, lang));
  return sortWords(out, sort, lang);
}

export function curriculumCompare(a, b) {
  return (a.ch - b.ch) || (a.sec - b.sec) || (a.les - b.les) || (a.rank - b.rank);
}

export function sortWords(list, sort, lang = 'en') {
  const arr = list.slice();
  switch (sort) {
    case 'freq_desc': arr.sort((a, b) => (b.occ - a.occ) || (a.rank - b.rank)); break;
    case 'freq_asc': arr.sort((a, b) => (a.occ - b.occ) || (b.rank - a.rank)); break;
    case 'alpha_ar': arr.sort((a, b) => (a.sk || '').localeCompare(b.sk || '', 'ar')); break;
    case 'alpha_meaning': {
      const m = (w) => (w.m && (w.m[lang] || w.m.en)) || '';
      arr.sort((a, b) => m(a).localeCompare(m(b), lang === 'in' ? 'id' : lang));
      break;
    }
    default: arr.sort(curriculumCompare);
  }
  return arr;
}
