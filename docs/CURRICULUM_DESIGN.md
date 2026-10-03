# 🎓 Curriculum Design

## What is taught

The curriculum teaches **3,833 distinct lemmas** from the Quranic Arabic Corpus. Every one is a
real word with its own meaning; none is a repeat or a grammatical fragment. Together they account
for **97.4% of the Qur'an's lexical segments**, meaning its words plus attached particles
such as وَ and بِ.

About 880 very rare lemmas are not taught. They are mostly hapax legomena (words that occur only
once) that never appear without an attached pronoun or preposition, so no clean source meaning
exists for them in all 8 languages. Each one is listed with its reason in
[`tools/pipeline/reports/excluded_lemmas.tsv`](../tools/pipeline/reports/excluded_lemmas.tsv).

## The three parts of speech (Aqsām al-Kalimah), interleaved by frequency

Each chapter covers a single part of speech. After the function words, verb and noun chapters
alternate by frequency band. A learner therefore meets اللَّه, رَبّ, يَوْم and كِتاب in Chapter 3,
instead of after 1,400 verbs as in the previous design.

| Ch | Title | Words | Qur'an share |
|---|---|---|---|
| 1 | Particles & Function Words | 72 | 46.8% |
| 2 | Essential Verbs | 150 | 14.4% |
| 3 | Essential Nouns | 250 | 22.9% |
| 4 | Common Verbs | 250 | 3.2% |
| 5 | Common Nouns | 500 | 5.3% |
| 6 | Frequent Verbs | 350 | 1.4% |
| 7 | Frequent Nouns | 650 | 2.2% |
| 8 | Further Verbs | 394 | 0.5% |
| 9 | Further Nouns | 650 | 1.0% |
| 10 | Rare & Unique Nouns | 634 | 0.7% |

**What Chapter 1 contains.** Chapter 1 holds particles together with closed-class nominals:
pronouns, demonstratives, relatives and interrogatives. These are grammatically *ism* and are
badged that way, but they behave like function words and are among the most frequent words in the
Qur'an.

## Why teach-then-quiz

A quiz on a word you've never seen tests, it doesn't teach. Every word gets a non-scored teach
step (`WordIntro`) immediately followed by its own quizzes. Testing recall right after exposure is
retrieval practice, and it is far more effective than a long study block followed by a test.

## A lesson

| Order | Step |
|---|---|
| 1 | Teach each word: meaning, root, verb forms, contextual senses, a highlighted verse |
| 2 | Meaning quiz (Arabic → meaning) for each word |
| 3 | A verse exercise per word, alternating between *complete the verse* (pick the missing word) and *tap the word in the verse* that has the shown meaning |
| 4 | A closing matching round |

The learning style setting (practice once, 3× or 5×) repeats the quiz steps, re-shuffled so the
same word is never asked twice in a row.

## Checkpoints

Each section of up to 10 lessons ends with a **review** (15 words) and an **exam** (20 words);
each chapter ends with a **chapter exam** (25 words). Exams require **80% on first tries**:
retrying a wrong answer is allowed, but the retry does not count. A failed exam stays open
for another attempt and is never recorded as completed.

## Contextual senses (Wujūh al-Qurʾān)

When a word's occurrences split clearly between meanings (for example مِنْ, "from / of"), the
word gets one tab per sense. Each tab shows a verse where the source translates the word with
exactly that sense. 333 words have more than one sense in English, and 870 in at least one
language.

## Spaced review

Every first-try answer updates the word's memory model (FSRS, see
[`ALGORITHMS.md`](ALGORITHMS.md)). The Daily Review brings back words just before they would be
forgotten, and the exercise type adapts to the word's strength.
