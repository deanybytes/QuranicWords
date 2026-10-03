// Quiz view (4 modes) and a reusable multiple-choice question renderer used by lessons.
import { h, clear, arabic } from '../dom.js';
import { t, formatNumber, getLang, langInfo, pick } from '../i18n.js';
import { buildQuestion, isCorrect, pickTarget, verseBlank, MODES } from '../quiz.js';
import { verseNode, ayahMark } from '../components.js';

const AUTO_ADVANCE_MS = 1300;
const MODE_KEYS = { ar2m: 'modeArToMeaning', m2ar: 'modeMeaningToAr', verse: 'modeVerse' };
const POOLS = { filtered: 'poolFiltered', learned: 'poolLearned', mistakes: 'poolMistakes' };

function optionLabel(q, w) {
  if (q.mode === 'm2ar' || q.mode === 'verse') return arabic(w.ar, 'opt-ar');
  const info = langInfo(getLang());
  return h('span', { attrs: { lang: info.bcp47 }, text: pick(w.m) });
}

function stimulus(q) {
  const w = q.target;
  const info = langInfo(getLang());
  switch (q.mode) {
    case 'ar2m':
      return h('div', { class: 'quiz-stimulus' }, h('p', { class: 'quiz-question-arabic', attrs: { lang: 'ar', dir: 'rtl' }, text: w.ar }));
    case 'm2ar':
      return h('div', { class: 'quiz-stimulus' }, h('p', { class: 'quiz-question-meaning', attrs: { lang: info.bcp47 }, text: pick(w.m) }));
    case 'verse': {
      const v = q.verse;
      const [before, after] = verseBlank(v);
      const ar = h('p', { class: 'card-verse-arabic quiz-verse', attrs: { lang: 'ar', dir: 'rtl' } },
        before, h('span', { class: 'blank', attrs: { role: 'img', 'aria-label': t('blankLabel') }, text: '_____' }), after, ayahMark(v.key));
      // The translation is shown unmarked: marking it would give the answer away.
      const tr = h('p', { class: 'card-verse-trans', attrs: { lang: info.bcp47, dir: info.dir }, text: v.tr || '' });
      return h('div', { class: 'quiz-stimulus' }, ar, tr);
    }
    default: return null;
  }
}

const PROMPTS = { ar2m: 'quizPromptMeaning', m2ar: 'quizPromptArabic', verse: 'quizPromptVerse' };

/**
 * Renders question `q` into `el`. Calls onAnswer(correct) exactly once (first try), then
 * onNext() when the learner moves on. Returns a cleanup function that clears timers.
 */
export function renderQuestion(el, q, { onAnswer, onNext, header = null, autoAdvance = true }) {
  let timer = null;
  let answered = false;
  clear(el);
  const feedback = h('p', { class: 'quiz-feedback', attrs: { role: 'status', 'aria-live': 'polite' } });
  const nextBtn = h('button', { type: 'button', class: 'btn btn-primary', hidden: true, text: t('quizNext'), on: { click: () => { clearTimeout(timer); onNext(); } } });
  const grid = h('div', { class: 'quiz-options-grid', attrs: { role: 'group', 'aria-label': t(PROMPTS[q.mode]) } });
  for (const opt of q.options) {
    const btn = h('button', { type: 'button', class: 'quiz-option-btn', dataset: { id: opt.id } }, optionLabel(q, opt));
    btn.addEventListener('click', () => {
      if (answered) return;
      answered = true;
      const ok = isCorrect(q, opt.id);
      for (const b of grid.querySelectorAll('.quiz-option-btn')) {
        b.setAttribute('aria-disabled', 'true');
        if (b.dataset.id === q.correctId) b.classList.add('correct');
      }
      if (!ok) btn.classList.add('wrong');
      const answerText = q.mode === 'm2ar' || q.mode === 'verse' ? q.target.ar : pick(q.target.m);
      feedback.textContent = ok ? t('quizCorrect') : t('quizWrong', { answer: answerText });
      feedback.className = `quiz-feedback ${ok ? 'ok' : 'bad'}`;
      if (q.mode === 'verse') el.querySelector('.quiz-stimulus')?.replaceWith(h('div', { class: 'quiz-stimulus' }, verseNode(q.verse, q.verse.ref)));
      onAnswer(ok);
      nextBtn.hidden = false;
      if (ok && autoAdvance) timer = setTimeout(onNext, AUTO_ADVANCE_MS);
      else nextBtn.focus();
    });
    grid.appendChild(btn);
  }
  el.append(
    header || '',
    h('p', { class: 'quiz-prompt', text: t(PROMPTS[q.mode]) }),
    stimulus(q),
    grid,
    h('div', { class: 'quiz-footer' }, feedback, nextBtn));
  return () => clearTimeout(timer);
}

export const quizView = {
  mode: 'ar2m',
  pool: 'filtered',
  score: 0,
  total: 0,
  question: null,
  cleanup: null,
  token: 0,
  lastId: null,

  enter() {
    this.question = null;
  },

  poolWords(ctx) {
    let list;
    if (this.pool === 'learned') list = ctx.data.words.filter((w) => ctx.store.state.cards[w.id]);
    else if (this.pool === 'mistakes') list = ctx.data.words.filter((w) => ctx.store.state.mistakes[w.id]);
    else list = ctx.filtered();
    return list;
  },

  render(ctx, el) {
    clear(el);
    const modeSel = h('select', { id: 'quiz-mode', class: 'filter-select', on: { change: (e) => { this.mode = e.target.value; this.next(ctx); } } },
      ...MODES.map((m) => h('option', { value: m, selected: m === this.mode, text: t(MODE_KEYS[m]) })));
    const poolSel = h('select', { id: 'quiz-pool', class: 'filter-select', on: { change: (e) => { this.pool = e.target.value; this.next(ctx); } } },
      ...Object.entries(POOLS).map(([p, k]) => h('option', { value: p, selected: p === this.pool, text: t(k) })));
    this.scoreEl = h('span', { class: 'quiz-score', text: t('quizScore', { score: formatNumber(this.score), total: formatNumber(this.total) }) });
    this.comboEl = h('span', { class: 'quiz-combo', hidden: ctx.store.combo < 2, text: t('quizCombo', { n: formatNumber(ctx.store.combo) }) });
    this.body = h('div', { class: 'quiz-body' });
    el.append(
      h('div', { class: 'quiz-box' },
        h('div', { class: 'quiz-header' }, h('h1', { class: 'view-title', text: t('quizTitle') }), h('div', { class: 'quiz-stats' }, this.comboEl, this.scoreEl)),
        h('div', { class: 'quiz-settings' },
          h('div', { class: 'field' }, h('label', { attrs: { for: 'quiz-mode' }, text: t('quizMode') }), modeSel),
          h('div', { class: 'field' }, h('label', { attrs: { for: 'quiz-pool' }, text: t('quizPool') }), poolSel)),
        this.body));
    if (this.question && this.question.mode === this.mode && !this.question.answered) this.show(ctx, this.question);
    else this.next(ctx);
  },

  async next(ctx, tries = 0) {
    if (this.cleanup) { this.cleanup(); this.cleanup = null; }
    const token = ++this.token;
    const pool = this.poolWords(ctx);
    if (!pool.length) {
      this.question = null;
      this.body.replaceChildren(h('p', { class: 'empty-state', text: t('quizNeedWords') }));
      return;
    }
    const prefer = this.mode === 'verse' ? (w) => ctx.data.hasChapterVerses(w.ch) : null;
    const target = pickTarget(pool, { prefer, avoidId: this.lastId });
    let verse = null;
    if (this.mode === 'verse') {
      this.body.replaceChildren(h('p', { class: 'muted', attrs: { role: 'status' }, text: t('verseLoading') }));
      try {
        verse = await ctx.data.verseFor(target.id);
      } catch {
        if (token !== this.token) return;
        this.body.replaceChildren(h('p', { class: 'error-text', attrs: { role: 'alert' } }, t('verseError'), ' ',
          h('button', { type: 'button', class: 'btn btn-sm', text: t('retry'), on: { click: () => this.next(ctx) } })));
        return;
      }
      if (token !== this.token) return; // user moved on meanwhile
      if (!verse || !verseBlank(verse)) { this.lastId = target.id; if (tries < 5) this.next(ctx, tries + 1); else this.body.replaceChildren(h('p', { class: 'muted', text: t('noVerse') })); return; }
    }
    const q = buildQuestion(this.mode, target, ctx.data.words, { lang: getLang() });
    if (!q) { this.body.replaceChildren(h('p', { class: 'empty-state', text: t('quizNeedWords') })); return; }
    q.verse = verse;
    this.lastId = target.id;
    this.question = q;
    this.show(ctx, q);
  },

  show(ctx, q) {
    this.cleanup = renderQuestion(this.body, q, {
      onAnswer: (ok) => {
        q.answered = true;
        this.total++;
        if (ok) this.score++;
        ctx.answerQuiz(q.target.id, ok);
        this.scoreEl.textContent = t('quizScore', { score: formatNumber(this.score), total: formatNumber(this.total) });
        this.comboEl.hidden = ctx.store.combo < 2;
        this.comboEl.textContent = t('quizCombo', { n: formatNumber(ctx.store.combo) });
      },
      onNext: () => { this.question = null; this.next(ctx); },
    });
  },

  leave() {
    this.token++;
    if (this.cleanup) { this.cleanup(); this.cleanup = null; }
  },
};
