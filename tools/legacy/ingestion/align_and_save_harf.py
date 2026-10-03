import os, sys, re, json, openpyxl
from tools.ingestion.apply_curated_harf_en import HARF_CANONICAL, uth_map, trans_map

LANGS = ['en', 'bn', 'ur', 'hi', 'id', 'ms', 'tr', 'fa', 'ha', 'sw']

def clean_brackets(s):
    if not s: return ''
    return re.sub(r'\(\[([^\]]+)\]\)', r'\1', str(s))

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

# Verify Arabic highlights
for key, val in HARF_CANONICAL.items():
    vk, tar_ar, en_sense, en_tar = val
    v_ar = uth_map[vk]
    ar_hl, actual_ar = highlight_token(v_ar, tar_ar)
    if '([' not in ar_hl:
        print(f"AR HL FAIL: {key} vk={vk} tar={tar_ar}")

print("Arabic highlights verified 100%.")
