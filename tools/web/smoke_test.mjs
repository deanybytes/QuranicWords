#!/usr/bin/env node
// Smoke tests for the QuranicWords web app's pure modules and static wiring.
// Run from the repository root:  node tools/web/smoke_test.mjs
import assert from 'node:assert/strict';
import { readFileSync, existsSync, readdirSync, statSync } from 'node:fs';
import { join, dirname, relative } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const imp = (p) => import(pathToFileURL(join(ROOT, p)).href);
const read = (p) => readFileSync(join(ROOT, p), 'utf8');

let passed = 0;
let failed = 0;
async function test(name, fn) {
  try {
    await fn();
    passed++;
    console.log(`  ok  ${name}`);
  } catch (e) {
    failed++;
    console.log(`  FAIL ${name}\n       ${e && e.stack ? e.stack.split('\n').slice(0, 3).join('\n       ') : e}`);
  }
}

const { escapeHtml, splitSpan, splitHighlight, isTypingTarget } = await imp('js/dom.js');
const i18n = await imp('js/i18n.js');
const router = await imp('js/router.js');
const srs = await imp('js/srs.js');
const progress = await imp('js/progress.js');
const quiz = await imp('js/quiz.js');
const search = await imp('js/search.js');
const { buildLessons } = await imp('js/data.js');
const { APP_VERSION } = await imp('js/config.js');
const index = JSON.parse(read('data/index.json'));
const WORDS = index.words;
const META = index.meta;
const byId = new Map(WORDS.map((w) => [w.id, w]));

// Deterministic RNG for reproducible tests.
function rng(seed = 42) {
  let s = seed >>> 0;
  return () => { s = (s * 1664525 + 1013904223) >>> 0; return s / 2 ** 32; };
}

console.log('i18n');
await test('exactly the 8 data languages are offered', () => {
  assert.deepEqual(i18n.LANG_CODES, ['en', 'bn', 'ur', 'hi', 'in', 'tr', 'fa', 'fr']);
  assert.deepEqual([...i18n.LANG_CODES].sort(), [...META.languages].sort());
});
await test('every language has the same keys as English', () => {
  const ref = Object.keys(i18n.DICTIONARY.en).sort();
  for (const code of i18n.LANG_CODES) {
    const keys = Object.keys(i18n.DICTIONARY[code]).sort();
    const missing = ref.filter((k) => !keys.includes(k));
    const extra = keys.filter((k) => !ref.includes(k));
    assert.deepEqual([missing, extra], [[], []], `${code}: missing ${missing} extra ${extra}`);
  }
});
await test('placeholders match across languages and strings are non-empty', () => {
  const ph = (s) => (String(s).match(/\{\w+\}/g) || []).sort().join(',');
  for (const code of i18n.LANG_CODES) {
    for (const [k, v] of Object.entries(i18n.DICTIONARY.en)) {
      const tr = i18n.DICTIONARY[code][k];
      assert.ok(typeof tr === 'string' && tr.trim(), `${code}.${k} empty`);
      assert.equal(ph(tr), ph(v), `${code}.${k} placeholders`);
    }
  }
});
await test('UI strings contain no emoji or HTML (icons live in markup)', () => {
  const emoji = /\p{Extended_Pictographic}/u;
  for (const code of i18n.LANG_CODES) {
    for (const [k, v] of Object.entries(i18n.DICTIONARY[code])) {
      assert.ok(!emoji.test(v), `${code}.${k} has emoji: ${v}`);
      assert.ok(!/<[a-z/]/i.test(v), `${code}.${k} has HTML`);
    }
  }
});
await test('surahWord values and citation format', () => {
  const expected = { en: 'Surah', bn: 'সূরা', ur: 'سورۃ', hi: 'सूरह', in: 'Surah', tr: 'Sure', fa: 'سوره', fr: 'Sourate' };
  for (const [code, word] of Object.entries(expected)) assert.equal(i18n.DICTIONARY[code].surahWord, word);
  assert.equal(i18n.cite('Al-Baqarah 2:22', 'en'), 'Surah Al-Baqarah 2:22');
  assert.equal(i18n.cite('Al-Baqarah 2:22', 'tr'), 'Sure Al-Baqarah 2:22');
});
await test('language metadata: RTL for ur/fa, BCP-47 "id" for Indonesian', () => {
  assert.equal(i18n.langInfo('ur').dir, 'rtl');
  assert.equal(i18n.langInfo('fa').dir, 'rtl');
  for (const c of ['en', 'bn', 'hi', 'in', 'tr', 'fr']) assert.equal(i18n.langInfo(c).dir, 'ltr');
  assert.equal(i18n.langInfo('in').bcp47, 'id');
  assert.equal(i18n.normalizeLang('id'), 'in');
  assert.equal(i18n.normalizeLang('fr-CA'), 'fr');
  assert.equal(i18n.normalizeLang('ms'), null);
  assert.equal(i18n.normalizeLang('sw'), null);
});
await test('translate falls back and formats parameters', () => {
  assert.equal(i18n.translate('en', 'wordsFound', { count: 5 }), '5 words found');
  assert.equal(i18n.translate('xx', 'navLearn'), 'Learn');
  assert.equal(i18n.format('{a}-{b}-{c}', { a: 1, b: 2 }), '1-2-{c}');
});

console.log('dom helpers');
await test('escapeHtml escapes all HTML-significant characters', () => {
  assert.equal(escapeHtml('<img src=x onerror="alert(1)">'), '&lt;img src=x onerror=&quot;alert(1)&quot;&gt;');
  assert.equal(escapeHtml("a&b'c`"), 'a&amp;b&#39;c&#96;');
  assert.equal(escapeHtml(null), '');
  assert.equal(escapeHtml(42), '42');
});
await test('span / highlight splitting', () => {
  assert.deepEqual(splitSpan('abcdef', 1, 3), ['a', 'bc', 'def']);
  assert.equal(splitSpan('abc', 2, 9), null);
  assert.equal(splitSpan('abc', 2, 2), null);
  assert.deepEqual(splitHighlight('And We guided', 'We'), ['And ', 'We', ' guided']);
  assert.equal(splitHighlight('abc', 'z'), null);
  assert.equal(splitHighlight('abc', ''), null);
});
await test('keyboard shortcuts are suppressed in text fields', () => {
  assert.equal(isTypingTarget({ tagName: 'INPUT', type: 'search' }), true);
  assert.equal(isTypingTarget({ tagName: 'INPUT', type: 'text' }), true);
  assert.equal(isTypingTarget({ tagName: 'TEXTAREA' }), true);
  assert.equal(isTypingTarget({ tagName: 'SELECT' }), true);
  assert.equal(isTypingTarget({ tagName: 'INPUT', type: 'checkbox' }), false);
  assert.equal(isTypingTarget({ tagName: 'BUTTON' }), false);
  assert.equal(isTypingTarget({ tagName: 'DIV', isContentEditable: true }), true);
  assert.equal(isTypingTarget(null), false);
});

console.log('routing');
await test('clean paths map to views', () => {
  const cases = { '/': 'cards', '/index.html': 'cards', '/dictionary': 'cards', '/dictionary/': 'cards', '/flashcards': 'flashcards', '/roots': 'roots', '/quiz': 'quiz', '/learn': 'learn', '/review': 'review', '/progress': 'progress', '/table': 'table', '/nope': 'cards' };
  for (const [p, v] of Object.entries(cases)) assert.equal(router.parseRoute(p, '').view, v, p);
});
await test('legacy ?view= / ?mode= still work and win over the path', () => {
  assert.equal(router.parseRoute('/', '?view=roots').view, 'roots');
  assert.equal(router.parseRoute('/', '?mode=flashcards').view, 'flashcards');
  assert.equal(router.parseRoute('/dictionary', '?view=quiz').view, 'quiz');
  assert.equal(router.parseRoute('/roots', '?view=bogus').view, 'roots');
});
await test('filters parse, including legacy pos/chapter/sort values', () => {
  const r = router.parseRoute('/dictionary', '?q=qala&ch=3&cat=verb&sort=freq_desc&saved=1&root=%D9%83-%D8%AA-%D8%A8&lang=bn');
  assert.deepEqual(r, { view: 'cards', q: 'qala', ch: 3, cat: 'VERB', root: 'ك-ت-ب', sort: 'freq_desc', saved: true, lang: 'bn' });
  assert.equal(router.parseRoute('/', '?pos=particle').cat, 'PARTICLE');
  assert.equal(router.parseRoute('/', '?chapter=ch_07').ch, 7);
  assert.equal(router.parseRoute('/', '?sort=occ_desc').sort, 'freq_desc');
  assert.equal(router.parseRoute('/', '?sort=evil').sort, 'curriculum');
  assert.equal(router.parseRoute('/', '?ch=abc').ch, null);
});
await test('buildUrl round-trips and omits defaults', () => {
  assert.equal(router.buildUrl('learn', { q: 'x' }), '/learn');
  assert.equal(router.buildUrl('cards', {}), '/dictionary');
  const url = router.buildUrl('table', { q: 'كتب', ch: 2, cat: 'NOUN', sort: 'alpha_ar', saved: true });
  const [path, qs] = url.split('?');
  const back = router.parseRoute(path, `?${qs}`);
  assert.equal(back.view, 'table');
  assert.equal(back.q, 'كتب');
  assert.equal(back.ch, 2);
  assert.equal(back.cat, 'NOUN');
  assert.equal(back.sort, 'alpha_ar');
  assert.equal(back.saved, true);
  for (const v of router.VIEWS) assert.equal(router.parseRoute(router.buildUrl(v, {}), '').view, v);
});

console.log('spaced repetition');
await test('intervals 0,1,3,7,16 for boxes 1-5', () => {
  assert.deepEqual(srs.INTERVALS, [0, 1, 3, 7, 16]);
  assert.deepEqual([1, 2, 3, 4, 5].map(srs.intervalFor), [0, 1, 3, 7, 16]);
});
await test('Leitner grading moves cards between boxes', () => {
  const T = 20000;
  let c = srs.introduce(undefined, T);
  assert.deepEqual([c.b, c.due], [1, T]);
  assert.equal(srs.isDue(c, T), true);
  c = srs.grade(c, 'good', T);
  assert.deepEqual([c.b, c.due], [2, T + 1]);
  assert.equal(srs.isDue(c, T), false);
  c = srs.grade(c, 'easy', T + 1);
  assert.deepEqual([c.b, c.due], [4, T + 8]);
  c = srs.grade(c, 'easy', T + 8);
  assert.deepEqual([c.b, c.due], [5, T + 24]);
  c = srs.grade(c, 'good', T + 24);
  assert.equal(c.b, 5, 'box is capped at 5');
  c = srs.grade(c, 'again', T + 40);
  assert.deepEqual([c.b, c.due, c.l], [1, T + 40, 1]);
  assert.throws(() => srs.grade(c, 'meh', T));
  const fresh = srs.grade(undefined, 'good', T);
  assert.deepEqual([fresh.b, fresh.due, fresh.l], [1, T, 0]);
  assert.equal(srs.introduce({ b: 3, due: T + 5 }, T).b, 3, 'introduce keeps existing progress');
});
await test('due queue, next due day and box counts', () => {
  const T = 100;
  const cards = { a: { b: 1, due: 100 }, b: { b: 3, due: 98 }, c: { b: 2, due: 101 }, d: { b: 5, due: 90 } };
  assert.deepEqual(srs.dueIds(cards, T), ['d', 'b', 'a']);
  assert.equal(srs.nextDueDay(cards, T), 101);
  assert.deepEqual(srs.boxCounts(cards), [1, 1, 1, 0, 1]);
});
await test('local day numbers', () => {
  const d1 = new Date(2026, 0, 1, 0, 5);
  const d2 = new Date(2026, 0, 1, 23, 55);
  const d3 = new Date(2026, 0, 2, 0, 1);
  assert.equal(srs.dayNumber(d1), srs.dayNumber(d2));
  assert.equal(srs.dayNumber(d3), srs.dayNumber(d1) + 1);
  assert.equal(srs.dayToDate(srs.dayNumber(d1)).getDate(), 1);
});

console.log('progress & gamification');
await test('level formula floor((1+sqrt(1+0.08xp))/2) and thresholds', () => {
  assert.equal(progress.levelForXp(0), 1);
  assert.equal(progress.levelForXp(99), 1);
  assert.equal(progress.levelForXp(100), 2);
  assert.equal(progress.levelForXp(299), 2);
  assert.equal(progress.levelForXp(300), 3);
  for (let L = 1; L < 40; L++) {
    assert.equal(progress.levelForXp(progress.xpForLevel(L)), L);
    assert.equal(progress.levelForXp(progress.xpForLevel(L + 1) - 1), L);
  }
});
await test('XP: 10 per correct, +2 combo at >=5, +5 at >=10', () => {
  assert.equal(progress.xpForAnswer(1), 10);
  assert.equal(progress.xpForAnswer(4), 10);
  assert.equal(progress.xpForAnswer(5), 12);
  assert.equal(progress.xpForAnswer(9), 12);
  assert.equal(progress.xpForAnswer(10), 15);
  assert.equal(progress.xpForAnswer(30), 15);
});
await test('recordAnswer: XP, goal, mistakes, level-up', () => {
  const s = progress.emptyState();
  const T = 5000;
  let ev;
  for (let i = 1; i <= 2; i++) ev = progress.recordAnswer(s, { id: 'w1', correct: true, today: T, combo: i });
  assert.equal(s.xp, 20);
  assert.equal(ev.goalReached, true, 'default goal 20 reached on the 2nd answer');
  ev = progress.recordAnswer(s, { id: 'w1', correct: true, today: T, combo: 3 });
  assert.equal(ev.goalReached, false, 'goal reached only once');
  progress.recordAnswer(s, { id: 'w2', correct: false, today: T });
  progress.recordAnswer(s, { id: 'w2', correct: false, today: T });
  assert.equal(s.mistakes.w2.n, 2);
  progress.recordAnswer(s, { id: 'w2', correct: true, today: T, combo: 1 });
  assert.equal(s.mistakes.w2.n, 1);
  progress.recordAnswer(s, { id: 'w2', correct: true, today: T, combo: 2 });
  assert.equal(s.mistakes.w2, undefined);
  const s2 = progress.emptyState();
  s2.xp = 95;
  ev = progress.recordAnswer(s2, { id: 'x', correct: true, today: T, combo: 1 });
  assert.equal(ev.levelUp, 2);
});
await test('day streak with local days and milestones 7/30/100', () => {
  const s = progress.emptyState();
  const T = 9000;
  const milestones = [];
  for (let d = 0; d < 30; d++) {
    const m = progress.touchStreak(s, T + d);
    progress.touchStreak(s, T + d); // second activity the same day changes nothing
    if (m) milestones.push(m);
  }
  assert.deepEqual(milestones, [7, 30]);
  assert.equal(s.streak.cur, 30);
  assert.equal(progress.currentStreak(s, T + 29), 30);
  assert.equal(progress.currentStreak(s, T + 30), 30, 'still alive the next day');
  assert.equal(progress.currentStreak(s, T + 31), 0, 'broken after a missed day');
  progress.touchStreak(s, T + 35);
  assert.equal(s.streak.cur, 1);
  assert.equal(s.streak.best, 30);
});
await test('known share = sum(occ of learned) / meta.occurrences', () => {
  const ids = WORDS.slice(0, 3).map((w) => w.id);
  const cards = Object.fromEntries(ids.map((id) => [id, { b: 1, due: 0 }]));
  const expected = WORDS.slice(0, 3).reduce((a, w) => a + w.occ, 0) / META.occurrences;
  assert.ok(Math.abs(progress.knownFraction(cards, byId, META.occurrences) - expected) < 1e-12);
  assert.equal(progress.knownFraction({}, byId, META.occurrences), 0);
  assert.equal(progress.knownFraction(Object.fromEntries(WORDS.map((w) => [w.id, { b: 1, due: 0 }])), byId, META.occurrences), 1);
});
await test('progress validation accepts exports and rejects junk', () => {
  const s = progress.emptyState();
  s.xp = 120;
  s.cards.wn_abc = { b: 2, due: 100, n: 3, l: 0, last: 99 };
  s.goal = 50;
  s.lessons['1-1-1'] = 99;
  const env = progress.exportEnvelope(s);
  const back = progress.validateProgress(JSON.parse(JSON.stringify(env)));
  assert.equal(back.xp, 120);
  assert.equal(back.goal, 50);
  assert.deepEqual(back.cards.wn_abc, s.cards.wn_abc);
  assert.equal(progress.validateProgress(null), null);
  assert.equal(progress.validateProgress('x'), null);
  assert.equal(progress.validateProgress({ schema: 99 }), null);
  assert.equal(progress.validateProgress({ app: 'Other', schema: 1, progress: s }), null);
  assert.equal(progress.validateProgress({ schema: 1, xp: -5 }), null);
  assert.equal(progress.validateProgress({ schema: 1, cards: { 'bad id!': { b: 1, due: 1 } } }), null);
  assert.equal(progress.validateProgress({ schema: 1, cards: { ok: { b: 9, due: 1 } } }), null);
  assert.equal(progress.validateProgress({ schema: 1, goal: 999 }).goal, 20);
  assert.equal(progress.STORAGE_KEY, 'qw.progress.v1');
});
await test('ProgressStore survives missing/blocked storage', async () => {
  const storage = await imp('js/storage.js');
  storage.setStorageBackend({ getItem() { throw new Error('blocked'); }, setItem() { throw new Error('blocked'); }, removeItem() { throw new Error('blocked'); } });
  const store = new progress.ProgressStore();
  store.answer('w', true, 1);
  assert.equal(store.state.xp, 10);
  storage.setStorageBackend({ getItem: () => '{not json', setItem() {}, removeItem() {} });
  assert.equal(new progress.ProgressStore().state.xp, 0, 'corrupt JSON falls back to empty state');
  storage.setStorageBackend(null);
});

console.log('quiz');
await test('Fisher-Yates shuffle is a permutation and roughly uniform', () => {
  const r = rng(7);
  const counts = Array.from({ length: 4 }, () => [0, 0, 0, 0]);
  for (let i = 0; i < 8000; i++) {
    const a = quiz.shuffle([0, 1, 2, 3], r);
    assert.deepEqual([...a].sort(), [0, 1, 2, 3]);
    a.forEach((v, pos) => counts[v][pos]++);
  }
  for (const row of counts) for (const c of row) assert.ok(c > 1700 && c < 2300, `biased: ${row}`);
});
await test('distractors: same cat, nearby rank, unique meaning and skeleton', () => {
  const r = rng(11);
  for (const lang of i18n.LANG_CODES) {
    for (let i = 0; i < WORDS.length; i += 13) {
      const w = WORDS[i];
      const ds = quiz.pickDistractors(w, WORDS, { lang, rng: r });
      assert.equal(ds.length, 3, `${w.id} ${lang}`);
      const keys = new Set([quiz.meaningKey(w, lang)]);
      const sks = new Set([w.sk]);
      for (const d of ds) {
        assert.notEqual(d.id, w.id);
        assert.equal(d.cat, w.cat, `${w.id}: cat`);
        assert.ok(!keys.has(quiz.meaningKey(d, lang)), `${w.id}/${lang}: duplicate meaning ${quiz.meaningOf(d, lang)}`);
        assert.ok(!sks.has(d.sk), `${w.id}: duplicate skeleton`);
        keys.add(quiz.meaningKey(d, lang));
        sks.add(d.sk);
      }
      if (w.cat !== 'PARTICLE') for (const d of ds) assert.ok(Math.abs(d.rank - w.rank) < 400, `${w.id}: far rank ${d.rank}`);
    }
  }
});
await test('questions are graded by word id, not by text', () => {
  const target = { id: 'a', cat: 'NOUN', rank: 1, sk: 'x', m: { en: 'same' } };
  const twin = { id: 'b', cat: 'NOUN', rank: 2, sk: 'y', m: { en: 'same' } };
  const others = ['c', 'd', 'e'].map((id, i) => ({ id, cat: 'NOUN', rank: 3 + i, sk: id, m: { en: `m${id}` } }));
  const q = quiz.buildQuestion('ar2m', target, [target, twin, ...others], { lang: 'en', rng: rng(3) });
  assert.ok(!q.options.some((o) => o.id === 'b'), 'a distractor with identical meaning text is excluded');
  assert.equal(q.options.filter((o) => o.id === 'a').length, 1);
  assert.equal(quiz.isCorrect(q, 'a'), true);
  assert.equal(quiz.isCorrect(q, 'b'), false);
  assert.equal(quiz.buildQuestion('nope', target, others), null);
});
await test('verse fill-in blanks exactly the word span', () => {
  assert.deepEqual(quiz.verseBlank({ v_ar: 'abc def ghi', s: 4, e: 7 }), ['abc ', ' ghi']);
  assert.equal(quiz.verseBlank({ v_ar: 'abc', s: 1, e: 9 }), null);
  const v = JSON.parse(read('data/verses/ch_01.json'));
  for (const [id, entry] of Object.entries(v)) {
    const parts = quiz.verseBlank(entry);
    assert.ok(parts, id);
    assert.equal(parts[0].length + parts[1].length + (entry.e - entry.s), entry.v_ar.length);
  }
});
await test('pickTarget avoids repeating the previous word', () => {
  const pool = WORDS.slice(0, 2);
  for (let i = 0; i < 20; i++) assert.equal(quiz.pickTarget(pool, { avoidId: pool[0].id, rng: rng(i) }).id, pool[1].id);
  assert.equal(quiz.pickTarget([], {}), null);
});

console.log('search & data');
await test('JS skeleton matches the precomputed sk for every word', () => {
  for (const w of WORDS) assert.equal(search.skeleton(w.ar), w.sk, w.id);
});
await test('filter by category uses `cat` (the old POS filter never matched)', () => {
  for (const cat of ['NOUN', 'VERB', 'PARTICLE']) {
    const list = search.filterWords(WORDS, { cat });
    assert.ok(list.length > 0);
    assert.ok(list.every((w) => w.cat === cat));
  }
  const total = ['NOUN', 'VERB', 'PARTICLE'].reduce((a, cat) => a + search.filterWords(WORDS, { cat }).length, 0);
  assert.equal(total, WORDS.length);
});
await test('Arabic, root, transliteration, meaning and reference search', () => {
  const ktb = search.filterWords(WORDS, { q: 'كتب' });
  assert.ok(ktb.some((w) => w.rt === 'ك-ت-ب'));
  assert.ok(search.filterWords(WORDS, { q: 'قَالَ' }).length > 0, 'diacritics ignored');
  assert.ok(search.filterWords(WORDS, { q: 'qala' }).some((w) => w.tl === 'qāla'), 'transliteration folds ā');
  assert.ok(search.filterWords(WORDS, { q: 'and', lang: 'en' }).some((w) => w.id === WORDS[0].id));
  assert.ok(search.filterWords(WORDS, { q: 'dan', lang: 'in' }).some((w) => w.id === WORDS[0].id));
  assert.equal(search.filterWords(WORDS, { q: '' }).length, WORDS.length);
  const ch3 = search.filterWords(WORDS, { ch: 3 });
  assert.equal(ch3.length, META.chapters.find((c) => c.n === 3).words);
  const saved = search.filterWords(WORDS, { saved: true, savedIds: new Set([WORDS[5].id]) });
  assert.deepEqual(saved.map((w) => w.id), [WORDS[5].id]);
});
await test('sorting', () => {
  const desc = search.sortWords(WORDS, 'freq_desc');
  for (let i = 1; i < desc.length; i++) assert.ok(desc[i - 1].occ >= desc[i].occ);
  const cur = search.sortWords(WORDS, 'curriculum');
  for (let i = 1; i < cur.length; i++) assert.ok(search.curriculumCompare(cur[i - 1], cur[i]) <= 0);
});
await test('lessons partition the curriculum in order', () => {
  const lessons = buildLessons(WORDS);
  const seen = new Set();
  for (const l of lessons) for (const w of l.words) { assert.ok(!seen.has(w.id)); seen.add(w.id); assert.equal(l.key, `${w.ch}-${w.sec}-${w.les}`); }
  assert.equal(seen.size, WORDS.length);
  assert.ok(lessons.every((l) => l.words.length >= 3 && l.words.length <= 8));
  assert.equal(lessons[0].key, '1-1-1');
  for (let i = 1; i < lessons.length; i++) {
    const a = lessons[i - 1];
    const b = lessons[i];
    assert.ok(a.ch < b.ch || (a.ch === b.ch && (a.sec < b.sec || (a.sec === b.sec && a.les < b.les))));
  }
});
await test('meta stats are consistent with the data', () => {
  assert.equal(META.wordCount, WORDS.length);
  assert.equal(META.rootCount, Object.keys(JSON.parse(read('data/roots.json'))).length);
  assert.ok(WORDS.every((w) => !('au' in w)), 'no word carries an audio path (audio was withdrawn)');
});

console.log('static wiring');
await test('APP_VERSION matches the service worker cache version', () => {
  const m = read('sw.js').match(/const VERSION = '([^']+)'/);
  assert.ok(m);
  assert.equal(m[1], APP_VERSION);
});
await test('service-worker shell lists every JS/CSS/font file, and they all exist', () => {
  const sw = read('sw.js');
  const listed = [...sw.matchAll(/'(\/[^']+\.(?:js|css|woff2|png|json|html))'/g)].map((m) => m[1]);
  for (const p of listed) assert.ok(existsSync(join(ROOT, p)), `missing ${p}`);
  const walk = (dir) => readdirSync(join(ROOT, dir)).flatMap((f) => {
    const p = join(dir, f);
    return statSync(join(ROOT, p)).isDirectory() ? walk(p) : [p];
  });
  for (const f of [...walk('js'), ...walk('css')].filter((f) => /\.(js|css|woff2)$/.test(f))) {
    assert.ok(listed.includes(`/${f.split('\\').join('/')}`), `not precached: ${f}`);
  }
});
await test('index.html references only files that exist and is CSP-clean', () => {
  const html = read('index.html');
  const refs = [...html.matchAll(/(?:src|href)="(\/[^"#?]*)/g)].map((m) => m[1]);
  const routes = new Set(['/', ...Object.values(router.VIEW_TO_PATH)]);
  assert.ok(refs.length > 10);
  for (const r of refs) if (!routes.has(r)) assert.ok(existsSync(join(ROOT, r)), `missing ${r}`);
  assert.ok(!/\sstyle="/.test(html), 'no inline style attributes (CSP style-src self)');
  assert.ok(!/\son[a-z]+="/.test(html), 'no inline event handlers');
  const scripts = [...html.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)];
  for (const [, attrs, body] of scripts) {
    if (/application\/ld\+json/.test(attrs)) { JSON.parse(body); continue; }
    assert.ok(/src="/.test(attrs) && !body.trim(), 'no inline executable scripts');
  }
  assert.ok(!/google-site-verification/.test(html), 'bogus verification meta removed');
  assert.ok(!/fonts\.googleapis|cdn\.|https?:\/\/(?!quranicwords\.vercel\.app|schema\.org|github\.com|www\.gnu\.org|www\.w3\.org)/.test(html.replace(/<script type="application\/ld\+json">[\s\S]*?<\/script>/, '')), 'no third-party URLs');
  assert.ok(!/4,709|77,430|251 roots|11 languages|Apache/.test(html), 'no stale claims');
});
await test('JS never parses data as HTML or sets inline style attributes', () => {
  const files = readdirSync(join(ROOT, 'js')).filter((f) => f.endsWith('.js')).map((f) => `js/${f}`)
    .concat(readdirSync(join(ROOT, 'js/views')).map((f) => `js/views/${f}`));
  for (const f of files) {
    const src = read(f);
    assert.ok(!/\.innerHTML\s*=|insertAdjacentHTML|outerHTML\s*=|document\.write/.test(src), `${f} uses HTML injection`);
    assert.ok(!/setAttribute\(\s*['"]style/.test(src), `${f} sets a style attribute`);
    assert.ok(!/\beval\(|new Function\(/.test(src), `${f} uses eval`);
  }
  const css = read('css/main.css') + read('css/components.css');
  assert.ok(!/@import|https?:\/\//.test(css), 'CSS loads nothing third-party');
  assert.ok(!/outline:\s*none;\s*}\s*$/m.test(css.replace(/:focus\s*{\s*outline:\s*none;\s*}/, '').replace(/main:focus-visible\s*{\s*outline:\s*none;\s*}/, '')), 'focus outlines are not removed');
});
await test('manifest icons exist with the declared sizes and separate purposes', () => {
  const man = JSON.parse(read('manifest.json'));
  for (const icon of man.icons) {
    const buf = readFileSync(join(ROOT, icon.src));
    const w = buf.readUInt32BE(16);
    const h = buf.readUInt32BE(20);
    assert.equal(`${w}x${h}`, icon.sizes, icon.src);
    assert.ok(['any', 'maskable'].includes(icon.purpose), icon.purpose);
  }
  assert.ok(man.icons.some((i) => i.purpose === 'maskable' && i.sizes === '512x512'));
  assert.ok(man.icons.some((i) => i.purpose === 'any' && i.sizes === '192x192'));
  assert.ok(!/4,709|251|11 languages/.test(JSON.stringify(man)));
});
await test('vercel.json: CSP, routes, no standalone rewrites', () => {
  const v = JSON.parse(read('vercel.json'));
  const all = v.headers.flatMap((x) => x.headers.map((hh) => [x.source, hh.key, hh.value]));
  const csp = all.find(([, k]) => k === 'Content-Security-Policy');
  assert.ok(csp);
  for (const d of ["default-src 'self'", "script-src 'self'", "style-src 'self'", "img-src 'self' data:", "media-src 'self'", "connect-src 'self'", "font-src 'self'", "frame-ancestors 'self'", "base-uri 'self'", "form-action 'self'", "object-src 'none'"]) assert.ok(csp[2].includes(d), d);
  assert.ok(!/unsafe-inline|unsafe-eval/.test(csp[2]));
  assert.ok(all.some(([, k, val]) => k === 'Permissions-Policy' && /camera=\(\)/.test(val)));
  const rewrites = v.rewrites.map((r) => r.source);
  for (const p of ['/dictionary', '/flashcards', '/roots', '/quiz', '/learn', '/review', '/progress']) assert.ok(rewrites.includes(p), p);
  assert.ok(!rewrites.includes('/standalone') && !rewrites.includes('/v1.0.0'));
});
await test('.vercelignore excludes the Android app and tooling', () => {
  const ig = read('.vercelignore');
  for (const line of ['/app/', '/tools/', '/QuranicWords-v1.0.0.html']) {
    assert.ok(ig.split('\n').includes(line), line);
  }
});

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed ? 1 : 0);

