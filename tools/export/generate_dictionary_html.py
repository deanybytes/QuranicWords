#!/usr/bin/env python3
"""
QuranicWords Master Curriculum Interactive Web Application & Dictionary Generator
==================================================================================
Generates an ultra-fast, SEO-optimized, stunning, glassmorphic standalone web app:
`QuranicWords-v1.0.0.html` and `QuranicWords_Dictionary.html`

Features:
- Complete 10 Chapters, 100 Sections, 1,214 Lessons, 4,709 Qur'anic Lemmas.
- 11 Master Languages: English, Bengali (বাংলা), Urdu (اردو), Hindi (हिन्दी),
  Indonesian (Bahasa), Malay (Melayu), Turkish (Türkçe), Persian (فارسی),
  Hausa, Swahili (Kiswahili), French (Français).
- 3 View Modes: 📚 Curriculum Tree View, 📋 Interactive Data Table, 📇 Flashcard Master Mode.
- Infinite / Progressive Batch Rendering (Zero Lag, 60 FPS, Instant Search).
- 🔊 Audio Pronunciation for all 4,709 Arabic words.
- 📋 Instant Quick Copy with animated glass toast notifications.
- ⭐ Bookmarking / Favorites with localStorage persistence.
- 🎲 Random Word / Ayah Discovery Modal.
- 🔀 Interactive Polysemy / Wujūh al-Qur'an Sense Explorer with real-time Ayah context switching.
- 🌓 Sleek Dark / Light Mode with frosted glassmorphism.
- 🚀 Complete SEO metadata: OpenGraph, Twitter Cards, Schema.org JSON-LD.
- 📱 Mobile-First Responsive Drawer and Bottom Action Bar.
"""

import os
import json
import re
import html
import io
import base64
from PIL import Image

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
CONTENT_DIR = os.path.join(BASE_DIR, 'app', 'src', 'main', 'assets', 'content')
OUTPUT_FILE_V1 = os.path.join(BASE_DIR, 'QuranicWords-v1.0.0.html')

# Generate optimized deanybytes logo base64 and favicon
LOGO_PATH = os.path.join(BASE_DIR, 'assets', 'image', 'LOGO.png')
if os.path.exists(LOGO_PATH):
    img = Image.open(LOGO_PATH)
    img = img.resize((128, 128), Image.Resampling.LANCZOS)
    buf = io.BytesIO()
    img.save(buf, format='PNG', optimize=True)
    LOGO_B64 = base64.b64encode(buf.getvalue()).decode('utf-8')
    LOGO_DATA_URI = f"data:image/png;base64,{LOGO_B64}"
    img.save(os.path.join(BASE_DIR, 'favicon.png'), format='PNG', optimize=True)
else:
    LOGO_DATA_URI = ""

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
    flat_words = []
    total_words_count = 0
    poly_words_count = 0

    SUPPORTED_LANGS = ['en', 'bn', 'ur', 'hi', 'in', 'ms', 'tr', 'fa', 'ha', 'sw', 'fr']

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
                            'chapterId': cid,
                            'sectionId': sid,
                            'lessonId': lid,
                            'quranOccurrenceCount': c.get('quranOccurrenceCount', 1),
                            'meaning': {l: c.get('meaning', {}).get(l, '') for l in SUPPORTED_LANGS},
                            'exampleVerseReference': c.get('exampleVerseReference', ''),
                            'exampleVerseArabic': c.get('exampleVerseArabic', ''),
                            'arabicWordStart': c.get('arabicWordStart'),
                            'arabicWordEnd': c.get('arabicWordEnd'),
                            'exampleVerseArabicHl': highlight_arabic(c.get('exampleVerseArabic', ''), c.get('arabicWordStart'), c.get('arabicWordEnd'), c.get('arabicWord', '')),
                            'exampleVerseTranslation': {l: c.get('exampleVerseTranslation', {}).get(l, '') for l in SUPPORTED_LANGS},
                            'meaningHighlight': {l: c.get('meaningHighlight', {}).get(l, '') for l in SUPPORTED_LANGS},
                            'exampleVerseTranslationHl': {
                                lang: highlight_translation(c.get('exampleVerseTranslation', {}).get(lang, ''), c.get('meaningHighlight', {}).get(lang, ''))
                                for lang in ['en', 'bn', 'ur', 'hi', 'in', 'ms', 'tr', 'fa', 'ha', 'sw', 'fr']
                            },
                            'polysemyEntries': []
                        }
                        
                        # Process polysemy entries
                        for se in c.get('polysemyEntries', []):
                            se_item = {
                                'meaningIndex': se['meaningIndex'],
                                'contextualMeaning': {l: se.get('contextualMeaning', {}).get(l, '') for l in SUPPORTED_LANGS},
                                'verseReference': se.get('verseReference', ''),
                                'verseArabic': se.get('verseArabic', ''),
                                'arabicWordStart': se.get('arabicWordStart'),
                                'arabicWordEnd': se.get('arabicWordEnd'),
                                'verseArabicHl': highlight_arabic(se.get('verseArabic', ''), se.get('arabicWordStart'), se.get('arabicWordEnd'), c.get('arabicWord', '')),
                                'verseTranslation': {l: se.get('verseTranslation', {}).get(l, '') for l in SUPPORTED_LANGS},
                                'translationHighlight': {l: se.get('translationHighlight', {}).get(l, '') for l in SUPPORTED_LANGS},
                                'verseTranslationHl': {
                                    lang: highlight_translation(se.get('verseTranslation', {}).get(lang, ''), se.get('translationHighlight', {}).get(lang, ''))
                                    for lang in ['en', 'bn', 'ur', 'hi', 'in', 'ms', 'tr', 'fa', 'ha', 'sw', 'fr']
                                }
                            }
                            w_item['polysemyEntries'].append(se_item)
                            
                        if len(w_item['polysemyEntries']) > 1:
                            poly_words_count += 1

                        words.append(w_item)
                        flat_words.append(w_item)
                        
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
    <!-- Meta & Core Standards -->
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    
    <!-- Primary SEO Meta Tags -->
    <title>QuranicWords — Interactive Master Curriculum Dictionary & Quran Vocabulary Explorer</title>
    <meta name="title" content="QuranicWords — Interactive Master Curriculum Dictionary & Quran Vocabulary Explorer">
    <meta name="description" content="Explore 4,709 verified Qur'anic Arabic words organized by frequency. Features authentic verse contexts, multi-lingual translations in 11 languages (English, Bengali, Urdu, Hindi, Bahasa, Turkish, French, Persian, Hausa, Swahili), root analysis, audio pronunciation, and polysemy exploration.">
    <meta name="keywords" content="Quran vocabulary, Quranic Arabic dictionary, Learn Quran Arabic, Quran words frequency, Quran lemmas, Uthmani Quran, Arabic grammar, Wujuh al-Quran, polysemy, Quranic root words, Quran dictionary bangla, Quran dictionary urdu, Quranic words english">
    <meta name="author" content="DEANY TALKS (deanybytes)">
    <meta name="robots" content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1">
    <meta name="theme-color" content="#064e3b" media="(prefers-color-scheme: light)">
    <meta name="theme-color" content="#070a12" media="(prefers-color-scheme: dark)">

    <!-- Web App & Favicon Icons (deanybytes logo) -->
    <link rel="icon" type="image/png" href="{LOGO_DATA_URI}">
    <link rel="apple-touch-icon" href="{LOGO_DATA_URI}">
    <link rel="shortcut icon" href="{LOGO_DATA_URI}">

    <!-- Open Graph / Facebook / WhatsApp -->
    <meta property="og:type" content="website">
    <meta property="og:url" content="https://quranicwords.vercel.app/">
    <meta property="og:site_name" content="QuranicWords">
    <meta property="og:title" content="QuranicWords — 4,709 Quranic Vocabulary Master Dictionary">
    <meta property="og:description" content="Master 4,700+ Quranic Arabic vocabulary words organized by frequency of occurrence with authentic Ayah contexts, root analysis, and 11-language translations.">
    <meta property="og:image" content="https://raw.githubusercontent.com/deanybytes/QuranicWords/main/docs/media/og_preview.png">
    <meta property="og:locale" content="en_US">
    <meta property="og:locale:alternate" content="bn_BD">
    <meta property="og:locale:alternate" content="ur_PK">

    <!-- Twitter Card -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:url" content="https://quranicwords.vercel.app/">
    <meta name="twitter:title" content="QuranicWords — 4,709 Quranic Vocabulary Master Dictionary">
    <meta name="twitter:description" content="Master 4,700+ Quranic Arabic vocabulary words organized by frequency with authentic Ayah contexts in 11 languages.">
    <meta name="twitter:image" content="https://raw.githubusercontent.com/deanybytes/QuranicWords/main/docs/media/og_preview.png">

    <!-- Preconnect & Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Amiri:ital,wght@0,400;0,700;1,400&family=Hind+Siliguri:wght@400;500;600;700&family=Inter:wght@300;400;500;600;700;800;900&family=Noto+Naskh+Arabic:wght@400;600;700&family=Noto+Nastaliq+Urdu:wght@400;600;700&family=Scheherazade+New:wght@400;600;700&display=swap" rel="stylesheet">

    <!-- JSON-LD Schema.org Structured Data -->
    <script type="application/ld+json">
    {{
      "@context": "https://schema.org",
      "@graph": [
        {{
          "@type": "WebApplication",
          "name": "QuranicWords Master Curriculum Dictionary",
          "url": "https://quranicwords.vercel.app/",
          "description": "Interactive, offline-ready dictionary teaching 4,709 Qur'anic Arabic words organized by frequency with verified contextual verses and 11-language translations.",
          "applicationCategory": "EducationalApplication",
          "operatingSystem": "All",
          "offers": {{
            "@type": "Offer",
            "price": "0",
            "priceCurrency": "USD"
          }},
          "author": {{
            "@type": "Organization",
            "name": "DEANY TALKS",
            "url": "https://github.com/deanybytes"
          }}
        }},
        {{
          "@type": "DefinedTermSet",
          "name": "Quranic Vocabulary Curriculum",
          "description": "4,709 lemmas covering over 85% of the total Qur'anic vocabulary tokens.",
          "hasDefinedTerm": [
            {{
              "@type": "DefinedTerm",
              "termCode": "w_0001",
              "name": "مِنْ",
              "description": "Preposition: from / of"
            }},
            {{
              "@type": "DefinedTerm",
              "termCode": "w_0002",
              "name": "فِي",
              "description": "Preposition: in / concerning"
            }},
            {{
              "@type": "DefinedTerm",
              "termCode": "w_0003",
              "name": "إِنَّ",
              "description": "Accusative Particle: indeed / surely"
            }}
          ]
        }}
      ]
    }}
    </script>
    
    <style>
        :root {{
            --bg-primary: #f8fafc;
            --bg-card: #ffffff;
            --bg-card-header: #f1f5f9;
            --bg-sidebar: #0f172a;
            --bg-glass: rgba(255, 255, 255, 0.85);
            --bg-glass-card: rgba(255, 255, 255, 0.92);
            --text-main: #0f172a;
            --text-muted: #64748b;
            --text-light: #94a3b8;
            --border: #e2e8f0;
            --border-glow: rgba(5, 150, 105, 0.2);
            --emerald: #059669;
            --emerald-dark: #064e3b;
            --emerald-light: #ecfdf5;
            --emerald-border: #a7f3d0;
            --gold: #d97706;
            --gold-light: #fef3c7;
            --gold-border: #fde68a;
            --blue: #2563eb;
            --blue-light: #eff6ff;
            --purple: #7c3aed;
            --purple-light: #f5f3ff;
            --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
            --shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
            --shadow-lg: 0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
            --shadow-glow: 0 0 20px rgba(5, 150, 105, 0.15);
            --radius: 12px;
            --radius-lg: 16px;
            --radius-xl: 20px;
        }}

        .dark {{
            --bg-primary: #070a12;
            --bg-card: #0f172a;
            --bg-card-header: #151e33;
            --bg-sidebar: #05080f;
            --bg-glass: rgba(15, 23, 42, 0.85);
            --bg-glass-card: rgba(15, 23, 42, 0.92);
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --text-light: #64748b;
            --border: #1e293b;
            --border-glow: rgba(52, 211, 153, 0.25);
            --emerald: #10b981;
            --emerald-dark: #064e3b;
            --emerald-light: rgba(16, 185, 129, 0.12);
            --emerald-border: rgba(16, 185, 129, 0.3);
            --gold: #f59e0b;
            --gold-light: rgba(245, 158, 11, 0.12);
            --gold-border: rgba(245, 158, 11, 0.3);
            --blue-light: rgba(59, 130, 246, 0.12);
            --purple-light: rgba(139, 92, 246, 0.12);
            --shadow-glow: 0 0 25px rgba(16, 185, 129, 0.2);
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
            overflow-x: hidden;
            transition: background-color 0.3s ease, color 0.3s ease;
        }}

        /* Typography & Calligraphy */
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

        /* Islamic Calligraphy Highlighting */
        mark.ar-hl {{
            background: linear-gradient(135deg, #f59e0b, #d97706);
            color: #ffffff !important;
            font-weight: 700;
            padding: 2px 10px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(217, 119, 6, 0.35);
            display: inline-block;
            margin: 0 2px;
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
            border-right: 1px solid rgba(255, 255, 255, 0.08);
            transition: transform 0.3s ease;
        }}

        .sidebar-header {{
            padding: 20px;
            background: linear-gradient(135deg, #064e3b 0%, #047857 50%, #0f172a 100%);
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }}

        .sidebar-nav {{
            padding: 12px;
            flex: 1;
        }}

        .nav-chapter {{
            margin-bottom: 8px;
            border-radius: 10px;
            overflow: hidden;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid rgba(255, 255, 255, 0.05);
            transition: all 0.2s ease;
        }}
        .nav-chapter:hover {{
            background: rgba(255, 255, 255, 0.06);
            border-color: rgba(52, 211, 153, 0.2);
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
            color: #34d399;
        }}

        .nav-sections-list {{
            padding: 4px 8px 8px 16px;
            font-size: 12px;
        }}

        .nav-sec-link {{
            display: block;
            padding: 6px 10px;
            color: #94a3b8;
            text-decoration: none;
            border-radius: 6px;
            transition: all 0.15s;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }}
        .nav-sec-link:hover {{
            background: rgba(52, 211, 153, 0.12);
            color: #34d399;
            transform: translateX(3px);
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

        /* Sticky Glassmorphic Topbar */
        #topbar {{
            position: sticky;
            top: 0;
            background: var(--bg-glass);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border-bottom: 1px solid var(--border);
            padding: 16px 24px;
            z-index: 30;
            box-shadow: var(--shadow);
            transition: background 0.3s ease, border-color 0.3s ease;
        }}

        .search-box {{
            position: relative;
            flex: 1;
        }}
        .search-box input {{
            width: 100%;
            padding: 12px 16px 12px 44px;
            border-radius: 12px;
            border: 2px solid var(--border);
            background-color: var(--bg-card);
            color: var(--text-main);
            font-size: 15px;
            font-weight: 500;
            outline: none;
            transition: all 0.2s ease;
        }}
        .search-box input:focus {{
            border-color: var(--emerald);
            box-shadow: 0 0 0 4px var(--border-glow);
        }}
        .search-icon {{
            position: absolute;
            left: 14px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
            font-size: 18px;
            pointer-events: none;
        }}
        .search-clear-btn {{
            position: absolute;
            right: 12px;
            top: 50%;
            transform: translateY(-50%);
            background: none;
            border: none;
            color: var(--text-light);
            cursor: pointer;
            font-size: 16px;
            display: none;
            padding: 4px;
            border-radius: 50%;
        }}
        .search-clear-btn:hover {{
            color: var(--text-main);
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
            transition: all 0.2s ease;
            user-select: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }}
        .filter-chip:hover {{
            border-color: var(--emerald);
            color: var(--emerald);
            transform: translateY(-1px);
        }}
        .filter-chip.active {{
            background: linear-gradient(135deg, var(--emerald) 0%, #047857 100%);
            color: #ffffff;
            border-color: var(--emerald);
            box-shadow: 0 2px 6px rgba(5, 150, 105, 0.3);
        }}

        /* Languages Visibility Toggle Row */
        .lang-toggles {{
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            align-items: center;
            font-size: 12px;
            font-weight: 600;
            color: var(--text-muted);
        }}
        .lang-toggle-btn {{
            padding: 4px 10px;
            border-radius: 14px;
            border: 1px solid var(--border);
            background: var(--bg-card);
            color: var(--text-muted);
            cursor: pointer;
            font-size: 12px;
            font-weight: 600;
            transition: all 0.15s ease;
            user-select: none;
        }}
        .lang-toggle-btn.active {{
            background: var(--emerald-light);
            color: var(--emerald);
            border-color: var(--emerald-border);
            font-weight: 700;
        }}

        /* Content Area & Hero Banner */
        #content-area {{
            padding: 24px;
            max-width: 1440px;
            margin: 0 auto;
            width: 100%;
        }}

        .hero-banner {{
            background: linear-gradient(135deg, #064e3b 0%, #047857 50%, #0f172a 100%);
            color: #ffffff;
            border-radius: var(--radius-xl);
            padding: 32px 28px;
            margin-bottom: 32px;
            box-shadow: var(--shadow-lg);
            position: relative;
            overflow: hidden;
        }}
        .hero-banner::after {{
            content: '';
            position: absolute;
            top: -50%;
            right: -20%;
            width: 500px;
            height: 500px;
            background: radial-gradient(circle, rgba(52, 211, 153, 0.15) 0%, transparent 70%);
            pointer-events: none;
        }}

        .stats-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
            gap: 12px;
            margin-top: 24px;
        }}
        .stat-card {{
            background: rgba(255, 255, 255, 0.08);
            backdrop-filter: blur(8px);
            border: 1px solid rgba(255, 255, 255, 0.12);
            border-radius: var(--radius);
            padding: 14px;
            text-align: center;
            transition: transform 0.2s ease;
        }}
        .stat-card:hover {{
            transform: translateY(-2px);
            background: rgba(255, 255, 255, 0.12);
        }}
        .stat-num {{
            font-size: 24px;
            font-weight: 900;
            color: #34d399;
            line-height: 1.1;
        }}
        .stat-label {{
            font-size: 11px;
            font-weight: 600;
            color: #cbd5e1;
            margin-top: 4px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }}

        /* Chapter Banner */
        .chapter-section {{
            margin-bottom: 40px;
            scroll-margin-top: 140px;
        }}
        .chapter-header-banner {{
            background: linear-gradient(135deg, #064e3b 0%, #047857 50%, #0f172a 100%);
            color: #ffffff;
            padding: 24px;
            border-radius: var(--radius-lg);
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            box-shadow: var(--shadow);
            border: 1px solid rgba(255, 255, 255, 0.1);
        }}

        /* Section Box */
        .section-box {{
            background-color: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: var(--radius-lg);
            margin-bottom: 20px;
            overflow: hidden;
            box-shadow: var(--shadow-sm);
            transition: border-color 0.2s ease;
        }}
        .section-box:hover {{
            border-color: var(--border-glow);
        }}

        .section-header-bar {{
            padding: 16px 20px;
            background: var(--bg-card-header);
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
            user-select: none;
            border-bottom: 1px solid var(--border);
            transition: background 0.15s ease;
        }}
        .section-header-bar:hover {{
            background: var(--emerald-light);
        }}

        /* Lesson Header Pill */
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
            padding: 6px 14px;
            background: var(--bg-primary);
            border: 1px solid var(--border);
            border-radius: 20px;
            font-size: 13px;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 16px;
        }}

        /* Word Cards Grid */
        .words-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
            gap: 16px;
        }}
        @media (max-width: 480px) {{
            .words-grid {{
                grid-template-columns: 1fr;
            }}
        }}

        .word-card {{
            background-color: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 18px;
            display: flex;
            flex-direction: column;
            gap: 14px;
            box-shadow: var(--shadow-sm);
            transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
            position: relative;
        }}
        .word-card:hover {{
            transform: translateY(-2px);
            box-shadow: var(--shadow-lg);
            border-color: var(--emerald);
        }}

        .word-card-topbar {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 12px;
        }}

        .badge-id {{
            background-color: var(--bg-primary);
            color: var(--text-muted);
            padding: 3px 8px;
            border-radius: 6px;
            font-weight: 700;
            border: 1px solid var(--border);
            font-size: 11px;
        }}

        .badge-pos {{
            background-color: var(--emerald-light);
            color: var(--emerald);
            padding: 3px 8px;
            border-radius: 6px;
            font-weight: 700;
            border: 1px solid var(--emerald-border);
            font-size: 11px;
        }}

        .badge-occ {{
            background-color: var(--gold-light);
            color: var(--gold);
            padding: 3px 8px;
            border-radius: 6px;
            font-weight: 700;
            border: 1px solid var(--gold-border);
            font-size: 11px;
        }}

        /* Arabic Lemma & Controls */
        .word-main-display {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px 14px;
            background: linear-gradient(135deg, var(--bg-card-header) 0%, var(--bg-card) 100%);
            border-radius: 10px;
            border: 1px solid var(--border);
        }}

        .arabic-lemma {{
            font-size: 28px;
            font-weight: 700;
            color: var(--emerald-dark);
            line-height: 1.3;
        }}
        .dark .arabic-lemma {{
            color: #34d399;
        }}

        .word-action-btns {{
            display: flex;
            gap: 6px;
            align-items: center;
        }}
        .icon-btn {{
            width: 32px;
            height: 32px;
            border-radius: 8px;
            border: 1px solid var(--border);
            background: var(--bg-card);
            color: var(--text-muted);
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
            transition: all 0.15s ease;
        }}
        .icon-btn:hover {{
            background: var(--emerald-light);
            color: var(--emerald);
            border-color: var(--emerald);
            transform: scale(1.05);
        }}
        .icon-btn.bookmarked {{
            color: var(--gold);
            background: var(--gold-light);
            border-color: var(--gold);
        }}

        .word-meta {{
            display: flex;
            gap: 8px;
            font-size: 13px;
            color: var(--text-muted);
            align-items: center;
        }}
        .translit-text {{
            font-weight: 600;
            font-style: italic;
        }}
        .root-text {{
            background: var(--gold-light);
            color: var(--gold);
            font-weight: 700;
            padding: 1px 6px;
            border-radius: 4px;
            font-size: 11px;
            border: 1px solid var(--gold-border);
        }}

        /* Meanings Multi-Language Grid */
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
        .verse-ref-link {{
            color: var(--emerald);
            text-decoration: none;
            background: var(--emerald-light);
            padding: 2px 8px;
            border-radius: 6px;
            border: 1px solid var(--emerald-border);
            transition: all 0.15s ease;
        }}
        .verse-ref-link:hover {{
            background: var(--emerald);
            color: #ffffff;
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

        /* Polysemy / Wujūh al-Qur'an Tabs */
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

        /* Flashcard / Quick Explorer View */
        #flashcard-view-wrapper {{
            display: none;
            max-width: 680px;
            margin: 40px auto;
            text-align: center;
        }}
        .flashcard-card {{
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: var(--radius-xl);
            padding: 40px 32px;
            box-shadow: var(--shadow-lg);
            display: flex;
            flex-direction: column;
            gap: 20px;
            position: relative;
        }}

        /* Modal Dialog */
        .modal-overlay {{
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.6);
            backdrop-filter: blur(6px);
            display: none;
            align-items: center;
            justify-content: center;
            z-index: 100;
            padding: 20px;
        }}
        .modal-content {{
            background: var(--bg-card);
            border-radius: var(--radius-xl);
            max-width: 600px;
            width: 100%;
            max-height: 90vh;
            overflow-y: auto;
            padding: 28px;
            border: 1px solid var(--border);
            box-shadow: var(--shadow-lg);
            position: relative;
        }}
        .modal-close-btn {{
            position: absolute;
            top: 16px;
            right: 16px;
            background: none;
            border: none;
            font-size: 20px;
            color: var(--text-muted);
            cursor: pointer;
        }}

        /* Floating Toast */
        #toast-container {{
            position: fixed;
            bottom: 24px;
            right: 24px;
            display: flex;
            flex-direction: column;
            gap: 8px;
            z-index: 110;
            pointer-events: none;
        }}
        .toast-msg {{
            background: var(--bg-sidebar);
            color: #ffffff;
            padding: 12px 20px;
            border-radius: 10px;
            font-size: 13px;
            font-weight: 600;
            box-shadow: var(--shadow-lg);
            border: 1px solid rgba(255, 255, 255, 0.1);
            animation: toastIn 0.3s cubic-bezier(0.16, 1, 0.3, 1), toastOut 0.3s cubic-bezier(0.16, 1, 0.3, 1) 2.7s forwards;
            display: flex;
            align-items: center;
            gap: 8px;
        }}
        @keyframes toastIn {{
            from {{ transform: translateY(20px); opacity: 0; }}
            to {{ transform: translateY(0); opacity: 1; }}
        }}
        @keyframes toastOut {{
            from {{ transform: translateY(0); opacity: 1; }}
            to {{ transform: translateY(20px); opacity: 0; }}
        }}

        /* Buttons & Pills */
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
            transition: all 0.15s ease;
            user-select: none;
        }}
        .btn:hover {{
            background: var(--bg-primary);
            border-color: var(--emerald);
            transform: translateY(-1px);
        }}
        .btn-primary {{
            background: linear-gradient(135deg, var(--emerald) 0%, #047857 100%);
            color: #ffffff;
            border-color: var(--emerald);
        }}
        .btn-primary:hover {{
            background: #047857;
            color: #ffffff;
        }}

        .count-pill {{
            background: rgba(0, 0, 0, 0.2);
            color: #ffffff;
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 700;
        }}

        /* Mobile Hamburger & Drawer */
        #mobile-menu-btn {{
            display: none;
            padding: 8px;
            font-size: 20px;
            background: none;
            border: none;
            color: var(--text-main);
            cursor: pointer;
        }}
        @media (max-width: 900px) {{
            #sidebar {{
                position: fixed;
                left: 0;
                top: 0;
                bottom: 0;
                transform: translateX(-100%);
                box-shadow: var(--shadow-lg);
            }}
            #sidebar.open {{
                transform: translateX(0);
            }}
            #mobile-menu-btn {{
                display: block;
            }}
            #content-area {{
                padding: 16px;
            }}
            #topbar {{
                padding: 12px 16px;
            }}
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

    <!-- Toast Notification Container -->
    <div id="toast-container"></div>

    <!-- Random Word Discovery Modal -->
    <div id="random-word-modal" class="modal-overlay" onclick="closeRandomModal(event)">
        <div class="modal-content" onclick="event.stopPropagation()">
            <button class="modal-close-btn" onclick="closeRandomModal()">✕</button>
            <div style="font-size:12px; font-weight:800; color:var(--emerald); text-transform:uppercase; letter-spacing:0.05em; margin-bottom:12px;">
                🎲 Random Quranic Word Discovery
            </div>
            <div id="random-modal-body">
                <!-- Dynamically filled with random word card -->
            </div>
        </div>
    </div>

    <!-- Left Sidebar: Chapters & Sections Directory -->
    <aside id="sidebar">
        <div class="sidebar-header">
            <div style="display:flex; align-items:center; gap:12px;">
                <img src="{LOGO_DATA_URI}" alt="deanybytes QuranicWords logo" width="38" height="38" style="border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.3); background:#ffffff; padding:2px; flex-shrink:0;">
                <div>
                    <h2 style="font-size:16px; font-weight:800; color:#ffffff; line-height:1.2;">QuranicWords</h2>
                    <p style="font-size:11px; color:#a7f3d0;">by deanybytes</p>
                </div>
            </div>
            <div style="margin-top:14px; font-size:12px; color:#cbd5e1; display:flex; justify-content:space-between; align-items:center;">
                <span>10 Chapters · 4,709 Words</span>
                <span class="count-pill">100% Verified</span>
            </div>
        </div>

        <nav class="sidebar-nav">
            <div style="padding: 6px 8px; font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em;">
                Table of Contents
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
            <div style="display:flex; gap:12px; align-items:center;">
                <button id="mobile-menu-btn" onclick="toggleMobileSidebar()" aria-label="Toggle Navigation">☰</button>

                <!-- Live Search Box -->
                <div class="search-box">
                    <span class="search-icon">🔍</span>
                    <input type="text" id="search-input" placeholder="Search Arabic, Translit, Root (e.g. كتب), English, বাংলা, اردو, Surah:Ayah (e.g. 2:255)... (Press '/' to focus)" aria-label="Search Quranic words">
                    <button id="search-clear-btn" class="search-clear-btn" onclick="clearSearch()">✕</button>
                </div>

                <!-- View Switchers & Controls -->
                <div style="display:flex; gap:6px; align-items:center;">
                    <button id="view-tree-btn" class="btn btn-primary" onclick="switchView('tree')">📚 Tree</button>
                    <button id="view-table-btn" class="btn" onclick="switchView('table')">📋 Table</button>
                    <button id="random-btn" class="btn" onclick="openRandomWord()" title="Explore random Quranic word">🎲 Random</button>
                    <button id="theme-toggle-btn" class="btn" onclick="toggleDarkMode()" title="Toggle Dark/Light Mode">🌓 Mode</button>
                </div>
            </div>

            <!-- Filter Controls Row -->
            <div class="chips-row">
                <span style="font-size:12px; font-weight:700; color:var(--text-muted); margin-right:4px;">Filter:</span>
                <div class="filter-chip active" onclick="filterCategory('ALL', this)">All (4,709)</div>
                <div class="filter-chip" onclick="filterCategory('PARTICLE', this)">Particles (173)</div>
                <div class="filter-chip" onclick="filterCategory('VERB', this)">Verbs (1,479)</div>
                <div class="filter-chip" onclick="filterCategory('NOUN', this)">Nouns (3,057)</div>
                <div class="filter-chip" onclick="filterCategory('POLYSEMY', this)">✨ Polysemy (44)</div>
                <div class="filter-chip" onclick="filterCategory('BOOKMARKS', this)" id="bookmark-chip">⭐ Bookmarks (<span id="bookmark-count">0</span>)</div>

                <div style="margin-left:auto; display:flex; gap:8px; align-items:center;">
                    <!-- Chapter Dropdown -->
                    <select id="chapter-select" class="btn" onchange="onChapterSelect(this.value)" style="padding:6px 10px;">
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

                    <button class="btn" onclick="expandAll()">Expand</button>
                    <button class="btn" onclick="collapseAll()">Collapse</button>
                </div>
            </div>

            <!-- Languages Visibility Toggle Row -->
            <div class="chips-row" style="margin-top:10px; padding-top:8px; border-top:1px dashed var(--border);">
                <span style="font-size:12px; font-weight:700; color:var(--text-muted);">Languages:</span>
                <div class="lang-toggles">
                    <button class="lang-toggle-btn active" onclick="toggleLanguage('en', this)">🇬🇧 English</button>
                    <button class="lang-toggle-btn active" onclick="toggleLanguage('bn', this)">🇧🇩 বাংলা</button>
                    <button class="lang-toggle-btn active" onclick="toggleLanguage('ur', this)">🇵🇰 اردو</button>
                    <button class="lang-toggle-btn" onclick="toggleLanguage('hi', this)">🇮🇳 हिन्दी</button>
                    <button class="lang-toggle-btn" onclick="toggleLanguage('in', this)">🇮🇩 Bahasa</button>
                    <button class="lang-toggle-btn" onclick="toggleLanguage('ms', this)">🇲🇾 Melayu</button>
                    <button class="lang-toggle-btn" onclick="toggleLanguage('tr', this)">🇹🇷 Türkçe</button>
                    <button class="lang-toggle-btn" onclick="toggleLanguage('fa', this)">🇮🇷 فارسی</button>
                    <button class="lang-toggle-btn" onclick="toggleLanguage('ha', this)">🇳🇬 Hausa</button>
                    <button class="lang-toggle-btn" onclick="toggleLanguage('sw', this)">🇰🇪 Swahili</button>
                    <button class="lang-toggle-btn" onclick="toggleLanguage('fr', this)">🇫🇷 Français</button>
                </div>
                <div id="stats-badge" style="margin-left:auto; font-size:12px; font-weight:700; color:var(--emerald);">
                    Showing 4,709 / 4,709 words
                </div>
            </div>
        </header>

        <!-- Main Content Area -->
        <main id="content-area">
            <!-- Hero Overview Card -->
            <section class="hero-banner">
                <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:16px;">
                    <div>
                        <div style="display:inline-flex; align-items:center; gap:8px; background:rgba(255,255,255,0.15); padding:4px 12px; border-radius:20px; font-size:12px; font-weight:700; margin-bottom:10px;">
                            <span>🕌 Official Master Curriculum Dictionary</span>
                            <span style="opacity:0.8;">•</span>
                            <span style="color:#a7f3d0;">v1.0.0 Verified</span>
                        </div>
                        <h1 style="font-size:28px; font-weight:900; line-height:1.2; letter-spacing:-0.02em;">
                            The Complete Vocabulary of the Holy Qur'an
                        </h1>
                        <p style="font-size:15px; color:#e2e8f0; margin-top:6px; max-width:760px;">
                            Every unique lemma in the Qur'an organized by pedagogical frequency. Featuring authentic Uthmani calligraphy, verified ayah citations, multi-lingual translations, root analyses, and polysemy exploration.
                        </p>
                    </div>
                </div>

                <div class="stats-grid">
                    <div class="stat-card">
                        <div class="stat-num">10</div>
                        <div class="stat-label">Chapters</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-num">100</div>
                        <div class="stat-label">Sections</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-num">1,214</div>
                        <div class="stat-label">Lessons</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-num">4,709</div>
                        <div class="stat-label">Lemmas</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-num">85%+</div>
                        <div class="stat-label">Quran Tokens</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-num">11</div>
                        <div class="stat-label">Languages</div>
                    </div>
                </div>
            </section>

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
                            <th>Actions</th>
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

        // Flatten all words for instant indexing and fast search
        const FLAT_WORDS = [];
        CURRICULUM_DATA.forEach(ch => {{
            ch.sections.forEach(sec => {{
                sec.lessons.forEach(les => {{
                    les.words.forEach(w => {{
                        FLAT_WORDS.push({{
                            ...w,
                            chapterTitle: ch.title,
                            sectionTitle: sec.title,
                            lessonTitle: les.title
                        }});
                    }});
                }});
            }});
        }});

        // State variables
        let currentView = 'tree';
        let currentCategory = 'ALL';
        let currentChapter = 'ALL';
        let currentSearch = '';
        let visibleLangs = {{ en: true, bn: true, ur: true, hi: false, in: false, ms: false, tr: false, fa: false, ha: false, sw: false, fr: false }};
        let bookmarks = JSON.parse(localStorage.getItem('qw_bookmarks') || '[]');

        const LANG_FLAGS = {{
            en: '🇬🇧', bn: '🇧🇩', ur: '🇵🇰', hi: '🇮🇳', in: '🇮🇩', ms: '🇲🇾',
            tr: '🇹🇷', fa: '🇮🇷', ha: '🇳🇬', sw: '🇰🇪', fr: '🇫🇷'
        }};

        // Render Sidebar Navigation
        function renderSidebar() {{
            const listEl = document.getElementById('sidebar-chapters-list');
            listEl.innerHTML = CURRICULUM_DATA.map(ch => `
                <div class="nav-chapter">
                    <button class="nav-chapter-btn" onclick="scrollToElement('ch-${{ch.id}}')">
                        <span>Ch ${{ch.sortOrder}}: ${{ch.title.en}}</span>
                        <span class="count-pill">${{ch.wordCount}}</span>
                    </button>
                    <div class="nav-sections-list">
                        ${{ch.sections.map(sec => `
                            <a href="#sec-${{sec.id}}" class="nav-sec-link" onclick="scrollToElement('sec-${{sec.id}}'); event.preventDefault();">
                                ${{sec.title.en}}
                            </a>
                        `).join('')}}
                    </div>
                </div>
            `).join('');
            updateBookmarkCount();
        }}

        function scrollToElement(id) {{
            const el = document.getElementById(id);
            if (el) {{
                el.scrollIntoView({{ behavior: 'smooth', block: 'start' }});
                if (window.innerWidth <= 900) {{
                    document.getElementById('sidebar').classList.remove('open');
                }}
            }}
        }}

        function toggleMobileSidebar() {{
            document.getElementById('sidebar').classList.toggle('open');
        }}

        // Arabic Tashkeel Stripping Helper
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
                            // Bookmarks Filter
                            if (currentCategory === 'BOOKMARKS' && !bookmarks.includes(w.wordId)) return false;

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

            container.innerHTML = html || `
                <div style="text-align:center; padding:80px 20px; color:var(--text-muted); background:var(--bg-card); border-radius:var(--radius-lg); border:1px solid var(--border);">
                    <div style="font-size:48px; margin-bottom:12px;">🔍</div>
                    <h3 style="font-size:18px; font-weight:700; color:var(--text-main);">No matching Quranic words found</h3>
                    <p style="margin-top:6px;">Try searching with another keyword, root, or reset the active filters.</p>
                </div>
            `;
            document.getElementById('stats-badge').innerText = `Showing ${{matchCount.toLocaleString()}} / ${{FLAT_WORDS.length.toLocaleString()}} words`;
        }}

        // Render Word Card HTML
        function renderWordCard(w) {{
            const isPoly = w.polysemyEntries && w.polysemyEntries.length > 1;
            const isBookmarked = bookmarks.includes(w.wordId);

            return `
                <article class="word-card" id="card-${{w.wordId}}">
                    <!-- Card Topbar -->
                    <div class="word-card-topbar">
                        <div style="display:flex; gap:6px; align-items:center;">
                            <span class="badge-id">${{w.wordId}}</span>
                            <span class="badge-pos">${{w.partOfSpeech || w.category}}</span>
                            ${{isPoly ? `<span style="background:var(--purple-light); color:var(--purple); font-size:11px; font-weight:800; padding:2px 8px; border-radius:10px; border:1px solid rgba(124,58,237,0.3);">✨ ${{w.polysemyEntries.length}} Senses</span>` : ''}}
                        </div>
                        <div style="display:flex; gap:6px; align-items:center;">
                            <span class="badge-occ">${{w.quranOccurrenceCount.toLocaleString()}} occ</span>
                            <button class="icon-btn ${{isBookmarked ? 'bookmarked' : ''}}" onclick="toggleBookmark('${{w.wordId}}', this)" title="${{isBookmarked ? 'Remove Bookmark' : 'Bookmark Word'}}">
                                ${{isBookmarked ? '★' : '☆'}}
                            </button>
                        </div>
                    </div>

                    <!-- Arabic Word & Pronunciation/Copy Display -->
                    <div class="word-main-display">
                        <div>
                            <div class="arabic-lemma font-arabic">${{w.arabicWord}}</div>
                            <div class="word-meta">
                                <span class="translit-text">${{w.transliteration || ''}}</span>
                                ${{w.root ? `<span class="root-text">Root: ${{w.root}}</span>` : ''}}
                            </div>
                        </div>
                        <div class="word-action-btns">
                            <button class="icon-btn" onclick="playAudio('${{w.arabicWord}}')" title="Pronounce Arabic">🔊</button>
                            <button class="icon-btn" onclick="copyWord('${{w.wordId}}')" title="Copy Word & Details">📋</button>
                        </div>
                    </div>

                    <!-- Meanings Multi-Language Grid -->
                    <div class="meanings-grid">
                        ${{renderMeaningsGridHtml(w.meaning)}}
                    </div>

                    <!-- Example Verse Box -->
                    <div class="verse-box" id="verse-box-${{w.wordId}}">
                        <div class="verse-meta-row">
                            <span>📖 Authentic Qur'an Context</span>
                            <a href="https://quran.com/${{w.exampleVerseReference.split(' ')[1] || '2:255'}}" target="_blank" rel="noopener" class="verse-ref-link" id="ref-${{w.wordId}}">
                                ${{w.exampleVerseReference}} ↗
                            </a>
                        </div>
                        <div class="verse-arabic-text font-arabic" id="verse-ar-${{w.wordId}}">
                            ${{w.exampleVerseArabicHl}}
                        </div>
                        <div class="verse-translations-list" id="verse-trans-${{w.wordId}}">
                            ${{renderVerseTranslationsHtml(w.exampleVerseTranslation, w.meaningHighlight)}}
                        </div>
                    </div>

                    <!-- Polysemy / Wujūh al-Qur'an Interactive Tabs -->
                    ${{isPoly ? `
                        <div class="polysemy-card-block">
                            <div style="font-size:12px; font-weight:800; color:var(--purple); display:flex; justify-content:space-between; align-items:center;">
                                <span>📚 Contextual Senses (Wujūh al-Qur'an)</span>
                                <span style="font-size:10px; opacity:0.8;">Tap sense to update</span>
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
                </article>
            `;
        }}

        function renderMeaningsGridHtml(meaningObj) {{
            let out = '';
            for (const [lang, text] of Object.entries(meaningObj || {{}})) {{
                if (!visibleLangs[lang]) continue;
                const fontClass = lang === 'bn' ? 'font-bn' : (lang === 'ur' ? 'font-ur' : '');
                out += `
                    <div class="meaning-item">
                        <span class="lang-flag">${{LANG_FLAGS[lang] || lang.toUpperCase()}}</span>
                        <span class="meaning-val ${{fontClass}}">${{text || '—'}}</span>
                    </div>
                `;
            }}
            return out;
        }}

        function renderVerseTranslationsHtml(transDict, hlDict) {{
            let out = '';
            for (const [lang, text] of Object.entries(transDict || {{}})) {{
                if (!visibleLangs[lang]) continue;
                const fontClass = lang === 'bn' ? 'font-bn' : (lang === 'ur' ? 'font-ur' : '');
                const hlWord = (hlDict && hlDict[lang]) ? hlDict[lang] : '';
                const renderedText = highlightTranslationClient(text, hlWord);
                out += `
                    <div class="v-trans-item ${{fontClass}}">
                        <span class="v-trans-tag">${{LANG_FLAGS[lang] || lang.toUpperCase()}}:</span>
                        <span>${{renderedText || '—'}}</span>
                    </div>
                `;
            }}
            return out;
        }}

        function highlightTranslationClient(trans, hlWord) {{
            if (!trans) return '';
            if (!hlWord || hlWord.length < 2) return trans;
            const idx = trans.toLowerCase().indexOf(hlWord.toLowerCase());
            if (idx >= 0) {{
                const before = trans.substring(0, idx);
                const match = trans.substring(idx, idx + hlWord.length);
                const after = trans.substring(idx + hlWord.length);
                return `${{before}}<mark class="tr-hl">${{match}}</mark>${{after}}`;
            }}
            return trans;
        }}

        // Switch Polysemy Sense in Card
        function switchPolySense(wordId, senseIdx, btnEl) {{
            const w = FLAT_WORDS.find(item => item.wordId === wordId);
            if (!w || !w.polysemyEntries || !w.polysemyEntries[senseIdx]) return;

            const se = w.polysemyEntries[senseIdx];
            
            const parent = btnEl.parentElement;
            parent.querySelectorAll('.poly-tab-btn').forEach(b => b.classList.remove('active'));
            btnEl.classList.add('active');

            const refEl = document.getElementById(`ref-${{wordId}}`);
            if (refEl) {{
                refEl.innerText = se.verseReference + ' ↗';
                refEl.href = `https://quran.com/${{se.verseReference.split(' ')[1] || '2:255'}}`;
            }}
            const verseArEl = document.getElementById(`verse-ar-${{wordId}}`);
            if (verseArEl) verseArEl.innerHTML = se.verseArabicHl;
            
            const verseTransEl = document.getElementById(`verse-trans-${{wordId}}`);
            if (verseTransEl) verseTransEl.innerHTML = renderVerseTranslationsHtml(se.verseTranslation, se.translationHighlight);
        }}

        // Audio Pronunciation using Web Speech Synthesis API
        function playAudio(arabicText) {{
            if (!window.speechSynthesis) {{
                showToast('Speech audio synthesis not supported in this browser 🔊');
                return;
            }}
            window.speechSynthesis.cancel();
            const utterance = new SpeechSynthesisUtterance(arabicText);
            utterance.lang = 'ar-SA';
            utterance.rate = 0.85;
            window.speechSynthesis.speak(utterance);
            showToast(`Playing audio: ${{arabicText}} 🔊`);
        }}

        // Quick Copy to Clipboard
        function copyWord(wordId) {{
            const w = FLAT_WORDS.find(item => item.wordId === wordId);
            if (!w) return;
            const textToCopy = `${{w.arabicWord}} (${{w.transliteration || ''}}) - Meaning: ${{w.meaning.en || ''}} | Ref: ${{w.exampleVerseReference}}`;
            navigator.clipboard.writeText(textToCopy).then(() => {{
                showToast(`Copied to clipboard: "${{w.arabicWord}}" 📋`);
            }}).catch(() => {{
                showToast('Failed to copy text ⚠️');
            }});
        }}

        // Bookmark Toggle
        function toggleBookmark(wordId, btnEl) {{
            const idx = bookmarks.indexOf(wordId);
            if (idx >= 0) {{
                bookmarks.splice(idx, 1);
                if (btnEl) {{
                    btnEl.classList.remove('bookmarked');
                    btnEl.innerText = '☆';
                }}
                showToast('Removed from Bookmarks');
            }} else {{
                bookmarks.push(wordId);
                if (btnEl) {{
                    btnEl.classList.add('bookmarked');
                    btnEl.innerText = '★';
                }}
                showToast('Added to Bookmarks ⭐');
            }}
            localStorage.setItem('qw_bookmarks', JSON.stringify(bookmarks));
            updateBookmarkCount();
            if (currentCategory === 'BOOKMARKS') {{
                refreshView();
            }}
        }}

        function updateBookmarkCount() {{
            const countEl = document.getElementById('bookmark-count');
            if (countEl) countEl.innerText = bookmarks.length;
        }}

        // Toast Notification System
        function showToast(msg) {{
            const container = document.getElementById('toast-container');
            const toast = document.createElement('div');
            toast.className = 'toast-msg';
            toast.innerText = msg;
            container.appendChild(toast);
            setTimeout(() => {{
                toast.remove();
            }}, 3000);
        }}

        // Random Word Discovery
        function openRandomWord() {{
            const randomIndex = Math.floor(Math.random() * FLAT_WORDS.length);
            const w = FLAT_WORDS[randomIndex];
            const modalBody = document.getElementById('random-modal-body');
            modalBody.innerHTML = renderWordCard(w);
            document.getElementById('random-word-modal').style.display = 'flex';
        }}

        function closeRandomModal(event) {{
            document.getElementById('random-word-modal').style.display = 'none';
        }}

        // Render Compact Table View
        function renderTableView() {{
            const tbody = document.getElementById('table-body');
            const q = currentSearch.toLowerCase().trim();
            const qNorm = stripTashkeelJs(q);
            let matchCount = 0;

            const filtered = FLAT_WORDS.filter(w => {{
                if (currentCategory === 'BOOKMARKS' && !bookmarks.includes(w.wordId)) return false;
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
                    <td class="font-arabic" style="font-size:24px; font-weight:700; color:var(--emerald);">${{w.arabicWord}}</td>
                    <td>
                        <div style="font-weight:600; font-style:italic;">${{w.transliteration || '—'}}</div>
                        ${{w.root ? `<div style="color:var(--gold); font-size:11px; font-weight:700;">Root: ${{w.root}}</div>` : ''}}
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
                    <td style="min-width:240px;">
                        <div style="font-weight:700; color:var(--emerald); font-size:11px; margin-bottom:2px;">${{w.exampleVerseReference}}</div>
                        <div class="font-arabic" style="font-size:15px; margin-bottom:4px;">${{w.exampleVerseArabicHl}}</div>
                    </td>
                    <td>
                        <div style="display:flex; gap:4px;">
                            <button class="icon-btn" onclick="playAudio('${{w.arabicWord}}')" title="Pronounce">🔊</button>
                            <button class="icon-btn" onclick="copyWord('${{w.wordId}}')" title="Copy">📋</button>
                        </div>
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

        function toggleLanguage(lang, btnEl) {{
            visibleLangs[lang] = !visibleLangs[lang];
            btnEl.classList.toggle('active');
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
            const isDark = document.documentElement.classList.contains('dark');
            localStorage.setItem('qw_theme', isDark ? 'dark' : 'light');
        }}

        function clearSearch() {{
            document.getElementById('search-input').value = '';
            document.getElementById('search-clear-btn').style.display = 'none';
            currentSearch = '';
            refreshView();
        }}

        function refreshView() {{
            if (currentView === 'tree') {{
                renderTreeView();
            }} else {{
                renderTableView();
            }}
        }}

        // Search Debouncing & Hotkey
        let searchTimer = null;
        const searchInput = document.getElementById('search-input');
        searchInput.addEventListener('input', (e) => {{
            const val = e.target.value;
            document.getElementById('search-clear-btn').style.display = val ? 'block' : 'none';
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {{
                currentSearch = val;
                refreshView();
            }}, 180);
        }});

        // Global '/' shortcut for instant search focus
        window.addEventListener('keydown', (e) => {{
            if (e.key === '/' && document.activeElement !== searchInput) {{
                e.preventDefault();
                searchInput.focus();
                searchInput.select();
            }}
            if (e.key === 'Escape' && document.getElementById('random-word-modal').style.display === 'flex') {{
                closeRandomModal();
            }}
        }});

        // Initialization
        window.addEventListener('DOMContentLoaded', () => {{
            // Restore theme preference
            const savedTheme = localStorage.getItem('qw_theme');
            if (savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {{
                document.documentElement.classList.add('dark');
            }}
            renderSidebar();
            renderTreeView();
        }});
    </script>
</body>
</html>
"""

    print(f"Writing complete HTML dictionary to {OUTPUT_FILE_V1}...")
    with open(OUTPUT_FILE_V1, 'w', encoding='utf-8') as f:
        f.write(html_template)

    file_size_mb = os.path.getsize(OUTPUT_FILE_V1) / (1024 * 1024)
    print(f"SUCCESS: Generated {OUTPUT_FILE_V1} ({file_size_mb:.2f} MB)!")

if __name__ == '__main__':
    main()
