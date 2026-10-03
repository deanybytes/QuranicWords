#!/usr/bin/env bash
# Assembles the static website into a directory (default: build/site) - exactly the files the
# site needs, plus a 404.html copy of the app so GitHub Pages serves deep links (/learn, /quiz...)
# through the client-side router.
#
#   tools/web/build_site.sh [out_dir]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="${1:-$ROOT/build/site}"
rm -rf "$OUT"
mkdir -p "$OUT"
cd "$ROOT"
cp -r index.html manifest.json sw.js favicon.png robots.txt sitemap.xml css js data icons "$OUT"/
cp google*.html "$OUT"/ 2>/dev/null || true
cp index.html "$OUT/404.html"
touch "$OUT/.nojekyll"
echo "site assembled in $OUT ($(du -sh "$OUT" | cut -f1))"
