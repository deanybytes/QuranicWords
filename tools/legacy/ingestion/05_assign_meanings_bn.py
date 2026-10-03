# -*- coding: utf-8 -*-
"""
Assign meaningBn to every lemma using:
  1. Curated top-37 Bangla (hand-verified, same tier as their English overrides).
  2. Root-level Bangla gloss (root_bn_batch1..8.py) - one gloss per matched root, reused across
     every lemma sharing that root, mirroring Quran-bil-Quran's own root-based lexicographic
     approach rather than claiming independent per-lemma precision at this scale.
  3. POS-fallback Bangla for particles/unmatched entries with no root.

meaningBnReviewed is False for every entry here - none of this is independently verified against
a real Bangla source (see docs/CONTENT_SOURCES.md); it's authored, grounded in the sourced English
meaning, not fabricated from nothing, but not source-verified either.
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from root_bn_batch1 import ROOT_BN_1
from root_bn_batch2 import ROOT_BN_2
from root_bn_batch3 import ROOT_BN_3
from root_bn_batch4 import ROOT_BN_4
from root_bn_batch5 import ROOT_BN_5
from root_bn_batch6 import ROOT_BN_6
from root_bn_batch7 import ROOT_BN_7
from root_bn_batch8 import ROOT_BN_8

IN_PATH = Path(__file__).parent / "output" / "lemmas_with_meanings.json"
OUT_PATH = Path(__file__).parent / "output" / "lemmas_final.json"

ROOT_BN = {}
for batch in [ROOT_BN_1, ROOT_BN_2, ROOT_BN_3, ROOT_BN_4, ROOT_BN_5, ROOT_BN_6, ROOT_BN_7, ROOT_BN_8]:
    ROOT_BN.update(batch)

CURATED_BN = {
    "min+pos%3Ap": "থেকে",
    "%7Bll%7Eah+pos%3Apn": "আল্লাহ, একমাত্র সত্য উপাস্য",
    "fiY+pos%3Ap": "মধ্যে",
    "%3Cin%7E+pos%3Aacc": "নিশ্চয়ই, প্রকৃতপক্ষে (জোর দেওয়ার অব্যয়)",
    "EalaY%60+pos%3Ap": "উপর",
    "%7Bl%7Ea*iY+pos%3Arel": "যে, যিনি, যা (সম্বন্ধবাচক সর্বনাম)",
    "laA+pos%3Aneg": "না, নেই",
    "maA+pos%3Arel": "যা, যা কিছু",
    "rab%7E+pos%3An": "রব, প্রতিপালক, প্রভু",
    "%3CilaY%60+pos%3Ap": "দিকে, প্রতি",
    "maA+pos%3Aneg": "না (ক্রিয়া নেতিবাচক করতে)",
    "man+pos%3Arel": "যে (ব্যক্তির জন্য)",
    "%3Cin+pos%3Acond": "যদি",
    "%3Ean+pos%3Asub": "যে (অধীনস্থ বাক্য শুরু করে)",
    "%3Cil%7EaA+pos%3Ares": "ব্যতীত, ছাড়া",
    "*a%60lik+pos%3Adem": "সেটা (পুংলিঙ্গ, দূরবর্তী)",
    "Ean+pos%3Ap": "থেকে, সম্পর্কে",
    "%3EaroD+pos%3An": "পৃথিবী, ভূমি",
    "qad+pos%3Acert": "নিশ্চয়ই, ইতিমধ্যে",
    "%3Ci*aA+pos%3At": "যখন",
    "qawom+pos%3An": "সম্প্রদায়, জাতি",
    "%27aAyap+pos%3An": "নিদর্শন, আয়াত",
    "%3Ean%7E+pos%3Aacc": "যে (নিশ্চয়তাসহ বাক্য শুরু করে)",
    "kul%7E+pos%3An": "সব, প্রত্যেক",
    "lam+pos%3Aneg": "করেনি (অতীত ক্রিয়া নেতিবাচক)",
    "vum%7E+pos%3Aconj": "তারপর, অতঃপর",
    "rasuwl+pos%3An": "রাসূল, দূত",
    "laA+pos%3Apro": "কোরো না (নিষেধ)",
    "yawom+pos%3An": "দিন",
    "Ea*aAb+pos%3An": "শাস্তি, আযাব",
    "ha%60*aA+pos%3Adem": "এটা (পুংলিঙ্গ, নিকটবর্তী)",
    "samaA%5E%27+pos%3An": "আকাশ",
    "nafos+pos%3An": "আত্মা, নিজ সত্তা",
    "%24aYo%27+pos%3An": "বস্তু, জিনিস",
    "%3Eaw+pos%3Aconj": "অথবা",
    "kita%60b+pos%3An": "কিতাব, গ্রন্থ",
    "bayon+pos%3Aloc": "মাঝে",
}

POS_FALLBACK_BN = {
    "Preposition": "একটি সম্বন্ধপদ (উদাহরণ আয়াত দেখুন)",
    "Proper noun": "একটি নির্দিষ্ট নাম",
    "Relative pronoun": "সম্বন্ধবাচক সর্বনাম (\"যে\"/\"যা\")",
    "Negative particle": "নেতিবাচক অব্যয় (\"না\")",
    "Conditional particle": "শর্তসূচক অব্যয় (\"যদি\")",
    "Subordinating conjunction": "অধীনস্থ সংযোজক অব্যয় (\"যে\")",
    "Restriction particle": "সীমাবদ্ধতাসূচক অব্যয় (\"ব্যতীত\")",
    "Demonstrative pronoun": "নির্দেশক সর্বনাম (\"এই\"/\"সেই\")",
    "Particle of certainty": "নিশ্চয়তাসূচক অব্যয় (\"নিশ্চয়ই\")",
    "Time adverb": "সময়সূচক ক্রিয়াবিশেষণ (\"যখন\")",
    "Accusative particle": "জোরসূচক অব্যয় (\"নিশ্চয়ই\"/\"যে\")",
    "Coordinating conjunction": "সংযোজক অব্যয়",
    "Prohibition particle": "নিষেধসূচক অব্যয় (\"কোরো না\")",
    "Location adverb": "স্থানসূচক ক্রিয়াবিশেষণ",
    "Interogative particle": "প্রশ্নসূচক অব্যয়",
    "Vocative particle": "সম্বোধনসূচক অব্যয় (\"হে...\")",
    "Exceptive particle": "ব্যতিক্রমসূচক অব্যয় (\"ব্যতীত\")",
    "Imperative verbal noun": "আদেশসূচক ক্রিয়াবাচক বিশেষ্য",
    "Verb": "একটি ক্রিয়াপদ (উদাহরণ আয়াত দেখুন)",
    "Noun": "একটি বিশেষ্য (উদাহরণ আয়াত দেখুন)",
    "Adjective": "একটি বিশেষণ (উদাহরণ আয়াত দেখুন)",
    "Personal pronoun": "ব্যক্তিবাচক সর্বনাম",
    "Amendment particle": "সংশোধনসূচক অব্যয়",
    "Answer particle": "উত্তরসূচক অব্যয়",
    "Exhortation particle": "উৎসাহসূচক অব্যয়",
    "Future particle": "ভবিষ্যৎসূচক অব্যয়",
    "Inceptive particle": "সূচনাসূচক অব্যয়",
    "Particle of interpretation": "ব্যাখ্যাসূচক অব্যয়",
    "Retraction particle": "প্রত্যাহারসূচক অব্যয়",
    "Supplemental particle": "সম্পূরক অব্যয়",
    "Surprise particle": "বিস্ময়সূচক অব্যয়",
}

def main():
    with open(IN_PATH, encoding="utf-8") as f:
        lemmas = json.load(f)

    counts = {"curated": 0, "root": 0, "fallback": 0, "unmatched_no_bn": 0}
    for l in lemmas:
        if l["curated"] and l["lemkey"] in CURATED_BN:
            l["meaningBn"] = CURATED_BN[l["lemkey"]]
            counts["curated"] += 1
        elif l["root"] and l["root"] in ROOT_BN:
            l["meaningBn"] = ROOT_BN[l["root"]]
            counts["root"] += 1
        elif l["pos"] in POS_FALLBACK_BN:
            l["meaningBn"] = POS_FALLBACK_BN[l["pos"]]
            counts["fallback"] += 1
        else:
            l["meaningBn"] = None
            counts["unmatched_no_bn"] += 1
        l["meaningBnReviewed"] = False

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(lemmas, f, ensure_ascii=False, indent=2)

    print("meaningBn source breakdown:", counts)
    print(f"Wrote {OUT_PATH}")

    missing = [l for l in lemmas if l["meaningBn"] is None]
    if missing:
        print(f"WARNING: {len(missing)} lemmas have no meaningBn at all:")
        for l in missing[:30]:
            print(" ", l["rank"], l["arabic"], l["pos"], l["root"])

if __name__ == "__main__":
    main()
