#!/usr/bin/env python3
"""
QuranicWords Interactive HTML Dictionary Generator (100% Precision Grounded)
=============================================================================
Generates a standalone, beautiful, interactive single-file HTML dictionary:
`QuranicWords_Dictionary.html`

Features:
- Complete 10 Chapters, 100 Sections, 1,214 Lessons, 4,709 Qur'anic Lemmas.
- 6 Supported Master Languages: English, Bangla (বাংলা), Urdu (اردو), Bahasa Indonesia, Türkçe, Français.
- High-fidelity visual highlights for Arabic verses and all 6-language verse translations.
- Interactive Polysemy / Wujūh al-Qur'an multi-meaning tabs with instant contextual updates.
- Real-time search across Arabic (with/without tashkeel), Transliteration, Root, English, Bengali, Urdu, and Surah references.
- Multi-category filtering (Particles, Verbs, Nouns, Polysemy), chapter selector, and language visibility toggles.
- Multiple view modes: Curriculum Tree View, Compact Dictionary Table.
- 100% self-contained, offline-ready with embedded data and styling.
"""

import os
import json
import re
import html

BASE_DIR = '/home/rafi/WorkSpace/QuranicWords'
CONTENT_DIR = os.path.join(BASE_DIR, 'app', 'src', 'main', 'assets', 'content')
OUTPUT_FILE = os.path.join(BASE_DIR, 'QuranicWords_Dictionary.html')

def strip_tashkeel(text):
    if not text:
        return ''
    t = re.sub(r'\s*\([0-9]+\)\s*$', '', str(text))
    t = t.replace('\u0670', 'ا').replace('\u0627\u065F', 'ا').replace('\u06E5', 'و').replace('\u06E6', 'ي')
    t = re.sub(r'[\u064B-\u065F\u06D6-\u06ED\uFEFF]', '', t)
    t = re.sub(r'[إأآٱ]', 'ا', t)
    t = t.replace('ة', 'ه').replace('ى', 'ي')
    return t.strip()

def highlight_arabic(verse, start, end, fallback_word=''):
    if not verse:
        return ''
    if start is not None and end is not None and 0 <= start < end <= len(verse):
        before = html.escape(verse[:start])
        target = html.escape(verse[start:end])
        after = html.escape(verse[end:])
        return f'{before}<mark class="ar-hl">{target}</mark>{after}'
    v_escaped = html.escape(verse)
    if fallback_word:
        w_esc = html.escape(fallback_word)
        if w_esc in v_escaped:
            return v_escaped.replace(w_esc, f'<mark class="ar-hl">{w_esc}</mark>', 1)
    return v_escaped

def highlight_translation(trans, hl_word):
    if not trans:
        return ''
    if not hl_word or len(hl_word.strip()) < 2:
        return html.escape(trans)
    hw = hl_word.strip()
    idx = trans.lower().find(hw.lower())
    if idx >= 0:
        before = html.escape(trans[:idx])
        actual = html.escape(trans[idx:idx+len(hw)])
        after = html.escape(trans[idx+len(hw):])
        return f'{before}<mark class="tr-hl">{actual}</mark>{after}'
    # Sub-phrase fallback
    words = hw.split()
    if len(words) > 1:
        for w in words:
            w = w.strip('.,;:!?()[]{}')
            if len(w) >= 2:
                w_idx = trans.lower().find(w.lower())
                if w_idx >= 0:
                    before = html.escape(trans[:w_idx])
                    actual = html.escape(trans[w_idx:w_idx+len(w)])
                    after = html.escape(trans[w_idx+len(w):])
                    return f'{before}<mark class="tr-hl">{actual}</mark>{after}'
    return html.escape(trans)

def main():
    print("Loading curriculum content JSON files...")
    with open(os.path.join(CONTENT_DIR, 'chapters.json'), encoding='utf-8') as f:
        chapters = json.load(f)['chapters']
    with open(os.path.join(CONTENT_DIR, 'sections.json'), encoding='utf-8') as f:
        sections = json.load(f)['sections']
    with open(os.path.join(CONTENT_DIR, 'lessons_vocabulary.json'), encoding='utf-8') as f:
        lessons = json.load(f)['lessons']
    with open(os.path.join(CONTENT_DIR, 'exercises_vocabulary.json'), encoding='utf-8') as f:
        exercises = json.load(f)['exercises']

    print(f"Loaded {len(chapters)} chapters, {len(sections)} sections, {len(lessons)} lessons, {len(exercises)} exercises.")

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
        cid = les['chapterId']
        sid = les['sectionId']
        key = (cid, sid)
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

    curriculum_tree = []
    total_words_count = 0
    poly_words_count = 0

    for ch in chapters:
        cid = ch['id']
        ch_secs = sec_by_ch.get(cid, [])
        ch_node = {
            'id': cid,
            'sortOrder': ch['sortOrder'],
            'title': ch['title'],
            'wordCount': ch['wordCount'],
            'occurrenceCount': ch['quranOccurrenceCount'],
            'occurrencePercent': ch['quranOccurrencePercent'],
            'sections': []
        }
        
        for sec in ch_secs:
            sid = sec['id']
            sec_les_list = lessons_by_sec.get((cid, sid), [])
            sec_node = {
                'id': sid,
                'sortOrder': sec['sortOrder'],
                'title': sec['title'],
                'wordCount': sec['wordCount'],
                'occurrenceCount': sec['quranOccurrenceCount'],
                'occurrencePercent': sec['quranOccurrencePercent'],
                'lessons': []
            }
            
            for les in sec_les_list:
                lid = les['id']
                les_ex_list = ex_by_lesson.get(lid, [])
                words = []
                for ex in les_ex_list:
                    if ex['exerciseType'] == 'WORD_INTRO':
                        c = ex['content']
                        total_words_count += 1
                        
                        w_item = {
                            'wordId': c['wordId'],
                            'arabicWord': c['arabicWord'],
                            'normArabic': strip_tashkeel(c['arabicWord']),
                            'transliteration': c.get('transliteration', ''),
                            'root': c.get('root'),
                            'partOfSpeech': c.get('partOfSpeech', ''),
                            'category': les['category'],
                            'quranOccurrenceCount': c.get('quranOccurrenceCount', 1),
                            'meaning': c.get('meaning', {}),
                            'exampleVerseReference': c.get('exampleVerseReference', ''),
                            'exampleVerseArabic': c.get('exampleVerseArabic', ''),
                            'arabicWordStart': c.get('arabicWordStart'),
                            'arabicWordEnd': c.get('arabicWordEnd'),
                            'exampleVerseArabicHl': highlight_arabic(c.get('exampleVerseArabic', ''), c.get('arabicWordStart'), c.get('arabicWordEnd'), c.get('arabicWord', '')),
                            'exampleVerseTranslation': c.get('exampleVerseTranslation', {}),
                            'meaningHighlight': c.get('meaningHighlight', {}),
                            'exampleVerseTranslationHl': {
                                lang: highlight_translation(c.get('exampleVerseTranslation', {}).get(lang, ''), c.get('meaningHighlight', {}).get(lang, ''))
                                for lang in ['en', 'bn', 'ur', 'in', 'tr', 'fr']
                            },
                            'polysemyEntries': []
                        }
                        
                        # Process polysemy entries
                        for se in c.get('polysemyEntries', []):
                            se_item = {
                                'meaningIndex': se['meaningIndex'],
                                'contextualMeaning': se.get('contextualMeaning', {}),
                                'verseReference': se.get('verseReference', ''),
                                'verseArabic': se.get('verseArabic', ''),
                                'arabicWordStart': se.get('arabicWordStart'),
                                'arabicWordEnd': se.get('arabicWordEnd'),
                                'verseArabicHl': highlight_arabic(se.get('verseArabic', ''), se.get('arabicWordStart'), se.get('arabicWordEnd'), c.get('arabicWord', '')),
                                'verseTranslation': se.get('verseTranslation', {}),
                                'translationHighlight': se.get('translationHighlight', {}),
                                'verseTranslationHl': {
                                    lang: highlight_translation(se.get('verseTranslation', {}).get(lang, ''), se.get('translationHighlight', {}).get(lang, ''))
                                    for lang in ['en', 'bn', 'ur', 'in', 'tr', 'fr']
                                }
                            }
                            w_item['polysemyEntries'].append(se_item)
                            
                        if len(w_item['polysemyEntries']) > 1:
                            poly_words_count += 1

                        words.append(w_item)
                        
                sec_node['lessons'].append({
                    'id': lid,
                    'sortOrder': les['sortOrder'],
                    'kind': les['kind'],
                    'category': les['category'],
                    'title': les['title'],
                    'words': words
                })
                
            ch_node['sections'].append(sec_node)
        curriculum_tree.append(ch_node)

    print(f"Total Words Processed: {total_words_count}")
    print(f"Total Multi-meaning (Polysemous) Words: {poly_words_count}")

    # Generate Compact JSON
    curriculum_json = json.dumps(curriculum_tree, ensure_ascii=False)

    html_template = f"""<!DOCTYPE html>
<html lang="en" class="scroll-smooth">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>QuranicWords Master Curriculum Dictionary (100% Verified)</title>
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Amiri:ital,wght@0,400;0,700;1,400&family=Hind+Siliguri:wght@400;500;600;700&family=Inter:wght@300;400;500;600;700;800&family=Noto+Naskh+Arabic:wght@400;600;700&family=Noto+Nastaliq+Urdu:wght@400;600;700&family=Scheherazade+New:wght@400;600;700&display=swap" rel="stylesheet">
    
    <style>
        :root {{
            --bg-primary: #f8fafc;
            --bg-card: #ffffff;
            --bg-card-header: #f1f5f9;
            --bg-sidebar: #0f172a;
            --text-main: #1e293b;
            --text-muted: #64748b;
            --text-light: #94a3b8;
            --border: #e2e8f0;
            --emerald: #059669;
            --emerald-light: #ecfdf5;
            --emerald-border: #a7f3d0;
            --gold: #d97706;
            --gold-light: #fef3c7;
            --gold-border: #fde68a;
            --blue: #2563eb;
            --blue-light: #eff6ff;
            --purple: #7c3aed;
            --purple-light: #f5f3ff;
            --shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
            --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.04);
            --radius: 12px;
            --radius-lg: 16px;
        }}

        .dark {{
            --bg-primary: #0b0f19;
            --bg-card: #151c2e;
            --bg-card-header: #1e293b;
            --bg-sidebar: #070a12;
            --text-main: #f1f5f9;
            --text-muted: #94a3b8;
            --text-light: #64748b;
            --border: #2d3748;
            --emerald-light: rgba(5, 150, 105, 0.15);
            --emerald-border: rgba(5, 150, 105, 0.3);
            --gold-light: rgba(217, 119, 6, 0.15);
            --gold-border: rgba(217, 119, 6, 0.3);
            --blue-light: rgba(37, 99, 235, 0.15);
            --purple-light: rgba(124, 58, 237, 0.15);
        }}

        * {{
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }}

        body {{
            font-family: 'Inter', system-ui, -apple-system, sans-serif;
            background-color: var(--bg-primary);
            color: var(--text-main);
            line-height: 1.5;
            display: flex;
            min-height: 100vh;
        }}

        /* Typography fonts for languages */
        .font-arabic {{
            font-family: 'Scheherazade New', 'Amiri', 'Noto Naskh Arabic', serif;
            direction: rtl;
        }}
        .font-bn {{
            font-family: 'Hind Siliguri', 'Inter', sans-serif;
        }}
        .font-ur {{
            font-family: 'Noto Nastaliq Urdu', 'Scheherazade New', serif;
            direction: rtl;
        }}

        /* Highlights */
        mark.ar-hl {{
            background: linear-gradient(135deg, #f59e0b, #d97706);
            color: #ffffff;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 6px;
            box-shadow: 0 2px 4px rgba(217, 119, 6, 0.25);
            display: inline-block;
        }}

        mark.tr-hl {{
            background-color: #fef08a;
            color: #854d0e;
            font-weight: 700;
            padding: 1px 6px;
            border-radius: 4px;
            border: 1px solid #fde047;
        }}
        .dark mark.tr-hl {{
            background-color: #854d0e;
            color: #fef08a;
            border-color: #a16207;
        }}

        /* Sidebar Navigation */
        #sidebar {{
            width: 320px;
            background-color: var(--bg-sidebar);
            color: #f8fafc;
            height: 100vh;
            position: sticky;
            top: 0;
            overflow-y: auto;
            flex-shrink: 0;
            display: flex;
            flex-direction: column;
            z-index: 40;
            border-right: 1px solid #1e293b;
        }}

        .sidebar-header {{
            padding: 20px;
            background: linear-gradient(135deg, #064e3b 0%, #0f172a 100%);
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }}

        .sidebar-nav {{
            padding: 12px;
            flex: 1;
        }}

        .nav-chapter {{
            margin-bottom: 8px;
            border-radius: 8px;
            overflow: hidden;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid rgba(255, 255, 255, 0.05);
        }}

        .nav-chapter-btn {{
            width: 100%;
            text-align: left;
            padding: 10px 14px;
            background: none;
            border: none;
            color: #e2e8f0;
            font-weight: 600;
            font-size: 13px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: space-between;
            transition: all 0.2s;
        }}
        .nav-chapter-btn:hover {{
            background: rgba(255, 255, 255, 0.08);
            color: #34d399;
        }}

        .nav-sections-list {{
            padding: 4px 8px 8px 16px;
            font-size: 12px;
        }}

        .nav-sec-link {{
            display: block;
            padding: 5px 8px;
            color: #94a3b8;
            text-decoration: none;
            border-radius: 4px;
            transition: all 0.15s;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }}
        .nav-sec-link:hover {{
            background: rgba(52, 211, 153, 0.1);
            color: #34d399;
        }}

        /* Main Content Container */
        #main-wrapper {{
            flex: 1;
            display: flex;
            flex-direction: column;
            min-width: 0;
            height: 100vh;
            overflow-y: auto;
        }}

        /* Sticky Control Bar */
        #topbar {{
            position: sticky;
            top: 0;
            background-color: var(--bg-card);
            border-bottom: 1px solid var(--border);
            padding: 16px 24px;
            z-index: 30;
            box-shadow: var(--shadow);
        }}

        .search-box {{
            position: relative;
            flex: 1;
        }}
        .search-box input {{
            width: 100%;
            padding: 12px 16px 12px 42px;
            border-radius: 10px;
            border: 2px solid var(--border);
            background-color: var(--bg-primary);
            color: var(--text-main);
            font-size: 15px;
            font-weight: 500;
            outline: none;
            transition: all 0.2s;
        }}
        .search-box input:focus {{
            border-color: var(--emerald);
            box-shadow: 0 0 0 3px rgba(5, 150, 105, 0.2);
        }}
        .search-icon {{
            position: absolute;
            left: 14px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
            font-size: 18px;
        }}

        /* Filter Chips */
        .chips-row {{
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            align-items: center;
            margin-top: 12px;
        }}

        .filter-chip {{
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            border: 1px solid var(--border);
            background-color: var(--bg-card);
            color: var(--text-muted);
            transition: all 0.15s;
            user-select: none;
        }}
        .filter-chip:hover {{
            border-color: var(--emerald);
            color: var(--emerald);
        }}
        .filter-chip.active {{
            background-color: var(--emerald);
            color: #ffffff;
            border-color: var(--emerald);
        }}

        /* Language Toggle Checkboxes */
        .lang-toggles {{
            display: flex;
            gap: 12px;
            align-items: center;
            font-size: 13px;
            font-weight: 600;
            color: var(--text-muted);
        }}
        .lang-toggle-label {{
            display: flex;
            align-items: center;
            gap: 4px;
            cursor: pointer;
            user-select: none;
        }}

        /* Content Area */
        #content-area {{
            padding: 24px;
            max-width: 1400px;
            margin: 0 auto;
            width: 100%;
        }}

        /* Chapter Card */
        .chapter-section {{
            margin-bottom: 40px;
            scroll-margin-top: 140px;
        }}

        .chapter-header-banner {{
            background: linear-gradient(135deg, #064e3b 0%, #047857 50%, #0f172a 100%);
            color: #ffffff;
            padding: 24px;
            border-radius: var(--radius-lg);
            box-shadow: var(--shadow-lg);
            margin-bottom: 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }}

        .section-box {{
            background-color: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: var(--radius-lg);
            margin-bottom: 24px;
            box-shadow: var(--shadow);
            overflow: hidden;
            scroll-margin-top: 140px;
        }}

        .section-header-bar {{
            padding: 16px 20px;
            background-color: var(--bg-card-header);
            border-bottom: 1px solid var(--border);
            display: flex;
            justify-content: space-between;
            align-items: center;
            cursor: pointer;
        }}

        .lesson-container {{
            padding: 20px;
            border-bottom: 1px dashed var(--border);
        }}
        .lesson-container:last-child {{
            border-bottom: none;
        }}

        .lesson-header-pill {{
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 4px 12px;
            border-radius: 12px;
            background-color: var(--emerald-light);
            color: var(--emerald);
            border: 1px solid var(--emerald-border);
            font-size: 13px;
            font-weight: 700;
            margin-bottom: 16px;
        }}

        /* Word Cards Grid */
        .words-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(620px, 1fr));
            gap: 20px;
        }}
        @media (max-width: 768px) {{
            .words-grid {{
                grid-template-columns: 1fr;
            }}
        }}

        .word-card {{
            background-color: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            box-shadow: var(--shadow);
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 14px;
            transition: transform 0.15s, box-shadow 0.15s;
        }}
        .word-card:hover {{
            box-shadow: var(--shadow-lg);
            border-color: var(--emerald-border);
        }}

        .word-card-topbar {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid var(--border);
            padding-bottom: 10px;
        }}

        .badge-id {{
            font-size: 12px;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 6px;
            background: var(--bg-card-header);
            color: var(--text-muted);
        }}

        .badge-pos {{
            font-size: 12px;
            font-weight: 600;
            padding: 2px 10px;
            border-radius: 12px;
            background: var(--blue-light);
            color: var(--blue);
        }}

        .badge-occ {{
            font-size: 12px;
            font-weight: 700;
            padding: 2px 10px;
            border-radius: 12px;
            background: var(--gold-light);
            color: var(--gold);
        }}

        /* Word Presentation */
        .word-main-display {{
            display: flex;
            align-items: baseline;
            justify-content: space-between;
            background: var(--bg-primary);
            padding: 14px 18px;
            border-radius: 10px;
            border: 1px solid var(--border);
        }}

        .arabic-lemma {{
            font-size: 34px;
            font-weight: 700;
            color: #064e3b;
            line-height: 1.2;
        }}
        .dark .arabic-lemma {{
            color: #34d399;
        }}

        .word-meta {{
            text-align: right;
            display: flex;
            flex-direction: column;
            gap: 2px;
        }}
        .translit-text {{
            font-size: 15px;
            font-weight: 600;
            color: var(--text-muted);
            font-style: italic;
        }}
        .root-text {{
            font-size: 13px;
            font-weight: 700;
            color: var(--gold);
        }}

        /* Meanings 6-Language Grid */
        .meanings-grid {{
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 8px;
            font-size: 13px;
        }}
        @media (max-width: 500px) {{
            .meanings-grid {{
                grid-template-columns: 1fr;
            }}
        }}

        .meaning-item {{
            padding: 8px 12px;
            border-radius: 8px;
            background-color: var(--bg-primary);
            border: 1px solid var(--border);
            display: flex;
            align-items: baseline;
            gap: 8px;
        }}
        .lang-flag {{
            font-size: 14px;
            font-weight: 700;
            flex-shrink: 0;
        }}
        .meaning-val {{
            font-weight: 600;
            color: var(--text-main);
            word-break: break-word;
        }}

        /* Example Verse Container */
        .verse-box {{
            background: linear-gradient(180deg, var(--bg-card-header) 0%, var(--bg-card) 100%);
            border: 1px solid var(--border);
            border-left: 4px solid var(--emerald);
            border-radius: 8px;
            padding: 14px;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }}

        .verse-meta-row {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 12px;
            font-weight: 700;
            color: var(--emerald);
        }}

        .verse-arabic-text {{
            font-size: 22px;
            line-height: 1.8;
            color: #0f172a;
            text-align: right;
            padding: 4px 0;
        }}
        .dark .verse-arabic-text {{
            color: #f8fafc;
        }}

        .verse-translations-list {{
            display: flex;
            flex-direction: column;
            gap: 6px;
            font-size: 13px;
            color: var(--text-main);
            border-top: 1px dashed var(--border);
            padding-top: 8px;
        }}

        .v-trans-item {{
            display: flex;
            gap: 8px;
            line-height: 1.4;
        }}
        .v-trans-tag {{
            font-weight: 700;
            color: var(--text-muted);
            min-width: 24px;
            flex-shrink: 0;
        }}

        /* Polysemy Badges & Tabs */
        .polysemy-card-block {{
            background-color: var(--purple-light);
            border: 1px solid rgba(124, 58, 237, 0.25);
            border-radius: 10px;
            padding: 12px;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }}

        .poly-tabs-header {{
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
            align-items: center;
        }}

        .poly-tab-btn {{
            padding: 4px 10px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 700;
            border: 1px solid rgba(124, 58, 237, 0.3);
            background: var(--bg-card);
            color: var(--purple);
            cursor: pointer;
            transition: all 0.15s;
        }}
        .poly-tab-btn.active {{
            background: var(--purple);
            color: #ffffff;
            border-color: var(--purple);
        }}

        /* Table View */
        #table-view-wrapper {{
            display: none;
            overflow-x: auto;
            background: var(--bg-card);
            border-radius: var(--radius-lg);
            border: 1px solid var(--border);
            box-shadow: var(--shadow);
        }}

        .dict-table {{
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
        }}
        .dict-table th {{
            background-color: var(--bg-card-header);
            padding: 12px 14px;
            text-align: left;
            font-weight: 700;
            border-bottom: 2px solid var(--border);
            position: sticky;
            top: 0;
            z-index: 10;
        }}
        .dict-table td {{
            padding: 10px 14px;
            border-bottom: 1px solid var(--border);
            vertical-align: top;
        }}
        .dict-table tr:hover {{
            background-color: var(--bg-primary);
        }}

        /* Utility buttons */
        .btn {{
            padding: 8px 14px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            border: 1px solid var(--border);
            background: var(--bg-card);
            color: var(--text-main);
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.15s;
        }}
        .btn:hover {{
            background: var(--bg-primary);
            border-color: var(--emerald);
        }}
        .btn-primary {{
            background: var(--emerald);
            color: #ffffff;
            border-color: var(--emerald);
        }}
        .btn-primary:hover {{
            background: #047857;
        }}

        .count-pill {{
            background: rgba(0, 0, 0, 0.2);
            color: #ffffff;
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 700;
        }}

        /* Scrollbar */
        ::-webkit-scrollbar {{
            width: 8px;
            height: 8px;
        }}
        ::-webkit-scrollbar-track {{
            background: var(--bg-primary);
        }}
        ::-webkit-scrollbar-thumb {{
            background: var(--text-light);
            border-radius: 4px;
        }}
        ::-webkit-scrollbar-thumb:hover {{
            background: var(--text-muted);
        }}
    </style>
</head>
<body>

    <!-- Left Sidebar: Chapters & Sections Directory -->
    <aside id="sidebar">
        <div class="sidebar-header">
            <div style="display:flex; align-items:center; gap:10px;">
                <span style="font-size:24px;">📖</span>
                <div>
                    <h2 style="font-size:16px; font-weight:800; color:#ffffff;">QuranicWords</h2>
                    <p style="font-size:11px; color:#a7f3d0;">Master Curriculum Dictionary</p>
                </div>
            </div>
            <div style="margin-top:14px; font-size:12px; color:#cbd5e1; display:flex; justify-content:space-between;">
                <span>10 Chapters · 4,709 Lemmas</span>
                <span class="count-pill">v7 Master</span>
            </div>
        </div>

        <nav class="sidebar-nav">
            <div style="padding: 6px 8px; font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em;">
                Curriculum Table of Contents
            </div>
            <div id="sidebar-chapters-list">
                <!-- Dynamically generated chapter links -->
            </div>
        </nav>
    </aside>

    <!-- Main Content Area -->
    <div id="main-wrapper">
        <!-- Sticky Top Control Bar -->
        <header id="topbar">
            <div style="display:flex; gap:14px; align-items:center;">
                <!-- Live Search Box -->
                <div class="search-box">
                    <span class="search-icon">🔍</span>
                    <input type="text" id="search-input" placeholder="Search Arabic, English, বাংলা, اردو, Root (e.g. كتب), POS, Ref (e.g. 2:255)...">
                </div>

                <!-- View Switcher -->
                <div style="display:flex; gap:6px;">
                    <button id="view-tree-btn" class="btn btn-primary" onclick="switchView('tree')">📚 Tree View</button>
                    <button id="view-table-btn" class="btn" onclick="switchView('table')">📋 Table View</button>
                    <button id="theme-toggle-btn" class="btn" onclick="toggleDarkMode()">🌓 Mode</button>
                </div>
            </div>

            <!-- Filter Controls Row -->
            <div class="chips-row">
                <span style="font-size:12px; font-weight:700; color:var(--text-muted); margin-right:4px;">Filter:</span>
                <div class="filter-chip active" onclick="filterCategory('ALL', this)">All (4,709)</div>
                <div class="filter-chip" onclick="filterCategory('PARTICLE', this)">Particles (173)</div>
                <div class="filter-chip" onclick="filterCategory('VERB', this)">Verbs (1,479)</div>
                <div class="filter-chip" onclick="filterCategory('NOUN', this)">Nouns (3,057)</div>
                <div class="filter-chip" onclick="filterCategory('POLYSEMY', this)">✨ Polysemy Only (44)</div>

                <div style="margin-left:auto; display:flex; gap:10px; align-items:center;">
                    <!-- Chapter Dropdown -->
                    <select id="chapter-select" class="btn" onchange="onChapterSelect(this.value)" style="padding:6px 12px;">
                        <option value="ALL">All 10 Chapters</option>
                        <option value="ch_01">Ch 1: Grammatical Particles (173)</option>
                        <option value="ch_02">Ch 2: High-Frequency Verbs (500)</option>
                        <option value="ch_03">Ch 3: Essential Verbal Forms (500)</option>
                        <option value="ch_04">Ch 4: Specialized Verbs (479)</option>
                        <option value="ch_05">Ch 5: Divine Names & Core Nominals (510)</option>
                        <option value="ch_06">Ch 6: Essential Quranic Nominals (510)</option>
                        <option value="ch_07">Ch 7: Devotional & Faith Nominals (510)</option>
                        <option value="ch_08">Ch 8: Prophetic & Narrative Nominals (510)</option>
                        <option value="ch_09">Ch 9: Moral & Social Nominals (510)</option>
                        <option value="ch_10">Ch 10: Cosmic & Lexical Nominals (507)</option>
                    </select>

                    <button class="btn" onclick="expandAll()">Expand All</button>
                    <button class="btn" onclick="collapseAll()">Collapse All</button>
                </div>
            </div>

            <!-- Languages Visibility Toggle Row -->
            <div class="chips-row" style="margin-top:10px; padding-top:8px; border-top:1px dashed var(--border);">
                <span style="font-size:12px; font-weight:700; color:var(--text-muted);">Languages:</span>
                <div class="lang-toggles">
                    <label class="lang-toggle-label"><input type="checkbox" checked onchange="toggleLang('en', this.checked)"> 🇬🇧 English</label>
                    <label class="lang-toggle-label"><input type="checkbox" checked onchange="toggleLang('bn', this.checked)"> 🇧🇩 বাংলা</label>
                    <label class="lang-toggle-label"><input type="checkbox" checked onchange="toggleLang('ur', this.checked)"> 🇵🇰 اردو</label>
                    <label class="lang-toggle-label"><input type="checkbox" checked onchange="toggleLang('in', this.checked)"> 🇮🇩 Bahasa</label>
                    <label class="lang-toggle-label"><input type="checkbox" checked onchange="toggleLang('tr', this.checked)"> 🇹🇷 Türkçe</label>
                    <label class="lang-toggle-label"><input type="checkbox" checked onchange="toggleLang('fr', this.checked)"> 🇫🇷 Français</label>
                </div>
                <div id="stats-badge" style="margin-left:auto; font-size:12px; font-weight:700; color:var(--emerald);">
                    Showing 4,709 / 4,709 words
                </div>
            </div>
        </header>

        <!-- Main Content Area -->
        <main id="content-area">
            <!-- Curriculum Tree View Container -->
            <div id="curriculum-tree-view">
                <!-- Dynamically rendered chapters, sections, lessons, and word cards -->
            </div>

            <!-- Compact Table View Container -->
            <div id="table-view-wrapper">
                <table class="dict-table">
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>ID</th>
                            <th>Arabic</th>
                            <th>Translit / Root</th>
                            <th>Category & POS</th>
                            <th>Occurrences</th>
                            <th>English Meaning</th>
                            <th>বাংলা অর্থ</th>
                            <th>اردو معنی</th>
                            <th>Example Verse & Ref</th>
                        </tr>
                    </thead>
                    <tbody id="table-body">
                        <!-- Dynamically filled table rows -->
                    </tbody>
                </table>
            </div>
        </main>
    </div>

    <!-- Embedded Master Curriculum JSON -->
    <script>
        const CURRICULUM_DATA = {curriculum_json};

        // Efficient flat word derivation on client
        const FLAT_WORDS = CURRICULUM_DATA.flatMap(ch => 
            ch.sections.flatMap(sec => 
                sec.lessons.flatMap(les => 
                    les.words.map(w => ({{
                        ...w,
                        chapterId: ch.id,
                        sectionId: sec.id,
                        lessonId: les.id,
                        chapterTitle: ch.title,
                        sectionTitle: sec.title,
                        lessonTitle: les.title
                    }}))
                )
            )
        );

        let currentCategory = 'ALL';
        let currentChapter = 'ALL';
        let currentSearch = '';
        let visibleLangs = {{ en: true, bn: true, ur: true, in: true, tr: true, fr: true }};
        let currentView = 'tree';

        function escapeHtml(str) {{
            if (!str) return '';
            return str.replace(/&/g, '&amp;')
                      .replace(/</g, '&lt;')
                      .replace(/>/g, '&gt;')
                      .replace(/"/g, '&quot;')
                      .replace(/'/g, '&#039;');
        }}

        function highlightTranslationClient(trans, hlWord) {{
            if (!trans) return '';
            if (!hlWord || hlWord.trim().length < 2) return escapeHtml(trans);
            const hw = hlWord.trim();
            const idx = trans.toLowerCase().indexOf(hw.toLowerCase());
            if (idx >= 0) {{
                const before = escapeHtml(trans.substring(0, idx));
                const actual = escapeHtml(trans.substring(idx, idx + hw.length));
                const after = escapeHtml(trans.substring(idx + hw.length));
                return `${{before}}<mark class="tr-hl">${{actual}}</mark>${{after}}`;
            }}
            // Sub-phrase word match
            const subWords = hw.split(/\\s+/).filter(w => w.length >= 2);
            for (const sw of subWords) {{
                const sIdx = trans.toLowerCase().indexOf(sw.toLowerCase());
                if (sIdx >= 0) {{
                    const before = escapeHtml(trans.substring(0, sIdx));
                    const actual = escapeHtml(trans.substring(sIdx, sIdx + sw.length));
                    const after = escapeHtml(trans.substring(sIdx + sw.length));
                    return `${{before}}<mark class="tr-hl">${{actual}}</mark>${{after}}`;
                }}
            }}
            return escapeHtml(trans);
        }}

        // Render Sidebar Directory
        function renderSidebar() {{
            const listEl = document.getElementById('sidebar-chapters-list');
            listEl.innerHTML = CURRICULUM_DATA.map(ch => `
                <div class="nav-chapter">
                    <button class="nav-chapter-btn" onclick="toggleSidebarAccordion('nav-sec-ch-${{ch.id}}')">
                        <span>${{ch.sortOrder}}. ${{ch.title.en}}</span>
                        <span class="count-pill">${{ch.wordCount}}</span>
                    </button>
                    <div id="nav-sec-ch-${{ch.id}}" class="nav-sections-list">
                        ${{ch.sections.map(sec => `
                            <a href="#sec-${{sec.id}}" class="nav-sec-link">
                                Sec ${{sec.sortOrder}}: ${{sec.title.en}} (${{sec.wordCount}})
                            </a>
                        `).join('')}}
                    </div>
                </div>
            `).join('');
        }}

        function toggleSidebarAccordion(id) {{
            const el = document.getElementById(id);
            if (el) {{
                el.style.display = el.style.display === 'none' ? 'block' : 'none';
            }}
        }}

        // Helper to strip tashkeel in client JS
        function stripTashkeelJs(text) {{
            if (!text) return '';
            return text.replace(/[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\uFEFF]/g, '')
                       .replace(/[إأآٱ]/g, 'ا')
                       .replace(/ة/g, 'ه')
                       .replace(/ى/g, 'ي')
                       .trim();
        }}

        // Render Tree View
        function renderTreeView() {{
            const container = document.getElementById('curriculum-tree-view');
            const q = currentSearch.toLowerCase().trim();
            const qNorm = stripTashkeelJs(q);
            let matchCount = 0;

            let html = '';

            CURRICULUM_DATA.forEach(ch => {{
                if (currentChapter !== 'ALL' && ch.id !== currentChapter) return;

                let chWordMatches = 0;
                let sectionsHtml = '';

                ch.sections.forEach(sec => {{
                    let secWordMatches = 0;
                    let lessonsHtml = '';

                    sec.lessons.forEach(les => {{
                        const filteredWords = les.words.filter(w => {{
                            // Category Filter
                            if (currentCategory === 'PARTICLE' && w.category !== 'PARTICLE') return false;
                            if (currentCategory === 'VERB' && w.category !== 'VERB') return false;
                            if (currentCategory === 'NOUN' && w.category !== 'NOUN') return false;
                            if (currentCategory === 'POLYSEMY' && (!w.polysemyEntries || w.polysemyEntries.length <= 1)) return false;

                            // Search Filter
                            if (q) {{
                                const matchAr = w.arabicWord && (w.arabicWord.includes(q) || w.normArabic.includes(qNorm));
                                const matchTranslit = w.transliteration && w.transliteration.toLowerCase().includes(q);
                                const matchRoot = w.root && w.root.toLowerCase().includes(q);
                                const matchId = w.wordId && w.wordId.toLowerCase().includes(q);
                                const matchRef = w.exampleVerseReference && w.exampleVerseReference.toLowerCase().includes(q);
                                const matchEn = w.meaning.en && w.meaning.en.toLowerCase().includes(q);
                                const matchBn = w.meaning.bn && w.meaning.bn.toLowerCase().includes(q);
                                const matchUr = w.meaning.ur && w.meaning.ur.toLowerCase().includes(q);
                                const matchPos = w.partOfSpeech && w.partOfSpeech.toLowerCase().includes(q);

                                return matchAr || matchTranslit || matchRoot || matchId || matchRef || matchEn || matchBn || matchUr || matchPos;
                            }}
                            return true;
                        }});

                        if (filteredWords.length === 0 && les.words.length > 0) return;

                        secWordMatches += filteredWords.length;
                        matchCount += filteredWords.length;

                        lessonsHtml += `
                            <div class="lesson-container">
                                <div class="lesson-header-pill">
                                    <span>📝 ${{les.title.en}}</span>
                                    <span style="opacity:0.7;">·</span>
                                    <span class="font-bn">${{les.title.bn}}</span>
                                    <span class="count-pill">${{filteredWords.length}} words</span>
                                </div>
                                <div class="words-grid">
                                    ${{filteredWords.map(w => renderWordCard(w)).join('')}}
                                </div>
                            </div>
                        `;
                    }});

                    if (secWordMatches === 0 && sec.wordCount > 0 && (q || currentCategory !== 'ALL')) return;
                    chWordMatches += secWordMatches;

                    sectionsHtml += `
                        <div class="section-box" id="sec-${{sec.id}}">
                            <div class="section-header-bar" onclick="toggleAccordion('sec-body-${{sec.id}}')">
                                <div>
                                    <h4 style="font-size:16px; font-weight:700;">${{sec.title.en}}</h4>
                                    <p style="font-size:13px; color:var(--text-muted);" class="font-bn">${{sec.title.bn}} · <span class="font-ur">${{sec.title.ur}}</span></p>
                                </div>
                                <div style="display:flex; gap:10px; align-items:center;">
                                    <span class="badge-occ">${{sec.occurrenceCount.toLocaleString()}} Occurrences (${{sec.occurrencePercent}}%)</span>
                                    <span class="count-pill">${{sec.wordCount}} Words</span>
                                    <span style="font-size:18px;">▼</span>
                                </div>
                            </div>
                            <div id="sec-body-${{sec.id}}">
                                ${{lessonsHtml}}
                            </div>
                        </div>
                    `;
                }});

                if (chWordMatches === 0 && (q || currentCategory !== 'ALL')) return;

                html += `
                    <section class="chapter-section" id="ch-${{ch.id}}">
                        <div class="chapter-header-banner">
                            <div>
                                <div style="display:flex; align-items:center; gap:10px; margin-bottom:6px;">
                                    <span style="background:rgba(255,255,255,0.2); padding:3px 10px; border-radius:12px; font-size:12px; font-weight:800;">CHAPTER ${{ch.sortOrder}}</span>
                                    <span style="background:rgba(52,211,153,0.3); color:#a7f3d0; padding:3px 10px; border-radius:12px; font-size:12px; font-weight:800;">${{ch.sections[0]?.lessons[0]?.category || 'VOCABULARY'}}</span>
                                </div>
                                <h2 style="font-size:24px; font-weight:800; line-height:1.2;">${{ch.title.en}}</h2>
                                <p style="font-size:16px; opacity:0.9; margin-top:4px;" class="font-bn">${{ch.title.bn}} · <span class="font-ur">${{ch.title.ur}}</span></p>
                            </div>
                            <div style="text-align:right; display:flex; flex-direction:column; gap:4px;">
                                <div style="font-size:22px; font-weight:800;">${{ch.wordCount}} Lemmas</div>
                                <div style="font-size:13px; opacity:0.85;">${{ch.occurrenceCount.toLocaleString()}} Quranic Tokens (${{ch.occurrencePercent}}%)</div>
                            </div>
                        </div>
                        ${{sectionsHtml}}
                    </section>
                `;
            }});

            container.innerHTML = html || `<div style="text-align:center; padding:60px 20px; color:var(--text-muted);"><h3>No matching Quranic words found</h3><p>Try searching with another keyword, root, or reset the filters.</p></div>`;
            document.getElementById('stats-badge').innerText = `Showing ${{matchCount.toLocaleString()}} / ${{FLAT_WORDS.length.toLocaleString()}} words`;
        }}

        // Render Word Card HTML
        function renderWordCard(w) {{
            const isPoly = w.polysemyEntries && w.polysemyEntries.length > 1;

            return `
                <div class="word-card" id="card-${{w.wordId}}">
                    <!-- Card Topbar -->
                    <div class="word-card-topbar">
                        <div style="display:flex; gap:6px; align-items:center;">
                            <span class="badge-id">${{w.wordId}}</span>
                            <span class="badge-pos">${{w.partOfSpeech || w.category}}</span>
                            ${{isPoly ? `<span style="background:var(--purple-light); color:var(--purple); font-size:11px; font-weight:800; padding:2px 8px; border-radius:10px; border:1px solid rgba(124,58,237,0.3);">✨ ${{w.polysemyEntries.length}} Senses</span>` : ''}}
                        </div>
                        <span class="badge-occ">${{w.quranOccurrenceCount.toLocaleString()}} occ</span>
                    </div>

                    <!-- Arabic Word & Transliteration Display -->
                    <div class="word-main-display">
                        <div class="arabic-lemma font-arabic">${{w.arabicWord}}</div>
                        <div class="word-meta">
                            <span class="translit-text">${{w.transliteration || ''}}</span>
                            ${{w.root ? `<span class="root-text">Root: ${{w.root}}</span>` : ''}}
                        </div>
                    </div>

                    <!-- Core Meanings in 6 Languages -->
                    <div class="meanings-grid">
                        ${{visibleLangs.en ? `
                            <div class="meaning-item">
                                <span class="lang-flag">🇬🇧</span>
                                <span class="meaning-val">${{w.meaning.en || '—'}}</span>
                            </div>
                        ` : ''}}
                        ${{visibleLangs.bn ? `
                            <div class="meaning-item">
                                <span class="lang-flag">🇧🇩</span>
                                <span class="meaning-val font-bn">${{w.meaning.bn || '—'}}</span>
                            </div>
                        ` : ''}}
                        ${{visibleLangs.ur ? `
                            <div class="meaning-item">
                                <span class="lang-flag">🇵🇰</span>
                                <span class="meaning-val font-ur">${{w.meaning.ur || '—'}}</span>
                            </div>
                        ` : ''}}
                        ${{visibleLangs.in ? `
                            <div class="meaning-item">
                                <span class="lang-flag">🇮🇩</span>
                                <span class="meaning-val">${{w.meaning.in || '—'}}</span>
                            </div>
                        ` : ''}}
                        ${{visibleLangs.tr ? `
                            <div class="meaning-item">
                                <span class="lang-flag">🇹🇷</span>
                                <span class="meaning-val">${{w.meaning.tr || '—'}}</span>
                            </div>
                        ` : ''}}
                        ${{visibleLangs.fr ? `
                            <div class="meaning-item">
                                <span class="lang-flag">🇫🇷</span>
                                <span class="meaning-val">${{w.meaning.fr || '—'}}</span>
                            </div>
                        ` : ''}}
                    </div>

                    <!-- Example Verse Box -->
                    <div class="verse-box" id="verse-box-${{w.wordId}}">
                        <div class="verse-meta-row">
                            <span>📖 Authentic Qur'anic Example</span>
                            <span id="ref-${{w.wordId}}" style="background:var(--emerald-light); padding:2px 8px; border-radius:6px; border:1px solid var(--emerald-border);">${{w.exampleVerseReference}}</span>
                        </div>
                        <div class="verse-arabic-text font-arabic" id="verse-ar-${{w.wordId}}">
                            ${{w.exampleVerseArabicHl}}
                        </div>
                        <div class="verse-translations-list" id="verse-trans-${{w.wordId}}">
                            ${{renderVerseTranslationsHtml(w.exampleVerseTranslation, w.meaningHighlight)}}
                        </div>
                    </div>

                    <!-- Polysemy / Wujūh al-Qur'an Interactive Tabs (if multi-meaning) -->
                    ${{isPoly ? `
                        <div class="polysemy-card-block">
                            <div style="font-size:12px; font-weight:800; color:var(--purple); display:flex; justify-content:space-between; align-items:center;">
                                <span>📚 Polysemous Contextual Senses (Wujūh al-Qur'an)</span>
                                <span>Tap sense to switch context</span>
                            </div>
                            <div class="poly-tabs-header">
                                ${{w.polysemyEntries.map((se, idx) => `
                                    <button class="poly-tab-btn ${{idx === 0 ? 'active' : ''}}" onclick="switchPolySense('${{w.wordId}}', ${{idx}}, this)">
                                        [${{se.meaningIndex}}] ${{se.contextualMeaning.en || 'Sense ' + se.meaningIndex}}
                                    </button>
                                `).join('')}}
                            </div>
                        </div>
                    ` : ''}}
                </div>
            `;
        }}

        function renderVerseTranslationsHtml(transDict, hlDict) {{
            const flags = {{ en: '🇬🇧', bn: '🇧🇩', ur: '🇵🇰', in: '🇮🇩', tr: '🇹🇷', fr: '🇫🇷' }};
            let out = '';
            for (const [lang, text] of Object.entries(transDict || {{}})) {{
                if (!visibleLangs[lang]) continue;
                const fontClass = lang === 'bn' ? 'font-bn' : (lang === 'ur' ? 'font-ur' : '');
                const hlWord = (hlDict && hlDict[lang]) ? hlDict[lang] : '';
                const renderedText = highlightTranslationClient(text, hlWord);
                out += `
                    <div class="v-trans-item ${{fontClass}}">
                        <span class="v-trans-tag">${{flags[lang] || lang.toUpperCase()}}:</span>
                        <span>${{renderedText || '—'}}</span>
                    </div>
                `;
            }}
            return out;
        }}

        // Switch Polysemy Sense in Card
        function switchPolySense(wordId, senseIdx, btnEl) {{
            const w = FLAT_WORDS.find(item => item.wordId === wordId);
            if (!w || !w.polysemyEntries || !w.polysemyEntries[senseIdx]) return;

            const se = w.polysemyEntries[senseIdx];
            
            // Update Active button state
            const parent = btnEl.parentElement;
            parent.querySelectorAll('.poly-tab-btn').forEach(b => b.classList.remove('active'));
            btnEl.classList.add('active');

            // Update Verse Reference, Arabic Verse, and Translations
            document.getElementById(`ref-${{wordId}}`).innerText = se.verseReference;
            document.getElementById(`verse-ar-${{wordId}}`).innerHTML = se.verseArabicHl;
            document.getElementById(`verse-trans-${{wordId}}`).innerHTML = renderVerseTranslationsHtml(se.verseTranslation, se.translationHighlight);
        }}

        // Render Compact Table View
        function renderTableView() {{
            const tbody = document.getElementById('table-body');
            const q = currentSearch.toLowerCase().trim();
            const qNorm = stripTashkeelJs(q);
            let matchCount = 0;

            const filtered = FLAT_WORDS.filter(w => {{
                if (currentChapter !== 'ALL' && w.chapterId !== currentChapter) return false;
                if (currentCategory === 'PARTICLE' && w.category !== 'PARTICLE') return false;
                if (currentCategory === 'VERB' && w.category !== 'VERB') return false;
                if (currentCategory === 'NOUN' && w.category !== 'NOUN') return false;
                if (currentCategory === 'POLYSEMY' && (!w.polysemyEntries || w.polysemyEntries.length <= 1)) return false;

                if (q) {{
                    const matchAr = w.arabicWord && (w.arabicWord.includes(q) || w.normArabic.includes(qNorm));
                    const matchTranslit = w.transliteration && w.transliteration.toLowerCase().includes(q);
                    const matchRoot = w.root && w.root.toLowerCase().includes(q);
                    const matchId = w.wordId && w.wordId.toLowerCase().includes(q);
                    const matchRef = w.exampleVerseReference && w.exampleVerseReference.toLowerCase().includes(q);
                    const matchEn = w.meaning.en && w.meaning.en.toLowerCase().includes(q);
                    const matchBn = w.meaning.bn && w.meaning.bn.toLowerCase().includes(q);
                    const matchUr = w.meaning.ur && w.meaning.ur.toLowerCase().includes(q);
                    return matchAr || matchTranslit || matchRoot || matchId || matchRef || matchEn || matchBn || matchUr;
                }}
                return true;
            }});

            matchCount = filtered.length;

            tbody.innerHTML = filtered.map((w, idx) => `
                <tr>
                    <td style="font-weight:700; color:var(--text-muted);">${{idx + 1}}</td>
                    <td><span class="badge-id">${{w.wordId}}</span></td>
                    <td class="font-arabic" style="font-size:22px; font-weight:700; color:#064e3b;">${{w.arabicWord}}</td>
                    <td>
                        <div style="font-weight:600; font-style:italic;">${{w.transliteration || '—'}}</div>
                        ${{w.root ? `<div style="color:var(--gold); font-size:11px; font-weight:700;">${{w.root}}</div>` : ''}}
                    </td>
                    <td>
                        <span class="badge-pos">${{w.partOfSpeech || w.category}}</span>
                    </td>
                    <td>
                        <span class="badge-occ">${{w.quranOccurrenceCount.toLocaleString()}}</span>
                    </td>
                    <td style="font-weight:600;">${{w.meaning.en || '—'}}</td>
                    <td class="font-bn" style="font-weight:600;">${{w.meaning.bn || '—'}}</td>
                    <td class="font-ur" style="font-weight:600;">${{w.meaning.ur || '—'}}</td>
                    <td style="min-width:260px;">
                        <div style="font-weight:700; color:var(--emerald); font-size:11px; margin-bottom:2px;">${{w.exampleVerseReference}}</div>
                        <div class="font-arabic" style="font-size:16px; margin-bottom:4px;">${{w.exampleVerseArabicHl}}</div>
                        <div style="font-size:12px; color:var(--text-muted);">${{w.exampleVerseTranslation.en || ''}}</div>
                    </td>
                </tr>
            `).join('');

            document.getElementById('stats-badge').innerText = `Showing ${{matchCount.toLocaleString()}} / ${{FLAT_WORDS.length.toLocaleString()}} words`;
        }}

        // Accordion Handlers
        function toggleAccordion(id) {{
            const el = document.getElementById(id);
            if (el) {{
                el.style.display = el.style.display === 'none' ? 'block' : 'none';
            }}
        }}

        function expandAll() {{
            document.querySelectorAll('[id^="sec-body-"]').forEach(el => el.style.display = 'block');
        }}

        function collapseAll() {{
            document.querySelectorAll('[id^="sec-body-"]').forEach(el => el.style.display = 'none');
        }}

        // Filter Handlers
        function filterCategory(cat, btnEl) {{
            currentCategory = cat;
            document.querySelectorAll('.filter-chip').forEach(b => b.classList.remove('active'));
            btnEl.classList.add('active');
            refreshView();
        }}

        function onChapterSelect(val) {{
            currentChapter = val;
            refreshView();
        }}

        function toggleLang(lang, isChecked) {{
            visibleLangs[lang] = isChecked;
            refreshView();
        }}

        function switchView(view) {{
            currentView = view;
            const treeWrapper = document.getElementById('curriculum-tree-view');
            const tableWrapper = document.getElementById('table-view-wrapper');
            const treeBtn = document.getElementById('view-tree-btn');
            const tableBtn = document.getElementById('view-table-btn');

            if (view === 'tree') {{
                treeWrapper.style.display = 'block';
                tableWrapper.style.display = 'none';
                treeBtn.className = 'btn btn-primary';
                tableBtn.className = 'btn';
            }} else {{
                treeWrapper.style.display = 'none';
                tableWrapper.style.display = 'block';
                treeBtn.className = 'btn';
                tableBtn.className = 'btn btn-primary';
            }}
            refreshView();
        }}

        function toggleDarkMode() {{
            document.documentElement.classList.toggle('dark');
        }}

        function refreshView() {{
            if (currentView === 'tree') {{
                renderTreeView();
            }} else {{
                renderTableView();
            }}
        }}

        // Live Search Debounce
        let searchTimer = null;
        document.getElementById('search-input').addEventListener('input', (e) => {{
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {{
                currentSearch = e.target.value;
                refreshView();
            }}, 200);
        }});

        // Initialization
        window.addEventListener('DOMContentLoaded', () => {{
            renderSidebar();
            renderTreeView();
        }});
    </script>
</body>
</html>
"""

    print(f"Writing complete HTML dictionary to {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write(html_template)

    file_size_mb = os.path.getsize(OUTPUT_FILE) / (1024 * 1024)
    print(f"SUCCESS: Generated {OUTPUT_FILE} ({file_size_mb:.2f} MB)!")

if __name__ == '__main__':
    main()
