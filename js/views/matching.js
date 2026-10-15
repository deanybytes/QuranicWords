// Matching round renderer, shared by the Quiz view and the Learn lesson check.
// Tap an Arabic word, then its meaning (either side first). A right pair locks and fades;
// a wrong pair shakes. Each word is graded once, on its first attempt.
import { h, clear } from '../dom.js';
import { t, formatNumber, getLang, langInfo, pick } from '../i18n.js';

const SETTLE_MS = 420;

/** Sets the entrance-stagger index through the CSSOM (inline style attributes are blocked by the CSP). */
function stagger(el, i) {
  el.style.setProperty('--i', String(i));
  return el;
}

/**
 * Renders `round` (from buildMatchRound) into `el`. Calls onPair(wordId, firstTryCorrect) once per
 * word, then onNext() when the learner moves on after the last pair. Returns a cleanup function.
 */
export function renderMatch(el, round, { onPair, onNext, header = null }) {
  const timers = new Set();
  const later = (fn, ms) => { const id = setTimeout(() => { timers.delete(id); fn(); }, ms); timers.add(id); };
  const graded = new Map();
  const matched = new Set();
  let pending = null; // { side: 'l' | 'r', id }
  clear(el);
  const info = langInfo(getLang());

  const feedback = h('p', { class: 'quiz-feedback', attrs: { role: 'status', 'aria-live': 'polite' } });
  const nextBtn = h('button', { type: 'button', class: 'btn btn-primary', hidden: true, text: t('quizNext'), on: { click: () => onNext() } });

  const tile = (side, w, i) => stagger(h('button', {
    type: 'button',
    class: `match-tile ${side === 'l' ? 'match-ar' : 'match-meaning'}`,
    dataset: { id: w.id, side },
    attrs: side === 'l'
      ? { lang: 'ar', dir: 'rtl', 'aria-pressed': 'false' }
      : { lang: info.bcp47, 'aria-pressed': 'false' },
    text: side === 'l' ? w.ar : pick(w.m),
  }), i);
  const left = h('div', { class: 'match-col' }, ...round.words.map((w, i) => tile('l', w, i)));
  const right = h('div', { class: 'match-col' }, ...round.right.map((w, i) => tile('r', w, i)));
  const board = h('div', { class: 'match-board', attrs: { role: 'group', 'aria-label': t('matchPrompt') } }, left, right);

  const tileFor = (side, id) => (side === 'l' ? left : right).querySelector(`[data-id="${CSS.escape(id)}"]`);
  const select = (p) => {
    for (const b of board.querySelectorAll('.match-tile[aria-pressed="true"]')) b.setAttribute('aria-pressed', 'false');
    pending = p;
    if (p) tileFor(p.side, p.id)?.setAttribute('aria-pressed', 'true');
  };
  const grade = (id, ok) => {
    if (graded.has(id)) return;
    graded.set(id, ok);
    onPair(id, ok);
  };

  const onTap = (btn) => {
    const { id, side } = btn.dataset;
    if (matched.has(id) || btn.classList.contains('matched')) return;
    if (!pending || pending.side === side) { select(pending && pending.side === side && pending.id === id ? null : { side, id }); return; }
    const leftId = side === 'l' ? id : pending.id;
    const rightId = side === 'r' ? id : pending.id;
    const a = tileFor('l', leftId);
    const b = tileFor('r', rightId);
    select(null);
    if (leftId === rightId) {
      matched.add(leftId);
      grade(leftId, true);
      for (const x of [a, b]) { x.classList.add('matched'); x.setAttribute('aria-disabled', 'true'); }
      if (matched.size === round.words.length) later(done, SETTLE_MS);
    } else {
      // The Arabic word picked is the one the learner did not know.
      grade(leftId, false);
      for (const x of [a, b]) {
        x.classList.remove('mismatch');
        void x.offsetWidth; // restart the shake on a repeat mistake
        x.classList.add('mismatch');
      }
      later(() => { a.classList.remove('mismatch'); b.classList.remove('mismatch'); }, SETTLE_MS);
    }
  };
  board.addEventListener('click', (e) => {
    const btn = e.target.closest('.match-tile');
    if (btn && board.contains(btn)) onTap(btn);
  });

  function done() {
    const firstTry = [...graded.values()].filter(Boolean).length;
    board.classList.add('is-done');
    feedback.textContent = t('matchDone', { score: formatNumber(firstTry), n: formatNumber(round.words.length) });
    feedback.className = `quiz-feedback ${firstTry === round.words.length ? 'ok' : 'bad'}`;
    nextBtn.hidden = false;
    nextBtn.focus();
  }

  el.append(
    header || '',
    h('p', { class: 'quiz-prompt', text: t('matchPrompt') }),
    board,
    h('div', { class: 'quiz-footer' }, feedback, nextBtn));
  return () => { for (const id of timers) clearTimeout(id); timers.clear(); };
}
