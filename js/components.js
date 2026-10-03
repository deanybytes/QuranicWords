// Reusable word widgets: verse context (lazy), word card, senses dialog.
import { h, icon, arabic, splitSpan, markedText } from './dom.js';
import { t, pick, cite, getLang, langInfo, formatNumber } from './i18n.js';
import { toast, openModal } from './ui.js';
import { BASE } from './config.js';

/* ---------------- Verse context ---------------- */
function uiDirAttrs() {
  const info = langInfo(getLang());
  return { lang: info.bcp47, dir: info.dir };
}

function spanLine(cls, text, s, e, markClass, attrs) {
  const p = h('p', { class: cls, attrs });
  const parts = splitSpan(text, s, e);
  if (parts) p.appendChild(markedText(parts, markClass)); else p.textContent = text || '';
  return p;
}

const ARABIC_INDIC = '٠١٢٣٤٥٦٧٨٩';
/** End-of-ayah mark with the ayah number, as in a printed mushaf ("۝٣٤"), for a "s:a" key. */
export function ayahMark(key) {
  const a = key ? Number(String(key).split(':')[1]) : 0;
  return a > 0 ? `\u00a0\u06dd${String(a).replace(/[0-9]/g, (d) => ARABIC_INDIC[d])}` : '';
}

/**
 * Renders one sense example (see DataStore.sensesFor): the complete ayah with only the taught
 * word marked, and ONE translation with exactly the card's meaning marked - the full translation
 * when it contains that meaning verbatim, otherwise the word-by-word translation (labelled).
 */
export function verseNode(v, ref) {
  const wrap = h('div', { class: 'verse' });
  const info = uiDirAttrs();
  const ar = spanLine('card-verse-arabic', v.v_ar, v.s, v.e, null, { lang: 'ar', dir: 'rtl' });
  if (v.key) ar.append(ayahMark(v.key));   // after the verse: the spans are unaffected
  wrap.appendChild(ar);
  if (v.tr && Number.isInteger(v.ts)) {
    wrap.appendChild(spanLine('card-verse-trans', v.tr, v.ts, v.te, 'mark-tr', info));
  } else if (v.wbw) {
    wrap.appendChild(h('p', { class: 'verse-label', text: t('wbwLabel') }));
    wrap.appendChild(spanLine('card-verse-trans', v.wbw, v.ws, v.we, 'mark-tr', info));
  }
  if (ref) wrap.appendChild(h('p', { class: 'verse-ref', text: cite(ref) }));
  return wrap;
}

function senseList(senses) {
  if (senses.length === 1) return [verseNode(senses[0], senses[0].ref)];
  return senses.map((s) => h('section', { class: 'sense-item' },
    h('h3', { class: 'sense-title' }, h('span', { text: t('senseN', { n: formatNumber(s.i) }) }), ': ', h('span', { attrs: uiDirAttrs(), text: s.m })),
    verseNode(s, s.ref)));
}

/**
 * Collapsible verse context that loads its chapter file on demand.
 * `expanded` is a Set of word ids kept by the caller so the open state survives re-renders.
 */
export function verseToggle(ctx, w, expanded) {
  const box = h('div', { class: 'card-verse-box' });
  const regionId = `verse-${w.id}`;
  const btn = h('button', { type: 'button', class: 'verse-toggle', attrs: { 'aria-expanded': 'false', 'aria-controls': regionId } });
  const label = h('span', { class: 'verse-toggle-label' });
  // Each language has its own examples; the badge shows the reference once it is known.
  const badge = h('span', { class: 'verse-ref-badge', text: getLang() === 'en' ? cite(w.ref) : '' });
  btn.append(icon('📖'), label, badge);
  const region = h('div', { class: 'card-verse-content', id: regionId, hidden: true });
  box.append(btn, region);

  const setLabel = (open) => { label.textContent = open ? t('hideVerse') : t('showVerse'); btn.setAttribute('aria-expanded', String(open)); };
  const load = () => {
    region.replaceChildren(h('p', { class: 'muted', attrs: { role: 'status' }, text: t('verseLoading') }));
    ctx.data.sensesFor(w.id).then((senses) => {
      if (!senses.length) { region.replaceChildren(h('p', { class: 'muted', text: t('noVerse') })); return; }
      badge.textContent = cite(senses[0].ref);
      region.replaceChildren(...senseList(senses));
    }).catch(() => {
      region.replaceChildren(h('p', { class: 'error-text', attrs: { role: 'alert' } }, t('verseError'), ' ',
        h('button', { type: 'button', class: 'btn btn-sm', text: t('retry'), on: { click: load } })));
    });
  };
  const open = (state) => {
    region.hidden = !state;
    setLabel(state);
    if (state) { expanded && expanded.add(w.id); load(); } else if (expanded) expanded.delete(w.id);
  };
  btn.addEventListener('click', (e) => { e.stopPropagation(); open(region.hidden); });
  setLabel(false);
  if (expanded && expanded.has(w.id)) open(true);
  return box;
}

/* ---------------- Senses (polysemy) dialog ---------------- */
export function openSenses(ctx, w) {
  const body = h('div', { class: 'senses' });
  body.append(
    h('div', { class: 'senses-head' }, arabic(w.ar, 'senses-ar')),
    h('p', { class: 'muted', text: t('sensesSub') }));
  const list = h('div', { class: 'senses-list' }, h('p', { class: 'muted', attrs: { role: 'status' }, text: t('verseLoading') }));
  body.appendChild(list);
  openModal(t('contextSenses'), body);
  const render = () => ctx.data.sensesFor(w.id).then((senses) => {
    if (!senses.length) { list.replaceChildren(h('p', { class: 'muted', text: t('noVerse') })); return; }
    list.replaceChildren(...senseList(senses));
  }).catch(() => {
    list.replaceChildren(h('p', { class: 'error-text', attrs: { role: 'alert' } }, t('verseError'), ' ',
      h('button', { type: 'button', class: 'btn btn-sm', text: t('retry'), on: { click: render } })));
  });
  render();
}

/* ---------------- Small pieces ---------------- */
export function catBadge(w) {
  return h('span', { class: `card-pos-badge cat-${w.cat.toLowerCase()}`, text: t(`badge${w.cat}`) });
}

export function verbFormsLine(w) {
  if (!w.vf) return null;
  const parts = [];
  if (w.vf.pastArabic) parts.push([t('pastLabel'), w.vf.pastArabic]);
  if (w.vf.presentArabic) parts.push([t('presentLabel'), w.vf.presentArabic]);
  if (w.vf.masdarArabic) parts.push([t('masdarLabel'), w.vf.masdarArabic]);
  if (parts.length < 2) return null;
  return h('p', { class: 'verb-forms' }, h('span', { class: 'visually-hidden', text: `${t('verbForms')}: ` }),
    ...parts.flatMap(([label, ar], i) => [i ? ' · ' : '', h('span', { class: 'vf-label', text: `${label} ` }), arabic(ar, 'vf-ar')]));
}

export function meaningText(w) {
  return pick(w.m);
}

/* ---------------- Dictionary word card ---------------- */
export function wordCard(ctx, w, { expanded } = {}) {
  const lang = getLang();
  const card = ctx.store.state.cards[w.id];
  const saved = ctx.isSaved(w.id);
  const meaning = meaningText(w);

  const arBtn = h('span', { class: 'card-arabic', attrs: { lang: 'ar', dir: 'rtl' }, text: w.ar });
  const head = h('div', { class: 'card-header' },
    h('div', { class: 'card-arabic-wrap' }, arBtn),
    h('div', { class: 'card-meta-right' }, catBadge(w), h('span', { class: 'card-occ-pill', text: t('occurrences', { n: formatNumber(w.occ) }) })));

  const tags = h('div', { class: 'card-tags-row' });
  if (w.rt) tags.appendChild(h('button', { type: 'button', class: 'tag-root', attrs: { lang: 'ar', dir: 'rtl', 'aria-label': t('filterByRoot', { root: w.rt }) }, text: w.rt, on: { click: () => ctx.filterByRoot(w.rt) } }));
  tags.appendChild(h('span', { class: 'tag-chapter', text: t('chapterLabel', { n: formatNumber(w.ch) }) }));
  tags.appendChild(h('span', { class: 'tag-chapter', text: t('rankLabel', { n: formatNumber(w.rank) }) }));
  if (card) tags.appendChild(h('span', { class: `tag-learned box-${card.b}`, text: t('learnedBox', { n: formatNumber(card.b) }) }));

  const meaningBox = h('div', { class: 'card-meaning-box' },
    h('p', { class: 'card-primary-meaning', attrs: { lang: langInfo(lang).bcp47 }, text: meaning }),
    w.pos ? h('p', { class: 'card-pos-detail', attrs: { lang: langInfo(getLang()).bcp47, dir: langInfo(getLang()).dir }, text: pick(w.pos) }) : null,
    verbFormsLine(w),
    w.poly && w.poly[lang] ? h('button', { type: 'button', class: 'card-poly-indicator', on: { click: () => openSenses(ctx, w) } }, icon('🔀'), h('span', { text: t('contextSenses') })) : null);

  const pageLink = w.u ? h('a', { class: 'card-action-btn', attrs: { href: `${BASE}quran-words/${w.u}.html` } }, icon('🔗'), h('span', { text: t('wordPage') })) : null;
  const copyBtn = h('button', { type: 'button', class: 'card-action-btn', on: { click: () => ctx.copyWord(w) } }, icon('📋'), h('span', { text: t('copy') }));
  const saveBtn = h('button', { type: 'button', class: `card-action-btn bookmark-btn${saved ? ' active' : ''}`, attrs: { 'aria-pressed': String(saved) } },
    icon(saved ? '★' : '☆'), h('span', { text: saved ? t('saved') : t('save') }));
  saveBtn.addEventListener('click', () => {
    const now = ctx.toggleSaved(w.id);
    saveBtn.classList.toggle('active', now);
    saveBtn.setAttribute('aria-pressed', String(now));
    saveBtn.replaceChildren(icon(now ? '★' : '☆'), h('span', { text: now ? t('saved') : t('save') }));
  });

  return h('article', { class: 'word-card', attrs: { 'aria-label': `${w.ar} — ${meaning}` } },
    head, tags, meaningBox, w.ref ? verseToggle(ctx, w, expanded) : null,
    h('div', { class: 'card-actions' }, copyBtn, pageLink, saveBtn));
}
