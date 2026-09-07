import json, re, html

with open('db/harf_canonical_192.json') as f:
    canonical = json.load(f)

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

HA_MAP = {
    2: ('daga', 'daga', None), # 2:22
    3: ('daga / cikin', 'daga', None), # 2:25
    4: ('a cikin', 'a cikin', None), # 2:176
    5: ('a cikin / game da', 'a cikin', None), # 2:65
    6: ('lalle ne / haƙĩƙa', 'Lalle ne', None), # 2:6
    7: ('lalle ne / haƙĩƙa', 'lalle ne', None), # 2:143
    8: ('a kan / kan', 'kan', None), # 2:5
    9: ('a kan / bisa', 'a kan', None), # 2:89
    10: ('wanda', 'wanda', None), # 2:17
    11: ('wadda', 'wadda', 'Kuma ka bãyar da bishãra ga waɗanda suka yi ĩmãni kuma suka aikata ayyuka na ƙwarai, cẽwa lallene, suna da gidãjen Aljanna, wadda ƙoramu ke gudãna daga ƙarƙashinsu.'), # 2:25
    12: ('babu / ba', 'bãbu', None), # 2:2
    13: ('kada / kada ku', 'Kada', None), # 2:11
    14: ('abin da', 'Abin da', None), # 16:96
    15: ('ba / bai', 'bai', None), # 93:3
    16: ('zuwa ga / har', 'zuwa ga', None), # 2:187
    17: ('zuwa ga', 'zuwa ga', None), # 2:257
    18: ('wanda', 'wanda', None), # 2:8
    19: ('wanda ya / duk wanda', 'Wanda ya', None), # 2:112
    20: ('idan / in', 'idan', None), # 2:278
    21: ('fãce / ba komai', 'fãce', None), # 6:25
    22: ('cewa / ya', 'Ya bayyana', None), # 2:26
    23: ('fãce / sai', 'fãce', None), # 2:9
    24: ('fãce / bãcin', 'fãce', None), # 21:22
    25: ('wãncan / wannan', 'Wãncan', None), # 2:2
    26: ('daga', 'daga', None), # 2:48
    27: ('daga', 'daga', None), # 2:142
    28: ('haƙĩƙa / lalle', 'haƙĩƙa', None), # 2:144
    29: ('haƙĩƙa / tabbas', 'haƙĩƙa', None), # 2:256
    30: ('cẽwa / cewa', 'cẽwa', None), # 2:25
    31: ('bai / ba', 'Bai', None), # 112:3
    32: ('ba ka yi / ba', 'ba ka yi', None), # 2:6
    33: ("sa'an nan", "sa'an nan", None), # 2:28
    34: ('wannan', 'Wannan', None), # 2:25
    35: ('ko / kõ', 'ko', None), # 76:24
    36: ('a lõkacin da', 'a lõkacin da', None), # 2:133
    37: ('a lõkacin da', 'a lõkacin da', None), # 2:133
    38: ('waɗannan', 'Waɗannan', None), # 2:5
    39: ('dã / idan', 'dã', None), # 2:102
    40: ('a wurin / wurin', 'a wurin', None), # 2:62
    41: ('a wurinsa', 'a wurinsa', None), # 13:43
    42: ('tãre da', 'tãre da', None), # 2:14
    43: ('tãre da', 'tãre da', None), # 2:14
    44: ('shi / shi ne', 'Shi ne', None), # 2:29
    45: ('shi / shi ne', 'Shi ne', None), # 112:1
    46: ('su / sũ', 'su', None), # 2:38
    47: ('su / sũ', 'su', None), # 2:38
    48: ('yã / ya', 'Yã', None), # 2:21
    49: ('amma', 'Amma', None), # 3:198
    50: ('tsammãninku / mai yiwuwa', 'tsammãninku', None), # 2:21
    51: ('kamar', 'kamar', 'Kuma idan ana karanta masa ãyõyinMu, sai ya jũya yanã mai girman kai, kamar bai ji su ba, kamar akwai wani nauyi a cikin kunnuwansa.'), # 31:7
    52: ('kaitona / dã ma', 'Kaitona', None), # 19:23
    53: ('bã zã mu / har abada ba', 'Bã zã mu', None), # 2:55
    54: ('nan gaba', 'Nan gaba', None), # 102:3
    55: ('a\'aha / bã haka ba', "A'aha", None), # 102:3
    56: ('shin / lalle ne', 'Lalle ne', None), # 88:1
    57: ('na\'am / hakika', "Na'am", 'Na\'am! Wanda ya sami mummunan aiki kuma kuskurensa ya kewayeshi, to, waɗannan sũ ne \'yan Wuta, su madawwama ne a cikinta.'), # 2:81
    58: ('na\'am / i', "Na'am", 'Kuma \'yan Aljanna suka kira \'yan Wuta: "Haƙĩƙa mun sãmi abin da Ubangijinmu Ya yi mana wa\'adi gaskiya ne, shin kũ ma kun sãmi abin da Ubangijinku Ya yi muku wa\'adi gaskiya ne?" Suka ce: "Na\'am!"'), # 7:44
    59: ('fãce dai / a\'a', 'fãce dai', 'Kuma suka ce: "Ku kasance Yahũdãwa ko kuwa Nasãra, kwa shiryu." Ka ce: "A\'a, fãce dai addinin Ibrãhĩm mai karkata zuwa ga gaskiya."'), # 2:135
    60: ('sai / har', 'sai', None), # 2:55
    61: ('kõ / ko', 'kõ', None), # 2:6
    62: ('to, lalle ne', 'To, lalle ne', None), # 2:12
    63: ('a tsakãnin', 'a tsakãnin', None), # 2:213
    64: ('tallahi', 'Tallahi', None), # 12:73
    65: ('kamar', 'kamar', None), # 2:261
    66: ('ga / wa', 'ga', None), # 1:2
    67: ('da', 'Da', None), # 1:1
    68: ('kuma / da', 'Kuma', None), # 2:4
    69: ('sai / sa\'an nan', 'Sai', None), # 2:36
    70: ('zã su', 'zã su', None), # 2:142
    71: ('ita', 'Ita', None), # 20:18
    72: ('sũ', 'sũ', None), # 2:187
    73: ('na biyun biyu', 'na biyun biyu', None), # 9:40
    74: ('kai', 'Kai', None), # 2:32
    75: ('kũ / kuna', 'kuna', None), # 2:85
    76: ('kun kasance kunã', 'kun kasance kunã', None), # 33:28
    77: ('nĩ / ni', 'Nĩ', None), # 20:14
    78: ('mũ / mu', 'Mũ', None), # 2:11
    79: ('wannan', 'wannan', None), # 2:35
    80: ('waɗannan', 'waɗannan', None), # 2:31
    81: ('waccan', 'Waccan', None), # 2:134
    82: ('wancan', 'wancan', None), # 2:49
    83: ('waɗanda', 'waɗanda', None), # 1:7
    84: ('wadda', 'wadda', None), # 2:24
    85: ('yaushe', 'Yaushe', None), # 2:214
    86: ('a yaushe', 'a yaushe', None), # 7:187
    87: ('ina', 'Ina', None), # 75:10
    88: ('yãya', 'Yãya', None), # 3:40
    89: ('inda', 'inda', None), # 2:35
    90: ('yanzu', 'Yanzu', 'Suka ce: "Yanzu kã kãwo gaskiya." Sai suka soke ta, kuma sun kusa bã zã su aikata ba.'), # 2:71
    91: ('daga gunKa', 'daga gunKa', None), # 3:38
    92: ('akwai tsammãnin', 'Akwai tsammãnin', None), # 17:79
    93: ('bãbu', 'bãbu', None), # 38:3
    94: ('bai i da ... ba', 'bai i da', None), # 80:23
    95: ('wõfinta', 'wõfinta', None), # 2:76
    96: ('kada ku yi tsõkana', 'kada ku yi tsõkana', None), # 2:190
    97: ('tsarki ya tabbata ga Allah', 'Tsarki ya tabbata ga Allah', 'Sai suka ce: "Tsarki ya tabbata ga Allah! Wannan ba mutum ba ne, wannan ba kõme ba ne fãce wani mala\'ika mai daraja."'), # 12:31
    98: ('da yawa', 'da yawa', None), # 3:146
    99: ('kamar wancan', 'Kamar wancan', None), # 2:73
    100: ('a can', 'A can', None), # 3:38
    101: ('wannan wurin', 'wannan wurin', None), # 76:20
    102: ('mãdalla', 'Mãdalla', None), # 38:30
    103: ('tir', 'Tir', None), # 2:90
    104: ('bone', 'Bone', None), # 83:1
    105: ('idan', 'Idan', None), # 110:1
    106: ('yaya', 'Yaya', None), # 2:28
    107: ('amma', 'Amma', None), # 18:79
    108: ('ko ... ko', 'ko', None), # 76:3
    109: ('ban gaya muku ba', 'Ban gaya muku ba', None), # 68:28
    110: ('a lõkacin', 'A lõkacin', None), # 17:75
    111: ('mẽne ne', 'Mẽne ne', None), # 2:26
    112: ('kõ me', 'Kõ me', None), # 7:132
    113: ('inda duk', 'inda duk', None), # 2:144
    114: ('inda duk', 'Inda duk', None), # 4:78
    115: ('abin sani kawai', 'Abin sani kawai', None), # 21:108
    116: ('abin sani kawai', 'Abin sani kawai', None), # 9:60
    117: ('ko da yaushe', 'ko da yaushe', None), # 2:20
    118: ('dõmin', 'dõmin', 'Sai Muka mayar da kai ga mahaifiyarka, dõmin idonta ya yi sanyi, kuma kada ta yi baƙin ciki.'), # 20:40
    119: ('sagaga', 'sagaga', None), # 75:36
    120: ('dõmin kada ku yi baƙin ciki', 'dõmin kada ku yi baƙin ciki', 'Sai Ya sanya muku baƙin ciki a kan wani baƙin ciki, dõmin kada ku yi baƙin ciki a kan abin da ya sãɓa muku da abin da ya sãme ku.'), # 3:153
    121: ('dõmin kada', 'dõmin kada', 'Kuma daga inda kuka fita, to, ku jũyar da fuskõkinku wajensa, dõmin kada mutãne su kasance da wata hujja a kanku.'), # 2:150
    122: ('kada mu bauta', 'kada mu bauta', None), # 3:64
    123: ('hankalta', 'hankalta', 'Shin, kuna umurnin mutãne da alhẽri, kuma ku manta da kanku alhãli kuwa kuna karatun littãfi? Shin, bã zã ku hankalta ba?'), # 2:44
    124: ('ba su yi tafiya ba', 'ba su yi tafiya ba', 'Shin fa, ba su yi tafiya ba a cikin ƙasa dõmin su dũba yadda ãƙibar waɗanda ke a gabãninsu ta kasance?'), # 12:109
    125: ('bã su sanin', 'bã su sanin', None), # 2:77
    126: ('bã su sanin', 'bã su sanin', None), # 2:77
    127: ('ku kãwo', 'Ku kãwo', None), # 6:150
    128: ('ku kãwo', 'Ku kãwo', 'Kuma suka ce: "Bãbu mai shiga Aljanna fãce waɗanda suka zama Yahũdu ko Nasãra." Ka ce: "Ku kãwo hujjarku idan kun kasance mãsu gaskiya."'), # 2:111
    129: ('ku karɓa', 'Ku karɓa', None), # 69:19
    130: ('faufau faufau', 'Faufau faufau', None), # 23:36
    131: ('ƙas / kalma maras kyau', 'ƙas', 'Idan ɗayansu ko duka biyun suka kai ga tsũfa a wurinka, to, kada ka ce musu "ƙas" kuma kada ka tsãwace su.'), # 17:23
    132: ('ĩ / ina rantsuwa', 'Ĩ', None), # 10:53
    133: ('wane abu ne', 'Wane abu ne', None), # 6:19
    134: ('yã', 'Yã', None), # 2:21
    135: ('yã kai', 'Yã kai', None), # 89:27
    136: ('a wurin ƙõfa', 'a wurin ƙõfa', None), # 12:25
    137: ('a bãyansu', 'a bãyansu', '"Amma Jirgin, to, ya zama na waɗansu matalauta ne sunã aiki a cikin tẽku... kuma a bãyansu akwai wani sarki mai ƙwace kõwane jirgi mai kyau."'), # 18:79
    138: ('a gabansa', 'a gabansa', None), # 75:5
    139: ('a bãyansu', 'a bãyansu', 'Yana sanin abin da ke gaba gare su da abin da ke a bãyansu.'), # 2:255
    140: ('a bisanku', 'a bisanku', 'Kuma a lõkacin da Muka riƙi alƙawarinku, kuma Muka ɗaga dũtse a bisanku.'), # 2:63
    141: ('daga ƙarƙashinsu', 'daga ƙarƙashinsu', None), # 2:25
    142: ('dãmansu', 'dãmansu', 'Sa\'an nan lalle zã ni je musu daga gaba gare su, da daga bãyansu, da daga dãmansu, da daga hagunsu.'), # 7:17
    143: ('hagunsu / hagu', 'hagunsu', 'Kuma kana ganin rãna idan ta ɓullo tana karkata daga kõgon dutsensu wajen dãma, kuma idan ta fãɗi tana yanke su wajen hagunsu.'), # 18:17
    144: ('wajen', 'wajen', None), # 7:47
    145: ('gẽfensa', 'gẽfensa', None), # 2:17
    146: ('daga gabãninka', 'daga gabãninka', None), # 2:4
    147: ('daga bãyan', 'daga bãyan', None), # 2:27
    148: ('a kan mẽ', 'A kan mẽ', None), # 78:1
    149: ('me ya haɗã ka', 'Me ya haɗã ka', None), # 79:43
    150: ('da me', 'Da me', None), # 27:35
    151: ('daga mẽ', 'daga mẽ', None), # 86:5
    152: ('don me', 'Don me', None), # 3:65
    153: ('kwanaki nawa', 'Kwanaki nawa', 'Kõ kuwa wanda ya shũɗe a kan wata alƙarya... Ya ce: "Kwanaki nawa ka zauna?" Ya ce: "Na zauna yini ɗaya ko sãshen yini."'), # 2:259
    154: ('waɗanda (mazã biyu)', 'waɗanda (mazã biyu)', None), # 4:16
    155: ('biye-biyen nan', 'wadannan biyun', None), # 41:29
    156: ('waɗanda', 'waɗanda', None), # 24:60
    157: ('waɗanda', 'waɗanda', None), # 33:4
    158: ('hujjõji biyu', 'hujjõji biyu', 'Ka shigar da hannunka a cikin aljihunka ya fita fari tas ba tare da wata cũta ba... Waɗannan hujjõji biyu ne daga Ubangijinka zuwa ga Fir\'auna da jama\'arsa.'), # 28:32
    159: ('waɗannan', 'waɗannan', None), # 20:84
    160: ('a nan', 'a nan', 'Suka ce: "Ya Musa! Lalle ne mu, bã za mu shiga cikinta ba har abada... sai ka tafi kai da Ubangijinka ku yi yãƙi, lalle mu muna zaune a nan."'), # 5:24
    161: ('muku', 'muku', None), # 28:35
    162: ('ke', 'ke', None), # 12:29
    163: ('Kai kaɗai', 'Kai', None), # 1:5
    164: ('fãce Shi', 'fãce Shi', None), # 17:23
    165: ('gare Ni / Ni kaɗai', 'gare Ni', 'Yã Banĩ Isrã\'ĩla! Ku tuna ni\'imãTa da Na yi muku, kuma ku cika alƙawariNa, In cika muku alƙawarinku, kuma gare Ni kaɗai ku ji tsoro.'), # 2:40
    166: ('mũ', 'mũ', 'Waɗanda magana ta wajaba a kansu suka ce: "Yã Ubangijinmu! Waɗannan sũ ne waɗanda muka halakar... bã mũ suka kasance sunã bauta wa ba."'), # 28:63
    167: ('kõ ku / kũ', 'kõ ku', None), # 34:24
    168: ('su da ku', 'su da ku', 'Kada ku kashe \'ya\'yanku sabõda tsoron talauci, Mũ ne Muke azurta su da ku.'), # 6:151
    169: ('dõmin me bã zã ka', 'Dõmin me bã zã ka', None), # 15:7
    170: ('bãbu makawã', 'Bãbu makawã', None), # 11:22
    171: ('suka shiga', 'suka shiga', 'Sa\'an nan a lõkacin da suka ɗanɗani itãciyar, al\'aurarsu ta bayyana gare su kuma suka shiga liƙa ganyen Aljanna a kansu.'), # 7:22
    172: ('tana yin kusa', 'tana yin kusa', None), # 2:20
    173: ('kõwane', 'Kõwane', None), # 17:110
    174: ('wai dõmin ba su yi ĩmãni ba', 'ba su yi ĩmãni', None), # 18:6
    175: ('fãce idan', 'fãce idan', 'Kuma ba ya halatta a gare ku ku karɓi wani abu daga abin da kuka ba su, fãce idan su biyun sun ji tsoron bã zã su tsayar da dokokin Allah ba.'), # 2:229
    176: ('lalle ne', 'lalle ne', None), # 2:143
    177: ('har abada', 'har abada', 'Kuma bã zã su yi gũrinta ba har abada sabõda abin da hannayensu suka gabãtar.'), # 2:95
    178: ('jiya', 'jiya', 'Sai Muka mayar da ita ƙẽƙasasshiya kamar ba ta yi albarka ba a jiya.'), # 10:24
    179: ('a gõbe', 'a gõbe', None), # 18:23
    180: ('sãfiyar', 'sãfiyar', None), # 37:177
    181: ('marẽce', 'marẽce', None), # 79:46
    182: ('hantsi', 'hantsi', None), # 20:59
    183: ('a lõkacin asuba', 'a lõkacin asuba', None), # 54:34
    184: ('da dare', 'da dare', None), # 17:1
    185: ('zamani', 'zamani', None), # 76:1
    186: ('a cikin hãlãye', 'a cikin hãlãye', None), # 71:14
    187: ('gabã ɗaya', 'gabã ɗaya', 'Yã kũ waɗanda suka yi ĩmãni! Ku riƙi shirinku sa\'an nan ku fitar da hari jama\'a jama\'a ko ku fitar da shi gabã ɗaya.'), # 4:71
    188: ('daidai ne', 'daidai ne', None), # 2:6
    189: ('ba', 'ba', None), # 1:7
    190: ('bãcin / wanin', 'bãcin', 'To, ku zõ da sũra guda misãlinta, kuma ku kira shaidunku bãcin Allah idan kun kasance mãsu gaskiya.'), # 2:23
    191: ('madaidaici', 'madaidaici', 'Sai ka sanya wani wa\'adi a tsakãninmu da tsakãninka, a wani wuri madaidaici, wanda bã zamu sãɓa masa ba, kuma kai ma bã zaka sãɓa ba.'), # 20:58
    192: ('misãlin', 'misãlin', None), # 2:137
    193: ('su biyun', 'su biyun', 'Idan ɗayansu ko kuma su biyun suka kai ga tsũfa a wurinka, kada ka ce musu "ƙas".'), # 17:23
}

fails = []
for entry in canonical:
    r = entry['row']
    if r not in HA_MAP:
        fails.append((r, 'Missing from HA_MAP'))
        continue
    m, t, custom_v = HA_MAP[r]
    txt = custom_v if custom_v else ha_dict.get(entry['vk'], '')
    if t not in txt:
        fails.append((r, f'"{t}" not in text: {txt[:40]}...'))

print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('FAIL:', f)
if not fails:
    print('ALL 192 ROWS PERFECT!')
