import json, re, openpyxl, html

with open('db/harf_canonical_192.json') as f:
    rows = json.load(f)

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
with open('db/corpus_data/quran_sw.json') as f:
    sw_raw = json.load(f)['translations']

sw_dict = {}
for u, t in zip(uth_list, sw_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = html.unescape(txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    sw_dict[u['verse_key']] = txt

from test_sw_all import SW_MAP

wb_sw = openpyxl.load_workbook('db/qw_sw.xlsx')
sheet_sw = wb_sw['Harf']

print('Applying curated Swahili mappings to qw_sw.xlsx...')
fails = []

for r_data in rows:
    r_num = r_data['row']
    rank = r_data['rank']
    sense = r_data['sense']
    vk = r_data['vk']
    cit = r_data['cit']
    
    sw_m, sw_t, custom_v = SW_MAP.get(r_num, (r_data['en_m'], 'TODO', None))
    v_txt = custom_v if custom_v else sw_dict.get(vk, '')
    
    # Check if sw_t in v_txt
    m = re.search(r'\b' + re.escape(sw_t) + r'\b', v_txt)
    if not m:
        idx = v_txt.find(sw_t)
        if idx == -1:
            fails.append((r_num, rank, r_data['lemma'], sw_t, cit, v_txt[:60]))
            continue
        start, end = idx, idx + len(sw_t)
    else:
        start, end = m.start(), m.end()
        
    v_bracketed = v_txt[:start] + f'([{v_txt[start:end]}])' + v_txt[end:]
    actual_t = v_txt[start:end]
    
    # Write canonical data
    sheet_sw.cell(row=r_num, column=1).value = r_data['rank']
    sheet_sw.cell(row=r_num, column=2).value = r_data['lemma']
    sheet_sw.cell(row=r_num, column=3).value = r_data['translit']
    sheet_sw.cell(row=r_num, column=4).value = r_data['root']
    sheet_sw.cell(row=r_num, column=5).value = r_data['pos']
    sheet_sw.cell(row=r_num, column=6).value = r_data['occ']
    sheet_sw.cell(row=r_num, column=7).value = r_data['cov']
    sheet_sw.cell(row=r_num, column=8).value = r_data['sense']
    sheet_sw.cell(row=r_num, column=9).value = sw_m
    sheet_sw.cell(row=r_num, column=10).value = cit
    sheet_sw.cell(row=r_num, column=11).value = r_data['ar_t']
    sheet_sw.cell(row=r_num, column=12).value = r_data['full_ar']
    sheet_sw.cell(row=r_num, column=13).value = actual_t
    sheet_sw.cell(row=r_num, column=14).value = v_bracketed

if sheet_sw.max_row > 193:
    sheet_sw.delete_rows(194, sheet_sw.max_row - 193)

wb_sw.save('db/qw_sw.xlsx')
print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('  FAIL:', f)
print('Saved db/qw_sw.xlsx successfully.')
