// QuranicWords web app — orchestrator: boot, routing, header chrome, filters and shared actions.
import { APP_VERSION, BASE } from './config.js';
import { h, clear, isTypingTarget, arabic } from './dom.js';
import { LANGUAGES, normalizeLang, setLang, getLang, langInfo, t, formatNumber, formatPercent, pick } from './i18n.js';
import { readJSON, writeJSON, readString, writeString } from './storage.js';
import { DataStore } from './data.js';
import { filterWords, skeleton } from './search.js';
import { parseRoute, buildUrl, setBase, VIEWS } from './router.js';

setBase(BASE);
import { dayNumber, dueIds } from './srs.js';
import { ProgressStore, levelForXp, xpForLevel, currentStreak, knownFraction } from './progress.js';
import { toast, initModal, modalOpen, closeModal, celebrate } from './ui.js';
import { cardsView, tableView, rootsView } from './views/dictionary.js';
import { flashcardsView, reviewView } from './views/study.js';
import { quizView } from './views/quizView.js';
import { learnView, nextLesson } from './views/learn.js';
import { progressView } from './views/progressView.js';

const VIEW_IMPL = { learn: learnView, review: reviewView, cards: cardsView, table: tableView, flashcards: flashcardsView, quiz: quizView, roots: rootsView, progress: progressView };
const NAV_KEY = { learn: 'navLearn', review: 'navReview', cards: 'navDictionary', table: 'navTable', flashcards: 'navFlashcards', quiz: 'navQuiz', roots: 'navRoots', progress: 'navProgress' };
const BOOKMARKS_KEY = 'qw_bookmarks';

const $ = (id) => document.getElementById(id);

class App {
  constructor() {
    this.data = new DataStore();
    this.store = new ProgressStore();
    this.state = { view: 'cards', q: '', ch: null, cat: null, root: null, sort: 'curriculum', saved: false };
    this.filterVersion = 0;
    this.memo = { key: null, list: [] };
    const saved = readJSON(BOOKMARKS_KEY, []);
    this.bookmarks = new Set(Array.isArray(saved) ? saved.filter((x) => typeof x === 'string') : []);
    this.savedVersion = 0;
    this.ready = false;
    this.currentView = null;
    this.ctx = this.makeContext();
  }

  /* ---------------- Context handed to views ---------------- */
  makeContext() {
    const app = this;
    return {
      data: this.data,
      store: this.store,
      get filterVersion() { return app.filterVersion; },
      today: () => dayNumber(),
      filtered: () => app.filtered(),
      skeleton,
      rerender: () => app.renderView(),
      navigate: (v) => app.navigate(v),
      isSaved: (id) => app.bookmarks.has(id),
      toggleSaved: (id) => app.toggleSaved(id),
      copyWord: (w) => app.copyWord(w),
      filterByRoot: (root) => app.filterByRoot(root),
      clearFilters: () => app.clearFilters(),
      practiceMistakes: () => { quizView.pool = 'mistakes'; quizView.question = null; app.navigate('quiz'); },
      gradeWord: (id, g, firstTry) => app.gradeWord(id, g, firstTry),
      answerQuiz: (id, ok) => app.answerQuiz(id, ok),
      answerLesson: (id, ok) => app.answerLesson(id, ok),
    };
  }

  /* ---------------- Boot ---------------- */
  async init() {
    const route = parseRoute(location.pathname, location.search);
    const lang = normalizeLang(route.lang) || normalizeLang(readString('qw_lang')) || normalizeLang(navigator.language) || 'en';
    if (route.lang && normalizeLang(route.lang)) writeString('qw_lang', lang);
    this.applyRoute(route);
    this.setLanguage(lang, false);
    this.bindChrome();
    initModal();
    this.store.onChange(() => this.renderChrome());
    this.registerServiceWorker();
    await this.loadData();
  }

  async loadData() {
    const status = $('app-status');
    status.hidden = false;
    status.className = 'app-status';
    clear(status).append(h('div', { class: 'spinner', attrs: { 'aria-hidden': 'true' } }), h('p', { text: t('loadingData') }));
    try {
      await this.data.load();
      this.ready = true;
      this.renderFilters();
      this.renderHero();
      this.renderAbout();
      this.renderChrome();
      this.showView(this.state.view);
      status.hidden = true;
    } catch (e) {
      console.error(e);
      this.ready = false;
      status.hidden = false;
      status.className = 'app-status is-error';
      clear(status).append(h('p', { attrs: { role: 'alert' }, text: t('loadError') }),
        h('button', { type: 'button', class: 'btn btn-primary', text: t('retry'), on: { click: () => this.loadData() } }));
    }
  }

  /* ---------------- Language & theme ---------------- */
  setLanguage(code, announce = true) {
    const lang = setLang(code);
    const info = langInfo(lang);
    document.documentElement.lang = info.bcp47;
    document.documentElement.dir = info.dir;
    writeString('qw_lang', lang);
    this.applyStaticI18n();
    const sel = $('lang-select');
    if (sel && !sel.options.length) {
      for (const l of LANGUAGES) sel.appendChild(h('option', { value: l.code, text: l.name, attrs: { lang: l.bcp47 } }));
    }
    if (sel) sel.value = lang;
    this.memo.key = null;
    if (this.ready) {
      const render = () => {
        this.memo.key = null;
        this.renderFilters();
        this.renderHero();
        this.renderChrome();
        this.renderView();
        this.updateTitle();
      };
      // Meanings are fetched per language; render once this language's file is in.
      if (this.data.hasLanguage(lang)) render();
      else this.data.ensureLanguage(lang).then(render).catch(() => toast(t('loadError')));
    }
    if (announce) toast(t('langSwitched', { lang: info.name }));
  }

  applyStaticI18n() {
    for (const el of document.querySelectorAll('[data-i18n]')) el.textContent = t(el.dataset.i18n);
    for (const el of document.querySelectorAll('[data-i18n-attr]')) {
      for (const pair of el.dataset.i18nAttr.split(';')) {
        const [attr, key] = pair.split(':');
        if (attr && key) el.setAttribute(attr.trim(), t(key.trim()));
      }
    }
  }

  toggleTheme() {
    const next = document.documentElement.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', next);
    writeString('qw_theme', next);
    this.updateThemeIcon();
  }

  updateThemeIcon() {
    const light = document.documentElement.getAttribute('data-theme') === 'light';
    $('theme-icon').textContent = light ? '☾' : '☀';
    $('theme-toggle').setAttribute('aria-pressed', String(!light));
  }

  /* ---------------- Chrome bindings ---------------- */
  bindChrome() {
    $('lang-select').addEventListener('change', (e) => this.setLanguage(e.target.value));
    $('theme-toggle').addEventListener('click', () => this.toggleTheme());
    this.updateThemeIcon();

    // Internal links: <a data-route> anywhere (nav, brand, buttons rendered by views).
    document.addEventListener('click', (e) => {
      const a = e.target.closest('a[data-route]');
      if (!a || e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      e.preventDefault();
      this.navigate(a.dataset.route);
    });
    window.addEventListener('popstate', () => {
      this.applyRoute(parseRoute(location.pathname, location.search));
      if (this.ready) { this.renderFilters(); this.showView(this.state.view, { focus: false }); }
    });

    // Filters
    const input = $('search-input');
    let debounce = null;
    input.addEventListener('input', () => {
      $('search-clear').hidden = !input.value;
      clearTimeout(debounce);
      debounce = setTimeout(() => this.setFilter({ q: input.value.trim() }), 150);
    });
    input.addEventListener('keydown', (e) => { if (e.key === 'Escape' && input.value) { e.stopPropagation(); input.value = ''; this.setFilter({ q: '' }); $('search-clear').hidden = true; } });
    $('search-clear').addEventListener('click', () => { input.value = ''; $('search-clear').hidden = true; this.setFilter({ q: '' }); input.focus(); });
    $('chapter-filter').addEventListener('change', (e) => this.setFilter({ ch: e.target.value ? Number(e.target.value) : null }));
    $('cat-filter').addEventListener('change', (e) => this.setFilter({ cat: e.target.value || null }));
    $('sort-filter').addEventListener('change', (e) => this.setFilter({ sort: e.target.value }));
    $('saved-toggle').addEventListener('click', () => this.setFilter({ saved: !this.state.saved }));
    $('random-btn').addEventListener('click', () => this.randomWord());

    document.addEventListener('keydown', (e) => this.onKey(e));
  }

  onKey(e) {
    if (e.defaultPrevented || e.ctrlKey || e.metaKey || e.altKey) return;
    if (modalOpen()) return; // the dialog handles its own keys (Esc, focus trap)
    if (isTypingTarget(e.target)) return; // never steal keys from text fields / selects
    if (e.key === '/' && !$('filters').hidden) { e.preventDefault(); $('search-input').focus(); return; }
    // Let Space/Enter activate the focused button, link or summary natively.
    const interactive = e.target.closest && e.target.closest('button, a, summary, [role="button"]');
    if (interactive && (e.key === ' ' || e.key === 'Enter') && !interactive.classList.contains('flashcard-perspective-box')) return;
    if (interactive && interactive.classList.contains('flashcard-perspective-box') && e.key === 'Enter') return;
    const view = VIEW_IMPL[this.state.view];
    if (this.ready && view && view.onKey) view.onKey(this.ctx, $(`view-${this.state.view}`), e);
  }

  /* ---------------- Routing ---------------- */
  applyRoute(r) {
    this.state.view = VIEWS.includes(r.view) ? r.view : 'cards';
    const f = { q: r.q, ch: r.ch, cat: r.cat, root: r.root, sort: r.sort, saved: r.saved };
    const changed = ['q', 'ch', 'cat', 'root', 'sort', 'saved'].some((k) => this.state[k] !== f[k]);
    Object.assign(this.state, f);
    if (changed) this.filterVersion++;
  }

  navigate(view, { push = true } = {}) {
    if (!VIEWS.includes(view)) view = 'cards';
    if (modalOpen()) closeModal();
    const url = buildUrl(view, this.state);
    if (push && url !== location.pathname + location.search) history.pushState({ view }, '', url);
    this.state.view = view;
    if (this.ready) this.showView(view);
  }

  showView(view, { focus = true } = {}) {
    const prev = this.currentView;
    if (prev && prev !== view && VIEW_IMPL[prev].leave) VIEW_IMPL[prev].leave();
    this.currentView = view;
    for (const sec of document.querySelectorAll('section.view')) sec.hidden = sec.dataset.view !== view;
    for (const el of document.querySelectorAll('[data-views]')) el.hidden = !el.dataset.views.split(' ').includes(view);
    for (const a of document.querySelectorAll('.nav-link')) {
      if (a.dataset.route === view) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
    }
    const impl = VIEW_IMPL[view];
    if (prev !== view && impl.enter) impl.enter(this.ctx);
    this.renderView();
    this.updateTitle();
    if (focus && prev && prev !== view) {
      window.scrollTo({ top: 0 });
      $('main').focus({ preventScroll: true });
    }
  }

  renderView() {
    if (!this.ready) return;
    const view = this.state.view;
    const el = $(`view-${view}`);
    VIEW_IMPL[view].render(this.ctx, el);
    this.renderResultCount();
  }

  updateTitle() {
    document.title = `${t(NAV_KEY[this.state.view])} — QuranicWords`;
  }

  /* ---------------- Filters ---------------- */
  filtered() {
    const s = this.state;
    const key = JSON.stringify([s.q, s.ch, s.cat, s.root, s.sort, s.saved, getLang(), s.saved ? this.savedVersion : 0]);
    if (this.memo.key !== key) {
      this.memo = { key, list: filterWords(this.data.words, { ...s, savedIds: this.bookmarks, lang: getLang() }) };
    }
    return this.memo.list;
  }

  setFilter(patch) {
    Object.assign(this.state, patch);
    this.filterVersion++;
    history.replaceState(history.state, '', buildUrl(this.state.view, this.state));
    this.renderFilters();
    this.renderView();
  }

  clearFilters() {
    $('search-input').value = '';
    $('search-clear').hidden = true;
    this.setFilter({ q: '', ch: null, cat: null, root: null, saved: false });
  }

  filterByRoot(root) {
    $('search-input').value = '';
    Object.assign(this.state, { root, q: '', ch: null, cat: null, saved: false });
    this.filterVersion++;
    this.renderFilters();
    if (this.state.view === 'cards' || this.state.view === 'table') {
      history.replaceState(history.state, '', buildUrl(this.state.view, this.state));
      this.renderView();
      window.scrollTo({ top: 0 });
    } else this.navigate('cards');
  }

  renderFilters() {
    if (!this.ready) return;
    const s = this.state;
    const input = $('search-input');
    if (document.activeElement !== input) input.value = s.q;
    $('search-clear').hidden = !input.value;

    const ch = $('chapter-filter');
    ch.replaceChildren(h('option', { value: '', text: t('allChapters') }),
      ...this.data.meta.chapters.map((c) => h('option', { value: String(c.n), text: `${t('chapterLabel', { n: formatNumber(c.n) })}: ${pick(c.title)}` })));
    ch.value = s.ch ? String(s.ch) : '';

    const cat = $('cat-filter');
    cat.replaceChildren(h('option', { value: '', text: t('allCats') }),
      ...['NOUN', 'VERB', 'PARTICLE'].map((c) => h('option', { value: c, text: t(`cat${c}`) })));
    cat.value = s.cat || '';

    const sort = $('sort-filter');
    sort.replaceChildren(...[['curriculum', 'sortCurriculum'], ['freq_desc', 'sortFreqDesc'], ['freq_asc', 'sortFreqAsc'], ['alpha_ar', 'sortAlphaAr'], ['alpha_meaning', 'sortAlphaMeaning']]
      .map(([v, k]) => h('option', { value: v, text: t(k) })));
    sort.value = s.sort;

    const savedBtn = $('saved-toggle');
    savedBtn.setAttribute('aria-pressed', String(s.saved));
    savedBtn.classList.toggle('btn-gold', s.saved);

    const chip = clear($('root-chip-wrap'));
    if (s.root) {
      chip.appendChild(h('span', { class: 'root-chip' }, h('span', { text: t('rootChip', { root: '' }) }), arabic(s.root),
        h('button', { type: 'button', class: 'chip-remove', attrs: { 'aria-label': t('removeRootFilter') }, text: '✕', on: { click: () => this.setFilter({ root: null }) } })));
    }
  }

  renderResultCount() {
    const el = $('result-count');
    if (!el || $('filters').hidden) return;
    el.textContent = t('wordsFound', { count: formatNumber(this.filtered().length) });
  }

  randomWord() {
    const words = this.data.words;
    if (!words.length) return;
    const w = words[Math.floor(Math.random() * words.length)];
    $('search-input').value = w.ar;
    this.setFilter({ q: w.ar, ch: null, cat: null, root: null, saved: false });
    if (this.state.view !== 'cards') this.navigate('cards');
    toast(t('randomToast', { word: w.ar }));
  }

  /* ---------------- Saved words / clipboard ---------------- */
  toggleSaved(id) {
    const now = !this.bookmarks.has(id);
    if (now) this.bookmarks.add(id); else this.bookmarks.delete(id);
    this.savedVersion++;
    writeJSON(BOOKMARKS_KEY, [...this.bookmarks]);
    toast(now ? t('savedToast') : t('removedToast'));
    return now;
  }

  copyWord(w) {
    const text = `${w.ar} — ${pick(w.m)}`;
    const fail = () => toast(t('copyFailed'), { tone: 'error' });
    try {
      if (!navigator.clipboard || !navigator.clipboard.writeText) { fail(); return; }
      navigator.clipboard.writeText(text).then(() => toast(t('copied', { word: w.ar })), fail);
    } catch {
      fail();
    }
  }

  /* ---------------- Answers, XP and celebrations ---------------- */
  handleEvents(ev) {
    if (!ev) return;
    if (ev.levelUp) { toast(t('levelUp', { n: formatNumber(ev.levelUp) }), { tone: 'success' }); celebrate($('ps-level')); }
    if (ev.milestone) { toast(t('streakMilestone', { n: formatNumber(ev.milestone) }), { tone: 'success' }); celebrate($('ps-streak')); }
    if (ev.goalReached) { toast(t('goalReached'), { tone: 'success' }); celebrate($('ps-goal')); }
  }

  /** Flashcard / review grading. XP only on the first attempt for the word in this session. */
  gradeWord(id, g, firstTry) {
    const card = this.store.grade(id, g, dayNumber());
    if (firstTry) this.handleEvents(this.store.answer(id, g !== 'again', dayNumber()));
    return card;
  }

  /** Quiz view answer (always a first try). Words already in review are re-scheduled. */
  answerQuiz(id, ok) {
    const ev = this.store.answer(id, ok, dayNumber());
    if (this.store.state.cards[id]) this.store.grade(id, ok ? 'good' : 'again', dayNumber());
    this.handleEvents(ev);
    return ev;
  }

  /** Lesson check: first-try answers grade the freshly introduced word. */
  answerLesson(id, ok) {
    const ev = this.store.answer(id, ok, dayNumber());
    this.store.grade(id, ok ? 'good' : 'again', dayNumber());
    this.handleEvents(ev);
    return ev;
  }

  /* ---------------- Hero, about, progress strip ---------------- */
  renderHero() {
    const m = this.data.meta;
    const stats = [
      [formatNumber(m.wordCount), 'statWords', 'primary'],
      [formatNumber(m.occurrences), 'statOccurrences', 'gold'],
      [formatNumber(m.rootCount), 'statRoots', 'light'],
      [formatNumber(m.chapters.length), 'statChapters', ''],
      [formatNumber(m.languages.length), 'statLanguages', ''],
      [formatPercent(m.coveragePercent / 100), 'statCoverage', 'primary'],
    ];
    $('stats-ribbon').replaceChildren(...stats.map(([v, k, cls]) => h('div', { class: 'stat-chip' },
      h('dt', { class: 'stat-lbl', text: t(k) }), h('dd', { class: `stat-val ${cls}`, text: v }))));
    const started = Object.keys(this.store.state.lessons).length > 0;
    $('hero-cta').textContent = started ? t('continueLearning') : t('startLearning');
  }

  renderAbout() {
    const m = this.data.meta;
    const values = {
      wordCount: formatNumber(m.wordCount, 'en'),
      rootCount: formatNumber(m.rootCount, 'en'),
      occurrences: formatNumber(m.occurrences, 'en'),
      coveragePercent: formatPercent(m.coveragePercent / 100, 'en'),
    };
    for (const el of document.querySelectorAll('[data-stat]')) el.textContent = values[el.dataset.stat] ?? '';
    const catName = { NOUN: 'Noun', VERB: 'Verb', PARTICLE: 'Particle' };
    $('about-top').replaceChildren(...this.data.words.slice(0, 10).map((w) => h('tr', {},
      h('td', {}, w.u ? h('a', { attrs: { href: `${BASE}quran-words/${w.u}.html` } }, arabic(w.ar, 'seo-ar-word')) : arabic(w.ar, 'seo-ar-word')), h('td', { text: catName[w.cat] }),
      h('td', { text: formatNumber(w.occ, 'en') }), h('td', { text: (w.m && w.m.en) || '' }))));
  }

  renderChrome() {
    if (!this.ready) return;
    const s = this.store.state;
    const today = dayNumber();
    const level = levelForXp(s.xp);
    const lo = xpForLevel(level);
    const hi = xpForLevel(level + 1);
    const streak = currentStreak(s, today);
    const todayXp = s.daily[today] || 0;
    const goalFrac = Math.min(1, todayXp / s.goal);
    const known = knownFraction(s.cards, this.data.byId, this.data.meta.occurrences);

    const strip = $('progress-strip');
    strip.hidden = false;
    const ring = ringSvg(goalFrac);
    strip.replaceChildren(
      h('a', { class: 'ps-item ps-level', id: 'ps-level', href: buildUrl('progress'), dataset: { route: 'progress' }, attrs: { 'aria-label': `${t('levelLabel', { n: formatNumber(level) })}, ${t('xpProgress', { xp: formatNumber(s.xp), next: formatNumber(hi) })}` } },
        h('span', { class: 'ps-level-badge', text: t('levelShort', { n: formatNumber(level) }) }),
        h('span', { class: 'ps-xp' },
          h('progress', { max: hi - lo, value: s.xp - lo, attrs: { 'aria-hidden': 'true' } }),
          h('span', { class: 'ps-xp-text', attrs: { 'aria-hidden': 'true' }, text: t('xpProgress', { xp: formatNumber(s.xp), next: formatNumber(hi) }) }))),
      h('span', { class: `ps-item ps-streak${streak ? ' on' : ''}`, id: 'ps-streak', attrs: { role: 'img', 'aria-label': t('streakDays', { n: formatNumber(streak) }) } },
        h('span', { attrs: { 'aria-hidden': 'true' }, text: '🔥' }), h('span', { attrs: { 'aria-hidden': 'true' }, text: formatNumber(streak) })),
      h('span', { class: `ps-item ps-goal${goalFrac >= 1 ? ' met' : ''}`, id: 'ps-goal', attrs: { role: 'img', 'aria-label': t('goalToday', { xp: formatNumber(todayXp), goal: formatNumber(s.goal) }) } },
        ring, h('span', { attrs: { 'aria-hidden': 'true' }, text: `${formatNumber(todayXp)}/${formatNumber(s.goal)}` })),
      h('span', { class: 'ps-item ps-known', attrs: { title: t('knownLong', { pct: formatPercent(known) }) } },
        h('span', { class: 'visually-hidden', text: t('knownLong', { pct: formatPercent(known) }) }),
        h('span', { attrs: { 'aria-hidden': 'true' }, text: t('knownShort', { pct: formatPercent(known) }) })));

    const due = dueIds(s.cards, today).length;
    const badge = $('review-badge');
    badge.hidden = due === 0;
    badge.textContent = formatNumber(due);
    badge.setAttribute('aria-label', t('reviewDueBadge', { n: formatNumber(due) }));
    const started = Object.keys(s.lessons).length > 0;
    const cta = $('hero-cta');
    if (cta) cta.textContent = started && nextLesson(this.ctx) ? t('continueLearning') : t('startLearning');
  }

  /* ---------------- Service worker ---------------- */
  registerServiceWorker() {
    if (!('serviceWorker' in navigator) || location.protocol === 'file:') return;
    const hadController = Boolean(navigator.serviceWorker.controller);
    let reloading = false;
    navigator.serviceWorker.addEventListener('controllerchange', () => {
      if (!hadController) { toast(t('offlineReady')); return; }
      // A new version has taken over: reload once so the page never runs old code against
      // new data (the new worker has already cached the new files).
      if (reloading) return;
      reloading = true;
      location.reload();
    });
    window.addEventListener('load', () => {
      navigator.serviceWorker.register(`${BASE}sw.js`, { scope: BASE }).catch((e) => console.warn('Service worker registration failed', e));
    });
  }
}

function ringSvg(frac) {
  const NS = 'http://www.w3.org/2000/svg';
  const r = 9;
  const c = 2 * Math.PI * r;
  const svg = document.createElementNS(NS, 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('class', 'goal-ring');
  svg.setAttribute('aria-hidden', 'true');
  const bg = document.createElementNS(NS, 'circle');
  for (const [k, v] of Object.entries({ cx: 12, cy: 12, r, class: 'goal-ring-bg' })) bg.setAttribute(k, v);
  const fg = document.createElementNS(NS, 'circle');
  for (const [k, v] of Object.entries({ cx: 12, cy: 12, r, class: 'goal-ring-fg', 'stroke-dasharray': `${c.toFixed(2)}`, 'stroke-dashoffset': `${(c * (1 - frac)).toFixed(2)}`, transform: 'rotate(-90 12 12)' })) fg.setAttribute(k, v);
  svg.append(bg, fg);
  return svg;
}

const app = new App();
window.addEventListener('DOMContentLoaded', () => { app.init(); });
// Exposed for debugging in the console only.
window.QuranicWords = { version: APP_VERSION };
