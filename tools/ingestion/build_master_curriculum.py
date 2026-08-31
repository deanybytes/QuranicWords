#!/usr/bin/env python3
"""
Master Curriculum Ingestion & Generation Engine
================================================
Ingests 3,057 Noun Lemmas and 1,479 Verb Lemmas from assets/Words & Meanings/,
extracts polysemy (Wujūh al-Qur'an), calculates exact character spans from 【...】
and [...] brackets, and builds the full multi-tier curriculum:
  - 10 Chapters
  - 100 Sections
  - Dedicated Noun Lessons & Verb Lessons
  - Section Exams, Chapter Exams, Flashbacks
  - 6 Master Languages: en, bn, ur, in, tr, fr
"""

import zipfile
import xml.etree.ElementTree as ET
import json
import re
import os
import sys
import random

LANGUAGES = ["en", "bn", "ur", "in", "tr", "fr"]

def parse_sheet_rows(xml_bytes):
    root = ET.fromstring(xml_bytes)
    ns = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    rows = []
    for row_elem in root.findall(".//s:row", ns):
        r_num = int(row_elem.attrib.get("r", 0))
        if r_num >= 5:
            row_dict = {}
            for c_elem in row_elem.findall("s:c", ns):
                ref = c_elem.attrib.get("r", "")
                col = "".join([c for c in ref if c.isalpha()])
                is_elem = c_elem.find("s:is", ns)
                t_str = "".join([t.text for t in is_elem.findall(".//s:t", ns) if t.text]) if is_elem is not None else ""
                v_elem = c_elem.find("s:v", ns)
                v_str = v_elem.text if v_elem is not None and v_elem.text else ""
                row_dict[col] = t_str or v_str
            if row_dict.get("B"):
                rows.append((r_num, row_dict))
    return rows

def load_sheet(fpath, sheet_idx):
    with zipfile.ZipFile(fpath) as z:
        wroot = ET.fromstring(z.read("xl/workbook.xml"))
        ns = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
        sheets = wroot.findall(".//s:sheet", ns)
        rroot = ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
        rels = {}
        for r in rroot:
            r_id = r.attrib.get("Id")
            target = r.attrib.get("Target")
            if target.startswith("/"):
                target = target[1:]
            elif not target.startswith("xl/"):
                target = "xl/" + target
            rels[r_id] = target
        s = sheets[sheet_idx]
        target = rels.get(s.attrib.get("{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"))
        return parse_sheet_rows(z.read(target))

def parse_bracket_verse(raw_arabic, raw_en, raw_bn, raw_ur, raw_in, raw_tr, raw_fr):
    def split_verses(text):
        if not text:
            return []
        parts = re.split(r'(?:^|\n|\|\s*)(?:[1-9]\d*\.\s*)', text.strip())
        return [p.strip() for p in parts if p.strip()]

    ar_parts = split_verses(raw_arabic)
    en_parts = split_verses(raw_en)
    bn_parts = split_verses(raw_bn)
    ur_parts = split_verses(raw_ur)
    in_parts = split_verses(raw_in)
    tr_parts = split_verses(raw_tr)
    fr_parts = split_verses(raw_fr)

    if not ar_parts:
        ar_parts = [raw_arabic.strip()] if raw_arabic.strip() else []
        en_parts = [raw_en.strip()] if raw_en.strip() else []
        bn_parts = [raw_bn.strip()] if raw_bn.strip() else []
        ur_parts = [raw_ur.strip()] if raw_ur.strip() else []
        in_parts = [raw_in.strip()] if raw_in.strip() else []
        tr_parts = [raw_tr.strip()] if raw_tr.strip() else []
        fr_parts = [raw_fr.strip()] if raw_fr.strip() else []

    entries = []
    max_count = max(len(ar_parts), len(en_parts), 1)
    
    for i in range(max_count):
        ar = ar_parts[i] if i < len(ar_parts) else (ar_parts[0] if ar_parts else "")
        en = en_parts[i] if i < len(en_parts) else (en_parts[0] if en_parts else "")
        bn = bn_parts[i] if i < len(bn_parts) else (bn_parts[0] if bn_parts else "")
        ur = ur_parts[i] if i < len(ur_parts) else (ur_parts[0] if ur_parts else "")
        ind = in_parts[i] if i < len(in_parts) else (in_parts[0] if in_parts else "")
        tr = tr_parts[i] if i < len(tr_parts) else (tr_parts[0] if tr_parts else "")
        fr = fr_parts[i] if i < len(fr_parts) else (fr_parts[0] if fr_parts else "")

        if not ar and not en:
            continue

        ref_match = re.search(r'\[(\d+:\d+)\]', ar + " " + en)
        verse_ref = f"Surah {ref_match.group(1)}" if ref_match else None

        m_ar = re.search(r'【(.*?)】', ar)
        if m_ar:
            highlight_ar = m_ar.group(1)
            clean_ar = ar.replace('【', '').replace('】', '').strip()
            clean_ar = re.sub(r'\s*\[\d+:\d+\]\s*$', '', clean_ar).strip()
            start_ar = clean_ar.find(highlight_ar)
            end_ar = start_ar + len(highlight_ar) if start_ar != -1 else None
        else:
            clean_ar = ar.replace('【', '').replace('】', '').strip()
            clean_ar = re.sub(r'\s*\[\d+:\d+\]\s*$', '', clean_ar).strip()
            start_ar = None
            end_ar = None

        def clean_trans(t):
            if not t:
                return "", None
            t_no_ref = re.sub(r'\s*\[\d+:\d+\]\s*$', '', t).strip()
            m_hl = re.search(r'\[(.*?)\]', t_no_ref)
            hl = m_hl.group(1) if m_hl else None
            cleaned = t_no_ref.replace('[', '').replace(']', '').strip()
            return cleaned, hl

        clean_en, hl_en = clean_trans(en)
        clean_bn, hl_bn = clean_trans(bn)
        clean_ur, hl_ur = clean_trans(ur)
        clean_in, hl_in = clean_trans(ind)
        clean_tr, hl_tr = clean_trans(tr)
        clean_fr, hl_fr = clean_trans(fr)

        entry = {
            "meaningIndex": i + 1,
            "verseReference": verse_ref,
            "verseArabic": clean_ar,
            "arabicWordStart": start_ar,
            "arabicWordEnd": end_ar,
            "verseTranslation": {
                "en": clean_en,
                "bn": clean_bn,
                "ur": clean_ur,
                "in": clean_in,
                "tr": clean_tr,
                "fr": clean_fr
            },
            "translationHighlight": {
                "en": hl_en or "",
                "bn": hl_bn or "",
                "ur": hl_ur or "",
                "in": hl_in or "",
                "tr": hl_tr or "",
                "fr": hl_fr or ""
            }
        }
        entries.append(entry)

    return entries

def main():
    print("Loading Noun and Verb spreadsheets...")
    noun_rows = load_sheet("assets/Words & Meanings/quranic_noun_lemmas_3057-v5.xlsx", 1)
    verb_rows = load_sheet("assets/Words & Meanings/quranic_verb_lemmas_1479-v5.xlsx", 1)
    verb_s3_rows = load_sheet("assets/Words & Meanings/quranic_verb_lemmas_1479-v5.xlsx", 2)

    verb_s3_by_lemma = {}
    for r_num, r in verb_s3_rows:
        lemma = r.get("B", "").strip()
        if lemma and lemma not in verb_s3_by_lemma:
            verb_s3_by_lemma[lemma] = r

    nouns_list = []
    for r_num, r in noun_rows:
        lemma = r.get("B", "").strip()
        occ = int(r.get("F", "0") or 0)
        meaning = {
            "en": r.get("J", "").strip(),
            "bn": r.get("K", "").strip(),
            "ur": r.get("L", "").strip(),
            "in": r.get("M", "").strip(),
            "tr": r.get("N", "").strip(),
            "fr": r.get("O", "").strip()
        }
        poly_entries = parse_bracket_verse(
            r.get("R", ""), r.get("S", ""), r.get("T", ""),
            r.get("U", ""), r.get("V", ""), r.get("W", ""), r.get("X", "")
        )
        nouns_list.append({
            "category": "NOUN",
            "arabicWord": lemma,
            "transliteration": r.get("C", "").strip(),
            "triliteralRoot": r.get("D", "").strip(),
            "occurrences": occ,
            "meaning": meaning,
            "polysemyEntries": poly_entries
        })

    verbs_list = []
    for r_num, r in verb_rows:
        lemma = r.get("B", "").strip()
        occ = int(r.get("F", "0") or 0)
        meaning = {
            "en": r.get("J", "").strip(),
            "bn": r.get("K", "").strip(),
            "ur": r.get("L", "").strip(),
            "in": r.get("M", "").strip(),
            "tr": r.get("N", "").strip(),
            "fr": r.get("O", "").strip()
        }
        
        s3 = verb_s3_by_lemma.get(lemma)
        if s3 and s3.get("H"):
            poly_entries = parse_bracket_verse(
                s3.get("H", ""), s3.get("I", ""), s3.get("J", ""),
                s3.get("K", ""), s3.get("L", ""), s3.get("M", ""), s3.get("N", "")
            )
        else:
            poly_entries = parse_bracket_verse(
                r.get("R", ""), r.get("S", ""), r.get("T", ""),
                r.get("U", ""), r.get("V", ""), r.get("W", ""), r.get("X", "")
            )

        verbs_list.append({
            "category": "VERB",
            "arabicWord": lemma,
            "transliteration": r.get("C", "").strip(),
            "triliteralRoot": r.get("D", "").strip(),
            "occurrences": occ,
            "meaning": meaning,
            "polysemyEntries": poly_entries
        })

    nouns_list.sort(key=lambda x: x["occurrences"], reverse=True)
    verbs_list.sort(key=lambda x: x["occurrences"], reverse=True)

    print(f"Extracted {len(nouns_list)} Nouns and {len(verbs_list)} Verbs.")

    all_lemmas = []
    for item in nouns_list + verbs_list:
        all_lemmas.append(item)
    all_lemmas.sort(key=lambda x: x["occurrences"], reverse=True)

    words_db = []
    word_by_id = {}
    for rank, w in enumerate(all_lemmas, start=1):
        prefix = "wn" if w["category"] == "NOUN" else "wv"
        w_id = f"{prefix}_{rank:04d}"
        w["id"] = w_id
        w["frequencyRank"] = rank
        w["frequencyCount"] = w["occurrences"]
        w["audioAssetPath"] = f"audio/words/{w_id}.mp3"
        w["tierLevel"] = 1 if rank <= 1000 else (2 if rank <= 3000 else 3)
        words_db.append(w)
        word_by_id[w_id] = w

    out_dir = "app/src/main/assets/content"
    os.makedirs(out_dir, exist_ok=True)
    
    # Save word_frequency.json
    with open(f"{out_dir}/word_frequency.json", "w", encoding="utf-8") as f:
        json.dump({"words": words_db}, f, ensure_ascii=False, indent=2)
    print(f"Saved {len(words_db)} words to word_frequency.json")

    TOTAL_CHAPTERS = 10
    SECTIONS_PER_CHAPTER = 10
    TOTAL_SECTIONS = TOTAL_CHAPTERS * SECTIONS_PER_CHAPTER # 100

    section_nouns = [[] for _ in range(TOTAL_SECTIONS)]
    section_verbs = [[] for _ in range(TOTAL_SECTIONS)]

    for i, noun in enumerate(nouns_list):
        sec_idx = min(i * TOTAL_SECTIONS // len(nouns_list), TOTAL_SECTIONS - 1)
        section_nouns[sec_idx].append(noun)

    for i, verb in enumerate(verbs_list):
        sec_idx = min(i * TOTAL_SECTIONS // len(verbs_list), TOTAL_SECTIONS - 1)
        section_verbs[sec_idx].append(verb)

    chapters = []
    sections = []
    lessons = []
    exercises = []

    total_occurrences_all = sum(w["occurrences"] for w in words_db)

    lesson_counter = 1
    exercise_counter = 1

    chapter_names = [
        {"en": "Divine Foundations & Core Pillars", "bn": "তাওহিদ, আল্লাহর পরিচয় ও মৌলিক স্তম্ভ", "ur": "توحید اور بنیادی ارکان", "in": "Fondasi Ketauhidan & Rukun Utama", "tr": "Tevhid ve Temel Esaslar", "fr": "Fondements Divins & Piliers Majeurs"},
        {"en": "Prophets, Nations & Moral Guidance", "bn": "নবীগণ, ঐতিহাসিক জাতিসমূহ ও নৈতিক শিক্ষা", "ur": "انبیاء، اقوام اور اخلاقی رہنمائی", "in": "Para Nabi, Kisah Kaum & Bimbingan Moral", "tr": "Peygamberler, Kavimler ve Ahlak", "fr": "Prophètes, Nations & Morales"},
        {"en": "Worship, Devotion & Righteous Deeds", "bn": "ইবাদত, আত্মশুদ্ধি ও সৎকর্মের আহ্বান", "ur": "عبادت، تقویٰ اور اعمالِ صالحہ", "in": "Ibadah, Pengabdian & Amal Saleh", "tr": "İbadet, Takva ve Salih Ameller", "fr": "Adoration, Dévotion & Bonnes Actions"},
        {"en": "Faith, Belief & Spiritual Realities", "bn": "ঈমান, অন্তরের বিশ্বাস ও আত্মিক উপলব্ধি", "ur": "ایمان، یقین اور روحانی حقائق", "in": "Iman, Keyakinan & Realitas Spiritual", "tr": "İman, İnanç ve Maneviyat", "fr": "Foi, Croyance & Réalités Spirituelles"},
        {"en": "Creation, Heavens, Earth & Cosmos", "bn": "সৃষ্টিজগৎ, আকাশমণ্ডল ও প্রকৃতির নিদর্শন", "ur": "تخلیقِ کائنات، زمین و آسمان", "in": "Penciptaan Alam Semesta, Langit & Bumi", "tr": "Yaratılış, Gökyüzü ve Yeryüzü", "fr": "Création, Cieux & Signes de la Terre"},
        {"en": "Humanity, Society & Sacred Law", "bn": "মানবসমাজ, পরিবার ও শরিয়তের বিধান", "ur": "انسانی معاشرہ، خاندان اور احکام", "in": "Kemanusiaan, Masyarakat & Hukum Syariat", "tr": "İnsanlık, Toplum ve Hükümler", "fr": "Humanité, Société & Lois Sacrées"},
        {"en": "The Hereafter, Resurrection & Eternity", "bn": "আখিরাত, কিয়ামত, জান্নাত ও জাহান্নাম", "ur": "آخرت، قیامت اور ابدی زندگی", "in": "Hari Kiamat, Kebangkitan & Keabadian", "tr": "Ahiret, Kıyamet ve Ebediyet", "fr": "L'Au-delà, Résurrection & Éternité"},
        {"en": "Divine Wisdom, Parables & Signs", "bn": "ঐশ্বরিক প্রজ্ঞা, উপদেশমূলক রূপক ও নিদর্শন", "ur": "حکمتِ الٰہی، امثال اور عبرتیں", "in": "Hikmah Ilahi, Perumpamaan & Tanda Kekuasaan", "tr": "İlahi Hikmet, Meseller ve Deliller", "fr": "Sagesse Divine, Paraboles & Signes"},
        {"en": "Patience, Struggle & Divine Victory", "bn": "ধৈর্য, অবিচল সাধনা ও আল্লাহর সাহায্য", "ur": "صبر، استقامت اور نصرتِ الٰہی", "in": "Kesabaran, Perjuangan & Pertolongan Allah", "tr": "Sabır, Mücadele ve İlahi Zafer", "fr": "Patience, Épreuves & Victoire Divine"},
        {"en": "Comprehensive Lexicon & Deep Eloquence", "bn": "সার্বজনীন কুরআনিক শব্দভাণ্ডার ও গভীর বাগ্মিতা", "ur": "جامع قرآنی الفاظ اور بلاغت", "in": "Kosakata Lengkap & Keindahan Balaghah", "tr": "Kapsamlı Sözlük ve Belagat", "fr": "Lexique Intégral & Haute Éloquence"}
    ]

    for ch_idx in range(TOTAL_CHAPTERS):
        ch_id = f"ch_{ch_idx + 1:02d}"
        ch_sec_start = ch_idx * SECTIONS_PER_CHAPTER
        ch_sec_end = ch_sec_start + SECTIONS_PER_CHAPTER
        
        ch_nouns = [w for s in section_nouns[ch_sec_start:ch_sec_end] for w in s]
        ch_verbs = [w for s in section_verbs[ch_sec_start:ch_sec_end] for w in s]
        ch_words = ch_nouns + ch_verbs
        
        ch_occ = sum(w["occurrences"] for w in ch_words)
        ch_pct = (ch_occ / total_occurrences_all) * 100.0 if total_occurrences_all else 10.0

        chapters.append({
            "id": ch_id,
            "title": chapter_names[ch_idx],
            "description": {
                "en": f"Covers {len(ch_words)} core Qur'anic lemmas ({len(ch_nouns)} Nouns & {len(ch_verbs)} Verbs) with contextual polysemy examples.",
                "bn": f"{len(ch_words)}টি মূল কুরআনিক শব্দ ({len(ch_nouns)}টি বিশেষ্য ও {len(ch_verbs)}টি ক্রিয়া) এবং আয়াতভিত্তিক অর্থ ধারণ করে।",
                "ur": f"قرآن کے {len(ch_words)} اہم الفاظ ({len(ch_nouns)} اسم اور {len(ch_verbs)} فعل) اور ان کے معانی کا احاطہ کرتا ہے۔",
                "in": f"Mencakup {len(ch_words)} kata inti Al-Qur'an ({len(ch_nouns)} Nomina & {len(ch_verbs)} Verba) beserta contoh ayat.",
                "tr": f"{len(ch_words)} temel Kur'an kelimesini ({len(ch_nouns)} İsim ve {len(ch_verbs)} Fiil) ve ayet örneklerini kapsar.",
                "fr": f"Couvre {len(ch_words)} lemmes coraniques majeurs ({len(ch_nouns)} Noms & {len(ch_verbs)} Verbes) avec exemples de versets."
            },
            "sortOrder": ch_idx + 1,
            "wordCount": len(ch_words),
            "quranOccurrenceCount": ch_occ,
            "quranOccurrencePercent": round(ch_pct, 2)
        })

        for sec_local_idx in range(SECTIONS_PER_CHAPTER):
            global_sec_idx = ch_sec_start + sec_local_idx
            sec_id = f"sec_{global_sec_idx + 1:03d}"
            
            s_nouns = section_nouns[global_sec_idx]
            s_verbs = section_verbs[global_sec_idx]
            s_words = s_nouns + s_verbs
            s_occ = sum(w["occurrences"] for w in s_words)
            s_pct = (s_occ / total_occurrences_all) * 100.0 if total_occurrences_all else 1.0

            sections.append({
                "id": sec_id,
                "chapterId": ch_id,
                "title": {
                    "en": f"Section {sec_local_idx + 1}: {chapter_names[ch_idx]['en'].split('&')[0].strip()}",
                    "bn": f"অধ্যায় {sec_local_idx + 1}: {chapter_names[ch_idx]['bn'].split(',')[0].strip()}",
                    "ur": f"سیکشن {sec_local_idx + 1}: {chapter_names[ch_idx]['ur'].split('،')[0].strip()}",
                    "in": f"Bagian {sec_local_idx + 1}: {chapter_names[ch_idx]['in'].split('&')[0].strip()}",
                    "tr": f"Bölüm {sec_local_idx + 1}: {chapter_names[ch_idx]['tr'].split('ve')[0].strip()}",
                    "fr": f"Section {sec_local_idx + 1}: {chapter_names[ch_idx]['fr'].split('&')[0].strip()}"
                },
                "sortOrder": sec_local_idx + 1,
                "wordCount": len(s_words),
                "quranOccurrenceCount": s_occ,
                "quranOccurrencePercent": round(s_pct, 2)
            })

            # 6 Noun Lessons
            noun_sublists = [[] for _ in range(6)]
            for n_i, n_w in enumerate(s_nouns):
                noun_sublists[n_i % 6].append(n_w)

            # 3 Verb Lessons
            verb_sublists = [[] for _ in range(3)]
            for v_i, v_w in enumerate(s_verbs):
                verb_sublists[v_i % 3].append(v_w)

            sec_lesson_ids = []

            # 1. Noun Lessons
            for nl_idx, nl_words in enumerate(noun_sublists, start=1):
                if not nl_words:
                    continue
                l_id = f"les_{lesson_counter:04d}"
                lesson_counter += 1
                sec_lesson_ids.append(l_id)

                l_word_ids = [w["id"] for w in nl_words]
                lessons.append({
                    "id": l_id,
                    "chapterId": ch_id,
                    "sectionId": sec_id,
                    "title": {
                        "en": f"Noun Lesson {nl_idx}: {nl_words[0]['arabicWord']} & Nouns",
                        "bn": f"বিশেষ্য পাঠ {nl_idx}: {nl_words[0]['arabicWord']} ও বিশেষ্যসমূহ",
                        "ur": f"اسماء سبق {nl_idx}: {nl_words[0]['arabicWord']} اور اسماء",
                        "in": f"Pelajaran Nomina {nl_idx}: {nl_words[0]['arabicWord']} & Nomina",
                        "tr": f"İsim Dersi {nl_idx}: {nl_words[0]['arabicWord']} ve İsimler",
                        "fr": f"Leçon Noms {nl_idx}: {nl_words[0]['arabicWord']} & Noms"
                    },
                    "sortOrder": len(sec_lesson_ids),
                    "kind": "REGULAR",
                    "category": "NOUN"
                })

                order_in_lesson = 1
                for w in nl_words:
                    ex_id = f"ex_{exercise_counter:06d}"
                    exercise_counter += 1
                    p_entry = w["polysemyEntries"][0] if w["polysemyEntries"] else None
                    exercises.append({
                        "id": ex_id,
                        "lessonId": l_id,
                        "orderIndex": order_in_lesson,
                        "exerciseType": "WORD_INTRO",
                        "content": {
                            "type": "word_intro",
                            "wordId": w["id"],
                            "arabicWord": w["arabicWord"],
                            "lemmaCategory": "NOUN",
                            "prompt": {
                                "en": "Listen & Learn the Qur'anic Noun",
                                "bn": "শুনুন ও কুরআনিক বিশেষ্যটি শিখুন",
                                "ur": "سنیں اور قرآنی اسم سیکھیں",
                                "in": "Dengarkan & Pelajari Nomina Al-Qur'an",
                                "tr": "Dinleyin ve Kur'an İsmini Öğrenin",
                                "fr": "Écoutez & Apprenez le Nom Coranique"
                            },
                            "meaning": w["meaning"],
                            "polysemyEntries": w["polysemyEntries"],
                            "exampleVerseReference": p_entry["verseReference"] if p_entry and p_entry.get("verseReference") else "Surah Reference",
                            "exampleVerseArabic": p_entry["verseArabic"] if p_entry and p_entry.get("verseArabic") else w["arabicWord"],
                            "arabicWordStart": p_entry["arabicWordStart"] if p_entry else None,
                            "arabicWordEnd": p_entry["arabicWordEnd"] if p_entry else None,
                            "exampleVerseTranslation": p_entry["verseTranslation"] if p_entry else w["meaning"],
                            "meaningHighlight": p_entry["translationHighlight"] if p_entry and p_entry.get("translationHighlight") else w["meaning"]
                        }
                    })
                    order_in_lesson += 1

                    # MultipleChoice Exercise
                    ex_id = f"ex_{exercise_counter:06d}"
                    exercise_counter += 1
                    distractors = [cand for cand in words_db if cand["id"] != w["id"] and cand["category"] == "NOUN"][:3]
                    opts = [{"id": w["id"], "labelArabic": w["arabicWord"], "label": w["meaning"]}]
                    for d in distractors:
                        opts.append({"id": d["id"], "labelArabic": d["arabicWord"], "label": d["meaning"]})
                    random.seed(w["frequencyRank"])
                    random.shuffle(opts)

                    exercises.append({
                        "id": ex_id,
                        "lessonId": l_id,
                        "orderIndex": order_in_lesson,
                        "exerciseType": "MULTIPLE_CHOICE",
                        "content": {
                            "type": "multiple_choice",
                            "wordId": w["id"],
                            "prompt": {
                                "en": "What is the meaning of this Qur'anic noun?",
                                "bn": "এই কুরআনিক বিশেষ্য শব্দটির অর্থ কী?",
                                "ur": "اس قرآنی اسم کا کیا مطلب ہے؟",
                                "in": "Apa arti dari nomina Al-Qur'an ini?",
                                "tr": "Bu Kur'an isminin anlamı nedir?",
                                "fr": "Quel est le sens de ce nom coranique ?"
                            },
                            "promptArabic": w["arabicWord"],
                            "exampleVerseReference": p_entry["verseReference"] if p_entry else None,
                            "exampleVerseArabic": p_entry["verseArabic"] if p_entry else None,
                            "arabicWordStart": p_entry["arabicWordStart"] if p_entry else None,
                            "arabicWordEnd": p_entry["arabicWordEnd"] if p_entry else None,
                            "options": opts,
                            "correctOptionId": w["id"]
                        }
                    })
                    order_in_lesson += 1

                # Matching Exercise
                if len(nl_words) >= 4:
                    ex_id = f"ex_{exercise_counter:06d}"
                    exercise_counter += 1
                    pairs = []
                    for pw in nl_words[:4]:
                        pairs.append({
                            "wordId": pw["id"],
                            "left": pw["arabicWord"],
                            "right": pw["meaning"]
                        })
                    exercises.append({
                        "id": ex_id,
                        "lessonId": l_id,
                        "orderIndex": order_in_lesson,
                        "exerciseType": "MATCHING",
                        "content": {
                            "type": "matching",
                            "prompt": {
                                "en": "Match the Qur'anic nouns with their meanings",
                                "bn": "কুরআনিক বিশেষ্য শব্দের সাথে অর্থ মিলান",
                                "ur": "قرآنی اسماء کو ان کے معانی کے ساتھ ملائیں",
                                "in": "Pasangkan nomina Al-Qur'an dengan artinya",
                                "tr": "Kur'an isimlerini anlamlarıyla eşleştirin",
                                "fr": "Associez les noms coraniques à leur sens"
                            },
                            "pairs": pairs
                        }
                    })

            # 2. Verb Lessons
            for vl_idx, vl_words in enumerate(verb_sublists, start=1):
                if not vl_words:
                    continue
                l_id = f"les_{lesson_counter:04d}"
                lesson_counter += 1
                sec_lesson_ids.append(l_id)

                l_word_ids = [w["id"] for w in vl_words]
                lessons.append({
                    "id": l_id,
                    "chapterId": ch_id,
                    "sectionId": sec_id,
                    "title": {
                        "en": f"Verb Lesson {vl_idx}: {vl_words[0]['arabicWord']} & Verbs",
                        "bn": f"ক্রিয়াপদ পাঠ {vl_idx}: {vl_words[0]['arabicWord']} ও ক্রিয়াসমূহ",
                        "ur": f"افعال سبق {vl_idx}: {vl_words[0]['arabicWord']} اور افعال",
                        "in": f"Pelajaran Verba {vl_idx}: {vl_words[0]['arabicWord']} & Verba",
                        "tr": f"Fiil Dersi {vl_idx}: {vl_words[0]['arabicWord']} ve Fiiller",
                        "fr": f"Leçon Verbes {vl_idx}: {vl_words[0]['arabicWord']} & Verbes"
                    },
                    "sortOrder": len(sec_lesson_ids),
                    "kind": "REGULAR",
                    "category": "VERB"
                })

                order_in_lesson = 1
                for w in vl_words:
                    ex_id = f"ex_{exercise_counter:06d}"
                    exercise_counter += 1
                    p_entry = w["polysemyEntries"][0] if w["polysemyEntries"] else None
                    exercises.append({
                        "id": ex_id,
                        "lessonId": l_id,
                        "orderIndex": order_in_lesson,
                        "exerciseType": "WORD_INTRO",
                        "content": {
                            "type": "word_intro",
                            "wordId": w["id"],
                            "arabicWord": w["arabicWord"],
                            "lemmaCategory": "VERB",
                            "prompt": {
                                "en": "Listen & Learn the Qur'anic Verb",
                                "bn": "শুনুন ও কুরআনিক ক্রিয়াটি শিখুন",
                                "ur": "سنیں اور قرآنی فعل سیکھیں",
                                "in": "Dengarkan & Pelajari Verba Al-Qur'an",
                                "tr": "Dinleyin ve Kur'an Fiilini Öğrenin",
                                "fr": "Écoutez & Apprenez le Verbe Coranique"
                            },
                            "meaning": w["meaning"],
                            "polysemyEntries": w["polysemyEntries"],
                            "exampleVerseReference": p_entry["verseReference"] if p_entry and p_entry.get("verseReference") else "Surah Reference",
                            "exampleVerseArabic": p_entry["verseArabic"] if p_entry and p_entry.get("verseArabic") else w["arabicWord"],
                            "arabicWordStart": p_entry["arabicWordStart"] if p_entry else None,
                            "arabicWordEnd": p_entry["arabicWordEnd"] if p_entry else None,
                            "exampleVerseTranslation": p_entry["verseTranslation"] if p_entry else w["meaning"],
                            "meaningHighlight": p_entry["translationHighlight"] if p_entry and p_entry.get("translationHighlight") else w["meaning"]
                        }
                    })
                    order_in_lesson += 1

                    # MultipleChoice
                    ex_id = f"ex_{exercise_counter:06d}"
                    exercise_counter += 1
                    distractors = [cand for cand in words_db if cand["id"] != w["id"] and cand["category"] == "VERB"][:3]
                    opts = [{"id": w["id"], "labelArabic": w["arabicWord"], "label": w["meaning"]}]
                    for d in distractors:
                        opts.append({"id": d["id"], "labelArabic": d["arabicWord"], "label": d["meaning"]})
                    random.seed(w["frequencyRank"])
                    random.shuffle(opts)

                    exercises.append({
                        "id": ex_id,
                        "lessonId": l_id,
                        "orderIndex": order_in_lesson,
                        "exerciseType": "MULTIPLE_CHOICE",
                        "content": {
                            "type": "multiple_choice",
                            "wordId": w["id"],
                            "prompt": {
                                "en": "What is the meaning of this Qur'anic verb?",
                                "bn": "এই কুরআনিক ক্রিয়াপদটির অর্থ কী?",
                                "ur": "اس قرآنی فعل کا کیا مطلب ہے؟",
                                "in": "Apa arti dari verba Al-Qur'an ini?",
                                "tr": "Bu Kur'an fiilinin anlamı nedir?",
                                "fr": "Quel est le sens de ce verbe coranique ?"
                            },
                            "promptArabic": w["arabicWord"],
                            "exampleVerseReference": p_entry["verseReference"] if p_entry else None,
                            "exampleVerseArabic": p_entry["verseArabic"] if p_entry else None,
                            "arabicWordStart": p_entry["arabicWordStart"] if p_entry else None,
                            "arabicWordEnd": p_entry["arabicWordEnd"] if p_entry else None,
                            "options": opts,
                            "correctOptionId": w["id"]
                        }
                    })
                    order_in_lesson += 1

                # Matching Exercise for Verbs
                if len(vl_words) >= 4:
                    ex_id = f"ex_{exercise_counter:06d}"
                    exercise_counter += 1
                    pairs = []
                    for pw in vl_words[:4]:
                        pairs.append({
                            "wordId": pw["id"],
                            "left": pw["arabicWord"],
                            "right": pw["meaning"]
                        })
                    exercises.append({
                        "id": ex_id,
                        "lessonId": l_id,
                        "orderIndex": order_in_lesson,
                        "exerciseType": "MATCHING",
                        "content": {
                            "type": "matching",
                            "prompt": {
                                "en": "Match the Qur'anic verbs with their meanings",
                                "bn": "কুরআনিক ক্রিয়াপদের সাথে অর্থ মিলান",
                                "ur": "قرآنی افعال کو ان کے معانی کے ساتھ ملائیں",
                                "in": "Pasangkan verba Al-Qur'an dengan artinya",
                                "tr": "Kur'an fiillerini anlamlarıyla eşleştirin",
                                "fr": "Associez les verbes coraniques à leur sens"
                            },
                            "pairs": pairs
                        }
                    })

            # 3. Section Flashback
            fb_id = f"les_{lesson_counter:04d}"
            lesson_counter += 1
            lessons.append({
                "id": fb_id,
                "chapterId": ch_id,
                "sectionId": sec_id,
                "title": {
                    "en": "Section Flashback: Nouns & Verbs Review",
                    "bn": "অধ্যায় পর্যালোচনা: বিশেষ্য ও ক্রিয়া রিভিউ",
                    "ur": "سیکشن دہرائی: اسماء اور افعال کا جائزہ",
                    "in": "Ulasan Kilas Balik: Nomina & Verba",
                    "tr": "Bölüm Tekrarı: İsimler ve Fiiller",
                    "fr": "Révision de la Section: Noms & Verbes"
                },
                "sortOrder": len(sec_lesson_ids) + 1,
                "kind": "SECTION_FLASHBACK",
                "category": "MIXED"
            })

            # Exercises for Section Flashback
            fb_sample_words = s_words[:8]
            for fb_idx, w in enumerate(fb_sample_words, start=1):
                ex_id = f"ex_{exercise_counter:06d}"
                exercise_counter += 1
                distractors = [cand for cand in words_db if cand["id"] != w["id"]][:3]
                opts = [{"id": w["id"], "labelArabic": w["arabicWord"], "label": w["meaning"]}]
                for d in distractors:
                    opts.append({"id": d["id"], "labelArabic": d["arabicWord"], "label": d["meaning"]})
                random.seed(w["frequencyRank"] + 77)
                random.shuffle(opts)

                p_entry = w["polysemyEntries"][0] if w["polysemyEntries"] else None
                exercises.append({
                    "id": ex_id,
                    "lessonId": fb_id,
                    "orderIndex": fb_idx,
                    "exerciseType": "MULTIPLE_CHOICE",
                    "content": {
                        "type": "multiple_choice",
                        "wordId": w["id"],
                        "prompt": {
                            "en": "Review: Select the correct meaning",
                            "bn": "রিভিউ: সঠিক অর্থটি নির্বাচন করুন",
                            "ur": "دہرائی: درست معنی منتخب کریں",
                            "in": "Ulasan: Pilih arti yang benar",
                            "tr": "Tekrar: Doğru anlamı seçin",
                            "fr": "Révision: Choisissez la bonne signification"
                        },
                        "promptArabic": w["arabicWord"],
                        "exampleVerseReference": p_entry["verseReference"] if p_entry else None,
                        "exampleVerseArabic": p_entry["verseArabic"] if p_entry else None,
                        "arabicWordStart": p_entry["arabicWordStart"] if p_entry else None,
                        "arabicWordEnd": p_entry["arabicWordEnd"] if p_entry else None,
                        "options": opts,
                        "correctOptionId": w["id"]
                    }
                })

            # 4. Section Exam
            exam_id = f"les_{lesson_counter:04d}"
            lesson_counter += 1
            lessons.append({
                "id": exam_id,
                "chapterId": ch_id,
                "sectionId": sec_id,
                "title": {
                    "en": "Section Exam: Master Assessment",
                    "bn": "অধ্যায় চূড়ান্ত পরীক্ষা: মূল্যায়ন",
                    "ur": "سیکشن امتحان: جامع جانچ",
                    "in": "Ujian Bagian: Evaluasi Lengkap",
                    "tr": "Bölüm Sınavı: Kapsamlı Değerlendirme",
                    "fr": "Examen de Section: Évaluation Finale"
                },
                "sortOrder": len(sec_lesson_ids) + 2,
                "kind": "SECTION_EXAM",
                "category": "MIXED"
            })

            # Exercises for Section Exam
            exam_sample_words = s_words[:10]
            for ex_idx, w in enumerate(exam_sample_words, start=1):
                ex_id = f"ex_{exercise_counter:06d}"
                exercise_counter += 1
                distractors = [cand for cand in words_db if cand["id"] != w["id"]][:3]
                opts = [{"id": w["id"], "labelArabic": w["arabicWord"], "label": w["meaning"]}]
                for d in distractors:
                    opts.append({"id": d["id"], "labelArabic": d["arabicWord"], "label": d["meaning"]})
                random.seed(w["frequencyRank"] + 99)
                random.shuffle(opts)

                p_entry = w["polysemyEntries"][0] if w["polysemyEntries"] else None
                exercises.append({
                    "id": ex_id,
                    "lessonId": exam_id,
                    "orderIndex": ex_idx,
                    "exerciseType": "MULTIPLE_CHOICE",
                    "content": {
                        "type": "multiple_choice",
                        "wordId": w["id"],
                        "prompt": {
                            "en": "Exam Question: Identify the correct meaning",
                            "bn": "পরীক্ষার প্রশ্ন: সঠিক অর্থটি চিহ্নিত করুন",
                            "ur": "امتحانی سوال: درست معنی کی شناخت کریں",
                            "in": "Soal Ujian: Tentukan arti yang benar",
                            "tr": "Sınav Sorusu: Doğru anlamı belirleyin",
                            "fr": "Question d'Examen: Identifiez le sens exact"
                        },
                        "promptArabic": w["arabicWord"],
                        "exampleVerseReference": p_entry["verseReference"] if p_entry else None,
                        "exampleVerseArabic": p_entry["verseArabic"] if p_entry else None,
                        "arabicWordStart": p_entry["arabicWordStart"] if p_entry else None,
                        "arabicWordEnd": p_entry["arabicWordEnd"] if p_entry else None,
                        "options": opts,
                        "correctOptionId": w["id"]
                    }
                })

        # Chapter Exam
        ch_exam_id = f"les_{lesson_counter:04d}"
        lesson_counter += 1
        lessons.append({
            "id": ch_exam_id,
            "chapterId": ch_id,
            "sectionId": None,
            "title": {
                "en": f"Chapter {ch_idx + 1} Grand Exam: Master Milestone",
                "bn": f"অধ্যায় {ch_idx + 1} গ্র্যান্ড এক্সাম: মাইলফলক মূল্যায়ন",
                "ur": f"باب {ch_idx + 1} عظیم امتحان: شاندار سنگ میل",
                "in": f"Ujian Akhir Bab {ch_idx + 1}: Tonggak Utama",
                "tr": f"{ch_idx + 1}. Bölüm Büyük Sınavı: Başarı Kilidi",
                "fr": f"Grand Examen du Chapitre {ch_idx + 1}: Étape Majeure"
            },
            "sortOrder": 100,
            "kind": "CHAPTER_EXAM",
            "category": "MIXED"
        })

        # Exercises for Chapter Exam
        ch_sample_words = ch_words[:15]
        for ch_ex_idx, w in enumerate(ch_sample_words, start=1):
            ex_id = f"ex_{exercise_counter:06d}"
            exercise_counter += 1
            distractors = [cand for cand in words_db if cand["id"] != w["id"]][:3]
            opts = [{"id": w["id"], "labelArabic": w["arabicWord"], "label": w["meaning"]}]
            for d in distractors:
                opts.append({"id": d["id"], "labelArabic": d["arabicWord"], "label": d["meaning"]})
            random.seed(w["frequencyRank"] + 123)
            random.shuffle(opts)

            p_entry = w["polysemyEntries"][0] if w["polysemyEntries"] else None
            exercises.append({
                "id": ex_id,
                "lessonId": ch_exam_id,
                "orderIndex": ch_ex_idx,
                "exerciseType": "MULTIPLE_CHOICE",
                "content": {
                    "type": "multiple_choice",
                    "wordId": w["id"],
                    "prompt": {
                        "en": "Grand Exam: Select the Qur'anic meaning",
                        "bn": "গ্র্যান্ড এক্সাম: কুরআনিক অর্থটি নির্বাচন করুন",
                        "ur": "عظیم امتحان: قرآنی معنی منتخب کریں",
                        "in": "Ujian Utama: Pilih arti kata Al-Qur'an",
                        "tr": "Büyük Sınav: Kur'an anlamını seçin",
                        "fr": "Grand Examen: Choisissez le sens coranique"
                    },
                    "promptArabic": w["arabicWord"],
                    "exampleVerseReference": p_entry["verseReference"] if p_entry else None,
                    "exampleVerseArabic": p_entry["verseArabic"] if p_entry else None,
                    "arabicWordStart": p_entry["arabicWordStart"] if p_entry else None,
                    "arabicWordEnd": p_entry["arabicWordEnd"] if p_entry else None,
                    "options": opts,
                    "correctOptionId": w["id"]
                }
            })

    # Save JSON files
    with open(f"{out_dir}/chapters.json", "w", encoding="utf-8") as f:
        json.dump({"chapters": chapters}, f, ensure_ascii=False, indent=2)
    print(f"Saved {len(chapters)} chapters to chapters.json")

    with open(f"{out_dir}/sections.json", "w", encoding="utf-8") as f:
        json.dump({"sections": sections}, f, ensure_ascii=False, indent=2)
    print(f"Saved {len(sections)} sections to sections.json")

    with open(f"{out_dir}/lessons_vocabulary.json", "w", encoding="utf-8") as f:
        json.dump({"lessons": lessons}, f, ensure_ascii=False, indent=2)
    print(f"Saved {len(lessons)} lessons to lessons_vocabulary.json")

    with open(f"{out_dir}/exercises_vocabulary.json", "w", encoding="utf-8") as f:
        json.dump({"exercises": exercises}, f, ensure_ascii=False, indent=2)
    print(f"Saved {len(exercises)} exercises to exercises_vocabulary.json")

if __name__ == "__main__":
    main()
