#!/usr/bin/env python3
"""Tells Bing, Yandex and other IndexNow search engines which pages exist, so new and changed
pages are crawled within hours. Needs no account: the key file at the site root (<key>.txt) proves
the site is ours.

    python3 tools/web/indexnow.py            # submit every URL in sitemap*.xml
    python3 tools/web/indexnow.py --dry-run  # only count them

Google does not use IndexNow; it finds the sitemaps through robots.txt (and Search Console).
"""
import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HOST = "quranicwords.vercel.app"
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
    urls = list(dict.fromkeys(u for u in urls if u.startswith(f"https://{HOST}/")))
    print(f"{len(urls)} URLs from the sitemaps; key file https://{HOST}/{key}.txt")
    if "--dry-run" in sys.argv:
        return
    for i in range(0, len(urls), BATCH):
        body = json.dumps({"host": HOST, "key": key, "keyLocation": f"https://{HOST}/{key}.txt",
                           "urlList": urls[i:i + BATCH]}).encode()
        req = urllib.request.Request(ENDPOINT, body, {"Content-Type": "application/json; charset=utf-8"})
        with urllib.request.urlopen(req, timeout=60) as r:
            print(f"submitted {len(urls[i:i + BATCH])} URLs: HTTP {r.status}")


if __name__ == "__main__":
    main()
