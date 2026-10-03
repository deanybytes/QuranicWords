"""Golden-word and invariant tests for the content pipeline. Run: pytest tools/pipeline/tests"""
import copy
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import run  # noqa: E402
from qw import arabic, glosses, validate  # noqa: E402


@pytest.fixture(scope="module")
def built():
    assets, legacy_map, report, excluded, words = run.build()
    return assets, legacy_map, report, words


def by_arabic(words, text, category=None):
    key = arabic.pronunciation_key(text)
    hits = [w for w in words if arabic.pronunciation_key(w.arabic) == key and (category is None or w.category == category)]
    assert hits, text
    return max(hits, key=lambda w: w.frequency)


GOLDEN = [
    # arabic, category, {lang: expected substring of meaning}
    ("قَالَ", "VERB", {"en": "said", "ur": "کہا", "tr": "dedi", "fr": "dit"}),
    ("اللَّه", "NOUN", {"en": "Allah", "bn": "আল্লাহ", "in": "Allah"}),
    ("رَبّ", "NOUN", {"en": "Lord", "bn": "রব", "fa": "پروردگار"}),
    ("يَوْم", "NOUN", {"en": "day", "ur": "دن", "hi": "दिन", "in": "hari", "tr": "gün", "fa": "روز", "fr": "jour"}),
    ("أَرْض", "NOUN", {"en": "earth", "ur": "زمین", "in": "bumi"}),
    ("كِتاب", "NOUN", {"en": "Book", "ur": "کتاب"}),
    ("عَذاب", "NOUN", {"en": "punishment"}),
    ("رَسُول", "NOUN", {"en": "Messenger"}),
    ("خَلَقَ", "VERB", {"en": "created", "tr": "yarattı"}),
    ("مِنْ", "PARTICLE", {"en": "from", "fr": "de"}),
    ("فِي", "PARTICLE", {"en": "in", "ur": "میں"}),
]


@pytest.mark.parametrize("text,category,expected", GOLDEN)
def test_golden_meanings(built, text, category, expected):
    w = by_arabic(built[3], text, category)
    for lang, needle in expected.items():
        assert needle.lower() in w.meaning[lang].lower(), (lang, w.meaning[lang])


def test_previously_corrupted_entries_are_fixed(built):
    words = built[3]
    rahma = by_arabic(words, "رَحْمَة", "NOUN")
    assert "gracious" not in rahma.meaning["en"].lower()
    assert "mercy" in rahma.meaning["en"].lower()
    ilm = by_arabic(words, "عِلْم", "NOUN")
    assert "knowledge" in ilm.meaning["en"].lower()
    kana = by_arabic(words, "كَانَ", "VERB")
    assert "কাফির" not in kana.meaning["bn"]


def test_every_word_is_distinct(built):
    words = built[3]
    keys = [(arabic.pronunciation_key(w.arabic), w.category) for w in words]
    assert len(keys) == len(set(keys))


def test_assets_validate(built):
    assert validate.run(built[0]) == []


def _mutate(assets):
    return copy.deepcopy(assets)


def test_validator_catches_duplicate_word(built):
    a = _mutate(built[0])
    a["word_frequency"]["words"].append(a["word_frequency"]["words"][0])
    assert any("duplicate" in e for e in validate.run(a))


def test_validator_catches_fragment_meaning(built):
    a = _mutate(built[0])
    w = next(w for w in a["word_frequency"]["words"] if w["id"].startswith("wn_") and w["frequencyRank"] > 200)
    w["meaning"]["en"] = "and they said to him"
    assert any("fragment" in e for e in validate.run(a))


def test_validator_catches_missing_language(built):
    a = _mutate(built[0])
    del a["word_frequency"]["words"][5]["meaning"]["fr"]
    assert any("meaning languages" in e for e in validate.run(a))


def test_validator_catches_bad_span(built):
    a = _mutate(built[0])
    e = next(e for e in a["exercises"]["exercises"] if e["exerciseType"] == "WORD_INTRO")
    e["content"]["arabicWordEnd"] = len(e["content"]["exampleVerseArabic"]) + 5
    assert any("span" in x for x in validate.run(a))


def test_deterministic(built):
    again = run.build()[0]
    assert run.dump(again["exercises"]) == run.dump(built[0]["exercises"])


def test_transliteration():
    assert arabic.transliterate("كِتاب") == "kitāb"
    assert arabic.transliterate("رَحْمَة") == "raḥmah"
    assert arabic.transliterate("اللَّه") == "Allāh"
    assert arabic.transliterate("صَلَوٰة") == "ṣalāh"


def test_gloss_cleaning():
    assert glosses.clean("(is) the Book", "en", "N") == "Book"
    assert glosses.clean("And they said", "en", "V") == "said"
    assert glosses.clean("de la terre", "fr", "N") == "terre"
    assert glosses.meaning_problem("and him", "en", "NOUN") == "phrase fragment"
