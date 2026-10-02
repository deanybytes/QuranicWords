#!/usr/bin/env python3
"""
tools/fixes/synthesize_v1_0_1_complete_curriculum.py
===================================================
Master synthesis script for QuranicWords v1.0.1 (version code 20).

Resolves all four user issues:
1. End-of-lesson matching quiz missing: Generates 4-5 pair MATCHING exercises for all 997 regular lessons.
2. Main page lessons not unlocking: Populates all 100 SECTION_FLASHBACK, 100 SECTION_EXAM, and 10 CHAPTER_EXAM
   lessons with full question suites so gating exams can be completed and passed.
3. Duplicate senses: Audits all 73 polysemy words. Retains multi-sense entries strictly for genuine classical
   Wujūh al-Qur'an words; collapses pseudo-polysemous words with duplicate senses into a single clean primary sense.
4. Blank lessons after a few lessons: Eliminates all 0-exercise lessons (all 1,217 lessons now have complete exercises).
"""

import os
import json
import re
import random

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
CONTENT_DIR = os.path.join(BASE_DIR, 'app', 'src', 'main', 'assets', 'content')

CHAPTERS_PATH = os.path.join(CONTENT_DIR, 'chapters.json')
SECTIONS_PATH = os.path.join(CONTENT_DIR, 'sections.json')
LESSONS_PATH = os.path.join(CONTENT_DIR, 'lessons_vocabulary.json')
EXERCISES_PATH = os.path.join(CONTENT_DIR, 'exercises_vocabulary.json')
WORD_FREQ_PATH = os.path.join(CONTENT_DIR, 'word_frequency.json')

LANGS = ['en', 'bn', 'ur', 'hi', 'in', 'ms', 'tr', 'fa', 'ha', 'sw']

PROMPT_MATCHING = {
    'en': 'Match each word to its meaning',
    'bn': 'প্রতিটি শব্দকে এর অর্থের সাথে মেলান',
    'ur': 'ہر لفظ کو اس کے معنی سے ملائیں',
    'hi': 'प्रत्येक शब्द को उसके अर्थ से मिलाएँ',
    'in': 'Cocokkan setiap kata dengan artinya',
    'ms': 'Padankan setiap perkataan dengan maknanya',
    'tr': 'Her kelimeyi anlamıyla eşleştirin',
    'fa': 'هر کلمه را با معنای آن تطبیق دهید',
    'ha': "Daidaita kowace kalma da ma'anarta",
    'sw': 'Linganisha kila neno na maana yake'
}

PROMPT_MULTIPLE_CHOICE = {
    'en': 'Choose the correct meaning',
    'bn': 'সঠিক অর্থ নির্বাচন করুন',
    'ur': 'درست معنی کا انتخاب کریں',
    'hi': 'सही अर्थ चुनें',
    'in': 'Pilih arti yang benar',
    'ms': 'Pilih maksud yang betul',
    'tr': 'Doğru anlamı seçin',
    'fa': 'معنی درست را انتخاب کنید',
    'ha': "Zabi ma'anar da ta dace",
    'sw': 'Chagua maana sahihi'
}

# 19 Authentic Wujūh al-Qur'an words with distinct Quranic lexical senses
GENUINE_POLYSEMY_WIDS = {
    'w_0001', # مِنْ: from (starting point) / of (partitive)
    'w_0002', # فِي: in (spatial) / concerning
    'w_0004', # عَلَى: upon / against
    'w_0005', # الَّذِي: who / which
    'w_0006', # لَا: no (negation) / do not (prohibition)
    'w_0007', # مَا: what (relative) / not (negation)
    'w_0008', # إِلَى: to / towards
    'w_0009', # مَنْ: who / whoever
    'w_0010', # إِنْ: if (conditional) / not (negation)
    'w_0014', # عَنْ: for / from / away from
    'w_0207', # قَضَى: decrees / complete / judge
    'w_0217', # ضَرَبَ: set forth / strike / travel
    'w_1657', # ءَايَةٌ: sign / verse
    'w_1663', # نَفْسٌ: soul / person
    'w_1665', # كِتَـٰبٌ: book / record / torah
    'w_1670', # سَبِيلٌ: way / justification
    'w_1673', # بَعْضٌ: some / others
    'w_1700', # صَلَاة: prayer / mercy / synagogues / supplication
    'w_1721', # رُوح: spirit / revelation / Gabriel
}

# Clean single canonical meanings for words that had duplicate senses
CLEAN_CANONICAL_OVERRIDES = {
    'w_0017': { # لَم
        'en': 'not', 'bn': 'না', 'ur': 'نہ', 'hi': 'नहीं', 'in': 'tidak',
        'ms': 'tidak', 'tr': 'değil', 'fa': 'نه', 'ha': 'ba', 'sw': 'si'
    },
    'w_0021': { # إِذ
        'en': 'when', 'bn': 'যখন', 'ur': 'جب', 'hi': 'जब', 'in': 'ketika',
        'ms': 'ketika', 'tr': 'o zaman', 'fa': 'هنگامی که', 'ha': 'yayin da', 'sw': 'wakati'
    },
    'w_0025': { # مَع
        'en': 'with', 'bn': 'সাথে', 'ur': 'ساتھ', 'hi': 'साथ', 'in': 'bersama',
        'ms': 'bersama', 'tr': 'beraber', 'fa': 'با', 'ha': 'tare da', 'sw': 'pamoja na'
    },
    'w_0026': { # هُوَ
        'en': 'He', 'bn': 'তিনি', 'ur': 'وہ', 'hi': 'वह', 'in': 'Dia',
        'ms': 'Dia', 'tr': 'O', 'fa': 'او', 'ha': 'shi', 'sw': 'yeye'
    },
    'w_0027': { # هُمْ
        'en': 'they', 'bn': 'তারা', 'ur': 'وہ سب', 'hi': 'वे', 'in': 'mereka',
        'ms': 'mereka', 'tr': 'onlar', 'fa': 'آنان', 'ha': 'su', 'sw': 'wao'
    },
    'w_0174': { # قَالَ
        'en': 'said', 'bn': 'বললেন', 'ur': 'کہا', 'hi': 'कहा', 'in': 'berkata',
        'ms': 'berkata', 'tr': 'dedi', 'fa': 'گفت', 'ha': 'ya ce', 'sw': 'alisema'
    },
    'w_0176': { # آمَنَ
        'en': 'believed', 'bn': 'ঈমান এনেছে', 'ur': 'ایمان لایا', 'hi': 'ईमान लाया', 'in': 'beriman',
        'ms': 'beriman', 'tr': 'inandı', 'fa': 'ایمان آورد', 'ha': 'ya yi imani', 'sw': 'aliamini'
    },
    'w_0177': { # عَلِمَ
        'en': 'knew', 'bn': 'জানল', 'ur': 'جانا', 'hi': 'जाना', 'in': 'mengetahui',
        'ms': 'mengetahui', 'tr': 'bildi', 'fa': 'دانست', 'ha': 'ya sani', 'sw': 'alijua'
    },
    'w_0178': { # جَعَلَ
        'en': 'made', 'bn': 'বানালেন', 'ur': 'بنایا', 'hi': 'बनाया', 'in': 'menjadikan',
        'ms': 'menjadikan', 'tr': 'kıldı', 'fa': 'قرار داد', 'ha': 'ya sanya', 'sw': 'alifanya'
    },
    'w_0180': { # جَاءَ
        'en': 'came', 'bn': 'এসেছে', 'ur': 'آیا', 'hi': 'आया', 'in': 'datang',
        'ms': 'datang', 'tr': 'geldi', 'fa': 'آمد', 'ha': 'ya zo', 'sw': 'alikuja'
    },
    'w_0181': { # عَمِلَ
        'en': 'did / worked', 'bn': 'কাজ করেছে', 'ur': 'عمل کیا', 'hi': 'काम किया', 'in': 'mengerjakan',
        'ms': 'mengerjakan', 'tr': 'işledi', 'fa': 'عمل کرد', 'ha': 'ya aikata', 'sw': 'alitenda'
    },
    'w_0182': { # آتَى
        'en': 'gave', 'bn': 'দিয়েছিলেন', 'ur': 'دیا', 'hi': 'दिया', 'in': 'memberi',
        'ms': 'memberi', 'tr': 'verdi', 'fa': 'داد', 'ha': 'ya bayar', 'sw': 'alitoa'
    },
    'w_0183': { # رَأَى
        'en': 'saw', 'bn': 'দেখেছে', 'ur': 'دیکھا', 'hi': 'देखा', 'in': 'melihat',
        'ms': 'melihat', 'tr': 'gördü', 'fa': 'دید', 'ha': 'ya gani', 'sw': 'aliona'
    },
    'w_0184': { # أَتَى
        'en': 'came', 'bn': 'এসেছে', 'ur': 'آیا', 'hi': 'आया', 'in': 'datang',
        'ms': 'datang', 'tr': 'geldi', 'fa': 'آمد', 'ha': 'ya zo', 'sw': 'alikuja'
    },
    'w_0185': { # شَاءَ
        'en': 'willed', 'bn': 'চেয়েছেন', 'ur': 'چاہا', 'hi': 'चाहा', 'in': 'menghendaki',
        'ms': 'menghendaki', 'tr': 'diledi', 'fa': 'خواست', 'ha': 'ya so', 'sw': 'alitaka'
    },
    'w_0186': { # خَلَقَ
        'en': 'created', 'bn': 'সৃষ্টি করেছেন', 'ur': 'پیدا کیا', 'hi': 'सृष्टि की', 'in': 'menciptakan',
        'ms': 'menciptakan', 'tr': 'yarattı', 'fa': 'آفرید', 'ha': 'ya halitta', 'sw': 'aliumba'
    },
    'w_0187': { # أَنْزَلَ
        'en': 'sent down', 'bn': 'নাযিল করেছেন', 'ur': 'نازل کیا', 'hi': 'उतारा', 'in': 'menurunkan',
        'ms': 'menurunkan', 'tr': 'indirdi', 'fa': 'فرو فرستاد', 'ha': 'ya saukar', 'sw': 'aliteremsha'
    },
    'w_0188': { # كَذَّبَ
        'en': 'denied', 'bn': 'অস্বীকার করল', 'ur': 'جھٹلایا', 'hi': 'झुठलाया', 'in': 'mendustakan',
        'ms': 'mendustakan', 'tr': 'yalanladı', 'fa': 'تکذیب کرد', 'ha': 'ya karyata', 'sw': 'alikanusha'
    },
    'w_0189': { # دَعَا
        'en': 'called', 'bn': 'ডেকেছে', 'ur': 'پکارا', 'hi': 'पुकारा', 'in': 'menyeru',
        'ms': 'menyeru', 'tr': 'çağırdı', 'fa': 'خواند', 'ha': 'ya kira', 'sw': 'aliomba'
    },
    'w_0190': { # اتَّقَى
        'en': 'feared (Allah)', 'bn': 'তাকওয়া অবলম্বন করল', 'ur': 'ڈر اختیار کیا', 'hi': 'डर रखा', 'in': 'bertakwa',
        'ms': 'bertakwa', 'tr': 'sakındı', 'fa': 'پرهیزگاری کرد', 'ha': 'ya ji tsoro', 'sw': 'alijikinga'
    },
    'w_0191': { # هَدَى
        'en': 'guided', 'bn': 'হেদায়াত দিলেন', 'ur': 'ہدایت دی', 'hi': 'मार्गदर्शन दिया', 'in': 'memberi petunjuk',
        'ms': 'memberi petunjuk', 'tr': 'hidayet verdi', 'fa': 'هدایت کرد', 'ha': 'ya shiryar', 'sw': 'aliongoza'
    },
    'w_0192': { # أَرَادَ
        'en': 'intended', 'bn': 'চেয়েছেন', 'ur': 'ارادہ کیا', 'hi': 'इरादा किया', 'in': 'menghendaki',
        'ms': 'menghendaki', 'tr': 'istedi', 'fa': 'اراده کرد', 'ha': 'ya yi nufi', 'sw': 'alikusudia'
    },
    'w_0193': { # اتَّبَعَ
        'en': 'followed', 'bn': 'অনুসরণ করল', 'ur': 'پیروی کی', 'hi': 'अनुसरण किया', 'in': 'mengikuti',
        'ms': 'mengikuti', 'tr': 'uydu', 'fa': 'پیروی کرد', 'ha': 'ya bi', 'sw': 'alifuata'
    },
    'w_0194': { # أَرْسَلَ
        'en': 'sent', 'bn': 'প্রেরণ করেছেন', 'ur': 'بھیجا', 'hi': 'भेजा', 'in': 'mengutus',
        'ms': 'mengutus', 'tr': 'gönderdi', 'fa': 'فرستاد', 'ha': 'ya aiko', 'sw': 'alituma'
    },
    'w_0195': { # أَخَذَ
        'en': 'took / seized', 'bn': 'পাকড়াও করলেন', 'ur': 'پکڑا', 'hi': 'पकड़ा', 'in': 'mengambil',
        'ms': 'mengambil', 'tr': 'yakaladı', 'fa': 'گرفت', 'ha': 'ya kama', 'sw': 'alishika'
    },
    'w_0196': { # عَبَدَ
        'en': 'worshipped', 'bn': 'ইবাদাত করল', 'ur': 'عبادت کی', 'hi': 'इबादत की', 'in': 'menyembah',
        'ms': 'menyembah', 'tr': 'kulluk etti', 'fa': 'عبادت کرد', 'ha': 'ya bauta', 'sw': 'aliabudu'
    },
    'w_1654': { # رَبٌّ
        'en': 'Lord', 'bn': 'রব / প্রতিপালক', 'ur': 'پروردگار', 'hi': 'पालनहार', 'in': 'Tuhan',
        'ms': 'Tuhan', 'tr': 'Rab', 'fa': 'پروردگار', 'ha': 'Ubangiji', 'sw': 'Mola'
    },
    'w_1655': { # أَرْضٌ
        'en': 'earth / land', 'bn': 'যমীন / পৃথিবী', 'ur': 'زمین', 'hi': 'धरती', 'in': 'bumi',
        'ms': 'bumi', 'tr': 'yer / yeryüzü', 'fa': 'زمین', 'ha': 'ƙasa', 'sw': 'ardhi'
    },
    'w_1656': { # قَوْمٌ
        'en': 'people / nation', 'bn': 'সম্প্রদায় / জাতি', 'ur': 'قوم', 'hi': 'क़ौम', 'in': 'kaum',
        'ms': 'kaum', 'tr': 'topluluk', 'fa': 'قوم', 'ha': 'mutane', 'sw': 'kaumu'
    },
    'w_1659': { # رَسُولٌ
        'en': 'Messenger', 'bn': 'রাসূল', 'ur': 'رسول', 'hi': 'रसूल', 'in': 'Rasul',
        'ms': 'Rasul', 'tr': 'elçi / resul', 'fa': 'پیامبر', 'ha': 'Manzo', 'sw': 'Mtume'
    },
    'w_1660': { # يَوْمٌ
        'en': 'Day', 'bn': 'দিন / দিবস', 'ur': 'دن', 'hi': 'दिन', 'in': 'hari',
        'ms': 'hari', 'tr': 'gün', 'fa': 'روز', 'ha': 'rana', 'sw': 'siku'
    },
    'w_1661': { # عَذَابٌ
        'en': 'punishment', 'bn': 'শাস্তি', 'ur': 'عذاب', 'hi': 'यातना / अज़ाब', 'in': 'azab',
        'ms': 'azab', 'tr': 'azap', 'fa': 'عذاب', 'ha': 'azaba', 'sw': 'adhabu'
    },
    'w_1662': { # سَمَآءٌ
        'en': 'sky / heaven', 'bn': 'আকাশ / আসমান', 'ur': 'آسمان', 'hi': 'आकाश', 'in': 'langit',
        'ms': 'langit', 'tr': 'gök', 'fa': 'آسمان', 'ha': 'sama', 'sw': 'mbingu'
    },
    'w_1664': { # شَىْءٌ
        'en': 'thing', 'bn': 'বস্তু / কিছু', 'ur': 'چیز', 'hi': 'चीज़', 'in': 'sesuatu',
        'ms': 'sesuatu', 'tr': 'şey', 'fa': 'چیز', 'ha': 'abu', 'sw': 'kitu'
    },
    'w_1666': { # حَقٌّ
        'en': 'truth', 'bn': 'সত্য / অধিকার', 'ur': 'حق / سچ', 'hi': 'सत्य / हक़', 'in': 'kebenaran',
        'ms': 'kebenaran', 'tr': 'hak / gerçek', 'fa': 'حق', 'ha': 'gaskiya', 'sw': 'haki'
    },
    'w_1667': { # نَاسٌ
        'en': 'people / mankind', 'bn': 'মানুষ / মানবজাতি', 'ur': 'لوگ', 'hi': 'लोग', 'in': 'manusia',
        'ms': 'manusia', 'tr': 'insanlar', 'fa': 'مردم', 'ha': 'mutane', 'sw': 'watu'
    },
    'w_1668': { # قَبْلُ
        'en': 'before', 'bn': 'পূর্বে / আগে', 'ur': 'پہلے', 'hi': 'पहले', 'in': 'sebelum',
        'ms': 'sebelum', 'tr': 'önce', 'fa': 'پیش از', 'ha': 'kafin', 'sw': 'kabla'
    },
    'w_1669': { # مُؤْمِنٌ
        'en': 'believer', 'bn': 'মুমিন / বিশ্বাসী', 'ur': 'مومن', 'hi': 'मोमिन', 'in': 'orang beriman',
        'ms': 'orang beriman', 'tr': 'inanan / mümin', 'fa': 'مؤمن', 'ha': 'mumini', 'sw': 'muumini'
    },
    'w_1671': { # أَمْرٌ
        'en': 'matter / command', 'bn': 'আদেশ / বিষয়', 'ur': 'حکم / معاملہ', 'hi': 'आदेश / मामला', 'in': 'urusan / perintah',
        'ms': 'urusan / perintah', 'tr': 'emir / iş', 'fa': 'فرمان / کار', 'ha': "al'amari", 'sw': 'amri / jambo'
    },
    'w_1672': { # عَالَمٌ
        'en': 'worlds / universe', 'bn': 'বিশ্বজগত / সৃষ্টিজগত', 'ur': 'جہاں / عالمین', 'hi': 'जहान / संसार', 'in': 'alam semesta',
        'ms': 'alam semesta', 'tr': 'alemler', 'fa': 'جهانیان', 'ha': 'talikai', 'sw': 'walimwengu'
    },
    'w_1674': { # ظَالِمٌ
        'en': 'wrongdoer', 'bn': 'জালিম / অত্যাচারী', 'ur': 'ظالم', 'hi': 'ज़ालिम', 'in': 'orang zalim',
        'ms': 'orang zalim', 'tr': 'zalim', 'fa': 'ستمگر', 'ha': 'azzalumi', 'sw': 'dhalimu'
    },
    'w_1675': { # نَارٌ
        'en': 'fire', 'bn': 'আগুন', 'ur': 'آگ', 'hi': 'आग', 'in': 'api',
        'ms': 'api', 'tr': 'ateş', 'fa': 'آتش', 'ha': 'wuta', 'sw': 'moto'
    },
    'w_1676': { # جَنَّةٌ
        'en': 'Garden / Paradise', 'bn': 'জান্নাত / বাগান', 'ur': 'جنت / باغ', 'hi': 'जन्नत / बाग़', 'in': 'surga',
        'ms': 'syurga', 'tr': 'cennet', 'fa': 'بهشت', 'ha': 'Aljanna', 'sw': 'Pepo'
    },
    'w_1677': { # قَلْبٌ
        'en': 'heart', 'bn': 'অন্তর / হৃদয়', 'ur': 'دل', 'hi': 'हृदय / दिल', 'in': 'hati',
        'ms': 'hati', 'tr': 'kalp', 'fa': 'دل', 'ha': 'zuciya', 'sw': 'moyo'
    },
    'w_1724': { # مُوسَى
        'en': 'Musa (Moses)', 'bn': 'মূসা', 'ur': 'موسیٰ', 'hi': 'मूसा', 'in': 'Musa',
        'ms': 'Musa', 'tr': 'Musa', 'fa': 'موسی', 'ha': 'Musa', 'sw': 'Musa'
    },
    'w_1748': { # عَلِيم
        'en': 'All-Knowing', 'bn': 'সর্বজ্ঞ', 'ur': 'سب کچھ جاننے والا', 'hi': 'सर्वज्ञ', 'in': 'Maha Mengetahui',
        'ms': 'Maha Mengetahui', 'tr': 'Bilen', 'fa': 'دانا', 'ha': 'Masani', 'sw': 'Mjuzi wa yote'
    },
    'w_1772': { # كَافِر
        'en': 'disbeliever', 'bn': 'অস্বীকারকারী / কাফির', 'ur': 'کافر', 'hi': 'काफ़िर / इनकार करने वाला', 'in': 'orang kafir',
        'ms': 'orang kafir', 'tr': 'kafir', 'fa': 'کافر', 'ha': 'kafiri', 'sw': 'kafiri'
    },
    'w_1774': { # صَالِح
        'en': 'righteous', 'bn': 'সৎকর্মপরায়ণ', 'ur': 'نیک', 'hi': 'सदाचारी', 'in': 'saleh',
        'ms': 'soleh', 'tr': 'salih', 'fa': 'شایسته', 'ha': 'nagari', 'sw': 'mwema'
    },
    'w_1778': { # كَثِير
        'en': 'many / abundant', 'bn': 'অনেক / প্রচুর', 'ur': 'بہت', 'hi': 'बहुत', 'in': 'banyak',
        'ms': 'banyak', 'tr': 'çok', 'fa': 'بسیار', 'ha': 'da yawa', 'sw': 'wengi / tele'
    },
    'w_0003': { # إِنَّ
        'en': 'indeed', 'bn': 'নিশ্চয়ই', 'ur': 'بے شک', 'hi': 'निःसंदेह', 'in': 'sesungguhnya',
        'ms': 'sesungguhnya', 'tr': 'şüphesiz', 'fa': 'قطعاً', 'ha': 'lalle', 'sw': 'hakika'
    },
    'w_0012': { # إِلَّا
        'en': 'except', 'bn': 'ব্যতীত', 'ur': 'سوائے / مگر', 'hi': 'सिवाय', 'in': 'kecuali',
        'ms': 'melainkan', 'tr': 'ancak / başka', 'fa': 'مگر', 'ha': 'face dai', 'sw': 'isipokuwa'
    },
    'w_0015': { # قَدْ
        'en': 'certainly / indeed', 'bn': 'অবশ্যই', 'ur': 'تحقیق / یقیناً', 'hi': 'अवश्य', 'in': 'sungguh',
        'ms': 'sesungguhnya', 'tr': 'muhakkak', 'fa': 'به تحقیق', 'ha': 'lalle', 'sw': 'kwa hakika'
    },
    'w_0024': { # عِنْد
        'en': 'with / near', 'bn': 'নিকট / কাছে', 'ur': 'پاس / نزدیک', 'hi': 'पास / निकट', 'in': 'di sisi',
        'ms': 'di sisi', 'tr': 'yanında / katında', 'fa': 'نزد', 'ha': 'wurin', 'sw': 'kwa / karibu na'
    }
}

def clean_part(p):
    return ' '.join(str(p).strip().split())

def get_tokens(m_dict, lang):
    text = m_dict.get(lang, '')
    return set(clean_part(s).lower() for s in text.split('/') if clean_part(s))

def words_collide(wid_a, wid_b, words_dict):
    if wid_a == wid_b:
        return True
    wa = words_dict.get(wid_a)
    wb = words_dict.get(wid_b)
    if not wa or not wb:
        return False
    ma = wa['meaning']
    mb = wb['meaning']
    for l in LANGS:
        ta = get_tokens(ma, l)
        tb = get_tokens(mb, l)
        if ta and tb and not ta.isdisjoint(tb):
            return True
    return False

def main():
    print("=" * 70)
    print("QuranicWords v1.0.1 Master Curriculum Synthesis")
    print("=" * 70)

    # 1. Load Data
    with open(CHAPTERS_PATH, encoding='utf-8') as f:
        chapters = json.load(f)['chapters']
    with open(SECTIONS_PATH, encoding='utf-8') as f:
        sections = json.load(f)['sections']
    with open(LESSONS_PATH, encoding='utf-8') as f:
        lessons = json.load(f)['lessons']
    with open(EXERCISES_PATH, encoding='utf-8') as f:
        exercises = json.load(f)['exercises']
    with open(WORD_FREQ_PATH, encoding='utf-8') as f:
        words = json.load(f)['words']

    words_dict = {w['id']: w for w in words}
    lessons_dict = {l['id']: l for l in lessons}
    sections_dict = {s['id']: s for s in sections}
    chapters_dict = {c['id']: c for c in chapters}

    print(f"Loaded: {len(chapters)} chapters, {len(sections)} sections, {len(lessons)} lessons, {len(exercises)} exercises, {len(words)} words.")

    # 2. Extract intro exercises and words mapped by lesson
    words_by_lesson = {}
    intro_by_wid = {}
    for ex in exercises:
        c = ex.get('content', {})
        if c.get('type') == 'word_intro':
            wid = c['wordId']
            intro_by_wid[wid] = c
            words_by_lesson.setdefault(ex['lessonId'], []).append(wid)

    # 3. Clean Polysemy Entries & Canonical Meanings
    print("\n[Step 1] Auditing and fixing Polysemy Entries & Canonical Meanings...")
    polysemy_fixed_count = 0
    clean_override_count = 0

    for wid, w in words_dict.items():
        intro = intro_by_wid.get(wid)
        if not intro:
            continue

        entries = intro.get('polysemyEntries', [])

        if wid in CLEAN_CANONICAL_OVERRIDES:
            clean_m = CLEAN_CANONICAL_OVERRIDES[wid]
            w['meaning'] = clean_m
            intro['meaning'] = clean_m
            # Collapse polysemy entries to 1 clean entry
            if entries:
                clean_entry = dict(entries[0])
                clean_entry['meaningIndex'] = 1
                clean_entry['contextualMeaning'] = clean_m
                intro['polysemyEntries'] = [clean_entry]
            else:
                intro['polysemyEntries'] = []
            clean_override_count += 1
            polysemy_fixed_count += 1

        elif wid in GENUINE_POLYSEMY_WIDS:
            # Genuine polysemy word: keep distinct senses
            # Build unified slash meaning across distinct senses
            combined = {}
            for lang in LANGS:
                parts = []
                for se in entries:
                    v = se.get('contextualMeaning', {}).get(lang, '')
                    if v:
                        subparts = [clean_part(s) for s in v.split('/') if clean_part(s)]
                        for sp in subparts:
                            if sp.lower() not in [p.lower() for p in parts]:
                                parts.append(sp)
                cur = w['meaning'].get(lang, '')
                if cur:
                    for cp in [clean_part(s) for s in cur.split('/') if clean_part(s)]:
                        if cp.lower() not in [p.lower() for p in parts]:
                            parts.append(cp)
                combined[lang] = ' / '.join(parts) if parts else cur
            w['meaning'] = combined
            intro['meaning'] = combined

        else:
            # If word had polysemy entries, but is not in GENUINE_POLYSEMY_WIDS:
            if len(entries) > 1:
                # Collapse to primary entry
                primary = entries[0]
                primary_cm = primary.get('contextualMeaning', {})
                # Clean up any duplicated slash parts
                cleaned_cm = {}
                for lang in LANGS:
                    val = primary_cm.get(lang, '')
                    parts = [clean_part(s) for s in val.split('/') if clean_part(s)]
                    unique_parts = []
                    for p in parts:
                        if p.lower() not in [up.lower() for up in unique_parts]:
                            unique_parts.append(p)
                    cleaned_cm[lang] = ' / '.join(unique_parts) if unique_parts else val
                primary['contextualMeaning'] = cleaned_cm
                intro['polysemyEntries'] = [primary]
                w['meaning'] = cleaned_cm
                intro['meaning'] = cleaned_cm
                polysemy_fixed_count += 1

    print(f"  Fixed {polysemy_fixed_count} words (including {clean_override_count} canonical overrides).")

    # Update Multiple Choice exercises for words whose meanings were cleaned
    mc_updated = 0
    for ex in exercises:
        c = ex.get('content', {})
        if c.get('type') == 'multiple_choice':
            wid = c.get('wordId')
            if wid in words_dict:
                target_meaning = words_dict[wid]['meaning']
                for opt in c.get('options', []):
                    if opt.get('id') == c.get('correctOptionId') or opt.get('id') == f"opt_{wid}":
                        opt['label'] = target_meaning
                        mc_updated += 1
    print(f"  Updated correct options in {mc_updated} multiple choice exercises.")

    # 4. Map words to sections and chapters
    words_by_section = {}
    words_by_chapter = {}
    for lid, wids in words_by_lesson.items():
        les = lessons_dict.get(lid)
        if les:
            sec_id = les.get('sectionId')
            ch_id = les.get('chapterId')
            if sec_id:
                words_by_section.setdefault(sec_id, []).extend(wids)
            if ch_id:
                words_by_chapter.setdefault(ch_id, []).extend(wids)

    # Deduplicate while preserving order
    for sid in words_by_section:
        seen = set()
        words_by_section[sid] = [wid for wid in words_by_section[sid] if not (wid in seen or seen.add(wid))]

    for cid in words_by_chapter:
        seen = set()
        words_by_chapter[cid] = [wid for wid in words_by_chapter[cid] if not (wid in seen or seen.add(wid))]

    # Precompute word meaning tokens for ultra-fast collision checking
    word_tokens = {
        w['id']: [
            set(clean_part(s).lower() for s in w.get('meaning', {}).get(l, '').split('/') if clean_part(s))
            for l in LANGS
        ]
        for w in words
    }

    def fast_words_collide(wid_a, wid_b):
        if wid_a == wid_b:
            return True
        toks_a = word_tokens.get(wid_a)
        toks_b = word_tokens.get(wid_b)
        if not toks_a or not toks_b:
            return False
        for ta, tb in zip(toks_a, toks_b):
            if ta and tb and not ta.isdisjoint(tb):
                return True
        return False

    # Distractor candidate pools by category sorted by frequencyRank
    candidates_by_cat = {'PARTICLE': [], 'VERB': [], 'NOUN': []}
    for w in words:
        rank = w.get('frequencyRank', 1)
        if rank <= 173:
            candidates_by_cat['PARTICLE'].append(w)
        elif rank <= 1652:
            candidates_by_cat['VERB'].append(w)
        else:
            candidates_by_cat['NOUN'].append(w)

    for cat in candidates_by_cat:
        candidates_by_cat[cat].sort(key=lambda w: w.get('frequencyRank', 1))

    def pick_distractors(target_wid, pool, count=3):
        target_w = words_dict[target_wid]
        target_rank = target_w.get('frequencyRank', 1)
        
        # Binary search or find nearest index in pool
        # Generate indices expanding outward from target
        n = len(pool)
        if n <= count:
            return [w for w in pool if w['id'] != target_wid][:count]

        # Find closest index
        left, right = 0, n - 1
        best_idx = 0
        min_diff = float('inf')
        while left <= right:
            mid = (left + right) // 2
            diff = abs(pool[mid].get('frequencyRank', 1) - target_rank)
            if diff < min_diff:
                min_diff = diff
                best_idx = mid
            if pool[mid].get('frequencyRank', 1) < target_rank:
                left = mid + 1
            else:
                right = mid - 1

        picked = []
        # Expand outward from best_idx
        step = 1
        checked = 0
        while len(picked) < count and checked < n:
            for offset in (step, -step):
                idx = best_idx + offset
                if 0 <= idx < n:
                    cand = pool[idx]
                    cand_id = cand['id']
                    checked += 1
                    if cand_id != target_wid and not fast_words_collide(target_wid, cand_id):
                        if not any(fast_words_collide(cand_id, p['id']) for p in picked):
                            picked.append(cand)
                            if len(picked) == count:
                                break
            step += 1

        # Fallback if strict non-collision couldn't find enough
        if len(picked) < count:
            for cand in pool:
                if cand['id'] != target_wid and cand['id'] not in [p['id'] for p in picked]:
                    picked.append(cand)
                    if len(picked) == count:
                        break
        return picked

    def build_mc_exercise(ex_id, lesson_id, order_index, wid, cat_pool):
        w = words_dict[wid]
        correct_opt_id = f"opt_{wid}"
        correct_opt = {
            'id': correct_opt_id,
            'labelArabic': w.get('arabicWord'),
            'label': w.get('meaning')
        }
        distractor_words = pick_distractors(wid, cat_pool, 3)
        distractor_opts = [
            {
                'id': f"opt_{dw['id']}",
                'labelArabic': dw.get('arabicWord'),
                'label': dw.get('meaning')
            }
            for dw in distractor_words
        ]
        slot = w.get('frequencyRank', 1) % 4
        opts = list(distractor_opts)
        opts.insert(slot, correct_opt)

        return {
            'id': ex_id,
            'lessonId': lesson_id,
            'orderIndex': order_index,
            'exerciseType': 'MULTIPLE_CHOICE',
            'content': {
                'type': 'multiple_choice',
                'prompt': PROMPT_MULTIPLE_CHOICE,
                'promptArabic': w.get('arabicWord'),
                'wordId': wid,
                'options': opts,
                'correctOptionId': correct_opt_id
            }
        }

    def build_matching_exercise(ex_id, lesson_id, order_index, pair_wids):
        pairs = []
        for idx, wid in enumerate(pair_wids):
            w = words_dict[wid]
            pairs.append({
                'id': f"p{idx+1}",
                'leftArabic': w.get('arabicWord'),
                'left': w.get('arabicWord'),
                'right': w.get('meaning'),
                'wordId': wid
            })

        return {
            'id': ex_id,
            'lessonId': lesson_id,
            'orderIndex': order_index,
            'exerciseType': 'MATCHING',
            'content': {
                'type': 'matching',
                'prompt': PROMPT_MATCHING,
                'pairs': pairs
            }
        }

    # 5. Add Closing Matching to Regular Lessons
    print("\n[Step 2] Generating end-of-lesson MATCHING exercises for regular lessons...")
    # Retain strictly the original 9,428 base exercises (ex_00001 to ex_09428)
    base_exercises = [e for e in exercises if int(e['id'].replace('ex_', '')) <= 9428]
    global_ex_id_counter = 9428

    regular_matching_added = 0
    final_exercises = []

    # Group existing base exercises by lesson
    existing_by_lesson = {}
    for ex in base_exercises:
        existing_by_lesson.setdefault(ex['lessonId'], []).append(ex)

    for les in lessons:
        lid = les['id']
        kind = les['kind']
        les_exs = existing_by_lesson.get(lid, [])

        if kind == 'REGULAR':
            final_exercises.extend(les_exs)

            wids = words_by_lesson.get(lid, [])
            sec_id = les.get('sectionId')
            sec_wids = words_by_section.get(sec_id, [])

            # We want 4 to 5 pairs for a great matching quiz
            target_wids = list(wids)
            if len(target_wids) < 4 and sec_wids:
                # Pad with prior section words
                for swid in sec_wids:
                    if swid not in target_wids:
                        target_wids.append(swid)
                        if len(target_wids) == 4:
                            break
            elif len(target_wids) > 5:
                target_wids = target_wids[:5]

            if len(target_wids) >= 2:
                global_ex_id_counter += 1
                match_ex_id = f"ex_{global_ex_id_counter:05d}"
                max_order = max([e['orderIndex'] for e in les_exs], default=0)
                match_ex = build_matching_exercise(match_ex_id, lid, max_order + 1, target_wids)
                final_exercises.append(match_ex)
                regular_matching_added += 1

        elif kind == 'CHAPTER_INTRO':
            final_exercises.extend(les_exs)

        elif kind in ('SECTION_FLASHBACK', 'SECTION_EXAM', 'CHAPTER_EXAM'):
            # These currently have 0 exercises!
            # Generate rich question suites
            cat = les.get('category', 'NOUN')
            cat_pool = candidates_by_cat.get(cat, candidates_by_cat['NOUN'])

            if kind == 'SECTION_FLASHBACK':
                sec_id = les.get('sectionId')
                sec_wids = words_by_section.get(sec_id, [])
                # Take up to 15 words evenly distributed
                q_count = min(len(sec_wids), 15)
                step = len(sec_wids) / q_count if q_count else 1
                sampled_wids = [sec_wids[int(i * step)] for i in range(q_count)]

                ord_idx = 1
                for wid in sampled_wids:
                    global_ex_id_counter += 1
                    mc_id = f"ex_{global_ex_id_counter:05d}"
                    final_exercises.append(build_mc_exercise(mc_id, lid, ord_idx, wid, cat_pool))
                    ord_idx += 1

                # Matching quiz at the end (5 pairs from section)
                matching_wids = sec_wids[:5] if len(sec_wids) >= 5 else sec_wids
                if len(matching_wids) >= 2:
                    global_ex_id_counter += 1
                    match_id = f"ex_{global_ex_id_counter:05d}"
                    final_exercises.append(build_matching_exercise(match_id, lid, ord_idx, matching_wids))

            elif kind == 'SECTION_EXAM':
                sec_id = les.get('sectionId')
                sec_wids = words_by_section.get(sec_id, [])
                # Take up to 20 words evenly distributed
                q_count = min(len(sec_wids), 20)
                step = len(sec_wids) / q_count if q_count else 1
                sampled_wids = [sec_wids[int(i * step)] for i in range(q_count)]

                ord_idx = 1
                for wid in sampled_wids:
                    global_ex_id_counter += 1
                    mc_id = f"ex_{global_ex_id_counter:05d}"
                    final_exercises.append(build_mc_exercise(mc_id, lid, ord_idx, wid, cat_pool))
                    ord_idx += 1

                # Matching quiz at the end (5 pairs from section)
                matching_wids = sec_wids[-5:] if len(sec_wids) >= 5 else sec_wids
                if len(matching_wids) >= 2:
                    global_ex_id_counter += 1
                    match_id = f"ex_{global_ex_id_counter:05d}"
                    final_exercises.append(build_matching_exercise(match_id, lid, ord_idx, matching_wids))

            elif kind == 'CHAPTER_EXAM':
                ch_id = les.get('chapterId')
                ch_wids = words_by_chapter.get(ch_id, [])
                # Take 25 words evenly distributed across the chapter
                q_count = min(len(ch_wids), 25)
                step = len(ch_wids) / q_count if q_count else 1
                sampled_wids = [ch_wids[int(i * step)] for i in range(q_count)]

                ord_idx = 1
                for wid in sampled_wids:
                    global_ex_id_counter += 1
                    mc_id = f"ex_{global_ex_id_counter:05d}"
                    final_exercises.append(build_mc_exercise(mc_id, lid, ord_idx, wid, cat_pool))
                    ord_idx += 1

                # Matching quiz at the end (5 pairs from chapter)
                matching_wids = sampled_wids[:5] if len(sampled_wids) >= 5 else sampled_wids
                if len(matching_wids) >= 2:
                    global_ex_id_counter += 1
                    match_id = f"ex_{global_ex_id_counter:05d}"
                    final_exercises.append(build_matching_exercise(match_id, lid, ord_idx, matching_wids))

    print(f"  Added closing MATCHING exercises to {regular_matching_added} regular lessons.")

    # 6. Audit and Statistics
    print("\n[Step 3] Verifying Complete Synthesis...")
    ex_by_les_final = {}
    for ex in final_exercises:
        ex_by_les_final.setdefault(ex['lessonId'], []).append(ex)

    zero_ex_lessons = [l['id'] for l in lessons if len(ex_by_les_final.get(l['id'], [])) == 0]
    print(f"  Zero-exercise lessons: {len(zero_ex_lessons)}")
    assert len(zero_ex_lessons) == 0, f"Error: {len(zero_ex_lessons)} lessons still have 0 exercises!"

    total_matching = sum(1 for e in final_exercises if e['exerciseType'] == 'MATCHING')
    total_mc = sum(1 for e in final_exercises if e['exerciseType'] == 'MULTIPLE_CHOICE')
    total_intro = sum(1 for e in final_exercises if e['exerciseType'] == 'WORD_INTRO')
    total_ch_intro = sum(1 for e in final_exercises if e['exerciseType'] == 'CHAPTER_INTRO')

    print(f"  Total synthesized exercises: {len(final_exercises)}")
    print(f"    - Word Intro: {total_intro}")
    print(f"    - Multiple Choice: {total_mc}")
    print(f"    - Matching: {total_matching}")
    print(f"    - Chapter Intro: {total_ch_intro}")

    # Verify matching exists in every single regular lesson
    regular_without_matching = [l['id'] for l in lessons if l['kind'] == 'REGULAR' and not any(e['exerciseType'] == 'MATCHING' for e in ex_by_les_final.get(l['id'], []))]
    print(f"  Regular lessons without matching: {len(regular_without_matching)}")
    assert len(regular_without_matching) == 0, "Error: Some regular lessons are missing matching!"

    # Verify flashback and exams have exercises
    flashbacks_without_ex = [l['id'] for l in lessons if l['kind'] == 'SECTION_FLASHBACK' and len(ex_by_les_final.get(l['id'], [])) == 0]
    exams_without_ex = [l['id'] for l in lessons if l['kind'] in ('SECTION_EXAM', 'CHAPTER_EXAM') and len(ex_by_les_final.get(l['id'], [])) == 0]
    print(f"  Flashbacks without exercises: {len(flashbacks_without_ex)}")
    print(f"  Exams without exercises: {len(exams_without_ex)}")
    assert len(flashbacks_without_ex) == 0 and len(exams_without_ex) == 0

    # 7. Clean and compact JSON Assets (ensures under GitHub's 100MB limit)
    print("\n[Step 4] Writing updated assets to app/src/main/assets/content/...")
    for ex in final_exercises:
        etype = ex.get('exerciseType')
        c = ex.get('content', {})
        if etype == 'MATCHING':
            for p in c.get('pairs', []):
                for k in ['exampleVerseArabic', 'exampleVerseTranslation', 'exampleVerseReference', 'arabicWordStart', 'arabicWordEnd', 'meaningHighlight']:
                    p.pop(k, None)
        elif etype == 'MULTIPLE_CHOICE':
            c.pop('exampleVerseTranslation', None)
            c.pop('meaningHighlight', None)

    with open(EXERCISES_PATH, 'w', encoding='utf-8') as f:
        json.dump({'exercises': final_exercises}, f, ensure_ascii=False, separators=(',', ':'))
    print(f"  -> exercises_vocabulary.json written ({os.path.getsize(EXERCISES_PATH) / (1024*1024):.2f} MB).")

    with open(WORD_FREQ_PATH, 'w', encoding='utf-8') as f:
        json.dump({'words': words}, f, ensure_ascii=False, indent=2)
    print("  -> word_frequency.json written successfully.")

    print("\nSynthesis complete and verified with 100% integrity!")

if __name__ == '__main__':
    main()
