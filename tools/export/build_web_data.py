#!/usr/bin/env python3
"""
QuranicWords Master Data Compiler & Optimizer for Web App
Generates optimized, ultra-fast JSON datasets:
- data/metadata.json (Curriculum hierarchy, chapters, stats, 11 languages)
- data/words.json (4,709 Quranic lemmas with roots, transliterations, meanings, verse contexts)
- data/roots.json (251 Quranic roots mapped to lemmas with frequency)
"""

import os
import json
import re
import gzip

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
CONTENT_DIR = os.path.join(BASE_DIR, 'app', 'src', 'main', 'assets', 'content')
DATA_DIR = os.path.join(BASE_DIR, 'data')
os.makedirs(DATA_DIR, exist_ok=True)

def strip_tashkeel(text):
    if not text:
        return ''
    t = re.sub(r'\s*\([0-9]+\)\s*$', '', str(text))
    t = t.replace('\u0670', 'ا').replace('\u0627\u065F', 'ا').replace('\u06E5', 'و').replace('\u06E6', 'ي')
    t = re.sub(r'[\u064B-\u065F\u06D6-\u06ED\uFEFF]', '', t)
    t = re.sub(r'[إأآٱ]', 'ا', t)
    t = t.replace('ة', 'ه').replace('ى', 'ي')
    return t.strip()

def build_data():
    print("Loading curriculum content...")
    with open(os.path.join(CONTENT_DIR, 'chapters.json'), encoding='utf-8') as f:
        chapters = json.load(f)['chapters']
    with open(os.path.join(CONTENT_DIR, 'sections.json'), encoding='utf-8') as f:
        sections = json.load(f)['sections']
    with open(os.path.join(CONTENT_DIR, 'lessons_vocabulary.json'), encoding='utf-8') as f:
        lessons = json.load(f)['lessons']
    with open(os.path.join(CONTENT_DIR, 'exercises_vocabulary.json'), encoding='utf-8') as f:
        exercises = json.load(f)['exercises']

    SUPPORTED_LANGS = ['en', 'bn', 'ur', 'hi', 'in', 'ms', 'tr', 'fa', 'ha', 'sw', 'fr']
    CORE_VERSE_LANGS = ['en', 'bn', 'ur', 'hi', 'in', 'tr', 'fr']

    # Group exercises by lessonId
    ex_by_lesson = {}
    for ex in exercises:
        lid = ex['lessonId']
        if lid not in ex_by_lesson:
            ex_by_lesson[lid] = []
        ex_by_lesson[lid].append(ex)

    # Group lessons by (chapterId, sectionId)
    lessons_by_sec = {}
    for les in lessons:
        key = (les['chapterId'], les['sectionId'])
        if key not in lessons_by_sec:
            lessons_by_sec[key] = []
        lessons_by_sec[key].append(les)

    # Group sections by chapterId
    sec_by_ch = {}
    for sec in sections:
        cid = sec['chapterId']
        if cid not in sec_by_ch:
            sec_by_ch[cid] = []
        sec_by_ch[cid].append(sec)

    flat_words = []
    chapters_meta = []
    roots_map = {}
    total_quran_occ = 0

    for ch in chapters:
        cid = ch['id']
        ch_secs = sec_by_ch.get(cid, [])
        ch_meta = {
            'id': cid,
            'sort': ch['sortOrder'],
            'title': ch['title'],
            'words': ch['wordCount'],
            'occ': ch['quranOccurrenceCount'],
            'pct': ch['quranOccurrencePercent'],
            'secs': []
        }
        for sec in ch_secs:
            sid = sec['id']
            sec_les_list = lessons_by_sec.get((cid, sid), [])
            sec_meta = {
                'id': sid,
                'sort': sec['sortOrder'],
                'title': sec['title'],
                'words': sec['wordCount'],
                'occ': sec['quranOccurrenceCount'],
                'pct': sec['quranOccurrencePercent']
            }
            ch_meta['secs'].append(sec_meta)

            for les in sec_les_list:
                lid = les['id']
                les_ex_list = ex_by_lesson.get(lid, [])
                for ex in les_ex_list:
                    if ex['exerciseType'] == 'WORD_INTRO':
                        c = ex['content']
                        m_all = {l: c.get('meaning', {}).get(l, '') for l in SUPPORTED_LANGS if c.get('meaning', {}).get(l)}
                        v_tr = {l: c.get('exampleVerseTranslation', {}).get(l, '') for l in CORE_VERSE_LANGS if c.get('exampleVerseTranslation', {}).get(l)}
                        hl_m = {l: c.get('meaningHighlight', {}).get(l, '') for l in CORE_VERSE_LANGS if c.get('meaningHighlight', {}).get(l)}

                        poly_entries = []
                        for se in c.get('polysemyEntries', []):
                            se_m = {l: se.get('contextualMeaning', {}).get(l, '') for l in SUPPORTED_LANGS if se.get('contextualMeaning', {}).get(l)}
                            se_v_tr = {l: se.get('verseTranslation', {}).get(l, '') for l in CORE_VERSE_LANGS if se.get('verseTranslation', {}).get(l)}
                            se_hl_m = {l: se.get('translationHighlight', {}).get(l, '') for l in CORE_VERSE_LANGS if se.get('translationHighlight', {}).get(l)}
                            poly_entries.append({
                                'idx': se['meaningIndex'],
                                'm': se_m,
                                'ref': se.get('verseReference', ''),
                                'v_ar': se.get('verseArabic', ''),
                                'v_hl': [se.get('arabicWordStart', 0), se.get('arabicWordEnd', 0)],
                                'v_tr': se_v_tr,
                                'tr_hl': se_hl_m
                            })

                        root_raw = c.get('root', '').strip()
                        # Ignore placeholder dashes for rootless words (particles, pronouns)
                        root_val = '' if root_raw in ('—', '-', 'None', 'null', 'N/A') else root_raw
                        occ = c.get('quranOccurrenceCount', 1)
                        total_quran_occ += occ
                        
                        ch_num = ch.get('sortOrder', cid.replace('ch_', ''))
                        w_item = {
                            'id': c['wordId'],
                            'ar': c['arabicWord'],
                            'cl': strip_tashkeel(c['arabicWord']),
                            'tr': c.get('transliteration', ''),
                            'rt': root_val,
                            'rt_cl': strip_tashkeel(root_val),
                            'pos': c.get('partOfSpeech', 'noun'),
                            'cat': les['category'],
                            'ch': cid,
                            'ch_num': ch_num,
                            'sec': sid,
                            'les': lid,
                            'occ': occ,
                            'm': m_all,
                            'ref': c.get('exampleVerseReference', ''),
                            'v_ar': c.get('exampleVerseArabic', ''),
                            'v_hl': [c.get('arabicWordStart', 0), c.get('arabicWordEnd', 0)],
                            'v_tr': v_tr,
                            'tr_hl': hl_m
                        }
                        if len(poly_entries) > 1:
                            w_item['poly'] = poly_entries
                        
                        flat_words.append(w_item)

                        if root_val:
                            if root_val not in roots_map:
                                roots_map[root_val] = {'root': root_val, 'cl': strip_tashkeel(root_val), 'words': [], 'total_occ': 0}
                            roots_map[root_val]['words'].append(c['wordId'])
                            roots_map[root_val]['total_occ'] += occ

        chapters_meta.append(ch_meta)

    # Sort roots by total occurrence descending
    roots_list = sorted(list(roots_map.values()), key=lambda r: r['total_occ'], reverse=True)

    meta = {
        'version': '1.0.0',
        'title': "QuranicWords Master Curriculum",
        'author': "DeanyBytes",
        'website': "https://quranicwords.vercel.app/",
        'total_words': len(flat_words),
        'total_chapters': len(chapters),
        'total_sections': len(sections),
        'total_lessons': len(lessons),
        'total_roots': len(roots_list),
        'total_occurrences': total_quran_occ,
        'chapters': chapters_meta,
        'languages': [
            {'code': 'en', 'name': 'English', 'native': 'English'},
            {'code': 'bn', 'name': 'Bengali', 'native': 'বাংলা'},
            {'code': 'ur', 'name': 'Urdu', 'native': 'اردو'},
            {'code': 'hi', 'name': 'Hindi', 'native': 'हिन्दी'},
            {'code': 'in', 'name': 'Indonesian', 'native': 'Bahasa Indonesia'},
            {'code': 'ms', 'name': 'Malay', 'native': 'Bahasa Melayu'},
            {'code': 'tr', 'name': 'Turkish', 'native': 'Türkçe'},
            {'code': 'fa', 'name': 'Persian', 'native': 'فارسی'},
            {'code': 'ha', 'name': 'Hausa', 'native': 'Hausa'},
            {'code': 'sw', 'name': 'Swahili', 'native': 'Kiswahili'},
            {'code': 'fr', 'name': 'French', 'native': 'Français'}
        ]
    }

    # Create lightweight summary words for instantaneous bootstrap
    summary_words = []
    for w in flat_words:
        sw = {
            'id': w['id'],
            'ar': w['ar'],
            'cl': w['cl'],
            'tr': w['tr'],
            'rt': w['rt'],
            'pos': w['pos'],
            'cat': w['cat'],
            'ch': w['ch'],
            'ch_num': w.get('ch_num', 1),
            'sec': w['sec'],
            'les': w['les'],
            'occ': w['occ'],
            'm': w['m'],
            'ref': w['ref'],
            'has_poly': bool(w.get('poly'))
        }
        summary_words.append(sw)

    with open(os.path.join(DATA_DIR, 'metadata.json'), 'w', encoding='utf-8') as f:
        json.dump(meta, f, separators=(',', ':'), ensure_ascii=False)

    with open(os.path.join(DATA_DIR, 'words_summary.json'), 'w', encoding='utf-8') as f:
        json.dump(summary_words, f, separators=(',', ':'), ensure_ascii=False)

    with open(os.path.join(DATA_DIR, 'words.json'), 'w', encoding='utf-8') as f:
        json.dump(flat_words, f, separators=(',', ':'), ensure_ascii=False)

    with open(os.path.join(DATA_DIR, 'roots.json'), 'w', encoding='utf-8') as f:
        json.dump(roots_list, f, separators=(',', ':'), ensure_ascii=False)

    words_size = os.path.getsize(os.path.join(DATA_DIR, 'words.json'))
    words_gz = len(gzip.compress(open(os.path.join(DATA_DIR, 'words.json'), 'rb').read()))
    sum_size = os.path.getsize(os.path.join(DATA_DIR, 'words_summary.json'))
    sum_gz = len(gzip.compress(open(os.path.join(DATA_DIR, 'words_summary.json'), 'rb').read()))
    print(f"Data Build Complete!")
    print(f"  Words Full: {len(flat_words)} items ({words_size/1024/1024:.2f} MB raw, {words_gz/1024:.2f} KB gzipped)")
    print(f"  Words Summary: {len(summary_words)} items ({sum_size/1024/1024:.2f} MB raw, {sum_gz/1024:.2f} KB gzipped)")
    print(f"  Roots: {len(roots_list)} roots")
    print(f"  Metadata: {os.path.getsize(os.path.join(DATA_DIR, 'metadata.json'))/1024:.2f} KB")

if __name__ == '__main__':
    build_data()
