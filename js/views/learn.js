// Learn path: the curriculum in order (chapter → section → lesson of ~5 words).
// A lesson = teach cards, then a short check quiz; finishing it schedules the words for review.
import { h, clear, arabic } from '../dom.js';
import { t, formatNumber, pick, getLang, langInfo } from '../i18n.js';
import { buildQuestion, shuffle } from '../quiz.js';
import { audioButton, catBadge, meaningText, play, verbFormsLine, verseToggle } from '../components.js';
import { renderQuestion } from './quizView.js';
import { celebrate } from '../ui.js';
import { stopAudio } from '../audio.js';

export function lessonName(l) {
  return `${t('chapterLabel', { n: formatNumber(l.ch) })} · ${t('sectionLabel', { n: formatNumber(l.sec) })} · ${t('lessonLabel', { n: formatNumber(l.les) })}`;
}

export function nextLesson(ctx) {
  const done = ctx.store.state.lessons;
  return ctx.data.lessons.find((l) => !done[l.key]) || null;
}

export const learnView = {
  run: null, // {lesson, phase, i, queue, first:Map, xp}
  expanded: new Set(),
  openChapters: new Set(),
  cleanup: null,

  start(ctx, lesson) {
    this.run = { lesson, phase: 'teach', i: 0, queue: [], qi: 0, first: new Map(), xp: 0 };
    ctx.rerender();
    document.getElementById('main')?.focus();
  },

  render(ctx, el) {
    if (this.cleanup) { this.cleanup(); this.cleanup = null; }
    clear(el);
    if (!this.run) this.renderPath(ctx, el);
    else if (this.run.phase === 'teach') this.renderTeach(ctx, el);
    else if (this.run.phase === 'check') this.renderCheck(ctx, el);
    else this.renderDone(ctx, el);
  },

  renderPath(ctx, el) {
    const done = ctx.store.state.lessons;
    const next = nextLesson(ctx);
    el.appendChild(h('div', { class: 'section-head' }, h('h1', { class: 'view-title', text: t('learnTitle') }), h('p', { class: 'muted', text: t('learnSub') })));
    if (next) {
      el.appendChild(h('div', { class: 'panel continue-panel' },
        h('div', {},
          h('p', { class: 'continue-name', text: lessonName(next) }),
          h('p', { class: 'continue-words', attrs: { lang: 'ar', dir: 'rtl' }, text: next.words.map((w) => w.ar).join(' · ') })),
        h('button', { type: 'button', class: 'btn btn-primary btn-lg', on: { click: () => this.start(ctx, next) } }, h('span', { text: t('continueLesson', { lesson: t('lessonLabel', { n: formatNumber(next.les) }) }) }))));
      if (!this.openChapters.size) this.openChapters.add(next.ch);
    } else {
      el.appendChild(h('div', { class: 'panel done-panel' }, h('p', { text: t('allLessonsDone') })));
    }

    const byChapter = new Map();
    for (const l of ctx.data.lessons) {
      if (!byChapter.has(l.ch)) byChapter.set(l.ch, []);
      byChapter.get(l.ch).push(l);
    }
    const list = h('div', { class: 'chapter-list' });
    for (const [ch, lessons] of byChapter) {
      const meta = ctx.data.chapter(ch);
      const nDone = lessons.filter((l) => done[l.key]).length;
      const details = h('details', { class: 'chapter-item', open: this.openChapters.has(ch) });
      const body = h('div', { class: 'chapter-body' });
      const fill = () => {
        if (body.childElementCount) return;
        const bySec = new Map();
        for (const l of lessons) { if (!bySec.has(l.sec)) bySec.set(l.sec, []); bySec.get(l.sec).push(l); }
        for (const [sec, ls] of bySec) {
          body.appendChild(h('section', { class: 'section-block' },
            h('h3', { class: 'section-title', text: t('sectionLabel', { n: formatNumber(sec) }) }),
            h('ul', { class: 'lesson-grid' }, ...ls.map((l) => h('li', {},
              h('button', { type: 'button', class: `lesson-btn${done[l.key] ? ' is-done' : ''}${next && next.key === l.key ? ' is-next' : ''}`, on: { click: () => this.start(ctx, l) } },
                h('span', { class: 'lesson-btn-title' }, h('span', { text: t('lessonLabel', { n: formatNumber(l.les) }) }), done[l.key] ? h('span', { class: 'lesson-check', attrs: { 'aria-label': t('lessonDone') }, text: '✓' }) : null),
                h('span', { class: 'lesson-btn-words', attrs: { lang: 'ar', dir: 'rtl' }, text: l.words.map((w) => w.ar).join(' ') })))))));
        }
      };
      details.addEventListener('toggle', () => {
        if (details.open) { this.openChapters.add(ch); fill(); } else this.openChapters.delete(ch);
      });
      details.append(
        h('summary', { class: 'chapter-summary' },
          h('span', { class: 'chapter-name' }, h('span', { class: 'chapter-num', text: t('chapterLabel', { n: formatNumber(ch) }) }), h('span', { attrs: { lang: langInfo().bcp47 }, text: meta ? pick(meta.title) : '' })),
          h('span', { class: 'chapter-progress' },
            h('progress', { max: lessons.length, value: nDone, attrs: { 'aria-label': t('lessonsDone', { done: formatNumber(nDone), total: formatNumber(lessons.length) }) } }),
            h('span', { class: 'muted', text: t('lessonsDone', { done: formatNumber(nDone), total: formatNumber(lessons.length) }) }))),
        body);
      if (details.open) fill();
      list.appendChild(details);
    }
    el.appendChild(list);
  },

  header(ctx, step) {
    return h('div', { class: 'lesson-head' },
      h('div', {}, h('p', { class: 'muted lesson-name', text: lessonName(this.run.lesson) }), h('p', { class: 'lesson-step', attrs: { role: 'status' }, text: step })),
      h('button', { type: 'button', class: 'btn btn-sm', on: { click: () => { this.run = null; stopAudio(); ctx.rerender(); } } }, h('span', { text: t('exitLesson') })));
  },

  renderTeach(ctx, el) {
    const r = this.run;
    const words = r.lesson.words;
    const w = words[r.i];
    const info = langInfo(getLang());
    const dots = h('ol', { class: 'step-dots', attrs: { 'aria-hidden': 'true' } }, ...words.map((_, i) => h('li', { class: i < r.i ? 'done' : i === r.i ? 'current' : '' })));
    el.append(
      this.header(ctx, t('teachStep', { i: formatNumber(r.i + 1), n: formatNumber(words.length) })),
      dots,
      h('article', { class: 'panel teach-card' },
        h('button', { type: 'button', class: 'teach-arabic', attrs: { lang: 'ar', dir: 'rtl', 'aria-label': `${t('playAudio')}: ${w.ar}` }, text: w.ar, on: { click: () => play(w) } }),
        h('p', { class: 'card-translit', text: w.tl }),
        h('p', { class: 'teach-meaning', attrs: { lang: info.bcp47 }, text: meaningText(w) }),
        h('div', { class: 'card-tags-row centered' }, catBadge(w),
          w.rt ? h('span', { class: 'tag-root static' }, arabic(w.rt)) : null,
          h('span', { class: 'card-occ-pill', text: t('occurrences', { n: formatNumber(w.occ) }) })),
        w.pos ? h('p', { class: 'card-pos-detail', attrs: { lang: 'en' }, text: w.pos }) : null,
        verbFormsLine(w),
        audioButton(w, { cls: 'btn' }),
        w.ref ? verseToggle(ctx, w, this.expanded) : null),
      h('div', { class: 'flashcard-nav-bar' },
        h('button', { type: 'button', class: 'btn', disabled: r.i === 0, on: { click: () => { r.i--; ctx.rerender(); } } }, h('span', { text: t('back') })),
        r.i < words.length - 1
          ? h('button', { type: 'button', class: 'btn btn-primary', attrs: { autofocus: true }, on: { click: () => { r.i++; ctx.rerender(); el.querySelector('.btn-primary')?.focus(); } } }, h('span', { text: t('fcNext') }))
          : h('button', { type: 'button', class: 'btn btn-primary', on: { click: () => this.beginCheck(ctx) } }, h('span', { text: t('startCheck') }))));
  },

  beginCheck(ctx) {
    const r = this.run;
    ctx.store.introduce(r.lesson.words.map((w) => w.id), ctx.today());
    r.phase = 'check';
    r.queue = shuffle(r.lesson.words.map((w) => w.id));
    r.qi = 0;
    r.question = null;
    ctx.rerender();
  },

  renderCheck(ctx, el) {
    const r = this.run;
    if (r.qi >= r.queue.length) { this.finish(ctx); return; }
    const w = ctx.data.byId.get(r.queue[r.qi]);
    if (!r.question || r.question.target.id !== w.id || r.question.answered) {
      r.question = buildQuestion('ar2m', w, ctx.data.words, { lang: getLang() });
    }
    const q = r.question;
    const holder = h('div', { class: 'quiz-box' });
    el.append(this.header(ctx, t('quizStep', { i: formatNumber(r.qi + 1), n: formatNumber(r.queue.length) })), holder);
    this.cleanup = renderQuestion(holder, q, {
      onAnswer: (ok) => {
        q.answered = true;
        if (!r.first.has(w.id)) {
          r.first.set(w.id, ok);
          const ev = ctx.answerLesson(w.id, ok);
          r.xp += ev.xp;
        }
        if (!ok) r.queue.push(w.id); // ask again later in the lesson (no XP the second time)
      },
      onNext: () => { r.qi++; r.question = null; ctx.rerender(); el.querySelector('.quiz-option-btn')?.focus(); },
    });
  },

  finish(ctx) {
    const r = this.run;
    r.phase = 'done';
    ctx.store.completeLesson(r.lesson.key, ctx.today());
    ctx.rerender();
  },

  renderDone(ctx, el) {
    const r = this.run;
    const next = nextLesson(ctx);
    const panel = h('div', { class: 'panel done-panel lesson-done' },
      h('div', { class: 'done-icon', attrs: { 'aria-hidden': 'true' }, text: '✓' }),
      h('h1', { class: 'view-title', text: t('lessonComplete') }),
      h('p', { text: t('lessonCompleteSub', { n: formatNumber(r.lesson.words.length) }) }),
      h('p', { class: 'xp-gain', text: t('xpEarned', { n: formatNumber(r.xp) }) }),
      h('ul', { class: 'done-words' }, ...r.lesson.words.map((w) => h('li', {}, arabic(w.ar), ' — ', h('span', { text: pick(w.m) })))),
      h('div', { class: 'flashcard-nav-bar' },
        h('button', { type: 'button', class: 'btn', on: { click: () => { this.run = null; ctx.rerender(); } } }, h('span', { text: t('backToPath') })),
        next ? h('button', { type: 'button', class: 'btn btn-primary', attrs: { autofocus: true }, on: { click: () => this.start(ctx, next) } }, h('span', { text: t('nextLesson') })) : null));
    el.appendChild(panel);
    if (!r.celebrated) { r.celebrated = true; celebrate(panel); }
    panel.querySelector('[autofocus]')?.focus();
  },

  leave() {
    if (this.cleanup) { this.cleanup(); this.cleanup = null; }
    stopAudio();
  },
};
