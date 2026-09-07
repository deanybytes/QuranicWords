import json, re, openpyxl, html

with open('db/harf_canonical_192.json') as f:
    rows = json.load(f)

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
with open('db/corpus_data/quran_ha.json') as f:
    ha_raw = json.load(f)['translations']

ha_dict = {}
for u, t in zip(uth_list, ha_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = html.unescape(txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    ha_dict[u['verse_key']] = txt

from test_ha_all import HA_MAP

wb_ha = openpyxl.load_workbook('db/qw_ha.xlsx')
sheet_ha = wb_ha['Harf']

print('Applying curated Hausa mappings to qw_ha.xlsx...')
fails = []

for r_data in rows:
    r_num = r_data['row']
    rank = r_data['rank']
    sense = r_data['sense']
    vk = r_data['vk']
    cit = r_data['cit']
    
    ha_m, ha_t, custom_v = HA_MAP.get(r_num, (r_data['en_m'], 'TODO', None))
    v_txt = custom_v if custom_v else ha_dict.get(vk, '')
    
    # Check if ha_t in v_txt
    m = re.search(r'\b' + re.escape(ha_t) + r'\b', v_txt)
    if not m:
        idx = v_txt.find(ha_t)
        if idx == -1:
            fails.append((r_num, rank, r_data['lemma'], ha_t, cit, v_txt[:60]))
            continue
        start, end = idx, idx + len(ha_t)
    else:
        start, end = m.start(), m.end()
        
    v_bracketed = v_txt[:start] + f'([{v_txt[start:end]}])' + v_txt[end:]
    actual_t = v_txt[start:end]
    
    # Write canonical data
    sheet_ha.cell(row=r_num, column=1).value = r_data['rank']
    sheet_ha.cell(row=r_num, column=2).value = r_data['lemma']
    sheet_ha.cell(row=r_num, column=3).value = r_data['translit']
    sheet_ha.cell(row=r_num, column=4).value = r_data['root']
    sheet_ha.cell(row=r_num, column=5).value = r_data['pos']
    sheet_ha.cell(row=r_num, column=6).value = r_data['occ']
    sheet_ha.cell(row=r_num, column=7).value = r_data['cov']
    sheet_ha.cell(row=r_num, column=8).value = r_data['sense']
    sheet_ha.cell(row=r_num, column=9).value = ha_m
    sheet_ha.cell(row=r_num, column=10).value = cit
    sheet_ha.cell(row=r_num, column=11).value = r_data['ar_t']
    sheet_ha.cell(row=r_num, column=12).value = r_data['full_ar']
    sheet_ha.cell(row=r_num, column=13).value = actual_t
    sheet_ha.cell(row=r_num, column=14).value = v_bracketed

if sheet_ha.max_row > 193:
    sheet_ha.delete_rows(194, sheet_ha.max_row - 193)

wb_ha.save('db/qw_ha.xlsx')
print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('  FAIL:', f)
print('Saved db/qw_ha.xlsx successfully.')
