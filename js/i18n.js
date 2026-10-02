// Internationalisation: the 8 languages the curriculum data is translated into.
// Data uses "in" for Indonesian; the document language uses the BCP-47 tag "id".
import en from './locales/en.js';
import bn from './locales/bn.js';
import ur from './locales/ur.js';
import hi from './locales/hi.js';
import id from './locales/in.js';
import tr from './locales/tr.js';
import fa from './locales/fa.js';
import fr from './locales/fr.js';

export const LANGUAGES = [
  { code: 'en', bcp47: 'en', name: 'English', dir: 'ltr' },
  { code: 'bn', bcp47: 'bn', name: 'বাংলা', dir: 'ltr' },
  { code: 'ur', bcp47: 'ur', name: 'اردو', dir: 'rtl' },
  { code: 'hi', bcp47: 'hi', name: 'हिन्दी', dir: 'ltr' },
  { code: 'in', bcp47: 'id', name: 'Bahasa Indonesia', dir: 'ltr' },
  { code: 'tr', bcp47: 'tr', name: 'Türkçe', dir: 'ltr' },
  { code: 'fa', bcp47: 'fa', name: 'فارسی', dir: 'rtl' },
  { code: 'fr', bcp47: 'fr', name: 'Français', dir: 'ltr' },
];

export const LANG_CODES = LANGUAGES.map((l) => l.code);
export const DICTIONARY = { en, bn, ur, hi, in: id, tr, fa, fr };

let current = 'en';

/** Normalises a requested language code (accepts BCP-47 "id" for Indonesian). */
export function normalizeLang(code) {
  if (!code) return null;
  const c = String(code).toLowerCase().split(/[-_]/)[0];
  if (c === 'id') return 'in';
  return LANG_CODES.includes(c) ? c : null;
}

export function langInfo(code = current) {
  return LANGUAGES.find((l) => l.code === code) || LANGUAGES[0];
}

export function setLang(code) {
  current = normalizeLang(code) || 'en';
  return current;
}

export function getLang() {
  return current;
}

/** Formats a template: replaces {name} with params.name. */
export function format(template, params = {}) {
  return String(template).replace(/\{(\w+)\}/g, (m, k) => (k in params ? String(params[k]) : m));
}

/** Translate `key` in an explicit language, falling back to English then the key itself. */
export function translate(lang, key, params) {
  const dict = DICTIONARY[lang] || en;
  const s = dict[key] ?? en[key] ?? key;
  return params ? format(s, params) : s;
}

/** Translate in the current UI language. */
export function t(key, params) {
  return translate(current, key, params);
}

export function formatNumber(n, lang = current) {
  try {
    return new Intl.NumberFormat(langInfo(lang).bcp47).format(n);
  } catch {
    return String(n);
  }
}

export function formatPercent(fraction, lang = current, digits = 1) {
  try {
    return new Intl.NumberFormat(langInfo(lang).bcp47, { style: 'percent', minimumFractionDigits: digits, maximumFractionDigits: digits }).format(fraction);
  } catch {
    return `${(fraction * 100).toFixed(digits)}%`;
  }
}

export function formatDate(date, lang = current) {
  try {
    return new Intl.DateTimeFormat(langInfo(lang).bcp47, { weekday: 'short', day: 'numeric', month: 'short' }).format(date);
  } catch {
    return date.toDateString();
  }
}

/** Picks the value for the current language from a {lang: text} map, falling back to English. */
export function pick(map, lang = current) {
  if (!map || typeof map !== 'object') return '';
  return map[lang] || map.en || '';
}

/** "Surah Al-Baqarah 2:22" in the current language. */
export function cite(ref, lang = current) {
  return ref ? `${translate(lang, 'surahWord')} ${ref}` : '';
}
