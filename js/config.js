// Single source of truth for the web app version. sw.js carries the same value in its cache name;
// tools/web/smoke_test.mjs fails if the two drift apart.
export const APP_VERSION = '2.0.0';

export const REPO_URL = 'https://github.com/rmrashahriar/QuranicWords';

export const DATA_URLS = {
  index: '/data/index.json',
  roots: '/data/roots.json',
  verses: (ch) => `/data/verses/ch_${String(ch).padStart(2, '0')}.json`,
};
