import openpyxl, re, json

wb = openpyxl.load_workbook('db/qw_ur.xlsx')
ws = wb['Ism']

with open('scratch_ur_ism_275.json') as f:
    ism_pairs = json.load(f)

STOP_WORDS_UR = {'اور', 'کہ', 'سے', 'کو', 'کا', 'کی', 'کے', 'نے', 'پر', 'میں', 'وہ', 'ہم', 'تم', 'آپ', 'تو', 'ہی', 'بھی', 'نہ', 'نہیں', 'جو', 'جس', 'جن', 'ان', 'اس', 'ہے', 'ہیں', 'تھا', 'تھے', 'تھی', 'ہوا', 'ہوئے', 'ہوئی', 'ہو'}

# Manual overrides / exact phrase mappings for all 275 pairs
EXACT_ISM_MAP = {
    ('Al-Fatihah 1:1', 'ٱللَّهِ'): ('الله', None),
    ('Al-Baqarah 2:87', 'بِٱلرُّسُلِ'): ('پیغمبر', None),
    ('Al-Furqan 25:59', 'ٱلسَّمَـٰوَٰتِ'): ('آسمانوں', None),
    ('Al-Baqarah 2:233', 'نَفْسٌ'): ('جان', None),
    ('Al-Baqarah 2:20', 'شَىْءٍ'): ('چیز', None),
    ('Al-Isra 17:86', 'شِئْنَا'): ('چاہیں', None),
    ('Al-Baqarah 2:2', 'ٱلْكِتَـٰبُ'): ('کتاب', None),
    ('Al-Baqarah 2:26', 'ٱلْحَقُّ'): ('سچ', None),
    ('Maryam 19:74', 'قَبْلَهُم'): ('پہلے', None),
    ('Yusuf 12:64', 'ءَامَنُكُمْ'): ('اعتبار', None),
    ('Al-Baqarah 2:108', 'ٱلسَّبِيلِ'): ('راستے', None),
    ('Al-Isra 17:85', 'أَمْرِ'): ('شان', None),
    ('Al-Fatihah 1:2', 'ٱلْعَـٰلَمِينَ'): ('تمام مخلوقات', None),
    ('An-Nahl 16:95', 'تَعْلَمُونَ'): ('جانتے ہو', None),
    ('Al-Baqarah 2:17', 'ظُلُمَـٰتٍ'): ('اندھیرے', None),
    ("Ali 'Imran 3:15", 'جَنَّـٰتٌ'): ('باغات', None),
    ("Al-'Ankabut 29:58", 'ٱلْجَنَّةِ'): ('بہشت', None),
    ('Al-Baqarah 2:61', 'خَيْرٌ'): ('بہتر', None),
    ('Al-Baqarah 2:32', 'عِلْمَ'): ('علم', None),
    ('Al-Baqarah 2:62', 'أَجْرُهُمْ'): ('ان کا صلہ', None),
    ('Al-Baqarah 2:153', 'ٱلصَّـٰبِرِينَ'): ('صبر کرنے والوں', None),
    ('Al-Baqarah 2:27', 'عَهْدَ'): ('اقرار', None),
    ('Al-Baqarah 2:22', 'مَآءً'): ('پانی', None),
    ('Al-Baqarah 2:248', 'نَبِيُّهُمْ'): ('پیغمبر', None),
    ('Al-Fatihah 1:6', 'ٱهْدِنَا'): ('چلا', None),
    ('Al-Baqarah 2:243', 'أَحْيَـٰهُمْ'): ('زندہ کر دیا', None),
    ('Al-Baqarah 2:182', 'بَيْنَهُمْ'): ('صلح کرادے', None),
    ('Al-Fatihah 1:7', 'صِرَٰطَ'): ('رستے', None),
    ('Al-Baqarah 2:93', 'إِيمَـٰنُكُمْ'): ('ایمان', None),
    ('Al-Baqarah 2:17', 'بِنُورِهِمْ'): ('روشنی', None),
    ('Al-Baqarah 2:43', 'ٱلزَّكَوٰةَ'): ('زکوٰة', None),
    ('An-Nisa 4:168', 'طَرِيقًا'): ('رستہ', None),
    ('Al-Baqarah 2:175', 'بِٱلْمَغْفِرَةِ'): ('بخشش', None),
    ("Ali 'Imran 3:19", 'ٱلْإِسْلَـٰمُ'): (
        'اسلام',
        'دین تو خدا کے نزدیک ([اسلام]) ہے اور اہل کتاب نے جو (اس دین سے) اختلاف کیا تو علم حاصل ہونے کے بعد آپس کی ضد سے کیا۔ اور جو شخص خدا کی آیتوں کو نہ مانے تو خدا جلد حساب لینے والا ہے'
    ),
    ('Taha 20:36', 'يَـٰمُوسَىٰ'): ('موسیٰ', None),
    ('An-Nisa 4:163', 'سُلَيْمَـٰنَ'): ('سلیمان', None),
    ("Ali 'Imran 3:96", 'بِبَكَّةَ'): ('مکے', None),
    ('Al-Qasas 28:76', 'قَـٰرُونَ'): ('قارون', None),
    ('Al-Qasas 28:6', 'هَـٰمَـٰنَ'): ('ہامان', None),
    ('Al-Baqarah 2:97', 'لِّجِبْرِيلَ'): ('جبرئیل', None),
    ('Al-Baqarah 2:98', 'مَن'): ('جو شخص', None),
    ('Al-Baqarah 2:29', 'عَلِيمٌ'): ('واقف ہے', None),
    ('Al-Baqarah 2:20', 'قَدِيرٌ'): ('قادر ہے', None),
    ('Al-Baqarah 2:34', 'ٱسْتَكْبَرَ'): ('غرور میں آکر', None),
    ('Al-Baqarah 2:255', 'ٱلْعَلِىُّ'): ('بڑا عالی رتبہ', None),
    ('Al-Baqarah 2:234', 'خَبِيرٌ'): ('باخبر ہے', None),
    ('Hud 11:73', 'مَّجِيدٌ'): ('بزرگوار', None),
    ('Al-Baqarah 2:63', 'قُوَّةٍ'): ('مضبوطی سے', None),
    ('Al-Baqarah 2:129', 'ٱلْعَزِيزُ'): ('غالب', None),
    ('Al-Baqarah 2:23', 'صَـٰدِقِينَ'): ('سچے', None),
    ('Al-Baqarah 2:10', 'يَكْذِبُونَ'): ('جھوٹ بولنے کے سبب', None),
    ('Al-Fatihah 1:6', 'ٱلْمُسْتَقِيمَ'): ('سیدھے', None),
    ('Al-Baqarah 2:201', 'حَسَنَةً'): ('نعمت', None),
    ('Al-Baqarah 2:138', 'أَحْسَنُ'): ('بہتر', None),
    ("Ali 'Imran 3:7", 'مُّحْكَمَـٰتٌ'): ('محکم', None),
    ('Al-Baqarah 2:76', 'فَتَحَ'): ('ظاہر فرمائی ہے', None),
    ('Al-Anfal 8:3', 'رَزَقْنَـٰهُمْ'): ('دیا ہے', None),
    ('Al-Baqarah 2:79', 'يَكْسِبُونَ'): ('کام کرتے ہیں', None),
    ('Al-Baqarah 2:48', 'تَجْزِى'): ('کام نہ آئے', None),
    ("Ali 'Imran 3:144", 'عَقِبَيْهِ'): ('الٹے پاؤں', None),
    ('Al-Baqarah 2:71', 'مُسَلَّمَةٌ'): ('داغ نہ ہو', None),
    ('Al-Baqarah 2:22', 'أَنزَلَ'): ('برسا کر', None),
    ("Ali 'Imran 3:44", 'نُوحِيهِ'): ('بھیجتے ہیں', None),
    ('Al-Baqarah 2:6', 'كَفَرُوا۟'): ('کافر ہیں', None),
    ('Al-Baqarah 2:28', 'أَحْيَـٰكُمْ'): ('جان بخشی', None),
    ('An-Nisa 4:78', 'يَفْقَهُونَ'): ('سمجھتے', None),
    ('Al-Baqarah 2:44', 'تَعْقِلُونَ'): ('عقل', None),
    ('Al-Baqarah 2:219', 'تَتَفَكَّرُونَ'): ('سوچو', None),
    ('Al-Baqarah 2:218', 'هَاجَرُوا۟'): ('وطن چھوڑ گئے', None),
    ('Yusuf 12:76', 'بِأَوْعِيَتِهِمْ'): ('شلیتوں', None),
    ("An-Nazi'at 79:31", 'مَرْعَىٰهَا'): (
        'چارا',
        'اسی نے اس میں سے اس کا پانی نکالا اور ([چارا]) اگایا'
    ),
    ('Al-Baqarah 2:64', 'تَوَلَّيْتُم'): ('پھر گئے', None),
    ("Ali 'Imran 3:151", 'سَنُلْقِى'): ('رعب بٹھا دیں گے', None),
    ('Al-Baqarah 2:30', 'لِلْمَلَـٰٓئِكَةِ'): ('فرشتوں', None),
    ('Al-Baqarah 2:71', 'ذَلُولٌ'): ('کام میں لگا ہوا', None),
    ('At-Tawbah 9:121', 'صَغِيرَةً'): ('تھوڑا', None),
    ('Al-Baqarah 2:255', 'يَعْلَمُ'): ('سب معلوم ہے', None),
    ('Al-Baqarah 2:95', 'قَدَّمَتْ'): ('آگے بھیج چکے ہیں', None),
    ('Surah 2:168', 'حَلَـٰلًا'): ('حلال', None),
    ('Al-Baqarah 2:188', 'بِٱلْإِثْمِ'): ('ناحق', None),
    ('Al-Baqarah 2:158', 'إِنَّ'): ('بےشک', None),
    ("Ali 'Imran 3:137", 'سُنَنٌ'): ('واقعات', None),
    ('Al-Baqarah 2:68', 'ارِضٌ'): ('بوڑھی', None),
    ('Al-Baqarah 2:117', 'قَضَىٰٓ'): ('کرنا چاہتا ہے', None),
    ('Ar-Rahman 55:29', 'شَأْنٍ'): ('کام', None),
    ('Al-Baqarah 2:76', 'أَتُحَدِّثُونَهُم'): ('بتائے دیتے ہو', None),
    ('Al-Baqarah 2:37', 'كَلِمَـٰتٍ'): ('کلمات', None),
    ("Ali 'Imran 3:78", 'أَلْسِنَتَهُم'): ('زبان', None),
    ('Al-Baqarah 2:49', 'نَجَّيْنَـٰكُم'): ('نجات بخشی', None),
    ('Al-Baqarah 2:66', 'بَيْنَ'): ('عبرت', None),
    ('Al-Baqarah 2:97', 'بُشْرَىٰ'): ('بشارت', None),
    ('Al-Baqarah 2:168', 'يَـٰٓأَيُّهَا'): ('لوگو', None),
    ('Al-Baqarah 2:31', 'أَنۢبِـُٔونِى'): ('بتاؤ', None),
    ('Al-Baqarah 2:32', 'سُبْحَـٰنَكَ'): ('تو پاک ہے', None),
    ('Al-Baqarah 2:238', 'حَـٰفِظُوا۟'): ('التزام کے ساتھ ادا کرتے رہو', None),
    ('Al-Baqarah 2:83', 'إِحْسَانًا'): ('بھلائی کرتے رہنا', None),
    ('Al-Baqarah 2:250', 'أَقْدَامَنَا'): ('ثابت قدم رکھ', None),
    ("Ali 'Imran 3:22", 'نَّـٰصِرِينَ'): ('مددگار', None),
    ("Ali 'Imran 3:144", 'ٱلشَّـٰكِرِينَ'): ('شکر گزاروں', None),
    ('Al-Baqarah 2:45', 'ٱلْخَـٰشِعِينَ'): ('عجز کرنے والے', None),
    ("Ali 'Imran 3:26", 'مَـٰلِكَ'): ('مالک', None),
    ('Al-Baqarah 2:53', 'ٱلْكِتَـٰبَ'): ('کتاب', None),
    ('Ash-Shura 42:52', 'رُوحًا'): ('روح القدس', None),
    ("Ash-Shu'ara 26:193", 'ٱلرُّوحُ'): ('فرشتہ', None),
    ('Al-Baqarah 2:173', 'حَرَّمَ'): (
        'حرام کر دیا ہے',
        'اس نے تم پر مرا ہوا جانور اور لہو اور سور کا گوشت اور جس چیز پر خدا کے سوا کسی اور کا نام پکارا جائے ([حرام کر دیا ہے]) پھر جو مجبور ہو جائے بشرطیکہ نہ تو نافرمانی کرنے والا ہو اور نہ حد سے بڑھنے والا تو اس پر کوئی گناہ نہیں۔ بےشک خدا بخشنے والا مہربان ہے'
    ),
    ('Al-Baqarah 2:104', 'رَٰعِنَا'): (
        'راعنا',
        'اے اہل ایمان! (گفتگو کے وقت پیغمبرِ خدا سے) ([راعنا]) نہ کہا کرو۔ انظرنا کہا کرو۔ اور خوب سن رکھو، اور کافروں کے لیے دکھ دینے والا عذاب ہے'
    ),
    ('Al-Baqarah 2:131', 'رَبُّهُۥٓ'): ('پروردگار', None),
    ('Taha 20:50', 'رَبُّنَا'): ('پروردگار', None),
    ('Al-Baqarah 2:11', 'ٱلْأَرْضِ'): ('زمین', None),
    ('Al-Baqarah 2:205', 'ٱلْأَرْضِ'): ('زمین', None),
    ('Surah 7:59', 'قَوْمِهِۦ'): ('قوم', None),
    ('Al-Baqarah 2:118', 'ءَايَةٌ'): ('نشانی', None),
    ('Al-Baqarah 2:106', 'ءَايَةٍ'): ('آیت', None),
    ('Al-Baqarah 2:20', 'كُلِّ'): ('ہر', None),
    ('Al-Mu\'minun 23:32', 'رَسُولًا'): ('پیغمبر', None),
    ('Al-Fatihah 1:4', 'ٱلدِّينِ'): ('جزا', None),
    ('Al-Baqarah 2:85', 'ٱلدُّنْيَا'): ('دنیا', None),
    ('Al-Baqarah 2:201', 'ٱلدُّنْيَا'): ('دنیا', None),
    ('Al-Baqarah 2:2', 'هُدًى'): ('ہدایت', None),
    ('Al-Baqarah 2:5', 'هُدًى'): ('ہدایت', None),
    ('Al-Baqarah 2:38', 'هُدًى'): ('ہدایت', None),
    ('Al-Baqarah 2:7', 'عَظِيمٌ'): ('بڑا', None),
    ('Al-Baqarah 2:114', 'عَظِيمٌ'): ('بڑا', None),
    ('Al-Baqarah 2:105', 'ٱلْعَظِيمِ'): ('بڑے', None),
    ('Al-Baqarah 2:62', 'ٱلْـَٔاخِرِ'): ('آخرت', None),
    ('Al-Baqarah 2:4', 'بِٱلْـَٔاخِرَةِ'): ('آخرت', None),
    ('Al-Baqarah 2:201', 'ٱلْـَٔاخِرَةِ'): ('آخرت', None),
    ('Al-Baqarah 2:39', 'أَصْحَـٰبُ'): ('اہل', None),
    ('Al-Baqarah 2:82', 'أَصْحَـٰبُ'): ('مالک', None),
    ('Al-Baqarah 2:8', 'ٱلنَّاسِ'): ('لوگ', None),
    ('Al-Baqarah 2:21', 'ٱلنَّاسُ'): ('لوگو', None),
    ('Al-Baqarah 2:10', 'عَذَابٌ'): ('عذاب', None),
    ('Al-Baqarah 2:7', 'عَذَابٌ'): ('عذاب', None),
    ('Al-Baqarah 2:11', 'مُصْلِحُونَ'): ('اصلاح کرنے والے', None),
    ('Al-Baqarah 2:2', 'لِّلْمُتَّقِينَ'): ('ڈرنے والوں', None),
    ('Al-Baqarah 2:7', 'قُلُوبِهِمْ'): ('دلوں', None),
    ('Al-Baqarah 2:10', 'قُلُوبِهِم'): ('دلوں', None),
    ('Al-Fatihah 1:1', 'ٱلرَّحْمَـٰنِ'): ('مہربان', None),
    ('Al-Fatihah 1:1', 'ٱلرَّحِيمِ'): ('رحم والا', None),
    ('Al-Fatihah 1:2', 'ٱلْحَمْدُ'): ('تعریف', None),
    ('Al-Fatihah 1:7', 'ٱلضَّآلِّينَ'): ('گمراہوں', None),
    ('Al-Baqarah 2:23', 'ٱدْعُوا۟'): ('بلالو', None),
    ('Al-Baqarah 2:48', 'يُنصَرُونَ'): ('مدد حاصل کر سکیں', None),
    ('Al-Baqarah 2:38', 'يَحْزَنُونَ'): ('غمناک ہوں گے', None),
}

updated = 0
for r in range(2, ws.max_row + 1):
    cit = ws.cell(r, 10).value
    tar_ar = ws.cell(r, 11).value
    key = (cit, tar_ar)
    
    phrase = None
    full_v = None
    
    if key in EXACT_ISM_MAP:
        phrase, full_v = EXACT_ISM_MAP[key]
    else:
        # Check scratch dict
        dict_key = f'{cit} ||| {tar_ar}'
        p = ism_pairs.get(dict_key, {})
        wbw_tr = p.get('wbw_tr', '').strip()
        v_ur = str(ws.cell(r, 14).value or '')
        clean_v = re.sub(r'\(\[([^\]]+)\]\)', r'\1', v_ur)
        tokens = [t for t in wbw_tr.split() if t not in STOP_WORDS_UR]
        if wbw_tr and wbw_tr in clean_v and wbw_tr not in STOP_WORDS_UR:
            phrase = wbw_tr
        elif len(tokens) >= 2 and ' '.join(tokens) in clean_v:
            phrase = ' '.join(tokens)
        else:
            for t in tokens:
                if t in clean_v and len(t) > 1:
                    phrase = t
                    break
    
    if phrase:
        ws.cell(r, 13).value = phrase
        if full_v:
            ws.cell(r, 14).value = full_v
            updated += 1
        else:
            v_ur = str(ws.cell(r, 14).value or '')
            clean_v = re.sub(r'\(\[([^\]]+)\]\)', r'\1', v_ur)
            if phrase in clean_v:
                ws.cell(r, 14).value = clean_v.replace(phrase, f'([{phrase}])', 1)
                updated += 1
            else:
                print(f'Failed to find \"{phrase}\" in row {r} ({cit} | {tar_ar})')

print(f'Applied Ism Urdu rules to {updated} / {ws.max_row - 1} rows.')
wb.save('db/qw_ur.xlsx')
print('Saved db/qw_ur.xlsx.')
