import json, os, re, openpyxl

CORPUS_DIR = 'db/corpus_data'
with open(os.path.join(CORPUS_DIR, 'quran_uthmani.json')) as f:
    uth_list = json.load(f)['verses']

def load_trans(fname):
    with open(os.path.join(CORPUS_DIR, fname)) as f:
        data = json.load(f)
    items = data.get('translations', data.get('verses', []))
    res = {}
    for u, item in zip(uth_list, items):
        txt = re.sub(r'<sup[^>]*>.*?</sup>', '', item.get('text', ''))
        txt = re.sub(r'\[[০-৯0-9]+\]', '', txt) # remove footnote markers like [১]
        txt = re.sub(r'<[^>]+>', '', txt)
        txt = re.sub(r'\s+', ' ', txt).strip()
        res[u['verse_key']] = txt
    return res

zak_trans = load_trans('quran_bn_zakaria.json')
muj_trans = load_trans('quran_bn_mujibur.json')

wb_en = openpyxl.load_workbook('db/qw_en.xlsx', data_only=True)
sheet_en = wb_en['Harf']

wb_bn = openpyxl.load_workbook('db/qw_bn.xlsx', data_only=True)
sheet_bn = wb_bn['Harf']

# Read en canonical
en_rows = []
for r in range(2, sheet_en.max_row+1):
    en_rows.append({
        'row': r,
        'rank': sheet_en.cell(row=r, column=1).value,
        'lemma': sheet_en.cell(row=r, column=2).value,
        'translit': sheet_en.cell(row=r, column=3).value,
        'root': sheet_en.cell(row=r, column=4).value,
        'pos': sheet_en.cell(row=r, column=5).value,
        'occ': sheet_en.cell(row=r, column=6).value,
        'cov': sheet_en.cell(row=r, column=7).value,
        'sense': sheet_en.cell(row=r, column=8).value,
        'en_m': sheet_en.cell(row=r, column=9).value,
        'citation': sheet_en.cell(row=r, column=10).value,
        'ar_t': sheet_en.cell(row=r, column=11).value,
        'full_ar': sheet_en.cell(row=r, column=12).value,
        'en_t': sheet_en.cell(row=r, column=13).value,
        'full_en': sheet_en.cell(row=r, column=14).value,
    })

# Read existing bn rows to get existing Bangla meanings
bn_dict = {}
for r in range(2, sheet_bn.max_row+1):
    rank = sheet_bn.cell(row=r, column=1).value
    lemma = sheet_bn.cell(row=r, column=2).value
    sense = sheet_bn.cell(row=r, column=8).value
    meaning = sheet_bn.cell(row=r, column=9).value
    tm = sheet_bn.cell(row=r, column=13).value
    if rank is not None:
        bn_dict[(rank, sense)] = (meaning, tm)

print(f'Loaded {len(en_rows)} canonical EN rows and {len(bn_dict)} BN rows.')

# Now let us test verse matching for all 192 rows
vk_re = re.compile(r'(\d+):(\d+)')
matches = 0
fails = []

for item in en_rows:
    m = vk_re.search(item['citation'])
    vk = m.group(0)
    v_txt = zak_trans.get(vk, '')
    
    # Check bn meaning
    rank = item['rank']
    sense = item['sense']
    bn_m, old_tm = bn_dict.get((rank, sense), (None, None))
    
    # If rank == 20 (aw), meaning should be অথবা / বা
    if rank == 20:
        bn_m = 'বা / অথবা'
        old_tm = 'বা'
    
    found = False
    target = None
    
    # Try candidates from bn_m (split by /)
    candidates = []
    if old_tm and old_tm in v_txt:
        candidates.append(old_tm)
    if bn_m:
        for p in bn_m.split('/'):
            c = p.strip()
            if c:
                candidates.append(c)
                # also try without punctuation
                c_clean = re.sub(r'[^\w\s]', '', c).strip()
                if c_clean:
                    candidates.append(c_clean)
    
    for c in candidates:
        if c in v_txt:
            found = True
            target = c
            break
            
    if found:
        matches += 1
    else:
        fails.append((item['row'], rank, item['lemma'], sense, item['en_m'], bn_m, old_tm, vk, v_txt))

print(f'Bengali initial match: {matches}/192 matched. {len(fails)} need target mapping.')
for f in fails[:15]:
    print(f'Row {f[0]:3d} | Rank {f[1]:3d} {f[2]} {f[3]} | EN: {f[4]} | BN: {f[5]} | OldTM: {f[6]} | VK: {f[7]}')
    print(f'   Verse: {f[8][:100]}...')
