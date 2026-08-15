# -*- coding: utf-8 -*-
"""
Emit the final Android-consumable content assets from the fully-processed lemma dataset:
  - word_frequency.json  (WordFrequencyEntity rows - curriculum ordering source of truth)
  - lessons_vocabulary.json (LessonEntity rows, module_vocabulary, ~10 words/lesson)
  - exercises_vocabulary.json (teach WordIntro + quiz MultipleChoice per word, plus closing
    Matching review exercises per lesson - same teach-then-quiz pattern as Tier 1)
"""
import json
from pathlib import Path

IN_PATH = Path(__file__).parent / "output" / "lemmas_with_verses.json"
OUT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"

WORDS_PER_LESSON = 10
PAIRS_PER_MATCH = 5

TEACH_PROMPT_EN = "Meet a new word"
TEACH_PROMPT_BN = "একটি নতুন শব্দ চিনুন"
QUIZ_PROMPT_EN = "What does this word mean?"
QUIZ_PROMPT_BN = "এই শব্দের অর্থ কী?"
MATCH_PROMPT_EN = "Match each word to its meaning."
MATCH_PROMPT_BN = "প্রতিটি শব্দকে এর অর্থের সাথে মেলান।"

def word_id(rank: int) -> str:
    return f"wf_{rank}"

def main():
    with open(IN_PATH, encoding="utf-8") as f:
        lemmas = json.load(f)
    lemmas.sort(key=lambda l: l["rank"])

    word_freq_rows = []
    lessons = []
    exercises = []

    lesson_num = 0
    for start in range(0, len(lemmas), WORDS_PER_LESSON):
        group = lemmas[start:start + WORDS_PER_LESSON]
        lesson_num += 1
        lesson_id = f"lesson_vocabulary_{lesson_num}"
        first_rank, last_rank = group[0]["rank"], group[-1]["rank"]
        end_pct = group[-1]["cumulativePercent"]

        lessons.append({
            "id": lesson_id,
            "moduleId": "module_vocabulary",
            "titleEn": f"Vocabulary {lesson_num}: words {first_rank}-{last_rank} (~{end_pct:.1f}% cumulative coverage)",
            "titleBn": f"শব্দভাণ্ডার {lesson_num}: শব্দ {first_rank}-{last_rank} (~{end_pct:.1f}% সঞ্চিত কভারেজ)",
            "sortOrder": lesson_num,
        })

        order = 0
        ex_num = 0
        pool_ids = [g["rank"] for g in group]

        for lemma in group:
            wid = word_id(lemma["rank"])
            ex_num += 1
            exercises.append({
                "id": f"ex_v{lesson_num}_{ex_num}",
                "lessonId": lesson_id,
                "orderIndex": order,
                "exerciseType": "TEACH_WORD",
                "content": {
                    "type": "word_intro",
                    "promptEn": TEACH_PROMPT_EN,
                    "promptBn": TEACH_PROMPT_BN,
                    "wordId": wid,
                    "arabicWord": lemma["arabic"],
                    "meaningEn": lemma["meaningEn"],
                    "meaningBn": lemma["meaningBn"],
                    "meaningBnReviewed": False,
                    "root": lemma["root"],
                    "exampleVerseArabic": lemma["exampleVerseArabic"],
                    "exampleVerseTranslationEn": lemma["exampleVerseTranslationEn"],
                    "exampleVerseTranslationBn": lemma["exampleVerseTranslationBn"],
                    "exampleVerseReference": lemma["exampleVerseReference"],
                    "audioAssetPath": f"audio/words/{wid}.mp3",
                }
            })
            order += 1
            ex_num += 1

            distractor_pool = [g for g in group if g["rank"] != lemma["rank"]]
            distractors = distractor_pool[:3]
            opts = [lemma] + distractors
            options = []
            correct_option_id = None
            for i, o in enumerate(opts):
                oid = f"o{i+1}"
                options.append({"id": oid, "labelEn": o["meaningEn"][:60], "labelBn": o["meaningBn"]})
                if o["rank"] == lemma["rank"]:
                    correct_option_id = oid

            exercises.append({
                "id": f"ex_v{lesson_num}_{ex_num}",
                "lessonId": lesson_id,
                "orderIndex": order,
                "exerciseType": "MULTIPLE_CHOICE",
                "content": {
                    "type": "multiple_choice",
                    "promptEn": QUIZ_PROMPT_EN,
                    "promptBn": QUIZ_PROMPT_BN,
                    "promptArabic": lemma["arabic"],
                    "options": options,
                    "correctOptionId": correct_option_id,
                }
            })
            order += 1

        for chunk_start in range(0, len(group), PAIRS_PER_MATCH):
            chunk = group[chunk_start:chunk_start + PAIRS_PER_MATCH]
            if len(chunk) < 2:
                continue
            ex_num += 1
            pairs = [
                {"id": f"p{i+1}", "leftArabic": g["arabic"], "rightEn": g["meaningEn"][:60], "rightBn": g["meaningBn"]}
                for i, g in enumerate(chunk)
            ]
            exercises.append({
                "id": f"ex_v{lesson_num}_{ex_num}",
                "lessonId": lesson_id,
                "orderIndex": order,
                "exerciseType": "MATCHING",
                "content": {
                    "type": "matching",
                    "promptEn": MATCH_PROMPT_EN,
                    "promptBn": MATCH_PROMPT_BN,
                    "pairs": pairs,
                }
            })
            order += 1

        for lemma in group:
            word_freq_rows.append({
                "id": word_id(lemma["rank"]),
                "arabicWord": lemma["arabic"],
                "frequencyRank": lemma["rank"],
                "frequencyCount": lemma["frequency"],
                "meaningEn": lemma["meaningEn"][:200],
                "meaningBn": lemma["meaningBn"],
                "audioAssetPath": None,
                "tierLevel": 2,
            })

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    with open(OUT_DIR / "word_frequency.json", "w", encoding="utf-8") as f:
        json.dump({"words": word_freq_rows}, f, ensure_ascii=False, indent=None, separators=(",", ":"))
    with open(OUT_DIR / "lessons_vocabulary.json", "w", encoding="utf-8") as f:
        json.dump({"lessons": lessons}, f, ensure_ascii=False, indent=None, separators=(",", ":"))
    with open(OUT_DIR / "exercises_vocabulary.json", "w", encoding="utf-8") as f:
        json.dump({"exercises": exercises}, f, ensure_ascii=False, indent=None, separators=(",", ":"))

    print(f"Lessons: {len(lessons)}")
    print(f"Exercises: {len(exercises)}")
    print(f"Word frequency rows: {len(word_freq_rows)}")

if __name__ == "__main__":
    main()
