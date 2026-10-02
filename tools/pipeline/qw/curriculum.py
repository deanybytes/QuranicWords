"""Chapters -> sections -> lessons -> exercises, emitted in the exact JSON shape the Android
ContentSeeder reads (chapters.json, sections.json, lessons_vocabulary.json,
exercises_vocabulary.json, word_frequency.json).

Chapters stay part-of-speech pure (Aqsām al-Kalimah), but alternate verbs and nouns by
frequency band so a learner meets اللَّه, رَبّ and يَوْم early instead of after 1,400 verbs."""
import random
from collections import defaultdict

from . import arabic, config, text

# (track, number of words) per chapter after the function-word chapter; the last chapter of
# each track takes whatever remains.
CHAPTER_PLAN = [
    ("FUNCTION", None),
    ("VERB", 150), ("NOUN", 250), ("VERB", 250), ("NOUN", 500),
    ("VERB", 350), ("NOUN", 650), ("VERB", None), ("NOUN", 650), ("NOUN", None),
]
CATEGORY_FOR_TRACK = {"FUNCTION": "MIXED", "VERB": "VERB", "NOUN": "NOUN"}
SECTION_WORDS = config.WORDS_PER_LESSON * config.LESSONS_PER_SECTION
FLASHBACK_SIZE = 15
SECTION_EXAM_SIZE = 20
CHAPTER_EXAM_SIZE = 25


def _rng(*parts):
    return random.Random(f"{config.SEED}:" + ":".join(map(str, parts)))


class Builder:
    def __init__(self, words, gtaf, verse_tr, surah_names, total_occurrences):
        self.words = words
        self.by_id = {w.id: w for w in words}
        self.gtaf = gtaf
        self.verse_tr = verse_tr
        self.surah_names = surah_names
        self.total = total_occurrences
        self.ex_counter = 0
        self.chapters, self.sections, self.lessons, self.exercises = [], [], [], []
        self.tier = {}

    # ---- verse helpers -------------------------------------------------------------------
    def verse(self, ref):
        s, a, w = ref
        toks = self.gtaf["en"][(s, a)]
        parts = [t["arabic"] for t in toks]
        start = sum(len(p) + 1 for p in parts[: w - 1])
        verse_ar = " ".join(parts)
        spans = []
        pos = 0
        for p in parts:
            spans.append({"start": pos, "end": pos + len(p)})
            pos += len(p) + 1
        return verse_ar, start, start + len(parts[w - 1]), spans

    def reference(self, ref):
        s, a, _ = ref
        return f"{self.surah_names[s]} {s}:{a}"

    def translation(self, ref):
        s, a, _ = ref
        return {lang: self.verse_tr[lang][(s, a)] for lang in config.LANGS}

    def highlight(self, word, ref, meaning=None):
        """Where the word's own meaning appears inside each verse translation (if it does)."""
        tr = self.translation(ref)
        out = {}
        for lang in config.LANGS:
            gloss = (meaning or word.meaning)[lang]
            for sense in [s.strip() for s in gloss.split(" / ")]:
                if len(sense) < 2:
                    continue
                idx = tr[lang].lower().find(sense.lower())
                if idx >= 0:
                    out[lang] = tr[lang][idx: idx + len(sense)]
                    break
        return out

    # ---- exercise factories ----------------------------------------------------------------
    def _ex(self, lesson_id, order, etype, content):
        self.ex_counter += 1
        self.exercises.append({"id": f"ex2_{self.ex_counter:05d}", "lessonId": lesson_id,
                               "orderIndex": order, "exerciseType": etype, "content": content})

    def intro(self, w):
        verse_ar, st, en, _ = self.verse(w.verse)
        senses = []
        for idx, ctx, ref in w.senses:
            v_ar, s_st, s_en, _ = self.verse(ref)
            senses.append({
                "meaningIndex": idx, "contextualMeaning": ctx, "verseReference": self.reference(ref),
                "verseArabic": v_ar, "arabicWordStart": s_st, "arabicWordEnd": s_en,
                "verseTranslation": self.translation(ref), "translationHighlight": self.highlight(w, ref, ctx),
            })
        c = {
            "type": "word_intro", "prompt": text.PROMPT_INTRO, "wordId": w.id, "arabicWord": w.arabic,
            "meaning": w.meaning, "lemmaCategory": w.category, "polysemyEntries": senses,
            "meaningReviewed": w.reviewed, "root": w.root, "transliteration": w.translit,
            "quranOccurrenceCount": w.frequency,
            "exampleVerseArabic": verse_ar, "exampleVerseTranslation": self.translation(w.verse),
            "exampleVerseReference": self.reference(w.verse), "exampleVerseVerified": True,
            "audioAssetPath": w.audio, "arabicWordStart": st, "arabicWordEnd": en,
            "meaningHighlight": self.highlight(w, w.verse),
        }
        for k, v in (("verbForm", w.verb_form), ("pastArabic", w.past), ("presentArabic", w.present),
                     ("masdarArabic", w.masdar), ("particleType", w.particle_type),
                     ("partOfSpeechDetail", w.pos_detail or w.particle_type)):
            if v:
                c[k] = v
        return c

    def option(self, w):
        return {"id": f"opt_{w.id}", "labelArabic": w.arabic, "label": w.meaning}

    def mc(self, w):
        verse_ar, st, en, _ = self.verse(w.verse)
        opts = [self.option(w)] + [self.option(self.by_id[d]) for d in self.distractors[w.id][:3]]
        _rng("mc", w.id).shuffle(opts)
        return {"type": "multiple_choice", "prompt": text.PROMPT_MC, "promptArabic": w.arabic, "wordId": w.id,
                "options": opts, "correctOptionId": f"opt_{w.id}", "exampleVerseArabic": verse_ar,
                "exampleVerseTranslation": self.translation(w.verse), "exampleVerseReference": self.reference(w.verse),
                "arabicWordStart": st, "arabicWordEnd": en, "meaningHighlight": self.highlight(w, w.verse)}

    def fill(self, w):
        verse_ar, st, en, _ = self.verse(w.verse)
        # Options show the word exactly as it is written in this verse; distractors are other
        # words of the same category in their dictionary form.
        surface = verse_ar[st:en]
        correct = {"id": f"opt_{w.id}", "labelArabic": surface, "label": w.meaning}
        opts = [correct] + [self.option(self.by_id[d]) for d in self.distractors[w.id][:3]]
        _rng("fill", w.id).shuffle(opts)
        return {"type": "fill_in_the_blank", "prompt": text.PROMPT_FILL, "wordId": w.id,
                "sentenceArabic": verse_ar, "blankStart": st, "blankEnd": en,
                "sentenceTranslation": self.translation(w.verse), "sentenceReference": self.reference(w.verse),
                "options": opts, "correctOptionId": f"opt_{w.id}"}

    def tap(self, w):
        verse_ar, st, en, spans = self.verse(w.verse)
        return {"type": "tap_word_in_verse", "prompt": text.PROMPT_TAP, "wordId": w.id,
                "verseArabic": verse_ar, "verseReference": self.reference(w.verse),
                "correctWordStart": st, "correctWordEnd": en, "tappableSpans": spans,
                "meaning": w.meaning, "verseTranslation": self.translation(w.verse),
                "meaningHighlight": self.highlight(w, w.verse)}

    def listen(self, w):
        opts = [self.option(w)] + [self.option(self.by_id[d]) for d in self.distractors[w.id][:3]]
        _rng("listen", w.id).shuffle(opts)
        return {"type": "tap_what_you_hear", "prompt": text.PROMPT_LISTEN, "audioAssetPath": w.audio,
                "wordId": w.id, "options": opts, "correctOptionId": f"opt_{w.id}"}

    def matching(self, ws):
        pairs = []
        for i, w in enumerate(ws, 1):
            verse_ar, st, en, _ = self.verse(w.verse)
            pairs.append({"id": f"p{i}", "leftArabic": w.arabic, "left": w.arabic, "right": w.meaning,
                          "wordId": w.id, "exampleVerseArabic": verse_ar,
                          "exampleVerseTranslation": self.translation(w.verse),
                          "exampleVerseReference": self.reference(w.verse), "arabicWordStart": st,
                          "arabicWordEnd": en, "meaningHighlight": self.highlight(w, w.verse)})
        return {"type": "matching", "prompt": text.PROMPT_MATCH, "pairs": pairs}

    # ---- distractors -----------------------------------------------------------------------
    def build_distractors(self, chapters_words):
        """12 candidates per word: same chapter (so same category and frequency band), never the
        same written form or root, and no shared meaning in *any* language - so the correct
        answer is always the only defensible one."""
        from .words import meaning_tokens
        self.distractors = {}
        for ch_words in chapters_words:
            for i, w in enumerate(ch_words):
                rng = _rng("dist", w.id)
                order = sorted(range(len(ch_words)), key=lambda j: (abs(j - i), rng.random()))
                picks = []
                w_skel = arabic.skeleton(w.arabic)
                w_tokens = {lang: meaning_tokens(w.meaning[lang]) for lang in config.LANGS}
                for j in order:
                    d = ch_words[j]
                    if d.id == w.id or arabic.skeleton(d.arabic) == w_skel:
                        continue
                    if w.root and d.root == w.root:
                        continue
                    clash = False
                    for lang in config.LANGS:
                        b = {s.strip().lower() for s in d.meaning[lang].split(" / ")}
                        # Distinct from the answer *and* from every distractor already picked.
                        for other in [w] + [self.by_id[p] for p in picks]:
                            a = {s.strip().lower() for s in other.meaning[lang].split(" / ")}
                            if a & b or d.meaning[lang].lower() == other.meaning[lang].lower():
                                clash = True
                                break
                        if not clash and w_tokens[lang] and w_tokens[lang] == meaning_tokens(d.meaning[lang]):
                            clash = True
                        if clash:
                            break
                    if clash:
                        continue
                    picks.append(d.id)
                    if len(picks) == 12:
                        break
                self.distractors[w.id] = picks

    # ---- curriculum ------------------------------------------------------------------------
    def plan_chapters(self):
        by_track = defaultdict(list)
        for w in sorted(self.words, key=lambda w: (-w.frequency, w.id)):
            by_track[w.track].append(w)
        cursor = defaultdict(int)
        out = []
        for idx, (track, size) in enumerate(CHAPTER_PLAN):
            pool = by_track[track]
            start = cursor[track]
            remaining_same_track = [s for t, s in CHAPTER_PLAN[idx + 1:] if t == track]
            end = len(pool) if size is None or not remaining_same_track else min(len(pool), start + size)
            out.append((track, pool[start:end]))
            cursor[track] = end
        return out

    def build(self):
        chapters = self.plan_chapters()
        self.build_distractors([ws for _, ws in chapters])
        cumulative_occ = 0
        cumulative_words = 0
        section_no = 0
        lesson_no = 0
        for ch_index, (track, ch_words) in enumerate(chapters, 1):
            ch_id = f"ch_{ch_index:02d}"
            for w in ch_words:
                self.tier[w.id] = ch_index
            occ = sum(w.frequency for w in ch_words)
            cumulative_occ += occ
            cumulative_words += len(ch_words)
            title = text.CHAPTER_TITLES[ch_index - 1]
            desc = text.CHAPTER_DESCRIPTIONS[ch_index - 1] or (text.VERB_DESC if track == "VERB" else text.NOUN_DESC)
            self.chapters.append({"id": ch_id, "title": title, "description": desc, "sortOrder": ch_index,
                                  "wordCount": len(ch_words), "quranOccurrenceCount": occ,
                                  "quranOccurrencePercent": round(100 * occ / self.total, 2)})
            category = CATEGORY_FOR_TRACK[track]
            sec_chunks = [ch_words[i:i + SECTION_WORDS] for i in range(0, len(ch_words), SECTION_WORDS)]
            for s_idx, sec_words in enumerate(sec_chunks, 1):
                section_no += 1
                sec_id = f"s2_{section_no:03d}"
                s_occ = sum(w.frequency for w in sec_words)
                self.sections.append({"id": sec_id, "chapterId": ch_id,
                                      "title": text.loc(text.SECTION, n=s_idx, t=title), "sortOrder": s_idx,
                                      "wordCount": len(sec_words), "quranOccurrenceCount": s_occ,
                                      "quranOccurrencePercent": round(100 * s_occ / self.total, 2)})
                order = 0

                def lesson(kind, title_loc):
                    nonlocal lesson_no, order
                    lesson_no += 1
                    order += 1
                    lid = f"l2_{lesson_no:04d}"
                    self.lessons.append({"id": lid, "chapterId": ch_id, "sectionId": sec_id, "title": title_loc,
                                         "sortOrder": order, "kind": kind, "category": category})
                    return lid

                if s_idx == 1:
                    lid = lesson("CHAPTER_INTRO", text.loc(text.CHAPTER_INTRO, n=ch_index))
                    counts = defaultdict(int)
                    for w in ch_words:
                        counts[w.category] += 1
                    self._ex(lid, 1, "CHAPTER_INTRO", {
                        "type": "chapter_intro", "prompt": title, "chapterId": ch_id, "chapterNumber": ch_index,
                        "chapterTitle": title, "chapterDescription": desc, "wordCount": len(ch_words),
                        "quranOccurrenceCount": occ, "chapterCoveragePercent": round(100 * occ / self.total, 2),
                        "accumulatedCoveragePercent": round(100 * cumulative_occ / self.total, 2),
                        "accumulatedWords": cumulative_words, "nounCount": counts["NOUN"],
                        "verbCount": counts["VERB"], "particleCount": counts["PARTICLE"], "learningObjectives": desc})
                lessons_words = [sec_words[i:i + config.WORDS_PER_LESSON]
                                 for i in range(0, len(sec_words), config.WORDS_PER_LESSON)]
                # Never leave a lone 1-word lesson at a section's end.
                if len(lessons_words) > 1 and len(lessons_words[-1]) < 3:
                    lessons_words[-2].extend(lessons_words.pop())
                for l_idx, lw in enumerate(lessons_words, 1):
                    lid = lesson("REGULAR", text.loc(text.LESSON, n=l_idx, a=lw[0].arabic, b=lw[-1].arabic))
                    o = 0
                    for w in lw:
                        o += 1
                        self._ex(lid, o, "WORD_INTRO", self.intro(w))
                    for w in lw:
                        o += 1
                        self._ex(lid, o, "MULTIPLE_CHOICE", self.mc(w))
                    for k, w in enumerate(lw):
                        o += 1
                        if (lesson_no + k) % 2 == 0:
                            self._ex(lid, o, "FILL_IN_THE_BLANK", self.fill(w))
                        else:
                            self._ex(lid, o, "WORD_IN_VERSE_TAP", self.tap(w))
                    for w in lw:
                        if w.audio:
                            o += 1
                            self._ex(lid, o, "TAP_WHAT_YOU_HEAR", self.listen(w))
                    if len(lw) >= 3:
                        o += 1
                        self._ex(lid, o, "MATCHING", self.matching(lw))
                rng = _rng("section", sec_id)
                review = rng.sample(sec_words, min(FLASHBACK_SIZE, len(sec_words)))
                lid = lesson("SECTION_FLASHBACK", text.loc(text.FLASHBACK, n=s_idx))
                for o, w in enumerate(review, 1):
                    self._ex(lid, o, "MULTIPLE_CHOICE", self.mc(w))
                self._ex(lid, len(review) + 1, "MATCHING", self.matching(review[:5]))
                exam = rng.sample(sec_words, min(SECTION_EXAM_SIZE, len(sec_words)))
                lid = lesson("SECTION_EXAM", text.loc(text.SECTION_EXAM, n=s_idx))
                for o, w in enumerate(exam, 1):
                    self._ex(lid, o, "MULTIPLE_CHOICE" if o % 3 else "FILL_IN_THE_BLANK",
                             self.mc(w) if o % 3 else self.fill(w))
            # Chapter exam belongs to the chapter, not a section.
            lesson_no += 1
            lid = f"l2_{lesson_no:04d}"
            self.lessons.append({"id": lid, "chapterId": ch_id, "sectionId": None,
                                 "title": text.loc(text.CHAPTER_EXAM, n=ch_index), "sortOrder": 1,
                                 "kind": "CHAPTER_EXAM", "category": category})
            exam = _rng("chapter", ch_id).sample(ch_words, min(CHAPTER_EXAM_SIZE, len(ch_words)))
            for o, w in enumerate(exam, 1):
                self._ex(lid, o, "MULTIPLE_CHOICE" if o % 4 else "WORD_IN_VERSE_TAP", self.mc(w) if o % 4 else self.tap(w))

    def word_frequency(self):
        ranked = sorted(self.words, key=lambda w: (-w.frequency, w.id))
        return [{"id": w.id, "arabicWord": w.arabic, "frequencyRank": r, "frequencyCount": w.frequency,
                 "meaning": w.meaning, "audioAssetPath": w.audio, "tierLevel": self.tier[w.id]}
                for r, w in enumerate(ranked, 1)]
