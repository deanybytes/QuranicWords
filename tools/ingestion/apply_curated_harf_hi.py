import json, re, openpyxl

with open('db/harf_canonical_192.json') as f:
    rows = json.load(f)

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
with open('db/corpus_data/quran_hi.json') as f:
    hi_raw = json.load(f)['translations']

hi_dict = {}
for u, t in zip(uth_list, hi_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    hi_dict[u['verse_key']] = txt

HI_OVERRIDES = {
    5: ('में / विषय में', 'में', None), # 2:65
    9: ('विरुद्ध / ख़िलाफ़', 'विरुद्ध', 'और जब उनके पास अल्लाह की ओर से एक किताब आई जो उसकी पुष्टि करती है जो उनके पास है—यद्यपि इससे पहले वे काफ़िरों के विरुद्ध विजय की प्रार्थना करते थे—तो जब वह चीज़ उनके पास आ गई जिसे वे पहचानते थे, तो उन्होंने उसका इनकार किया। अतः काफ़िरों पर अल्लाह की लानत है।'), # 2:89
    11: ('जो', 'जो', None), # 2:25
    15: ('नहीं / न', 'न', None), # 93:3
    17: ('की ओर / तक', 'की ओर', None), # 2:257
    27: ('से', 'से', None), # 2:142
    28: ('निश्चय / अवश्य', 'निश्चय', None), # 2:144
    32: ('नहीं / न', 'न', None), # 2:6
    35: ('या / अथवा', 'या', 'तो अपने पालनहार के आदेश के लिए धैर्य से काम लो और उनमें से किसी पापी या कृतघ्न का कहना न मानो।'), # 76:24
    36: ('जब', 'जब', None), # 2:133
    37: ('जब', 'जब', None), # 2:133
    44: ('वह / वही', 'वही', None), # 2:29
    45: ('वह', 'वह', None), # 112:1
    46: ('वे', 'वे', None), # 2:38
    47: ('वे', 'वे', None), # 2:38
    48: ('ऐ', 'ऐ', None), # 2:21
    49: ('परन्तु / लेकिन', 'परन्तु', None), # 3:198
    51: ('जैसे / मानो', 'जैसे', None), # 31:7
    53: ('कदापि नहीं', 'कदापि', None), # 2:55
    54: ('शीघ्र ही / जल्द', 'शीघ्र', None), # 102:3
    56: ('क्या', 'क्या', None), # 88:1
    62: ('सावधान / खबरदार', 'सावधान', None), # 2:12
    65: ('तरह / जैसे', 'तरह', None), # 2:261
    68: ('तथा / और', 'तथा', None), # 2:4
    69: ('तो / फिर', 'तो', None), # 2:36
    72: ('वे (स्त्रियां)', 'वे', None), # 2:187
    74: ('तू / आप', 'तू', None), # 2:32
    76: ('तुम (स्त्रियां)', 'तुम', None), # 33:28
    77: ('मैं', 'मैं', None), # 20:14
    86: ('कब', 'कब', None), # 7:187
    87: ('कहाँ', 'कहाँ', None), # 75:10
    92: ('क़रीब है / शायद', 'क़रीब है', None), # 17:79
    94: ('अभी तक नहीं', 'अभी तक', None), # 80:23
    96: ('अत्याचार करना / सीमा लांघना', 'अत्याचार', None), # 2:190
    101: ('वहाँ', 'वहाँ', None), # 76:20
    102: ('बहुत अच्छा / उत्तम', 'बहुत अच्छा', None), # 38:30
    109: ('क्यों नहीं', 'क्यों', None), # 68:28
    110: ('तब / तो', 'तब', None), # 17:75
    114: ('जहाँ भी', 'जहाँ भी', None), # 4:78
    137: ('आगे / पीछे', 'आगे', None), # 18:79
    155: ('वे दो', 'वे दो', 'तथा जिन लोगों ने कुफ़्र किया, वे कहेंगे : ऐ हमारे पालनहार! हमें जिन्नों और इनसानों में से वे दो दिखा दे जिन्होंने हमें भटकाया, हम उन्हें अपने पैरों तले रौंदेंगे تاکہ वे अपमानितों में से हो जाएँ।'), # 41:29
    160: ('यहाँ / इसमें', 'यहाँ', 'उन्होंने कहा : ऐ मूसा! हम कभी वहाँ नहीं जाएँगे जब तक वे वहाँ हैं, तुम और तुम्हारा रब जाओ और लड़ो, हम तो यहाँ बैठे हैं।'), # 5:24
    162: ('तू (स्त्री)', 'तू', None), # 12:29
    174: ('यदि नहीं', 'यदि', None), # 18:6
    182: ('दिन चढ़े / पूर्वाह्न', 'दिन चढ़े', None), # 20:59
    185: ('समय / काल', 'समय', None), # 76:1
}

wb_hi = openpyxl.load_workbook('db/qw_hi.xlsx')
sheet_hi = wb_hi['Harf']

hi_meanings = {}
for r in range(2, sheet_hi.max_row+1):
    rank = sheet_hi.cell(row=r, column=1).value
    sense = sheet_hi.cell(row=r, column=8).value
    meaning = sheet_hi.cell(row=r, column=9).value
    if rank == 20:
        meaning = 'या / अथवा'
    if rank is not None and (rank, sense) not in hi_meanings:
        hi_meanings[(rank, sense)] = meaning

print('Applying curated Hindi mappings to qw_hi.xlsx...')
fails = []

for r_data in rows:
    r_num = r_data['row']
    rank = r_data['rank']
    sense = r_data['sense']
    vk = r_data['vk']
    cit = r_data['cit']
    
    v_txt = hi_dict[vk]
    
    if r_num in HI_OVERRIDES:
        hi_m, hi_t, custom_v = HI_OVERRIDES[r_num]
        if custom_v:
            v_txt = custom_v
    else:
        hi_m = hi_meanings.get((rank, sense), '')
        old_t = sheet_hi.cell(row=r_num, column=13).value or ''
        if old_t and old_t in v_txt:
            hi_t = old_t
        else:
            hi_t = None
            for p in hi_m.split('/'):
                c = p.strip()
                if c and c in v_txt:
                    hi_t = c
                    break
            if not hi_t:
                hi_t = old_t
                
    if not hi_t or hi_t not in v_txt:
        fails.append((r_num, rank, r_data['lemma'], hi_t, cit, v_txt[:60]))
        continue
        
    idx = v_txt.find(hi_t)
    v_bracketed = v_txt[:idx] + f'([{hi_t}])' + v_txt[idx+len(hi_t):]
    
    # Write canonical data
    sheet_hi.cell(row=r_num, column=1).value = r_data['rank']
    sheet_hi.cell(row=r_num, column=2).value = r_data['lemma']
    sheet_hi.cell(row=r_num, column=3).value = r_data['translit']
    sheet_hi.cell(row=r_num, column=4).value = r_data['root']
    sheet_hi.cell(row=r_num, column=5).value = r_data['pos']
    sheet_hi.cell(row=r_num, column=6).value = r_data['occ']
    sheet_hi.cell(row=r_num, column=7).value = r_data['cov']
    sheet_hi.cell(row=r_num, column=8).value = r_data['sense']
    sheet_hi.cell(row=r_num, column=9).value = hi_m
    sheet_hi.cell(row=r_num, column=10).value = cit
    sheet_hi.cell(row=r_num, column=11).value = r_data['ar_t']
    sheet_hi.cell(row=r_num, column=12).value = r_data['full_ar']
    sheet_hi.cell(row=r_num, column=13).value = hi_t
    sheet_hi.cell(row=r_num, column=14).value = v_bracketed

wb_hi.save('db/qw_hi.xlsx')
print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('  FAIL:', f)
print('Saved db/qw_hi.xlsx successfully.')
