import json, re, openpyxl

with open('db/harf_canonical_192.json') as f:
    rows = json.load(f)

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
with open('db/corpus_data/quran_id.json') as f:
    id_raw = json.load(f)['translations']

id_dict = {}
for u, t in zip(uth_list, id_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    id_dict[u['verse_key']] = txt

# Complete Indonesian curated overrides / mappings
ID_MAP = {
    2: ('dari', 'dari', None), # 2:22
    3: ('dari / di antara', 'dari', None), # 2:25
    4: ('dalam / dengan', 'dengan', None), # 2:176
    5: ('mengenai / tentang', 'tentang', 'Dan sungguh, kamu telah mengetahui orang-orang yang melakukan pelanggaran di antara kamu tentang hari Sabat, lalu Kami katakan kepada mereka, "Jadilah kamu kera yang hina!"'), # 2:65
    6: ('sesungguhnya / sungguh', 'Sesungguhnya', None), # 2:6
    7: ('sesungguhnya / sungguh', 'sungguh', None), # 2:143
    8: ('di atas / atas', 'di atas', 'Mereka itulah yang berada di atas petunjuk dari Tuhan mereka, dan mereka itulah orang-orang yang beruntung.'), # 2:5
    9: ('terhadap / atas', 'terhadap', 'Dan setelah sampai kepada mereka Kitab (Alquran) dari Allah yang membenarkan apa yang ada pada mereka—padahal sebelumnya mereka memohon kemenangan terhadap orang-orang kafir—maka setelah sampai kepada mereka apa yang telah mereka ketahui, mereka mengingkarinya. Maka laknat Allah bagi orang-orang yang ingkar.'), # 2:89
    10: ('yang', 'yang', None), # 2:17
    11: ('yang', 'yang', None), # 2:25
    12: ('tidak / tiada', 'tidak', None), # 2:2
    13: ('janganlah', 'janganlah', None), # 2:22
    14: ('apa yang', 'apa yang', None), # 16:96
    15: ('tidak', 'tidak', None), # 2:9
    16: ('ke / kepada', 'ke', None), # 2:28
    17: ('ke / kepada', 'ke', None), # 2:257
    18: ('orang yang / siapa', 'orang yang', None), # 2:8
    19: ('barang siapa / siapa saja', 'Barang siapa', None), # 2:112
    20: ('jika / jikalau', 'jika', None), # 2:278
    21: ('tidak lain / bukan', 'tidak lain', None), # 6:25
    22: ('bahwa / untuk', 'bahwa', None), # 2:26
    23: ('kecuali / selain', 'kecuali', 'Mereka menipu Allah dan orang-orang yang beriman, padahal mereka tidak menipu kecuali diri mereka sendiri tanpa mereka sadari.'), # 2:9
    24: ('selain / kecuali', 'selain', None), # 21:22
    25: ('itu / demikian', 'itu', 'Kitab itu tidak ada keraguan padanya; petunjuk bagi mereka yang bertakwa,'), # 2:2
    26: ('dari', 'dari', None), # 2:48
    27: ('dari', 'dari', 'Orang-orang yang kurang akal di antara manusia akan berkata, "Apakah yang memalingkan mereka dari kiblat yang dahulu mereka berkiblat kepadanya?" Katakanlah, "Milik Allah-lah timur dan barat; Dia memberi petunjuk kepada siapa yang Dia kehendaki ke jalan yang lurus."'), # 2:142
    28: ('sungguh / benar-benar', 'sungguh', 'Sungguh Kami melihat wajahmu sering menengadah ke langit, maka akan Kami palingkan engkau ke kiblat yang engkau senangi. Maka hadapkanlah wajahmu ke arah Masjidilharam.'), # 2:144
    29: ('telah / sungguh', 'telah', None), # 2:256
    30: ('bahwa', 'bahwa', None), # 2:25
    31: ('tidak', 'tidak', None), # 112:3
    32: ('tidak', 'tidak', None), # 2:6
    33: ('kemudian / lalu', 'kemudian', None), # 2:28
    34: ('ini / inilah', 'Inilah', None), # 2:25
    35: ('atau', 'atau', 'Maka bersabarlah untuk ketetapan Tuhanmu, dan janganlah engkau ikuti dari mereka orang yang berdosa atau orang yang kufur.'), # 76:24
    36: ('ketika / saat', 'ketika', None), # 2:133
    37: ('ketika / waktu', 'ketika', None), # 2:133
    38: ('mereka itulah', 'Merekalah', None), # 2:5
    39: ('sekiranya / jika', 'sekiranya', None), # 2:102
    40: ('di sisi', 'di sisi', 'Sesungguhnya orang-orang yang beriman, orang-orang Yahudi, orang-orang Nasrani dan orang-orang Shabiin, siapa saja yang beriman kepada Allah dan hari akhir serta berbuat kebajikan, mereka mendapat pahala di sisi Tuhannya, tidak ada rasa takut pada mereka, dan mereka tidak bersedih hati.'), # 2:62
    41: ('di sisi', 'di sisi', 'Dan orang-orang kafir berkata, "Engkau bukanlah seorang Rasul." Katakanlah, "Cukuplah Allah menjadi saksi antara aku dan kamu dan orang yang mempunyai pengetahuan di sisi Kitab."'), # 13:43
    42: ('bersama / beserta', 'bersama', None), # 2:14
    43: ('bersama / beserta', 'bersama', None), # 2:14
    44: ('Dia / Dialah', 'Dialah', None), # 2:29
    45: ('Dia / Dialah', 'Dialah', None), # 112:1
    46: ('mereka', 'mereka', None), # 2:38
    47: ('mereka', 'mereka', None), # 2:38
    48: ('wahai', 'Wahai', None), # 2:21
    49: ('tetapi / namun', 'Tetapi', None), # 3:198
    50: ('agar / supaya', 'agar', None), # 2:21
    51: ('seolah-olah / bagaikan', 'seolah-olah', None), # 31:7
    52: ('sekiranya / andai', 'sekiranya', 'Kemudian rasa sakit akan melahirkan memaksanya pada pangkal pohon kurma, dia berkata, "Wahai, betapa baiknya sekiranya aku mati sebelum ini, dan aku menjadi seorang yang tidak diperhatikan dan dilupakan."'), # 19:23
    53: ('tidak akan / sekali-kali tidak', 'tidak akan', None), # 2:55
    54: ('kelak / nanti', 'Kelak', None), # 102:3
    55: ('sekali-kali tidak', 'Sekali-kali tidak', None), # 102:3
    56: ('sudahkah / apakah', 'Sudahkah', None), # 88:1
    57: ('tidaklah demikian / bahkan / ya', 'Tidaklah demikian!', None), # 2:81
    58: ('ya / benar', 'Benar', None), # 7:44
    59: ('bahkan / tetapi', 'Tetapi', None), # 2:135
    60: ('hingga / sampai', 'sebelum', None), # 2:55
    61: ('atau / apakah', 'atau', None), # 2:6
    62: ('ingatlah / ketahuilah', 'Ingatlah', 'Ingatlah, sesungguhnya merekalah yang berbuat kerusakan, tetapi mereka tidak menyadari.'), # 2:12
    63: ('antara / di antara', 'antara', None), # 2:213
    64: ('demi Allah', 'Demi Allah', None), # 12:73
    65: ('seperti / bagaikan', 'seperti', 'Perumpamaan orang yang menginfakkan hartanya di jalan Allah seperti sebutir biji yang menumbuhkan tujuh tangkai, pada setiap tangkai ada seratus biji. Allah melipatgandakan bagi siapa yang Dia kehendaki, dan Allah Mahaluas, Maha Mengetahui.'), # 2:261
    66: ('untuk / bagi / milik', 'bagi', None), # 1:2
    67: ('dengan', 'Dengan', None), # 1:1
    68: ('dan', 'dan', None), # 2:4
    69: ('lalu / maka', 'Lalu', None), # 2:36
    70: ('akan', 'akan', None), # 2:142
    71: ('dia / itu', 'Ini', None), # 20:18
    72: ('mereka (fem.)', 'mereka', None), # 2:187
    73: ('keduanya', 'keduanya', None), # 9:40
    74: ('Engkau', 'Engkau', None), # 2:32
    75: ('kamu / kalian', 'kamu', None), # 2:85
    76: ('kamu (fem. pl.)', 'kamu', None), # 33:28
    77: ('Aku', 'Aku', None), # 20:14
    78: ('kami / kita', 'Kami', None), # 2:11
    79: ('ini (fem.)', 'ini', None), # 2:35
    80: ('ini / mereka ini', 'ini', None), # 2:31
    81: ('itu (fem.)', 'Itu', None), # 2:134
    82: ('itu / demikianlah', 'yang demikian itu', None), # 2:49
    83: ('orang-orang yang', 'orang-orang yang', None), # 1:7
    84: ('yang (fem.)', 'yang', None), # 2:24
    85: ('kapan / bilakah', 'Kapankah', None), # 2:214
    86: ('kapan / bilakah', 'kapan', None), # 7:187
    87: ('di mana / ke mana', 'Kemana', 'Pada hari itu manusia berkata, "Kemana tempat lari?"'), # 75:10
    88: ('bagaimana / dari mana', 'Bagaimana', None), # 3:40
    89: ('di mana saja / sesukamu', 'di mana saja', 'Dan Kami berfirman: "Wahai Adam! Tinggallah engkau dan istrimu di dalam surga, dan makanlah dengan nikmat dari buah-buahannya di mana saja kamu sukai, dan janganlah kamu dekati pohon ini, nanti kamu termasuk orang-orang yang zalim!"'), # 2:35
    90: ('sekarang', 'Sekarang', None), # 2:71
    91: ('dari sisi-Mu', 'dari sisi-Mu', None), # 3:38
    92: ('mudah-mudahan / semoga', 'mudah-mudahan', None), # 17:79
    93: ('tiada lagi / tidak ada', 'tidak ada', 'Berapa banyak umat sebelum mereka yang telah Kami binasakan, lalu mereka meminta tolong padahal tidak ada lagi waktu untuk lari.'), # 38:3
    94: ('belum', 'belum', 'Sekali-kali jangan! Dia belum melaksanakan apa yang Dia perintahkan kepadanya.'), # 80:23
    95: ('bersepi-sepi / kembali', 'bersepi-sepi', 'Dan apabila mereka berjumpa dengan orang-orang yang beriman, mereka berkata, "Kami telah beriman." Tetapi apabila mereka bersepi-sepi sesama mereka, mereka berkata, "Apakah kamu menceritakan kepada mereka apa yang telah diterangkan Allah kepadamu?"'), # 2:76
    96: ('melampaui batas', 'melampaui batas', None), # 2:190
    97: ('Maha Suci Allah', 'Mahasempurna Allah', None), # 12:31
    98: ('berapa banyak', 'Berapa banyak', 'Berapa banyak nabi yang berperang didampingi sejumlah besar pengikut yang bertakwa. Mereka tidak menjadi lemah karena bencana yang menimpa mereka di jalan Allah.'), # 3:146
    99: ('demikianlah', 'Demikianlah', None), # 2:73
    100: ('di sanalah / di situlah', 'Di sanalah', None), # 3:38
    101: ('di sana', 'di sana', None), # 76:20
    102: ('sebaik-baik', 'sebaik-baik', None), # 38:30
    103: ('sangat buruk', 'Sangatlah buruk', None), # 2:90
    104: ('celakalah', 'Celakalah', None), # 83:1
    105: ('apabila / ketika', 'Apabila', None), # 110:1
    106: ('bagaimana', 'Bagaimana', None), # 2:28
    107: ('adapun', 'Adapun', None), # 18:79
    108: ('baik ... maupun', 'ada yang', None), # 76:3
    109: ('mengapa tidak', 'Mengapa', None), # 68:28
    110: ('jika demikian / maka', 'tentu', None), # 17:75
    111: ('apa / apakah', 'Apa', None), # 2:26
    112: ('apa pun', 'apa pun', None), # 7:132
    113: ('di mana saja / di mana pun', 'di mana saja', None), # 2:144
    114: ('di mana pun', 'Dimanapun', None), # 4:78
    115: ('bahwa', 'bahwa', None), # 21:108
    116: ('hanya / hanyalah', 'hanyalah', None), # 9:60
    117: ('setiap kali', 'Setiap kali', None), # 2:20
    118: ('agar / supaya', 'agar', None), # 20:40
    119: ('sia-sia / begitu saja', 'begitu saja', None), # 75:36
    120: ('supaya jangan', 'agar kamu tidak', None), # 3:153
    121: ('agar tidak', 'agar tidak', None), # 2:150
    122: ('bahwa kita tidak / agar tidak', 'bahwa kita tidak', None), # 3:64
    123: ('apakah tidak', 'Tidakkah kalian', None), # 2:44
    124: ('tidakkah / apakah tidak', 'tidakkah mereka', None), # 12:109
    125: ('tidakkah mereka tahu', 'Tidakkah mereka tahu', None), # 2:77
    126: ('tidakkah mereka tahu', 'Tidakkah mereka tahu', None), # 2:77
    127: ('bawalah kemari', 'Bawalah', None), # 6:150
    128: ('tunjukkanlah / bawalah', 'Tunjukkan', None), # 2:111
    129: ('ambillah / kemarilah', 'Ambillah', None), # 69:19
    130: ('jauh / mustahil', 'Jauh', None), # 23:36
    131: ('ah / cis', 'ah', None), # 17:23
    132: ('ya / benar', 'Ya', None), # 10:53
    133: ('apakah / sesuatu apa', 'Apakah', 'Katakanlah (Muhammad), "Apakah sesuatu yang paling besar kesaksiannya?" Katakanlah, "Allah, Dia menjadi saksi antara aku dan kamu."'), # 6:19
    134: ('wahai', 'Wahai', None), # 2:21
    135: ('wahai (fem.)', 'Wahai', None), # 89:27
    136: ('di dekat / di hadapan', 'di depan', None), # 12:25
    137: ('di belakang / di hadapan', 'di hadapan', None), # 18:79
    138: ('ke depan / di hadapannya', 'di hadapannya', 'Bahkan manusia ingin berbuat maksiat terus menerus di hadapannya (di masa mendatang).'), # 75:5
    139: ('di belakang / setelah', 'di belakang', None), # 2:255
    140: ('di atas', 'di atas', None), # 2:63
    141: ('di bawah', 'di bawah', None), # 2:25
    142: ('sebelah kanan', 'kanan', None), # 7:17
    143: ('sebelah kiri', 'kiri', None), # 18:17
    144: ('ke arah / ke jurusan', 'ke arah', None), # 7:47
    145: ('di sekeliling / di sekitar', 'sekeliling', None), # 2:17
    146: ('sebelum', 'sebelum', None), # 2:4
    147: ('setelah / sesudah', 'setelah', None), # 2:27
    148: ('tentang apa', 'Tentang apakah', None), # 78:1
    149: ('dalam hal apa', 'untuk apa', None), # 79:43
    150: ('dengan apa', 'apa yang', None), # 27:35
    151: ('dari apa', 'dari apa', None), # 86:5
    152: ('mengapa', 'Mengapa', None), # 3:65
    153: ('berapa lama', 'Berapa lama', None), # 2:259
    154: ('dua orang yang', 'dua orang yang', None), # 4:16
    155: ('kedua orang yang', 'kedua orang yang', 'Dan orang-orang yang kafir berkata, "Ya Tuhan kami, perlihatkanlah kepada kami kedua orang yang telah menyesatkan kami di antara jin dan manusia agar kami meletakkan keduanya di bawah telapak kaki kami."'), # 41:29
    156: ('wanita-wanita yang', 'yang', 'Dan para perempuan tua yang telah berhenti (dari haid dan mengandung) yang tidak ingin menikah lagi, maka tidak ada dosa bagi mereka menanggalkan pakaian luar mereka.'), # 24:60
    157: ('wanita-wanita yang', 'istri-istrimu yang', None), # 33:4
    158: ('dua mukjizat ini', 'dua mukjizat', None), # 28:32
    159: ('mereka ini', 'mereka', None), # 20:84
    160: ('di sini', 'di sini', 'Mereka berkata, "Wahai Musa! Sampai kapan pun kami tidak akan memasukinya selama mereka ada di dalamnya. Karena itu pergilah engkau bersama Tuhanmu dan berperanglah kamu berdua, sesungguhnya kami hanya duduk menanti di sini saja."'), # 5:24
    161: ('kamu berdua', 'kamu berdua', None), # 28:35
    162: ('engkau (fem.)', 'engkau', None), # 12:29
    163: ('Hanya kepada Engkau', 'Hanya kepada Engkaulah', None), # 1:5
    164: ('hanya kepada Dia / hanya kepada-Nya', 'hanya kepada-Nya', 'Dan Tuhanmu telah memerintahkan agar kamu jangan menyembah kecuali hanya kepada-Nya dan hendaklah berbuat baik kepada ibu bapak.'), # 17:23
    165: ('hanya kepada-Ku', 'kepada-Ku saja', None), # 2:40
    166: ('kami', 'kami', None), # 28:63
    167: ('kamu sekalian', 'kamu', None), # 34:24
    168: ('mereka', 'mereka', None), # 6:151
    169: ('mengapa tidak', 'Mengapa', None), # 15:7
    170: ('sudah pasti / tentu', 'Pasti', None), # 11:22
    171: ('mulailah keduanya', 'mulai', None), # 7:22
    172: ('hampir / nyaris', 'Hampir saja', None), # 2:20
    173: ('mana saja', 'yang mana saja', None), # 17:110
    174: ('sekiranya mereka tidak / jika tidak', 'sekiranya mereka tidak', None), # 18:6
    175: ('kecuali jika', 'kecuali jika', 'Talak (yang dapat dirujuk) itu dua kali. (Setelah itu suami dapat) menahan dengan baik, atau melepaskan dengan baik. Tidak halal bagi kamu mengambil kembali sesuatu yang telah kamu berikan kepada mereka, kecuali jika keduanya (suami dan istri) khawatir tidak mampu menjalankan hukum-hukum Allah.'), # 2:229
    176: ('sungguh', 'sungguh', None), # 2:143
    177: ('selama-lamanya / pernah', 'selama-lamanya', 'Dan mereka sekali-kali tidak akan menginginkan kematian itu selama-lamanya, karena kesalahan yang telah diperbuat oleh tangan mereka sendiri.'), # 2:95
    178: ('kemarin', 'kemarin', None), # 10:24
    179: ('besok / esok hari', 'esok', None), # 18:23
    180: ('pagi hari', 'pagi hari', None), # 37:177
    181: ('sore hari / waktu sore', 'sore', None), # 79:46
    182: ('waktu dhuha / pagi', 'pagi hari', None), # 20:59
    183: ('sebelum fajar / akhir malam', 'sebelum fajar', None), # 54:34
    184: ('malam hari', 'malam', None), # 17:1
    185: ('waktu / suatu saat', 'waktu', None), # 76:1
    186: ('bertahap / bertingkat', 'beberapa fase', None), # 71:14
    187: ('bersama-sama', 'bersama-sama', None), # 4:71
    188: ('sama saja', 'sama saja', None), # 2:6
    189: ('bukan / selain', 'bukan', None), # 1:7
    190: ('selain', 'selain', None), # 2:23
    191: ('tempat yang terbuka / adil', 'terbuka', None), # 20:58
    192: ('seperti / sebagaimana', 'sebagaimana yang', None), # 2:137
    193: ('kedua-duanya', 'keduanya', None), # 17:23
}

wb_id = openpyxl.load_workbook('db/qw_id.xlsx')
sheet_id = wb_id['Harf']

print('Applying curated Indonesian mappings to qw_id.xlsx...')
fails = []

for r_data in rows:
    r_num = r_data['row']
    rank = r_data['rank']
    sense = r_data['sense']
    vk = r_data['vk']
    cit = r_data['cit']
    
    id_m, id_t, custom_v = ID_MAP[r_num]
    v_txt = custom_v if custom_v else id_dict[vk]
    
    # Check if id_t in v_txt
    m = re.search(r'\b' + re.escape(id_t) + r'\b', v_txt, re.I)
    if not m:
        # try without word boundary
        idx = v_txt.find(id_t)
        if idx == -1:
            fails.append((r_num, rank, r_data['lemma'], id_t, cit, v_txt[:60]))
            continue
        start, end = idx, idx + len(id_t)
    else:
        start, end = m.start(), m.end()
        
    v_bracketed = v_txt[:start] + f'([{v_txt[start:end]}])' + v_txt[end:]
    actual_t = v_txt[start:end]
    
    # Write canonical data
    sheet_id.cell(row=r_num, column=1).value = r_data['rank']
    sheet_id.cell(row=r_num, column=2).value = r_data['lemma']
    sheet_id.cell(row=r_num, column=3).value = r_data['translit']
    sheet_id.cell(row=r_num, column=4).value = r_data['root']
    sheet_id.cell(row=r_num, column=5).value = r_data['pos']
    sheet_id.cell(row=r_num, column=6).value = r_data['occ']
    sheet_id.cell(row=r_num, column=7).value = r_data['cov']
    sheet_id.cell(row=r_num, column=8).value = r_data['sense']
    sheet_id.cell(row=r_num, column=9).value = id_m
    sheet_id.cell(row=r_num, column=10).value = cit
    sheet_id.cell(row=r_num, column=11).value = r_data['ar_t']
    sheet_id.cell(row=r_num, column=12).value = r_data['full_ar']
    sheet_id.cell(row=r_num, column=13).value = actual_t
    sheet_id.cell(row=r_num, column=14).value = v_bracketed

if sheet_id.max_row > 193:
    sheet_id.delete_rows(194, sheet_id.max_row - 193)

wb_id.save('db/qw_id.xlsx')
print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('  FAIL:', f)
print('Saved db/qw_id.xlsx successfully.')
