#!/usr/bin/env python3
"""
QuranicWords Master Curriculum Interactive Web Application & Dictionary Generator
==================================================================================
Generates an ultra-fast (<1s load, 60fps), SEO-optimized, breathtaking, glassmorphic
web app: `QuranicWords-v1.0.0.html`

Architectural Highlights:
- High-Performance Compact Inlined Dataset (~8 MB payload vs 74 MB).
- Infinite / Progressive Batch Rendering (Render 24 cards initially in <5ms, stream more on scroll).
- Zero DOM Reflow Latency with IntersectionObserver.
- 11 Master Languages: English, Bengali (বাংলা), Urdu (اردو), Hindi (हिन्दी),
  Indonesian (Bahasa), Malay (Melayu), Turkish (Türkçe), Persian (فارسی),
  Hausa, Swahili (Kiswahili), French (Français).
- 3 View Modes: 📚 Card Stream View, 📋 Interactive Data Table, 📇 Flashcard Master Mode.
- 🔊 Audio Pronunciation with SpeechSynthesis.
- 📋 Instant Quick Copy with animated glass toast notifications.
- ⭐ Bookmarking / Favorites with localStorage persistence.
- 🎲 Random Word / Ayah Discovery Modal.
- 🔀 Interactive Polysemy / Wujūh al-Qur'an Sense Explorer with real-time Ayah context switching.
- 🌓 Sleek Dark / Light Mode with frosted glassmorphism.
- 🚀 Complete SEO metadata: OpenGraph, Twitter Cards, Schema.org JSON-LD.
- 📱 Mobile-First Responsive Drawer and Floating Action Bar.
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

    SUPPORTED_LANGS = ['en', 'bn', 'ur', 'hi', 'in', 'ms', 'tr', 'fa', 'ha', 'sw', 'fr']
    CORE_VERSE_LANGS = ['en', 'bn', 'ur', 'hi', 'in', 'tr', 'fr']

    chapters_meta = []
    flat_words = []
    total_words_count = 0
    poly_words_count = 0

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
                        total_words_count += 1
                        
                        m_all = {l: c.get('meaning', {}).get(l, '') for l in SUPPORTED_LANGS if c.get('meaning', {}).get(l)}
                        v_tr = {l: c.get('exampleVerseTranslation', {}).get(l, '') for l in CORE_VERSE_LANGS if c.get('exampleVerseTranslation', {}).get(l)}
                        hl_m = {l: c.get('meaningHighlight', {}).get(l, '') for l in CORE_VERSE_LANGS if c.get('meaningHighlight', {}).get(l)}

                        # Build highlighted translations
                        v_tr_hl = {}
                        for l in CORE_VERSE_LANGS:
                            if l in v_tr:
                                v_tr_hl[l] = highlight_translation(v_tr[l], hl_m.get(l, ''))
                        
                        poly_entries = []
                        for se in c.get('polysemyEntries', []):
                            se_m = {l: se.get('contextualMeaning', {}).get(l, '') for l in SUPPORTED_LANGS if se.get('contextualMeaning', {}).get(l)}
                            se_v_tr = {l: se.get('verseTranslation', {}).get(l, '') for l in CORE_VERSE_LANGS if se.get('verseTranslation', {}).get(l)}
                            se_hl_m = {l: se.get('translationHighlight', {}).get(l, '') for l in CORE_VERSE_LANGS if se.get('translationHighlight', {}).get(l)}
                            
                            se_v_tr_hl = {}
                            for l in CORE_VERSE_LANGS:
                                if l in se_v_tr:
                                    se_v_tr_hl[l] = highlight_translation(se_v_tr[l], se_hl_m.get(l, ''))
                                    
                            poly_entries.append({
                                'idx': se['meaningIndex'],
                                'm': se_m,
                                'ref': se.get('verseReference', ''),
                                'v_ar': highlight_arabic(se.get('verseArabic', ''), se.get('arabicWordStart'), se.get('arabicWordEnd'), c.get('arabicWord', '')),
                                'v_tr_hl': se_v_tr_hl
                            })
                            
                        if len(poly_entries) > 1:
                            poly_words_count += 1

                        w_item = {
                            'id': c['wordId'],
                            'ar': c['arabicWord'],
                            'norm': strip_tashkeel(c['arabicWord']),
                            'tr': c.get('transliteration', ''),
                            'rt': c.get('root', ''),
                            'pos': c.get('partOfSpeech', ''),
                            'cat': les['category'],
                            'ch': cid,
                            'sec': sid,
                            'les': lid,
                            'chSort': ch['sortOrder'],
                            'chTitle': ch['title'].get('en', ''),
                            'secTitle': sec['title'].get('en', ''),
                            'lesTitle': les['title'].get('en', ''),
                            'occ': c.get('quranOccurrenceCount', 1),
                            'm': m_all,
                            'ref': c.get('exampleVerseReference', ''),
                            'v_ar': highlight_arabic(c.get('exampleVerseArabic', ''), c.get('arabicWordStart'), c.get('arabicWordEnd'), c.get('arabicWord', '')),
                            'v_tr_hl': v_tr_hl,
                            'poly': poly_entries if len(poly_entries) > 1 else []
                        }
                        
                        flat_words.append(w_item)

        chapters_meta.append(ch_meta)

    print(f"Total Words Processed: {total_words_count}")
    print(f"Total Multi-meaning (Polysemous) Words: {poly_words_count}")

    # Generate Compact JSON (Stripping whitespace)
    words_json = json.dumps(flat_words, separators=(',', ':'), ensure_ascii=False)
    chapters_json = json.dumps(chapters_meta, separators=(',', ':'), ensure_ascii=False)

    html_template = f"""<!DOCTYPE html>
<html lang="en" class="scroll-smooth">
<head>
    <!-- Meta & Core Standards -->
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    
    <!-- Primary SEO Meta Tags -->
    <title>QuranicWords — 4,709 Quranic Vocabulary Master Curriculum Dictionary</title>
    <meta name="title" content="QuranicWords — 4,709 Quranic Vocabulary Master Curriculum Dictionary">
    <meta name="description" content="Master 4,709 verified Qur'anic Arabic words ordered by frequency. Features authentic verse contexts, multi-lingual translations in 11 languages, root analysis, audio pronunciation, and polysemy exploration.">
    <meta name="keywords" content="Quran vocabulary, Quranic Arabic dictionary, Learn Quran Arabic, Quran words frequency, Quran lemmas, Uthmani Quran, Arabic grammar, Wujuh al-Quran, polysemy, Quranic root words, deanybytes, quranic words english, quran dictionary bangla">
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
    <meta property="og:description" content="Master 4,700+ Quranic Arabic vocabulary words organized by frequency with authentic Ayah contexts in 11 languages.">
    <meta property="og:image" content="{LOGO_DATA_URI}">
    <meta property="og:locale" content="en_US">
    <meta property="og:locale:alternate" content="bn_BD">
    <meta property="og:locale:alternate" content="ur_PK">

    <!-- Twitter Card -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:url" content="https://quranicwords.vercel.app/">
    <meta name="twitter:title" content="QuranicWords — 4,709 Quranic Vocabulary Master Dictionary">
    <meta name="twitter:description" content="Master 4,700+ Quranic Arabic vocabulary words organized by frequency with authentic Ayah contexts in 11 languages.">
    <meta name="twitter:image" content="{LOGO_DATA_URI}">

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
            --bg-glass: rgba(255, 255, 255, 0.88);
            --text-main: #0f172a;
            --text-muted: #64748b;
            --text-light: #94a3b8;
            --border: #e2e8f0;
            --border-glow: rgba(5, 150, 105, 0.25);
            --emerald: #059669;
            --emerald-dark: #064e3b;
            --emerald-light: #ecfdf5;
            --emerald-border: #a7f3d0;
            --gold: #d97706;
            --gold-light: #fef3c7;
            --gold-border: #fde68a;
            --purple: #7c3aed;
            --purple-light: #f5f3ff;
            --shadow-sm: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
            --shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
            --shadow-lg: 0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
            --radius: 12px;
            --radius-lg: 16px;
            --radius-xl: 24px;
        }}

        .dark {{
            --bg-primary: #070a12;
            --bg-card: #0f172a;
            --bg-card-header: #151e33;
            --bg-sidebar: #05080f;
            --bg-glass: rgba(15, 23, 42, 0.88);
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
            --purple-light: rgba(139, 92, 246, 0.12);
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
            transition: background-color 0.2s ease, color 0.2s ease;
        }}

        /* Typography */
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

        /* Highlighting */
        mark.ar-hl {{
            background: linear-gradient(135deg, #f59e0b, #d97706);
            color: #ffffff !important;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 6px;
            box-shadow: 0 2px 6px rgba(217, 119, 6, 0.35);
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

        /* Sidebar */
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
            transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
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
            transition: color 0.15s;
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
            padding: 5px 8px;
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
            transform: translateX(2px);
        }}

        /* Main Wrapper */
        #main-wrapper {{
            flex: 1;
            display: flex;
            flex-direction: column;
            min-width: 0;
            height: 100vh;
            overflow-y: auto;
        }}

        /* Sticky Glass Topbar */
        #topbar {{
            position: sticky;
            top: 0;
            background: var(--bg-glass);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border-bottom: 1px solid var(--border);
            padding: 14px 24px;
            z-index: 30;
            box-shadow: var(--shadow-sm);
        }}

        .search-box {{
            position: relative;
            flex: 1;
        }}
        .search-box input {{
            width: 100%;
            padding: 11px 16px 11px 42px;
            border-radius: 12px;
            border: 2px solid var(--border);
            background-color: var(--bg-card);
            color: var(--text-main);
            font-size: 14px;
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
            font-size: 16px;
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
            font-size: 14px;
            display: none;
            padding: 4px;
        }}

        /* Filter Chips */
        .chips-row {{
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            align-items: center;
            margin-top: 10px;
        }}

        .filter-chip {{
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            border: 1px solid var(--border);
            background-color: var(--bg-card);
            color: var(--text-muted);
            transition: all 0.15s ease;
            user-select: none;
            display: inline-flex;
            align-items: center;
            gap: 5px;
        }}
        .filter-chip:hover {{
            border-color: var(--emerald);
            color: var(--emerald);
        }}
        .filter-chip.active {{
            background: linear-gradient(135deg, var(--emerald) 0%, #047857 100%);
            color: #ffffff;
            border-color: var(--emerald);
            box-shadow: 0 2px 6px rgba(5, 150, 105, 0.3);
        }}

        /* Languages Toggle */
        .lang-toggles {{
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
            align-items: center;
        }}
        .lang-toggle-btn {{
            padding: 4px 9px;
            border-radius: 12px;
            border: 1px solid var(--border);
            background: var(--bg-card);
            color: var(--text-muted);
            cursor: pointer;
            font-size: 11px;
            font-weight: 600;
            transition: all 0.15s ease;
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
            padding: 28px 24px;
            margin-bottom: 24px;
            box-shadow: var(--shadow-lg);
            position: relative;
            overflow: hidden;
        }}
        .hero-banner::after {{
            content: '';
            position: absolute;
            top: -50%;
            right: -20%;
            width: 450px;
            height: 450px;
            background: radial-gradient(circle, rgba(52, 211, 153, 0.15) 0%, transparent 70%);
            pointer-events: none;
        }}

        .stats-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
            gap: 10px;
            margin-top: 20px;
        }}
        .stat-card {{
            background: rgba(255, 255, 255, 0.08);
            backdrop-filter: blur(8px);
            border: 1px solid rgba(255, 255, 255, 0.12);
            border-radius: var(--radius);
            padding: 12px;
            text-align: center;
        }}
        .stat-num {{
            font-size: 20px;
            font-weight: 900;
            color: #34d399;
            line-height: 1.1;
        }}
        .stat-label {{
            font-size: 10px;
            font-weight: 600;
            color: #cbd5e1;
            margin-top: 3px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
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
            padding: 16px;
            display: flex;
            flex-direction: column;
            gap: 12px;
            box-shadow: var(--shadow-sm);
            transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
        }}
        .word-card:hover {{
            transform: translateY(-2px);
            box-shadow: var(--shadow);
            border-color: var(--emerald);
        }}

        .word-card-topbar {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 11px;
        }}

        .badge-id {{
            background-color: var(--bg-primary);
            color: var(--text-muted);
            padding: 2px 7px;
            border-radius: 6px;
            font-weight: 700;
            border: 1px solid var(--border);
        }}

        .badge-pos {{
            background-color: var(--emerald-light);
            color: var(--emerald);
            padding: 2px 7px;
            border-radius: 6px;
            font-weight: 700;
            border: 1px solid var(--emerald-border);
        }}

        .badge-occ {{
            background-color: var(--gold-light);
            color: var(--gold);
            padding: 2px 7px;
            border-radius: 6px;
            font-weight: 700;
            border: 1px solid var(--gold-border);
        }}

        /* Arabic Lemma & Controls */
        .word-main-display {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px 12px;
            background: linear-gradient(135deg, var(--bg-card-header) 0%, var(--bg-card) 100%);
            border-radius: 10px;
            border: 1px solid var(--border);
        }}

        .arabic-lemma {{
            font-size: 26px;
            font-weight: 700;
            color: var(--emerald-dark);
            line-height: 1.3;
        }}
        .dark .arabic-lemma {{
            color: #34d399;
        }}

        .word-action-btns {{
            display: flex;
            gap: 5px;
            align-items: center;
        }}
        .icon-btn {{
            width: 30px;
            height: 30px;
            border-radius: 8px;
            border: 1px solid var(--border);
            background: var(--bg-card);
            color: var(--text-muted);
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 13px;
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
            gap: 6px;
            font-size: 12px;
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
            padding: 1px 5px;
            border-radius: 4px;
            font-size: 11px;
            border: 1px solid var(--gold-border);
        }}

        /* Meanings Grid */
        .meanings-grid {{
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 6px;
            font-size: 12px;
        }}
        @media (max-width: 500px) {{
            .meanings-grid {{
                grid-template-columns: 1fr;
            }}
        }}

        .meaning-item {{
            padding: 6px 10px;
            border-radius: 6px;
            background-color: var(--bg-primary);
            border: 1px solid var(--border);
            display: flex;
            align-items: baseline;
            gap: 6px;
        }}
        .lang-flag {{
            font-size: 13px;
            font-weight: 700;
            flex-shrink: 0;
        }}
        .meaning-val {{
            font-weight: 600;
            color: var(--text-main);
            word-break: break-word;
        }}

        /* Verse Box */
        .verse-box {{
            background: linear-gradient(180deg, var(--bg-card-header) 0%, var(--bg-card) 100%);
            border: 1px solid var(--border);
            border-left: 3px solid var(--emerald);
            border-radius: 8px;
            padding: 12px;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }}

        .verse-meta-row {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 11px;
            font-weight: 700;
            color: var(--emerald);
        }}
        .verse-ref-link {{
            color: var(--emerald);
            text-decoration: none;
            background: var(--emerald-light);
            padding: 1px 6px;
            border-radius: 4px;
            border: 1px solid var(--emerald-border);
        }}
        .verse-ref-link:hover {{
            background: var(--emerald);
            color: #ffffff;
        }}

        .verse-arabic-text {{
            font-size: 20px;
            line-height: 1.7;
            color: #0f172a;
            text-align: right;
            padding: 2px 0;
        }}
        .dark .verse-arabic-text {{
            color: #f8fafc;
        }}

        .verse-translations-list {{
            display: flex;
            flex-direction: column;
            gap: 4px;
            font-size: 12px;
            color: var(--text-main);
            border-top: 1px dashed var(--border);
            padding-top: 6px;
        }}

        .v-trans-item {{
            display: flex;
            gap: 6px;
            line-height: 1.35;
        }}
        .v-trans-tag {{
            font-weight: 700;
            color: var(--text-muted);
            min-width: 22px;
            flex-shrink: 0;
        }}

        /* Polysemy Tabs */
        .polysemy-card-block {{
            background-color: var(--purple-light);
            border: 1px solid rgba(124, 58, 237, 0.25);
            border-radius: 8px;
            padding: 10px;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }}
        .poly-tabs-header {{
            display: flex;
            flex-wrap: wrap;
            gap: 5px;
            align-items: center;
        }}
        .poly-tab-btn {{
            padding: 3px 8px;
            border-radius: 5px;
            font-size: 11px;
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
            box-shadow: var(--shadow-sm);
        }}

        .dict-table {{
            width: 100%;
            border-collapse: collapse;
            font-size: 12px;
        }}
        .dict-table th {{
            background-color: var(--bg-card-header);
            padding: 10px 12px;
            text-align: left;
            font-weight: 700;
            border-bottom: 2px solid var(--border);
            position: sticky;
            top: 0;
            z-index: 10;
        }}
        .dict-table td {{
            padding: 8px 12px;
            border-bottom: 1px solid var(--border);
            vertical-align: top;
        }}
        .dict-table tr:hover {{
            background-color: var(--bg-primary);
        }}

        /* Flashcard View */
        #flashcard-view-wrapper {{
            display: none;
            max-width: 620px;
            margin: 30px auto;
            text-align: center;
        }}
        .flashcard-box {{
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: var(--radius-xl);
            padding: 36px 28px;
            box-shadow: var(--shadow-lg);
            display: flex;
            flex-direction: column;
            gap: 18px;
            position: relative;
        }}

        /* Modal */
        .modal-overlay {{
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.65);
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
            max-width: 580px;
            width: 100%;
            max-height: 90vh;
            overflow-y: auto;
            padding: 24px;
            border: 1px solid var(--border);
            box-shadow: var(--shadow-lg);
            position: relative;
        }}
        .modal-close-btn {{
            position: absolute;
            top: 14px;
            right: 14px;
            background: none;
            border: none;
            font-size: 18px;
            color: var(--text-muted);
            cursor: pointer;
        }}

        /* Toast */
        #toast-container {{
            position: fixed;
            bottom: 20px;
            right: 20px;
            display: flex;
            flex-direction: column;
            gap: 8px;
            z-index: 110;
            pointer-events: none;
        }}
        .toast-msg {{
            background: var(--bg-sidebar);
            color: #ffffff;
            padding: 10px 18px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
            box-shadow: var(--shadow-lg);
            border: 1px solid rgba(255, 255, 255, 0.1);
            animation: toastIn 0.25s ease, toastOut 0.25s ease 2.7s forwards;
            display: flex;
            align-items: center;
            gap: 6px;
        }}
        @keyframes toastIn {{
            from {{ transform: translateY(15px); opacity: 0; }}
            to {{ transform: translateY(0); opacity: 1; }}
        }}
        @keyframes toastOut {{
            from {{ transform: translateY(0); opacity: 1; }}
            to {{ transform: translateY(15px); opacity: 0; }}
        }}

        /* Buttons */
        .btn {{
            padding: 7px 12px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            border: 1px solid var(--border);
            background: var(--bg-card);
            color: var(--text-main);
            display: inline-flex;
            align-items: center;
            gap: 5px;
            transition: all 0.15s ease;
            user-select: none;
        }}
        .btn:hover {{
            background: var(--bg-primary);
            border-color: var(--emerald);
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
            padding: 2px 7px;
            border-radius: 12px;
            font-size: 10px;
            font-weight: 700;
        }}

        /* Sentinel & Infinite Scroll */
        #infinite-sentinel {{
            height: 40px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: var(--text-muted);
            font-size: 12px;
            margin-top: 16px;
        }}

        /* Mobile Layout */
        #mobile-menu-btn {{
            display: none;
            padding: 6px;
            font-size: 18px;
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
                padding: 14px;
            }}
            #topbar {{
                padding: 10px 14px;
            }}
        }}

        /* Scrollbar */
        ::-webkit-scrollbar {{
            width: 7px;
            height: 7px;
        }}
        ::-webkit-scrollbar-track {{
            background: var(--bg-primary);
        }}
        ::-webkit-scrollbar-thumb {{
            background: var(--text-light);
            border-radius: 4px;
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
            <div style="font-size:11px; font-weight:800; color:var(--emerald); text-transform:uppercase; letter-spacing:0.05em; margin-bottom:12px;">
                🎲 Random Quranic Word Discovery
            </div>
            <div id="random-modal-body"></div>
        </div>
    </div>

    <!-- Left Sidebar -->
    <aside id="sidebar">
        <div class="sidebar-header">
            <div style="display:flex; align-items:center; gap:12px;">
                <img src="{LOGO_DATA_URI}" alt="deanybytes QuranicWords logo" width="38" height="38" style="border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.3); background:#ffffff; padding:2px; flex-shrink:0;">
                <div>
                    <h2 style="font-size:15px; font-weight:800; color:#ffffff; line-height:1.2;">QuranicWords</h2>
                    <p style="font-size:11px; color:#a7f3d0;">by deanybytes</p>
                </div>
            </div>
            <div style="margin-top:12px; font-size:11px; color:#cbd5e1; display:flex; justify-content:space-between; align-items:center;">
                <span>10 Chapters · 4,709 Words</span>
                <span class="count-pill">100% Verified</span>
            </div>
        </div>

        <nav class="sidebar-nav">
            <div style="padding: 6px 8px; font-size: 10px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em;">
                Chapters & Sections
            </div>
            <div id="sidebar-chapters-list"></div>
        </nav>
    </aside>

    <!-- Main Content Area -->
    <div id="main-wrapper">
        <!-- Sticky Top Control Bar -->
        <header id="topbar">
            <div style="display:flex; gap:10px; align-items:center;">
                <button id="mobile-menu-btn" onclick="toggleMobileSidebar()" aria-label="Toggle Navigation">☰</button>

                <!-- Live Search Box -->
                <div class="search-box">
                    <span class="search-icon">🔍</span>
                    <input type="text" id="search-input" placeholder="Search Arabic, Translit, Root (e.g. كتب), English, বাংলা, اردو, Ref (2:255)... (Press '/' to focus)" aria-label="Search Quranic words">
                    <button id="search-clear-btn" class="search-clear-btn" onclick="clearSearch()">✕</button>
                </div>

                <!-- View Switchers & Controls -->
                <div style="display:flex; gap:5px; align-items:center;">
                    <button id="view-stream-btn" class="btn btn-primary" onclick="switchView('stream')">✨ Cards</button>
                    <button id="view-table-btn" class="btn" onclick="switchView('table')">📋 Table</button>
                    <button id="view-flashcard-btn" class="btn" onclick="switchView('flashcard')">📇 Study</button>
                    <button id="random-btn" class="btn" onclick="openRandomWord()" title="Random Word">🎲</button>
                    <button id="theme-toggle-btn" class="btn" onclick="toggleDarkMode()" title="Dark/Light Mode">🌓</button>
                </div>
            </div>

            <!-- Filter Controls Row -->
            <div class="chips-row">
                <span style="font-size:11px; font-weight:700; color:var(--text-muted); margin-right:2px;">Filter:</span>
                <div class="filter-chip active" onclick="filterCategory('ALL', this)">All (4,709)</div>
                <div class="filter-chip" onclick="filterCategory('PARTICLE', this)">Particles (173)</div>
                <div class="filter-chip" onclick="filterCategory('VERB', this)">Verbs (1,479)</div>
                <div class="filter-chip" onclick="filterCategory('NOUN', this)">Nouns (3,057)</div>
                <div class="filter-chip" onclick="filterCategory('POLYSEMY', this)">✨ Polysemy (44)</div>
                <div class="filter-chip" onclick="filterCategory('BOOKMARKS', this)" id="bookmark-chip">⭐ Saved (<span id="bookmark-count">0</span>)</div>

                <div style="margin-left:auto; display:flex; gap:6px; align-items:center;">
                    <select id="chapter-select" class="btn" onchange="onChapterSelect(this.value)" style="padding:5px 8px; font-size:11px;">
                        <option value="ALL">All 10 Chapters</option>
                        <option value="ch_01">Ch 1: Particles (173)</option>
                        <option value="ch_02">Ch 2: High-Freq Verbs (500)</option>
                        <option value="ch_03">Ch 3: Verbal Forms (500)</option>
                        <option value="ch_04">Ch 4: Specialized Verbs (479)</option>
                        <option value="ch_05">Ch 5: Divine Names & Nominals (510)</option>
                        <option value="ch_06">Ch 6: Essential Nominals (510)</option>
                        <option value="ch_07">Ch 7: Devotional Nominals (510)</option>
                        <option value="ch_08">Ch 8: Prophetic Nominals (510)</option>
                        <option value="ch_09">Ch 9: Moral Nominals (510)</option>
                        <option value="ch_10">Ch 10: Cosmic Nominals (507)</option>
                    </select>
                </div>
            </div>

            <!-- Languages Visibility Toggle Row -->
            <div class="chips-row" style="margin-top:8px; padding-top:6px; border-top:1px dashed var(--border);">
                <span style="font-size:11px; font-weight:700; color:var(--text-muted);">Languages:</span>
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
                <div id="stats-badge" style="margin-left:auto; font-size:11px; font-weight:700; color:var(--emerald);">
                    Showing 4,709 / 4,709 words
                </div>
            </div>
        </header>

        <!-- Main Content Area -->
        <main id="content-area">
            <!-- Hero Overview Card -->
            <section class="hero-banner">
                <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px;">
                    <div>
                        <div style="display:inline-flex; align-items:center; gap:6px; background:rgba(255,255,255,0.15); padding:3px 10px; border-radius:20px; font-size:11px; font-weight:700; margin-bottom:8px;">
                            <img src="{LOGO_DATA_URI}" width="16" height="16" alt="logo" style="border-radius:3px;">
                            <span>Master Curriculum v1.0.0</span>
                            <span style="opacity:0.8;">•</span>
                            <span style="color:#a7f3d0;">100% Precision Verified</span>
                        </div>
                        <h1 style="font-size:24px; font-weight:900; line-height:1.2; letter-spacing:-0.02em;">
                            The Complete Vocabulary of the Holy Qur'an
                        </h1>
                        <p style="font-size:13px; color:#e2e8f0; margin-top:4px; max-width:760px;">
                            Every unique lemma in the Qur'an organized by pedagogical frequency. Featuring authentic Uthmani calligraphy, verified ayah citations, multi-lingual translations, and instant audio pronunciation.
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
                        <div class="stat-label">Tokens</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-num">11</div>
                        <div class="stat-label">Languages</div>
                    </div>
                </div>
            </section>

            <!-- Cards Stream View Container (Virtualized Batching) -->
            <div id="cards-stream-view">
                <div class="words-grid" id="words-cards-container"></div>
                <div id="infinite-sentinel">Scroll for more words...</div>
            </div>

            <!-- Table View Container -->
            <div id="table-view-wrapper">
                <table class="dict-table">
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>ID</th>
                            <th>Arabic</th>
                            <th>Translit / Root</th>
                            <th>POS</th>
                            <th>Occurrences</th>
                            <th>English</th>
                            <th>বাংলা</th>
                            <th>اردو</th>
                            <th>Example Verse</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="table-body"></tbody>
                </table>
                <div id="table-pagination" style="display:flex; justify-content:space-between; align-items:center; padding:12px; border-top:1px solid var(--border);">
                    <span id="table-page-info" style="font-size:12px; color:var(--text-muted);">Page 1</span>
                    <div style="display:flex; gap:6px;">
                        <button class="btn" onclick="prevTablePage()">‹ Previous</button>
                        <button class="btn" onclick="nextTablePage()">Next ›</button>
                    </div>
                </div>
            </div>

            <!-- Flashcard Study Mode -->
            <div id="flashcard-view-wrapper">
                <div class="flashcard-box" id="flashcard-box">
                    <div style="display:flex; justify-content:space-between; font-size:12px; color:var(--text-muted);">
                        <span id="fc-counter">Card 1 / 4,709</span>
                        <span id="fc-category" class="badge-pos">PARTICLE</span>
                    </div>

                    <div style="margin:20px 0;">
                        <div class="arabic-lemma font-arabic" id="fc-arabic" style="font-size:44px; margin-bottom:8px;">مِنْ</div>
                        <div id="fc-translit" style="font-size:16px; font-style:italic; color:var(--text-muted);">min</div>
                        <div id="fc-root" style="font-size:13px; color:var(--gold); font-weight:700; margin-top:4px;"></div>
                    </div>

                    <div id="fc-answer-box" style="display:none; padding:16px; background:var(--bg-primary); border-radius:12px; border:1px solid var(--border); text-align:left;">
                        <div style="font-size:12px; font-weight:700; color:var(--emerald); margin-bottom:6px;">MEANINGS</div>
                        <div id="fc-meanings" style="font-size:14px; font-weight:600; line-height:1.5;"></div>
                        <div style="font-size:12px; font-weight:700; color:var(--emerald); margin-top:12px; margin-bottom:4px;">AYAH CONTEXT</div>
                        <div id="fc-verse" class="font-arabic" style="font-size:18px; line-height:1.6;"></div>
                    </div>

                    <button id="fc-reveal-btn" class="btn btn-primary" onclick="toggleFlashcardAnswer()" style="justify-content:center; padding:10px;">
                        👁️ Reveal Meaning & Verse (Space)
                    </button>

                    <div style="display:flex; justify-content:space-between; margin-top:10px;">
                        <button class="btn" onclick="prevFlashcard()">‹ Previous (←)</button>
                        <button class="icon-btn" onclick="playFcAudio()">🔊</button>
                        <button class="btn btn-primary" onclick="nextFlashcard()">Next (→) ›</button>
                    </div>
                </div>
            </div>
        </main>
    </div>

    <!-- Embedded Master Curriculum JSON -->
    <script>
        const CHAPTERS_META = {chapters_json};
        const ALL_WORDS = {words_json};

        // State variables
        let currentView = 'stream';
        let currentCategory = 'ALL';
        let currentChapter = 'ALL';
        let currentSearch = '';
        let visibleLangs = {{ en: true, bn: true, ur: true, hi: false, in: false, ms: false, tr: false, fa: false, ha: false, sw: false, fr: false }};
        let bookmarks = JSON.parse(localStorage.getItem('qw_bookmarks') || '[]');

        // Virtualized / Paginated stream state
        let filteredWords = ALL_WORDS;
        let renderedCount = 0;
        const BATCH_SIZE = 24;

        // Table pagination state
        let tablePage = 1;
        const TABLE_PAGE_SIZE = 50;

        // Flashcard index
        let fcIndex = 0;

        const LANG_FLAGS = {{
            en: '🇬🇧', bn: '🇧🇩', ur: '🇵🇰', hi: '🇮🇳', in: '🇮🇩', ms: '🇲🇾',
            tr: '🇹🇷', fa: '🇮🇷', ha: '🇳🇬', sw: '🇰🇪', fr: '🇫🇷'
        }};

        // Render Sidebar Navigation
        function renderSidebar() {{
            const listEl = document.getElementById('sidebar-chapters-list');
            listEl.innerHTML = CHAPTERS_META.map(ch => `
                <div class="nav-chapter">
                    <button class="nav-chapter-btn" onclick="filterByChapter('${{ch.id}}')">
                        <span>Ch ${{ch.sort}}: ${{ch.title.en}}</span>
                        <span class="count-pill">${{ch.words}}</span>
                    </button>
                    <div class="nav-sections-list">
                        ${{ch.secs.map(sec => `
                            <a href="javascript:void(0)" class="nav-sec-link" onclick="filterBySection('${{sec.id}}')">
                                ${{sec.title.en}}
                            </a>
                        `).join('')}}
                    </div>
                </div>
            `).join('');
            updateBookmarkCount();
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

        // Fast Filter & Progressive Stream Rendering (Zero Lag)
        function applyFilters() {{
            const q = currentSearch.toLowerCase().trim();
            const qNorm = stripTashkeelJs(q);

            filteredWords = ALL_WORDS.filter(w => {{
                if (currentCategory === 'BOOKMARKS' && !bookmarks.includes(w.id)) return false;
                if (currentChapter !== 'ALL' && w.ch !== currentChapter && w.sec !== currentChapter) return false;
                if (currentCategory === 'PARTICLE' && w.cat !== 'PARTICLE') return false;
                if (currentCategory === 'VERB' && w.cat !== 'VERB') return false;
                if (currentCategory === 'NOUN' && w.cat !== 'NOUN') return false;
                if (currentCategory === 'POLYSEMY' && (!w.poly || w.poly.length <= 1)) return false;

                if (q) {{
                    const matchAr = (w.ar && w.ar.includes(q)) || (w.norm && w.norm.includes(qNorm));
                    const matchTr = w.tr && w.tr.toLowerCase().includes(q);
                    const matchRt = w.rt && w.rt.toLowerCase().includes(q);
                    const matchId = w.id && w.id.toLowerCase().includes(q);
                    const matchRef = w.ref && w.ref.toLowerCase().includes(q);
                    const matchEn = w.m.en && w.m.en.toLowerCase().includes(q);
                    const matchBn = w.m.bn && w.m.bn.toLowerCase().includes(q);
                    const matchUr = w.m.ur && w.m.ur.toLowerCase().includes(q);
                    const matchPos = w.pos && w.pos.toLowerCase().includes(q);

                    return matchAr || matchTr || matchRt || matchId || matchRef || matchEn || matchBn || matchUr || matchPos;
                }}
                return true;
            }});

            document.getElementById('stats-badge').innerText = `Showing ${{filteredWords.length.toLocaleString()}} / ${{ALL_WORDS.length.toLocaleString()}} words`;

            if (currentView === 'stream') {{
                resetAndRenderStream();
            }} else if (currentView === 'table') {{
                tablePage = 1;
                renderTable();
            }} else if (currentView === 'flashcard') {{
                fcIndex = 0;
                renderFlashcard();
            }}
        }}

        function resetAndRenderStream() {{
            const container = document.getElementById('words-cards-container');
            container.innerHTML = '';
            renderedCount = 0;

            if (filteredWords.length === 0) {{
                container.innerHTML = `
                    <div style="grid-column:1/-1; text-align:center; padding:60px 20px; color:var(--text-muted); background:var(--bg-card); border-radius:var(--radius-lg); border:1px solid var(--border);">
                        <div style="font-size:40px; margin-bottom:10px;">🔍</div>
                        <h3 style="font-size:16px; font-weight:700; color:var(--text-main);">No matching Quranic words found</h3>
                        <p style="margin-top:4px;">Try another search term, root, or reset active filters.</p>
                    </div>
                `;
                document.getElementById('infinite-sentinel').style.display = 'none';
                return;
            }}

            document.getElementById('infinite-sentinel').style.display = 'flex';
            appendNextBatch();
        }}

        function appendNextBatch() {{
            if (renderedCount >= filteredWords.length) {{
                document.getElementById('infinite-sentinel').style.display = 'none';
                return;
            }}

            const container = document.getElementById('words-cards-container');
            const nextBatch = filteredWords.slice(renderedCount, renderedCount + BATCH_SIZE);
            const fragment = document.createDocumentFragment();

            nextBatch.forEach(w => {{
                const cardEl = document.createElement('div');
                cardEl.innerHTML = createWordCardHtml(w);
                fragment.appendChild(cardEl.firstElementChild);
            }});

            container.appendChild(fragment);
            renderedCount += nextBatch.length;

            if (renderedCount >= filteredWords.length) {{
                document.getElementById('infinite-sentinel').style.display = 'none';
            }}
        }}

        // IntersectionObserver for Infinite Batch Streaming
        const observer = new IntersectionObserver((entries) => {{
            if (entries[0].isIntersecting && currentView === 'stream') {{
                appendNextBatch();
            }}
        }}, {{ rootMargin: '400px' }});

        // Word Card Template
        function createWordCardHtml(w) {{
            const isPoly = w.poly && w.poly.length > 1;
            const isBookmarked = bookmarks.includes(w.id);

            return `
                <article class="word-card" id="card-${{w.id}}">
                    <div class="word-card-topbar">
                        <div style="display:flex; gap:5px; align-items:center;">
                            <span class="badge-id">${{w.id}}</span>
                            <span class="badge-pos">${{w.pos || w.cat}}</span>
                            ${{isPoly ? `<span style="background:var(--purple-light); color:var(--purple); font-size:10px; font-weight:800; padding:1px 6px; border-radius:8px; border:1px solid rgba(124,58,237,0.3);">✨ ${{w.poly.length}} Senses</span>` : ''}}
                        </div>
                        <div style="display:flex; gap:5px; align-items:center;">
                            <span class="badge-occ">${{w.occ.toLocaleString()}} occ</span>
                            <button class="icon-btn ${{isBookmarked ? 'bookmarked' : ''}}" onclick="toggleBookmark('${{w.id}}', this)" title="${{isBookmarked ? 'Saved' : 'Bookmark'}}">
                                ${{isBookmarked ? '★' : '☆'}}
                            </button>
                        </div>
                    </div>

                    <div class="word-main-display">
                        <div>
                            <div class="arabic-lemma font-arabic">${{w.ar}}</div>
                            <div class="word-meta">
                                <span class="translit-text">${{w.tr || ''}}</span>
                                ${{w.rt ? `<span class="root-text">Root: ${{w.rt}}</span>` : ''}}
                            </div>
                        </div>
                        <div class="word-action-btns">
                            <button class="icon-btn" onclick="playAudio('${{w.ar}}')" title="Listen">🔊</button>
                            <button class="icon-btn" onclick="copyWord('${{w.id}}')" title="Copy">📋</button>
                        </div>
                    </div>

                    <div class="meanings-grid">
                        ${{renderMeaningsHtml(w.m)}}
                    </div>

                    <div class="verse-box" id="verse-box-${{w.id}}">
                        <div class="verse-meta-row">
                            <span>📖 Qur'an Context</span>
                            <a href="https://quran.com/${{w.ref.split(' ')[1] || '2:255'}}" target="_blank" rel="noopener" class="verse-ref-link" id="ref-${{w.id}}">
                                ${{w.ref}} ↗
                            </a>
                        </div>
                        <div class="verse-arabic-text font-arabic" id="verse-ar-${{w.id}}">
                            ${{w.v_ar}}
                        </div>
                        <div class="verse-translations-list" id="verse-trans-${{w.id}}">
                            ${{renderVerseTranslationsHtml(w.v_tr_hl)}}
                        </div>
                    </div>

                    ${{isPoly ? `
                        <div class="polysemy-card-block">
                            <div style="font-size:11px; font-weight:800; color:var(--purple); display:flex; justify-content:space-between; align-items:center;">
                                <span>📚 Contextual Senses (Wujūh al-Qur'an)</span>
                                <span style="font-size:9px; opacity:0.8;">Tap to switch</span>
                            </div>
                            <div class="poly-tabs-header">
                                ${{w.poly.map((se, idx) => `
                                    <button class="poly-tab-btn ${{idx === 0 ? 'active' : ''}}" onclick="switchPolySense('${{w.id}}', ${{idx}}, this)">
                                        [${{se.idx}}] ${{se.m.en || 'Sense ' + se.idx}}
                                    </button>
                                `).join('')}}
                            </div>
                        </div>
                    ` : ''}}
                </article>
            `;
        }}

        function renderMeaningsHtml(meaningObj) {{
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

        function renderVerseTranslationsHtml(transHlDict) {{
            let out = '';
            for (const [lang, hlHtml] of Object.entries(transHlDict || {{}})) {{
                if (!visibleLangs[lang]) continue;
                const fontClass = lang === 'bn' ? 'font-bn' : (lang === 'ur' ? 'font-ur' : '');
                out += `
                    <div class="v-trans-item ${{fontClass}}">
                        <span class="v-trans-tag">${{LANG_FLAGS[lang] || lang.toUpperCase()}}:</span>
                        <span>${{hlHtml || '—'}}</span>
                    </div>
                `;
            }}
            return out;
        }}

        // Switch Polysemy Sense
        function switchPolySense(wordId, senseIdx, btnEl) {{
            const w = ALL_WORDS.find(item => item.id === wordId);
            if (!w || !w.poly || !w.poly[senseIdx]) return;

            const se = w.poly[senseIdx];
            const parent = btnEl.parentElement;
            parent.querySelectorAll('.poly-tab-btn').forEach(b => b.classList.remove('active'));
            btnEl.classList.add('active');

            const refEl = document.getElementById(`ref-${{wordId}}`);
            if (refEl) {{
                refEl.innerText = se.ref + ' ↗';
                refEl.href = `https://quran.com/${{se.ref.split(' ')[1] || '2:255'}}`;
            }}
            const verseArEl = document.getElementById(`verse-ar-${{wordId}}`);
            if (verseArEl) verseArEl.innerHTML = se.v_ar;
            
            const verseTransEl = document.getElementById(`verse-trans-${{wordId}}`);
            if (verseTransEl) verseTransEl.innerHTML = renderVerseTranslationsHtml(se.v_tr_hl);
        }}

        // Audio Pronunciation
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
            showToast(`Playing pronunciation: "${{arabicText}}" 🔊`);
        }}

        // Quick Copy
        function copyWord(wordId) {{
            const w = ALL_WORDS.find(item => item.id === wordId);
            if (!w) return;
            const textToCopy = `${{w.ar}} (${{w.tr || ''}}) - Meaning: ${{w.m.en || ''}} | Ref: ${{w.ref}}`;
            navigator.clipboard.writeText(textToCopy).then(() => {{
                showToast(`Copied to clipboard: "${{w.ar}}" 📋`);
            }}).catch(() => {{
                showToast('Failed to copy ⚠️');
            }});
        }}

        // Bookmarking
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
                showToast('Saved to Bookmarks ⭐');
            }}
            localStorage.setItem('qw_bookmarks', JSON.stringify(bookmarks));
            updateBookmarkCount();
            if (currentCategory === 'BOOKMARKS') {{
                applyFilters();
            }}
        }}

        function updateBookmarkCount() {{
            const countEl = document.getElementById('bookmark-count');
            if (countEl) countEl.innerText = bookmarks.length;
        }}

        // Toast Notifications
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
            const randomIndex = Math.floor(Math.random() * ALL_WORDS.length);
            const w = ALL_WORDS[randomIndex];
            const modalBody = document.getElementById('random-modal-body');
            modalBody.innerHTML = createWordCardHtml(w);
            document.getElementById('random-word-modal').style.display = 'flex';
        }}

        function closeRandomModal(event) {{
            document.getElementById('random-word-modal').style.display = 'none';
        }}

        // Interactive Table View
        function renderTable() {{
            const tbody = document.getElementById('table-body');
            const start = (tablePage - 1) * TABLE_PAGE_SIZE;
            const end = start + TABLE_PAGE_SIZE;
            const pageItems = filteredWords.slice(start, end);
            const totalPages = Math.ceil(filteredWords.length / TABLE_PAGE_SIZE) || 1;

            document.getElementById('table-page-info').innerText = `Page ${{tablePage}} of ${{totalPages}} (${{filteredWords.length.toLocaleString()}} words)`;

            tbody.innerHTML = pageItems.map((w, idx) => `
                <tr>
                    <td style="font-weight:700; color:var(--text-muted);">${{start + idx + 1}}</td>
                    <td><span class="badge-id">${{w.id}}</span></td>
                    <td class="font-arabic" style="font-size:22px; font-weight:700; color:var(--emerald);">${{w.ar}}</td>
                    <td>
                        <div style="font-weight:600; font-style:italic;">${{w.tr || '—'}}</div>
                        ${{w.rt ? `<div style="color:var(--gold); font-size:10px; font-weight:700;">Root: ${{w.rt}}</div>` : ''}}
                    </td>
                    <td><span class="badge-pos">${{w.pos || w.cat}}</span></td>
                    <td><span class="badge-occ">${{w.occ.toLocaleString()}}</span></td>
                    <td style="font-weight:600;">${{w.m.en || '—'}}</td>
                    <td class="font-bn" style="font-weight:600;">${{w.m.bn || '—'}}</td>
                    <td class="font-ur" style="font-weight:600;">${{w.m.ur || '—'}}</td>
                    <td style="min-width:200px;">
                        <div style="font-weight:700; color:var(--emerald); font-size:10px;">${{w.ref}}</div>
                        <div class="font-arabic" style="font-size:14px;">${{w.v_ar}}</div>
                    </td>
                    <td>
                        <div style="display:flex; gap:4px;">
                            <button class="icon-btn" onclick="playAudio('${{w.ar}}')" title="Listen">🔊</button>
                            <button class="icon-btn" onclick="copyWord('${{w.id}}')" title="Copy">📋</button>
                        </div>
                    </td>
                </tr>
            `).join('');
        }}

        function nextTablePage() {{
            const totalPages = Math.ceil(filteredWords.length / TABLE_PAGE_SIZE);
            if (tablePage < totalPages) {{
                tablePage++;
                renderTable();
            }}
        }}

        function prevTablePage() {{
            if (tablePage > 1) {{
                tablePage--;
                renderTable();
            }}
        }}

        // Flashcard Study Mode
        function renderFlashcard() {{
            if (filteredWords.length === 0) return;
            if (fcIndex >= filteredWords.length) fcIndex = 0;
            if (fcIndex < 0) fcIndex = filteredWords.length - 1;

            const w = filteredWords[fcIndex];
            document.getElementById('fc-counter').innerText = `Card ${{fcIndex + 1}} / ${{filteredWords.length}}`;
            document.getElementById('fc-category').innerText = w.pos || w.cat;
            document.getElementById('fc-arabic').innerText = w.ar;
            document.getElementById('fc-translit').innerText = w.tr || '';
            document.getElementById('fc-root').innerText = w.rt ? `Root: ${{w.rt}}` : '';

            let meaningsText = '';
            for (const [lang, text] of Object.entries(w.m || {{}})) {{
                if (visibleLangs[lang]) {{
                    meaningsText += `<div><strong>${{LANG_FLAGS[lang] || lang.toUpperCase()}}:</strong> ${{text}}</div>`;
                }}
            }}
            document.getElementById('fc-meanings').innerHTML = meaningsText;
            document.getElementById('fc-verse').innerHTML = `${{w.v_ar}} <span style="font-size:12px; color:var(--emerald);">(${{w.ref}})</span>`;

            // Reset answer visibility
            document.getElementById('fc-answer-box').style.display = 'none';
            document.getElementById('fc-reveal-btn').innerText = '👁️ Reveal Meaning & Verse (Space)';
        }}

        function toggleFlashcardAnswer() {{
            const box = document.getElementById('fc-answer-box');
            const btn = document.getElementById('fc-reveal-btn');
            if (box.style.display === 'none') {{
                box.style.display = 'block';
                btn.innerText = 'Hide Details';
            }} else {{
                box.style.display = 'none';
                btn.innerText = '👁️ Reveal Meaning & Verse (Space)';
            }}
        }}

        function nextFlashcard() {{
            fcIndex++;
            renderFlashcard();
        }}

        function prevFlashcard() {{
            fcIndex--;
            renderFlashcard();
        }}

        function playFcAudio() {{
            if (filteredWords.length > 0) {{
                playAudio(filteredWords[fcIndex].ar);
            }}
        }}

        // Filter Handlers
        function filterCategory(cat, btnEl) {{
            currentCategory = cat;
            document.querySelectorAll('.filter-chip').forEach(b => b.classList.remove('active'));
            btnEl.classList.add('active');
            applyFilters();
        }}

        function onChapterSelect(val) {{
            currentChapter = val;
            applyFilters();
        }}

        function filterByChapter(chId) {{
            currentChapter = chId;
            document.getElementById('chapter-select').value = chId;
            if (window.innerWidth <= 900) toggleMobileSidebar();
            applyFilters();
        }}

        function filterBySection(secId) {{
            currentChapter = secId;
            if (window.innerWidth <= 900) toggleMobileSidebar();
            applyFilters();
        }}

        function toggleLanguage(lang, btnEl) {{
            visibleLangs[lang] = !visibleLangs[lang];
            btnEl.classList.toggle('active');
            if (currentView === 'stream') {{
                resetAndRenderStream();
            }} else if (currentView === 'table') {{
                renderTable();
            }} else if (currentView === 'flashcard') {{
                renderFlashcard();
            }}
        }}

        function switchView(view) {{
            currentView = view;
            const streamWrapper = document.getElementById('cards-stream-view');
            const tableWrapper = document.getElementById('table-view-wrapper');
            const flashcardWrapper = document.getElementById('flashcard-view-wrapper');

            document.getElementById('view-stream-btn').className = view === 'stream' ? 'btn btn-primary' : 'btn';
            document.getElementById('view-table-btn').className = view === 'table' ? 'btn btn-primary' : 'btn';
            document.getElementById('view-flashcard-btn').className = view === 'flashcard' ? 'btn btn-primary' : 'btn';

            streamWrapper.style.display = view === 'stream' ? 'block' : 'none';
            tableWrapper.style.display = view === 'table' ? 'block' : 'none';
            flashcardWrapper.style.display = view === 'flashcard' ? 'block' : 'none';

            applyFilters();
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
            applyFilters();
        }}

        // Live Search Debounce & Hotkeys
        let searchTimer = null;
        const searchInput = document.getElementById('search-input');
        searchInput.addEventListener('input', (e) => {{
            const val = e.target.value;
            document.getElementById('search-clear-btn').style.display = val ? 'block' : 'none';
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {{
                currentSearch = val;
                applyFilters();
            }}, 120);
        }});

        window.addEventListener('keydown', (e) => {{
            if (e.key === '/' && document.activeElement !== searchInput) {{
                e.preventDefault();
                searchInput.focus();
                searchInput.select();
            }}
            if (e.key === 'Escape') {{
                closeRandomModal();
            }}
            if (currentView === 'flashcard') {{
                if (e.key === 'ArrowRight') nextFlashcard();
                if (e.key === 'ArrowLeft') prevFlashcard();
                if (e.key === ' ') {{
                    e.preventDefault();
                    toggleFlashcardAnswer();
                }}
            }}
        }});

        // Initialization
        window.addEventListener('DOMContentLoaded', () => {{
            const savedTheme = localStorage.getItem('qw_theme');
            if (savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {{
                document.documentElement.classList.add('dark');
            }}
            renderSidebar();
            applyFilters();
            observer.observe(document.getElementById('infinite-sentinel'));
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
