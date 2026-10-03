// Data layer for the generated curriculum (tools/export/build_web_data.py):
//   data/index.json        {meta, words[]}   loaded at start
//   data/roots.json        {root: [wordIds]}
//   data/verses/LANG/ch_NN.json {words: {wordId: [sense]}, verses: {"s:a": verse}}
//                          lazy-loaded per language and chapter
// Offline caching and revalidation are handled by the service worker (sw.js).
import { DATA_URLS } from './config.js';
import { curriculumCompare } from './search.js';
import { lessonKey } from './progress.js';
import { getLang } from './i18n.js';

/** `fresh` asks for a network copy (the service worker honours cache: 'reload'). */
async function getJSON(url, { fresh = false } = {}) {
  const res = await fetch(url, { credentials: 'same-origin', ...(fresh ? { cache: 'reload' } : {}) });
  if (!res.ok) throw new Error(`${url}: HTTP ${res.status}`);
  return res.json();
}

/** Groups words into ordered lessons (chapter → section → lesson). Pure; exported for tests. */
export function buildLessons(words) {
  const map = new Map();
  for (const w of words.slice().sort(curriculumCompare)) {
    const key = lessonKey(w.ch, w.sec, w.les);
    let l = map.get(key);
    if (!l) { l = { key, ch: w.ch, sec: w.sec, les: w.les, words: [] }; map.set(key, l); }
    l.words.push(w);
  }
  return [...map.values()];
}

/** Puts one language's meanings (an array in index order) onto the words as w.m[lang]. */
export function attachMeanings(words, lang, list) {
  if (!Array.isArray(list) || list.length !== words.length) throw new Error(`meanings/${lang}.json: expected ${words.length} entries`);
  for (let i = 0; i < words.length; i++) {
    const w = words[i];
    (w.m || (w.m = {}))[lang] = list[i];
  }
}

/** Resolves each word's grammar-label index (w.p) to the shared {lang: label} entry. */
export function attachPos(words, table) {
  for (const w of words) w.pos = Number.isInteger(w.p) ? table[w.p] : null;
}

export class DataStore {
  constructor() {
    this.meta = null;
    this.words = [];
    this.byId = new Map();
    this.roots = [];          // [{root, ids}] sorted by size desc (file order)
    this.lessons = [];
    this.lessonByKey = new Map();
    this.verseFiles = new Map(); // "lang:ch" -> Promise<object>
    this.loadedChapters = new Set(); // "lang:ch"
    this.meaningLangs = new Map();   // lang -> Promise (one meanings file per language)
  }

  /** Loads the index (which carries English), roots and the meanings of `lang` (other languages
   * load on demand via ensureLanguage, keeping the first download small). */
  async load(lang = getLang()) {
    try {
      await this.loadFrom(lang, false);
    } catch (e) {
      // A cached copy from an older release can be out of step with this code: fetch fresh once.
      console.warn('Data load failed, retrying from the network', e);
      await this.loadFrom(lang, true);
    }
  }

  async loadFrom(lang, fresh) {
    const langs = lang === 'en' ? [] : [lang];
    const opts = { fresh };
    const [index, roots, ...lists] = await Promise.all([getJSON(DATA_URLS.index, opts), getJSON(DATA_URLS.roots, opts),
      ...langs.map((l) => getJSON(DATA_URLS.meanings(l), opts))]);
    if (!index || !index.meta || !Array.isArray(index.words) || !index.words.length) throw new Error('index.json: unexpected shape');
    if (!index.words[0].m || !index.words[0].m.en) throw new Error('index.json is out of date (no English meanings)');
    this.meta = index.meta;
    this.words = index.words;
    attachPos(this.words, this.meta.pos || []);
    this.meaningLangs.set('en', Promise.resolve());
    langs.forEach((l, i) => { attachMeanings(this.words, l, lists[i]); this.meaningLangs.set(l, Promise.resolve()); });
    this.byId = new Map(this.words.map((w) => [w.id, w]));
    this.roots = Object.entries(roots || {}).map(([root, ids]) => ({ root, ids: ids.filter((id) => this.byId.has(id)) }));
    this.lessons = buildLessons(this.words);
    this.lessonByKey = new Map(this.lessons.map((l) => [l.key, l]));
  }

  hasLanguage(lang) {
    return this.meaningLangs.has(lang);
  }

  /** Loads (once) a language's meanings. Failed loads are not cached so they can be retried. */
  ensureLanguage(lang) {
    if (!this.meaningLangs.has(lang)) {
      const p = getJSON(DATA_URLS.meanings(lang)).then((list) => attachMeanings(this.words, lang, list));
      p.catch(() => this.meaningLangs.delete(lang));
      this.meaningLangs.set(lang, p);
    }
    return this.meaningLangs.get(lang);
  }

  chapter(n) {
    return (this.meta && this.meta.chapters.find((c) => c.n === n)) || null;
  }

  /** Whether the chapter's verse file for the current language is already loaded. */
  hasChapterVerses(ch, lang = getLang()) {
    return this.loadedChapters.has(`${lang}:${ch}`);
  }

  /** Loads (once) a chapter's verse file for a language. Failed loads are not cached so they can be retried. */
  loadChapterVerses(ch, lang = getLang()) {
    const key = `${lang}:${ch}`;
    if (!this.verseFiles.has(key)) {
      const p = getJSON(DATA_URLS.verses(lang, ch)).then((d) => { this.loadedChapters.add(key); return d; });
      p.catch(() => this.verseFiles.delete(key));
      this.verseFiles.set(key, p);
    }
    return this.verseFiles.get(key);
  }

  /**
   * The word's senses in a language, each with its complete example verse:
   * [{i, m, ref, v_ar, s, e, wbw, ws, we, tr, ts, te}] - `s..e` is the taught word in the Arabic,
   * `ws..we` the sense text in the word-by-word line, `ts..te` the same text in the translation
   * (null when the translation does not contain it exactly once). Rejects on network failure.
   */
  async sensesFor(wordId, lang = getLang()) {
    const w = this.byId.get(wordId);
    if (!w) return [];
    const file = await this.loadChapterVerses(w.ch, lang);
    const items = (file && file.words && file.words[wordId]) || [];
    return items.map((x, i) => {
      const v = file.verses[x.v] || {};
      return { i: i + 1, m: x.m, key: x.v, ref: v.ref, v_ar: v.ar, s: x.s, e: x.e, wbw: v.wbw, ws: x.ws, we: x.we, tr: v.tr, ts: x.ts, te: x.te };
    });
  }

  /** First sense's example verse, or null. */
  async verseFor(wordId, lang = getLang()) {
    return (await this.sensesFor(wordId, lang))[0] || null;
  }
}
