#!/usr/bin/env python3
"""Tells Bing, Yandex and other IndexNow search engines which pages exist, so new and changed
pages are crawled within hours. Needs no account: the key file at the site root (<key>.txt) proves
the site is ours. On a project site the key file sits
under /QuranicWords/, which scopes it to the URLs below that path - exactly the site.

    python3 tools/web/indexnow.py            # submit every URL in sitemap*.xml
    python3 tools/web/indexnow.py --dry-run  # only count them

Google does not use IndexNow; submit sitemap.xml in Search Console (a project site's robots.txt is
not at the host root, so crawlers do not read it).
"""
import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HOST = "deanybytes.github.io"
SITE = f"https://{HOST}/QuranicWords"   # a GitHub Pages project site: everything lives under /QuranicWords/
ENDPOINT = "https://api.indexnow.org/indexnow"
BATCH = 9000                                   # the protocol allows up to 10,000 URLs per request


def main():
    keys = [p.stem for p in ROOT.glob("*.txt") if re.fullmatch(r"[0-9a-f]{32}", p.stem)]
    if len(keys) != 1:
        sys.exit(f"expected exactly one <key>.txt in the repository root, found {len(keys)}")
    key = keys[0]
    urls = []
    for sm in sorted(ROOT.glob("sitemap*.xml")):
        urls += re.findall(r"<loc>([^<]+)</loc>", sm.read_text(encoding="utf-8"))
    urls = list(dict.fromkeys(u for u in urls if u.startswith(f"{SITE}/")))
    print(f"{len(urls)} URLs from the sitemaps; key file {SITE}/{key}.txt")
    if "--dry-run" in sys.argv:
        return
    for i in range(0, len(urls), BATCH):
        body = json.dumps({"host": HOST, "key": key, "keyLocation": f"{SITE}/{key}.txt",
                           "urlList": urls[i:i + BATCH]}).encode()
        req = urllib.request.Request(ENDPOINT, body, {"Content-Type": "application/json; charset=utf-8"})
        with urllib.request.urlopen(req, timeout=60) as r:
            print(f"submitted {len(urls[i:i + BATCH])} URLs: HTTP {r.status}")


if __name__ == "__main__":
    main()
