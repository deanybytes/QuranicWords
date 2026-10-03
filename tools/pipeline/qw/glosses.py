"""Dictionary-form meanings for verbs and nouns, derived from the GTAF word-by-word gloss of the
lemma's own occurrences. Only occurrences where the Qur'anic word *is* the lemma (no attached
conjunction/preposition/pronoun) are trusted, so a gloss can never be a neighbouring word or a
phrase fragment - the failure mode of the previous content."""
import re
import unicodedata
from collections import Counter

LATIN_LANGS = {"en", "in", "tr", "fr"}

# Leading words that are translations of something other than the lemma itself.
LEADING_STRIP = {
    "en": ["the ", "a ", "an "],
    "fr": ["de la ", "de l'", "de l’", "le ", "la ", "les ", "l'", "l’", "un ", "une ", "des ", "du ", "de ", "d'", "d’"],
    "tr": ["bir "],
}
# Nouns/adjectives that GTAF renders as a relative clause ("yang taat" = "who is obedient"),
# or with an indefinite ("کوئی پناہ" = "some refuge").
NOUN_LEADING = {
    "in": ["yang "],
    "ur": ["کوئی ", "ان کی ", "ان کے ", "ان کا ", "اس کی ", "اس کے ", "اس کا ", "جو ", "کچھ ", "ایک "],
    "hi": ["कोई ", "उनकी ", "उनके ", "उनका ", "उसकी ", "उसके ", "उसका ", "जो ", "कुछ ", "एक "],
    "bn": ["কোনো "],
    "fa": ["یک "],
}
NOUN_TRAILING = {"fa": [" را"]}
# Subject pronouns Urdu/Hindi word-by-word put after the verb ("پھسل گئے تم").
VERB_TRAILING = {
    "ur": [" تم", " ہم", " وہ", " تو", " ہم نے", " انہوں نے", " اس نے", " تم نے", " میں نے", " اسے", " انہیں", " تمہیں", " ہمیں"],
    "hi": [" तुम", " हम", " वे", " वह", " मैं", " तू", " हमने", " उन्होंने", " उसने", " तुमने", " मैंने", " उसे", " उन्हें", " तुम्हें", " हमें"],
}
# Word-by-word glosses sometimes list alternatives ("مقبروں میں/ قبروں میں"); keep the first.
ALTERNATIVE_SEPARATORS = re.compile(r"\s*(?:۔|/|؛)\s*")
FRENCH_ELISION = re.compile(r"(?i)(?:\b(?:qu|l|d|j|n|s|c|m|t|jusqu|lorsqu|puisqu))['’]$")
# A "gloss" that is only one of these is a function word the token borrowed from context.
STOPWORDS = {
    "en": {"and", "so", "then", "the", "of", "to", "in", "is", "it", "that", "which", "who", "not", "a"},
    "fr": {"et", "de", "le", "la", "les", "à", "en", "que", "qui", "ne", "pas", "est"},
    "bn": {"এবং", "আর", "ও", "যে", "না", "কি"},
    "ur": {"اور", "کہ", "جو", "نہ", "سے", "میں", "کو", "ہے"},
    "hi": {"और", "कि", "जो", "न", "से", "में", "को", "है"},
    "in": {"dan", "yang", "itu", "di", "ke", "dari", "tidak"},
    "tr": {"ve", "ki", "bu", "o", "ile", "de", "da", "*"},
    "fa": {"و", "که", "از", "به", "در", "را", "این", "آن"},
}
# Languages whose case/possessive endings attach to the stem (দিন/দিনে, gün/günü): inflected
# variants pool their votes into the shared base form.
SUFFIXING = {"bn", "hi", "tr", "ur", "fa", "in"}
VERB_PRONOUNS = {
    "en": ["he ", "it ", "she ", "they ", "you ", "we ", "i "],
    "fr": ["il ", "elle ", "ils ", "elles ", "vous ", "nous ", "tu ", "je ", "on ", "j'", "j’"],
    "bn": ["সে ", "তারা ", "তোমরা ", "তুমি ", "আমরা ", "আমি ", "তিনি "],
    "ur": ["وہ ", "تم ", "ہم ", "میں ", "تو "],
    "hi": ["वह ", "वे ", "वो ", "ये ", "तुम ", "हम ", "मैं ", "तू ", "आप "],
    "in": ["dia ", "mereka ", "kamu ", "kami ", "kita ", "engkau ", "aku "],
    "fa": ["او ", "آنان ", "آنها ", "شما ", "ما ", "من ", "تو "],
}
# Translations of a leading وَ / فَ (and, so, then) - stripped when the occurrence carries one.
CONJUNCTIONS = {
    "en": ["and ", "so ", "then ", "but "],
    "fr": ["et ", "puis ", "alors ", "donc ", "or "],
    "bn": ["এবং ", "আর ", "অতঃপর ", "তারপর ", "সুতরাং ", "অতএব ", "তখন ", "ও "],
    "ur": ["اور ", "پس ", "پھر ", "تو "],
    "hi": ["और ", "फिर ", "तो ", "अतः "],
    "in": ["dan ", "maka ", "lalu ", "kemudian "],
    "tr": ["ve ", "sonra ", "artık ", "böylece "],
    "fa": ["و ", "پس ", "سپس "],
}
# Words of the surrounding clause that word-by-word glosses fold into a content word ("but",
# "if", "when", "who"): never part of a verb's or noun's own meaning. Not applied to function
# words, whose meaning may legitimately be such a word ("آنان که" = those who).
CLAUSE_LEADING = {
    "en": ["but ", "if ", "when ", "indeed ", "surely "],
    "fr": ["mais ", "si ", "quand ", "lorsque ", "certes "],
    "bn": ["কিন্তু ", "অথচ ", "তাই ", "ফলে ", "যদি ", "যখন ", "নিশ্চয়ই ", "নিশ্চয় ", "অবশ্যই ", "তবে "],
    "ur": ["اگر ", "جب ", "یقیناً ", "یقینا ", "بیشک ", "بے شک "],
    "hi": ["अगर ", "जब ", "बेशक ", "यक़ीनन ", "यकीनन "],
    "in": ["tetapi ", "jika ", "ketika ", "sungguh ", "sesungguhnya "],
    "tr": ["fakat ", "eğer ", "şüphesiz ", "muhakkak "],
    "fa": ["اگر ", "چون ", "هر گاه ", "همانا ", "بی گمان ", "بی‌گمان "],
}
# A verb's subject or object that is not part of the verb itself: the relative "who" from a
# preceding الَّذِينَ, a named subject, or an object pronoun ("তাকে বাঁচালেন" = saved him).
VERB_CONTEXT_LEADING = {
    "en": ["allah ", "who ", "which ", "that "],
    "fr": ["qui ", "que ", "qu'", "qu’", "allah "],
    "bn": ["যারা ", "যে ", "যা ", "তাকে ", "তাদেরকে ", "তাদের ", "তোমাদেরকে ", "তোমাকে ", "আমাকে ", "আমাদেরকে ", "আল্লাহ "],
    "ur": ["انہوں نے ", "اس نے ", "ہم نے ", "تم نے ", "میں نے ", "آپ نے ", "جس نے ", "جنہوں نے ", "جو ", "اللہ "],
    "hi": ["उसने ", "उन्होंने ", "हमने ", "तुमने ", "मैंने ", "आपने ", "जिसने ", "जिन्होंने ", "जो ", "अल्लाह "],
    "in": ["yang ", "allah "],
    "tr": ["biz ", "siz ", "onlar ", "ben ", "sen ", "o ", "allah "],
    "fa": ["که ", "تا ", "آن را ", "او را ", "آنها را ", "خدا ", "الله "],
}
VERB_CONTEXT_LEADING["bn"] += ["তা "]
VERB_CONTEXT_LEADING["ur"] += ["کہ ", "ان دونوں نے ", "انہوں "]
VERB_CONTEXT_LEADING["hi"] += ["कि "]
# Trailing words that belong to the clause, not the word: a verb's object ("increased him",
# "بمیراند او را") and a noun's case postposition ("ज़मीन में" = in the earth).
VERB_CONTEXT_TRAILING = {
    "en": [" him", " it", " them", " us", " me", " her", " on him", " to him", " upon him"],
    "bn": [" তাকে", " তা", " তাদেরকে", " তাদের", " তোমাদেরকে", " তোমাকে", " আমাকে"],
    "ur": [" اس کو", " ان کو", " تم کو", " ہم کو", " مجھ کو", " تجھ کو", " اسے", " انہیں", " اس پر", " ان پر"],
    "hi": [" उसको", " उनको", " तुमको", " हमको", " मुझको", " उसे", " उन्हें", " उस पर", " उन पर"],
    "fa": [" او را", " آن را", " آنها را", " ایشان را", " شما را", " ما را", " با او"],
    "in": [" mereka", " dia", " kamu", " kami"],
    "tr": [" da", " de"],
}
NOUN_CONTEXT_TRAILING = {
    "ur": [" میں", " کو", " سے", " پر", " کے", " کی", " کا", " نے"],
    "hi": [" में", " को", " से", " पर", " के", " की", " का", " ने"],
    "tr": [" da", " de", " ile"],
}
# Participles and other nouns glossed with the clause around them ("mereka kekal" = they abide,
# "o inek" = that cow, "qui guide" = who guides). Urdu ہم is left alone: ہم عمر, ہم نام are words.
NOUN_CONTEXT_LEADING = {
    "en": ["who ", "which "],
    "fr": ["qui ", "que "],
    "bn": ["তোমরা ", "তারা ", "আমরা ", "আমি ", "সে ", "যারা ", "তা "],
    "ur": ["کہ ", "وہ ", "تم ", "ان دونوں "],
    "hi": ["कि ", "वो ", "वह ", "वे "],
    "in": ["mereka ", "dia ", "kami ", "kamu "],
    "tr": ["o ", "biz ", "siz ", "onlar "],
    "fa": ["آن ", "این ", "که ", "تا ", "من ", "او ", "آنان ", "هر گونه "],
}
NOUN_CONTEXT_LEADING["ur"] += ["اللہ کے ", "اللہ کی ", "اللہ کا "]
NOUN_CONTEXT_LEADING["hi"] += ["अल्लाह के ", "अल्लाह की ", "अल्लाह का "]
_PUNCT = " \t,.;:!?\"'“”‘’«»…-–—()[]{}"


def _strip_leading(t, words):
    changed = True
    while changed:
        changed = False
        low = t.lower()
        for w in words:
            if low.startswith(w) and len(t) > len(w) + 1:
                t = t[len(w):].lstrip()
                changed = True
                break
    return t


# Attached-pronoun translations (possessive on nouns, object on verbs), stripped in the
# last-resort tier for lemmas that never occur without one.
PRONOUN_LEADING = {
    "en": ["their ", "his ", "her ", "your ", "our ", "my ", "its "],
    "fr": ["leurs ", "leur ", "son ", "sa ", "ses ", "votre ", "vos ", "notre ", "nos ", "mon ", "ma ",
           "mes ", "ton ", "ta ", "tes ", "les ", "lui ", "vous ", "nous ", "te ", "me ", "l'", "l’"],
    "bn": ["তাদেরকে ", "তোমাদেরকে ", "আমাদেরকে ", "তাদের ", "তোমাদের ", "আমাদের ", "তাঁর ", "তার ",
           "তোমার ", "আমার ", "তাকে ", "আমাকে ", "তোমাকে "],
    "ur": ["ان کے ", "ان کی ", "ان کا ", "اس کے ", "اس کی ", "اس کا ", "تمہارے ", "تمہاری ", "تمہارا ",
           "ہمارے ", "ہماری ", "ہمارا ", "میرے ", "میری ", "میرا ", "اپنے ", "اپنی ", "انہیں ", "ان کو ",
           "اسے ", "اس کو ", "تمہیں ", "تم کو ", "ہمیں ", "مجھے "],
    "hi": ["उनके ", "उनकी ", "उनका ", "उसके ", "उसकी ", "उसका ", "तुम्हारे ", "तुम्हारी ", "तुम्हारा ",
           "हमारे ", "हमारी ", "हमारा ", "मेरे ", "मेरी ", "मेरा ", "अपने ", "अपनी ", "उन्हें ", "उसे ",
           "तुम्हें ", "हमें ", "मुझे "],
}
_HI_POSS = ["उसकी", "उसके", "उसका", "उनके", "उनकी", "उनका", "अपनी", "अपना", "अपने", "तुम्हारा", "तुम्हारे",
            "तुम्हारी", "हमारा", "हमारे", "हमारी", "मेरा", "मेरे", "मेरी", "उन्हें", "उसे", "तुम्हें", "हमें", "मुझे"]
_UR_POSS = ["اس کے", "اس کی", "اس کا", "ان کے", "ان کی", "ان کا", "اپنا", "اپنی", "اپنے", "تمہارا", "تمہارے",
            "تمہاری", "ہمارا", "ہمارے", "ہماری", "میرا", "میرے", "میری", "انہیں", "اسے", "تمہیں", "ہمیں", "مجھے"]
PRONOUN_TRAILING = {
    "en": [" you", " them", " him", " it", " us", " me", " her", " of them", " of it"],
    "in": [" mereka", " kamu", " kami", " kita", " kalian", " dia", " engkau"],
    "hi": [" " + w for w in _HI_POSS] + [" के", " की", " का", " से", " है", " हैं"],
    "ur": [" " + w for w in _UR_POSS] + [" کے", " کی", " کا", " سے", " ہے", " ہیں"],
    "fa": [" را", " او", " آن", " آنها", " آنان", " شما", " ما", " من", " وی", " خود", " ایشان", " است"],
    "bn": [" তাদের", " তার", " তোমাদের", " আমাদের"],
}
# Pronouns written as part of the word (tongkatnya, عصایش).
PRONOUN_ATTACHED = {
    "in": ["Nya", "nya", "mu", "ku"],
    "fa": ["هایشان", "هایتان", "هایمان", "شان", "تان", "مان", "یش"],
}

TRAILING_COPULA = {"hi": [" है", " हैं"], "ur": [" ہے", " ہیں"], "fa": [" است"], "bn": [" হয়"]}
# GTAF's Persian is typed with Urdu/Arabic letter forms; fold to standard Persian orthography.
PERSIAN_FOLD = str.maketrans({"ھ": "ه", "ہ": "ه", "ۀ": "ه", "ي": "ی", "ك": "ک", "ۃ": "ه"})


def normalize_lang(text, lang):
    return text.translate(PERSIAN_FOLD) if lang == "fa" else text


def _strip_pronouns(t, lang):
    t = _strip_leading(t, PRONOUN_LEADING.get(lang, []) + [w.strip() + " " for w in PRONOUN_TRAILING.get(lang, [])
                                                           if lang in ("ur", "hi")])
    changed = True
    while changed:
        changed = False
        for tail in PRONOUN_TRAILING.get(lang, []):
            if t.lower().endswith(tail) and len(t) > len(tail) + 1:
                t, changed = t[: -len(tail)].rstrip(), True
    for suffix in PRONOUN_ATTACHED.get(lang, []):
        if t.endswith(suffix) and len(t) - len(suffix) >= 3:
            t = t[: -len(suffix)]
            if lang == "fa" and suffix.startswith("ها"):
                t += "ها"
            break
    return t


def clean(text, lang, coarse, conj=False, pronoun=False, function=False):
    t = normalize_lang(unicodedata.normalize("NFC", text or ""), lang)
    t = re.sub(r"\([^)]*\)|\[[^\]]*\]|\{[^}]*\}", " ", t)   # implied words GTAF parenthesizes
    t = re.sub(r"\s+", " ", t).strip()
    first = ALTERNATIVE_SEPARATORS.split(t)[0].strip()
    if first:
        t = first
    keep_elision = lang == "fr" and FRENCH_ELISION.search(t)
    t = t.strip(_PUNCT).strip()
    if keep_elision:
        t += keep_elision.group(0)[-1]
    low = t.lower()
    for prefix in LEADING_STRIP.get(lang, []):
        if low.startswith(prefix) and len(t) > len(prefix) + 1:
            t, low = t[len(prefix):], low[len(prefix):]
    # A leading "and/so" is never part of a verb's or noun's own meaning.
    t = _strip_leading(t, CONJUNCTIONS.get(lang, []))
    if not function:
        t = _strip_leading(t, CONJUNCTIONS.get(lang, []) + CLAUSE_LEADING.get(lang, []))
    t = _strip_leading(t, LEADING_STRIP.get(lang, []))
    if coarse != "V":
        t = _strip_leading(t, NOUN_LEADING.get(lang, []))
    if pronoun:
        t = _strip_pronouns(t.strip(_PUNCT), lang)
    if coarse == "V":
        lead = VERB_PRONOUNS.get(lang, []) + ([] if function else
                                               VERB_CONTEXT_LEADING.get(lang, []) + CONJUNCTIONS.get(lang, [])
                                               + CLAUSE_LEADING.get(lang, []))
        t = _strip_leading(t, lead)
        changed = True
        while changed:
            changed = False
            for tail in VERB_TRAILING.get(lang, []) + ([] if function else VERB_CONTEXT_TRAILING.get(lang, [])):
                if t.lower().endswith(tail) and len(t) > len(tail) + 1:
                    t, changed = t[: -len(tail)].rstrip(), True
        t = _strip_leading(t, lead)
    else:
        t = _strip_leading(t, NOUN_LEADING.get(lang, []) + ([] if function else
                                                            NOUN_CONTEXT_LEADING.get(lang, []) + CONJUNCTIONS.get(lang, [])
                                                            + CLAUSE_LEADING.get(lang, [])))
        changed = True
        while changed:
            changed = False
            for tail in NOUN_TRAILING.get(lang, []) + ([] if function else NOUN_CONTEXT_TRAILING.get(lang, [])):
                if t.endswith(tail) and len(t) > len(tail) + 1:
                    t, changed = t[: -len(tail)].rstrip(), True
        for tail in TRAILING_COPULA.get(lang, []):
            if t.endswith(tail) and len(t) > len(tail) + 1:
                t = t[: -len(tail)]
    out = t.strip(_PUNCT).strip()
    if keep_elision and not out.endswith(("'", "’")):
        out += keep_elision.group(0)[-1]
    return out


def norm(t):
    return re.sub(r"\s+", " ", t.lower()).strip()


def _vote(lem, candidates, gtaf, lang, pronoun):
    votes = Counter()
    surface = {}
    for o in candidates:
        toks = gtaf[lang].get((o.surah, o.ayah))
        if not toks or o.word > len(toks):
            continue
        g = clean(toks[o.word - 1]["translation"], lang, lem.coarse, conj=o.conj_only or pronoun,
                  pronoun=pronoun and o.suffixed)
        if not g or len(g.split()) > 5 or re.search(r"\d", g):
            continue
        k = norm(g)
        votes[k] += 1
        # Remember the casing seen mid-verse (verse-initial words are capitalized by GTAF).
        if o.word > 1 or k not in surface:
            surface[k] = g
    for k in [k for k in votes if k in STOPWORDS.get(lang, set()) or not re.search(r"\w", k)]:
        del votes[k]
    return votes, surface


def gloss_lemma(lem, gtaf, lang):
    """Returns (gloss, support, total, method) or (None, 0, 0, reason)."""
    tiers = []
    clean_occ = [o for o in lem.occurrences if o.bare or o.det_only]
    if lem.coarse == "V":
        clean_occ = clean_occ + [o for o in lem.occurrences if o.conj_only]
        perfect = [o for o in clean_occ if o.verb_3ms_perf]
        if len(perfect) >= 2:
            tiers.append(("bare-3ms", perfect, False))
    tiers.append(("bare", clean_occ, False))
    tiers.append(("conj", [o for o in lem.occurrences if o.conj_only], False))
    # Last resort: the word only ever occurs with an attached pronoun; strip its translation.
    tiers.append(("pronoun", [o for o in lem.occurrences if not o.prefixed or o.conj_only], True))
    for method, candidates, pronoun in tiers:
        if not candidates:
            continue
        votes, surface = _vote(lem, candidates, gtaf, lang, pronoun)
        if votes:
            break
    else:
        return None, 0, 0, "no-clean-occurrence"
    total = sum(votes.values())
    pooled = Counter(votes)
    keys = sorted(votes, key=len)
    for i, base in enumerate(keys):
        if len(base) < 3:
            continue
        for longer in keys[i + 1:]:
            same_word = lang in SUFFIXING and longer.startswith(base) and " " not in longer[len(base):].strip()
            if same_word or longer.startswith(base + " ") or longer.endswith(" " + base):
                pooled[base] += votes[longer]
    best = max(pooled.values())
    near = [k for k in pooled if pooled[k] >= 0.5 * best]
    top = min(near, key=lambda k: (len(k.split()), -pooled[k], len(k)))
    c = pooled[top]
    rest = [(k, pooled[k]) for k, _ in votes.most_common() if k != top and not (k.startswith(top) or top.startswith(k))]
    gloss = surface[top]
    if lang in LATIN_LANGS and not lem.is_proper_noun and gloss[:1].isupper() and gloss != gloss.upper():
        # Keep capitals only where GTAF itself capitalizes mid-verse (Allah, Lord, Messenger).
        mid = [o for o in candidates if o.word > 1]
        if not mid:
            gloss = gloss[0].lower() + gloss[1:]
    if rest:
        second, c2 = max(rest, key=lambda kv: kv[1])
        if c2 >= 2 and c2 / total >= 0.25 and second not in top and top not in second:
            sense2 = surface[second]
            if lang in LATIN_LANGS and gloss[:1].islower() and sense2[:1].isupper() and not lem.is_proper_noun:
                sense2 = sense2[0].lower() + sense2[1:]
            gloss = f"{gloss} / {sense2}"
    return gloss, c, total, method


FRAGMENT_STARTS = {
    "en": ("and ", "so ", "then ", "of ", "in ", "they ", "he ", "you ", "his ", "their ", "which ", "that ", "her "),
    "fr": ("et ", "de ", "du ", "des ", "puis ", "ils ", "il ", "leur ", "leurs "),
    "in": ("dan ", "maka ", "lalu ", "yang "),
    "tr": ("ve ",),
    "bn": ("এবং ", "আর "),
    "ur": ("اور ",),
    "hi": ("और ",),
    "fa": ("و ",),
}


def meaning_problem(gloss, lang, category):
    """Why a shipped meaning would be wrong-looking, or None. Shared by the build (which drops
    the lemma) and the validator (which fails the build), so they can never disagree."""
    g = (gloss or "").strip()
    if not g:
        return "empty"
    if re.search(r"[\[\]{}]|\d", g):
        return "brackets/digits"
    if len(g.split(" / ")[0].split()) > 6:
        return "too long"
    if category != "PARTICLE":
        low = g.lower()
        if low.startswith(FRAGMENT_STARTS.get(lang, ())):
            return "phrase fragment"
        if low in STOPWORDS.get(lang, set()):
            return "function word"
    return None


def accepted(support, total):
    """A gloss is verified when it is the sole bare occurrence's own translation, or is the
    clear majority across several bare occurrences."""
    if total == 0:
        return False
    if total == 1:
        return True
    return support >= 2 and support / total >= 0.25
