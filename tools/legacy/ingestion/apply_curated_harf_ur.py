import json, re, openpyxl

with open('db/harf_canonical_192.json') as f:
    rows = json.load(f)

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
with open('db/corpus_data/quran_ur.json') as f:
    ur_raw = json.load(f)['translations']

ur_dict = {}
for u, t in zip(uth_list, ur_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    ur_dict[u['verse_key']] = txt

# Specific curated overrides for Urdu Harf rows
UR_OVERRIDES = {
    6: ('بے شک / یقیناً', 'بے شک', 'بے شک جو لوگ کافر ہیں انہیں تم نصیحت کرو یا نہ کرو ان کے لیے برابر ہے۔ وہ ایمان نہیں لانے کے'), # 2:6
    9: ('خلاف / پر', 'پر', None), # 2:89
    11: ('جو / جس', 'جو', None), # 2:25
    27: ('سے', 'سے', None), # 2:142
    35: ('یا / خواہ', 'اور', None), # 76:24
    36: ('جب', 'جب', None), # 2:133
    37: ('جب', 'جب', None), # 2:133
    44: ('وہ / وہی', 'وہی', None), # 2:29
    45: ('وہ', 'وہ', None), # 112:1
    48: ('اے', 'اے', 'اے لوگو! اپنے پروردگار کی عبات کرو جس نے تم کو اور تم سے پہلے لوگوں کو پیدا کیا تاکہ تم (اس کے عذاب سے) بچو'), # 2:21
    49: ('لیکن / مگر', 'لیکن', None), # 3:198
    51: ('گویا / جیسے', 'گویا', None), # 31:7
    52: ('کاش / اے کاش', 'کاش', None), # 19:23
    53: ('ہرگز نہیں', 'نہیں', None), # 2:55
    56: ('کیا / بھلا', 'بھلا', None), # 88:1
    62: ('دیکھو / خبردار', 'دیکھو', None), # 2:12
    63: ('درمیان / آپس میں', 'آپس میں', None), # 2:213
    69: ('پھر / پس', 'پھر', None), # 2:36
    73: ('وہ دونوں', 'وہ', 'اگر تم پیغمبر کی مدد نہ کرو گے تو خدا اُن کا مددگار ہے جب ان کو کافروں نے نکال دیا تھا، جب وہ دونوں غار میں تھے، جب وہ اپنے ساتھی سے کہہ رہے تھے کہ غم نہ کھاؤ، یقیناً اللہ ہمارے ساتھ ہے۔'), # 9:40
    74: ('تو / آپ', 'تو', None), # 2:32
    76: ('تم (عورتیں)', 'تم', None), # 33:28
    77: ('میں', 'میں', None), # 20:14
    87: ('کہاں', 'کہاں', None), # 75:10
    91: ('اپنی جناب سے', 'اپنی جناب سے', None), # 3:38
    94: ('ابھی تک نہیں', 'نہ', None), # 80:23
    96: ('زیادتی کرنا', 'زیادتی', None), # 2:190
    101: ('وہاں / جہاں', 'جہاں', None), # 76:20
    109: ('کیوں نہیں', 'کیوں نہیں', None), # 68:28
    110: ('تب / اس وقت', 'اس وقت', None), # 17:75
    157: ('جن کو / جو عورتیں', 'جن کو', None), # 33:4
    160: ('یہاں / یہیں', 'یہیں', None), # 5:24
    162: ('تو (عورت)', 'تو', None), # 12:29
    182: ('چاشت کا وقت / دن چڑھے', 'چاشت', None), # 20:59
    185: ('وقت / زمانہ', 'وقت', None), # 76:1
}

wb_ur = openpyxl.load_workbook('db/qw_ur.xlsx')
sheet_ur = wb_ur['Harf']

# Read original meanings from sheet_ur to keep quality high
ur_meanings = {}
for r in range(2, sheet_ur.max_row+1):
    rank = sheet_ur.cell(row=r, column=1).value
    sense = sheet_ur.cell(row=r, column=8).value
    meaning = sheet_ur.cell(row=r, column=9).value
    if rank == 20:
        meaning = 'یا / خواہ'
    if rank is not None and (rank, sense) not in ur_meanings:
        ur_meanings[(rank, sense)] = meaning

print('Applying curated Urdu mappings to qw_ur.xlsx...')
fails = []

for r_data in rows:
    r_num = r_data['row']
    rank = r_data['rank']
    sense = r_data['sense']
    vk = r_data['vk']
    cit = r_data['cit']
    
    v_txt = ur_dict[vk]
    
    if r_num in UR_OVERRIDES:
        ur_m, ur_t, custom_v = UR_OVERRIDES[r_num]
        if custom_v:
            v_txt = custom_v
    else:
        ur_m = ur_meanings.get((rank, sense), '')
        # Pick target word from sheet_ur if present in v_txt
        old_t = sheet_ur.cell(row=r_num, column=13).value or ''
        if old_t and old_t in v_txt:
            ur_t = old_t
        else:
            # find first candidate from ur_m
            ur_t = None
            for p in ur_m.split('/'):
                c = p.strip()
                if c and c in v_txt:
                    ur_t = c
                    break
            if not ur_t:
                ur_t = old_t
                
    if not ur_t or ur_t not in v_txt:
        fails.append((r_num, rank, r_data['lemma'], ur_t, cit, v_txt[:60]))
        continue
        
    idx = v_txt.find(ur_t)
    v_bracketed = v_txt[:idx] + f'([{ur_t}])' + v_txt[idx+len(ur_t):]
    
    # Write canonical data
    sheet_ur.cell(row=r_num, column=1).value = r_data['rank']
    sheet_ur.cell(row=r_num, column=2).value = r_data['lemma']
    sheet_ur.cell(row=r_num, column=3).value = r_data['translit']
    sheet_ur.cell(row=r_num, column=4).value = r_data['root']
    sheet_ur.cell(row=r_num, column=5).value = r_data['pos']
    sheet_ur.cell(row=r_num, column=6).value = r_data['occ']
    sheet_ur.cell(row=r_num, column=7).value = r_data['cov']
    sheet_ur.cell(row=r_num, column=8).value = r_data['sense']
    sheet_ur.cell(row=r_num, column=9).value = ur_m
    sheet_ur.cell(row=r_num, column=10).value = cit
    sheet_ur.cell(row=r_num, column=11).value = r_data['ar_t']
    sheet_ur.cell(row=r_num, column=12).value = r_data['full_ar']
    sheet_ur.cell(row=r_num, column=13).value = ur_t
    sheet_ur.cell(row=r_num, column=14).value = v_bracketed

wb_ur.save('db/qw_ur.xlsx')
print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('  FAIL:', f)
print('Saved db/qw_ur.xlsx successfully.')
