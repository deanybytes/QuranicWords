import os, sys, re, openpyxl
from tools.ingestion.apply_curated_harf_en import HARF_CANONICAL, uth_map, trans_map
from tools.ingestion.build_harf_complete import surah_names

def highlight_token(text, target):
    if not text or not target:
        return text, ''
    target_clean = str(target).strip()
    idx = text.find(target_clean)
    if idx >= 0:
        actual = text[idx:idx+len(target_clean)]
        return text[:idx] + f"([{actual}])" + text[idx+len(target_clean):], actual
    pattern = r'(?i)\b' + re.escape(target_clean) + r'\b'
    m = re.search(pattern, text)
    if m:
        actual = text[m.start():m.end()]
        return text[:m.start()] + f"([{actual}])" + text[m.end():], actual
    idx = text.lower().find(target_clean.lower())
    if idx >= 0:
        actual = text[idx:idx+len(target_clean)]
        return text[:idx] + f"([{actual}])" + text[idx+len(target_clean):], actual
    return text, target_clean

wb_path = os.path.join('db', 'qw_en.xlsx')
wb = openpyxl.load_workbook(wb_path)
ws = wb['Harf']

updated_count = 0
for r in range(2, ws.max_row + 1):
    rank = ws.cell(r, 1).value
    s_idx = str(ws.cell(r, 8).value or '').strip()
    key = (rank, s_idx)
    
    if key in HARF_CANONICAL:
        vk, tar_ar, en_sense, en_tar = HARF_CANONICAL[key]
        s_num, a_num = [int(x) for x in vk.split(':')]
        s_name = surah_names[s_num]
        cit = f"{s_name} {vk}"
        
        v_ar = uth_map[vk]
        ar_hl, actual_ar = highlight_token(v_ar, tar_ar)
        
        v_en = trans_map['en'][vk]
        en_hl, actual_en = highlight_token(v_en, en_tar)
        
        ws.cell(r, 9).value = en_sense
        ws.cell(r, 10).value = cit
        ws.cell(r, 11).value = tar_ar
        ws.cell(r, 12).value = ar_hl
        ws.cell(r, 13).value = actual_en
        ws.cell(r, 14).value = en_hl
        updated_count += 1

wb.save(wb_path)
print(f"Successfully updated {updated_count}/192 rows in {wb_path} Harf sheet.")
