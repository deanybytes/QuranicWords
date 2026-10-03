// Dictionary views: word cards (incremental), table (paginated) and the roots index.
import { h, clear, arabic } from '../dom.js';
import { t, formatNumber, getLang, langInfo } from '../i18n.js';
import { wordCard, catBadge, meaningText } from '../components.js';

const CARD_BATCH = 24;
const TABLE_PAGE = 50;
const ROOT_BATCH = 150;

function emptyState(ctx) {
  return h('div', { class: 'empty-state' },
    h('div', { class: 'empty-state-icon', attrs: { 'aria-hidden': 'true' }, text: '🔍' }),
    h('h2', { text: t('emptyTitle') }),
    h('p', { text: t('emptySub') }),
    h('button', { type: 'button', class: 'btn', text: t('clearFilters'), on: { click: () => ctx.clearFilters() } }));
}

/* ---------------- Cards ---------------- */
export const cardsView = {
  shown: CARD_BATCH,
  version: -1,
  expanded: new Set(),
  observer: null,

  render(ctx, el) {
    const list = ctx.filtered();
    if (this.version !== ctx.filterVersion) { this.version = ctx.filterVersion; this.shown = CARD_BATCH; }
    clear(el);
    if (!list.length) { el.appendChild(emptyState(ctx)); return; }
    const grid = h('div', { class: 'words-grid' });
    const moreWrap = h('div', { class: 'load-more-wrap' });
    el.append(grid, moreWrap);
    const renderMore = () => {
      const start = grid.childElementCount;
      const end = Math.min(this.shown, list.length);
      const frag = document.createDocumentFragment();
      for (let i = start; i < end; i++) frag.appendChild(wordCard(ctx, list[i], { expanded: this.expanded }));
      grid.appendChild(frag);
      clear(moreWrap);
      if (end < list.length) {
        moreWrap.append(
          h('p', { class: 'muted', text: t('showingCount', { shown: formatNumber(end), total: formatNumber(list.length) }) }),
          h('button', { type: 'button', class: 'btn', text: t('loadMore'), on: { click: () => { this.shown += CARD_BATCH; renderMore(); } } }));
      }
    };
    renderMore();
    // Auto-load when the "show more" area scrolls into view (the button remains for keyboard users).
    this.leave();
    if ('IntersectionObserver' in window) {
      this.observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && grid.childElementCount < list.length && grid.isConnected) { this.shown += CARD_BATCH; renderMore(); }
      }, { rootMargin: '600px' });
      this.observer.observe(moreWrap);
    }
  },

  leave() {
    if (this.observer) { this.observer.disconnect(); this.observer = null; }
  },
};

/* ---------------- Table ---------------- */
export const tableView = {
  page: 0,
  version: -1,

  render(ctx, el) {
    const list = ctx.filtered();
    if (this.version !== ctx.filterVersion) { this.version = ctx.filterVersion; this.page = 0; }
    clear(el);
    if (!list.length) { el.appendChild(emptyState(ctx)); return; }
    const pages = Math.ceil(list.length / TABLE_PAGE);
    this.page = Math.min(this.page, pages - 1);
    const slice = list.slice(this.page * TABLE_PAGE, (this.page + 1) * TABLE_PAGE);
    const info = langInfo(getLang());

    const tbody = h('tbody', {}, ...slice.map((w) => h('tr', {},
      h('td', { class: 'num', text: formatNumber(w.rank) }),
      h('td', { class: 'table-arabic-cell', attrs: { lang: 'ar', dir: 'rtl' }, text: w.ar }),
      h('td', { class: 'translit', text: w.tl }),
      h('td', {}, w.rt ? h('button', { type: 'button', class: 'tag-root', attrs: { lang: 'ar', dir: 'rtl', 'aria-label': t('filterByRoot', { root: w.rt }) }, text: w.rt, on: { click: () => ctx.filterByRoot(w.rt) } }) : h('span', { class: 'muted', text: '—' })),
      h('td', {}, h('span', { attrs: { lang: info.bcp47 }, text: meaningText(w) }), ' ', catBadge(w)),
      h('td', { class: 'num', text: formatNumber(w.occ) }))));

    const table = h('table', { class: 'dict-table' },
      h('caption', { class: 'visually-hidden', text: t('navTable') }),
      h('thead', {}, h('tr', {}, ...['tableRank', 'tableWord', 'tableTranslit', 'tableRoot', 'tableMeaning', 'tableFreq'].map((k) => h('th', { attrs: { scope: 'col' }, text: t(k) })))),
      tbody);

    const go = (p) => { this.page = p; this.render(ctx, el); el.querySelector('.table-container').focus(); };
    const pager = h('nav', { class: 'pager', attrs: { 'aria-label': t('pageOf', { page: formatNumber(this.page + 1), pages: formatNumber(pages) }) } },
      h('button', { type: 'button', class: 'btn', disabled: this.page === 0, on: { click: () => go(this.page - 1) } }, h('span', { text: t('pagePrev') })),
      h('span', { class: 'pager-info', text: t('pageOf', { page: formatNumber(this.page + 1), pages: formatNumber(pages) }) }),
      h('button', { type: 'button', class: 'btn', disabled: this.page >= pages - 1, on: { click: () => go(this.page + 1) } }, h('span', { text: t('pageNext') })));

    el.append(h('div', { class: 'table-container', attrs: { tabindex: '-1' } }, table), pager);
  },
};

/* ---------------- Roots ---------------- */
export const rootsView = {
  shown: ROOT_BATCH,
  query: '',

  render(ctx, el) {
    clear(el);
    const roots = ctx.data.roots;
    const input = h('input', { type: 'search', class: 'search-input roots-search', id: 'roots-search', value: this.query, attrs: { autocomplete: 'off', placeholder: t('rootsSearch') } });
    const cloud = h('ul', { class: 'roots-cloud' });
    const more = h('div', { class: 'load-more-wrap' });
    el.append(
      h('div', { class: 'section-head' }, h('h1', { class: 'view-title', text: t('rootsTitle') }), h('p', { class: 'muted', text: t('rootsSub', { n: formatNumber(roots.length) }) })),
      h('div', { class: 'roots-tools' }, h('label', { class: 'visually-hidden', attrs: { for: 'roots-search' }, text: t('rootsSearch') }), input),
      cloud, more);

    const draw = () => {
      const q = ctx.skeleton(this.query);
      const list = q ? roots.filter((r) => ctx.skeleton(r.root).includes(q)) : roots;
      const visible = list.slice(0, this.shown);
      cloud.replaceChildren(...visible.map((r) => h('li', {},
        h('button', { type: 'button', class: 'root-pill', attrs: { 'aria-label': `${t('filterByRoot', { root: r.root })} (${t('wordCount', { n: formatNumber(r.ids.length) })})` }, on: { click: () => ctx.filterByRoot(r.root) } },
          arabic(r.root, 'root-pill-arabic'), h('span', { class: 'root-pill-count', attrs: { 'aria-hidden': 'true' }, text: formatNumber(r.ids.length) })))));
      clear(more);
      if (list.length > visible.length) {
        more.append(
          h('p', { class: 'muted', text: t('showingCount', { shown: formatNumber(visible.length), total: formatNumber(list.length) }) }),
          h('button', { type: 'button', class: 'btn', text: t('loadMore'), on: { click: () => { this.shown += ROOT_BATCH; draw(); } } }));
      }
    };
    input.addEventListener('input', () => { this.query = input.value; this.shown = ROOT_BATCH; draw(); });
    draw();
  },
};
