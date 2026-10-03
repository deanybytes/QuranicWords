"""Loading (and, when missing, fetching) every input. Downloads are pinned in pins.json by
sha256 so a rebuild can never silently pick up changed upstream data."""
import hashlib
import json
import urllib.request
from collections import defaultdict
from dataclasses import dataclass, field

from . import config

PINS_FILE = config.PIPELINE / "pins.json"


def _sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def _pins():
    return json.loads(PINS_FILE.read_text()) if PINS_FILE.exists() else {}


def fetch(name, url, pin=False):
    """Return the cached path for `name`, downloading it if absent; verify against pins.json."""
    config.CACHE.mkdir(parents=True, exist_ok=True)
    path = config.CACHE / name
    if not path.exists():
        print(f"  fetching {url}")
        with urllib.request.urlopen(url, timeout=300) as r, open(path, "wb") as f:
            f.write(r.read())
    digest = _sha256(path)
    pins = _pins()
    if name in pins:
        if pins[name] != digest:
            raise SystemExit(f"{name}: sha256 {digest} does not match pinned {pins[name]}")
    elif pin:
        pins[name] = digest
        PINS_FILE.write_text(json.dumps(pins, indent=2, sort_keys=True) + "\n")
    else:
        raise SystemExit(f"{name} is not pinned; run `run.py --pin` once to record its checksum")
    return path


@dataclass
class Segment:
    form: str
    coarse: str          # N / V / P
    tags: list           # feature list, first entries are fine POS tags
    lemma: str = None
    root: str = None
    prefix: bool = False
    suffix: bool = False

    @property
    def fine(self):
        return self.tags[0] if self.tags else self.coarse

    def has(self, tag):
        return tag in self.tags

    def feature(self, key):
        for t in self.tags:
            if t.startswith(key + ":"):
                return t.split(":", 1)[1]
        return None


def load_qac(pin=False):
    """{(surah, ayah): {word_index: [Segment, ...]}}"""
    path = fetch("quran-morphology.txt", config.QAC_URL, pin)
    words = defaultdict(lambda: defaultdict(list))
    with open(path, encoding="utf-8") as f:
        for line in f:
            loc, form, coarse, feats = line.rstrip("\n").split("\t")
            s, a, w, _ = map(int, loc.split(":"))
            tags = feats.split("|") if feats else []
            seg = Segment(form=form, coarse=coarse, tags=tags)
            seg.lemma = seg.feature("LEM")
            seg.root = seg.feature("ROOT")
            seg.prefix = "PREF" in tags
            seg.suffix = "SUFF" in tags
            words[(s, a)][w].append(seg)
    return words


def load_gtaf():
    """{lang: {(surah, ayah): [token dict, ...]}} - GTAF word-by-word, token-aligned across langs."""
    out = {}
    for lang, fname in config.GTAF_FILES.items():
        data = json.loads((config.GTAF_DIR / fname).read_text(encoding="utf-8"))["data"]
        out[lang] = {(int(s), int(a)): toks for s, ayahs in data.items() for a, toks in ayahs.items()}
    return out


def load_verse_translations(pin=False):
    """{lang: {(surah, ayah): text}} - one full translation per language (config.VERSE_EDITIONS)."""
    import re
    order = sorted(load_qac(pin).keys())
    out = {}
    for lang, (source, _name) in config.VERSE_EDITIONS.items():
        kind, ident = source.split(":", 1)
        if kind == "ac":
            path = fetch(f"tr_{ident}.json", config.VERSE_URL.format(edition=ident), pin)
            data = json.loads(path.read_text(encoding="utf-8"))["data"]["surahs"]
            out[lang] = {(s["number"], a["numberInSurah"]): a["text"].strip() for s in data for a in s["ayahs"]}
        else:
            path = fetch(f"qc_{ident}.json", config.QURAN_COM_URL.format(id=ident), pin)
            rows = json.loads(path.read_text(encoding="utf-8"))["translations"]
            assert len(rows) == len(order), (lang, len(rows))
            def clean(t):
                t = re.sub(r"<sup[^>]*>.*?</sup>|<[^>]+>", "", t)
                # Inline footnote markers such as [১] or [3] are editorial, not translation.
                t = re.sub(r"\s*\[\s*[0-9০-৯۰-۹٠-٩०-९]+\s*\]", "", t)
                t = re.sub(r"[¹²³⁴⁵⁶⁷⁸⁹⁰]+", "", t)
                return re.sub(r"\s+", " ", t).strip()
            out[lang] = {k: clean(r["text"]) for k, r in zip(order, rows)}
    return out
