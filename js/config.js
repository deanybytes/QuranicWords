// Single source of truth for the web app version. sw.js carries the same value in its cache name;
// tools/web/smoke_test.mjs fails if the two drift apart.
export const APP_VERSION = '2.2.3';

export const REPO_URL = 'https://github.com/rmrashahriar/QuranicWords';

/** Path the site is served from: "/" on its own domain, "/QuranicWords/" on GitHub Pages.
 * Derived from this module's own URL (js/config.js lives one level below the site root). */
export const BASE = typeof import.meta !== 'undefined' && import.meta.url.startsWith('http')
  ? new URL('../', import.meta.url).pathname
  : '/';

export const DATA_URLS = {
  index: `${BASE}data/index.json`,
  roots: `${BASE}data/roots.json`,
  meanings: (lang) => `${BASE}data/meanings/${lang}.json`,
  verses: (lang, ch) => `${BASE}data/verses/${lang}/ch_${String(ch).padStart(2, '0')}.json`,
};
