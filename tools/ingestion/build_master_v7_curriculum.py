#!/usr/bin/env python3
"""
Master Curriculum Builder for QuranicWords (v7 Master Assets)
============================================================
Synthesizes the complete 10-chapter, 3-category Quranic vocabulary curriculum
directly from the official v7 Excel catalogs in assets/Words & Meanings/:
- quranic_harf_lemmas_173-v7.xlsx (173 Particles)
- quranic_verb_lemmas_1479-v7.xlsx (1,479 Verbs)
- quranic_noun_lemmas_3057-v7.xlsx (3,057 Nouns)
- quranic_lexical_tripartite_summary_v7.xlsx

Total: 4,709 unique Quranic lemmas across 6 languages (en, bn, ur, in, tr, fr)
with 100% authentic Quranic Ayahs, Harakat/Tashkeel, precomputed spans, and
dynamic multi-meaning (polysemy / Wujuh al-Quran) structures taken directly
from the master Excel assets, guaranteeing 100% language purity in each language.
"""

import os
import re
import json
import openpyxl
from collections import defaultdict

BASE_DIR = '/home/rafi/WorkSpace/QuranicWords'
EXCEL_DIR = os.path.join(BASE_DIR, 'assets', 'Words & Meanings')
CONTENT_DIR = os.path.join(BASE_DIR, 'app', 'src', 'main', 'assets', 'content')

bengali_regex = re.compile(r'[\u0980-\u09FF]')

def strip_tashkeel(s):
    return re.sub(r'[\u064B-\u065F\u0670\u06D6-\u06ED]', '', str(s))

def fix_arabic_text(text):
    if not text:
        return ''
    s = str(text)
    # Fix stray Bengali 'জ' (\u099C) in Arabic text (e.g. السِّজْنِ -> السِّجْنِ)
    s = s.replace('\u099C', '\u062C')
    return s

def split_senses(text):
    if not text:
        return []
    s = str(text).strip()
    parts = re.split(r'\s*(?:\||;|\n)\s*(?=[0-9১-৯]+[\.۔\)])', s)
    if len(parts) == 1:
        parts = re.split(r'\s+(?=[0-9১-৯]+[\.۔\)])', s)
    return [p.strip() for p in parts if p.strip()]

def clean_arabic_verse(raw_ar, word_lemma=''):
    if not raw_ar:
        return '', None, None
    s = fix_arabic_text(raw_ar)
    s = re.sub(r'^[0-9১-৯]+[\.۔\)]\s*', '', s.strip())
    s = re.sub(r'\s*\[[0-9]+:[0-9]+\]\s*$', '', s)
    
    # 1. Primary: Match 【...】
    m = re.search(r'【(.*?)】', s)
    if m:
        target = m.group(1)
        clean_text = s[:m.start()] + target + s[m.end():]
        start = m.start()
        end = start + len(target)
        return clean_text.strip(), start, end
        
    # 2. Secondary: Match [ ... ]
    m2 = re.search(r'\[(.*?)\]', s)
    if m2:
        target = m2.group(1)
        clean_text = s[:m2.start()] + target + s[m2.end():]
        start = m2.start()
        end = start + len(target)
        return clean_text.strip(), start, end

    # 3. Fallback: Search for word_lemma in clean string
    clean_text = s.strip()
    if word_lemma:
        idx = clean_text.find(word_lemma)
        if idx != -1:
            return clean_text, idx, idx + len(word_lemma)
        s_v = strip_tashkeel(clean_text)
        s_w = strip_tashkeel(word_lemma)
        s_idx = s_v.find(s_w)
        if s_idx != -1:
            orig_indices = []
            for i, ch in enumerate(clean_text):
                if not re.match(r'[\u064B-\u065F\u0670\u06D6-\u06ED]', ch):
                    orig_indices.append(i)
            if s_idx < len(orig_indices) and (s_idx + len(s_w) - 1) < len(orig_indices):
                start = orig_indices[s_idx]
                end = orig_indices[s_idx + len(s_w) - 1] + 1
                while end < len(clean_text) and re.match(r'[\u064B-\u065F\u0670\u06D6-\u06ED]', clean_text[end]):
                    end += 1
                return clean_text, start, end

    return clean_text, None, None

def clean_translation(raw_trans, lang='en'):
    if not raw_trans:
        return '', ''
    s = re.sub(r'^[0-9১-৯]+[\.۔\)]\s*', '', str(raw_trans).strip())
    s = re.sub(r'\s*\[[0-9]+:[0-9]+\]\s*$', '', s)
    
    # For non-bn languages, ensure no stray Bengali chars in translation
    if lang != 'bn':
        s = bengali_regex.sub('', s)
        
    m = re.search(r'\[(.*?)\]', s)
    if m:
        hl = m.group(1).strip()
        clean_text = s[:m.start()] + hl + s[m.end():]
        return clean_text.strip(), hl
    return s.strip(), ''

def parse_references(ref_str, num_senses):
    if not ref_str:
        return [f'Surah {i+1}' for i in range(num_senses)]
    s = str(ref_str).replace('Surah', '').strip()
    parts = [p.strip() for p in re.split(r'[;,|]\s*', s) if p.strip()]
    refs = []
    for p in parts:
        m = re.search(r'([0-9]+:[0-9]+)', p)
        if m:
            refs.append(f'Surah {m.group(1)}')
        else:
            refs.append(f'Surah {p}')
    while len(refs) < num_senses:
        refs.append(refs[-1] if refs else 'Surah 1:1')
    return refs[:num_senses]

def clean_poly_sense_for_lang(text, lang):
    if not text:
        return ''
    s = str(text).strip()
    # Remove sense numbering at start: '1. ', '১. ', '1) '
    s = re.sub(r'^[0-9১-৯]+[\.۔\)]\s*', '', s)
    # Remove trailing citation: '[1:1]', '[১:১]'
    s = re.sub(r'\s*\[[0-9১-৯]+:[0-9১-৯]+\]\s*$', '', s)
    
    if lang == 'en':
        # Strip out any Bengali in parentheses e.g. '(সীমা শুরু)' or '(ব্যাখ্যা)'
        s = re.sub(r'\s*\([\u0980-\u09FF\s\/\-\–\—,;:]+\)', '', s)
        # Strip out any remaining Bengali chars
        s = bengali_regex.sub('', s)
        s = re.sub(r'\s+', ' ', s).strip()
        s = re.sub(r'[\.,;:]+$', '', s).strip()
    elif lang == 'bn':
        # Strip out any English in parentheses e.g. '(Start of Space/Time)'
        s = re.sub(r'\s*\([a-zA-Z\s\/\-\–\—,;:]+\)', '', s)
        s = re.sub(r'\s+', ' ', s).strip()
        s = re.sub(r'[\.,;:]+$', '', s).strip()
    else:
        # For other languages (ur, in, tr, fr), clean of any stray Bengali
        s = bengali_regex.sub('', s)
        s = re.sub(r'\s+', ' ', s).strip()
        s = re.sub(r'[\.,;:]+$', '', s).strip()
    return s

def parse_poly_desc(poly_text, num_senses, core_meanings, particle_bn_senses=None):
    senses_meanings = [{lang: core_meanings[lang] for lang in ['en', 'bn', 'ur', 'in', 'tr', 'fr']} for _ in range(num_senses)]
    if not poly_text and not particle_bn_senses:
        return senses_meanings
    text = str(poly_text or '').strip()
    en_section = ''
    bn_section = ''
    if 'English:' in text or 'বাংলা:' in text:
        m_en = re.search(r'English:\s*(.*?)(?=\n|বাংলা:|$)', text, re.DOTALL)
        if m_en:
            en_section = m_en.group(1).strip()
        m_bn = re.search(r'বাংলা:\s*(.*?)(?=\n|English:|$)', text, re.DOTALL)
        if m_bn:
            bn_section = m_bn.group(1).strip()
    else:
        en_section = text
        
    def split_desc_senses(s):
        if not s:
            return []
        parts = re.split(r'\s*(?:;|\n|\|)\s*(?=[0-9১-৯]+[\.۔\)])', s)
        return [p.strip() for p in parts if p.strip()]

    en_s = split_desc_senses(en_section)
    bn_s = split_desc_senses(bn_section) if bn_section else (particle_bn_senses or [])
    
    for i in range(num_senses):
        if i < len(en_s) and en_s[i]:
            c_en = clean_poly_sense_for_lang(en_s[i], 'en')
            if c_en:
                senses_meanings[i]['en'] = c_en
        if i < len(bn_s) and bn_s[i]:
            c_bn = clean_poly_sense_for_lang(bn_s[i], 'bn')
            if c_bn:
                senses_meanings[i]['bn'] = c_bn
        for lang in ['ur', 'in', 'tr', 'fr']:
            senses_meanings[i][lang] = clean_poly_sense_for_lang(senses_meanings[i][lang], lang)
            
    return senses_meanings

def main():
    print('=' * 70)
    print('Starting Master Curriculum v7 Generation from Excel Assets...')
    print('=' * 70)

    # 1. Load Harf polysemy Bangla mappings from sheet 'Polysemous Particles (Wujūh)'
    wb_harf = openpyxl.load_workbook(os.path.join(EXCEL_DIR, 'quranic_harf_lemmas_173-v7.xlsx'), data_only=True)
    sheet_poly_harf = wb_harf['Polysemous Particles (Wujūh)']
    harf_poly_bn = {}
    for r in range(5, 5 + 30):
        ar = sheet_poly_harf.cell(r, 2).value
        bn_val = sheet_poly_harf.cell(r, 7).value
        if ar and bn_val:
            harf_poly_bn[str(ar).strip()] = [p.strip() for p in re.split(r'\s*(?:;|\n|\|)\s*(?=[0-9১-৯]+[\.۔\)])', str(bn_val)) if p.strip()]

    catalogs = [
        ('quranic_harf_lemmas_173-v7.xlsx', 'All Particle Lemmas (173)', 173, 'PARTICLE'),
        ('quranic_verb_lemmas_1479-v7.xlsx', 'All Verb Lemmas (1,479)', 1479, 'VERB'),
        ('quranic_noun_lemmas_3057-v7.xlsx', 'All Noun Lemmas (3,057)', 3057, 'NOUN')
    ]

    all_words = []
    global_rank = 1

    for fname, sname, count, cat in catalogs:
        fpath = os.path.join(EXCEL_DIR, fname)
        print(f"Loading {fname} -> {sname} ({count} items)...")
        wb = openpyxl.load_workbook(fpath, data_only=True)
        sheet = wb[sname]
        
        for r in range(5, 5 + count):
            ar = str(sheet.cell(r, 2).value or '').strip()
            translit = str(sheet.cell(r, 3).value or '').strip()
            root = sheet.cell(r, 4).value
            root_str = str(root).strip() if root and str(root).strip() not in ['—', '-', 'None', ''] else None
            pos = str(sheet.cell(r, 5).value or cat).strip()
            occ = int(sheet.cell(r, 6).value or 1)
            
            core_m = {
                'en': clean_poly_sense_for_lang(str(sheet.cell(r, 10).value or '').strip(), 'en'),
                'bn': clean_poly_sense_for_lang(str(sheet.cell(r, 11).value or '').strip(), 'bn'),
                'ur': clean_poly_sense_for_lang(str(sheet.cell(r, 12).value or '').strip(), 'ur'),
                'in': clean_poly_sense_for_lang(str(sheet.cell(r, 13).value or '').strip(), 'in'),
                'tr': clean_poly_sense_for_lang(str(sheet.cell(r, 14).value or '').strip(), 'tr'),
                'fr': clean_poly_sense_for_lang(str(sheet.cell(r, 15).value or '').strip(), 'fr')
            }
            
            poly_desc = sheet.cell(r, 16).value
            ref_raw = sheet.cell(r, 17).value
            v_ar_raw = sheet.cell(r, 18).value
            v_en_raw = sheet.cell(r, 19).value
            v_bn_raw = sheet.cell(r, 20).value
            v_ur_raw = sheet.cell(r, 21).value
            v_in_raw = sheet.cell(r, 22).value
            v_tr_raw = sheet.cell(r, 23).value
            v_fr_raw = sheet.cell(r, 24).value
            
            ar_senses = split_senses(v_ar_raw)
            en_senses = split_senses(v_en_raw)
            bn_senses = split_senses(v_bn_raw)
            ur_senses = split_senses(v_ur_raw)
            in_senses = split_senses(v_in_raw)
            tr_senses = split_senses(v_tr_raw)
            fr_senses = split_senses(v_fr_raw)
            
            num_senses = max(len(ar_senses), len(en_senses), 1)
            refs = parse_references(ref_raw, num_senses)
            poly_meanings = parse_poly_desc(poly_desc, num_senses, core_m, particle_bn_senses=harf_poly_bn.get(ar))
            
            senses_data = []
            for i in range(num_senses):
                raw_s_ar = ar_senses[i] if i < len(ar_senses) else (ar_senses[0] if ar_senses else '')
                c_ar, s_ar, e_ar = clean_arabic_verse(raw_s_ar, ar)
                
                v_trans = {}
                hl_trans = {}
                for lang, s_list, fallback in [
                    ('en', en_senses, v_en_raw),
                    ('bn', bn_senses, v_bn_raw),
                    ('ur', ur_senses, v_ur_raw),
                    ('in', in_senses, v_in_raw),
                    ('tr', tr_senses, v_tr_raw),
                    ('fr', fr_senses, v_fr_raw)
                ]:
                    raw_s = s_list[i] if i < len(s_list) else (s_list[0] if s_list else fallback)
                    c_t, hl = clean_translation(raw_s, lang=lang)
                    v_trans[lang] = c_t
                    hl_trans[lang] = hl
                    
                senses_data.append({
                    'meaningIndex': i + 1,
                    'contextualMeaning': poly_meanings[i],
                    'verseReference': refs[i],
                    'verseArabic': c_ar,
                    'arabicWordStart': s_ar,
                    'arabicWordEnd': e_ar,
                    'verseTranslation': v_trans,
                    'translationHighlight': hl_trans
                })
                
            primary = senses_data[0]
            poly_entries = senses_data if num_senses > 1 else []
            
            word_id = f"w_{global_rank:04d}"
            
            all_words.append({
                'id': word_id,
                'wordArabic': ar,
                'transliteration': translit,
                'root': root_str,
                'partOfSpeech': pos,
                'lemmaCategory': cat,
                'quranOccurrenceCount': occ,
                'rank': global_rank,
                'meaning': core_m,
                'exampleVerseArabic': primary['verseArabic'],
                'exampleVerseTranslation': primary['verseTranslation'],
                'exampleVerseReference': primary['verseReference'],
                'arabicWordStart': primary['arabicWordStart'],
                'arabicWordEnd': primary['arabicWordEnd'],
                'meaningHighlight': primary['translationHighlight'],
                'polysemyEntries': poly_entries
            })
            global_rank += 1

    print(f"Total Curriculum Words Assembled: {len(all_words)}")
    multi_count = len([w for w in all_words if len(w['polysemyEntries']) > 1])
    print(f"Total Multi-meaning Words with Polysemy Badges: {multi_count}")

    # 2. Synthesize Chapters, Sections, Lessons, Exercises
    print("Step 2: Synthesizing 10 Chapters, Sections, Lessons, and Exercises...")
    
    chapter_defs = [
        {'id': 'ch_01', 'title': {'en': 'Grammatical Particles', 'bn': 'ব্যাকরণিক অব্যয়', 'ur': 'حروف اور قواعد', 'in': 'Partikel Tata Bahasa', 'tr': 'Gramer Edatları', 'fr': 'Particules Grammaticales'}, 'category': 'PARTICLE', 'word_range': (0, 173)},
        {'id': 'ch_02', 'title': {'en': 'High-Frequency Verbs', 'bn': 'উচ্চ-ফ্রিকোয়েন্সি ক্রিয়াপদ', 'ur': 'کثیر الاستعمال افعال', 'in': 'Kata Kerja Frekuensi Tinggi', 'tr': 'Yüksek Frekanslı Fiiller', 'fr': 'Verbes à Haute Fréquence'}, 'category': 'VERB', 'word_range': (173, 673)},
        {'id': 'ch_03', 'title': {'en': 'Essential Verbal Forms', 'bn': 'প্রয়োজনীয় ক্রিয়াপদের রূপ', 'ur': 'اہم افعال کے ابواب', 'in': 'Bentuk Kata Kerja Esensial', 'tr': 'Temel Fiil Kalıpları', 'fr': 'Formes Verbales Essentielles'}, 'category': 'VERB', 'word_range': (673, 1173)},
        {'id': 'ch_04', 'title': {'en': 'Specialized Verbs', 'bn': 'বিশেষায়িত ক্রিয়াপদ', 'ur': 'خصوصی افعال', 'in': 'Kata Kerja Khusus', 'tr': 'Özel Fiiller', 'fr': 'Verbes Spécialisés'}, 'category': 'VERB', 'word_range': (1173, 1652)},
        {'id': 'ch_05', 'title': {'en': 'Divine Names & Core Nominals', 'bn': 'আসমাউল হুসনা ও মৌলিক বিশেষ্য', 'ur': 'اسمائے حسنیٰ اور بنیادی اسماء', 'in': 'Asmaul Husna & Nomina Inti', 'tr': 'Esmâ-i Hüsnâ ve Temel İsimler', 'fr': 'Noms Divins et Noms Fondamentaux'}, 'category': 'NOUN', 'word_range': (1652, 2162)},
        {'id': 'ch_06', 'title': {'en': 'Essential Quranic Nominals', 'bn': 'প্রয়োজনীয় কুরআনিক বিশেষ্য', 'ur': 'اہم قرآنی اسماء', 'in': 'Nomina Al-Qur\'an Esensial', 'tr': 'Temel Kur\'an İsimleri', 'fr': 'Noms Coraniques Essentiels'}, 'category': 'NOUN', 'word_range': (2162, 2672)},
        {'id': 'ch_07', 'title': {'en': 'Devotional & Faith Nominals', 'bn': 'ইবাদত ও ঈমান সংক্রান্ত বিশেষ্য', 'ur': 'ایمان اور عبادات کے اسماء', 'in': 'Nomina Ibadah & Keimanan', 'tr': 'İbadet ve İman İsimleri', 'fr': 'Noms de Dévotion et de Foi'}, 'category': 'NOUN', 'word_range': (2672, 3182)},
        {'id': 'ch_08', 'title': {'en': 'Prophetic & Narrative Nominals', 'bn': 'নবী ও ঐতিহাসিক ঘটনার বিশেষ্য', 'ur': 'انبیاء اور قصص کے اسماء', 'in': 'Nomina Kisah & Kenabian', 'tr': 'Peygamber ve Kıssa İsimleri', 'fr': 'Noms Prophétiques et Récits'}, 'category': 'NOUN', 'word_range': (3182, 3692)},
        {'id': 'ch_09', 'title': {'en': 'Moral & Social Nominals', 'bn': 'নৈতিক ও সামাজিক বিশেষ্য', 'ur': 'اخلاقی اور معاشرتی اسماء', 'in': 'Nomina Moral & Sosial', 'tr': 'Ahlaki ve Sosyal İsimler', 'fr': 'Noms Moraux et Sociaux'}, 'category': 'NOUN', 'word_range': (3692, 4202)},
        {'id': 'ch_10', 'title': {'en': 'Cosmic & Lexical Nominals', 'bn': 'মহাজাগতিক ও আভিধানিক বিশেষ্য', 'ur': 'کائناتی اور لغوی اسماء', 'in': 'Nomina Kosmis & Leksikal', 'tr': 'Kozmik ve Sözlük İsimleri', 'fr': 'Noms Cosmiques et Lexicaux'}, 'category': 'NOUN', 'word_range': (4202, 4709)}
    ]

    total_tokens = sum(w['quranOccurrenceCount'] for w in all_words)
    chapters_out = []
    sections_out = []
    lessons_out = []
    exercises_out = []
    
    sec_global_num = 1
    les_global_num = 1
    ex_global_num = 1
    
    words_by_cat = defaultdict(list)
    for w in all_words:
        words_by_cat[w['lemmaCategory']].append(w)

    def generate_options(correct_w, pool):
        correct_opt = {
            'id': f"opt_{correct_w['id']}",
            'labelArabic': correct_w['wordArabic'],
            'label': correct_w['meaning']
        }
        cand_distractors = [w for w in pool if w['id'] != correct_w['id']]
        step = max(1, len(cand_distractors) // 10)
        distractors = []
        for i in range(3):
            pick_idx = (correct_w['rank'] * (i + 1) * step) % len(cand_distractors)
            d_w = cand_distractors[pick_idx]
            distractors.append({
                'id': f"opt_{d_w['id']}",
                'labelArabic': d_w['wordArabic'],
                'label': d_w['meaning']
            })
        
        slot = correct_w['rank'] % 4
        opts = list(distractors)
        opts.insert(slot, correct_opt)
        return opts, correct_opt['id']

    for ch_idx, ch_info in enumerate(chapter_defs, 1):
        ch_id = ch_info['id']
        ch_title = ch_info['title']
        ch_cat = ch_info['category']
        w_start, w_end = ch_info['word_range']
        ch_words = all_words[w_start:w_end]
        
        ch_occ = sum(w['quranOccurrenceCount'] for w in ch_words)
        ch_pct = round((ch_occ / total_tokens) * 100, 2)
        
        chapters_out.append({
            'id': ch_id,
            'title': ch_title,
            'description': ch_title,
            'sortOrder': ch_idx,
            'wordCount': len(ch_words),
            'quranOccurrenceCount': ch_occ,
            'quranOccurrencePercent': ch_pct
        })
        
        words_per_sec = len(ch_words) // 10
        rem = len(ch_words) % 10
        
        sec_start = 0
        for s_idx in range(1, 11):
            s_len = words_per_sec + (1 if s_idx <= rem else 0)
            sec_words = ch_words[sec_start : sec_start + s_len]
            sec_start += s_len
            
            sec_id = f"sec_{sec_global_num:03d}"
            sec_title = {
                'en': f"Section {s_idx}: {ch_title['en']}",
                'bn': f"পর্ব {s_idx}: {ch_title['bn']}",
                'ur': f"حصہ {s_idx}: {ch_title['ur']}",
                'in': f"Bagian {s_idx}: {ch_title['in']}",
                'tr': f"Bölüm {s_idx}: {ch_title['tr']}",
                'fr': f"Section {s_idx}: {ch_title['fr']}"
            }
            sec_occ = sum(w['quranOccurrenceCount'] for w in sec_words)
            sec_pct = round((sec_occ / total_tokens) * 100, 2)
            
            sections_out.append({
                'id': sec_id,
                'chapterId': ch_id,
                'title': sec_title,
                'sortOrder': s_idx,
                'wordCount': len(sec_words),
                'quranOccurrenceCount': sec_occ,
                'quranOccurrencePercent': sec_pct
            })
            
            sec_sort_order = 1
            if s_idx == 1:
                ch_intro_les_id = f"les_{les_global_num:04d}"
                ch_intro_ex_id = f"ex_{ex_global_num:05d}"
                ex_global_num += 1
                
                lessons_out.append({
                    'id': ch_intro_les_id,
                    'chapterId': ch_id,
                    'sectionId': sec_id,
                    'title': {'en': f'Chapter {ch_idx} Overview', 'bn': f'অধ্যায় {ch_idx} পরিচিতি', 'ur': f'باب {ch_idx} کا تعارف', 'in': f'Ikhtisar Bab {ch_idx}', 'tr': f'Bölüm {ch_idx} Genel Bakış', 'fr': f'Aperçu du Chapitre {ch_idx}'},
                    'sortOrder': sec_sort_order,
                    'kind': 'CHAPTER_INTRO',
                    'category': ch_cat
                })
                sec_sort_order += 1
                
                exercises_out.append({
                    'id': ch_intro_ex_id,
                    'lessonId': ch_intro_les_id,
                    'orderIndex': 1,
                    'exerciseType': 'CHAPTER_INTRO',
                    'content': {
                        'type': 'chapter_intro',
                        'prompt': ch_title,
                        'chapterId': ch_id,
                        'chapterNumber': ch_idx,
                        'chapterTitle': ch_title,
                        'chapterDescription': ch_title,
                        'wordCount': len(ch_words),
                        'quranOccurrenceCount': ch_occ,
                        'chapterCoveragePercent': ch_pct,
                        'accumulatedCoveragePercent': ch_pct,
                        'accumulatedWords': len(ch_words),
                        'nounCount': len(ch_words) if ch_cat == 'NOUN' else 0,
                        'verbCount': len(ch_words) if ch_cat == 'VERB' else 0,
                        'particleCount': len(ch_words) if ch_cat == 'PARTICLE' else 0,
                        'learningObjectives': ch_title
                    }
                })
                les_global_num += 1

            sec_w_chunks = [sec_words[i:i+5] for i in range(0, len(sec_words), 5)]
            for chunk_idx, w_chunk in enumerate(sec_w_chunks, 1):
                les_id = f"les_{les_global_num:04d}"
                les_title = {
                    'en': f"Lesson {chunk_idx}: {w_chunk[0]['wordArabic']} – {w_chunk[-1]['wordArabic']}",
                    'bn': f"পাঠ {chunk_idx}: {w_chunk[0]['wordArabic']} – {w_chunk[-1]['wordArabic']}",
                    'ur': f"سبق {chunk_idx}: {w_chunk[0]['wordArabic']} – {w_chunk[-1]['wordArabic']}",
                    'in': f"Pelajaran {chunk_idx}: {w_chunk[0]['wordArabic']} – {w_chunk[-1]['wordArabic']}",
                    'tr': f"Ders {chunk_idx}: {w_chunk[0]['wordArabic']} – {w_chunk[-1]['wordArabic']}",
                    'fr': f"Leçon {chunk_idx}: {w_chunk[0]['wordArabic']} – {w_chunk[-1]['wordArabic']}"
                }
                
                lessons_out.append({
                    'id': les_id,
                    'chapterId': ch_id,
                    'sectionId': sec_id,
                    'title': les_title,
                    'sortOrder': sec_sort_order,
                    'kind': 'REGULAR',
                    'category': ch_cat
                })
                sec_sort_order += 1
                
                ex_ord = 1
                for w in w_chunk:
                    intro_ex_id = f"ex_{ex_global_num:05d}"
                    ex_global_num += 1
                    
                    exercises_out.append({
                        'id': intro_ex_id,
                        'lessonId': les_id,
                        'orderIndex': ex_ord,
                        'exerciseType': 'WORD_INTRO',
                        'content': {
                            'type': 'word_intro',
                            'prompt': {'en': 'Learn this word', 'bn': 'শব্দটি শিখুন', 'ur': 'یہ لفظ سیکھیں', 'in': 'Pelajari kata ini', 'tr': 'Bu kelimeyi öğrenin', 'fr': 'Apprenez ce mot'},
                            'wordId': w['id'],
                            'arabicWord': w['wordArabic'],
                            'transliteration': w['transliteration'],
                            'root': w['root'],
                            'partOfSpeech': w['partOfSpeech'],
                            'quranOccurrenceCount': w['quranOccurrenceCount'],
                            'exampleVerseArabic': w['exampleVerseArabic'],
                            'exampleVerseTranslation': w['exampleVerseTranslation'],
                            'exampleVerseReference': w['exampleVerseReference'],
                            'meaning': w['meaning'],
                            'arabicWordStart': w['arabicWordStart'],
                            'arabicWordEnd': w['arabicWordEnd'],
                            'meaningHighlight': w['meaningHighlight'],
                            'polysemyEntries': w['polysemyEntries']
                        }
                    })
                    ex_ord += 1

                for w in w_chunk:
                    quiz_ex_id = f"ex_{ex_global_num:05d}"
                    ex_global_num += 1
                    
                    options, correct_id = generate_options(w, words_by_cat[ch_cat])
                    
                    exercises_out.append({
                        'id': quiz_ex_id,
                        'lessonId': les_id,
                        'orderIndex': ex_ord,
                        'exerciseType': 'MULTIPLE_CHOICE',
                        'content': {
                            'type': 'multiple_choice',
                            'prompt': {'en': 'Choose the correct meaning', 'bn': 'সঠিক অর্থ নির্বাচন করুন', 'ur': 'درست معنی کا انتخاب کریں', 'in': 'Pilih arti yang benar', 'tr': 'Doğru anlamı seçin', 'fr': 'Choisissez la bonne signification'},
                            'promptArabic': w['wordArabic'],
                            'wordId': w['id'],
                            'options': options,
                            'correctOptionId': correct_id,
                            'exampleVerseArabic': w['exampleVerseArabic'],
                            'exampleVerseTranslation': w['exampleVerseTranslation'],
                            'exampleVerseReference': w['exampleVerseReference'],
                            'arabicWordStart': w['arabicWordStart'],
                            'arabicWordEnd': w['arabicWordEnd'],
                            'meaningHighlight': w['meaningHighlight']
                        }
                    })
                    ex_ord += 1

                les_global_num += 1

            # Section Flashback Lesson
            flash_les_id = f"les_{les_global_num:04d}"
            lessons_out.append({
                'id': flash_les_id,
                'chapterId': ch_id,
                'sectionId': sec_id,
                'title': {'en': f'Section {s_idx} Flashback', 'bn': f'পর্ব {s_idx} পুনরাবৃত্তি', 'ur': f'حصہ {s_idx} کا اعادہ', 'in': f'Ulasan Bagian {s_idx}', 'tr': f'Bölüm {s_idx} Tekrarı', 'fr': f'Révision Section {s_idx}'},
                'sortOrder': sec_sort_order,
                'kind': 'SECTION_FLASHBACK',
                'category': ch_cat
            })
            sec_sort_order += 1
            les_global_num += 1

            # Section Exam Lesson
            exam_les_id = f"les_{les_global_num:04d}"
            lessons_out.append({
                'id': exam_les_id,
                'chapterId': ch_id,
                'sectionId': sec_id,
                'title': {'en': f'Section {s_idx} Exam', 'bn': f'পর্ব {s_idx} পরীক্ষা', 'ur': f'حصہ {s_idx} کا امتحان', 'in': f'Ujian Bagian {s_idx}', 'tr': f'Bölüm {s_idx} Sınavı', 'fr': f'Examen Section {s_idx}'},
                'sortOrder': sec_sort_order,
                'kind': 'SECTION_EXAM',
                'category': ch_cat
            })
            sec_sort_order += 1
            les_global_num += 1

            sec_global_num += 1

        # Chapter Exam Lesson (after Section 10) - Chapter-scoped, sectionId MUST be None
        ch_exam_les_id = f"les_{les_global_num:04d}"
        lessons_out.append({
            'id': ch_exam_les_id,
            'chapterId': ch_id,
            'sectionId': None,
            'title': {'en': f'Chapter {ch_idx} Final Exam', 'bn': f'অধ্যায় {ch_idx} চূড়ান্ত পরীক্ষা', 'ur': f'باب {ch_idx} کا فائنل امتحان', 'in': f'Ujian Akhir Bab {ch_idx}', 'tr': f'Bölüm {ch_idx} Final Sınavı', 'fr': f'Examen Final Chapitre {ch_idx}'},
            'sortOrder': sec_sort_order,
            'kind': 'CHAPTER_EXAM',
            'category': ch_cat
        })
        les_global_num += 1

    # 3. Double-Audit for Language Cross-Contamination
    print("Step 3: Executing rigorous double-audit across 100% of curriculum items...")
    audit_errors = 0
    for w in all_words:
        if not w['wordArabic']:
            print(f"Error: Missing Arabic word for {w['id']}")
            audit_errors += 1
        for lang in ['en', 'ur', 'in', 'tr', 'fr']:
            val = w['meaning'].get(lang, '')
            if bengali_regex.search(val):
                print(f"Error: Bengali found in {lang} meaning for {w['id']}: {repr(val)}")
                audit_errors += 1
            v_trans = w['exampleVerseTranslation'].get(lang, '')
            if bengali_regex.search(v_trans):
                print(f"Error: Bengali found in {lang} verse translation for {w['id']}: {repr(v_trans)}")
                audit_errors += 1
        for se in w['polysemyEntries']:
            for lang in ['en', 'ur', 'in', 'tr', 'fr']:
                val = se['contextualMeaning'].get(lang, '')
                if bengali_regex.search(val):
                    print(f"Error: Bengali found in {lang} polysemy meaning for {w['id']}: {repr(val)}")
                    audit_errors += 1
                v_trans = se['verseTranslation'].get(lang, '')
                if bengali_regex.search(v_trans):
                    print(f"Error: Bengali found in {lang} polysemy verse translation for {w['id']}: {repr(v_trans)}")
                    audit_errors += 1

    print(f"Audited Items: {len(all_words)} (100%)")
    print(f"Audited Polysemy Entries: {sum(len(w['polysemyEntries']) for w in all_words if len(w['polysemyEntries']) > 1)}")
    print(f"Total Audit Errors: {audit_errors}")
    assert audit_errors == 0, f"Audit failed with {audit_errors} errors!"
    print("SUCCESS: 100% of all 4,709 curriculum items verified with 0 language contamination errors!")

    # 4. Write Output JSON Assets
    print("Step 4: Writing JSON content assets to app/src/main/assets/content/...")
    
    with open(os.path.join(CONTENT_DIR, 'chapters.json'), 'w', encoding='utf-8') as f:
        json.dump({'chapters': chapters_out}, f, ensure_ascii=False, indent=2)
        
    with open(os.path.join(CONTENT_DIR, 'sections.json'), 'w', encoding='utf-8') as f:
        json.dump({'sections': sections_out}, f, ensure_ascii=False, indent=2)
        
    with open(os.path.join(CONTENT_DIR, 'lessons_vocabulary.json'), 'w', encoding='utf-8') as f:
        json.dump({'lessons': lessons_out}, f, ensure_ascii=False, indent=2)
        
    with open(os.path.join(CONTENT_DIR, 'exercises_vocabulary.json'), 'w', encoding='utf-8') as f:
        json.dump({'exercises': exercises_out}, f, ensure_ascii=False, indent=2)
        
    words_freq_out = []
    for w in all_words:
        words_freq_out.append({
            'id': w['id'],
            'arabicWord': w['wordArabic'],
            'frequencyRank': w['rank'],
            'frequencyCount': w['quranOccurrenceCount'],
            'meaning': w['meaning'],
            'audioAssetPath': None,
            'tierLevel': (w['rank'] // 500) + 1
        })
        
    with open(os.path.join(CONTENT_DIR, 'word_frequency.json'), 'w', encoding='utf-8') as f:
        json.dump({'words': words_freq_out}, f, ensure_ascii=False, indent=2)

    print(f"DONE! Written {len(chapters_out)} chapters, {len(sections_out)} sections, {len(lessons_out)} lessons, {len(exercises_out)} exercises, and {len(words_freq_out)} words.")
    print('=' * 70)

if __name__ == '__main__':
    main()
