import os, sys, re, json, openpyxl
from tools.ingestion.apply_curated_harf_en import HARF_CANONICAL, uth_map, trans_map

print(f"HARF_CANONICAL has {len(HARF_CANONICAL)} verified entries.")

LANGS = ['en', 'bn', 'ur', 'hi', 'id', 'ms', 'tr', 'fa', 'ha', 'sw']

# For each language, let's load the existing workbooks to get baseline meanings
existing_meanings = {l: {} for l in LANGS}
for l in LANGS:
    fname = f'qw_{l}.xlsx'
    wb = openpyxl.load_workbook(os.path.join('db', fname), read_only=True)
    ws = wb['Harf']
    for r in ws.iter_rows(min_row=2, values_only=True):
        key = (r[0], r[7])
        existing_meanings[l][key] = {
            'meaning': r[8],
            'tar_m': r[12]
        }

print("Loaded existing meanings across all 10 workbooks.")
