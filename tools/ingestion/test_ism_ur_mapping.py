import json, re

with open('scratch_ur_ism_275.json') as f:
    ism_pairs = json.load(f)

STOP_WORDS_UR = {'اور', 'کہ', 'سے', 'کو', 'کا', 'کی', 'کے', 'نے', 'پر', 'میں', 'وہ', 'ہم', 'تم', 'آپ', 'تو', 'ہی', 'بھی', 'نہ', 'نہیں', 'جو', 'جس', 'جن', 'ان', 'اس', 'ہے', 'ہیں', 'تھا', 'تھے', 'تھی', 'ہوا', 'ہوئے', 'ہوئی', 'ہو'}

CORRECTED_VERSES = {
    "Ali 'Imran 3:19 ||| ٱلْإِسْلَـٰمُ": 'دین تو خدا کے نزدیک اسلام ہے اور اہل کتاب نے جو (اس دین سے) اختلاف کیا تو علم حاصل ہونے کے بعد آپس کی ضد سے کیا۔ اور جو شخص خدا کی آیتوں کو نہ مانے تو خدا جلد حساب لینے والا ہے',
    "An-Nazi'at 79:31 ||| مَرْعَىٰهَا": 'اسی نے اس میں سے اس کا پانی نکالا اور چارا اگایا',
    'Al-Baqarah 2:173 ||| حَرَّمَ': 'اس نے تم پر مرا ہوا جانور اور لہو اور سور کا گوشت اور جس چیز پر خدا کے سوا کسی اور کا نام پکارا جائے حرام کر دیا ہے پھر جو مجبور ہو جائے بشرطیکہ نہ تو نافرمانی کرنے والا ہو اور نہ حد سے بڑھنے والا تو اس پر کوئی گناہ نہیں۔ بےشک خدا بخشنے والا مہربان ہے',
    'Al-Baqarah 2:104 ||| رَٰعِنَا': 'اے اہل ایمان! (گفتگو کے وقت پیغمبرِ خدا سے) راعنا نہ کہا کرو۔ انظرنا کہا کرو۔ اور خوب سن رکھو، اور کافروں کے لیے دکھ دینے والا عذاب ہے'
}

for k, v in CORRECTED_VERSES.items():
    if k in ism_pairs:
        ism_pairs[k]['verse_ur'] = v

matched = {}
unmatched = []

for key, p in ism_pairs.items():
    v_ur = p['verse_ur']
    wbw_tr = p['wbw_tr'].strip()
    tar_en = p['tar_en']
    
    # Try exact wbw
    cand = None
    if wbw_tr and wbw_tr in v_ur and wbw_tr not in STOP_WORDS_UR:
        cand = wbw_tr
    else:
        tokens = [t for t in wbw_tr.split() if t not in STOP_WORDS_UR]
        if len(tokens) >= 2 and ' '.join(tokens) in v_ur:
            cand = ' '.join(tokens)
        else:
            for t in tokens:
                if t in v_ur and len(t) > 1:
                    cand = t
                    break
    
    if cand:
        matched[key] = cand
    else:
        unmatched.append((key, p['lemma'], p['tar_en'], p['wbw_tr'], v_ur))

print(f'Matched: {len(matched)} / {len(ism_pairs)}')
print(f'Unmatched: {len(unmatched)}')
