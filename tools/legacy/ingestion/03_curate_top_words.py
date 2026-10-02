"""
Hand-verified override for the ~37 highest-frequency lemmas (coverage bands 0-25% and 25-50%).
These dominate the whole curve, so they get individual verification rather than trusting the
automated root-matcher, which is explicitly best-effort for the long tail (see
02_match_roots.py) and got real false positives even in this top slice (e.g. the frozen relative
pronoun "الذي" spuriously matched the rare root "ألل" ["to shine"] by coincidental subsequence).
"""
import json
from pathlib import Path

IN_PATH = Path(__file__).parent / "output" / "lemmas_matched.json"
OUT_PATH = Path(__file__).parent / "output" / "lemmas_matched.json"

# lemkey -> (meaningEn, root_override_or_None, keep_matched_root: bool)
# keep_matched_root=True means trust 02_match_roots.py's root for this entry (verified correct);
# False means force root=None (either a true rootless particle, or a root I'm not confident
# enough in to assert - leaving unmatched is more honest than guessing).
CURATED = {
    "min+pos%3Ap":            ("from", False),
    "%7Bll%7Eah+pos%3Apn":    ("Allah, the one true God", True),
    "fiY+pos%3Ap":            ("in", False),
    "%3Cin%7E+pos%3Aacc":     ("indeed, truly (emphasis particle)", False),
    "EalaY%60+pos%3Ap":       ("on, upon", False),
    "%7Bl%7Ea*iY+pos%3Arel":  ("who, which, that (relative pronoun)", False),
    "laA+pos%3Aneg":          ("no, not", False),
    "maA+pos%3Arel":          ("what, that which", False),
    "rab%7E+pos%3An":         ("Lord, Master, Sustainer", True),
    "%3CilaY%60+pos%3Ap":     ("to, towards", False),
    "maA+pos%3Aneg":          ("not (negates a verb)", False),
    "man+pos%3Arel":          ("who (for people)", False),
    "%3Cin+pos%3Acond":       ("if", False),
    "%3Ean+pos%3Asub":        ("that (introduces a subordinate clause)", False),
    "%3Cil%7EaA+pos%3Ares":   ("except, unless", False),
    "*a%60lik+pos%3Adem":     ("that (masculine, distant)", False),
    "Ean+pos%3Ap":            ("from, about, concerning", False),
    "%3EaroD+pos%3An":        ("earth, land", True),
    "qad+pos%3Acert":         ("indeed, already, certainly", False),
    "%3Ci*aA+pos%3At":        ("when, if (temporal)", False),
    "qawom+pos%3An":          ("people, nation", True),
    "%27aAyap+pos%3An":       ("sign, verse", False),
    "%3Ean%7E+pos%3Aacc":     ("that (introduces a clause with certainty)", False),
    "kul%7E+pos%3An":         ("all, every, each", True),
    "lam+pos%3Aneg":          ("did not (negates a past-tense verb)", False),
    "vum%7E+pos%3Aconj":      ("then, thereafter", False),
    "rasuwl+pos%3An":         ("messenger", True),
    "laA+pos%3Apro":          ("do not (prohibition)", False),
    "yawom+pos%3An":          ("day", True),
    "Ea*aAb+pos%3An":         ("punishment, torment", True),
    "ha%60*aA+pos%3Adem":     ("this (masculine, near)", False),
    "samaA%5E%27+pos%3An":    ("sky, heaven", False),
    "nafos+pos%3An":          ("soul, self", True),
    "%24aYo%27+pos%3An":      ("thing", False),
    "%3Eaw+pos%3Aconj":       ("or", False),
    "kita%60b+pos%3An":       ("book, scripture", True),
    "bayon+pos%3Aloc":        ("between", False),
}

def main():
    with open(IN_PATH, encoding="utf-8") as f:
        lemmas = json.load(f)

    applied = 0
    for l in lemmas:
        entry = CURATED.get(l["lemkey"])
        if entry is None:
            continue
        meaning_en, keep_root = entry
        l["meaningEnCurated"] = meaning_en
        l["curated"] = True
        if not keep_root:
            l["root"] = None
            l["rootMeaning"] = None
            l["rootExampleVerses"] = []
        applied += 1

    for l in lemmas:
        l.setdefault("curated", False)
        l.setdefault("meaningEnCurated", None)

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(lemmas, f, ensure_ascii=False, indent=2)

    print(f"Applied curated overrides to {applied} / {len(CURATED)} expected entries")
    missing = set(CURATED) - {l["lemkey"] for l in lemmas if l["curated"]}
    if missing:
        print("WARNING: lemkeys in CURATED but not found in dataset:", missing)

if __name__ == "__main__":
    main()
