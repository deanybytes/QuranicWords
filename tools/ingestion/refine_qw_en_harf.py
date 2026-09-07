import json, os, re, openpyxl

CORPUS_DIR = 'db/corpus_data'
with open(os.path.join(CORPUS_DIR, 'quran_uthmani.json')) as f:
    uth_list = json.load(f)['verses']
with open(os.path.join(CORPUS_DIR, 'quran_sahih.json')) as f:
    sahih_raw = json.load(f)['translations']

uth_dict = {u['verse_key']: u['text_uthmani'] for u in uth_list}
sahih_dict = {}
for u, t in zip(uth_list, sahih_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    sahih_dict[u['verse_key']] = txt

wb = openpyxl.load_workbook('db/qw_en.xlsx')
sheet = wb['Harf']

# 1. Update the 10 cleaner standalone citations
standalone_updates = {
    14: {'citation': 'An-Nahl 16:96', 'vk': '16:96', 'ar_t': 'مَا', 'en_t': 'Whatever',
         'en_pattern': r'\b(Whatever)\b'},
    20: {'citation': 'Al-Baqarah 2:278', 'vk': '2:278', 'ar_t': 'إِن', 'en_t': 'if',
         'en_pattern': r'\b(if)\s+you\s+should'},
    49: {'citation': "Ali 'Imran 3:198", 'vk': '3:198', 'ar_t': 'لَـٰكِنِ', 'en_t': 'But',
         'en_pattern': r'\b(But)\s+those\s+who'},
    102: {'citation': 'Sad 38:30', 'vk': '38:30', 'ar_t': 'نِعْمَ', 'en_t': 'excellent',
          'en_pattern': r'\b(excellent)\b'},
    104: {'citation': 'Al-Mutaffifin 83:1', 'vk': '83:1', 'ar_t': 'وَيْلٌ', 'en_t': 'Woe',
          'en_pattern': r'\b(Woe)\b'},
    110: {'citation': 'Al-Isra 17:75', 'vk': '17:75', 'ar_t': 'إِذًا', 'en_t': 'Then',
          'en_pattern': r'\b(Then)\b'},
    156: {'citation': 'An-Nur 24:60', 'vk': '24:60', 'ar_t': 'ٱلَّـٰتِى', 'en_t': 'who',
          'en_pattern': r'\b(who)\s+have\s+no\s+desire'},
    157: {'citation': 'Al-Ahzab 33:4', 'vk': '33:4', 'ar_t': 'ٱلَّـٰٓـِٔى', 'en_t': 'whom',
          'en_pattern': r'\bwives\s+(whom)\s+you\s+declare'},
    167: {'citation': 'Saba 34:24', 'vk': '34:24', 'ar_t': 'إِيَّاكُمْ', 'en_t': 'you',
          'en_pattern': r'\bwe\s+or\s+(you)\s+are'},
    174: {'citation': 'Al-Kahf 18:6', 'vk': '18:6', 'ar_t': 'إِن لَّمْ', 'en_t': 'if',
          'en_pattern': r'\b(if)\s+they\s+do\s+not'},
}

for r, u in standalone_updates.items():
    vk = u['vk']
    ar_text = uth_dict[vk]
    en_text = sahih_dict[vk]
    ar_t = u['ar_t']
    en_t = u['en_t']
    
    # Check that ar_t is in ar_text as a separate word (or at start/end of whitespace)
    assert ar_t in ar_text, f'ar_t {ar_t} not in {ar_text}'
    idx = ar_text.find(ar_t)
    # Ensure not attached to prefix
    if idx > 0:
        assert ar_text[idx-1].isspace(), f'ar_t {ar_t} in {vk} is attached to {ar_text[idx-1]}'
    
    ar_verse = ar_text.replace(ar_t, f'([{ar_t}])', 1)
    
    m = re.search(u['en_pattern'], en_text, re.I)
    assert m, f'en_pattern {u["en_pattern"]} failed on {en_text}'
    start, end = m.start(1), m.end(1)
    en_verse = en_text[:start] + f'([{en_text[start:end]}])' + en_text[end:]
    
    sheet.cell(row=r, column=10).value = u['citation']
    sheet.cell(row=r, column=11).value = ar_t
    sheet.cell(row=r, column=12).value = ar_verse
    sheet.cell(row=r, column=13).value = en_t
    sheet.cell(row=r, column=14).value = en_verse
    print(f'Updated Row {r} to standalone: {u["citation"]}')

# 2. Update the boundary fixes
boundary_updates = {
    35: (r'\bsinner\s+(or)\s+ungrateful', 'or'),
    39: (r'\b(if)\s+they\s+only\s+knew', 'if'),
    61: (r'\bthem\s+(or)\s+do\s+not', 'or'),
    76: (r'\"\bIf\s+(you)\s+should', 'you'),
    77: (r'\bIndeed,\s+(I)\s+am\s+Allāh', 'I'),
    162: (r'\bIndeed,\s+(you)\s+were', 'you'),
    166: (r'\bused\s+to\s+worship\s+\[i\.e\.,\s+obey\]\s+(us)\b', 'us'),
    177: (r'\bfor\s+it,\s+(ever)\b', 'ever'),
}

for r, (pattern, target_word) in boundary_updates.items():
    current_en = sheet.cell(row=r, column=14).value
    clean_en = current_en.replace('([', '').replace('])', '')
    m = re.search(pattern, clean_en, re.I)
    assert m, f'Boundary pattern {pattern} failed on Row {r}: {clean_en}'
    start, end = m.start(1), m.end(1)
    new_en = clean_en[:start] + f'([{clean_en[start:end]}])' + clean_en[end:]
    sheet.cell(row=r, column=13).value = target_word
    sheet.cell(row=r, column=14).value = new_en
    print(f'Fixed boundary on Row {r}: {clean_en[start:end]} in {target_word}')

wb.save('db/qw_en.xlsx')
print('Successfully saved db/qw_en.xlsx')
