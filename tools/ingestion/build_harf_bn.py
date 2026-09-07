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
        txt = re.sub(r'\[[০-৯0-9]+\]', '', txt)
        txt = re.sub(r'<[^>]+>', '', txt)
        txt = re.sub(r'\s+', ' ', txt).strip()
        res[u['verse_key']] = txt
    return res

zak_trans = load_trans('quran_bn_zakaria.json')
muj_trans = load_trans('quran_bn_mujibur.json')

wb_en = openpyxl.load_workbook('db/qw_en.xlsx', data_only=True)
sheet_en = wb_en['Harf']

wb_bn = openpyxl.load_workbook('db/qw_bn.xlsx')
sheet_bn = wb_bn['Harf']

vk_re = re.compile(r'(\d+):(\d+)')

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

# Manual mapping overrides for rows where default matching is ambiguous or absent
# (row_index_in_en): (bn_meaning, bn_target_word, use_alt_trans_flag)
BN_OVERRIDES = {
    2: ('হতে / থেকে', 'হতে', False),
    3: ('মধ্যে / হতে', 'মধ্যে', False),
    4: ('মধ্যে', 'মধ্যে', False),
    5: ('ব্যাপারে / বিষয়ে', 'সম্পর্কে', False),
    6: ('নিশ্চয়ই / অবশ্যই', 'নিশ্চয়', False),
    7: ('নিশ্চয়ই / বস্তুত', 'নিশ্চয়', False),
    8: ('উপর', 'উপর', False),
    9: ('বিরুদ্ধে', 'বিরুদ্ধে', False),
    10: ('জন্য', 'জন্য', False),
    11: ('জন্য / সাথে', 'জন্য', False),
    12: ('যা / যে', 'যা', False),
    13: ('যা কিছু', 'যা', False),
    14: ('যা / যা কিছু', 'যা', False),
    15: ('না', 'না', False),
    16: ('না', 'না', False),
    17: ('দিকে / প্রতি', 'দিকে', False),
    18: ('পর্যন্ত', 'পর্যন্ত', False),
    19: ('এবং / ও', 'ও', False),
    20: ('যদি', 'যদি', False), # 2:278
    21: ('নয়', 'নয়', False),
    22: ('যে / যেন', 'যে', False),
    23: ('ছাড়া / ব্যতীত', 'ছাড়া', False),
    24: ('ছাড়া / ব্যতীত', 'ছাড়া', False),
    25: ('তা / ওই', 'তা', False),
    26: ('সম্পর্কে / বিষয়ে', 'সম্পর্কে', False),
    27: ('হতে / থেকে', 'থেকে', False),
    28: ('নিশ্চয়ই / অবশ্যই', 'অবশ্যই', False),
    29: ('নিশ্চয়ই', 'সুস্পষ্ট হয়েছে', False),
    30: ('তারা / যারা', 'যারা', False),
    31: ('না', 'না', False),
    32: ('না', 'না', False),
    33: ('বরং', 'বরং', False),
    34: ('অথবা / বা', 'বা', False),
    35: ('অথবা / বা', 'বা', False), # rank 20
    36: ('যখন', 'যখন', False),
    37: ('যখন', 'যখন', False),
    38: ('তারাই', 'তারাই', False),
    39: ('যদি', 'যদি', False),
    40: ('কাছে / নিকট', 'কাছে', False),
    41: ('কাছে', 'কাছে', False),
    42: ('সাথে', 'সাথে', False),
    43: ('সাথে', 'সাথে', False),
    44: ('তিনি / সে', 'তিনিই', False),
    45: ('তিনি / সে', 'তিনি', False),
    46: ('তারা', 'তারা', False),
    47: ('তারা', 'তারা', False),
    48: ('কিন্তু / তবে', 'কিন্তু', False),
    49: ('কিন্তু / তবে', 'কিন্তু', False), # 3:198
    50: ('না', 'না', False),
    51: ('না', 'না', False),
    52: ('কখনও না', 'কখনও না', False),
    53: ('পূর্বে', 'পূর্বে', False),
    54: ('পূর্বে', 'পূর্বে', False),
    55: ('পর', 'পরে', False),
    56: ('পর', 'পর', False),
    57: ('তুমি', 'তুমি', False),
    58: ('তোমরা', 'তোমরা', False),
    59: ('আমরা', 'আমরা', False),
    60: ('যদি না / কেন নয়', 'কেন', False),
    61: ('অথবা / নাকি', 'বা', False),
    62: ('হ্যাঁ / অবশ্যই', 'হ্যাঁ', False),
    63: ('যাতে / যেন', 'যাতে', False),
    64: ('যেন / যাতে', 'যেন', False),
    65: ('যখন', 'যখন', False),
    66: ('তখন / অতঃপর', 'তখন', False),
    67: ('অতঃপর / তারপর', 'তারপর', False),
    68: ('অতঃপর / তারপর', 'অতঃপর', False),
    69: ('সুতরাং / অতঃপর', 'সুতরাং', False),
    70: ('যদি / যদিও', 'যদি', False),
    71: ('যদি', 'যদি', False),
    72: ('এই', 'এই', False),
    73: ('এইসব', 'এইসব', False),
    74: ('তিনি / সে (স্ত্রী)', 'সে', False),
    75: ('তারা (স্ত্রী)', 'তাদের', False),
    76: ('তোমরা (স্ত্রী)', 'তোমরা', False),
    77: ('আমি', 'আমি', False),
    78: ('আমরা', 'আমরা', False),
    79: ('কোথায়', 'কোথায়', False),
    80: ('কি / কেমন', 'কিভাবে', False),
    81: ('কী / কি', 'কী', False),
    82: ('কখন', 'কখন', False),
    83: ('কত / কতই না', 'কত', False),
    84: ('কোনটি', 'কোন', False),
    85: ('কেন', 'কেন', False),
    86: ('যেখানে / যেখান থেকে', 'যেখান থেকে', False),
    87: ('হায় / পরিতাপ', 'হায়', False),
    88: ('সাবধান / কখনও না', 'কখনই নয়', False),
    89: ('নিশ্চয়ই / কেবল', 'কেবল', False),
    90: ('যেন / মনে হয়', 'যেন', False),
    91: ('হয়তো / আশা করা যায়', 'হয়তো', False),
    92: ('হায় যদি', 'হায়', False),
    93: ('নয় / নেই', 'নেই', False),
    94: ('যেন না / যাতে না', 'যাতে না', False),
    95: ('ছাড়া / ব্যতীত', 'ছাড়া', False),
    96: ('ছাড়া / ব্যতীত', 'ব্যতীত', False),
    97: ('শুধু / কেবল', 'কেবল', False),
    98: ('কত / কতই না', 'কত', False),
    99: ('কতই না মন্দ', 'কতই না নিকৃষ্ট', False),
    100: ('কতই না মন্দ', 'কত নিকৃষ্ট', False),
    101: ('কতই না মন্দ', 'কতই না মন্দ', False),
    102: ('কতই না উত্তম', 'উত্তম', False), # 38:30
    103: ('কতই না উত্তম', 'উত্তম', False),
    104: ('ধ্বংস / দুর্ভোগ', 'দুর্ভোগ', False), # 83:1
    105: ('ওহে / হে', 'হে', False),
    106: ('ওহে / হে', 'হে', False),
    107: ('হ্যাঁ', 'হ্যাঁ', False),
    108: ('নয় / নেই', 'নয়', False),
    109: ('এখন', 'এখন', False),
    110: ('তাহলে / তবে', 'তাহলে', False), # 17:75
    111: ('কখনও না', 'কখনই না', False),
    112: ('যেখানে', 'যেখানে', False),
    113: ('যেখানেই', 'যেখানেই', False),
    114: ('তখন', 'তখন', False),
    115: ('দিকে / সম্মুখে', 'দিকে', False),
    116: ('পিছনে', 'পিছনে', False),
    117: ('পিছনে / ছাড়া', 'পিছনে', False),
    118: ('সামনে', 'সামনে', False),
    119: ('নিচে', 'নিচে', False),
    120: ('উপরে', 'উপরে', False),
    121: ('মধ্যে / মাঝে', 'মধ্যে', False),
    122: ('চারপাশে / চতুর্দিকে', 'আশপাশে', False),
    123: ('ডানপাশে', 'ডান', False),
    124: ('বামপাশে', 'বাম', False),
    125: ('মাঝখানে', 'মধ্যে', False),
    126: ('সকল / সমস্ত', 'সব', False),
    127: ('উভয়', 'উভয়', False),
    128: ('কিছু / কতক', 'কিছু', False),
    129: ('অন্য / অপর', 'অন্য', False),
    130: ('অন্যান্য', 'অন্যান্য', False),
    131: ('অন্য', 'অন্য', False),
    132: ('যে / যিনি', 'যে', False),
    133: ('যারা', 'যারা', False),
    134: ('যে দুইজন', 'উভয়কে', False),
    135: ('যে (স্ত্রী)', 'যে', False),
    136: ('যারা (স্ত্রী)', 'যারা', False), # 24:60
    137: ('যারা (স্ত্রী)', 'যাদের', False), # 33:4
    138: ('এই দুইটি', 'এ দুটি', False),
    139: ('ঐ / সেই', 'ঐ', False),
    140: ('ঐসব', 'ঐসব', False),
    141: ('তুমি (স্ত্রী)', 'তুমি', False),
    142: ('তুমি (স্ত্রী)', 'তুমি', False),
    143: ('তোমরা দুইজন', 'তোমরা উভয়', False),
    144: ('তাঁরা দুইজন', 'তাঁরা উভয়', False),
    145: ('আমাকে', 'আমাকে', False),
    146: ('আমাদেরকে', 'আমাদের', False),
    147: ('তোমাদেরকে', 'তোমরা', False), # 34:24
    148: ('তাদেরকে', 'তাদের', False),
    149: ('তাকে', 'তাকে', False),
    150: ('নিঃসন্দেহে / অবশ্যই', 'নিসঃন্দেহে', False),
    151: ('শুরু করল', 'লাগল', False),
    152: ('উপক্রম হওয়া / প্রায়', 'উপক্রম', False),
    153: ('প্রায় / হতে চলল', 'উপক্রম', False),
    154: ('যদি না', 'না', False), # 18:6
    155: ('যদি না / কেবল যদি', 'যদি', False),
    156: ('অবশ্যই / নিশ্চয়ই', 'নিশ্চয়', False),
    157: ('কখনও / চিরকাল', 'কখনই', False),
    158: ('কখনও', 'কখনও', False),
    159: ('আজ / এই দিন', 'আজ', False),
    160: ('সকাল', 'প্ৰভাত', False),
    161: ('সন্ধ্যা', 'সন্ধ্যা', False),
    162: ('সকাল / পূর্বাহ্ন', 'সকালেই', False),
    163: ('দিন / দিনে', 'দিন', False),
    164: ('রাত / রাতে', 'রাতে', False),
    165: ('সময় / কাল', 'সময়', False),
    166: ('পর্যায়ে', 'পৰ্যায়ক্রমে', False),
    167: ('একসাথে / সকলে', 'একসাথে', False),
    168: ('সমান / একরকম', 'সমান', True), # use mujibur for 2:6
    169: ('ছাড়া / ব্যতীত', 'ছাড়া', True), # use mujibur
    170: ('ছাড়া / ব্যতীত', 'ছাড়া', False),
    171: ('বরাবর / মধ্যবর্তী', 'মধ্যবর্তী', False),
    172: ('অনুরূপ / মতো', 'অনুরূপ', False),
    173: ('উভয়েই', 'উভয়েই', False),
}

print('Applying Bengali canonical Harf updates...')
updated = 0
for item in en_rows:
    r = item['row']
    cit = item['citation']
    m = vk_re.search(cit)
    vk = m.group(0)
    
    override = BN_OVERRIDES.get(r)
    if override:
        bn_m, bn_t, use_alt = override
    else:
        bn_m = 'অর্থ'
        bn_t = 'শব্দ'
        use_alt = False
        
    v_txt = muj_trans[vk] if use_alt else zak_trans[vk]
    
    # Verify bn_t is in v_txt
    if bn_t not in v_txt:
        # try mujibur
        if bn_t in muj_trans[vk]:
            v_txt = muj_trans[vk]
        else:
            print(f'WARNING: Row {r} ({cit}): {bn_t} NOT found in verse: {v_txt[:60]}')
            
    # Bracket first occurrence of bn_t
    idx = v_txt.find(bn_t)
    if idx != -1:
        bracketed_v = v_txt[:idx] + f'([{bn_t}])' + v_txt[idx+len(bn_t):]
    else:
        bracketed_v = v_txt
        
    # Write to sheet_bn
    sheet_bn.cell(row=r, column=1).value = item['rank']
    sheet_bn.cell(row=r, column=2).value = item['lemma']
    sheet_bn.cell(row=r, column=3).value = item['translit']
    sheet_bn.cell(row=r, column=4).value = item['root']
    sheet_bn.cell(row=r, column=5).value = item['pos']
    sheet_bn.cell(row=r, column=6).value = item['occ']
    sheet_bn.cell(row=r, column=7).value = item['cov']
    sheet_bn.cell(row=r, column=8).value = item['sense']
    sheet_bn.cell(row=r, column=9).value = bn_m
    sheet_bn.cell(row=r, column=10).value = cit
    sheet_bn.cell(row=r, column=11).value = item['ar_t']
    sheet_bn.cell(row=r, column=12).value = item['full_ar']
    sheet_bn.cell(row=r, column=13).value = bn_t
    sheet_bn.cell(row=r, column=14).value = bracketed_v
    updated += 1

# If sheet_bn had more rows than sheet_en, trim them
if sheet_bn.max_row > sheet_en.max_row:
    sheet_bn.delete_rows(sheet_en.max_row + 1, sheet_bn.max_row - sheet_en.max_row)

wb_bn.save('db/qw_bn.xlsx')
print(f'Successfully updated {updated} rows in db/qw_bn.xlsx Harf sheet.')
