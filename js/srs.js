// Leitner spaced repetition (pure functions). Days are integer "local day numbers" so that a
// review due "tomorrow" flips at local midnight, not at UTC midnight.

export const BOXES = 5;
/** Review interval in days for boxes 1..5. */
export const INTERVALS = [0, 1, 3, 7, 16];
export const GRADES = ['again', 'good', 'easy'];

/** Local calendar day number of a Date (days since 1970-01-01 in the user's time zone). */
export function dayNumber(date = new Date()) {
  return Math.floor(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86400000);
}

/** Converts a day number back to a Date at local noon (safe for display). */
export function dayToDate(day) {
  const d = new Date(day * 86400000);
  return new Date(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate(), 12);
}

export function intervalFor(box) {
  const b = Math.min(BOXES, Math.max(1, box | 0));
  return INTERVALS[b - 1];
}

/** A newly learned word: box 1, due today. Existing cards are returned unchanged. */
export function introduce(card, today) {
  if (card && card.b >= 1) return card;
  return { b: 1, due: today + intervalFor(1), n: 0, l: 0, last: today };
}

/**
 * Applies a grade. Missing card = box 0 (unseen).
 *   again -> box 1, good -> box + 1, easy -> box + 2 (max 5). due = today + interval(new box).
 */
export function grade(card, g, today) {
  if (!GRADES.includes(g)) throw new Error(`unknown grade ${g}`);
  const box = card && card.b >= 1 ? card.b : 0;
  let nb;
  if (g === 'again') nb = 1;
  else if (g === 'good') nb = Math.min(BOXES, box + 1);
  else nb = Math.min(BOXES, box + 2);
  return {
    b: nb,
    due: today + intervalFor(nb),
    n: ((card && card.n) || 0) + 1,
    l: ((card && card.l) || 0) + (g === 'again' && box > 0 ? 1 : 0),
    last: today,
  };
}

export function isDue(card, today) {
  return Boolean(card && card.b >= 1 && card.due <= today);
}

/** Ids of cards due on `today`, most overdue first, then lowest box. */
export function dueIds(cards, today) {
  return Object.entries(cards || {})
    .filter(([, c]) => isDue(c, today))
    .sort((a, b) => (a[1].due - b[1].due) || (a[1].b - b[1].b))
    .map(([id]) => id);
}

/** Earliest future due day (> today), or null. */
export function nextDueDay(cards, today) {
  let best = null;
  for (const c of Object.values(cards || {})) {
    if (c && c.b >= 1 && c.due > today && (best === null || c.due < best)) best = c.due;
  }
  return best;
}

/** Count of cards per box: index 0 = box 1. */
export function boxCounts(cards) {
  const counts = new Array(BOXES).fill(0);
  for (const c of Object.values(cards || {})) if (c && c.b >= 1 && c.b <= BOXES) counts[c.b - 1]++;
  return counts;
}

export function isValidCard(c) {
  return Boolean(c) && typeof c === 'object' && Number.isInteger(c.b) && c.b >= 1 && c.b <= BOXES && Number.isInteger(c.due);
}
