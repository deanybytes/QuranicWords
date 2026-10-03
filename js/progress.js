// Learner progress & gamification: XP, levels, daily goal, day streak, mistakes, lessons and the
// spaced-repetition cards. Pure helpers operate on a plain state object; ProgressStore persists it
// in localStorage under a schema-versioned key.
import { readJSON, writeJSON, remove } from './storage.js';
import { grade as srsGrade, introduce as srsIntroduce, isValidCard } from './srs.js';

export const STORAGE_KEY = 'qw.progress.v1';
export const SCHEMA = 1;
export const GOALS = [20, 50, 100];
export const XP_PER_CORRECT = 10;
export const STREAK_MILESTONES = [7, 30, 100];
const DAILY_KEEP = 60;
const ID_RE = /^[A-Za-z0-9_-]{1,40}$/;

export function emptyState() {
  return { schema: SCHEMA, cards: {}, xp: 0, daily: {}, streak: { cur: 0, best: 0, last: null }, goal: 20, mistakes: {}, lessons: {} };
}

/** Level from total XP: floor((1 + sqrt(1 + 0.08·xp)) / 2). Level 1 at 0 XP. */
export function levelForXp(xp) {
  return Math.floor((1 + Math.sqrt(1 + 0.08 * Math.max(0, xp))) / 2);
}

/** Minimum total XP for a level (inverse of levelForXp): 50·L·(L−1). */
export function xpForLevel(level) {
  return 50 * level * (level - 1);
}

/** XP for a first-try correct answer given the combo length *including* this answer. */
export function xpForAnswer(combo) {
  return XP_PER_CORRECT + (combo >= 10 ? 5 : combo >= 5 ? 2 : 0);
}

/** Current streak as displayed: broken if the last active day is before yesterday. */
export function currentStreak(state, today) {
  const s = state.streak || {};
  return s.last !== null && s.last !== undefined && s.last >= today - 1 ? (s.cur || 0) : 0;
}

/** Marks `today` as an active day. Returns the streak milestone reached (7/30/100) or null. */
export function touchStreak(state, today) {
  const s = state.streak || (state.streak = { cur: 0, best: 0, last: null });
  if (s.last === today) return null;
  s.cur = s.last === today - 1 ? (s.cur || 0) + 1 : 1;
  s.last = today;
  s.best = Math.max(s.best || 0, s.cur);
  return STREAK_MILESTONES.includes(s.cur) ? s.cur : null;
}

function pruneDaily(state, today) {
  for (const k of Object.keys(state.daily)) if (Number(k) < today - DAILY_KEEP) delete state.daily[k];
}

/**
 * Records a first-try answer. Mutates state and returns what happened.
 * @returns {{xp:number, levelUp:number|null, milestone:number|null, goalReached:boolean}}
 */
export function recordAnswer(state, { id, correct, today, combo = 1 }) {
  const before = levelForXp(state.xp);
  const todayBefore = state.daily[today] || 0;
  const milestone = touchStreak(state, today);
  let xp = 0;
  if (correct) {
    xp = xpForAnswer(combo);
    state.xp += xp;
    state.daily[today] = todayBefore + xp;
    const m = state.mistakes[id];
    if (m) { m.n -= 1; if (m.n <= 0) delete state.mistakes[id]; }
  } else if (id) {
    const m = state.mistakes[id] || (state.mistakes[id] = { n: 0, last: today });
    m.n += 1;
    m.last = today;
  }
  pruneDaily(state, today);
  const after = levelForXp(state.xp);
  return {
    xp,
    levelUp: after > before ? after : null,
    milestone,
    goalReached: todayBefore < state.goal && (state.daily[today] || 0) >= state.goal,
  };
}

export function gradeCard(state, id, g, today) {
  state.cards[id] = srsGrade(state.cards[id], g, today);
  return state.cards[id];
}

export function introduceCard(state, id, today) {
  state.cards[id] = srsIntroduce(state.cards[id], today);
  return state.cards[id];
}

/** Share of all Qur'anic word occurrences covered by learned words (0..1). */
export function knownFraction(cards, wordsById, totalOccurrences) {
  if (!totalOccurrences) return 0;
  let sum = 0;
  for (const [id, c] of Object.entries(cards || {})) {
    if (c && c.b >= 1) { const w = wordsById.get(id); if (w) sum += w.occ || 0; }
  }
  return Math.min(1, sum / totalOccurrences);
}

/**
 * Validates and normalises a progress object (from storage or an imported file).
 * Accepts either the raw state or an export envelope {app, schema, progress}. Returns null if invalid.
 */
export function validateProgress(input) {
  if (!input || typeof input !== 'object') return null;
  const src = input.progress && typeof input.progress === 'object' ? input.progress : input;
  if (input.progress && input.app !== 'QuranicWords') return null;
  if (src.schema !== SCHEMA) return null;
  const out = emptyState();
  if (src.cards !== undefined) {
    if (!src.cards || typeof src.cards !== 'object' || Array.isArray(src.cards)) return null;
    for (const [id, c] of Object.entries(src.cards)) {
      if (!ID_RE.test(id) || !isValidCard(c)) return null;
      out.cards[id] = { b: c.b, due: c.due, n: Math.max(0, c.n | 0), l: Math.max(0, c.l | 0), last: Number.isInteger(c.last) ? c.last : c.due };
    }
  }
  if (src.xp !== undefined) {
    if (typeof src.xp !== 'number' || !Number.isFinite(src.xp) || src.xp < 0) return null;
    out.xp = Math.floor(src.xp);
  }
  if (src.daily && typeof src.daily === 'object') {
    for (const [d, v] of Object.entries(src.daily)) if (/^\d{1,6}$/.test(d) && Number.isFinite(v) && v >= 0) out.daily[d] = Math.floor(v);
  }
  if (src.streak && typeof src.streak === 'object') {
    const { cur, best, last } = src.streak;
    out.streak = {
      cur: Number.isInteger(cur) && cur >= 0 ? cur : 0,
      best: Number.isInteger(best) && best >= 0 ? best : 0,
      last: Number.isInteger(last) ? last : null,
    };
  }
  if (GOALS.includes(src.goal)) out.goal = src.goal;
  if (src.mistakes && typeof src.mistakes === 'object') {
    for (const [id, m] of Object.entries(src.mistakes)) {
      if (ID_RE.test(id) && m && Number.isInteger(m.n) && m.n > 0) out.mistakes[id] = { n: m.n, last: Number.isInteger(m.last) ? m.last : 0 };
    }
  }
  if (src.lessons && typeof src.lessons === 'object') {
    for (const [k, v] of Object.entries(src.lessons)) if (/^\d+-\d+-\d+$/.test(k) && Number.isInteger(v)) out.lessons[k] = v;
  }
  return out;
}

export function exportEnvelope(state, now = new Date()) {
  return { app: 'QuranicWords', schema: SCHEMA, exportedAt: now.toISOString(), progress: state };
}

export function lessonKey(ch, sec, les) {
  return `${ch}-${sec}-${les}`;
}

/** Persistent store with change listeners. */
export class ProgressStore {
  constructor() {
    this.state = validateProgress(readJSON(STORAGE_KEY, null)) || emptyState();
    this.listeners = new Set();
    this.combo = 0;
  }

  save() {
    writeJSON(STORAGE_KEY, this.state);
    for (const fn of this.listeners) {
      try { fn(this.state); } catch (e) { console.error(e); }
    }
  }

  onChange(fn) {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  /** First-try answer: XP/combo/mistakes/streak. Returns events for celebrations. */
  answer(id, correct, today) {
    this.combo = correct ? this.combo + 1 : 0;
    const ev = recordAnswer(this.state, { id, correct, today, combo: this.combo });
    ev.combo = this.combo;
    this.save();
    return ev;
  }

  grade(id, g, today) {
    const c = gradeCard(this.state, id, g, today);
    this.save();
    return c;
  }

  introduce(ids, today) {
    for (const id of ids) introduceCard(this.state, id, today);
    this.save();
  }

  completeLesson(key, today) {
    this.state.lessons[key] = today;
    this.save();
  }

  setGoal(goal) {
    if (GOALS.includes(goal)) { this.state.goal = goal; this.save(); }
  }

  clearMistakes() {
    this.state.mistakes = {};
    this.save();
  }

  replace(state) {
    this.state = state;
    this.combo = 0;
    this.save();
  }

  reset() {
    remove(STORAGE_KEY);
    this.replace(emptyState());
  }
}
