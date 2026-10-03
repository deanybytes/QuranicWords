// Shared UI: toasts (aria-live), an accessible modal dialog, confirm prompts and celebrations.
import { h, clear } from './dom.js';
import { t } from './i18n.js';

export const reducedMotion = () => {
  try { return window.matchMedia('(prefers-reduced-motion: reduce)').matches; } catch { return false; }
};

/* ---------------- Toasts ---------------- */
export function toast(message, { action = null, duration = 3200, tone = 'info' } = {}) {
  const host = document.getElementById('toasts');
  if (!host) return;
  const el = h('div', { class: `toast toast-${tone}` }, h('span', { class: 'toast-msg', text: message }));
  if (action) {
    el.appendChild(h('button', { type: 'button', class: 'btn btn-sm', text: action.label, on: { click: () => { action.run(); el.remove(); } } }));
  }
  host.appendChild(el);
  requestAnimationFrame(() => el.classList.add('show'));
  if (duration > 0) {
    setTimeout(() => {
      el.classList.remove('show');
      setTimeout(() => el.remove(), 300);
    }, duration);
  }
}

/* ---------------- Modal dialog ---------------- */
const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
let returnFocus = null;
let onCloseCb = null;

function dialogEl() {
  return document.getElementById('modal');
}

export function initModal() {
  const dlg = dialogEl();
  if (!dlg) return;
  dlg.querySelector('#modal-close').addEventListener('click', () => closeModal());
  // Click on the backdrop (the <dialog> itself, outside the inner panel) closes it.
  dlg.addEventListener('click', (e) => { if (e.target === dlg) closeModal(); });
  dlg.addEventListener('cancel', (e) => { e.preventDefault(); closeModal(); });
  dlg.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') { e.preventDefault(); closeModal(); return; }
    if (e.key !== 'Tab') return;
    const items = [...dlg.querySelectorAll(FOCUSABLE)].filter((el) => el.offsetParent !== null || el === document.activeElement);
    if (!items.length) return;
    const first = items[0];
    const last = items[items.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
  });
}

/** Opens the modal with a title and body node. */
export function openModal(title, body, { onClose = null } = {}) {
  const dlg = dialogEl();
  if (!dlg) return;
  returnFocus = document.activeElement;
  onCloseCb = onClose;
  dlg.querySelector('#modal-title').textContent = title;
  dlg.querySelector('#modal-close').setAttribute('aria-label', t('close'));
  const bodyEl = clear(dlg.querySelector('#modal-body'));
  bodyEl.appendChild(body);
  if (!dlg.open) {
    if (typeof dlg.showModal === 'function') dlg.showModal();
    else dlg.setAttribute('open', '');
  }
  document.body.classList.add('modal-open');
  const focusTarget = bodyEl.querySelector('[autofocus]') || dlg.querySelector('#modal-close');
  if (focusTarget) focusTarget.focus();
}

export function closeModal() {
  const dlg = dialogEl();
  if (!dlg || !dlg.open) return;
  if (typeof dlg.close === 'function') dlg.close(); else dlg.removeAttribute('open');
  document.body.classList.remove('modal-open');
  const cb = onCloseCb;
  onCloseCb = null;
  if (cb) cb();
  if (returnFocus && typeof returnFocus.focus === 'function' && document.contains(returnFocus)) returnFocus.focus();
  returnFocus = null;
}

export function modalOpen() {
  const dlg = dialogEl();
  return Boolean(dlg && dlg.open);
}

/** Confirm prompt inside the accessible modal. Resolves true/false. */
export function confirmDialog(message, { confirmLabel = t('confirm'), danger = false } = {}) {
  return new Promise((resolve) => {
    let answered = false;
    const done = (v) => { if (answered) return; answered = true; resolve(v); };
    const body = h('div', { class: 'confirm-body' },
      h('p', { text: message }),
      h('div', { class: 'confirm-actions' },
        h('button', { type: 'button', class: 'btn', text: t('cancel'), on: { click: () => { done(false); closeModal(); } } }),
        h('button', { type: 'button', class: danger ? 'btn btn-danger' : 'btn btn-primary', text: confirmLabel, attrs: { autofocus: true }, on: { click: () => { done(true); closeModal(); } } })));
    openModal(confirmLabel, body, { onClose: () => done(false) });
  });
}

/* ---------------- Celebrations ---------------- */
const CONFETTI_COLORS = ['#ebc971', '#00bf63', '#7ed957', '#ffffff'];

/** Subtle confetti burst (skipped under prefers-reduced-motion — a static pulse is used instead). */
export function celebrate(anchor = null) {
  if (reducedMotion()) {
    if (anchor) { anchor.classList.remove('pulse-static'); void anchor.offsetWidth; anchor.classList.add('pulse-static'); }
    return;
  }
  if (anchor) { anchor.classList.remove('pulse'); void anchor.offsetWidth; anchor.classList.add('pulse'); }
  const layer = h('div', { class: 'confetti-layer', attrs: { 'aria-hidden': 'true' } });
  for (let i = 0; i < 36; i++) {
    const p = h('span', { class: 'confetti' });
    p.style.setProperty('--x', `${Math.round(Math.random() * 100)}vw`);
    p.style.setProperty('--dx', `${Math.round((Math.random() - 0.5) * 30)}vw`);
    p.style.setProperty('--r', `${Math.round(Math.random() * 720 - 360)}deg`);
    p.style.setProperty('--d', `${(1.6 + Math.random() * 1.2).toFixed(2)}s`);
    p.style.setProperty('--delay', `${(Math.random() * 0.25).toFixed(2)}s`);
    p.style.setProperty('--c', CONFETTI_COLORS[i % CONFETTI_COLORS.length]);
    layer.appendChild(p);
  }
  document.body.appendChild(layer);
  setTimeout(() => layer.remove(), 3200);
}
