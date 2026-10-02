// Progress: stats, Leitner boxes, daily goal, 14-day history, mistakes, export / import / reset.
import { h, clear, arabic } from '../dom.js';
import { t, formatNumber, formatPercent, formatDate, pick } from '../i18n.js';
import { levelForXp, xpForLevel, currentStreak, knownFraction, GOALS, validateProgress, exportEnvelope } from '../progress.js';
import { boxCounts, dueIds, dayToDate } from '../srs.js';
import { toast, confirmDialog } from '../ui.js';
import { storageAvailable } from '../storage.js';

function statTile(label, value, cls = '') {
  return h('div', { class: `stat-chip ${cls}` }, h('span', { class: 'stat-val', text: value }), h('span', { class: 'stat-lbl', text: label }));
}

export function downloadJSON(filename, obj) {
  const blob = new Blob([JSON.stringify(obj, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = h('a', { href: url, download: filename, hidden: true });
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export const progressView = {
  render(ctx, el) {
    const s = ctx.store.state;
    const today = ctx.today();
    const level = levelForXp(s.xp);
    const learned = Object.keys(s.cards).length;
    const known = knownFraction(s.cards, ctx.data.byId, ctx.data.meta.occurrences);
    clear(el);
    el.appendChild(h('div', { class: 'section-head' }, h('h1', { class: 'view-title', text: t('progressTitle') })));
    if (!storageAvailable()) el.appendChild(h('p', { class: 'error-text', attrs: { role: 'alert' }, text: t('storageUnavailable') }));

    el.appendChild(h('div', { class: 'stats-ribbon progress-stats' },
      statTile(t('statLevel'), formatNumber(level), 'primary'),
      statTile(t('statXp'), formatNumber(s.xp), 'gold'),
      statTile(t('statStreak'), formatNumber(currentStreak(s, today))),
      statTile(t('statBestStreak'), formatNumber(s.streak.best || 0)),
      statTile(t('statLearned'), formatNumber(learned), 'primary'),
      statTile(t('statKnown'), formatPercent(known)),
      statTile(t('statDue'), formatNumber(dueIds(s.cards, today).length), 'gold')));

    // Level bar
    const lo = xpForLevel(level);
    const hi = xpForLevel(level + 1);
    el.appendChild(h('div', { class: 'panel' },
      h('h2', { class: 'panel-title', text: t('levelLabel', { n: formatNumber(level) }) }),
      h('progress', { class: 'xp-progress', max: hi - lo, value: s.xp - lo, attrs: { 'aria-label': t('xpProgress', { xp: formatNumber(s.xp), next: formatNumber(hi) }) } }),
      h('p', { class: 'muted', text: t('xpProgress', { xp: formatNumber(s.xp), next: formatNumber(hi) }) })));

    // Daily goal
    const goalGroup = h('div', { class: 'goal-options', attrs: { role: 'radiogroup', 'aria-labelledby': 'goal-title' } },
      ...GOALS.map((g) => h('label', { class: `goal-option${s.goal === g ? ' active' : ''}` },
        h('input', { type: 'radio', name: 'goal', value: String(g), checked: s.goal === g, on: { change: () => { ctx.store.setGoal(g); ctx.rerender(); } } }),
        h('span', { text: t('goalOption', { n: formatNumber(g) }) }))));
    el.appendChild(h('div', { class: 'panel' }, h('h2', { class: 'panel-title', id: 'goal-title', text: t('dailyGoal') }), goalGroup,
      h('p', { class: 'muted', text: t('goalToday', { xp: formatNumber(s.daily[today] || 0), goal: formatNumber(s.goal) }) })));

    // History (last 14 days)
    const days = [];
    for (let d = today - 13; d <= today; d++) days.push([d, s.daily[d] || 0]);
    const max = Math.max(s.goal, ...days.map(([, v]) => v));
    el.appendChild(h('div', { class: 'panel' }, h('h2', { class: 'panel-title', text: t('historyTitle') }),
      h('ol', { class: 'history-chart' }, ...days.map(([d, v]) => {
        const bar = h('span', { class: `history-bar${v >= s.goal ? ' met' : ''}` });
        bar.style.setProperty('--h', `${Math.round((v / max) * 100)}%`);
        return h('li', { attrs: { 'aria-label': `${formatDate(dayToDate(d))}: ${formatNumber(v)} XP` } }, bar, h('span', { class: 'history-day', attrs: { 'aria-hidden': 'true' }, text: formatNumber(dayToDate(d).getDate()) }));
      }))));

    // Boxes
    const counts = boxCounts(s.cards);
    const maxC = Math.max(1, ...counts);
    el.appendChild(h('div', { class: 'panel' }, h('h2', { class: 'panel-title', text: t('boxesTitle') }), h('p', { class: 'muted', text: t('boxesSub') }),
      h('ol', { class: 'box-chart' }, ...counts.map((c, i) => {
        const bar = h('span', { class: `box-bar box-${i + 1}` });
        bar.style.setProperty('--w', `${Math.round((c / maxC) * 100)}%`);
        return h('li', {}, h('span', { class: 'box-name', text: t('boxLabel', { n: formatNumber(i + 1) }) }), bar, h('span', { class: 'box-count', text: formatNumber(c) }));
      }))));

    // Mistakes
    const mistakes = Object.entries(s.mistakes).filter(([id]) => ctx.data.byId.has(id)).sort((a, b) => (b[1].last - a[1].last) || (b[1].n - a[1].n));
    const mPanel = h('div', { class: 'panel' }, h('h2', { class: 'panel-title', text: t('mistakesTitle') }));
    if (!mistakes.length) mPanel.appendChild(h('p', { class: 'muted', text: t('mistakesEmpty') }));
    else {
      mPanel.append(
        h('ul', { class: 'mistake-list' }, ...mistakes.slice(0, 50).map(([id, m]) => {
          const w = ctx.data.byId.get(id);
          return h('li', {}, arabic(w.ar, 'mistake-ar'), h('span', { class: 'mistake-meaning', text: pick(w.m) }), h('span', { class: 'muted', text: t('mistakesCount', { n: formatNumber(m.n) }) }));
        })),
        h('div', { class: 'row-actions' },
          h('button', { type: 'button', class: 'btn btn-primary', on: { click: () => ctx.practiceMistakes() } }, h('span', { text: t('practiceMistakes') })),
          h('button', { type: 'button', class: 'btn', on: { click: () => { ctx.store.clearMistakes(); ctx.rerender(); } } }, h('span', { text: t('clearMistakes') }))));
    }
    el.appendChild(mPanel);

    // Data
    const fileInput = h('input', { type: 'file', id: 'import-file', class: 'visually-hidden', attrs: { accept: 'application/json,.json', tabindex: '-1', 'aria-hidden': 'true' } });
    fileInput.addEventListener('change', () => this.importFile(ctx, fileInput));
    el.appendChild(h('div', { class: 'panel' }, h('h2', { class: 'panel-title', text: t('dataTitle') }), h('p', { class: 'muted', text: t('dataSub') }),
      h('div', { class: 'row-actions' },
        h('button', { type: 'button', class: 'btn btn-primary', on: { click: () => this.exportFile(ctx) } }, h('span', { text: t('exportProgress') })),
        h('button', { type: 'button', class: 'btn', on: { click: () => fileInput.click() } }, h('span', { text: t('importProgress') })),
        h('button', { type: 'button', class: 'btn btn-danger', on: { click: () => this.reset(ctx) } }, h('span', { text: t('resetProgress') }))),
      fileInput));
  },

  exportFile(ctx) {
    const d = new Date();
    const stamp = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    downloadJSON(`quranicwords-progress-${stamp}.json`, exportEnvelope(ctx.store.state));
    toast(t('exportDone'));
  },

  async importFile(ctx, input) {
    const file = input.files && input.files[0];
    input.value = '';
    if (!file) return;
    let parsed = null;
    try {
      if (file.size > 5 * 1024 * 1024) throw new Error('too large');
      parsed = validateProgress(JSON.parse(await file.text()));
    } catch {
      parsed = null;
    }
    if (!parsed) { toast(t('importInvalid'), { tone: 'error' }); return; }
    if (!(await confirmDialog(t('importConfirm'), { confirmLabel: t('importProgress') }))) return;
    ctx.store.replace(parsed);
    toast(t('importDone'));
    ctx.rerender();
  },

  async reset(ctx) {
    if (!(await confirmDialog(t('resetConfirm'), { confirmLabel: t('resetProgress'), danger: true }))) return;
    ctx.store.reset();
    toast(t('resetDone'));
    ctx.rerender();
  },
};
