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

async function getJSON(url) {
  const res = await fetch(url, { credentials: 'same-origin' });
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
  }

  async load() {
    const [index, roots] = await Promise.all([getJSON(DATA_URLS.index), getJSON(DATA_URLS.roots)]);
    if (!index || !index.meta || !Array.isArray(index.words)) throw new Error('index.json: unexpected shape');
    this.meta = index.meta;
    this.words = index.words;
    this.byId = new Map(this.words.map((w) => [w.id, w]));
    this.roots = Object.entries(roots || {}).map(([root, ids]) => ({ root, ids: ids.filter((id) => this.byId.has(id)) }));
    this.lessons = buildLessons(this.words);
    this.lessonByKey = new Map(this.lessons.map((l) => [l.key, l]));
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
      return { i: i + 1, m: x.m, ref: v.ref, v_ar: v.ar, s: x.s, e: x.e, wbw: v.wbw, ws: x.ws, we: x.we, tr: v.tr, ts: x.ts, te: x.te };
    });
  }

  /** First sense's example verse, or null. */
  async verseFor(wordId, lang = getLang()) {
    return (await this.sensesFor(wordId, lang))[0] || null;
  }
}
