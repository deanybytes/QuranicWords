#!/usr/bin/env python3
"""Builds the static, crawlable pages for search engines from dataset/ (run build_dataset.py
first): one page per word and per root, chapter and root indexes, and their sitemaps.

    quran-words/index.html, chapter-NN.html, <slug>.html
    quran-roots/index.html, <slug>.html
    sitemap-words.xml, sitemap-roots.xml

The pages are plain HTML + css/seo.css (the site's CSP allows no inline style or script) and
work without JavaScript. Each one links into the web app for learning the word.
"""
import json
from collections import defaultdict
from html import escape
from pathlib import Path
from urllib.parse import quote

from slugs import word_slug

ROOT = Path(__file__).resolve().parents[2]
DATASET = ROOT / "dataset"
SITE = "https://quranicwords.vercel.app"
LASTMOD = "2026-10-03"      # date of the content build these pages describe
LANGS = ["en", "bn", "ur", "hi", "in", "tr", "fa", "fr"]
NATIVE = {"en": "English", "bn": "বাংলা", "ur": "اردو", "hi": "हिन्दी", "in": "Bahasa Indonesia",
          "tr": "Türkçe", "fa": "فارسی", "fr": "Français"}
ENGLISH_NAME = {"en": "English", "bn": "Bangla", "ur": "Urdu", "hi": "Hindi", "in": "Indonesian",
                "tr": "Turkish", "fa": "Persian", "fr": "French"}
BCP47 = {"in": "id"}
RTL = {"ur", "fa"}
CATEGORY = {"particle": "particle", "verb": "verb", "noun": "noun"}


def lang_attrs(lang):
    return f' lang="{BCP47.get(lang, lang)}"' + (' dir="rtl"' if lang in RTL else "")


def ar(text, cls=""):
    c = f' class="{cls}"' if cls else ""
    return f'<span lang="ar" dir="rtl"{c}>{escape(text)}</span>'


def marked(text, span):
    """Text with span [start, end) wrapped in <mark> (spans index the exact stored string)."""
    if not span or span[0] is None or not (0 <= span[0] < span[1] <= len(text)):
        return escape(text)
    s, e = span
    return f"{escape(text[:s])}<mark>{escape(text[s:e])}</mark>{escape(text[e:])}"


def page(*, title, description, path, body, depth=1, jsonld=None, heading_lang="en"):
    up = "../" * depth
    ld = ""
    if jsonld:
        ld = '<script type="application/ld+json">' + json.dumps(jsonld, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/") + "</script>\n"
    return f"""<!doctype html>
<html lang="{heading_lang}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{escape(title)}</title>
<meta name="description" content="{escape(description)}">
<link rel="canonical" href="{SITE}/{path}">
<meta name="theme-color" content="#053827">
<meta name="color-scheme" content="light dark">
<meta property="og:type" content="article">
<meta property="og:site_name" content="QuranicWords">
<meta property="og:title" content="{escape(title)}">
<meta property="og:description" content="{escape(description)}">
<meta property="og:url" content="{SITE}/{path}">
<meta property="og:image" content="{SITE}/icons/icon-512.png">
<meta name="twitter:card" content="summary">
<link rel="icon" type="image/png" href="{up}favicon.png">
<link rel="stylesheet" href="{up}css/seo.css">
{ld}</head>
<body>
<header class="bar"><div class="bar-inner">
<a class="brand" href="{up}"><img src="{up}icons/icon-96.png" alt="" width="32" height="32">QuranicWords</a>
<nav class="bar-nav" aria-label="Site"><a href="{up}quran-words/">Words</a><a href="{up}quran-roots/">Roots</a><a class="cta" href="{up}learn">Start learning</a></nav>
</div></header>
<main class="wrap">
{body}
</main>
<footer class="foot"><div class="wrap foot-inner">
<p>QuranicWords: learn the vocabulary of the Qur'an word by word. Free, open source (GPL-3.0), no ads, no tracking.</p>
<p><a href="{up}">Web app</a> · <a href="{up}quran-words/">All words</a> · <a href="{up}quran-roots/">All roots</a> · <a href="https://github.com/deanybytes/QuranicWords/tree/main/dataset">Open dataset</a> · <a href="https://github.com/deanybytes/QuranicWords/releases/latest">Android app</a> · <a href="{up}privacy.html">Privacy</a></p>
</div></footer>
</body>
</html>
"""


def crumbs(items, up):
    parts = [f'<a href="{up}">Home</a>']
    for label, href in items[:-1]:
        parts.append(f'<a href="{href}">{label}</a>')
    parts.append(f'<span aria-current="page">{items[-1][0]}</span>')
    return '<nav class="crumbs" aria-label="Breadcrumb">' + ' <span aria-hidden="true">›</span> '.join(parts) + "</nav>"


def breadcrumb_ld(items):
    return {"@type": "BreadcrumbList", "itemListElement": [
        {"@type": "ListItem", "position": i + 1, "name": name, "item": url} for i, (name, url) in enumerate(items)]}


def word_page(w, ctx):
    slug = word_slug(w["id"], w["transliteration"])
    path = f"quran-words/{slug}.html"
    en = w["meaning"]["en"]
    ch = w["curriculum"]["chapter"]
    ch_title = ctx["chapters"][ch]["title"]["en"]
    cat = CATEGORY[w["category"]]
    tl = w["transliteration"]
    name = f"{w['arabic']} ({tl})" if tl else w["arabic"]
    pos_en = (w["part_of_speech"] or {}).get("en")
    sense = w["senses"]["en"][0]
    v = ctx["verses"][sense["verse"]]
    root_html = ""
    if w["root"]:
        r = ctx["roots"][w["root"]]
        root_html = f'<a href="../quran-roots/{r["slug"]}.html">{ar(w["root"], "root")}</a>'

    others = [f"{ENGLISH_NAME[l]} {w['meaning'][l]}" for l in ("bn", "ur", "hi", "in", "tr", "fa", "fr")]
    title = f"{name} meaning: {en} – Qur'anic Arabic {cat} | QuranicWords"
    desc = (f"{name}: {en}. Qur'anic Arabic {cat}"
            + (f" from the root {w['root']}" if w["root"] else "")
            + f", used {w['occurrences']:,} times in the Qur'an. Meanings: " + ", ".join(others)
            + f". Example: Qur'an {sense['verse']}.")

    facts = [("Meaning", escape(en)), ("Type", escape(pos_en or cat.capitalize()))]
    if w["root"]:
        facts.append(("Root", root_html))
    vf = w["verb_form"]
    if vf:
        bits = [escape(vf["form"])] if vf.get("form") else []
        for label, key in (("past", "past"), ("present", "present"), ("verbal noun", "masdar")):
            if vf.get(key):
                bits.append(f"{label} {ar(vf[key])}")
        facts.append(("Verb form", " · ".join(bits)))
    facts += [("In the Qur'an", f"{w['occurrences']:,} times"), ("Frequency rank", f"#{w['rank']:,} of {ctx['count']:,}"),
              ("Curriculum", f'<a href="chapter-{ch:02d}.html">Chapter {ch}: {escape(ch_title)}</a>, section {w["curriculum"]["section"]}, lesson {w["curriculum"]["lesson"]}')]
    facts_html = "".join(f"<div><dt>{k}</dt><dd>{val}</dd></div>" for k, val in facts)

    rows = []
    for l in LANGS:
        senses = w["senses"][l]
        extra = ""
        if len(senses) > 1:
            extra = '<ul class="senses">' + "".join(
                f'<li><span{lang_attrs(l)}>{escape(s["meaning"])}</span> <span class="ref">{escape(ctx["verses"][s["verse"]]["reference"])}</span></li>'
                for s in senses) + "</ul>"
        rows.append(f'<tr><th scope="row" lang="{BCP47.get(l, l)}">{NATIVE[l]}</th><td><span{lang_attrs(l)}>{escape(w["meaning"][l])}</span>{extra}</td></tr>')

    trs = []
    for l in LANGS:
        same = next((s for s in w["senses"][l] if s["verse"] == sense["verse"]), None)
        span = same["translation_span"] if same else None
        trs.append(f'<div class="tr"><span class="tr-lang">{NATIVE[l]}</span><p{lang_attrs(l)}>{marked(v["translation"][l], span)}</p></div>')

    related = [x for x in ctx["by_root"].get(w["root"], []) if x["id"] != w["id"]] if w["root"] else []
    related_html = ""
    if related:
        related_html = ('<section><h2>Other words from the root ' + ar(w["root"]) + '</h2><ul class="chips">'
                        + "".join(f'<li><a href="{word_slug(x["id"], x["transliteration"])}.html">{ar(x["arabic"])} <span>{escape(x["meaning"]["en"])}</span></a></li>' for x in related[:24])
                        + "</ul></section>")
    i = ctx["index"][w["id"]]
    nav = []
    if i > 0:
        p = ctx["words"][i - 1]
        nav.append(f'<a rel="prev" href="{word_slug(p["id"], p["transliteration"])}.html">← {ar(p["arabic"])} {escape(p["meaning"]["en"])}</a>')
    if i + 1 < len(ctx["words"]):
        n = ctx["words"][i + 1]
        nav.append(f'<a rel="next" href="{word_slug(n["id"], n["transliteration"])}.html">{ar(n["arabic"])} {escape(n["meaning"]["en"])} →</a>')

    body = f"""{crumbs([("Qur'anic words", "./"), (f"Chapter {ch}", f"chapter-{ch:02d}.html"), (escape(w['arabic']), "")], "../")}
<article>
<header class="word-head">
<h1>{ar(w["arabic"], "big")} <span class="h1-text">{escape(tl or "")}{" – " if tl else ""}“{escape(en)}”</span></h1>
<p class="lede">A Qur'anic Arabic {cat}{f" from the root {ar(w['root'])}" if w["root"] else ""} that occurs <strong>{w["occurrences"]:,} times</strong> in the Qur'an. It is word #{w["rank"]:,} in QuranicWords' frequency-ordered curriculum.</p>
<p class="actions"><a class="btn" href="../dictionary?q={quote(w["arabic"])}">Learn it in the app</a> <a class="btn ghost" href="../learn">Start the course</a></p>
</header>
<dl class="facts">{facts_html}</dl>
<section><h2>Meaning in 8 languages</h2>
<table class="meanings"><tbody>{"".join(rows)}</tbody></table></section>
<section><h2>In the Qur'an: {escape(v["reference"])}</h2>
<p class="ayah" lang="ar" dir="rtl">{marked(v["arabic"], sense["arabic_span"])}</p>
<div class="trs">{"".join(trs)}</div>
<p class="note">The complete ayah, with the word highlighted. Translations: {escape(", ".join(ctx["translators"][l] for l in LANGS))}.</p>
</section>
{related_html}
<nav class="pager" aria-label="Next and previous words">{"".join(nav)}</nav>
</article>"""
    ld = {"@context": "https://schema.org", "@graph": [
        {"@type": "DefinedTerm", "name": w["arabic"], "alternateName": [x for x in (tl, en) if x],
         "description": f"{en} ({cat})", "termCode": w["id"], "url": f"{SITE}/{path}", "inLanguage": "ar",
         "inDefinedTermSet": {"@type": "DefinedTermSet", "name": "QuranicWords – vocabulary of the Qur'an", "url": f"{SITE}/quran-words/"}},
        breadcrumb_ld([("QuranicWords", f"{SITE}/"), ("Qur'anic words", f"{SITE}/quran-words/"),
                       (f"Chapter {ch}: {ch_title}", f"{SITE}/quran-words/chapter-{ch:02d}.html"), (w["arabic"], f"{SITE}/{path}")])]}
    return path, page(title=title, description=desc, path=path, body=body, jsonld=ld)


def word_list(words):
    return '<ul class="wordlist">' + "".join(
        f'<li><a href="{word_slug(w["id"], w["transliteration"])}.html">{ar(w["arabic"])}'
        f'<span class="m">{escape(w["meaning"]["en"])}</span><span class="n">{w["occurrences"]:,}×</span></a></li>' for w in words) + "</ul>"


def chapter_page(ch, words, ctx):
    c = ctx["chapters"][ch]
    t = c["title"]["en"]
    path = f"quran-words/chapter-{ch:02d}.html"
    native = " · ".join(f'<span{lang_attrs(l)}>{escape(c["title"][l])}</span>' for l in LANGS if l != "en")
    body = f"""{crumbs([("Qur'anic words", "./"), (f"Chapter {ch}", "")], "../")}
<h1>Chapter {ch}: {escape(t)}</h1>
<p class="lede">{len(words):,} Qur'anic Arabic words, in the order QuranicWords teaches them. Together they make up {c["coverage_percent"]}% of the words of the Qur'an.</p>
<p class="native">{native}</p>
{word_list(words)}"""
    desc = f"Chapter {ch} of the QuranicWords curriculum: {len(words):,} Qur'anic Arabic words ({t}) with their meanings in 8 languages, roots and verse examples."
    ld = {"@context": "https://schema.org", "@type": "CollectionPage", "name": f"Chapter {ch}: {t}", "url": f"{SITE}/{path}"}
    return path, page(title=f"Chapter {ch}: {t} – {len(words):,} Qur'anic Arabic words | QuranicWords",
                      description=desc, path=path, body=body, jsonld=ld)


def words_index(ctx):
    path = "quran-words/"
    items = "".join(
        f'<li><a href="chapter-{n:02d}.html"><strong>Chapter {n}: {escape(c["title"]["en"])}</strong>'
        f'<span>{c["words"]:,} words · {c["coverage_percent"]}% of the Qur\'an</span></a></li>'
        for n, c in sorted(ctx["chapters"].items()))
    top = ctx["words"][:60]
    body = f"""{crumbs([("Qur'anic words", "")], "../")}
<h1>The vocabulary of the Qur'an, word by word</h1>
<p class="lede">{ctx["count"]:,} Arabic words that make up {ctx["coverage"]}% of the Qur'an's text, ordered by how often they occur. Each word has its meaning in English, Bangla, Urdu, Hindi, Indonesian, Turkish, Persian and French, its root and grammar, and a complete ayah where it is used.</p>
<ul class="cards">{items}</ul>
<h2>The 60 most frequent words</h2>
{word_list(top)}"""
    desc = (f"All {ctx['count']:,} words of the Qur'an's core vocabulary ({ctx['coverage']}% of its text) with meanings in "
            "English, Bangla, Urdu, Hindi, Indonesian, Turkish, Persian and French, roots and verse examples.")
    ld = {"@context": "https://schema.org", "@type": "DefinedTermSet", "name": "QuranicWords – vocabulary of the Qur'an",
          "url": f"{SITE}/{path}", "inLanguage": "ar", "description": desc}
    return path + "index.html", page(title=f"Qur'anic Arabic vocabulary: {ctx['count']:,} words with meanings in 8 languages | QuranicWords",
                                     description=desc, path=path, body=body, jsonld=ld)


def root_page(r, ctx):
    path = f"quran-roots/{r['slug']}.html"
    ws = ctx["by_root"][r["root"]]
    meanings = ", ".join(dict.fromkeys(w["meaning"]["en"] for w in ws))
    items = "".join(
        f'<li><a href="../quran-words/{word_slug(w["id"], w["transliteration"])}.html">{ar(w["arabic"])}'
        f'<span class="m">{escape(w["meaning"]["en"])}</span><span class="n">{w["occurrences"]:,}×</span></a></li>' for w in ws)
    tbl = "".join(
        f'<tr><td>{ar(w["arabic"])}</td>' + "".join(f'<td{lang_attrs(l)}>{escape(w["meaning"][l])}</td>' for l in LANGS) + "</tr>" for w in ws)
    head = "".join(f'<th scope="col"{lang_attrs(l)}>{NATIVE[l]}</th>' for l in LANGS)
    body = f"""{crumbs([("Qur'anic roots", "./"), (ar(r["root"]), "")], "../")}
<h1>Root {ar(r["root"], "big")} <span class="h1-text">({escape(r["slug"])})</span></h1>
<p class="lede">{r["word_count"]} Qur'anic {"word comes" if r["word_count"] == 1 else "words come"} from this root, used <strong>{r["occurrences"]:,} times</strong> in the Qur'an. Meanings: {escape(meanings)}.</p>
<ul class="wordlist">{items}</ul>
<section><h2>Meanings in 8 languages</h2><div class="scroll"><table class="grid"><thead><tr><th scope="col">Arabic</th>{head}</tr></thead><tbody>{tbl}</tbody></table></div></section>"""
    desc = (f"Qur'anic root {r['root']} ({r['slug']}): {r['word_count']} words, {r['occurrences']:,} occurrences in the Qur'an. "
            f"Meanings: {meanings[:120]}. With translations in 8 languages.")
    ld = {"@context": "https://schema.org", "@graph": [
        {"@type": "DefinedTerm", "name": r["root"], "alternateName": r["slug"], "description": f"Arabic root: {meanings[:200]}",
         "url": f"{SITE}/{path}", "inDefinedTermSet": {"@type": "DefinedTermSet", "name": "Roots of the Qur'an", "url": f"{SITE}/quran-roots/"}},
        breadcrumb_ld([("QuranicWords", f"{SITE}/"), ("Qur'anic roots", f"{SITE}/quran-roots/"), (r["root"], f"{SITE}/{path}")])]}
    return path, page(title=f"Root {r['root']} ({r['slug']}) in the Qur'an – {r['word_count']} words, {r['occurrences']:,} uses | QuranicWords",
                      description=desc, path=path, body=body, jsonld=ld)


def roots_index(ctx):
    path = "quran-roots/"
    items = "".join(
        f'<li><a href="{r["slug"]}.html">{ar(r["root"])}<span class="m">{escape(", ".join(dict.fromkeys(w["meaning"]["en"] for w in ctx["by_root"][r["root"]][:2])))}</span>'
        f'<span class="n">{r["occurrences"]:,}×</span></a></li>' for r in ctx["root_list"])
    body = f"""{crumbs([("Qur'anic roots", "")], "../")}
<h1>Roots of the Qur'an</h1>
<p class="lede">{len(ctx["root_list"]):,} Arabic roots behind the Qur'an's core vocabulary, ordered by how often their words occur. Open a root to see every word built from it, with meanings in 8 languages.</p>
<ul class="wordlist">{items}</ul>"""
    desc = f"All {len(ctx['root_list']):,} Arabic roots of the Qur'an's core vocabulary with their words, frequencies and meanings in 8 languages."
    return path + "index.html", page(title=f"Qur'anic Arabic roots: {len(ctx['root_list']):,} roots and their words | QuranicWords",
                                     description=desc, path=path, body=body,
                                     jsonld={"@context": "https://schema.org", "@type": "DefinedTermSet", "name": "Roots of the Qur'an", "url": f"{SITE}/{path}"})


def sitemap(urls):
    rows = "".join(f"  <url><loc>{SITE}/{u}</loc><lastmod>{LASTMOD}</lastmod><priority>{p}</priority></url>\n" for u, p in urls)
    return f'<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n{rows}</urlset>\n'


def main():
    load = lambda n: json.loads((DATASET / n).read_text(encoding="utf-8"))
    words, roots, verses, meta = load("words.json"), load("roots.json"), load("verses.json"), load("metadata.json")
    by_root = defaultdict(list)
    for w in words:
        if w["root"]:
            by_root[w["root"]].append(w)
    ctx = {
        "words": words, "index": {w["id"]: i for i, w in enumerate(words)}, "count": len(words),
        "coverage": meta["counts"]["coverage_percent"], "translators": meta["translations"],
        "chapters": {c["chapter"]: c for c in meta["chapters"]},
        "verses": {v["verse"]: v for v in verses}, "roots": {r["root"]: r for r in roots},
        "root_list": roots, "by_root": by_root,
    }
    out = {}
    for w in words:
        p, html = word_page(w, ctx)
        out[p] = html
    by_ch = defaultdict(list)
    for w in words:
        by_ch[w["curriculum"]["chapter"]].append(w)
    for ch, ws in by_ch.items():
        p, html = chapter_page(ch, ws, ctx)
        out[p] = html
    p, html = words_index(ctx)
    out[p] = html
    for r in roots:
        p, html = root_page(r, ctx)
        out[p] = html
    p, html = roots_index(ctx)
    out[p] = html

    for d in ("quran-words", "quran-roots"):
        target = ROOT / d
        target.mkdir(exist_ok=True)
        for old in target.glob("*.html"):       # drop pages of words that no longer exist
            if f"{d}/{old.name}" not in out:
                old.unlink()
    for p, html in out.items():
        (ROOT / p).write_text(html, encoding="utf-8")

    word_urls = [("quran-words/", "0.8")] + [(f"quran-words/chapter-{n:02d}.html", "0.7") for n in sorted(by_ch)] + \
                [(f"quran-words/{word_slug(w['id'], w['transliteration'])}.html", "0.6") for w in words]
    root_urls = [("quran-roots/", "0.7")] + [(f"quran-roots/{r['slug']}.html", "0.5") for r in roots]
    (ROOT / "sitemap-words.xml").write_text(sitemap(word_urls), encoding="utf-8")
    (ROOT / "sitemap-roots.xml").write_text(sitemap(root_urls), encoding="utf-8")
    size = sum(len(h.encode()) for h in out.values())
    print(f"seo pages: {len(out)} files ({size // 1024 // 1024} MB), sitemaps: {len(word_urls)} + {len(root_urls)} urls")


if __name__ == "__main__":
    main()
