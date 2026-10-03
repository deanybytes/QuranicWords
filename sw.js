/* QuranicWords service worker.
 * - App shell (HTML/CSS/JS/fonts/icons): cache-first, precached into a versioned cache.
 * - data/*.json: stale-while-revalidate (index + roots precached; verse files cached on first use).
 * - Navigation: network-first, falling back to the cached app shell, then to an offline page.
 * VERSION must match APP_VERSION in js/config.js (checked by tools/web/smoke_test.mjs).
 */
const VERSION = '2.1.0';
const SHELL_CACHE = `qw-shell-${VERSION}`;
const DATA_CACHE = 'qw-data-v3';
// Anything else (including the retired qw-audio-* cache) is deleted on activate.
const KEEP = [SHELL_CACHE, DATA_CACHE];

// Paths are relative to the service worker's scope, so the site works at "/" and under a
// sub-path such as GitHub Pages' "/QuranicWords/".
const SCOPE = new URL(self.registration ? self.registration.scope : self.location.href.replace(/sw\.js$/, ''));
const at = (p) => new URL(p, SCOPE).href;
const SCOPE_PATH = SCOPE.pathname;

const SHELL_FILES = [
  'index.html',
  'manifest.json',
  'favicon.png',
  'css/main.css',
  'css/components.css',
  'css/fonts/ScheherazadeNew-Regular.woff2',
  'css/fonts/ScheherazadeNew-Bold.woff2',
  'css/fonts/AmiriQuran-Regular.woff2',
  'icons/icon-96.png',
  'icons/icon-192.png',
  'icons/icon-512.png',
  'icons/icon-maskable-192.png',
  'icons/icon-maskable-512.png',
  'icons/apple-touch-icon.png',
  'js/boot.js',
  'js/app.js',
  'js/components.js',
  'js/config.js',
  'js/data.js',
  'js/dom.js',
  'js/i18n.js',
  'js/progress.js',
  'js/quiz.js',
  'js/router.js',
  'js/search.js',
  'js/srs.js',
  'js/storage.js',
  'js/ui.js',
  'js/locales/en.js',
  'js/locales/bn.js',
  'js/locales/ur.js',
  'js/locales/hi.js',
  'js/locales/in.js',
  'js/locales/tr.js',
  'js/locales/fa.js',
  'js/locales/fr.js',
  'js/views/dictionary.js',
  'js/views/learn.js',
  'js/views/progressView.js',
  'js/views/quizView.js',
  'js/views/study.js',
];
const DATA_PRECACHE = ['data/index.json', 'data/roots.json'];

const OFFLINE_HTML = '<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Offline — QuranicWords</title><body style="font-family:system-ui,sans-serif;background:#021d14;color:#f3f8f5;display:grid;place-items:center;min-height:100vh;margin:0;text-align:center;padding:16px"><div><h1>You are offline</h1><p>QuranicWords will be available once you reconnect.</p></div></body></html>';

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const shell = await caches.open(SHELL_CACHE);
    await shell.addAll(SHELL_FILES.map((u) => new Request(at(u), { cache: 'reload' })));
    const data = await caches.open(DATA_CACHE);
    await Promise.all(DATA_PRECACHE.map(async (u) => {
      try {
        const res = await fetch(at(u), { cache: 'no-cache' });
        if (res.ok) await data.put(at(u), res);
      } catch { /* data will be cached on first use */ }
    }));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const names = await caches.keys();
    await Promise.all(names.filter((n) => n.startsWith('qw-') && !KEEP.includes(n)).map((n) => caches.delete(n)));
    await self.clients.claim();
  })());
});

async function networkFirstNavigation(request) {
  const shell = await caches.open(SHELL_CACHE);
  try {
    const res = await fetch(request);
    const type = res.headers.get('content-type') || '';
    if (res.ok && type.includes('text/html')) shell.put(at('index.html'), res.clone());
    return res;
  } catch {
    const cached = await shell.match(at('index.html'));
    return cached || new Response(OFFLINE_HTML, { status: 503, headers: { 'Content-Type': 'text/html; charset=utf-8' } });
  }
}

async function cacheFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request, { ignoreSearch: true });
  if (cached) return cached;
  const res = await fetch(request);
  if (res.ok && res.status === 200) cache.put(request, res.clone());
  return res;
}

async function staleWhileRevalidate(event, request) {
  const cache = await caches.open(DATA_CACHE);
  const cached = await cache.match(request, { ignoreSearch: true });
  const network = fetch(request, { cache: 'no-cache' }).then((res) => {
    if (res.ok && res.status === 200) cache.put(request, res.clone());
    return res;
  });
  if (cached) {
    event.waitUntil(network.catch(() => {}));
    return cached;
  }
  return network;
}

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  if (request.mode === 'navigate') {
    event.respondWith(networkFirstNavigation(request));
    return;
  }
  if (!url.pathname.startsWith(SCOPE_PATH)) return;
  const path = url.pathname.slice(SCOPE_PATH.length);
  if (path.startsWith('data/') && path.endsWith('.json')) {
    event.respondWith(staleWhileRevalidate(event, request));
  } else if (/^(css|js|icons)\//.test(path) || path === 'manifest.json' || path === 'favicon.png') {
    event.respondWith(cacheFirst(request, SHELL_CACHE));
  }
});
