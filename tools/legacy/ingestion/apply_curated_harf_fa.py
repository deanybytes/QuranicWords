import json, re, openpyxl, html

with open('db/harf_canonical_192.json') as f:
    rows = json.load(f)

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
with open('db/corpus_data/quran_fa.json') as f:
    fa_raw = json.load(f)['translations']

fa_dict = {}
for u, t in zip(uth_list, fa_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = html.unescape(txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    fa_dict[u['verse_key']] = txt

# Load FA_MAP from test_fa_all.py
from test_fa_all import FA_MAP

wb_fa = openpyxl.load_workbook('db/qw_fa.xlsx')
sheet_fa = wb_fa['Harf']

print('Applying curated Persian mappings to qw_fa.xlsx...')
fails = []

for r_data in rows:
    r_num = r_data['row']
    rank = r_data['rank']
    sense = r_data['sense']
    vk = r_data['vk']
    cit = r_data['cit']
    
    fa_m, fa_t, custom_v = FA_MAP.get(r_num, (r_data['en_m'], 'TODO', None))
    v_txt = custom_v if custom_v else fa_dict.get(vk, '')
    
    # Check if fa_t in v_txt
    m = re.search(r'\b' + re.escape(fa_t) + r'\b', v_txt)
    if not m:
        idx = v_txt.find(fa_t)
        if idx == -1:
            fails.append((r_num, rank, r_data['lemma'], fa_t, cit, v_txt[:60]))
            continue
        start, end = idx, idx + len(fa_t)
    else:
        start, end = m.start(), m.end()
        
    v_bracketed = v_txt[:start] + f'([{v_txt[start:end]}])' + v_txt[end:]
    actual_t = v_txt[start:end]
    
    # Write canonical data
    sheet_fa.cell(row=r_num, column=1).value = r_data['rank']
    sheet_fa.cell(row=r_num, column=2).value = r_data['lemma']
    sheet_fa.cell(row=r_num, column=3).value = r_data['translit']
    sheet_fa.cell(row=r_num, column=4).value = r_data['root']
    sheet_fa.cell(row=r_num, column=5).value = r_data['pos']
    sheet_fa.cell(row=r_num, column=6).value = r_data['occ']
    sheet_fa.cell(row=r_num, column=7).value = r_data['cov']
    sheet_fa.cell(row=r_num, column=8).value = r_data['sense']
    sheet_fa.cell(row=r_num, column=9).value = fa_m
    sheet_fa.cell(row=r_num, column=10).value = cit
    sheet_fa.cell(row=r_num, column=11).value = r_data['ar_t']
    sheet_fa.cell(row=r_num, column=12).value = r_data['full_ar']
    sheet_fa.cell(row=r_num, column=13).value = actual_t
    sheet_fa.cell(row=r_num, column=14).value = v_bracketed

if sheet_fa.max_row > 193:
    sheet_fa.delete_rows(194, sheet_fa.max_row - 193)

wb_fa.save('db/qw_fa.xlsx')
print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('  FAIL:', f)
print('Saved db/qw_fa.xlsx successfully.')
