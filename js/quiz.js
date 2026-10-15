// Pure quiz logic: shuffling, distractor selection and question building.

export const MODES = ['ar2m', 'm2ar', 'match'];
/** Modes answered by picking one of four options (the rest of MODES is matching). */
export const CHOICE_MODES = ['ar2m', 'm2ar'];
export const MATCH_SIZE = 5;
export const MATCH_MIN = 3;

/** Unbiased in-place Fisher–Yates shuffle; returns the array. `rng` returns [0, 1). */
export function shuffle(arr, rng = Math.random) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

export function meaningOf(w, lang) {
  return (w && w.m && (w.m[lang] || w.m.en)) || '';
}

/** Normalised meaning text used to detect "the same answer" between options. */
export function meaningKey(w, lang) {
  return meaningOf(w, lang).toLowerCase().normalize('NFC').replace(/\s+/g, ' ').trim();
}

/**
 * Picks `n` distractors for `target` from `source`.
 * Rules: same `cat`, nearby frequency rank, never the same meaning text in `lang` as the target
 * (or as each other), never the same Arabic skeleton `sk` as the target (or as each other).
 * Falls back to other categories only if the same category cannot supply enough options.
 */
export function pickDistractors(target, source, { lang = 'en', n = 3, rng = Math.random, window = 24 } = {}) {
  const tKey = meaningKey(target, lang);
  const usedKeys = new Set([tKey]);
  const usedSk = new Set([target.sk]);
  const chosen = [];

  const eligible = (w) => w && w.id !== target.id && w.sk !== target.sk && meaningKey(w, lang) !== tKey && meaningKey(w, lang) !== '';
  const take = (cands) => {
    for (const w of cands) {
      if (chosen.length >= n) break;
      const k = meaningKey(w, lang);
      if (usedKeys.has(k) || usedSk.has(w.sk)) continue;
      usedKeys.add(k);
      usedSk.add(w.sk);
      chosen.push(w);
    }
  };
  const byDistance = (list) => list.slice().sort((a, b) => Math.abs(a.rank - target.rank) - Math.abs(b.rank - target.rank));

  const sameCat = byDistance(source.filter((w) => w.cat === target.cat && eligible(w)));
  take(shuffle(sameCat.slice(0, window), rng));
  if (chosen.length < n) take(sameCat.slice(window));
  if (chosen.length < n) take(byDistance(source.filter((w) => w.cat !== target.cat && eligible(w))));
  return chosen;
}

/**
 * Builds a multiple-choice question.
 * @returns {{mode:string, target:object, options:object[], correctId:string}|null}
 */
export function buildQuestion(mode, target, source, { lang = 'en', rng = Math.random } = {}) {
  if (!CHOICE_MODES.includes(mode) || !target) return null;
  const distractors = pickDistractors(target, source, { lang, n: 3, rng });
  if (distractors.length < 3) return null;
  const options = shuffle([target, ...distractors], rng);
  return { mode, target, options, correctId: target.id };
}

/** Whether a chosen option is correct — compared by word id, never by displayed text. */
export function isCorrect(question, optionId) {
  return Boolean(question) && optionId === question.correctId;
}

/**
 * Builds a matching round: up to `size` words whose meanings (in `lang`) and Arabic skeletons are
 * all distinct, so every pair has exactly one defensible partner. `right` is the meaning column,
 * shuffled independently of the Arabic column.
 * @returns {{mode:'match', words:object[], right:object[]}|null} null when fewer than MATCH_MIN fit.
 */
export function buildMatchRound(candidates, { lang = 'en', size = MATCH_SIZE, rng = Math.random } = {}) {
  const keys = new Set();
  const sks = new Set();
  const words = [];
  for (const w of candidates) {
    if (words.length >= size) break;
    const k = meaningKey(w, lang);
    if (!w || !k || keys.has(k) || sks.has(w.sk)) continue;
    keys.add(k);
    sks.add(w.sk);
    words.push(w);
  }
  if (words.length < MATCH_MIN) return null;
  return { mode: 'match', words, right: shuffle(words.slice(), rng) };
}

/** Picks a random element, preferring items accepted by `prefer` (e.g. chapter already loaded). */
export function pickTarget(pool, { rng = Math.random, prefer = null, avoidId = null } = {}) {
  const usable = pool.filter((w) => w.id !== avoidId);
  const list = usable.length ? usable : pool;
  if (!list.length) return null;
  if (prefer) {
    const preferred = list.filter(prefer);
    if (preferred.length && rng() < 0.85) return preferred[Math.floor(rng() * preferred.length)];
  }
  return list[Math.floor(rng() * list.length)];
}
