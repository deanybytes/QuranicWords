import openpyxl, json, re

with open('db/corpus_data/quran_uthmani.json') as f:
    uth = json.load(f)['verses']
with open('db/corpus_data/quran_bn_zakaria.json') as f:
    bn = json.load(f)['translations']

bn_dict = {}
for u, b in zip(uth, bn):
    vk = u['verse_key']
    txt = re.sub(r'<[^>]+>', '', b.get('text', ''))
    txt = re.sub(r'\s+', ' ', txt).strip()
    bn_dict[vk] = txt

wb = openpyxl.load_workbook('db/qw_bn.xlsx', data_only=True)

for sname in ['Harf', 'Fil', 'Ism']:
    ws = wb[sname]
    mismatches = []
    for r in range(2, ws.max_row + 1):
        cit = ws.cell(r, 10).value
        v_curr = str(ws.cell(r, 14).value or '').strip()
        v_clean = re.sub(r'\(\[([^\]]+)\]\)', r'\1', v_curr).strip()
        
        m = re.search(r'(\d+:\d+)', cit)
        if m:
            vk = m.group(1)
            exp_v = bn_dict.get(vk, '')
            # Check if first 15 chars match or significant overlap
            clean_exp_prefix = re.sub(r'[^\w]', '', exp_v[:20])
            clean_curr_prefix = re.sub(r'[^\w]', '', v_clean[:20])
            if clean_exp_prefix[:10] != clean_curr_prefix[:10]:
                mismatches.append((r, cit, v_clean[:30], exp_v[:30]))
                
    print(f"Bengali {sname} verse content mismatches with citation: {len(mismatches)}")
    for m in mismatches[:10]:
        print("  ", m)
