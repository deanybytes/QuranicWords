"""
Parse the Quranic Arabic Corpus's public lemma-frequency table (corpus.quran.com/lemmas.jsp,
pages 1..74, fetched to raw/lemma_pages/) into a single ranked list.

Source: The Quranic Arabic Corpus (https://corpus.quran.com), GPL + attribution required.
"""
import html
import json
import re
from pathlib import Path

RAW_DIR = Path(__file__).parent / "raw" / "lemma_pages"
OUT_PATH = Path(__file__).parent / "output" / "lemmas.json"

ROW_RE = re.compile(
    r'<td class="at">(?P<arabic>[^<]*)</td>\s*'
    r'<td><a href="/search\.jsp\?q=lem%3A(?P<lemkey>[^"]*)">(?P<buckwalter>[^<]*)</a></td>\s*'
    r'<td>(?P<freq>\d+)</td>\s*'
    r'<td>(?P<pos>[^<]*)</td>',
    re.MULTILINE
)

def parse_page(page_html: str):
    # Buckwalter transliteration uses "<" for hamza-under-alif (e.g. "<in~"), which appears
    # HTML-escaped as "&lt;" in the raw markup - html.unescape() every captured field, not just
    # the ones that "look like" they need it, since any of arabic/buckwalter/pos could contain
    # escaped punctuation.
    return [
        {
            "arabic": html.unescape(m.group("arabic").strip()),
            "buckwalter": html.unescape(m.group("buckwalter").strip()),
            # The Corpus disambiguates lemmas that share a spelling by part-of-speech (e.g. "maA"
            # is 6 distinct lemmas: relative pronoun, negative particle, interrogative,
            # subordinating conjunction, conditional particle, supplemental particle - each with
            # its own frequency). lemkey (the full "buckwalter+pos%3A..." query string the site
            # itself uses to identify a lemma) is the real unique key, not (arabic, buckwalter).
            "lemkey": html.unescape(m.group("lemkey").strip()),
            "frequency": int(m.group("freq")),
            "pos": html.unescape(m.group("pos").strip()),
        }
        for m in ROW_RE.finditer(page_html)
    ]

def main():
    lemmas = []
    for i in range(1, 75):
        path = RAW_DIR / f"page_{i}.html"
        html = path.read_text(encoding="utf-8")
        rows = parse_page(html)
        if not rows:
            print(f"WARNING: page {i} parsed 0 rows")
        lemmas.extend(rows)

    print(f"Parsed {len(lemmas)} lemma rows from 74 pages")

    seen = set()
    deduped = []
    for l in lemmas:
        key = l["lemkey"]
        if key in seen:
            continue
        seen.add(key)
        deduped.append(l)
    print(f"After de-dup by lemkey: {len(deduped)} unique lemmas (was previously mis-deduped to a lower count by (arabic, buckwalter) alone, which collapsed distinct POS-differentiated lemmas)")

    deduped.sort(key=lambda l: -l["frequency"])

    # The commonly-cited "77,430 total words in the Quran" figure does NOT reconcile against the
    # sum of this lemma-frequency table (54,766 - a ~29% gap), most likely because that figure
    # counts orthographic (space-separated) words while this table counts morphological lemmas,
    # and Arabic frequently attaches clitics (pronouns, prepositions, the definite article) to a
    # host word as extra tokens that don't each get their own lemma-frequency row. Rather than
    # present a coverage percentage against an external figure this data doesn't actually
    # reconcile with, the denominator here is the self-consistent total: the sum of every lemma's
    # frequency in this exact dataset. Coverage bands below mean "% of lemma-tagged occurrences in
    # the Quranic Arabic Corpus's own lemma-frequency table", not "% of all words in the Quran" -
    # documented precisely in docs/CONTENT_SOURCES.md, not silently conflated.
    total_lemma_occurrences = sum(l["frequency"] for l in deduped)
    cumulative = 0
    for rank, l in enumerate(deduped, start=1):
        cumulative += l["frequency"]
        l["rank"] = rank
        l["cumulativeCount"] = cumulative
        pct = cumulative / total_lemma_occurrences * 100
        l["cumulativePercent"] = round(pct, 4)
        if pct <= 25:
            l["coverageBand"] = "0-25"
        elif pct <= 50:
            l["coverageBand"] = "25-50"
        elif pct <= 75:
            l["coverageBand"] = "50-75"
        else:
            l["coverageBand"] = "75-100"

    OUT_PATH.parent.mkdir(exist_ok=True)
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(deduped, f, ensure_ascii=False, indent=2)

    print(f"Wrote {OUT_PATH}")
    print(f"Total lemma occurrences (self-consistent denominator): {total_lemma_occurrences}")
    from collections import Counter
    band_counts = Counter(l["coverageBand"] for l in deduped)
    for band in ["0-25", "25-50", "50-75", "75-100"]:
        print(f"  band {band}: {band_counts[band]} lemmas")

if __name__ == "__main__":
    main()
