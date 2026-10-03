import json, re, openpyxl

with open('db/harf_canonical_192.json') as f:
    rows = json.load(f)

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
with open('db/corpus_data/quran_ms.json') as f:
    ms_raw = json.load(f)['translations']

ms_dict = {}
for u, t in zip(uth_list, ms_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    ms_dict[u['verse_key']] = txt

# Complete curated Malay mappings: (meaning, target_word, optional_custom_verse_text)
MS_MAP = {
    2: ('dari', 'dari', None), # 2:22
    3: ('dari / sebahagian dari', 'dari', None), # 2:25
    4: ('dalam / mengenai', 'mengenai', None), # 2:176
    5: ('mengenai / tentang', 'tentang', 'Dan sesungguhnya kamu telah mengetahui orang-orang di antara kamu yang melanggar (larangan) tentang hari Sabat, lalu Kami berfirman kepada mereka: "Jadilah kamu kera yang hina!"'), # 2:65
    6: ('sesungguhnya', 'Sesungguhnya', None), # 2:6
    7: ('sesungguhnya', 'sesungguhnya', None), # 2:143
    8: ('atas / di atas', 'di atas', 'Mereka itulah yang tetap berada di atas petunjuk dari Tuhan mereka, dan merekalah orang-orang yang berjaya.'), # 2:5
    9: ('atas / terhadap', 'atas', None), # 2:89
    10: ('yang', 'yang', None), # 2:17
    11: ('yang', 'yang', None), # 2:25
    12: ('tidak ada / tiada', 'tidak ada', None), # 2:2
    13: ('janganlah', 'Janganlah', None), # 2:11
    14: ('apa yang', 'apa yang', None), # 16:96
    15: ('tidak', 'tidak', None), # 93:3
    16: ('sehingga / sampai', 'sehingga', None), # 2:187
    17: ('kepada', 'kepada', None), # 2:257
    18: ('ada yang / orang yang', 'ada yang', None), # 2:8
    19: ('sesiapa yang / orang yang', 'sesiapa yang', None), # 2:112
    20: ('jika / sekiranya', 'jika', None), # 2:278
    21: ('tidak lain / bukan', 'tidak lain', None), # 6:25
    22: ('bahawa / untuk', 'bahawa', None), # 2:26
    23: ('melainkan / kecuali', 'melainkan', 'Mereka hendak memperdayakan Allah dan orang-orang yang beriman, padahal mereka tidak memperdayakan melainkan dirinya sendiri, sedang mereka tidak menyedarinya.'), # 2:9
    24: ('selain / lain dari', 'lain dari', None), # 21:22
    25: ('itu', 'itu', 'Kitab Al-Quran itu tidak ada sebarang syak padanya; ia menjadi petunjuk bagi orang-orang yang bertaqwa.'), # 2:2
    26: ('dari / daripada', 'dari', None), # 2:48
    27: ('dari / berpaling dari', 'dari', None), # 2:142
    28: ('kerap kali / sesungguhnya', 'Kerap kali', None), # 2:144
    29: ('telah / sesungguhnya', 'telah', None), # 2:256
    30: ('sesungguhnya / bahawa', 'sesungguhnya', None), # 2:25
    31: ('tiada / tidak', 'tiada', None), # 112:3
    32: ('tidak', 'tidak', None), # 2:6
    33: ('kemudian', 'kemudian', None), # 2:28
    34: ('inilah / ini', 'Inilah', None), # 2:25
    35: ('atau', 'atau', None), # 76:24
    36: ('ketika', 'ketika', None), # 2:133
    37: ('ketika / waktu', 'ketika', None), # 2:133
    38: ('mereka itulah', 'Mereka itulah', None), # 2:5
    39: ('kalaulah / sekiranya', 'kalaulah', None), # 2:102
    40: ('di sisi', 'di sisi', 'Sesungguhnya orang-orang yang beriman, orang-orang Yahudi, orang-orang Nasrani dan orang-orang Sabiah, sesiapa yang beriman kepada Allah dan hari akhirat serta berbuat kebajikan, mereka beroleh pahala di sisi Tuhan mereka, tidak ada kebimbangan terhadap mereka, dan mereka tidak akan berdukacita.'), # 2:62
    41: ('di sisi', 'di sisi', 'Dan orang-orang kafir berkata: "Engkau bukanlah seorang Rasul." Katakanlah: "Cukuplah Allah menjadi saksi antaraku dengan kamu dan orang yang mempunyai pengetahuan di sisi Kitab."'), # 13:43
    42: ('bersama / beserta', 'bersama', None), # 2:14
    43: ('bersama / beserta', 'bersama', None), # 2:14
    44: ('Dia / Dialah', 'Dia lah', None), # 2:29
    45: ('Dia / Dialah', 'Dia lah', 'Katakanlah (wahai Muhammad): "Dia lah Allah Yang Maha Esa;'), # 112:1
    46: ('mereka', 'mereka', None), # 2:38
    47: ('mereka', 'mereka', None), # 2:38
    48: ('wahai', 'Wahai', None), # 2:21
    49: ('tetapi / namun', 'Tetapi', None), # 3:198
    50: ('supaya / agar', 'supaya', None), # 2:21
    51: ('seoleh-oleh / seolah-olah', 'seoleh-oleh', None), # 31:7
    52: ('alangkah baiknya kalau / sekiranya', 'sekiranya', 'Lalu rasa sakit hendak bersalin memaksanya pergi ke pangkal sebatang pohon kurma, ia berkata: "Alangkah baiknya kalau sekiranya aku mati sebelum ini, dan menjadilah aku sesuatu yang dilupakan sama sekali!"'), # 19:23
    53: ('tidak akan / tidak sekali-kali', 'tidak akan', None), # 2:55
    54: ('kelak / nanti', 'kelak', None), # 102:3
    55: ('jangan sekali-kali / sekali-kali tidak', 'Jangan sekali-kali', None), # 102:3
    56: ('sudahkah / adakah', 'Sudahkah', None), # 88:1
    57: ('bahkan / ya', 'Bahkan', 'Bahkan! Sesiapa yang berbuat kejahatan dan ia diliputi oleh kesalahannya itu, maka merekalah ahli neraka, mereka kekal di dalamnya.'), # 2:81
    58: ('benar / ya', 'Benar', None), # 7:44
    59: ('bahkan / bahkan sebenarnya', 'bahkan', None), # 2:135
    60: ('sehingga / hingga', 'sehingga', None), # 2:55
    61: ('atau', 'atau', None), # 2:6
    62: ('ketahuilah / ingatlah', 'Ketahuilah', None), # 2:12
    63: ('antara', 'antara', None), # 2:213
    64: ('demi Allah', 'Demi Allah', None), # 12:73
    65: ('seperti / bagaikan', 'seperti', 'Bandingan pahala orang-orang yang membelanjakan harta mereka pada jalan Allah seperti sebiji benih yang menumbuhkan tujuh tangkai, pada setiap tangkai ada seratus biji.'), # 2:261
    66: ('bagi / kepunyaan', 'bagi', None), # 1:2
    67: ('dengan', 'Dengan', None), # 1:1
    68: ('dan', 'dan', None), # 2:4
    69: ('maka / lalu', 'maka', None), # 2:36
    70: ('akan', 'akan', None), # 2:142
    71: ('iaitu / ini', 'Ini', None), # 20:18
    72: ('mereka (fem.)', 'mereka', None), # 2:187
    73: ('mereka berdua / keduanya', 'mereka berdua', 'Kalau kamu tidak menolongnya (Nabi Muhammad) maka sesungguhnya Allah telah menolongnya ketika kaum kafir mengeluarkannya, sedang mereka berdua berada di dalam gua, ketika ia berkata kepada sahabatnya: "Janganlah engkau berdukacita, sesungguhnya Allah bersama kita."'), # 9:40
    74: ('Engkau', 'Engkau', None), # 2:32
    75: ('kamu', 'kamu', None), # 2:85
    76: ('kamu (fem.)', 'kamu', None), # 33:28
    77: ('Aku', 'Aku', None), # 20:14
    78: ('Kami', 'Kami', None), # 2:11
    79: ('ini (fem.)', 'ini', None), # 2:35
    80: ('ini / mereka ini', 'ini', None), # 2:31
    81: ('itu (fem.)', 'itu', None), # 2:134
    82: ('demikian itu / itu', 'yang demikian itu', None), # 2:49
    83: ('orang-orang yang', 'orang-orang yang', None), # 1:7
    84: ('yang (fem.)', 'yang', None), # 2:24
    85: ('bilakah / bila', 'Bilakah', None), # 2:214
    86: ('bila / bila berlakunya', 'bilakah', None), # 7:187
    87: ('ke mana / di mana', 'Ke mana', 'Pada hari itu manusia bertanya: "Ke mana hendak melarikan diri?"'), # 75:10
    88: ('bagaimanakah / dari mana', 'Bagaimanakah', None), # 3:40
    89: ('dari mana sahaja / di mana sahaja', 'di mana sahaja', 'Dan Kami berfirman: "Wahai Adam! Tinggallah engkau dan isterimu dalam syurga, dan makanlah dari buah-buahannya dengan nikmat di mana sahaja kamu sukai, dan janganlah kamu hampiri pokok ini, nanti kamu menjadi orang-orang yang zalim."'), # 2:35
    90: ('sekarang', 'Sekarang', None), # 2:71
    91: ('dari sisi-Mu', 'dari sisiMu', None), # 3:38
    92: ('semoga / mudah-mudahan', 'semoga', None), # 17:79
    93: ('tiada lagi / tidak ada', 'tidak ada', 'Berapa banyak umat sebelum mereka yang telah Kami binasakan, lalu mereka meminta tolong padahal tidak ada lagi waktu untuk lari.'), # 38:3
    94: ('belum lagi / belum', 'belum lagi', 'Janganlah berbuat demikian! Manusia belum lagi melaksanakan apa yang diperintahkan kepadanya.'), # 80:23
    95: ('bersendirian / kembali', 'bersendirian', 'Dan apabila mereka bertemu dengan orang-orang yang beriman, mereka berkata: "Kami telah beriman", dan apabila mereka bersendirian sesama sendiri, mereka berkata: "Patutkah kamu ceritakan kepada mereka apa yang telah diterangkan Allah kepada kamu?"'), # 2:76
    96: ('menceroboh / melampaui batas', 'menceroboh', None), # 2:190
    97: ('Maha Suci Allah / jauh dari kekurangan', 'Jauhnya Allah dari kekurangan!', None), # 12:31
    98: ('berapa banyak', 'Berapa banyak', 'Berapa banyak nabi yang berperang bersama-sama sejumlah besar pengikut yang bertakwa. Mereka tidak menjadi lemah kerana bencana yang menimpa mereka pada jalan Allah.'), # 3:146
    99: ('demikianlah', 'Demikianlah', None), # 2:73
    100: ('di sanalah / ketika itu', 'Ketika itu', None), # 3:38
    101: ('di sana', 'di sana', None), # 76:20
    102: ('sebaik-baik', 'sebaik-baik', None), # 38:30
    103: ('sejahat-jahat / seburuk-buruk', 'Sejahat-jahat', None), # 2:90
    104: ('celakalah / kecelakaan besar', 'Kecelakaan besar', None), # 83:1
    105: ('apabila', 'Apabila', None), # 110:1
    106: ('bagaimana', 'Bagaimana', None), # 2:28
    107: ('adapun', 'Adapun', None), # 18:79
    108: ('sama ada ... mahupun', 'sama ada', None), # 76:3
    109: ('mengapa kamu tidak / tidakkah', 'mengapa kamu tidak', 'Berkatalah orang yang paling adil di antara mereka: "Bukankah aku telah katakan kepada kamu: mengapa kamu tidak bertasbih memuji Allah?"'), # 68:28
    110: ('tentulah / kalau demikian', 'tentulah', None), # 17:75
    111: ('apakah / apa maksud', 'Apakah', None), # 2:26
    112: ('walau apa sahaja / apa jua', 'Walau apa sahaja', None), # 7:132
    113: ('di mana sahaja', 'di mana sahaja', None), # 2:144
    114: ('di mana jua / di mana sahaja', 'Di mana jua', None), # 4:78
    115: ('bahawa', 'bahawa', None), # 21:108
    116: ('hanyalah / hanya', 'hanyalah', None), # 9:60
    117: ('tiap-tiap kali / setiap kali', 'tiap-tiap kali', None), # 2:20
    118: ('supaya / agar', 'supaya', None), # 20:40
    119: ('sia-sia / terbiar', 'terbiar', None), # 75:36
    120: ('supaya kamu tidak', 'supaya kamu tidak', None), # 3:153
    121: ('supaya tidak', 'supaya tidak', None), # 2:150
    122: ('bahawa kita tidak / jangan kita', 'bahawa kita tidak', 'Katakanlah: "Wahai Ahli Kitab! Marilah kepada satu kalimah yang bersamaan antara kami dengan kamu, iaitu bahawa kita tidak menyembah melainkan Allah, dan kita tidak persekutukan sesuatu pun dengan-Nya."'), # 3:64
    123: ('tidakkah kamu', 'tidakkah kamu', None), # 2:44
    124: ('tidakkah mereka / mengapa tidak', 'tidakkah mereka', 'Dan tiadalah Kami mengutus Rasul sebelummu melainkan orang-orang lelaki yang Kami wahyukan kepada mereka; maka tidakkah mereka mengembara di muka bumi supaya memerhatikan bagaimana akibat orang-orang terdahulu?'), # 12:109
    125: ('tidakkah mereka ketahui / tahu', 'tidakkah mereka ketahui', None), # 2:77
    126: ('tidakkah mereka ketahui / tahu', 'tidakkah mereka ketahui', None), # 2:77
    127: ('bawalah kemari', 'Bawalah', None), # 6:150
    128: ('bawalah / tunjukkanlah', 'Bawalah', None), # 2:111
    129: ('nah bacalah / ambillah', 'Nah! Bacalah', None), # 69:19
    130: ('jauh / amat jauh', 'Jauh', None), # 23:36
    131: ('ah / cis', 'ah', None), # 17:23
    132: ('ya / bahkan', 'Ya', None), # 10:53
    133: ('apakah / apa bendanya', 'Apakah', 'Katakanlah: "Apakah sesuatu yang lebih besar kesaksiannya?" Katakanlah: "Allah menjadi saksi antaraku dengan kamu."'), # 6:19
    134: ('wahai', 'Wahai', None), # 2:21
    135: ('wahai (fem.)', 'Wahai', None), # 89:27
    136: ('di dekat / di muka', 'di muka', None), # 12:25
    137: ('di belakang / di hadapan', 'di belakang', None), # 18:79
    138: ('ke hadapan / di hadapannya', 'di hadapannya', 'Bahkan manusia mahu meneruskan maksiat di hadapannya (pada masa akan datang).'), # 75:5
    139: ('di belakang / di hadapan', 'di belakang', None), # 2:255
    140: ('ke atas / di atas', 'di atas', None), # 2:63
    141: ('di bawah', 'di bawah', None), # 2:25
    142: ('kanan / sebelah kanan', 'kanan', None), # 7:17
    143: ('arah kiri / sebelah kiri', 'arah kiri', None), # 18:17
    144: ('ke arah / ke pihak', 'ke arah', None), # 7:47
    145: ('di sekeliling / di sekelilingnya', 'sekelilingnya', None), # 2:17
    146: ('dahulu / sebelum', 'dahulu', None), # 2:4
    147: ('selepas / sesudah', 'sesudah', None), # 2:27
    148: ('tentang apa', 'Tentang apakah', None), # 78:1
    149: ('dalam hal apa', 'apa hubungannya', None), # 79:43
    150: ('dengan apa / apakah', 'apakah balasan', None), # 27:35
    151: ('dari apa', 'dari apa', None), # 86:5
    152: ('mengapa', 'Mengapa', None), # 3:65
    153: ('berapa lama', 'Berapa lama', None), # 2:259
    154: ('dua orang di antara kamu yang', 'dua orang di antara kamu yang', None), # 4:16
    155: ('dua golongan yang / kedua-dua yang', 'dua golongan yang', None), # 41:29
    156: ('yang / perempuan-perempuan yang', 'yang', 'Dan perempuan-perempuan tua yang telah putus haid yang tidak ada harapan berkahwin lagi, maka tidak ada dosa bagi mereka menanggalkan pakaian luar mereka.'), # 24:60
    157: ('isteri-isteri yang / wanita-wanita yang', 'isteri-isteri yang', None), # 33:4
    158: ('dua mukjizat', 'dua mukjizat', 'Masukkanlah tanganmu ke leher bajumu, nescaya keluarlah ia putih bersinar-sinar tanpa cacat; itulah dua mukjizat dari Tuhanmu kepada Firaun dan kaumnya.'), # 28:32
    159: ('mereka ini', 'Mereka itu', None), # 20:84
    160: ('di sini', 'di sini', 'Mereka berkata: "Wahai Musa, sesungguhnya kami tidak akan memasukinya selama mereka ada di dalamnya. Oleh itu pergilah engkau bersama Tuhanmu dan berperanglah kamu berdua, sesungguhnya kami hanya duduk menanti di sini sahaja."'), # 5:24
    161: ('kamu berdua', 'kamu berdua', None), # 28:35
    162: ('engkau (fem.)', 'engkau', None), # 12:29
    163: ('kepada Engkau / Engkaulah', 'Engkaulah', None), # 1:5
    164: ('kepada-Nya / melainkan kepada-Nya', 'kepada-Nya', 'Dan Tuhanmu telah memerintahkan supaya kamu tidak menyembah melainkan kepada-Nya semata-mata, dan hendaklah berbuat baik kepada ibu bapa.'), # 17:23
    165: ('kepada-Ku / kepada-Kulah', 'kepada-Kulah', 'Wahai Bani Israil! Kenangkanlah nikmat-Ku yang telah Kuberikan kepadamu, dan penuhilah janjimu kepada-Ku, nescaya Aku penuhi janji-Ku kepadamu, dan kepada-Kulah sahaja hendaklah kamu takut.'), # 2:40
    166: ('kami', 'kami', None), # 28:63
    167: ('kamu sekalian / kamu', 'kamu', None), # 34:24
    168: ('mereka', 'mereka', None), # 6:151
    169: ('mengapa tidak', 'Sepatutnya', None), # 15:7
    170: ('tidak syak lagi / pasti', 'Tidak syak lagi', None), # 11:22
    171: ('mulailah keduanya', 'mulai', 'Maka setelah mereka merasa pokok itu, terzahir kotoran mereka dan mulailah keduanya menutup aurat dengan daun-daun syurga.'), # 7:22
    172: ('hampir-hampir / hampir', 'hampir-hampir', None), # 2:20
    173: ('mana sahaja', 'mana sahaja', None), # 17:110
    174: ('sekiranya mereka tidak / jika tidak', 'sekiranya mereka tidak', 'Maka jangan-jangan engkau membinasakan dirimu kerana berdukacita terhadap perbuatan mereka, sekiranya mereka tidak beriman kepada keterangan ini.'), # 18:6
    175: ('kecuali jika', 'kecuali jika', 'Talak itu dua kali; sesudah itu bolehlah rujuk dengan cara baik atau melepaskan dengan baik. Tidak halal bagi kamu mengambil semula sesuatu yang telah kamu berikan kepada mereka, kecuali jika keduanya bimbang tidak dapat menjalankan hukum-hukum Allah.'), # 2:229
    176: ('sungguh / sesungguhnya', 'sungguh', 'Dan demikianlah Kami jadikan kamu satu umat yang adil, dan sungguh Allah Amat melimpah belas kasihan-Nya kepada manusia.'), # 2:143
    177: ('selama-lamanya', 'selama-lamanya', None), # 2:95
    178: ('kelmarin / semalam', 'semalam', 'Sesungguhnya perumpamaan kehidupan dunia adalah seperti air hujan yang Kami turunkan dari langit, lalu Kami menjadikannya kering layu seolah-olah tidak ada semalam.'), # 10:24
    179: ('esok', 'esok', 'Dan janganlah engkau berkata terhadap sesuatu: "Sesungguhnya aku akan melakukan perkara itu esok hari."'), # 18:23
    180: ('pagi hari / pada waktu pagi', 'waktu pagi', 'Maka apabila azab itu turun di halaman mereka, amat buruklah waktu pagi orang-orang yang telah diberi amaran.'), # 37:177
    181: ('petang / waktu petang', 'petang', 'Pada hari mereka melihat hari kiamat itu, mereka merasa seolah-olah tidak tinggal di dunia melainkan sekadar waktu petang atau pagi hari sahaja.'), # 79:46
    182: ('pagi hari / waktu siang', 'siang hari', 'Nabi Musa menjawab: "Waktu yang dijanjikan untuk kamu ialah hari perayaan, dan hendaklah orang ramai berkumpul pada waktu siang hari."'), # 20:59
    183: ('sebelum fajar / akhir malam', 'sebelum fajar', 'Sesungguhnya Kami hantarkan kepada mereka angin ribut yang merejam batu, kecuali keluarga Lut yang Kami selamatkan pada waktu sebelum fajar.'), # 54:34
    184: ('malam hari', 'malam hari', None), # 17:1
    185: ('suatu ketika / masa', 'satu ketika', None), # 76:1
    186: ('berperingkat-peringkat / bertahap', 'berperingkat-peringkat', None), # 71:14
    187: ('serentak beramai-ramai / bersama-sama', 'serentak beramai-ramai', None), # 4:71
    188: ('sama sahaja', 'sama sahaja', None), # 2:6
    189: ('bukan', 'bukan', None), # 1:7
    190: ('selain', 'selain', None), # 2:23
    191: ('tempat yang terbuka / adil', 'terbuka', 'Kalau demikian, sesungguhnya kami juga akan bawakan kepadamu sihir yang seperti itu, maka tentukanlah satu pertemuan antara kami dengan kamu di satu tempat yang terbuka.'), # 20:58
    192: ('sebagaimana', 'sebagaimana', None), # 2:137
    193: ('kedua-duanya', 'kedua-duanya', 'Dan Tuhanmu telah perintahkan supaya kamu tidak menyembah melainkan kepada-Nya, dan jika salah seorang atau kedua-duanya sampai berumur lanjut, maka janganlah engkau berkata "ah" kepada keduanya.'), # 17:23
}

wb_ms = openpyxl.load_workbook('db/qw_ms.xlsx')
sheet_ms = wb_ms['Harf']

print('Applying curated Malay mappings to qw_ms.xlsx...')
fails = []

for r_data in rows:
    r_num = r_data['row']
    rank = r_data['rank']
    sense = r_data['sense']
    vk = r_data['vk']
    cit = r_data['cit']
    
    ms_m, ms_t, custom_v = MS_MAP.get(r_num, (r_data['en_m'], 'TODO', None))
    v_txt = custom_v if custom_v else ms_dict.get(vk, '')
    
    # Check if ms_t in v_txt
    m = re.search(r'\b' + re.escape(ms_t) + r'\b', v_txt, re.I)
    if not m:
        idx = v_txt.find(ms_t)
        if idx == -1:
            fails.append((r_num, rank, r_data['lemma'], ms_t, cit, v_txt[:60]))
            continue
        start, end = idx, idx + len(ms_t)
    else:
        start, end = m.start(), m.end()
        
    v_bracketed = v_txt[:start] + f'([{v_txt[start:end]}])' + v_txt[end:]
    actual_t = v_txt[start:end]
    
    # Write canonical data
    sheet_ms.cell(row=r_num, column=1).value = r_data['rank']
    sheet_ms.cell(row=r_num, column=2).value = r_data['lemma']
    sheet_ms.cell(row=r_num, column=3).value = r_data['translit']
    sheet_ms.cell(row=r_num, column=4).value = r_data['root']
    sheet_ms.cell(row=r_num, column=5).value = r_data['pos']
    sheet_ms.cell(row=r_num, column=6).value = r_data['occ']
    sheet_ms.cell(row=r_num, column=7).value = r_data['cov']
    sheet_ms.cell(row=r_num, column=8).value = r_data['sense']
    sheet_ms.cell(row=r_num, column=9).value = ms_m
    sheet_ms.cell(row=r_num, column=10).value = cit
    sheet_ms.cell(row=r_num, column=11).value = r_data['ar_t']
    sheet_ms.cell(row=r_num, column=12).value = r_data['full_ar']
    sheet_ms.cell(row=r_num, column=13).value = actual_t
    sheet_ms.cell(row=r_num, column=14).value = v_bracketed

if sheet_ms.max_row > 193:
    sheet_ms.delete_rows(194, sheet_ms.max_row - 193)

wb_ms.save('db/qw_ms.xlsx')
print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('  FAIL:', f)
print('Saved db/qw_ms.xlsx successfully.')
