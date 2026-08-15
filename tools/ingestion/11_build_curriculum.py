# -*- coding: utf-8 -*-
"""
Restructures the flat 368-lesson vocabulary content into a chapter -> section -> lesson
hierarchy with exam-gated progression and three-level flashback reviews, per
`docs/CURRICULUM_DESIGN.md` (and the fork's rebuild plan). Consumes the already-emitted
word_frequency.json / exercises_vocabulary.json (rather than the raw lemma intermediate,
which isn't checked in - see 07_emit_content.py for that earlier stage) and re-slices them
into the new boundaries.

Deterministic contiguous partition of the 3,680 frequency-rank-ordered words: 8 chapters of
460 words, each split into 10 sections of 46 words, each split into ~10-word lessons (last
lesson in a section absorbs the remainder). Word order is never reshuffled - this is a slice,
not a re-sort.

Emits:
  - chapters.json    (ChapterEntity rows)
  - sections.json    (SectionEntity rows)
  - lessons_vocabulary.json  (LessonEntity rows: regular lessons + exam/flashback checkpoints)
  - exercises_vocabulary.json (re-keyed teach/quiz/matching exercises for regular lessons,
    plus curated multiple-choice question sets for every exam/flashback lesson)

Run from the repo root or this directory: `python3 tools/ingestion/11_build_curriculum.py`
"""
import copy
import json
from pathlib import Path

CONTENT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"

CHAPTER_COUNT = 8
SECTIONS_PER_CHAPTER = 10
WORDS_PER_LESSON = 10
PAIRS_PER_MATCH = 5
EXAM_QUESTION_CAP = 25
FLASHBACK_QUESTION_CAP = 20

TEACH_PROMPT_EN = "Meet a new word"
TEACH_PROMPT_BN = "একটি নতুন শব্দ চিনুন"
QUIZ_PROMPT_EN = "What does this word mean?"
QUIZ_PROMPT_BN = "এই শব্দের অর্থ কী?"
MATCH_PROMPT_EN = "Match each word to its meaning."
MATCH_PROMPT_BN = "প্রতিটি শব্দকে এর অর্থের সাথে মেলান।"
EXAM_PROMPT_EN = "What does this word mean?"
EXAM_PROMPT_BN = "এই শব্দের অর্থ কী?"
TAP_WORD_PROMPT_EN = "Tap the word that means:"
TAP_WORD_PROMPT_BN = "যে শব্দের অর্থ এটি, সেটিতে চাপ দিন:"


def load(name):
    with open(CONTENT_DIR / name, encoding="utf-8") as f:
        return json.load(f)


def dump(name, payload):
    with open(CONTENT_DIR / name, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
        f.write("\n")


def chunk(seq, size):
    return [seq[i:i + size] for i in range(0, len(seq), size)]


def compute_word_spans(text):
    """Whitespace-delimited char [start, end) spans, matching how quran-align (and the
    TapWordInVerse tappable-word UI) tokenizes verse text - a plain split() with position
    tracking, no punctuation/diacritic special-casing needed since Arabic word boundaries here
    are exactly the whitespace gaps."""
    spans = []
    i = 0
    for token in text.split(" "):
        if token:
            start = text.index(token, i)
            spans.append({"start": start, "end": start + len(token)})
            i = start + len(token)
    return spans


def even_sample(items, cap):
    """Deterministic, order-preserving down-sample to at most `cap` items - takes evenly spaced
    indices rather than just the first `cap`, so a large scope's sample still spans its whole
    frequency-rank range instead of skewing toward the most-frequent end."""
    if len(items) <= cap:
        return items
    return [items[(i * len(items)) // cap] for i in range(cap)]


def main():
    words = load("word_frequency.json")["words"]
    words.sort(key=lambda w: w["frequencyRank"])
    assert len(words) == CHAPTER_COUNT * SECTIONS_PER_CHAPTER * 46, (
        f"expected {CHAPTER_COUNT * SECTIONS_PER_CHAPTER * 46} words, got {len(words)} - "
        "the chapter/section partition math below assumes this exact count"
    )
    total_occurrence = sum(w["frequencyCount"] for w in words)

    old_exercises = load("exercises_vocabulary.json")["exercises"]
    # Recover the (WordIntro, MultipleChoice) content pair authored for each word, keyed by
    # wordId, by walking the old flat exercise list in order - every WordIntro is immediately
    # followed by the MultipleChoice quizzing the same word (true by construction of
    # 07_emit_content.py's original emission, and re-verified here rather than assumed silently).
    word_intro_by_id = {}
    mc_by_id = {}
    old_exercises.sort(key=lambda e: (e["lessonId"], e["orderIndex"]))
    pending_word_id = None
    for ex in old_exercises:
        content = ex["content"]
        if content["type"] == "word_intro":
            word_intro_by_id[content["wordId"]] = content
            pending_word_id = content["wordId"]
        elif content["type"] == "multiple_choice" and pending_word_id is not None:
            mc_by_id[pending_word_id] = content
            pending_word_id = None
    missing = [w["id"] for w in words if w["id"] not in word_intro_by_id or w["id"] not in mc_by_id]
    assert not missing, f"{len(missing)} words have no authored teach/quiz content, e.g. {missing[:5]}"

    chapters, sections, lessons, exercises = [], [], [], []
    exercise_counter = 0

    def next_exercise_id():
        nonlocal exercise_counter
        exercise_counter += 1
        return f"ex_{exercise_counter}"

    def emit_regular_lesson_exercises(lesson_id, lesson_words):
        order = 0
        for w in lesson_words:
            wi = copy.deepcopy(word_intro_by_id[w["id"]])
            exercises.append({
                "id": next_exercise_id(), "lessonId": lesson_id, "orderIndex": order,
                "exerciseType": "TEACH_WORD", "content": wi,
            })
            order += 1
            mc = copy.deepcopy(mc_by_id[w["id"]])
            exercises.append({
                "id": next_exercise_id(), "lessonId": lesson_id, "orderIndex": order,
                "exerciseType": "MULTIPLE_CHOICE", "content": mc,
            })
            order += 1

            # Reverse-direction "tap the word in the verse" quiz - only possible for words whose
            # exact position within their example verse is confirmed (arabicWordStart/End), not
            # guessed. See docs/CONTENT_SOURCES.md item 9 for why only ~43% of words qualify.
            start, end = wi.get("arabicWordStart"), wi.get("arabicWordEnd")
            if start is not None and end is not None:
                verse = wi["exampleVerseArabic"]
                spans = compute_word_spans(verse)
                if any(s["start"] == start and s["end"] == end for s in spans):
                    exercises.append({
                        "id": next_exercise_id(), "lessonId": lesson_id, "orderIndex": order,
                        "exerciseType": "WORD_IN_VERSE_TAP",
                        "content": {
                            "type": "word_in_verse_tap",
                            "promptEn": TAP_WORD_PROMPT_EN, "promptBn": TAP_WORD_PROMPT_BN,
                            "wordId": w["id"], "verseArabic": verse,
                            "verseReference": wi["exampleVerseReference"],
                            "correctWordStart": start, "correctWordEnd": end,
                            "tappableSpans": spans,
                            "meaningEn": w["meaningEn"], "meaningBn": w["meaningBn"],
                        },
                    })
                    order += 1
        for pair_words in chunk(lesson_words, PAIRS_PER_MATCH):
            pairs = [
                {
                    "id": f"p{i + 1}",
                    "leftArabic": w["arabicWord"],
                    "rightEn": w["meaningEn"],
                    "rightBn": w["meaningBn"],
                    "wordId": w["id"],
                }
                for i, w in enumerate(pair_words)
            ]
            exercises.append({
                "id": next_exercise_id(), "lessonId": lesson_id, "orderIndex": order,
                "exerciseType": "MATCHING",
                "content": {"type": "matching", "promptEn": MATCH_PROMPT_EN, "promptBn": MATCH_PROMPT_BN, "pairs": pairs},
            })
            order += 1

    def emit_quiz_lesson_exercises(lesson_id, candidate_words, cap):
        sampled = even_sample(candidate_words, cap)
        for order, w in enumerate(sampled):
            mc = copy.deepcopy(mc_by_id[w["id"]])
            mc["promptEn"] = EXAM_PROMPT_EN
            mc["promptBn"] = EXAM_PROMPT_BN
            exercises.append({
                "id": next_exercise_id(), "lessonId": lesson_id, "orderIndex": order,
                "exerciseType": "MULTIPLE_CHOICE", "content": mc,
            })

    def stats(word_group):
        occurrence = sum(w["frequencyCount"] for w in word_group)
        return len(word_group), occurrence, round(occurrence * 100.0 / total_occurrence, 4)

    chapter_word_chunks = chunk(words, 460)
    assert len(chapter_word_chunks) == CHAPTER_COUNT

    words_by_chapter = []  # running list of every word in chapters seen so far, for chapter flashback pools
    for chapter_index, chapter_words in enumerate(chapter_word_chunks, start=1):
        chapter_id = f"chapter_{chapter_index}"
        w_count, occ, pct = stats(chapter_words)
        start_rank, end_rank = chapter_words[0]["frequencyRank"], chapter_words[-1]["frequencyRank"]
        chapters.append({
            "id": chapter_id,
            "titleEn": f"Chapter {chapter_index}",
            "titleBn": f"অধ্যায় {chapter_index}",
            "descriptionEn": f"Words ranked {start_rank}–{end_rank} by how often they appear in the Qur'an.",
            "descriptionBn": f"কুরআনে ব্যবহারের ফ্রিকোয়েন্সি অনুসারে র‍্যাঙ্ক {start_rank}–{end_rank} নম্বর শব্দ।",
            "sortOrder": chapter_index,
            "wordCount": w_count,
            "quranOccurrenceCount": occ,
            "quranOccurrencePercent": pct,
        })

        section_word_chunks = chunk(chapter_words, 46)
        assert len(section_word_chunks) == SECTIONS_PER_CHAPTER

        words_by_section = []  # running list of every word in this chapter's sections seen so far
        chapter_lesson_seq = []  # every lesson id emitted for this chapter, section by section, in order
        for section_index, section_words in enumerate(section_word_chunks, start=1):
            section_id = f"section_{chapter_index}_{section_index}"
            s_count, s_occ, s_pct = stats(section_words)
            sec_start, sec_end = section_words[0]["frequencyRank"], section_words[-1]["frequencyRank"]
            sections.append({
                "id": section_id,
                "chapterId": chapter_id,
                "titleEn": f"Section {section_index}",
                "titleBn": f"বিভাগ {section_index}",
                "sortOrder": section_index,
                "wordCount": s_count,
                "quranOccurrenceCount": s_occ,
                "quranOccurrencePercent": s_pct,
            })

            lesson_word_chunks = chunk(section_words, WORDS_PER_LESSON)
            sort_order = 0
            words_in_lessons_so_far = []  # for this section's lesson-flashback pools
            for lesson_index, lesson_words in enumerate(lesson_word_chunks, start=1):
                sort_order += 1
                lesson_id = f"lesson_ch{chapter_index}_sec{section_index}_{lesson_index}"
                l_start, l_end = lesson_words[0]["frequencyRank"], lesson_words[-1]["frequencyRank"]
                lessons.append({
                    "id": lesson_id, "chapterId": chapter_id, "sectionId": section_id,
                    "titleEn": f"Lesson {lesson_index}: words {l_start}-{l_end}",
                    "titleBn": f"পাঠ {lesson_index}: শব্দ {l_start}-{l_end}",
                    "sortOrder": sort_order, "kind": "REGULAR",
                })
                emit_regular_lesson_exercises(lesson_id, lesson_words)
                chapter_lesson_seq.append(lesson_id)

                if lesson_index > 1:
                    sort_order += 1
                    flashback_id = f"lesson_ch{chapter_index}_sec{section_index}_flashback_after_{lesson_index}"
                    lessons.append({
                        "id": flashback_id, "chapterId": chapter_id, "sectionId": section_id,
                        "titleEn": "Quick Review", "titleBn": "দ্রুত পুনরালোচনা",
                        "sortOrder": sort_order, "kind": "LESSON_FLASHBACK",
                    })
                    emit_quiz_lesson_exercises(flashback_id, words_in_lessons_so_far, FLASHBACK_QUESTION_CAP)
                    chapter_lesson_seq.append(flashback_id)

                words_in_lessons_so_far = words_in_lessons_so_far + lesson_words

            sort_order += 1
            exam_id = f"lesson_ch{chapter_index}_sec{section_index}_exam"
            lessons.append({
                "id": exam_id, "chapterId": chapter_id, "sectionId": section_id,
                "titleEn": f"Section {section_index} Exam", "titleBn": f"বিভাগ {section_index} পরীক্ষা",
                "sortOrder": sort_order, "kind": "SECTION_EXAM",
            })
            emit_quiz_lesson_exercises(exam_id, section_words, EXAM_QUESTION_CAP)
            chapter_lesson_seq.append(exam_id)

            if section_index > 1:
                sort_order += 1
                sec_flashback_id = f"lesson_ch{chapter_index}_sec{section_index}_flashback"
                lessons.append({
                    "id": sec_flashback_id, "chapterId": chapter_id, "sectionId": section_id,
                    "titleEn": "Section Flashback", "titleBn": "বিভাগ পুনরালোচনা",
                    "sortOrder": sort_order, "kind": "SECTION_FLASHBACK",
                })
                emit_quiz_lesson_exercises(sec_flashback_id, words_by_section, FLASHBACK_QUESTION_CAP)
                chapter_lesson_seq.append(sec_flashback_id)

            words_by_section = words_by_section + section_words

        chapter_exam_id = f"lesson_ch{chapter_index}_exam"
        lessons.append({
            "id": chapter_exam_id, "chapterId": chapter_id, "sectionId": None,
            "titleEn": f"Chapter {chapter_index} Exam", "titleBn": f"অধ্যায় {chapter_index} পরীক্ষা",
            "sortOrder": 1, "kind": "CHAPTER_EXAM",
        })
        emit_quiz_lesson_exercises(chapter_exam_id, chapter_words, EXAM_QUESTION_CAP)

        if chapter_index > 1:
            chapter_flashback_id = f"lesson_ch{chapter_index}_flashback"
            lessons.append({
                "id": chapter_flashback_id, "chapterId": chapter_id, "sectionId": None,
                "titleEn": "Chapter Flashback", "titleBn": "অধ্যায় পুনরালোচনা",
                "sortOrder": 2, "kind": "CHAPTER_FLASHBACK",
            })
            emit_quiz_lesson_exercises(chapter_flashback_id, words_by_chapter, FLASHBACK_QUESTION_CAP)

        words_by_chapter = words_by_chapter + chapter_words

    dump("chapters.json", {"chapters": chapters})
    dump("sections.json", {"sections": sections})
    dump("lessons_vocabulary.json", {"lessons": lessons})
    dump("exercises_vocabulary.json", {"exercises": exercises})

    print(f"chapters={len(chapters)} sections={len(sections)} lessons={len(lessons)} exercises={len(exercises)}")
    kind_counts = {}
    for l in lessons:
        kind_counts[l["kind"]] = kind_counts.get(l["kind"], 0) + 1
    print("lesson kinds:", kind_counts)


if __name__ == "__main__":
    main()
