#!/usr/bin/env python3
"""
tools/fixes/unify_multisense_and_clean_quizzes.py
=================================================
1. Identifies all 73 polysemous words with multiple senses in exercises_vocabulary.json.
2. Unifies the contextual meanings across all senses using ' / ' separator across all 10 languages.
3. Updates app/src/main/assets/content/word_frequency.json with unified meanings.
4. Updates app/src/main/assets/content/exercises_vocabulary.json:
   - Sets unified meaning in all 4,709 WORD_INTRO exercises.
   - Updates all 4,709 MULTIPLE_CHOICE exercises:
     * Correct option label gets unified multi-sense meaning (A, B / X, Y).
     * Distractor options get unified multi-sense meanings.
     * Detects any sense collisions between distractors and the target word or between distractors.
     * Replaces colliding distractors with non-colliding candidates from nearby frequency ranks.
5. Verifies 100% coverage, 0 collisions, and valid option structure across all 4,709 exercises.
"""

import os
import json
import re

BASE_DIR = '/home/rafi/WorkSpace/QuranicWords'
CONTENT_DIR = os.path.join(BASE_DIR, 'app', 'src', 'main', 'assets', 'content')

WF_PATH = os.path.join(CONTENT_DIR, 'word_frequency.json')
EX_PATH = os.path.join(CONTENT_DIR, 'exercises_vocabulary.json')

LANGS = ['en', 'bn', 'ur', 'hi', 'in', 'ms', 'tr', 'fa', 'ha', 'sw']

def clean_part(p):
    return ' '.join(str(p).strip().split())

def main():
    print("=== 1. Loading Assets ===")
    with open(WF_PATH, 'r', encoding='utf-8') as f:
        wf_data = json.load(f)
    words = wf_data['words']
    wf_by_id = {w['id']: w for w in words}

    with open(EX_PATH, 'r', encoding='utf-8') as f:
        ex_data = json.load(f)
    exercises = ex_data['exercises']

    print(f"Loaded {len(words)} words and {len(exercises)} exercises.")

    # 2. Extract multi-sense words from word_intro
    intros = [ex['content'] for ex in exercises if ex['content'].get('type') == 'word_intro']
    polysemy_by_id = {}
    for intro in intros:
        entries = intro.get('polysemyEntries', [])
        if len(entries) > 1:
            polysemy_by_id[intro['wordId']] = entries

    print(f"Identified {len(polysemy_by_id)} multi-sense words.")

    # 3. Build unified meanings dictionary for all words
    def get_unified_meaning(wid):
        cur_m = wf_by_id[wid]['meaning']
        if wid not in polysemy_by_id:
            return cur_m
        entries = polysemy_by_id[wid]
        combined = {}
        for lang in LANGS:
            parts = []
            for e in entries:
                v = e.get('contextualMeaning', {}).get(lang, '')
                if v:
                    subparts = [clean_part(s) for s in v.split('/') if clean_part(s)]
                    for sp in subparts:
                        if sp.lower() not in [p.lower() for p in parts]:
                            parts.append(sp)
            cur = cur_m.get(lang, '')
            if cur:
                cur_parts = [clean_part(s) for s in cur.split('/') if clean_part(s)]
                for cp in cur_parts:
                    if cp.lower() not in [p.lower() for p in parts]:
                        parts.append(cp)
            combined[lang] = ' / '.join(parts) if parts else cur
        return combined

    unified_meanings = {w['id']: get_unified_meaning(w['id']) for w in words}

    # 4. Update word_frequency.json words
    updated_wf_count = 0
    for w in words:
        wid = w['id']
        if wid in polysemy_by_id:
            w['meaning'] = unified_meanings[wid]
            updated_wf_count += 1

    print(f"Updated {updated_wf_count} multi-sense words in word_frequency.json.")

    # 5. Collision checking logic
    def get_tokens(m_dict, lang):
        text = m_dict.get(lang, '')
        return set(clean_part(s).lower() for s in text.split('/') if clean_part(s))

    def words_collide(wid_a, wid_b):
        if wid_a == wid_b:
            return True
        wa = wf_by_id[wid_a]
        wb = wf_by_id[wid_b]
        # Same Arabic word (ignoring outer whitespace)
        if wa['arabicWord'].strip() == wb['arabicWord'].strip():
            return True
        ma = unified_meanings[wid_a]
        mb = unified_meanings[wid_b]
        for lang in ['en', 'bn', 'ur']:
            tok_a = get_tokens(ma, lang)
            tok_b = get_tokens(mb, lang)
            if tok_a and tok_b and tok_a.intersection(tok_b):
                return True
        return False

    # 6. Update exercises_vocabulary.json
    mc_updated_count = 0
    replaced_distractor_count = 0

    for ex in exercises:
        c = ex['content']
        ex_type = c.get('type')
        if ex_type == 'word_intro':
            wid = c.get('wordId')
            if wid in polysemy_by_id:
                c['meaning'] = unified_meanings[wid]
        elif ex_type == 'multiple_choice':
            target_wid = c['wordId']
            correct_opt_id = c['correctOptionId']
            old_options = c['options']

            # Find the slot of correct option
            correct_slot = 0
            for idx, opt in enumerate(old_options):
                if opt['id'] == correct_opt_id:
                    correct_slot = idx
                    break

            selected_wids = [target_wid]
            distractor_wids = []

            # Check existing distractors for validity
            for opt in old_options:
                opt_wid = opt['id'].replace('opt_', '')
                if opt_wid == target_wid:
                    continue
                if not any(words_collide(opt_wid, s) for s in selected_wids):
                    selected_wids.append(opt_wid)
                    distractor_wids.append(opt_wid)
                else:
                    replaced_distractor_count += 1

            # If distractors were colliding and need replacements, pick nearby rank candidates
            if len(distractor_wids) < 3:
                target_rank = wf_by_id[target_wid]['frequencyRank']
                for delta in range(1, 150):
                    for cand_rank in [target_rank - delta, target_rank + delta]:
                        if 1 <= cand_rank <= len(words):
                            cand_wid = words[cand_rank - 1]['id']
                            if not any(words_collide(cand_wid, s) for s in selected_wids):
                                selected_wids.append(cand_wid)
                                distractor_wids.append(cand_wid)
                                if len(distractor_wids) == 3:
                                    break
                    if len(distractor_wids) == 3:
                        break

            if len(distractor_wids) != 3:
                raise RuntimeError(f"Failed to find 3 non-colliding distractors for {target_wid}")

            # Reconstruct options
            correct_opt = {
                'id': f"opt_{target_wid}",
                'labelArabic': wf_by_id[target_wid]['arabicWord'],
                'label': unified_meanings[target_wid]
            }

            distractor_opts = [
                {
                    'id': f"opt_{dwid}",
                    'labelArabic': wf_by_id[dwid]['arabicWord'],
                    'label': unified_meanings[dwid]
                }
                for dwid in distractor_wids
            ]

            new_options = list(distractor_opts)
            new_options.insert(correct_slot, correct_opt)

            c['options'] = new_options
            c['correctOptionId'] = correct_opt['id']
            mc_updated_count += 1

    print(f"Updated {mc_updated_count} MULTIPLE_CHOICE exercises.")
    print(f"Replaced {replaced_distractor_count} colliding distractors.")

    # 7. Verification of all MC exercises
    print("\n=== 7. Verification Pass ===")
    total_violations = 0
    for ex in exercises:
        c = ex['content']
        if c.get('type') == 'multiple_choice':
            twid = c['wordId']
            opts = c['options']
            if len(opts) != 4:
                print(f"Violation: {ex['id']} does not have 4 options")
                total_violations += 1
            if c['correctOptionId'] != f"opt_{twid}":
                print(f"Violation: {ex['id']} correctOptionId mismatch")
                total_violations += 1
            opt_wids = [o['id'].replace('opt_', '') for o in opts]
            if len(set(opt_wids)) != 4:
                print(f"Violation: {ex['id']} duplicate option IDs")
                total_violations += 1
            for i in range(4):
                for j in range(i + 1, 4):
                    if words_collide(opt_wids[i], opt_wids[j]):
                        print(f"Violation in {ex['id']}: collision between {opt_wids[i]} and {opt_wids[j]}")
                        total_violations += 1

    if total_violations == 0:
        print("VERIFICATION 100% PASSED: 0 collisions, all 4,709 MC exercises clean!")
    else:
        raise RuntimeError(f"Verification failed with {total_violations} violations!")

    # 8. Write back to disk
    print("\n=== 8. Writing updated assets ===")
    with open(WF_PATH, 'w', encoding='utf-8') as f:
        json.dump(wf_data, f, ensure_ascii=False, indent=2)
    print("  -> word_frequency.json written successfully.")

    with open(EX_PATH, 'w', encoding='utf-8') as f:
        json.dump(ex_data, f, ensure_ascii=False)
    print("  -> exercises_vocabulary.json written successfully.")

if __name__ == '__main__':
    main()
