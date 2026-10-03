// Flashcards (browse the filtered list, grade to schedule) and Review (today's due queue).
import { buildUrl } from '../router.js';
import { h, clear } from '../dom.js';
import { t, formatNumber, formatDate, getLang, langInfo } from '../i18n.js';
import { catBadge, meaningText, verseToggle } from '../components.js';
import { dueIds, nextDueDay, dayToDate } from '../srs.js';

const GRADE_KEYS = { '1': 'again', '2': 'good', '3': 'easy' };

/** A flip card plus grading controls. Returns {el, flip()}. */
function flashcard(ctx, w, { flipped, onFlip, onGrade, graded, expanded }) {
  const info = langInfo(getLang());
  const front = h('span', { class: 'flashcard-face flashcard-front', attrs: { 'aria-hidden': String(flipped) } },
    h('span', { class: 'fc-arabic', attrs: { lang: 'ar', dir: 'rtl' }, text: w.ar }),
    w.rt ? h('span', { class: 'fc-root-tag', attrs: { lang: 'ar', dir: 'rtl' }, text: w.rt }) : null,
    h('span', { class: 'fc-hint-tap', text: t('fcHintFront') }));
  const back = h('span', { class: 'flashcard-face flashcard-back', attrs: { 'aria-hidden': String(!flipped) } },
    h('span', { class: 'fc-meaning', attrs: { lang: info.bcp47 }, text: meaningText(w) }),
    catBadge(w),
    h('span', { class: 'fc-occ', text: t('occurrences', { n: formatNumber(w.occ) }) }));
  const flipper = h('span', { class: `flashcard-flipper${flipped ? ' flipped' : ''}` }, front, back);
  const cardBtn = h('button', { type: 'button', class: 'flashcard-perspective-box', attrs: { 'aria-label': `${t('fcFlip')}: ${w.ar}`, 'aria-pressed': String(flipped) }, on: { click: onFlip } }, flipper);

  const grades = h('div', { class: 'grade-row', hidden: !flipped });
  if (graded) {
    grades.appendChild(h('p', { class: 'muted', text: t('boxLabel', { n: formatNumber(graded.b) }) }));
  } else {
    grades.append(h('p', { class: 'grade-prompt', text: t('gradePrompt') }),
      h('div', { class: 'grade-buttons' },
        ...[['again', 'gradeAgain', '1'], ['good', 'gradeGood', '2'], ['easy', 'gradeEasy', '3']].map(([g, key, k]) =>
          h('button', { type: 'button', class: `btn grade-${g}`, attrs: { 'aria-keyshortcuts': k }, on: { click: () => onGrade(g) } },
            h('span', { text: t(key) }), h('kbd', { attrs: { 'aria-hidden': 'true' }, text: k })))));
  }
  const extras = h('div', { class: 'fc-extras', hidden: !flipped }, w.ref ? verseToggle(ctx, w, expanded) : null);
  return h('div', { class: 'flashcard-wrap' }, cardBtn, grades, extras);
}

/* ---------------- Flashcards (browse) ---------------- */
export const flashcardsView = {
  index: 0,
  flipped: false,
  version: -1,
  graded: new Map(), // id -> card (graded this session: XP once per word)
  expanded: new Set(),

  render(ctx, el) {
    const list = ctx.filtered();
    if (this.version !== ctx.filterVersion) { this.version = ctx.filterVersion; this.index = 0; this.flipped = false; }
    clear(el);
    el.appendChild(h('div', { class: 'section-head' }, h('h1', { class: 'view-title', text: t('fcTitle') }), h('p', { class: 'muted', text: t('fcSub') })));
    if (!list.length) { el.appendChild(h('p', { class: 'empty-state', text: t('fcEmpty') })); return; }
    this.index = Math.min(this.index, list.length - 1);
    const w = list[this.index];
    this.current = w;
    const fc = flashcard(ctx, w, {
      flipped: this.flipped,
      graded: this.graded.get(w.id),
      expanded: this.expanded,
      onFlip: () => this.flip(ctx, el),
      onGrade: (g) => this.gradeCurrent(ctx, el, g),
    });
    el.append(
      h('div', { class: 'flashcard-controls-top' },
        h('span', { class: 'flashcard-progress-text', attrs: { role: 'status' }, text: t('fcCounter', { i: formatNumber(this.index + 1), n: formatNumber(list.length) }) }),
        ctx.store.state.cards[w.id] ? h('span', { class: 'flashcard-streak-badge', text: t('learnedBox', { n: formatNumber(ctx.store.state.cards[w.id].b) }) }) : null),
      fc,
      h('div', { class: 'flashcard-nav-bar' },
        h('button', { type: 'button', class: 'btn', disabled: this.index === 0, on: { click: () => this.move(ctx, el, -1) } }, h('span', { attrs: { 'aria-hidden': 'true' }, text: '←' }), h('span', { text: t('fcPrev') })),
        h('button', { type: 'button', class: 'btn', disabled: this.index >= list.length - 1, on: { click: () => this.move(ctx, el, 1) } }, h('span', { text: t('fcNext') }), h('span', { attrs: { 'aria-hidden': 'true' }, text: '→' }))),
      h('p', { class: 'kbd-hint', text: t('fcKeyboard') }));
  },

  flip(ctx, el) {
    this.flipped = !this.flipped;
    this.render(ctx, el);
    el.querySelector('.flashcard-perspective-box')?.focus();
  },

  move(ctx, el, delta) {
    const list = ctx.filtered();
    const next = this.index + delta;
    if (next < 0 || next >= list.length) return;
    this.index = next;
    this.flipped = false;
    this.render(ctx, el);
  },

  gradeCurrent(ctx, el, g) {
    const w = this.current;
    if (!w || this.graded.has(w.id) || !this.flipped) return;
    const card = ctx.gradeWord(w.id, g, true);
    this.graded.set(w.id, card);
    if (this.index < ctx.filtered().length - 1) { this.index++; this.flipped = false; }
    this.render(ctx, el);
  },

  onKey(ctx, el, e) {
    if (e.code === 'Space' || e.key === ' ') { e.preventDefault(); this.flip(ctx, el); }
    else if (e.key === 'ArrowRight') { e.preventDefault(); this.move(ctx, el, langInfo().dir === 'rtl' ? -1 : 1); }
    else if (e.key === 'ArrowLeft') { e.preventDefault(); this.move(ctx, el, langInfo().dir === 'rtl' ? 1 : -1); }
    else if (GRADE_KEYS[e.key] && this.flipped) this.gradeCurrent(ctx, el, GRADE_KEYS[e.key]);
  },
};

/* ---------------- Review (due queue) ---------------- */
export const reviewView = {
  session: null,
  expanded: new Set(),

  enter(ctx) {
    if (!this.session || this.session.done) {
      const ids = dueIds(ctx.store.state.cards, ctx.today()).filter((id) => ctx.data.byId.has(id));
      this.session = { ids, i: 0, attempted: new Set(), flipped: false, done: false, reviewed: 0 };
    }
  },

  render(ctx, el) {
    if (!this.session) this.enter(ctx);
    const s = this.session;
    clear(el);
    el.appendChild(h('div', { class: 'section-head' }, h('h1', { class: 'view-title', text: t('reviewTitle') }), h('p', { class: 'muted', text: t('reviewSub') })));

    if (s.i >= s.ids.length) {
      s.done = true;
      const cards = ctx.store.state.cards;
      const any = Object.keys(cards).length > 0;
      const next = nextDueDay(cards, ctx.today());
      const box = h('div', { class: 'panel done-panel' },
        h('div', { class: 'done-icon', attrs: { 'aria-hidden': 'true' }, text: any ? '✓' : '🧭' }),
        h('h2', { text: s.reviewed ? t('reviewSessionDone', { n: formatNumber(s.reviewed) }) : (any ? t('reviewDone') : t('navReview')) }),
        h('p', { class: 'muted', text: any ? t('reviewDoneSub') : t('reviewNothingYet') }),
        next !== null ? h('p', { text: t('reviewNextDue', { date: formatDate(dayToDate(next)) }) }) : null,
        h('a', { class: 'btn btn-primary', href: buildUrl('learn'), dataset: { route: 'learn' }, text: t('continueLearning') }));
      el.appendChild(box);
      return;
    }

    const w = ctx.data.byId.get(s.ids[s.i]);
    this.current = w;
    el.append(
      h('div', { class: 'flashcard-controls-top' },
        h('span', { class: 'flashcard-progress-text', attrs: { role: 'status' }, text: t('reviewRemaining', { n: formatNumber(s.ids.length - s.i) }) }),
        h('span', { class: 'flashcard-streak-badge', text: t('boxLabel', { n: formatNumber((ctx.store.state.cards[w.id] || { b: 1 }).b) }) })),
      flashcard(ctx, w, {
        flipped: s.flipped,
        expanded: this.expanded,
        onFlip: () => this.flip(ctx, el),
        onGrade: (g) => this.gradeCurrent(ctx, el, g),
      }),
      h('div', { class: 'flashcard-nav-bar' },
        s.flipped ? null : h('button', { type: 'button', class: 'btn btn-primary', on: { click: () => this.flip(ctx, el) } }, h('span', { text: t('fcShowAnswer') }))),
      h('p', { class: 'kbd-hint', text: t('fcKeyboard') }));
  },

  flip(ctx, el) {
    this.session.flipped = !this.session.flipped;
    this.render(ctx, el);
    el.querySelector(this.session.flipped ? '.grade-good' : '.flashcard-perspective-box')?.focus();
  },

  gradeCurrent(ctx, el, g) {
    const s = this.session;
    const w = this.current;
    if (!w || !s.flipped) return;
    const first = !s.attempted.has(w.id);
    s.attempted.add(w.id);
    ctx.gradeWord(w.id, g, first);
    if (g === 'again') s.ids.push(w.id); else s.reviewed++;
    s.i++;
    s.flipped = false;
    this.render(ctx, el);
    el.querySelector('.flashcard-perspective-box, .done-panel a')?.focus();
  },

  onKey(ctx, el, e) {
    if (!this.session || this.session.i >= this.session.ids.length) return;
    if (e.code === 'Space' || e.key === ' ') { e.preventDefault(); this.flip(ctx, el); }
    else if (GRADE_KEYS[e.key] && this.session.flipped) this.gradeCurrent(ctx, el, GRADE_KEYS[e.key]);
  },
};

