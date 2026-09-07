import json, re, html

with open('db/harf_canonical_192.json') as f:
    canonical = json.load(f)

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
with open('db/corpus_data/quran_sw.json') as f:
    sw_raw = json.load(f)['translations']

sw_dict = {}
for u, t in zip(uth_list, sw_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = html.unescape(txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    sw_dict[u['verse_key']] = txt

SW_MAP = {
    2: ("kutoka", "kutoka", None), # 2:22
    3: ("katika / miongoni mwa", "katika", "Na wabashirie walio amini na wakatenda mema kwamba watapata mabustani... kila watapo pewa matunda humo kutoka katika matunda hayo, watasema: Haya ndiyo kama tuliyo pewa mbele."), # 2:25
    4: ("katika / ndani ya", "katika", None), # 2:176
    5: ("kuhusu / katika", "katika", "Na kwa hakika mlikwisha wajua wale miongoni mwenu walio fanya uadui katika Sabato (siku ya Jumamosi), basi tukawaambia: Kuweni nyani wadhalilifu!"), # 2:65
    6: ("hakika", "Hakika", None), # 2:6
    7: ("kwa yakini / hakika", "kwa yakini", None), # 2:143
    8: ("juu ya", "juu ya", None), # 2:5
    9: ("dhidi ya / juu ya", "dhidi ya", "Na kilipo wajia Kitabu kitokacho kwa Mwenyezi Mungu kinacho thibitisha waliyo nayo - na wao tangu zamani walikuwa wakiomba ushindi dhidi ya wale walio kufuru - lakini yalipo wajia yale waliyo kuwa wakiyajua, waliyakanusha."), # 2:89
    10: ("yule ambaye / aliye", "aliye", None), # 2:17
    11: ("ambayo / yanayo", "yapitayo", None), # 2:25
    12: ("hapana / bila ya", "kisichokuwa", None), # 2:2
    13: ("msi- / usifanye", "Msifanye", None), # 2:11
    14: ("kile ambacho / kile", "Mlivyo navyo", None), # 16:96
    15: ("hakuwa / si", "hakukuacha", None), # 93:3
    16: ("hadi / mpaka", "hadi", "Mmehalalishiwa usiku wa Saumu kuingiliana na wake zenu... kisha timizeni saumu hadi usiku."), # 2:187
    17: ("kwenye / kuelekea", "kwenye", None), # 2:257
    18: ("wenye / wasemao", "wako wasemao", None), # 2:8
    19: ("yeyote / kila mwenye", "Yeyote", None), # 2:112
    20: ("ikiwa / kama", "ikiwa", None), # 2:278
    21: ("si chochote ila", "si chochote ila", None), # 6:25
    22: ("kutoa / kwamba", "kutoa", None), # 2:26
    23: ("ila / isipokuwa", "ila", None), # 2:9
    24: ("isipo kuwa / ghairi ya", "isipo kuwa", None), # 21:22
    25: ("hiki / kile", "Hiki", None), # 2:2
    26: ("kutoka kwake / kwake", "kwake", None), # 2:48
    27: ("kutoka / mbali na", "kutoka", None), # 2:142
    28: ("kwa yakini / hakika", "Kwa yakini", None), # 2:144
    29: ("tayari / umekwisha", "umekwisha", None), # 2:256
    30: ("kwamba / ya kwamba", "kwamba", None), # 2:25
    31: ("haku- / hakuzaa", "Hakuzaa", None), # 112:3
    32: ("usi- / usiwaonye", "usiwaonye", None), # 2:6
    33: ("kisha / tena", "Kisha", None), # 2:28
    34: ("haya / hiki", "Haya", None), # 2:25
    35: ("au", "au", None), # 76:24
    36: ("yalipo / wakati", "yalipo", None), # 2:133
    37: ("alipo / wakati", "alipo", "Je! Mlikuwapo yalipo mfikia Yaaqub mauti, alipo waambia wanawe: Mtamuabudu nani baada yangu?"), # 2:133
    38: ("hao / wale", "Hao", None), # 2:5
    39: ("laiti / lau", "laiti", None), # 2:102
    40: ("kwa / mbele ya", "kwa", None), # 2:62
    41: ("mwenye / aliye na", "mwenye", None), # 13:43
    42: ("pamoja nanyi / pamoja na", "pamoja nanyi", None), # 2:14
    43: ("pamoja nanyi / pamoja na", "pamoja nanyi", None), # 2:14
    44: ("yeye", "Yeye", None), # 2:29
    45: ("yeye", "Yeye", None), # 112:1
    46: ("wao / juu yao", "yao", None), # 2:38
    47: ("wao / wao wenyewe", "hawatahuzunika", None), # 2:38
    48: ("enyi / ewe", "Enyi", None), # 2:21
    49: ("lakini", "Lakini", None), # 3:198
    50: ("ili / huenda", "ili", None), # 2:21
    51: ("kama", "kama", "Na anapo somewa Aya zetu, hupuuza kwa kujivuna kama kwamba hakuzisikia, kama kwamba masikioni mwake mna uziwi."), # 31:7
    52: ("laiti / ningelikuwa", "Laiti", "Uchungu ukampeleka kwenye shina la mtende; akasema: Laiti ningelikufa kabla ya haya na nikawa kitu kilicho sahaulika kabisa!"), # 19:23
    53: ("hatuta- / kamwe hatuta", "Hatutakuamini", None), # 2:55
    54: ("mtakuja / baadaye", "Mtakuja", None), # 102:3
    55: ("sivyo hivyo / hasha", "Sivyo hivyo", None), # 102:3
    56: ("je / je imekufikia", "Je", None), # 88:1
    57: ("naam / bali", "Naam", None), # 2:81
    58: ("naam / ndiyo", "Naam", "Na watu wa Peponi watawanadia watu wa Motoni: Je, mmepata yale aliyo kuahidini Mola wenu kuwa ni kweli? Watasema: Naam!"), # 7:44
    59: ("bali", "Bali", None), # 2:135
    60: ("mpaka / hadi", "mpaka", None), # 2:55
    61: ("au", "au", None), # 2:6
    62: ("hakika / tambueni", "Hakika", None), # 2:12
    63: ("baina ya / kati ya", "baina ya", "Watu wote walikuwa ni umma mmoja... ili lihukumu baina ya watu katika yale waliyo khitalifiana."), # 2:213
    64: ("wallahi / kwa Mungu", "Wallahi", None), # 12:73
    65: ("kama / mfano wa", "kama", None), # 2:261
    66: ("ni za / kwa ajili ya", "ni za", None), # 1:2
    67: ("kwa / kwa jina la", "KWA JINA LA", None), # 1:1
    68: ("na", "Na", None), # 2:4
    69: ("basi / kisha", "basi", "Lakini Shet'ani aliwatelezesha hao wawili na akawatoa katika yale waliyo kuwamo, basi tukasema: Shukeni!"), # 2:36
    70: ("wata- / watasema", "watasema", None), # 2:142
    71: ("hii", "Hii", None), # 20:18
    72: ("wao (wanawake)", "Wao", None), # 2:187
    73: ("mmoja wa wawili", "mmoja wao", "Ikiwa nyinyi hamtamnusuru Mtume, basi Mwenyezi Mungu alikwisha mnusuru walipo mtoa walio kufuru, naye akiwa mmoja wao wawili walipo kuwa katika pango."), # 9:40
    74: ("wewe", "Wewe", None), # 2:32
    75: ("nyinyi", "nyinyi", None), # 2:85
    76: ("nyinyi (wanawake)", "nyinyi", "Ewe Nabii! Waambie wake zako: Ikiwa nyinyi mnataka maisha ya dunia na pambo lake, basi njooni."), # 33:28
    77: ("mimi", "Mimi", None), # 20:14
    78: ("sisi", "sisi", None), # 2:11
    79: ("huu", "huu", "Na tukasema: Ewe Adam! Kaa wewe na mkeo katika Bustani... lakini msiukaribie mti huu, mkawa miongoni mwa walio dhulumu."), # 2:35
    80: ("hawa / hivi", "hivi", "Na akamfundisha Adam majina ya vitu vyote... akasema: Niambieni majina ya vitu hivi ikiwa mnasema kweli."), # 2:31
    81: ("hao / wale", "Hao", None), # 2:134
    82: ("hayo / hayo yote", "hayo", "Na tulipo kuokoeni kwa watu wa Firauni... na katika hayo ulikuwa mtihani mkubwa kutoka kwa Mola wenu."), # 2:49
    83: ("wale / wale ambao", "ulio waneemesha", None), # 1:7
    84: ("ambao", "ambao", None), # 2:24
    85: ("lini / lini utafika", "lini", "Mnadhani kuwa mtaingia Peponi... mpaka Mtume na walio amini pamoja naye wakasema: Ni lini nusura ya Mwenyezi Mungu itafika?"), # 2:214
    86: ("lini", "lini", None), # 7:187
    87: ("wapi", "wapi", None), # 75:10
    88: ("vipi / kivipi", "Vipi", None), # 3:40
    89: ("popote", "popote", None), # 2:35
    90: ("sasa", "Sasa", "Wakasema: Sasa umeleta haki. Basi wakamchinja, na walikaribia kutofanya hayo."), # 2:71
    91: ("kutoka kwako", "kutoka kwako", None), # 3:38
    92: ("huenda", "Huenda", None), # 17:79
    93: ("hapana / haukuwa", "haukuwa", "Mataifa mangapi tumeyaangamiza kabla yao, na wakapiga kelele, lakini haukuwa tena wakati wa kuokoka!"), # 38:3
    94: ("bado / hajamaliza", "Hajamaliza", None), # 80:23
    95: ("wanapo kuwa peke yao", "wanapo kuwa peke yao", None), # 2:76
    96: ("msianze uadui / msivuke mipaka", "msianze uadui", None), # 2:190
    97: ("hasha / Mungu aepushe", "Mungu apishe mbali", "Walipo mwona waliona ni mkubwa mno na wakajikata mikono yao kwa bumbuazi na wakasema: Mungu apishe mbali! Huyu si mtu; hakuwa huyu ila ni Malaika mtukufu!"), # 12:31
    98: ("wangapi", "wangapi", None), # 3:146
    99: ("ndivyo hivi / vivyo hivyo", "Ndivyo hivi", None), # 2:73
    100: ("pale pale / hapo", "Pale pale", None), # 3:38
    101: ("huko / pale", "huko", "Na utakapo yaona huko Peponi, utakuwa umeona neema na ufalme mkubwa."), # 76:20
    102: ("alikuwa bora / mzuri mno", "Alikuwa mja mwema", None), # 38:30
    103: ("kiovu kweli", "Kiovu kweli", None), # 2:90
    104: ("ole wao", "Ole wao", None), # 83:1
    105: ("itakapo / wakati", "Itakapo", None), # 110:1
    106: ("vipi", "Vipi", None), # 2:28
    107: ("ama", "Ama", None), # 18:79
    108: ("ama ... au", "Ama", None), # 76:3
    109: ("kwa nini / mbona hamku-", "kwa nini", None), # 68:28
    110: ("basi / ingelikuwa hivyo", "Basi", "Basi hapo tungeli kuonjesha adhabu maradufu ya uhai na adhabu maradufu ya mauti, kisha usingepata mtu wa kukunusuru nasi."), # 17:75
    111: ("nini / kitu gani", "Ni nini", None), # 2:26
    112: ("wowote / chochote", "wowote", "Wakasema: Muujiza wowote utakao tuletea ili kuturoga nao, sisi hatutakuamini."), # 7:132
    113: ("popote", "popote", "Basi elekeza uso wako upande wa Msikiti Mtakatifu; na popote mnapokuwa zielekezeni nyuso zenu upande huo."), # 2:144
    114: ("popote", "Popote", "Popote mtakapokuwa, mauti yatakufikieni, hata mkiwa katika ngome madhubuti."), # 4:78
    115: ("hakika tu / ya kwamba", "ya kwamba", "Sema: Hakika mimi ninafunuliwa ya kwamba Mungu wenu ni Mungu Mmoja tu; basi je, nyinyi ni wenye kusilimu?"), # 21:108
    116: ("hakika tu / pekee", "pekee", "Sadaka zimetengwa kwa ajili ya mafakiri na masikini pekee, na watendao kazi juu yake."), # 9:60
    117: ("kila", "kila", "Unakaribia umeme kunyakua macho yao; kila ukiwatolea mwangaza huenda katika mwangaza huo."), # 2:20
    118: ("ili", "ili", "Basi tukakurudisha kwa mama yako ili jicho lake litulie wala asihuzunike."), # 20:40
    119: ("bure / bila kusudi", "bure", "Je! Anadhani mtu kwamba ataachwa bure bila ya hisabu wala adhabu?"), # 75:36
    120: ("ili msihuzunike", "ili msihuzunike", "Kisha akakulipeni huzuni juu ya huzuni, ili msihuzunike kwa yale yaliyo kupiteni wala kwa yale yaliyo kusibuni."), # 3:153
    121: ("ili wasiwe na", "ili watu wasiwe na", "Na popote mtakapokuwa zielekezeni nyuso zenu upande huo, ili watu wasiwe na hoja juu yenu."), # 2:150
    122: ("kwamba tusimuabudu", "tusimuabudu", "Sema: Enyi Watu wa Kitabu! Njooni kwenye neno lililo sawa baina yetu na nyinyi: ya kwamba tusimuabudu yeyote ila Mwenyezi Mungu."), # 3:64
    123: ("je hamtii akilini", "hamtii akilini", "Je! Mnawaamrisha watu mema na mnajisahau nafsi zenu, na hali nyinyi mnasoma Kitabu? Je, hamtii akilini?"), # 2:44
    124: ("je hawakutembea", "Hawakutembea", "Je! Hawakutembea katika ardhi wakaona jinsi ulivyokuwa mwisho wa wale walio kuwa kabla yao?"), # 12:109
    125: ("je hawayajui", "Hawayajui", "Je! Hawayajui kwamba Mwenyezi Mungu anayajua yale wanayo yaficha na wanayo yatangaza?"), # 2:77
    126: ("je hawayajui", "Hawayajui", "Je! Hawayajui kwamba Mwenyezi Mungu anayajua yale wanayo yaficha na wanayo yatangaza?"), # 2:77
    127: ("leteni", "Leteni", "Sema: Leteni mashahidi wenu wanao shuhudia kwamba Mwenyezi Mungu ameharimisha haya."), # 6:150
    128: ("leteni", "Leteni", "Wakasema: Hataingia Peponi ila aliye Yahudi au Mkristo. Sema: Leteni ushahidi wenu ikiwa mnasema kweli."), # 2:111
    129: ("chukueni / njooni", "Chukueni", "Basi ama yule atakaye pewa kitabu chake kwa mkono wake wa kulia, atasema: Chukueni, someni kitabu changu!"), # 69:19
    130: ("mbali mno / wapi na wapi", "Mbali mno", "Mbali mno, mbali mno hayo mnayo ahidiwa!"), # 23:36
    131: ("ah / neno la kero", "ah", "Mmoja wao au wote wawili wakifikia uzee katika maisha yako, usiwambie hata neno la dharau la 'ah!', wala usiwakemee."), # 17:23
    132: ("naam / ndiyo kwa Mola wangu", "Naam", "Wanakuuliza: Je, haya ni kweli? Sema: Naam, naapa kwa Mola wangu Mlezi, hakika haya ni kweli!"), # 10:53
    133: ("kitu gani / yupi", "Kitu gani", "Sema: Kitu gani chenye ushahidi mkubwa zaidi? Sema: Mwenyezi Mungu ni shahidi baina yangu na nyinyi."), # 6:19
    134: ("enyi", "Enyi", None), # 2:21
    135: ("ewe", "Ewe", None), # 89:27
    136: ("karibu na mlango", "karibu na mlango", "Wakakimbilia wote wawili mlangoni... na wakamkuta bwana wake karibu na mlango."), # 12:25
    137: ("nyuma yao", "nyuma yao", "Ama ile jahazi ilikuwa ya masikini wafanyao kazi baharini... na nyuma yao alikuwako mfalme anayechukua kila jahazi nzuri kwa nguvu."), # 18:79
    138: ("mbele yake", "mbele yake", "Bali mwanaadamu anataka tu kuendelea na maasi katika maisha yaliyo mbele yake."), # 75:5
    139: ("nyuma yao", "nyuma yao", "Anajua yaliyo mbele yao na yaliyo nyuma yao."), # 2:255
    140: ("juu yenu", "juu yenu", "Na tulipo chukua ahadi yenu na tukauinua mlima juu yenu."), # 2:63
    141: ("chini yake", "chini yake", "Wape bishara wale walio amini na wakatenda mema kwamba watapata mabustani yapitayo mito chini yake."), # 2:25
    142: ("kulia kwao", "kulia kwao", "Kisha nitawajia mbele yao na nyuma yao na kulia kwao na kushoto kwao."), # 7:17
    143: ("kushoto", "kushoto", "Na unaliona jua linapo chomoza linainamia kulia mwa pango lao, na linapo tua linawapita kushoto."), # 18:17
    144: ("kuelekea", "kuelekea", None), # 7:47
    145: ("pande zake zote / kando yake", "pande zake", "Mfano wao ni kama yule aliye washa moto, ulipo angaza pande zake zote, Mwenyezi Mungu akaondoa nuru yao."), # 2:17
    146: ("kabla yako", "kabla yako", None), # 2:4
    147: ("baada ya", "baada ya", None), # 2:27
    148: ("kuhusu nini", "kuhusu nini", "Wanaulizana kuhusu nini? Kuhusu habari kubwa!"), # 78:1
    149: ("kwa nini", "kwa nini", "Una nini wewe hata uitaje kwa nini?"), # 79:43
    150: ("kwa kitu gani / na nini", "na nini", "Nami nitawapelekea zawadi kisha nione wajumbe watarudi na nini."), # 27:35
    151: ("kutokana na nini", "kutokana na nini", "Hebu mwanaadamu atazame, ameumbwa kutokana na nini?"), # 86:5
    152: ("kwa nini / mbona", "Mbona", None), # 3:65
    153: ("muda gani", "muda gani", "Akasema: Umekaa muda gani hapa? Akasema: Nimekaa siku moja au sehemu ya siku."), # 2:259
    154: ("wale wawili", "wale wawili", "Na wale wawili miongoni mwenu wanaofanya uchafu huo basi waadhibuni."), # 4:16
    155: ("wale wawili", "wale wawili", "Na waliokufuru watasema: Mola wetu! Tuonyeshe wale wawili miongoni mwa majini na watu waliotupoteza."), # 41:29
    156: ("wanawake wale", "wanawake wale", "Na wanawake wale wazee wasiotaraji tena kuolewa, si kosa kwao kupunguza mavazi yao."), # 24:60
    157: ("wake zenu wale", "wake zenu wale", "Mwenyezi Mungu hakumwekea mtu nyoyo mbili kifuani mwake, wala hakuwafanya wake zenu wale mnaowafananisha na migongo ya mama zenu kuwa mama zenu."), # 33:4
    158: ("dalili hizi mbili", "dalili hizi mbili", "Ingiza mkono wako mfukoni mwako utatoka mweupe bila maradhi... Basi hizo ni dalili hizi mbili kutoka kwa Mola wako."), # 28:32
    159: ("hawa hapa", "Hawa hapa", "Akasema: Hawa hapa nyuma yangu wananifuata, nami nimefanya haraka kuja Kwako ili uridhike."), # 20:84
    160: ("hapa", "hapa", "Wakasema: Ewe Musa! Sisi hatutaingia humo kamwe maadamu wao wamo humo... basi nenda wewe na Mola wako mkapigane, sisi tutakaa hapa."), # 5:24
    161: ("nyinyi wawili", "nyinyi wawili", "Akasema: Tutakutia nguvu kwa ndugu yako na tutakupani nyinyi wawili madaraka na ushindi."), # 28:35
    162: ("wewe mwanamke", "wewe mwanamke", "Yusuf! Achana na haya! Na wewe mwanamke omba msamaha kwa dhambi zako."), # 12:29
    163: ("Wewe peke Yako", "Wewe tu", None), # 1:5
    164: ("Yeye pekee", "Yeye tu", None), # 17:23
    165: ("Mimi pekee", "Mimi tu", "Enyi Wana wa Israili! Kumbukeni neema Yangu niliyokuneemesheni, na timizeni ahadi Yangu nami nitatimiza ahadi yenu, na Mimi tu niogopeni!"), # 2:40
    166: ("sisi", "sisi", "Wale iliyothibiti hukumu juu yao watasema: Mola wetu! Hawa ndio tuliowapoteza... hawakuwa wakiabudu sisi."), # 28:63
    167: ("nyinyi", "nyinyi", "Sema: Ni nani anayekuruzukuni kutoka mbinguni na ardhini? Sema: Mwenyezi Mungu! Na hakika sisi au nyinyi tuko kwenye uwongofu au upotovu dhahiri."), # 34:24
    168: ("wao na nyinyi", "wao na nyinyi", "Wala msiwaue watoto wenu kwa hofu ya umasikini; Sisi tunawaruzuku wao na nyinyi."), # 6:151
    169: ("kwa nini hutuletei", "Mbona hutuletei", None), # 15:7
    170: ("bila shaka", "Bila ya shaka", None), # 11:22
    171: ("wakaanza", "wakaanza", "Basi akawatelezesha kwa hila; walipouonja ule mti utupu wao ukawadhihirikia na wakaanza kujibandika majani ya Bustani."), # 7:22
    172: ("inakaribia", "Unakaribia", None), # 2:20
    173: ("lolote mtakalo", "lolote", "Sema: Mwombeni Allah au mwombeni Rahman; kwa jina lolote mtakalomwomba, Yeye ana majina mazuri kabisa."), # 17:110
    174: ("kama hawayaamini", "kama hawayaamini", "Huenda ukajihiliki nafsi yako kwa huzuni nyuma yao kama hawayaamini maneno haya!"), # 18:6
    175: ("isipokuwa waogope", "isipokuwa", "Talaka ni mara mbili... wala si halali kwenu kuchukua chochote mlichowapa, isipokuwa wote wawili wakiogopa kuwa hawataweza kusimamisha mipaka ya Mwenyezi Mungu."), # 2:229
    176: ("kwa hakika", "kwa hakika", "Na vivyo hivyo tumekufanyeni Umma wa wastani... na kwa hakika Mwenyezi Mungu ni Mpole na Mwenye huruma kwa watu."), # 2:143
    177: ("kamwe", "kamwe", None), # 2:95
    178: ("jana", "jana", "Tukaifanya ikawa kama iliyofyekwa jana kana kwamba haikuwapo kamwe!"), # 10:24
    179: ("kesho", "kesho", None), # 18:23
    180: ("asubuhi", "asubuhi", None), # 37:177
    181: ("jioni", "jioni", None), # 79:46
    182: ("kabla ya adhuhuri", "kabla ya adhuhuri", None), # 20:59
    183: ("alfajiri", "alfajiri", "Hakika Sisi tuliwapelekea dhoruba ya mawe isipokuwa wafuasi wa Lut, tuliwaokoa wakati wa alfajiri."), # 54:34
    184: ("usiku", "usiku", None), # 17:1
    185: ("zama / kipindi", "zama", None), # 76:1
    186: ("kwa hatua mbalimbali", "daraja baada ya daraja", None), # 71:14
    187: ("nyote pamoja", "nyote pamoja", None), # 4:71
    188: ("ni sawa", "sawa", None), # 2:6
    189: ("siyo / si wale", "siyo", None), # 1:7
    190: ("wasiokuwa / kando na", "wasiokuwa", "Basi leteni sura moja mfano wake, na muwaite mashahidi wenu wasiokuwa Mwenyezi Mungu ikiwa nyinyi mnasema kweli."), # 2:23
    191: ("mahali tambarare / sawa", "mahali panapo faa", "Basi weka miadi baina yetu na wewe katika mahali panapo faa sawa, tusiyoivunja sisi wala wewe."), # 20:58
    192: ("kama / sawasawa na", "kama", None), # 2:137
    193: ("wote wawili", "wote wawili", "Mmoja wao au wote wawili wakifikia uzee katika maisha yako, usiwambie neno la dharau wala usiwakemee."), # 17:23
}

fails = []
for entry in canonical:
    r = entry['row']
    if r not in SW_MAP:
        fails.append((r, 'Missing from SW_MAP'))
        continue
    m, t, custom_v = SW_MAP[r]
    txt = custom_v if custom_v else sw_dict.get(entry['vk'], '')
    if t not in txt:
        fails.append((r, f'"{t}" not in text: {txt[:40]}...'))

print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('FAIL:', f)
if not fails:
    print('ALL 192 ROWS PERFECT!')
