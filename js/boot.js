// Runs before first paint (classic, render-blocking script; CSP forbids inline scripts):
// applies the saved theme and the UI language/direction so the page does not flash or reflow.
(function () {
  var root = document.documentElement;
  var theme = null;
  var lang = null;
  try {
    theme = localStorage.getItem('qw_theme');
    lang = localStorage.getItem('qw_lang');
  } catch (e) { /* storage blocked */ }
  try {
    var q = new URLSearchParams(location.search).get('lang');
    if (q) lang = q;
  } catch (e) { /* ignore */ }
  if (theme !== 'light' && theme !== 'dark') {
    theme = window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }
  root.setAttribute('data-theme', theme);
  var tags = { en: 'en', bn: 'bn', ur: 'ur', hi: 'hi', 'in': 'id', id: 'id', tr: 'tr', fa: 'fa', fr: 'fr' };
  if (lang && tags[lang]) {
    root.setAttribute('lang', tags[lang]);
    root.setAttribute('dir', lang === 'ur' || lang === 'fa' ? 'rtl' : 'ltr');
  }
})();
