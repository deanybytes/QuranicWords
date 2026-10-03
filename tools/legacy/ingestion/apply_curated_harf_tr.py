import json, re, openpyxl, html

with open('db/harf_canonical_192.json') as f:
    rows = json.load(f)

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
with open('db/corpus_data/quran_tr.json') as f:
    tr_raw = json.load(f)['translations']

tr_dict = {}
for u, t in zip(uth_list, tr_raw):
    txt = re.sub(r'<sup[^>]*>.*?</sup>', '', t.get('text', ''))
    txt = re.sub(r'<[^>]+>', '', txt)
    txt = html.unescape(txt)
    txt = re.sub(r'\s+', ' ', txt).strip()
    tr_dict[u['verse_key']] = txt

# Complete Turkish curated mappings: (meaning, target_word, optional_custom_verse_text)
TR_MAP = {
    2: ('-den / -dan', 'Gökten', None), # 2:22
    3: ('-den / rızık olarak', 'rızık olarak', None), # 2:25
    4: ('hakkında / içinde', 'hakkında', None), # 2:176
    5: ('hakkında / konusunda', 'hakkında', 'İçinizden cumartesi günü hakkında haddi aşanları elbette biliyorsunuz. Onlara "Aşağılık birer maymun olunuz" dedik.'), # 2:65
    6: ('şüphesiz / muhakkak', 'Şüphe yok ki', None), # 2:6
    7: ('doğrusu / şüphesiz', 'Doğrusu', None), # 2:143
    8: ('üzerinde / yolunda', 'yolunda olanlar', 'İşte Rab\'lerinin doğru yolunda olanlar ve kurtuluşa erenler bunlardır.'), # 2:5
    9: ('karşı / aleyhine', 'karşı', None), # 2:89
    10: ('o kimse ki / kimse', 'kimseye', None), # 2:17
    11: ('ki o / olduğunu', 'olduğunu', None), # 2:25
    12: ('yoktur / şüphe götürmeyen', 'şüphe götürmeyen', None), # 2:2
    13: ('yapmayın / etmeyin', 'yapmayın', None), # 2:11
    14: ('olanlar / şeyler', 'olanlar', None), # 16:96
    15: ('bırakmadı / terk etmedi', 'terk etmedi', 'Rabbin seni terk etmedi ve sana darılmadı da.'), # 93:3
    16: ('-e kadar', 'kadar', None), # 2:187
    17: ('-e / karanlıklara', 'karanlıklara', None), # 2:257
    18: ('kimseler / diyenler', 'diyenler', None), # 2:8
    19: ('kimse / her kim', 'kimsenin', None), # 2:112
    20: ('eğer / şayet', 'eğer', 'Ey inananlar! Allah\'tan sakının; eğer gerçekten inanıyorsanız, faizden arta kalanı bırakın.'), # 2:278
    21: ('başka bir şey değildir / değil', 'başka bir şey değildir', None), # 6:25
    22: ('-mesi / ki', 'ki', 'Şüphesiz Allah, bir sivrisineği ve ondan daha büyüğünü misal getirmekten çekinmez.'), # 2:26
    23: ('ancak / başkasını değil', 'ancak', 'Onlar Allah\'ı ve inananları aldatmaya çalışırlar; oysa ancak kendilerini aldatırlar da farkında olmazlar.'), # 2:9
    24: ('başka / gayrı', 'başka', None), # 21:22
    25: ('işte bu / o', 'Bu', None), # 2:2
    26: ('-den / kimseden', 'kimseden', 'Ve öyle bir günden sakının ki, o gün hiç kimse başkası adına bir şey ödeyemez, kimseden şefaat kabul edilmez, fidye alınmaz ve onlara yardım da edilmez.'), # 2:48
    27: ('-den / çeviren', 'çeviren', 'İnsanlardan bazı beyinsizler: "Onları daha önce yöneldikleri kıbleden çeviren nedir?" diyecekler. De ki: "Doğu da Batı da Allah\'ındır."'), # 2:142
    28: ('muhakkak / doğrusu', 'muhakkak', 'Biz senin yüzünün göğe doğru çevrilip durduğunu muhakkak görüyoruz; şimdi seni hoşnut olacağın bir kıbleye yöneltiyoruz.'), # 2:144
    29: ('muhakkak / artık', 'artık', 'Dinde zorlama yoktur; artık hak ile batıl apaçık ayrılmıştır.'), # 2:256
    30: ('olduğunu / ki', 'olduğunu', None), # 2:25
    31: ('doğurmamıştır / doğmadı', 'doğurmamıştır', 'O, doğurmamıştır ve doğurulmamıştır.'), # 112:3
    32: ('uyarsan da uyarmasan da', 'uyarmasan da', None), # 2:6
    33: ('sonra', 'sonra', 'Siz cansız iken size can veren Allah\'ı nasıl inkar edersiniz? Sonra sizi öldürecek, sonra tekrar diriltecektir.'), # 2:28
    34: ('bu', 'Bu', None), # 2:25
    35: ('veya / yahut', 'veya', 'O halde Rabbinin hükmüne sabret; onlardan hiçbir günahkara veya nanköre itaat etme.'), # 76:24
    36: ('vaktiyle / hani', 'Hani', 'Yoksa siz, Yakub\'a ölüm geldiği zaman orada hazır mıydınız? Hani o oğullarına: "Benden sonra kime kulluk edeceksiniz?" demişti.'), # 2:133
    37: ('o vakit / -ken', 'verirken', None), # 2:133
    38: ('işte onlar', 'İşte', None), # 2:5
    39: ('eğer / keşke', 'keşke', 'Şayet bilselerdi, uğruna canlarını sattıkları şey ne kötüydü! Keşke bilselerdi.'), # 2:102
    40: ('katında / yanında', 'katındadır', 'Şüphesiz iman edenler, Yahudiler, Hıristiyanlar ve Sabiilerden her kim Allah\'a ve ahiret gününe inanıp yararlı iş yaparsa, onların mükafatı Rableri katındadır.'), # 2:62
    41: ('yanında / nezdinde', 'yanında', 'İnkar edenler: "Sen peygamber değilsin" derler; de ki: "Benimle sizin aranızda şahit olarak Allah ve yanında Kitap ilmi bulunanlar yeter."'), # 13:43
    42: ('beraber / birlikte', 'beraberiz', 'İnananlarla karşılaştıklarında "İnandık" derler; şeytanlarıyla baş başa kaldıklarında ise: "Biz şüphesiz sizinle beraberiz, biz sadece alay etmekteyiz" derler.'), # 2:14
    43: ('beraber / birlikte', 'sizinleyiz', None), # 2:14
    44: ('O / O\'dur', 'O', 'O, yerde ne varsa hepsini sizin için yaratandır.'), # 2:29
    45: ('O', 'O', 'De ki: "O Allah birdir."'), # 112:1
    46: ('onlar', 'onlar', None), # 2:38
    47: ('onlar', 'onlar', None), # 2:38
    48: ('ey', 'Ey', 'Ey insanlar! Sizi ve sizden öncekileri yaratan Rabbinize kulluk edin.'), # 2:21
    49: ('fakat / lakin', 'Fakat', 'Fakat Rablerinden sakınanlar için altlarından ırmaklar akan cennetler vardır.'), # 3:198
    50: ('umulur ki / ta ki', 'umulur ki', 'Ey insanlar! Rabbinize kulluk edin ki, umulur ki sakınasınız.'), # 2:21
    51: ('sanki / güya', 'sanki', 'Ona ayetlerimiz okunduğu zaman, sanki kulaklarında bir ağırlık varmış gibi büyüklük taslayarak yüz çevirir.'), # 31:7
    52: ('keşke', 'Keşke', 'Doğum sancısı onu bir hurma dalına getirdiğinde: "Keşke bundan önce ölseydim de unutulup gitseydim!" dedi.'), # 19:23
    53: ('asla / kesinlikle ... -meyiz', 'asla', 'Hani siz: "Ey Musa! Biz Allah\'ı açıkça görmedikçe asla inanmayız" demiştiniz.'), # 2:55
    54: ('yakında / sonra', 'Yakında', 'Hayır! Yakında bileceksiniz.'), # 102:3
    55: ('hayır / asla', 'Hayır', 'Hayır! Yakında bileceksiniz.'), # 102:3
    56: ('geldi mi / -di mi', 'geldi mi', 'Dehşeti her şeyi kaplayan o kıyametin haberi sana geldi mi?'), # 88:1
    57: ('evet / bilakis', 'Bilakis', 'Bilakis! Kim bir kötülük kazanır da günahı kendisini kuşatırsa, işte onlar cehennemliktirler.'), # 2:81
    58: ('evet', 'Evet', 'Cennet halkı cehennem halkına: "Rabbimizin bize vadettiğini hak bulduk, siz de buldunuz mu?" diye seslenirler. Onlar da: "Evet" derler.'), # 7:44
    59: ('bilakis / hayır', 'Bilakis', 'Dediler ki: "Yahudi veya Hıristiyan olun ki hidayete eresiniz." De ki: "Bilakis, biz Hanif olan İbrahim\'in dinine uyarız."'), # 2:135
    60: ('-e kadar', 'kadar', 'Hani siz: "Ey Musa! Allah\'ı açıkça görünceye kadar sana inanmayız" demiştiniz.'), # 2:55
    61: ('yoksa / veya', 'uyarmasan da', None), # 2:6
    62: ('iyi bilin ki / dikkat edin', 'İyi bilin ki', 'İyi bilin ki, onlar bozguncuların ta kendileridir; fakat farkında değillerdir.'), # 2:12
    63: ('arasında', 'aralarında', 'İnsanlar tek bir ümmetti. Allah müjdeciler ve uyarıcılar olarak peygamberler gönderdi ve aralarında anlaşmazlığa düştükleri hususlarda hüküm vermek için hak Kitabı indirdi.'), # 2:213
    64: ('Allah\'a andolsun ki', 'Allah\'a andolsun', 'Dediler ki: "Allah\'a andolsun ki, bizim yeryüzünde fesat çıkarmak için gelmediğimizi siz de bilirsiniz."'), # 12:73
    65: ('gibi', 'gibidir', 'Mallarını Allah yolunda harcayanların durumu, yedi başak bitiren ve her başakta yüz tane bulunan bir tohum gibidir.'), # 2:261
    66: ('içindir / Allah\'a mahsustur', 'Allah\'a', 'Hamd, alemlerin Rabbi olan Allah\'a mahsustur.'), # 1:2
    67: ('ile / adına', 'adıyla', 'Rahman ve Rahim olan Allah\'ın adıyla.'), # 1:1
    68: ('ve', 've', 'Onlar sana indirilene ve senden önce indirilenlere inanırlar, ahirete de kesinlikle inanırlar.'), # 2:4
    69: ('bunun üzerine / sonra', 'Bunun üzerine', 'Bunun üzerine şeytan onları oradan kaydırdı ve içinde bulundukları nimetten çıkardı.'), # 2:36
    70: ('-ecek / yakında', 'diyecekler', None), # 2:142
    71: ('o / budur', 'Bu', 'Musa: "Bu benim asamdır; ona dayanırım..." dedi.'), # 20:18
    72: ('onlar (dişil)', 'onlar', None), # 2:187
    73: ('ikisi', 'ikisi', 'Siz ona yardım etmezseniz, Allah ona yardım etmiştir; hani kafirler onu çıkardıklarında ikisi mağaradaydı da arkadaşına: "Üzülme, Allah bizimle beraberdir" diyordu.'), # 9:40
    74: ('Sen', 'Sen', 'Dediler ki: "Seni tenzih ederiz, Senin bize öğrettiğinden başka ilmimiz yoktur. Şüphesiz Sen, her şeyi bilensin, hikmet sahibisin."'), # 2:32
    75: ('siz', 'siz', None), # 2:85
    76: ('sizler (dişil)', 'siz', 'Ey Peygamber! Eşlerine söyle: "Eğer dünya hayatını ve süsünü istiyorsanız, gelin size bağışta bulunayım ve sizi güzellikle salıvereyim."'), # 33:28
    77: ('Ben', 'Ben', 'Şüphesiz Ben Allah\'ım; Benden başka ilah yoktur, o halde Bana kulluk et.'), # 20:14
    78: ('Biz', 'Biz', 'Onlara: "Yeryüzünde bozgunculuk yapmayın" denildiğinde, "Biz sadece düzelticileriz" derler.'), # 2:11
    79: ('bu (dişil)', 'şu', 'Ve dedik ki: "Ey Adem! Sen ve eşin cennete yerleşin, dilediğiniz yerden bol bol yiyin; ancak şu ağaca yaklaşmayın, yoksa zalimlerden olursunuz."'), # 2:35
    80: ('bunlar / şunlar', 'bunların', None), # 2:31
    81: ('o / onlar', 'Onlar', 'Onlar bir ümmetti, gelip geçti. Onların kazandığı kendilerine, sizin kazandığınız da sizedir.'), # 2:134
    82: ('işte bu / bunda', 'bunda', 'Hani sizi Firavun ailesinden kurtarmıştık... Bunda Rabbinizden büyük bir imtihan vardı.'), # 2:49
    83: ('o kimseler ki / olanlar', 'kendilerine', 'Kendilerine lütufta bulunduğun kimselerin yoluna ilet.'), # 1:7
    84: ('ki o (dişil)', 'o', 'Eğer bunu yapamazsanız -ki asla yapamayacaksınız- o halde yakıtı insanlar ve taşlar olan o ateşten sakının.'), # 2:24
    85: ('ne zaman', 'Ne zaman', 'Yoksa siz, sizden öncekilerin başına gelenlerin benzeri başınıza gelmeden cennete gireceğinizi mi sandınız?... Nihayet Peygamber ve beraberindeki müminler: "Allah\'ın yardımı ne zaman?" dediler.'), # 2:214
    86: ('ne zaman', 'ne zaman', 'Sana kıyametin ne zaman demir atacağını sorarlar.'), # 7:187
    87: ('nereye', 'Nereye', 'O gün insan: "Nereye kaçmalı?" der.'), # 75:10
    88: ('nasıl / nereden', 'Nasıl', 'Zekeriya: "Rabbim! Bana ihtiyarlık ulaştığı ve karım da kısırken benim nasıl bir oğlum olabilir?" dedi.'), # 3:40
    89: ('dilediğiniz yerden / nereden isterseniz', 'dilediğiniz yerden', 'Dedik ki: "Ey Adem! Sen ve eşin cennette oturun, dilediğiniz yerden bol bol yiyin."'), # 2:35
    90: ('şimdi', 'Şimdi', 'Dediler ki: "Şimdi gerçeği getirdin."'), # 2:71
    91: ('katından / Senin katından', 'katından', 'Orada Zekeriya Rabbine dua edip dedi ki: "Rabbim! Bana katından temiz bir soy bağışla."'), # 3:38
    92: ('umulur ki / olabilir ki', 'umulur ki', 'Gecenin bir kısmında uyanıp sana mahsus bir nafile olarak namaz kıl; umulur ki Rabbin seni övgüye değer bir makama ulaştırır.'), # 17:79
    93: ('artık yok / değildi', 'artık', None), # 38:3
    94: ('henüz / asla', 'henüz', 'Hayır! O, Allah\'ın kendisine emrettiğini henüz tam yerine getirmemiştir.'), # 80:23
    95: ('baş başa kaldıklarında', 'baş başa kaldıklarında', 'İnananlarla karşılaştıklarında "İnandık" derler; baş başa kaldıklarında ise: "Allah\'ın size açıkladığını onlara anlatıyor musunuz?" derler.'), # 2:76
    96: ('haddi aşmayın / aşırı gitmeyin', 'haddi aşmayın', 'Sizinle savaşanlarla Allah yolunda savaşın; fakat haddi aşmayın.'), # 2:190
    97: ('haşa / Allah\'ı tenzih ederiz', 'Haşa', 'Kadınlar: "Haşa! Allah için, biz onda hiçbir kötülük görmedik" dediler.'), # 12:31
    98: ('nice', 'Nice', 'Nice peygamberler vardı ki, kendileriyle birlikte birçok rabbani kimseler savaştı.'), # 3:146
    99: ('böylece / işte böyle', 'Böylece', 'İşte böylece Allah ölüleri diriltir ve size ayetlerini gösterir.'), # 2:73
    100: ('orada / o vakit', 'Orada', 'Orada Zekeriya Rabbine dua etti.'), # 3:38
    101: ('orada', 'orada', 'Nereye baksan, orada bir nimet ve büyük bir mülk görürsün.'), # 76:20
    102: ('ne güzel', 'ne güzel', 'O ne güzel kuldu! Çünkü o daima Allah\'a yönelirdi.'), # 38:30
    103: ('ne kötü / pek fena', 'ne kötü', None), # 2:90
    104: ('yazıklar olsun / vay haline', 'Vay haline', 'Ölçüde ve tartıda hile yapanların vay haline!'), # 83:1
    105: ('geldiği zaman / zaman', 'geldiği zaman', 'Allah\'ın yardımı ve fetih geldiği zaman...'), # 110:1
    106: ('nasıl', 'Nasıl', 'Siz cansız iken size can veren Allah\'ı nasıl inkar edersiniz?'), # 2:28
    107: ('ise / gelince', 'Gemiye gelince', '"Gemiye gelince, o denizde çalışan birkaç yoksula aitti; onu kusurlu kılmak istedim, çünkü peşlerinde her sağlam gemiye zorla el koyan bir hükümdar vardı."'), # 18:79
    108: ('ister ... ister', 'ister', 'Biz ona yolu gösterdik; ister şükredici olsun, ister nankör.'), # 76:3
    109: ('tesbih etseniz ya / değil mi', 'dememiş miydim', 'Aralarından en aklı başında olanı: "Ben size tesbih etseniz ya dememiş miydim?" dedi.'), # 68:28
    110: ('o takdirde / elbette', 'o takdirde', 'O takdirde sana hayatın da ölümün de kat kat azabını tattırırdık.'), # 17:75
    111: ('ne / neyi', 'ne', 'İnkar edenler ise: "Allah bu misalle neyi murat etti?" derler.'), # 2:26
    112: ('her ne / ne getirirsen', 'Her ne', 'Dediler ki: "Bizi büyülemek için her ne mucize getirirsen getir, sana inanacak değiliz."'), # 7:132
    113: ('nerede olursanız / her nerede', 'nerede olursanız', 'Nerede olursanız yüzünüzü o yöne çevirin.'), # 2:144
    114: ('nerede olursanız olun', 'Nerede olursanız olun', 'Nerede olursanız olun, ölüm size ulaşır.'), # 4:78
    115: ('ki ancak / ancak', 'ancak', 'De ki: "Bana ancak sizin ilahınızın bir tek ilah olduğu vahyolunuyor."'), # 21:108
    116: ('ancak / sadece', 'ancak', 'Sadakalar (zekatlar) ancak fakirler, düşkünler... içindir.'), # 9:60
    117: ('her defasında / -dıkça', 'aydınlattıkça', None), # 2:20
    118: ('diye / için / ta ki', 'diye', None), # 20:40
    119: ('başıboş', 'başıboş', 'İnsan, kendisinin başıboş bırakılacağını mı sanır?'), # 75:36
    120: ('üzülmeyesiniz diye / olmasın diye', 'üzülmeyesiniz diye', None), # 3:153
    121: ('-mesin diye / olmasın diye', 'olmasın diye', 'İnsanların aleyhinizde bir delili olmasın diye yüzünüzü Mescid-i Haram\'a çevirin.'), # 2:150
    122: ('kulluk etmeyelim / koşmayalım', 'kulluk etmeyelim', 'De ki: "Ey Kitap ehli! Bizimle sizin aranızda ortak olan bir söze gelin: Allah\'tan başkasına kulluk etmeyelim ve O\'na hiçbir şeyi ortak koşmayalım."'), # 3:64
    123: ('akıl etmez misiniz / düşünmüyor musunuz', 'akıl erdirmiyor musunuz', 'İnsanlara iyiliği emreder de kendinizi unutur musunuz? Oysa siz Kitab\'ı okuyorsunuz; hala akıl erdirmiyor musunuz?'), # 2:44
    124: ('gezip dolaşmadılar mı', 'dolaşmadılar mı', 'Senden önce de şehirler halkından kendilerine vahyettiğimiz adamlardan başkasını göndermedik. Yeryüzünde dolaşmadılar mı ki kendilerinden öncekilerin sonunun nasıl olduğuna baksınlar?'), # 12:109
    125: ('bilmezler mi', 'Bilmezler mi', 'Bilmezler mi ki, Allah onların gizlediklerini de açığa vurduklarını da bilir?'), # 2:77
    126: ('bilmezler mi', 'Bilmezler mi', 'Bilmezler mi ki, Allah onların gizlediklerini de açığa vurduklarını da bilir?'), # 2:77
    127: ('getirin / çağırın', 'Getirin', 'De ki: "Allah\'ın bunu haram kıldığına şahitlik edecek şahitlerinizi getirin."'), # 6:150
    128: ('getirin', 'Getirin', 'De ki: "Eğer doğru sözlü iseniz delilinizi getirin."'), # 2:111
    129: ('alın / işte', 'Alın', 'Kitabı sağından verilen: "Alın, kitabımı okuyun!" der.'), # 69:19
    130: ('ne kadar uzak / heyhat', 'Heyhat', 'Heyhat, o size vadedilen şey ne kadar uzak!'), # 23:36
    131: ('öf / bıktım', 'öf', 'Onlardan biri veya her ikisi senin yanında ihtiyarlarsa, kendilerine "öf!" bile deme.'), # 17:23
    132: ('evet / andolsun', 'Evet', 'Sana sorarlar: "O gerçek midir?" De ki: "Evet, Rabbime andolsun ki o şüphesiz gerçektir."'), # 10:53
    133: ('hangi / ne', 'Hangi', 'De ki: "Hangi şey şahitlik bakımından daha büyüktür?" De ki: "Allah benimle sizin aranızda şahittir."'), # 6:19
    134: ('ey', 'Ey', 'Ey insanlar! Rabbinize kulluk edin.'), # 2:21
    135: ('ey', 'Ey', 'Ey huzura ermiş nefis!'), # 89:27
    136: ('yanında / kapının yanında', 'kapının yanında', 'İkisi kapıya doğru koştular... Kapının yanında kadının efendisine rastladılar.'), # 12:25
    137: ('ardında / önünde', 'ardında', 'Gemiye gelince; o, denizde çalışan yoksullarındı... Çünkü ardında her sağlam gemiyi zorla alan bir hükümdar vardı.'), # 18:79
    138: ('önündekini / geleceğini', 'önündekini', 'Fakat insan önündekini (kıyameti veya günahı) yalanlamak ister.'), # 75:5
    139: ('ardında / arkasında', 'arkalarında', 'O, onların önlerindekini ve arkalarındakini bilir.'), # 2:255
    140: ('üzerine / üstünüze', 'üstünüze', 'Hani sizden söz almış, Tur dağını üstünüze kaldırmıştık.'), # 2:63
    141: ('altından', 'altlarından', 'Onlar için altlarından ırmaklar akan cennetler vardır.'), # 2:25
    142: ('sağından / sağlarından', 'sağlarından', 'Sonra onlara önlerinden, arkalarından, sağlarından ve sollarından sokulacağım.'), # 7:17
    143: ('solundan / sol tarafa', 'sol', 'Güneş doğduğunda mağaranın sağ tarafına yöneldiğini, battığında ise sol taraftan onları makaslayıp geçtiğini görürsün.'), # 18:17
    144: ('tarafına / doğru', 'tarafına', 'Gözleri cehennem halkı tarafına çevrildiği zaman: "Rabbimiz! Bizi zalimler topluluğu ile beraber kılma" derler.'), # 7:47
    145: ('çevresini / etrafını', 'çevresini', 'Ateş çevresini aydınlattığı zaman, Allah onların nurlarını giderdi.'), # 2:17
    146: ('önce', 'önce', 'Onlar sana indirilene ve senden önce indirilene inanırlar.'), # 2:4
    147: ('sonra', 'sonra', 'Onlar ki, söz verip bağlandıktan sonra Allah\'ın ahdini bozarlar.'), # 2:27
    148: ('neyi / ne hakkında', 'Neyi', 'Neyi soruşturup duruyorlar?'), # 78:1
    149: ('nerede / sana ne', 'nerede', 'Sen nerede, onun vaktini bildirmek nerede!'), # 79:43
    150: ('ne ile / neyle', 'ne ile', 'Ben onlara bir hediye göndereyim de, elçilerin ne ile döneceklerine bakayım.'), # 27:35
    151: ('neden / neden yaratıldığına', 'neden', 'İnsan bir baksın, neden yaratılmıştır?'), # 86:5
    152: ('niçin / neden', 'Niçin', 'De ki: "Ey Kitap ehli! İbrahim hakkında niçin tartışıyorsunuz?"'), # 3:65
    153: ('ne kadar / ne kadar kaldın', 'Ne kadar', 'Allah: "Ne kadar kaldın?" dedi. O: "Bir gün yahut günün bir kısmı kadar kaldım" dedi.'), # 2:259
    154: ('içinizden o iki kişi / iki kişi', 'o iki kişi', 'İçinizden fuhuş yapan o iki kişiye eziyet edin; tevbe ederlerse bırakın.'), # 4:16
    155: ('o iki / bizi saptıran iki', 'o iki', 'Kafirler derler ki: "Rabbimiz! Cinlerden ve insanlardan bizi saptıran o iki zümreyi bize göster de ayaklarımızın altına alalım."'), # 41:29
    156: ('kadınlar / yaşlı kadınlar', 'kadınlara', None), # 24:60
    157: ('eşleriniz / zıhar yaptığınız eşleriniz', 'eşlerinizi', 'Allah bir insanın göğsünde iki kalp yaratmamıştır; zıhar yaptığınız eşlerinizi de anneleriniz kılmamıştır.'), # 33:4
    158: ('bu ikisi / iki delil', 'bu ikisi', None), # 28:32
    159: ('işte onlar / arkamdan gelenler', 'Onlar', 'Musa: "Onlar arkamdan beni takip ediyorlar; Rabbim, benden razı olasın diye acele ettim" dedi.'), # 20:84
    160: ('burada', 'burada', 'Dediler ki: "Ey Musa! Onlar orada oldukça biz oraya asla girmeyiz; sen ve Rabbin gidin savaşın, biz burada oturacağız."'), # 5:24
    161: ('ikiniz / siz ikiniz', 'siz ikiniz', 'Allah buyurdu: "Kardeşinle senin pazunu güçlendireceğiz ve size bir kudret vereceğiz; ayetlerimiz sayesinde size erişemeyecekler, siz ikiniz ve size uyanlar galip geleceksiniz."'), # 28:35
    162: ('sen (dişil)', 'sen', 'Ey Yusuf! Bundan vazgeç; kadın, sen de günahının bağışlanmasını dile!'), # 12:29
    163: ('ancak Sana / yalnız Sana', 'Ancak Sana', None), # 1:5
    164: ('yalnız Kendisine / ancak O\'na', 'yalnız Kendisine', None), # 17:23
    165: ('yalnız Benden / yalnız Bana', 'yalnız Benden', 'Ey İsrailoğulları! Size verdiğim nimeti hatırlayın, ahdime vefa gösterin ki Ben de ahdinize vefa göstereyim ve yalnız Benden korkun.'), # 2:40
    166: ('bize', 'bize', 'Azap sözü aleyhlerine gerçekleşenler derler ki: "Rabbimiz! Bunlar saptırdığımız kimselerdir; onlar bize tapmıyorlardı."'), # 28:63
    167: ('siz', 'siz', 'De ki: "Göklerden ve yerden size kim rızık veriyor?" De ki: "Allah! Öyleyse ya biz yahut siz doğru yol üzerindeyiz."'), # 34:24
    168: ('onları', 'onları', 'De ki: "Gelin, Rabbinizin size neleri haram kıldığını okuyayım... Sizi de onları da Biz rızıklandırırız."'), # 6:151
    169: ('neden ... getirmedin', 'getirmeliydin', 'Eğer doğru sözlülerden isen, bize melekleri getirmeliydin ya!'), # 15:7
    170: ('şüphesiz / muhakkak', 'Şüphesiz', 'Şüphesiz onlar ahirette en çok ziyana uğrayacak olanlardır.'), # 11:22
    171: ('başladılar', 'koyuldular', 'Ağacın meyvesinden tattıklarında ayıp yerleri kendilerine göründü ve cennet yapraklarıyla örtünmeye koyuldular.'), # 7:22
    172: ('neredeyse / az kalsın', 'neredeyse', 'Şimşek neredeyse gözlerini alıverecek.'), # 2:20
    173: ('hangisini', 'hangisini', 'De ki: "İster Allah deyin, ister Rahman deyin; hangisini çağırırsanız en güzel isimler O\'nundur."'), # 17:110
    174: ('şayet inanmazlarsa / eğer', 'inanmazlarsa', 'Bu söze (Kuran\'a) inanmazlarsa, arkalarından üzülerek kendini tüketeceksin neredeyse.'), # 18:6
    175: ('meğer ki / korkmaları müstesna', 'korkmaları müstesna', 'Boşama iki defadır... Allah\'ın koyduğu sınırları koruyamamaktan korkmaları müstesna, onlara verdiklerinizden bir şey almanız helal olmaz.'), # 2:229
    176: ('doğrusu / şüphesiz / muhakkak', 'Doğrusu', None), # 2:143
    177: ('asla / ebediyen', 'asla', 'Yaptıkları yüzünden ölümü asla istemezler.'), # 2:95
    178: ('dün / dünmüş gibi', 'dün', 'Sanki dün hiç yokmuş gibi onu biçilmiş bir ekin haline getirdik.'), # 10:24
    179: ('yarın', 'yarın', 'Hiçbir şey hakkında: "Ben bunu yarın kesinlikle yapacağım" deme.'), # 18:23
    180: ('sabah / sabah vakti', 'sabah', 'Azap onların sahasına indiği zaman, uyarılanların sabahı ne kötü olur!'), # 37:177
    181: ('akşam / bir akşam', 'bir akşam', 'Onu gördükleri gün, sanki dünyada sadece bir akşam veya bir kuşluk vakti kadar kalmış gibi olurlar.'), # 79:46
    182: ('kuşluk vakti', 'kuşluk vakti', 'Musa: "Buluşma vaktimiz bayram günü ve insanların toplanacağı kuşluk vaktidir" dedi.'), # 20:59
    183: ('seher vakti', 'seher vakti', 'Lut ailesi müstesna, seher vakti onları kurtardık.'), # 54:34
    184: ('geceleyin / bir gece', 'bir gece', 'Kendisine ayetlerimizden bir kısmını gösterelim diye kulunu bir gece Mescid-i Haram\'dan Mescid-i Aksa\'ya götüren Allah yücedir.'), # 17:1
    185: ('bir süre / zaman', 'bir zaman', 'İnsanın üzerinden, henüz anılır bir şey değilken uzun bir zaman geçmedi mi?'), # 76:1
    186: ('aşama aşama / evrelerden geçirerek', 'evrelerden', 'Oysa O sizi evrelerden geçirerek yaratmıştır.'), # 71:14
    187: ('hep birlikte / topluca', 'topluca', 'Ey inananlar! Tedbirinizi alın; bölük bölük veya topluca savaşa çıkın.'), # 4:71
    188: ('birdir / eşittir', 'birdir', 'İnkar edenleri uyarsan da uyarmasan da onlar için birdir; inanmazlar.'), # 2:6
    189: ('olmayan / değil', 'değil', None), # 1:7
    190: ('başka / Allah\'tan başka', 'Allah\'tan başka', 'Eğer doğru sözlü iseniz, Allah\'tan başka şahitlerinizi çağırın.'), # 2:23
    191: ('düz / uygun bir yer', 'uygun bir yer', 'Aramızda, ne bizim ne senin caymayacağımız düz ve uygun bir yerde buluşma vakti belirle.'), # 20:58
    192: ('gibi / benzeri', 'gibi', 'Eğer onlar da sizin inandığınız gibi inanırlarsa doğru yolu bulmuş olurlar.'), # 2:137
    193: ('ikisi de / her ikisi', 'her ikisi', 'Onlardan biri veya her ikisi senin yanında ihtiyarlarsa onlara "öf!" bile deme.'), # 17:23
}

wb_tr = openpyxl.load_workbook('db/qw_tr.xlsx')
sheet_tr = wb_tr['Harf']

print('Applying curated Turkish mappings to qw_tr.xlsx...')
fails = []

for r_data in rows:
    r_num = r_data['row']
    rank = r_data['rank']
    sense = r_data['sense']
    vk = r_data['vk']
    cit = r_data['cit']
    
    tr_m, tr_t, custom_v = TR_MAP.get(r_num, (r_data['en_m'], 'TODO', None))
    v_txt = custom_v if custom_v else tr_dict.get(vk, '')
    
    # Check if tr_t in v_txt
    m = re.search(r'\b' + re.escape(tr_t) + r'\b', v_txt, re.I)
    if not m:
        idx = v_txt.find(tr_t)
        if idx == -1:
            fails.append((r_num, rank, r_data['lemma'], tr_t, cit, v_txt[:60]))
            continue
        start, end = idx, idx + len(tr_t)
    else:
        start, end = m.start(), m.end()
        
    v_bracketed = v_txt[:start] + f'([{v_txt[start:end]}])' + v_txt[end:]
    actual_t = v_txt[start:end]
    
    # Write canonical data
    sheet_tr.cell(row=r_num, column=1).value = r_data['rank']
    sheet_tr.cell(row=r_num, column=2).value = r_data['lemma']
    sheet_tr.cell(row=r_num, column=3).value = r_data['translit']
    sheet_tr.cell(row=r_num, column=4).value = r_data['root']
    sheet_tr.cell(row=r_num, column=5).value = r_data['pos']
    sheet_tr.cell(row=r_num, column=6).value = r_data['occ']
    sheet_tr.cell(row=r_num, column=7).value = r_data['cov']
    sheet_tr.cell(row=r_num, column=8).value = r_data['sense']
    sheet_tr.cell(row=r_num, column=9).value = tr_m
    sheet_tr.cell(row=r_num, column=10).value = cit
    sheet_tr.cell(row=r_num, column=11).value = r_data['ar_t']
    sheet_tr.cell(row=r_num, column=12).value = r_data['full_ar']
    sheet_tr.cell(row=r_num, column=13).value = actual_t
    sheet_tr.cell(row=r_num, column=14).value = v_bracketed

if sheet_tr.max_row > 193:
    sheet_tr.delete_rows(194, sheet_tr.max_row - 193)

wb_tr.save('db/qw_tr.xlsx')
print(f'Total fails: {len(fails)}/192')
for f in fails:
    print('  FAIL:', f)
print('Saved db/qw_tr.xlsx successfully.')
