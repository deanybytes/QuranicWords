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

wb_ur = openpyxl.load_workbook('db/qw_ur.xlsx', data_only=True)
sheet_ur = wb_ur['Harf']

# Dump existing Urdu meanings and targets
ur_existing = {}
for r in range(2, sheet_ur.max_row+1):
    rank = sheet_ur.cell(row=r, column=1).value
    sense = sheet_ur.cell(row=r, column=8).value
    meaning = sheet_ur.cell(row=r, column=9).value
    target = sheet_ur.cell(row=r, column=13).value
    verse = sheet_ur.cell(row=r, column=14).value
    if rank is not None:
        ur_existing[(rank, sense)] = (meaning, target, verse)

print(f'Loaded {len(ur_existing)} existing Urdu rows.')

# Check which rows match automatically and which need curation
auto_matches = 0
needs_curation = []

for r_data in rows:
    r_num = r_data['row']
    rank = r_data['rank']
    sense = r_data['sense']
    lemma = r_data['lemma']
    en_m = r_data['en_m']
    cit = r_data['cit']
    vk = r_data['vk']
    
    ur_m, old_target, old_v = ur_existing.get((rank, sense), ('', '', ''))
    if rank == 20: # aw
        ur_m = 'یا / خواہ'
        old_target = 'یا'
        
    v_txt = ur_dict.get(vk, '')
    
    # Try candidates
    candidates = []
    if old_target and old_target in v_txt:
        candidates.append(old_target)
    if ur_m:
        for p in ur_m.split('/'):
            c = p.strip()
            if c and c in v_txt:
                candidates.append(c)
                
    if candidates:
        auto_matches += 1
    else:
        needs_curation.append((r_num, rank, lemma, sense, en_m, ur_m, old_target, cit, v_txt))

print(f'Auto matches: {auto_matches}/192, Needs curation: {len(needs_curation)}')
for item in needs_curation[:25]:
    print(f'Row {item[0]:3d} (Rank {item[1]:3d} {item[2]} {item[3]}): EN \"{item[4]}\" | UR meaning: \"{item[5]}\" | Old target: \"{item[6]}\" | Cit: {item[7]}')
    print(f'   Verse: {item[8][:100]}...')
