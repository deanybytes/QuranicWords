// DOM helpers. Data strings are never parsed as HTML: everything goes through textContent or
// escapeHtml(). Nothing here touches `document` at import time, so the pure helpers are testable
// in Node.

const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '`': '&#96;' };

/** Escapes a value for safe interpolation into HTML text or a quoted attribute. */
export function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value).replace(/[&<>"'`]/g, (c) => ESCAPES[c]);
}

/**
 * Creates an element. `props` keys:
 *   class, text, html (never used with data), attrs:{}, dataset:{}, on:{event: fn}, hidden, and any
 *   other key is assigned as a DOM property (e.g. type, value, disabled, href).
 * Children may be nodes, strings (become text nodes), arrays, or null/false (skipped).
 */
export function h(tag, props = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(props || {})) {
    if (v === undefined || v === null || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k === 'text') el.textContent = v;
    else if (k === 'attrs') for (const [a, av] of Object.entries(v)) { if (av !== null && av !== undefined && av !== false) el.setAttribute(a, av === true ? '' : String(av)); }
    else if (k === 'dataset') Object.assign(el.dataset, v);
    else if (k === 'on') for (const [ev, fn] of Object.entries(v)) el.addEventListener(ev, fn);
    else el[k] = v;
  }
  append(el, children);
  return el;
}

function append(el, children) {
  for (const c of children) {
    if (c === null || c === undefined || c === false) continue;
    if (Array.isArray(c)) append(el, c);
    else if (typeof c === 'string' || typeof c === 'number') el.appendChild(document.createTextNode(String(c)));
    else el.appendChild(c);
  }
}

/** Removes all children of an element. */
export function clear(el) {
  while (el && el.firstChild) el.removeChild(el.firstChild);
  return el;
}

/** Decorative icon (hidden from assistive technology). */
export function icon(glyph) {
  const s = document.createElement('span');
  s.className = 'ico';
  s.setAttribute('aria-hidden', 'true');
  s.textContent = glyph;
  return s;
}

/** An Arabic text span with correct language and direction. */
export function arabic(text, cls = 'ar') {
  return h('span', { class: cls, attrs: { lang: 'ar', dir: 'rtl' }, text });
}

/**
 * Splits `text` into [before, match, after] around the first occurrence of `needle`.
 * Returns null if `needle` is empty or not found.
 */
export function splitHighlight(text, needle) {
  if (!text || !needle) return null;
  const i = text.indexOf(needle);
  if (i < 0) return null;
  return [text.slice(0, i), text.slice(i, i + needle.length), text.slice(i + needle.length)];
}

/** Splits text by character span [s, e). Returns null for an invalid span. */
export function splitSpan(text, s, e) {
  if (typeof text !== 'string' || !Number.isInteger(s) || !Number.isInteger(e) || s < 0 || e <= s || e > text.length) return null;
  return [text.slice(0, s), text.slice(s, e), text.slice(e)];
}

/** Builds a node fragment: before + <mark>match</mark> + after (all as text). */
export function markedText(parts, markClass) {
  const frag = document.createDocumentFragment();
  if (!parts) return frag;
  frag.appendChild(document.createTextNode(parts[0]));
  const m = document.createElement('mark');
  if (markClass) m.className = markClass;
  m.textContent = parts[1];
  frag.appendChild(m);
  frag.appendChild(document.createTextNode(parts[2]));
  return frag;
}

/** True if the event target is a text-entry control where shortcuts must not fire. */
export function isTypingTarget(target) {
  if (!target || !target.tagName) return false;
  const tag = target.tagName.toUpperCase();
  if (tag === 'TEXTAREA' || tag === 'SELECT') return true;
  if (tag === 'INPUT') {
    const type = (target.type || 'text').toLowerCase();
    return !['button', 'submit', 'reset', 'checkbox', 'radio', 'range', 'color', 'file'].includes(type);
  }
  return Boolean(target.isContentEditable);
}
