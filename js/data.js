// Data layer for the generated curriculum (tools/export/build_web_data.py):
//   data/index.json        {meta, words[]}   loaded at start
//   data/roots.json        {root: [wordIds]}
//   data/verses/ch_NN.json {wordId: verse}   lazy-loaded per chapter
// Offline caching and revalidation are handled by the service worker (sw.js).
import { DATA_URLS } from './config.js';
import { curriculumCompare } from './search.js';
import { lessonKey } from './progress.js';

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
    this.verseFiles = new Map(); // ch -> Promise<object>
    this.loadedChapters = new Set();
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

  /** Loads (once) the verse file of a chapter. Failed loads are not cached so they can be retried. */
  loadChapterVerses(ch) {
    if (!this.verseFiles.has(ch)) {
      const p = getJSON(DATA_URLS.verses(ch)).then((d) => { this.loadedChapters.add(ch); return d; });
      p.catch(() => this.verseFiles.delete(ch));
      this.verseFiles.set(ch, p);
    }
    return this.verseFiles.get(ch);
  }

  /** Verse context for a word, or null if the word has none. Rejects on network failure. */
  async verseFor(wordId) {
    const w = this.byId.get(wordId);
    if (!w) return null;
    const file = await this.loadChapterVerses(w.ch);
    return (file && file[wordId]) || null;
  }
}
