#!/usr/bin/env bash
# Publishes the website to GitHub Pages (branch gh-pages of a git remote, default "deanybytes":
# https://deanybytes.github.io/QuranicWords/). The site is base-path aware, so it works at
# https://<owner>.github.io/QuranicWords/.
#
#   tools/web/deploy_pages.sh [remote]
set -euo pipefail
REMOTE="${1:-deanybytes}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SITE="$(mktemp -d)"
trap 'rm -rf "$SITE"' EXIT
"$ROOT/tools/web/build_site.sh" "$SITE"
SHA="$(git -C "$ROOT" rev-parse --short HEAD)"
cd "$SITE"
git init -q -b gh-pages
git add -A
git -c user.name="$(git -C "$ROOT" config user.name)" -c user.email="$(git -C "$ROOT" config user.email)" \
  commit -q -m "Deploy website from ${SHA}"
git push -q --force "$(git -C "$ROOT" remote get-url "$REMOTE")" gh-pages:gh-pages
echo "pushed gh-pages (${SHA}) to ${REMOTE}"
