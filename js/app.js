/**
 * QuranicWords — Master Application Orchestrator
 * High Performance SPA Controller with Full Multilingual (i18n) Engine
 */

import { dbService } from './db.js';
import { audioService } from './audio.js';
import { SearchEngine, stripTashkeel } from './search.js';
import { t, I18N_DICTIONARY } from './i18n.js';

class QuranicApp {
  constructor() {
    // Application State
    this.wordsSummary = [];
    this.wordsFull = new Map(); // id -> full word object with verses
    this.metadata = null;
    this.roots = [];
    this.filteredWords = [];
    this.searchEngine = new SearchEngine();

    // UI State
    this.currentLang = localStorage.getItem('qw_lang') || 'en';
    this.currentTheme = localStorage.getItem('qw_theme') || 'dark';
    this.currentView = 'cards'; // 'cards' | 'table' | 'flashcards' | 'roots' | 'quiz'
    this.bookmarks = new Set(JSON.parse(localStorage.getItem('qw_bookmarks') || '[]'));
    
    // Batch Rendering State
    this.renderedCount = 0;
    this.BATCH_SIZE = 24;
    this.observer = null;

    // Flashcard State
    this.fcIndex = 0;
    this.fcFlipped = false;
    this.fcStreak = 0;

    // Quiz State
    this.quizQuestion = null;
    this.quizScore = 0;
    this.quizTotal = 0;
    this.quizAnswered = false;

    // Elements
    this.el = {};
  }

  async init() {
    this.initTheme();
    this.cacheElements();
    this.bindEvents();
    this.initIntersectionObserver();

    try {
      // Step 1: Fast asynchronous load of metadata and summary dataset (<50ms)
      const [meta, summary, roots] = await Promise.all([
        dbService.fetchCached('/data/metadata.json?v=1.0.3', 'meta_v3'),
        dbService.fetchCached('/data/words_summary.json?v=1.0.3', 'words_summary_v3'),
        dbService.fetchCached('/data/roots.json?v=1.0.3', 'roots_v3')
      ]);

      this.metadata = meta;
      this.wordsSummary = summary;
      this.roots = roots;
      this.searchEngine.setWords(summary);

      // Populate UI filters & statistics
      this.renderMetadataStats();
      this.renderRootCloud();

      // Apply initial full internationalization
      this.updateUILanguage(this.currentLang, false);

      // Step 2: Background preload of full verses data
      setTimeout(() => this.preloadFullVerses(), 200);

    } catch (err) {
      console.error('Error initializing QuranicWords app:', err);
      this.showToast('⚠️ Error loading dictionary data. Please check your connection.');
    }
  }

  async preloadFullVerses() {
    try {
      const fullList = await dbService.fetchCached('/data/words.json?v=1.0.3', 'words_full_v3');
      if (Array.isArray(fullList)) {
        for (const w of fullList) {
          this.wordsFull.set(w.id, w);
        }
      }
    } catch (e) {
      console.warn('Background full verses preload notice:', e);
    }
  }

  cacheElements() {
    this.el.wordsGrid = document.getElementById('words-grid');
    this.el.tableBody = document.getElementById('table-body');
    this.el.cardsContainer = document.getElementById('cards-container');
    this.el.tableContainer = document.getElementById('table-container');
    this.el.flashcardContainer = document.getElementById('flashcard-container');
    this.el.rootsContainer = document.getElementById('roots-container');
    this.el.quizContainer = document.getElementById('quiz-container');
    this.el.loadingIndicator = document.getElementById('loading-indicator');
    this.el.emptyState = document.getElementById('empty-state');
    
    // Inputs & Controls
    this.el.searchInput = document.getElementById('search-input');
    this.el.searchClearBtn = document.getElementById('search-clear-btn');
    this.el.chapterFilter = document.getElementById('chapter-filter');
    this.el.posFilter = document.getElementById('pos-filter');
    this.el.sortFilter = document.getElementById('sort-filter');
    this.el.langSelect = document.getElementById('lang-select');
    this.el.themeToggleBtn = document.getElementById('theme-toggle-btn');
    this.el.bookmarksToggleBtn = document.getElementById('bookmarks-toggle-btn');
    this.el.randomWordBtn = document.getElementById('random-word-btn');
    
    // Stats
    this.el.statTotalWords = document.getElementById('stat-total-words');
    this.el.statOccurrences = document.getElementById('stat-occurrences');
    this.el.statRoots = document.getElementById('stat-roots');
    this.el.statFilteredCount = document.getElementById('stat-filtered-count');
    
    // Modal
    this.el.modalBackdrop = document.getElementById('polysemy-modal');
    this.el.modalBody = document.getElementById('modal-body');
    this.el.modalCloseBtn = document.getElementById('modal-close-btn');

    // Toast
    this.el.toastContainer = document.getElementById('toast-container');
  }

  initTheme() {
    document.documentElement.setAttribute('data-theme', this.currentTheme);
  }

  toggleTheme() {
    this.currentTheme = this.currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', this.currentTheme);
    localStorage.setItem('qw_theme', this.currentTheme);
    this.updateThemeButtonIcon();
  }

  updateThemeButtonIcon() {
    if (this.el.themeToggleBtn) {
      this.el.themeToggleBtn.innerHTML = this.currentTheme === 'dark' ? '☀️' : '🌙';
    }
  }

  bindEvents() {
    // Search input with debounce
    let searchDebounce;
    this.el.searchInput.addEventListener('input', (e) => {
      clearTimeout(searchDebounce);
      const val = e.target.value;
      this.el.searchClearBtn.style.display = val ? 'block' : 'none';
      searchDebounce = setTimeout(() => this.applyFilter(), 60);
    });

    this.el.searchClearBtn.addEventListener('click', () => {
      this.el.searchInput.value = '';
      this.el.searchClearBtn.style.display = 'none';
      this.applyFilter();
    });

    // Filters
    this.el.chapterFilter.addEventListener('change', () => this.applyFilter());
    this.el.posFilter.addEventListener('change', () => this.applyFilter());
    this.el.sortFilter.addEventListener('change', () => this.applyFilter());

    // Language Select
    this.el.langSelect.value = this.currentLang;
    this.el.langSelect.addEventListener('change', (e) => {
      this.currentLang = e.target.value;
      localStorage.setItem('qw_lang', this.currentLang);
      this.updateUILanguage(this.currentLang, true);
    });

    // Theme Toggle
    this.el.themeToggleBtn.addEventListener('click', () => this.toggleTheme());
    this.updateThemeButtonIcon();

    // Bookmarks Filter Toggle
    let showOnlyBookmarks = false;
    this.el.bookmarksToggleBtn.addEventListener('click', () => {
      showOnlyBookmarks = !showOnlyBookmarks;
      this.el.bookmarksToggleBtn.classList.toggle('btn-gold', showOnlyBookmarks);
      this.applyFilter(showOnlyBookmarks);
    });

    // Random Discovery
    this.el.randomWordBtn.addEventListener('click', () => this.openRandomWord());

    // View Mode Tabs
    document.querySelectorAll('.view-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const mode = btn.dataset.mode;
        this.switchView(mode);
      });
    });

    // Modal Close
    this.el.modalCloseBtn.addEventListener('click', () => this.closeModal());
    this.el.modalBackdrop.addEventListener('click', (e) => {
      if (e.target === this.el.modalBackdrop) this.closeModal();
    });

    // Keyboard Shortcuts
    document.addEventListener('keydown', (e) => {
      if (e.key === '/' && document.activeElement !== this.el.searchInput) {
        e.preventDefault();
        this.el.searchInput.focus();
      } else if (e.key === 'Escape') {
        this.closeModal();
      } else if (this.currentView === 'flashcards') {
        if (e.code === 'Space') {
          e.preventDefault();
          this.flipFlashcard();
        } else if (e.code === 'ArrowRight') {
          this.nextFlashcard();
        } else if (e.code === 'ArrowLeft') {
          this.prevFlashcard();
        } else if (e.key.toLowerCase() === 'a') {
          const w = this.filteredWords[this.fcIndex];
          if (w) audioService.speakArabic(w.ar);
        }
      }
    });
  }

  /**
   * Complete UI Internationalization Switcher
   */
  updateUILanguage(lang, showToastNotification = true) {
    const dict = I18N_DICTIONARY[lang] || I18N_DICTIONARY['en'];
    document.documentElement.setAttribute('lang', lang);
    document.documentElement.setAttribute('dir', dict.dir || 'ltr');

    // Update Header & Brand
    const brandSub = document.querySelector('.brand-sub');
    if (brandSub) brandSub.textContent = t(lang, 'brandSub');

    // Update Hero
    const heroPill = document.getElementById('hero-pill');
    if (heroPill) heroPill.textContent = t(lang, 'heroPill');
    const heroTitle = document.getElementById('hero-title');
    if (heroTitle) heroTitle.textContent = t(lang, 'heroTitle');
    const heroSub = document.getElementById('hero-sub');
    if (heroSub) heroSub.textContent = t(lang, 'heroSub');

    // Update Stats Ribbon labels
    const lblLemmas = document.getElementById('stat-lbl-lemmas');
    if (lblLemmas) lblLemmas.textContent = t(lang, 'statLemmas');
    const lblOcc = document.getElementById('stat-lbl-occ');
    if (lblOcc) lblOcc.textContent = t(lang, 'statOccurrences');
    const lblRoots = document.getElementById('stat-lbl-roots');
    if (lblRoots) lblRoots.textContent = t(lang, 'statRoots');
    const lblChapters = document.getElementById('stat-lbl-chapters');
    if (lblChapters) lblChapters.textContent = t(lang, 'statChapters');
    const lblLangs = document.getElementById('stat-lbl-languages');
    if (lblLangs) lblLangs.textContent = t(lang, 'statLanguages');

    // Update Search bar & shortcuts
    if (this.el.searchInput) this.el.searchInput.placeholder = t(lang, 'searchPlaceholder');
    const searchShortcut = document.getElementById('search-shortcut');
    if (searchShortcut) searchShortcut.textContent = t(lang, 'searchShortcut');

    // Update View Mode Tabs
    const tabCards = document.getElementById('tab-btn-cards');
    if (tabCards) tabCards.textContent = t(lang, 'tabCards');
    const tabTable = document.getElementById('tab-btn-table');
    if (tabTable) tabTable.textContent = t(lang, 'tabTable');
    const tabFlashcards = document.getElementById('tab-btn-flashcards');
    if (tabFlashcards) tabFlashcards.textContent = t(lang, 'tabFlashcards');
    const tabRoots = document.getElementById('tab-btn-roots');
    if (tabRoots) tabRoots.textContent = t(lang, 'tabRoots');
    const tabQuiz = document.getElementById('tab-btn-quiz');
    if (tabQuiz) tabQuiz.textContent = t(lang, 'tabQuiz');

    // Update Dropdown Filters
    this.renderChapterFilters();
    this.renderPosFilter();
    this.renderSortFilter();

    // Update Table Header
    const thIdx = document.getElementById('th-index');
    if (thIdx) thIdx.textContent = t(lang, 'tableIndex');
    const thLemma = document.getElementById('th-lemma');
    if (thLemma) thLemma.textContent = t(lang, 'tableLemma');
    const thTranslit = document.getElementById('th-translit');
    if (thTranslit) thTranslit.textContent = t(lang, 'tableTranslit');
    const thRoot = document.getElementById('th-root');
    if (thRoot) thRoot.textContent = t(lang, 'tableRoot');
    const thMeaning = document.getElementById('th-meaning');
    if (thMeaning) thMeaning.textContent = t(lang, 'tableMeaning');
    const thFreq = document.getElementById('th-freq');
    if (thFreq) thFreq.textContent = t(lang, 'tableFreq');
    const thAudio = document.getElementById('th-audio');
    if (thAudio) thAudio.textContent = t(lang, 'tableAudio');

    // Update Flashcards UI text
    const fcFrontHint = document.getElementById('fc-flip-hint-front');
    if (fcFrontHint) fcFrontHint.textContent = t(lang, 'fcFlipHintFront');
    const fcBackHint = document.getElementById('fc-flip-hint-back');
    if (fcBackHint) fcBackHint.textContent = t(lang, 'fcFlipHintBack');
    const fcPrev = document.getElementById('fc-prev-btn');
    if (fcPrev) fcPrev.textContent = t(lang, 'fcPrev');
    const fcNext = document.getElementById('fc-next-btn');
    if (fcNext) fcNext.textContent = t(lang, 'fcNext');
    const fcAudio = document.getElementById('fc-audio-btn');
    if (fcAudio) fcAudio.textContent = t(lang, 'fcAudio');
    const fcKb = document.getElementById('fc-keyboard-hint');
    if (fcKb) fcKb.textContent = t(lang, 'fcKeyboardHint');

    // Update Roots UI
    const rootsTitle = document.getElementById('roots-title');
    if (rootsTitle) rootsTitle.textContent = t(lang, 'rootsTitle');
    const rootsSub = document.getElementById('roots-sub');
    if (rootsSub) rootsSub.textContent = t(lang, 'rootsSub');

    // Update Quiz UI
    const quizTitle = document.getElementById('quiz-title');
    if (quizTitle) quizTitle.textContent = t(lang, 'quizTitle');

    // Update Empty State
    const emptyTitle = document.getElementById('empty-title');
    if (emptyTitle) emptyTitle.textContent = t(lang, 'emptyTitle');
    const emptySub = document.getElementById('empty-sub');
    if (emptySub) emptySub.textContent = t(lang, 'emptySub');

    // Update Modal
    const modalTitle = document.getElementById('modal-title');
    if (modalTitle) modalTitle.textContent = t(lang, 'modalTitle');

    // Update Buttons
    const bmBtn = document.getElementById('bookmarks-toggle-btn');
    if (bmBtn) bmBtn.innerHTML = `⭐ ${t(lang, 'btnBookmarks')}`;
    const randBtn = document.getElementById('random-word-btn');
    if (randBtn) randBtn.innerHTML = `🎲 ${t(lang, 'btnRandom')}`;

    // Update Footer
    const footAbout = document.getElementById('footer-about');
    if (footAbout) footAbout.textContent = t(lang, 'footerAbout');
    const footRepo = document.getElementById('footer-repo-link');
    if (footRepo) footRepo.textContent = t(lang, 'footerRepo');
    const footLive = document.getElementById('footer-live-link');
    if (footLive) footLive.textContent = t(lang, 'footerLive');
    const footReleases = document.getElementById('footer-releases-link');
    if (footReleases) footReleases.textContent = t(lang, 'footerReleases');
    const footCopy = document.getElementById('footer-copyright');
    if (footCopy) footCopy.textContent = t(lang, 'footerCopyright');

    // Re-render active view
    this.applyFilter();

    if (showToastNotification) {
      this.showToast(t(lang, 'langSwitchedToast', { lang: this.getLangName(lang) }));
    }
  }

  renderMetadataStats() {
    if (!this.metadata) return;
    if (this.el.statTotalWords) this.el.statTotalWords.textContent = (this.metadata.total_words || 4709).toLocaleString();
    if (this.el.statOccurrences) this.el.statOccurrences.textContent = (this.metadata.total_occurrences || 77430).toLocaleString();
    if (this.el.statRoots) this.el.statRoots.textContent = (this.metadata.total_roots || 250).toLocaleString();
  }

  renderChapterFilters() {
    if (!this.metadata || !this.metadata.chapters) return;
    const select = this.el.chapterFilter;
    const currentVal = select.value || 'all';
    select.innerHTML = `<option value="all">${t(this.currentLang, 'allChapters')}</option>`;
    for (const ch of this.metadata.chapters) {
      const opt = document.createElement('option');
      opt.value = ch.id;
      const title = (ch.title && (ch.title[this.currentLang] || ch.title.en)) || ch.title;
      const chLabel = t(this.currentLang, 'chapterLabel', { n: ch.sort });
      opt.textContent = `${chLabel}: ${title} (${ch.words})`;
      select.appendChild(opt);
    }
    select.value = currentVal;
  }

  renderPosFilter() {
    const select = this.el.posFilter;
    const currentVal = select.value || 'all';
    select.innerHTML = `
      <option value="all">${t(this.currentLang, 'allPos')}</option>
      <option value="noun">${t(this.currentLang, 'posNoun')}</option>
      <option value="verb">${t(this.currentLang, 'posVerb')}</option>
      <option value="particle">${t(this.currentLang, 'posParticle')}</option>
      <option value="pronoun">${t(this.currentLang, 'posPronoun')}</option>
      <option value="proper_noun">${t(this.currentLang, 'posProperNoun')}</option>
    `;
    select.value = currentVal;
  }

  renderSortFilter() {
    const select = this.el.sortFilter;
    const currentVal = select.value || 'default';
    select.innerHTML = `
      <option value="default">${t(this.currentLang, 'sortDefault')}</option>
      <option value="occ_desc">${t(this.currentLang, 'sortOccDesc')}</option>
      <option value="occ_asc">${t(this.currentLang, 'sortOccAsc')}</option>
      <option value="alpha_ar">${t(this.currentLang, 'sortAlphaAr')}</option>
      <option value="alpha_en">${t(this.currentLang, 'sortAlphaMeaning')}</option>
    `;
    select.value = currentVal;
  }

  renderRootCloud() {
    const cloud = document.getElementById('roots-cloud');
    if (!cloud || !this.roots) return;
    cloud.innerHTML = '';
    
    for (const r of this.roots.slice(0, 150)) {
      const pill = document.createElement('div');
      pill.className = 'root-pill';
      pill.innerHTML = `
        <span class="root-pill-arabic">${r.root}</span>
        <span class="root-pill-count">${r.words.length}</span>
      `;
      pill.addEventListener('click', () => {
        this.el.searchInput.value = r.root;
        this.switchView('cards');
        this.applyFilter();
      });
      cloud.appendChild(pill);
    }
  }

  switchView(mode) {
    this.currentView = mode;
    document.querySelectorAll('.view-tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.mode === mode);
    });

    this.el.cardsContainer.style.display = mode === 'cards' ? 'block' : 'none';
    this.el.tableContainer.style.display = mode === 'table' ? 'block' : 'none';
    this.el.flashcardContainer.style.display = mode === 'flashcards' ? 'block' : 'none';
    this.el.rootsContainer.style.display = mode === 'roots' ? 'block' : 'none';
    this.el.quizContainer.style.display = mode === 'quiz' ? 'block' : 'none';

    if (mode === 'flashcards') {
      this.initFlashcards();
    } else if (mode === 'quiz') {
      this.initQuiz();
    } else if (mode === 'table') {
      this.renderTable();
    }
  }

  applyFilter(bookmarksOnly = false) {
    const q = this.el.searchInput.value;
    const chapter = this.el.chapterFilter.value;
    const pos = this.el.posFilter.value;
    const sortBy = this.el.sortFilter.value;

    this.filteredWords = this.searchEngine.filter({
      query: q,
      chapter: chapter,
      pos: pos,
      lang: this.currentLang,
      bookmarksOnly: bookmarksOnly,
      bookmarkSet: this.bookmarks,
      sortBy: sortBy
    });

    if (this.el.statFilteredCount) {
      this.el.statFilteredCount.textContent = t(this.currentLang, 'wordsFound', { count: this.filteredWords.length.toLocaleString() });
    }

    if (this.currentView === 'cards') {
      this.resetCardStream();
    } else if (this.currentView === 'table') {
      this.renderTable();
    } else if (this.currentView === 'flashcards') {
      this.initFlashcards();
    }
  }

  /* ------------------------------------------------------------------------
     Card View Rendering (Infinite Stream)
     ------------------------------------------------------------------------ */
  resetCardStream() {
    this.el.wordsGrid.innerHTML = '';
    this.renderedCount = 0;
    
    if (this.filteredWords.length === 0) {
      this.el.emptyState.style.display = 'block';
      this.el.loadingIndicator.style.display = 'none';
      return;
    }

    this.el.emptyState.style.display = 'none';
    this.renderNextCardBatch();
  }

  renderNextCardBatch() {
    const start = this.renderedCount;
    const end = Math.min(start + this.BATCH_SIZE, this.filteredWords.length);
    const fragment = document.createDocumentFragment();

    for (let i = start; i < end; i++) {
      const word = this.filteredWords[i];
      const card = this.createCardElement(word);
      fragment.appendChild(card);
    }

    this.el.wordsGrid.appendChild(fragment);
    this.renderedCount = end;

    if (this.renderedCount < this.filteredWords.length) {
      this.el.loadingIndicator.style.display = 'block';
      const loadText = document.getElementById('loading-indicator-text');
      if (loadText) loadText.textContent = t(this.currentLang, 'streamingWords');
    } else {
      this.el.loadingIndicator.style.display = 'none';
    }
  }

  initIntersectionObserver() {
    this.observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && this.renderedCount < this.filteredWords.length) {
        this.renderNextCardBatch();
      }
    }, { rootMargin: '400px' });

    if (this.el.loadingIndicator) {
      this.observer.observe(this.el.loadingIndicator);
    }
  }

  createCardElement(w) {
    const card = document.createElement('div');
    card.className = 'word-card';
    card.dataset.id = w.id;

    const isBookmarked = this.bookmarks.has(w.id);
    const meaning = (w.m && w.m[this.currentLang]) || (w.m && w.m['en']) || '';
    const validRoot = (w.rt && w.rt !== '—' && w.rt !== '-' && w.rt !== 'None' && w.rt.trim() !== '') ? w.rt.trim() : null;
    const chNum = w.ch_num || (w.ch ? String(w.ch).replace(/^ch_0?/, '') : '1');
    const chLabel = t(this.currentLang, 'chapterLabel', { n: chNum });

    card.innerHTML = `
      <div class="card-header">
        <div class="card-arabic-wrap">
          <div class="card-arabic" title="Click to pronounce">${w.ar}</div>
          <div class="card-translit">${w.tr || ''}</div>
        </div>
        <div class="card-meta-right">
          <span class="card-pos-badge">${w.pos || 'noun'}</span>
          <span class="card-occ-pill">⚡ ${w.occ}×</span>
        </div>
      </div>

      <div class="card-tags-row">
        ${validRoot ? `<span class="tag-root" title="Filter by root">${validRoot}</span>` : ''}
        <span class="tag-chapter">${chLabel}</span>
      </div>

      <div class="card-meaning-box">
        <div class="card-primary-meaning">${meaning}</div>
        ${w.has_poly ? `<span class="card-poly-indicator">${t(this.currentLang, 'polyIndicator')}</span>` : ''}
      </div>

      <div class="card-verse-box" id="verse-box-${w.id}">
        <div class="card-verse-header">
          <span class="verse-ref-badge">📖 Ayah ${w.ref || ''}</span>
        </div>
        <div class="card-verse-content" id="verse-content-${w.id}">
          <p style="color: var(--text-dim); font-size: 0.8rem;">${t(this.currentLang, 'ayahContextPlaceholder')}</p>
        </div>
      </div>

      <div class="card-actions">
        <button class="card-action-btn audio-btn" title="Pronounce Arabic">${t(this.currentLang, 'btnAudio')}</button>
        <button class="card-action-btn copy-btn" title="Copy Lemma">${t(this.currentLang, 'btnCopy')}</button>
        <button class="card-action-btn bookmark-btn ${isBookmarked ? 'active' : ''}" title="Save Bookmark">
          ${isBookmarked ? t(this.currentLang, 'btnSaved') : t(this.currentLang, 'btnSave')}
        </button>
      </div>
    `;

    // Audio click
    card.querySelector('.card-arabic').addEventListener('click', () => audioService.speakArabic(w.ar));
    card.querySelector('.audio-btn').addEventListener('click', () => audioService.speakArabic(w.ar));

    // Copy click
    card.querySelector('.copy-btn').addEventListener('click', () => {
      navigator.clipboard.writeText(`${w.ar} (${w.tr}) - ${meaning}`);
      this.showToast(t(this.currentLang, 'copiedToast', { word: w.ar }));
    });

    // Bookmark toggle
    const bmBtn = card.querySelector('.bookmark-btn');
    bmBtn.addEventListener('click', () => this.toggleBookmark(w.id, bmBtn));

    // Root click
    if (validRoot) {
      const rootTag = card.querySelector('.tag-root');
      if (rootTag) {
        rootTag.addEventListener('click', () => {
          this.el.searchInput.value = validRoot;
          this.applyFilter();
        });
      }
    }

    // Polysemy click
    const polyInd = card.querySelector('.card-poly-indicator');
    if (polyInd) {
      polyInd.addEventListener('click', () => this.openPolysemyModal(w.id));
    }

    // Load full verse on hover or click
    card.addEventListener('mouseenter', () => this.loadCardVerse(w.id), { once: true });
    card.addEventListener('click', (e) => {
      if (!e.target.closest('button') && !e.target.closest('.tag-root') && !e.target.closest('.card-poly-indicator')) {
        this.loadCardVerse(w.id);
      }
    });

    return card;
  }

  loadCardVerse(wordId) {
    const box = document.getElementById(`verse-content-${wordId}`);
    if (!box) return;

    const fullWord = this.wordsFull.get(wordId);
    if (!fullWord) return;

    if (fullWord.v_ar) {
      const vTrans = (fullWord.v_tr && fullWord.v_tr[this.currentLang]) || (fullWord.v_tr && fullWord.v_tr['en']) || '';
      box.innerHTML = `
        <div class="card-verse-arabic">${fullWord.v_ar}</div>
        <div class="card-verse-trans">${vTrans}</div>
      `;
    }
  }

  toggleBookmark(id, btn) {
    if (this.bookmarks.has(id)) {
      this.bookmarks.delete(id);
      if (btn) {
        btn.classList.remove('active');
        btn.textContent = t(this.currentLang, 'btnSave');
      }
      this.showToast(t(this.currentLang, 'removedToast'));
    } else {
      this.bookmarks.add(id);
      if (btn) {
        btn.classList.add('active');
        btn.textContent = t(this.currentLang, 'btnSaved');
      }
      this.showToast(t(this.currentLang, 'savedToast'));
    }
    localStorage.setItem('qw_bookmarks', JSON.stringify([...this.bookmarks]));
  }

  /* ------------------------------------------------------------------------
     Table View Rendering
     ------------------------------------------------------------------------ */
  renderTable() {
    const tbody = this.el.tableBody;
    tbody.innerHTML = '';
    const slice = this.filteredWords.slice(0, 100); // Display up to 100 in table view

    for (let i = 0; i < slice.length; i++) {
      const w = slice[i];
      const meaning = (w.m && w.m[this.currentLang]) || (w.m && w.m['en']) || '';
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td style="color: var(--text-dim); font-size: 0.8rem;">#${w.id}</td>
        <td class="table-arabic-cell" style="cursor: pointer;" title="Click to listen">${w.ar}</td>
        <td><strong>${w.tr}</strong></td>
        <td>${w.rt ? `<span class="tag-root" style="font-size: 0.9rem;">${w.rt}</span>` : '<span style="color: var(--text-dim);">—</span>'}</td>
        <td>${meaning}</td>
        <td><span class="card-occ-pill">${w.occ}×</span></td>
        <td><button class="btn btn-icon btn-sm audio-btn">🔊</button></td>
      `;

      tr.querySelector('.table-arabic-cell').addEventListener('click', () => audioService.speakArabic(w.ar));
      tr.querySelector('.audio-btn').addEventListener('click', () => audioService.speakArabic(w.ar));
      tbody.appendChild(tr);
    }
  }

  /* ------------------------------------------------------------------------
     Flashcard SRS Study Mode
     ------------------------------------------------------------------------ */
  initFlashcards() {
    this.fcIndex = 0;
    this.fcFlipped = false;
    this.renderCurrentFlashcard();
    this.bindFlashcardControls();
  }

  renderCurrentFlashcard() {
    if (this.filteredWords.length === 0) return;
    const w = this.filteredWords[this.fcIndex];
    const flipper = document.getElementById('fc-flipper');
    if (flipper) flipper.classList.remove('flipped');
    this.fcFlipped = false;

    // Front elements
    document.getElementById('fc-front-arabic').textContent = w.ar;
    document.getElementById('fc-front-translit').textContent = w.tr;
    document.getElementById('fc-front-root').textContent = w.rt ? `Root: ${w.rt}` : '';

    // Back elements
    const meaning = (w.m && w.m[this.currentLang]) || (w.m && w.m['en']) || '';
    document.getElementById('fc-back-meaning').textContent = meaning;
    
    const chLabel = t(this.currentLang, 'chapterLabel', { n: w.ch_num || w.ch });
    document.getElementById('fc-back-occ').textContent = t(this.currentLang, 'fcAppears', { n: (w.occ || 0).toLocaleString(), ch: chLabel });

    // Counter
    document.getElementById('fc-counter').textContent = `${this.fcIndex + 1} / ${this.filteredWords.length}`;
  }

  flipFlashcard() {
    const flipper = document.getElementById('fc-flipper');
    if (!flipper) return;
    this.fcFlipped = !this.fcFlipped;
    flipper.classList.toggle('flipped', this.fcFlipped);
  }

  nextFlashcard() {
    if (this.fcIndex < this.filteredWords.length - 1) {
      this.fcIndex++;
      this.renderCurrentFlashcard();
    }
  }

  prevFlashcard() {
    if (this.fcIndex > 0) {
      this.fcIndex--;
      this.renderCurrentFlashcard();
    }
  }

  bindFlashcardControls() {
    const flipperWrap = document.getElementById('fc-perspective-box');
    if (flipperWrap) {
      flipperWrap.onclick = () => this.flipFlashcard();
    }
    const nextBtn = document.getElementById('fc-next-btn');
    if (nextBtn) nextBtn.onclick = () => this.nextFlashcard();
    const prevBtn = document.getElementById('fc-prev-btn');
    if (prevBtn) prevBtn.onclick = () => this.prevFlashcard();
    const audioBtn = document.getElementById('fc-audio-btn');
    if (audioBtn) audioBtn.onclick = () => {
      const w = this.filteredWords[this.fcIndex];
      if (w) audioService.speakArabic(w.ar);
    };
  }

  /* ------------------------------------------------------------------------
     Quiz & Self-Test Mode
     ------------------------------------------------------------------------ */
  initQuiz() {
    this.quizScore = 0;
    this.quizTotal = 0;
    this.generateNextQuizQuestion();
  }

  generateNextQuizQuestion() {
    if (this.filteredWords.length < 4) {
      this.showToast(t(this.currentLang, 'quizMinWords'));
      return;
    }

    this.quizAnswered = false;
    const correctIdx = Math.floor(Math.random() * this.filteredWords.length);
    const correctWord = this.filteredWords[correctIdx];

    // Pick 3 random distractor words
    const options = [correctWord];
    while (options.length < 4) {
      const rand = this.filteredWords[Math.floor(Math.random() * this.filteredWords.length)];
      if (!options.some(o => o.id === rand.id)) {
        options.push(rand);
      }
    }

    // Shuffle options
    options.sort(() => Math.random() - 0.5);

    document.getElementById('quiz-arabic').textContent = correctWord.ar;
    document.getElementById('quiz-translit').textContent = correctWord.tr;
    document.getElementById('quiz-score-display').textContent = t(this.currentLang, 'quizScore', { score: this.quizScore, total: this.quizTotal });

    const grid = document.getElementById('quiz-options');
    grid.innerHTML = '';

    for (const opt of options) {
      const btn = document.createElement('button');
      btn.className = 'quiz-option-btn';
      const optMeaning = (opt.m && opt.m[this.currentLang]) || (opt.m && opt.m['en']) || '';
      btn.textContent = optMeaning;

      btn.addEventListener('click', () => {
        if (this.quizAnswered) return;
        this.quizAnswered = true;
        this.quizTotal++;

        if (opt.id === correctWord.id) {
          btn.classList.add('correct');
          this.quizScore++;
          this.showToast(t(this.currentLang, 'quizCorrect'));
        } else {
          btn.classList.add('wrong');
          // Highlight correct one
          grid.querySelectorAll('.quiz-option-btn').forEach(b => {
            if (b.textContent === ((correctWord.m && correctWord.m[this.currentLang]) || correctWord.m['en'])) {
              b.classList.add('correct');
            }
          });
          this.showToast(t(this.currentLang, 'quizWrong'));
        }

        document.getElementById('quiz-score-display').textContent = t(this.currentLang, 'quizScore', { score: this.quizScore, total: this.quizTotal });
        setTimeout(() => this.generateNextQuizQuestion(), 1600);
      });

      grid.appendChild(btn);
    }
  }

  /* ------------------------------------------------------------------------
     Polysemy / Context Senses Modal
     ------------------------------------------------------------------------ */
  openPolysemyModal(wordId) {
    const fullWord = this.wordsFull.get(wordId);
    if (!fullWord || !fullWord.poly || fullWord.poly.length === 0) {
      this.showToast('No contextual polysemy records found for this lemma.');
      return;
    }

    let sensesHtml = '';
    for (const s of fullWord.poly) {
      const sMeaning = (s.m && s.m[this.currentLang]) || (s.m && s.m['en']) || '';
      const sTrans = (s.v_tr && s.v_tr[this.currentLang]) || (s.v_tr && s.v_tr['en']) || '';
      sensesHtml += `
        <div style="background: var(--bg-surface-elevated); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: 16px; margin-bottom: 14px;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
            <strong style="color: var(--accent-gold); font-size: 0.85rem;">Sense #${s.idx}: ${sMeaning}</strong>
            <span class="verse-ref-badge">📖 Ayah ${s.ref}</span>
          </div>
          <div class="card-verse-arabic" style="font-size: 1.3rem;">${s.v_ar}</div>
          <div class="card-verse-trans">${sTrans}</div>
        </div>
      `;
    }

    this.el.modalBody.innerHTML = `
      <div style="text-align: center; margin-bottom: 20px;">
        <div class="card-arabic" style="font-size: 2.8rem;">${fullWord.ar}</div>
        <div class="card-translit" style="font-size: 1.1rem;">${fullWord.tr}</div>
        <p style="color: var(--text-muted); font-size: 0.88rem; margin-top: 6px;">
          ${t(this.currentLang, 'modalSub')}
        </p>
      </div>
      ${sensesHtml}
    `;

    this.el.modalBackdrop.classList.add('open');
  }

  closeModal() {
    this.el.modalBackdrop.classList.remove('open');
  }

  openRandomWord() {
    if (this.wordsSummary.length === 0) return;
    const rand = this.wordsSummary[Math.floor(Math.random() * this.wordsSummary.length)];
    this.el.searchInput.value = rand.ar;
    this.switchView('cards');
    this.applyFilter();
    audioService.speakArabic(rand.ar);
    this.showToast(t(this.currentLang, 'randomToast', { word: `${rand.ar} (${rand.tr})` }));
  }

  getLangName(code) {
    const names = {
      en: 'English', bn: 'বাংলা', ur: 'اردو', hi: 'हिन्दी',
      in: 'Bahasa Indonesia', ms: 'Bahasa Melayu', tr: 'Türkçe',
      fa: 'فارسی', ha: 'Hausa', sw: 'Kiswahili', fr: 'Français'
    };
    return names[code] || code;
  }

  showToast(msg) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = msg;
    this.el.toastContainer.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add('show'));
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, 2800);
  }
}

// Instantiate and boot app on DOM load
window.addEventListener('DOMContentLoaded', () => {
  const app = new QuranicApp();
  app.init();
});
