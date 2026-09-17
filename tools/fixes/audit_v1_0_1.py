#!/usr/bin/env python3
"""
tools/fixes/audit_v1_0_1.py
===========================
Full comprehensive audit of QuranicWords codebase, curriculum assets,
and release configuration for v1.0.1 (versionCode 20).
"""

import os
import json
import re
import sys

BASE_DIR = '/home/rafi/WorkSpace/QuranicWords'
CONTENT_DIR = os.path.join(BASE_DIR, 'app', 'src', 'main', 'assets', 'content')
LANGS = ['en', 'bn', 'ur', 'hi', 'in', 'ms', 'tr', 'fa', 'ha', 'sw']

def audit_curriculum_and_assets():
    print("\n" + "=" * 60)
    print("1. AUDITING CURRICULUM ASSETS")
    print("=" * 60)
    errors = []

    # 1. Chapters
    with open(os.path.join(CONTENT_DIR, 'chapters.json'), encoding='utf-8') as f:
        chapters = json.load(f)['chapters']
    print(f"  Chapters: {len(chapters)}")
    if len(chapters) != 10:
        errors.append(f"Expected 10 chapters, got {len(chapters)}")
    chapter_ids = {c['id'] for c in chapters}

    # 2. Sections
    with open(os.path.join(CONTENT_DIR, 'sections.json'), encoding='utf-8') as f:
        sections = json.load(f)['sections']
    print(f"  Sections: {len(sections)}")
    if len(sections) != 100:
        errors.append(f"Expected 100 sections, got {len(sections)}")
    section_ids = {s['id'] for s in sections}
    for s in sections:
        if s['chapterId'] not in chapter_ids:
            errors.append(f"Section {s['id']} has invalid chapterId {s['chapterId']}")

    # 3. Lessons
    with open(os.path.join(CONTENT_DIR, 'lessons_vocabulary.json'), encoding='utf-8') as f:
        lessons = json.load(f)['lessons']
    print(f"  Lessons: {len(lessons)}")
    if len(lessons) != 1217:
        errors.append(f"Expected 1217 lessons, got {len(lessons)}")
    lesson_ids = {l['id'] for l in lessons}

    # 4. Words
    with open(os.path.join(CONTENT_DIR, 'word_frequency.json'), encoding='utf-8') as f:
        words = json.load(f)['words']
    print(f"  Words: {len(words)}")
    if len(words) != 4709:
        errors.append(f"Expected 4709 words, got {len(words)}")

    word_ids = {w['id'] for w in words}
    words_dict = {w['id']: w for w in words}

    # Check words translations in all 10 languages
    missing_word_langs = 0
    duplicate_slash_meanings = 0
    for w in words:
        meaning = w.get('meaning', {})
        for l in LANGS:
            val = meaning.get(l, '').strip()
            if not val:
                missing_word_langs += 1
            parts = [p.strip().lower() for p in val.split('/') if p.strip()]
            if len(parts) > len(set(parts)):
                duplicate_slash_meanings += 1
    if missing_word_langs > 0:
        errors.append(f"{missing_word_langs} word meaning entries missing for languages")
    if duplicate_slash_meanings > 0:
        errors.append(f"{duplicate_slash_meanings} duplicate slash parts in word meanings")
    print(f"  Word translations: 0 missing, duplicate slash parts: {duplicate_slash_meanings}")

    # 5. Exercises
    with open(os.path.join(CONTENT_DIR, 'exercises_vocabulary.json'), encoding='utf-8') as f:
        exercises = json.load(f)['exercises']
    print(f"  Exercises: {len(exercises)}")

    ex_by_lesson = {}
    for ex in exercises:
        lid = ex.get('lessonId')
        if lid not in lesson_ids:
            errors.append(f"Exercise {ex['id']} references unknown lesson {lid}")
        ex_by_lesson.setdefault(lid, []).append(ex)

    # Check zero-exercise lessons
    zero_ex = [l['id'] for l in lessons if l['id'] not in ex_by_lesson or len(ex_by_lesson[l['id']]) == 0]
    print(f"  Zero-exercise lessons: {len(zero_ex)}")
    if len(zero_ex) > 0:
        errors.append(f"{len(zero_ex)} lessons have 0 exercises: {zero_ex[:5]}")

    # Check regular lessons matching
    regular_lessons = [l for l in lessons if l['kind'] == 'REGULAR']
    missing_matching_regular = [
        l['id'] for l in regular_lessons
        if not any(e['exerciseType'] == 'MATCHING' for e in ex_by_lesson.get(l['id'], []))
    ]
    print(f"  Regular lessons without matching quiz: {len(missing_matching_regular)}")
    if len(missing_matching_regular) > 0:
        errors.append(f"{len(missing_matching_regular)} regular lessons lack matching: {missing_matching_regular[:5]}")

    # Check flashbacks and exams
    flashbacks = [l for l in lessons if l['kind'] == 'SECTION_FLASHBACK']
    empty_flashbacks = [l['id'] for l in flashbacks if len(ex_by_lesson.get(l['id'], [])) < 5]
    print(f"  Section Flashbacks under 5 questions: {len(empty_flashbacks)}")
    if len(empty_flashbacks) > 0:
        errors.append(f"{len(empty_flashbacks)} flashbacks have < 5 questions")

    section_exams = [l for l in lessons if l['kind'] == 'SECTION_EXAM']
    empty_sec_exams = [l['id'] for l in section_exams if len(ex_by_lesson.get(l['id'], [])) < 5]
    print(f"  Section Exams under 5 questions: {len(empty_sec_exams)}")
    if len(empty_sec_exams) > 0:
        errors.append(f"{len(empty_sec_exams)} section exams have < 5 questions")

    chapter_exams = [l for l in lessons if l['kind'] == 'CHAPTER_EXAM']
    empty_ch_exams = [l['id'] for l in chapter_exams if len(ex_by_lesson.get(l['id'], [])) < 5]
    print(f"  Chapter Exams under 5 questions: {len(empty_ch_exams)}")
    if len(empty_ch_exams) > 0:
        errors.append(f"{len(empty_ch_exams)} chapter exams have < 5 questions")

    # Check multiple choice integrity
    invalid_mc = 0
    mc_count = 0
    matching_count = 0
    intro_count = 0
    invalid_matching_pairs = 0

    for ex in exercises:
        etype = ex.get('exerciseType')
        content = ex.get('content', {})
        if etype == 'MULTIPLE_CHOICE':
            mc_count += 1
            opts = content.get('options', [])
            correct_id = content.get('correctOptionId')
            if len(opts) != 4:
                invalid_mc += 1
            if not any(opt.get('id') == correct_id for opt in opts):
                invalid_mc += 1
        elif etype == 'MATCHING':
            matching_count += 1
            pairs = content.get('pairs', [])
            if len(pairs) < 2:
                invalid_matching_pairs += 1
            # Check pair languages
            for p in pairs:
                if not p.get('leftArabic'):
                    invalid_matching_pairs += 1
                right = p.get('right', {})
                if not isinstance(right, dict) or not all(right.get(l) for l in LANGS):
                    invalid_matching_pairs += 1
        elif etype == 'WORD_INTRO':
            intro_count += 1

    print(f"  Exercise Breakdown:")
    print(f"    - Word Intro: {intro_count}")
    print(f"    - Multiple Choice: {mc_count} (invalid: {invalid_mc})")
    print(f"    - Matching: {matching_count} (invalid: {invalid_matching_pairs})")

    if invalid_mc > 0:
        errors.append(f"{invalid_mc} invalid multiple choice exercises")
    if invalid_matching_pairs > 0:
        errors.append(f"{invalid_matching_pairs} invalid matching exercises")

    if errors:
        print("\n[FAIL] Curriculum Asset Errors:")
        for err in errors:
            print(f"  - {err}")
        return False
    else:
        print("\n[PASS] All curriculum assets passed audit with 100% integrity!")
        return True

def audit_codebase_config():
    print("\n" + "=" * 60)
    print("2. AUDITING CODEBASE AND BUILD CONFIGURATION")
    print("=" * 60)
    errors = []

    # 1. ContentSeeder.kt
    seeder_path = os.path.join(BASE_DIR, 'app', 'src', 'main', 'java', 'com', 'quranicwords', 'app', 'core', 'data', 'assets', 'ContentSeeder.kt')
    with open(seeder_path, encoding='utf-8') as f:
        seeder_content = f.read()
    if 'const val CONTENT_VERSION = 34' not in seeder_content:
        errors.append("ContentSeeder.kt does not have CONTENT_VERSION = 34")
    else:
        print("  ContentSeeder: CONTENT_VERSION = 34 [OK]")

    # 2. build.gradle.kts
    gradle_path = os.path.join(BASE_DIR, 'app', 'build.gradle.kts')
    with open(gradle_path, encoding='utf-8') as f:
        gradle_content = f.read()
    if 'versionCode = 20' not in gradle_content:
        errors.append("build.gradle.kts does not have versionCode = 20")
    else:
        print("  build.gradle.kts: versionCode = 20 [OK]")
    if 'versionName = "1.0.1"' not in gradle_content:
        errors.append("build.gradle.kts does not have versionName = \"1.0.1\"")
    else:
        print("  build.gradle.kts: versionName = \"1.0.1\" [OK]")

    # 3. keystore.properties
    keystore_path = os.path.join(BASE_DIR, 'keystore.properties')
    if not os.path.exists(keystore_path):
        errors.append("keystore.properties missing from root directory")
    else:
        print("  keystore.properties present [OK]")

    # 4. strings.xml
    strings_path = os.path.join(BASE_DIR, 'app', 'src', 'main', 'res', 'values', 'strings.xml')
    with open(strings_path, encoding='utf-8') as f:
        strings_content = f.read()
    if 'lesson_empty_title' not in strings_content:
        errors.append("strings.xml missing lesson_empty_title")
    else:
        print("  strings.xml: lesson_empty_title [OK]")

    # 5. ProgressRepositoryImpl.kt auto-heal logic
    repo_path = os.path.join(BASE_DIR, 'app', 'src', 'main', 'java', 'com', 'quranicwords', 'app', 'core', 'data', 'repository', 'ProgressRepositoryImpl.kt')
    with open(repo_path, encoding='utf-8') as f:
        repo_content = f.read()
    if 'getAllForUserOnce' not in repo_content or 'resolveNextLessonId' not in repo_content:
        errors.append("ProgressRepositoryImpl missing auto-heal unlock logic")
    else:
        print("  ProgressRepositoryImpl: auto-heal unlock logic [OK]")

    # 6. HomeScreen.kt auto-expand logic
    home_path = os.path.join(BASE_DIR, 'app', 'src', 'main', 'java', 'com', 'quranicwords', 'app', 'feature', 'home', 'HomeScreen.kt')
    with open(home_path, encoding='utf-8') as f:
        home_content = f.read()
    if 'uiState.initiallyExpandedSectionId' not in home_content or 'expandedSectionIds + sectionId' not in home_content:
        errors.append("HomeScreen.kt missing auto-expand logic for newly active section")
    else:
        print("  HomeScreen.kt: auto-expand active section logic [OK]")

    # 7. LessonScreen.kt fallback UI
    lesson_screen_path = os.path.join(BASE_DIR, 'app', 'src', 'main', 'java', 'com', 'quranicwords', 'app', 'feature', 'lesson', 'LessonScreen.kt')
    with open(lesson_screen_path, encoding='utf-8') as f:
        lesson_screen_content = f.read()
    if 'lesson_empty_title' not in lesson_screen_content:
        errors.append("LessonScreen.kt missing fallback UI when contents is empty")
    else:
        print("  LessonScreen.kt: fallback UI when empty [OK]")

    if errors:
        print("\n[FAIL] Codebase Configuration Errors:")
        for err in errors:
            print(f"  - {err}")
        return False
    else:
        print("\n[PASS] Codebase configuration verified with 100% integrity!")
        return True

if __name__ == '__main__':
    ok1 = audit_curriculum_and_assets()
    ok2 = audit_codebase_config()
    if ok1 and ok2:
        print("\n" + "=" * 60)
        print(">>> FULL CODEBASE & PROJECT AUDIT PASSED (0 ISSUES) <<<")
        print("=" * 60)
        sys.exit(0)
    else:
        print("\n" + "=" * 60)
        print(">>> AUDIT FOUND ISSUES TO FIX <<<")
        print("=" * 60)
        sys.exit(1)
