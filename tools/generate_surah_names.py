import json

surahs = [
    (1, "Al-Fatihah", "الفاتحة", "আল-ফাতিহা", "الفاتحۃ", "अल-फ़ातिहा", "Al-Fatihah", "Fâtiha", "Al-Fatiha", "فاتحه"),
    (2, "Al-Baqarah", "البقرة", "আল-বাকারা", "البقرۃ", "अल-बक़रह", "Al-Baqarah", "Bakara", "Al-Baqara", "بقره"),
    (3, "Ali 'Imran", "آل عمران", "আলে ইমরান", "آل عمران", "आले-इमरान", "Ali 'Imran", "Âl-i İmrân", "Al-Imran", "آل عمران"),
    (4, "An-Nisa", "النساء", "আন-নিসা", "النساء", "अन-निसा", "An-Nisa'", "Nisâ", "An-Nisa", "نساء"),
    (5, "Al-Ma'idah", "المائدة", "আল-মায়িদাহ", "المائدۃ", "अल-माइदा", "Al-Ma'idah", "Mâide", "Al-Ma'ida", "مائده"),
    (6, "Al-An'am", "الأنعام", "আল-আন'আম", "الانعام", "अल-अनआम", "Al-An'am", "En'âm", "Al-An'am", "انعام"),
    (7, "Al-A'raf", "الأعراف", "আল-আ'রাফ", "الاعراف", "अल-अराफ़", "Al-A'raf", "A'râf", "Al-A'raf", "اعراف"),
    (8, "Al-Anfal", "الأنفال", "আল-আনফাল", "الانفال", "अल-अनफ़ाल", "Al-Anfal", "Enfâl", "Al-Anfal", "انفال"),
    (9, "At-Tawbah", "التوبة", "আত-তাওবাহ", "التوبۃ", "अत-तौबा", "At-Taubah", "Tevbe", "At-Tawba", "توبه"),
    (10, "Yunus", "يونس", "ইউনুস", "یونس", "यूनुस", "Yunus", "Yûnus", "Yunus", "یونس"),
    (11, "Hud", "هود", "হূদ", "ہود", "हूद", "Hud", "Hûd", "Hud", "هود"),
    (12, "Yusuf", "يوسف", "ইউসুফ", "یوسف", "यूसुफ़", "Yusuf", "Yûsuf", "Yusuf", "یوسف"),
    (13, "Ar-Ra'd", "الرعد", "আর-রাদ", "الرعد", "अर-रअद", "Ar-Ra'd", "Ra'd", "Ar-Ra'd", "رعد"),
    (14, "Ibrahim", "ابراهيم", "ইবরাহীম", "ابراہیم", "इब्राहीम", "Ibrahim", "İbrâhîm", "Ibrahim", "ابراهیم"),
    (15, "Al-Hijr", "الحجر", "আল-হিজর", "الحجر", "अल-हिज्र", "Al-Hijr", "Hicr", "Al-Hijr", "حجر"),
    (16, "An-Nahl", "النحل", "আন-নাহল", "النحل", "अन-नहल", "An-Nahl", "Nahl", "An-Nahl", "نحل"),
    (17, "Al-Isra", "الإسراء", "আল-ইসরা", "الاسراء", "अल-इस्रा", "Al-Isra'", "İsrâ", "Al-Isra", "اسراء"),
    (18, "Al-Kahf", "الكهف", "আল-কাহফ", "الکہف", "अल-कहफ़", "Al-Kahf", "Kehf", "Al-Kahf", "کهف"),
    (19, "Maryam", "مريم", "মারইয়াম", "مریم", "मरयम", "Maryam", "Meryem", "Maryam", "مریم"),
    (20, "Taha", "طه", "ত্বা-হা", "طٰہٰ", "ता-हा", "Taha", "Tâhâ", "Ta-Ha", "طه"),
    (21, "Al-Anbya", "الأنبياء", "আল-আম্বিয়া", "الانبیاء", "अल-अम्बिया", "Al-Anbiya'", "Enbiyâ", "Al-Anbiya", "انبیاء"),
    (22, "Al-Hajj", "الحج", "আল-হাজ্জ", "الحج", "अल-हज", "Al-Hajj", "Hac", "Al-Hajj", "حج"),
    (23, "Al-Mu'minun", "المؤمنون", "আল-মু'মিনূন", "المؤمنون", "अल-मोमिनून", "Al-Mu'minun", "Mü'minûn", "Al-Mu'minun", "مؤمنون"),
    (24, "An-Nur", "النور", "আন-নূর", "النور", "अन-नूर", "An-Nur", "Nûr", "An-Nur", "نور"),
    (25, "Al-Furqan", "الفرقان", "আল-ফুরকান", "الفرقان", "अल-फ़ुरक़ान", "Al-Furqan", "Furkân", "Al-Furqan", "فرقان"),
    (26, "Ash-Shu'ara", "الشعراء", "আশ-শু'আরা", "الشعراء", "अश-शुअरा", "Asy-Syu'ara'", "Şuarâ", "Ach-Chu'ara", "شعراء"),
    (27, "An-Naml", "النمل", "আন-নামল", "النمل", "अन-नम्ल", "An-Naml", "Neml", "An-Naml", "نمل"),
    (28, "Al-Qasas", "القصص", "আল-কাসাস", "القصص", "अल-क़सस", "Al-Qasas", "Kasas", "Al-Qasas", "قصص"),
    (29, "Al-'Ankabut", "العنكبوت", "আল-আনকাবূত", "العنکبوت", "अल-अंकबूत", "Al-'Ankabut", "Ankebût", "Al-Ankabut", "عنکبوت"),
    (30, "Ar-Rum", "الروم", "আর-রূম", "الروم", "अर-रूम", "Ar-Rum", "Rûm", "Ar-Rum", "روم"),
    (31, "Luqman", "لقمان", "লুকমান", "لقمان", "लुक़्मान", "Luqman", "Lokmân", "Luqman", "لقمان"),
    (32, "As-Sajdah", "السجدة", "আস-সাজদাহ", "السجدۃ", "अस-सजदा", "As-Sajdah", "Secde", "As-Sajda", "سجده"),
    (33, "Al-Ahzab", "الأحزاب", "আল-আহযাব", "الاحزاب", "अल-अहज़ाब", "Al-Ahzab", "Ahzâb", "Al-Ahzab", "احزاب"),
    (34, "Saba", "سبإ", "সাবা", "سبا", "सबा", "Saba'", "Sebe'", "Saba", "سبأ"),
    (35, "Fatir", "فاطر", "ফাতির", "فاطر", "फ़ातिर", "Fatir", "Fâtır", "Fatir", "فاطر"),
    (36, "Ya-Sin", "يس", "ইয়াসীন", "یٰسین", "या-सीन", "Yasin", "Yâsîn", "Ya-Sin", "یاسین"),
    (37, "As-Saffat", "الصافات", "আস-সাফফাত", "الصافات", "अस-साफ़्फ़ात", "As-Saffat", "Sâffât", "As-Saffat", "صافات"),
    (38, "Sad", "ص", "সোয়াদ", "ص", "साद", "Sad", "Sâd", "Sad", "ص"),
    (39, "Az-Zumar", "الزمر", "আজ-জুমার", "الزمر", "अज-ज़ुमर", "Az-Zumar", "Zümer", "Az-Zumar", "زمر"),
    (40, "Ghafir", "غافر", "গাফির", "غافر", "ग़ाफ़िर", "Gafir", "Mü'min", "Ghafir", "غافر"),
    (41, "Fussilat", "فصلت", "ফুসসিলাত", "فصلت", "फ़ुस्स़िलत", "Fussilat", "Fussilet", "Fussilat", "فصلت"),
    (42, "Ash-Shura", "الشورى", "আশ-শূরা", "الشوریٰ", "अश-शूरा", "Asy-Syura", "Şûrâ", "Ach-Chura", "شوری"),
    (43, "Az-Zukhruf", "الزخرف", "আজ-যুখরুফ", "الزخرف", "अज़-ज़ुख़रुफ़", "Az-Zukhruf", "Zuhruf", "Az-Zukhruf", "زخرف"),
    (44, "Ad-Dukhan", "الدخان", "আদ-দুখান", "الدخان", "अद-दुख़ान", "Ad-Dukhan", "Duhân", "Ad-Dukhan", "دخان"),
    (45, "Al-Jathiyah", "الجاثية", "আল-জাসিয়াহ", "الجاثیہ", "अल-जासिया", "Al-Jasiyah", "Câsiye", "Al-Jathiya", "جاثیه"),
    (46, "Al-Ahqaf", "الأحقاف", "আল-আহকাফ", "الاحقاف", "अल-अहक़ाफ़", "Al-Ahqaf", "Ahkâf", "Al-Ahqaf", "احقاف"),
    (47, "Muhammad", "محمد", "মুহাম্মদ", "محمد", "मुहम्मद", "Muhammad", "Muhammed", "Muhammad", "محمد"),
    (48, "Al-Fath", "الفتح", "আল-ফাতহ", "الفتح", "अल-फ़तह", "Al-Fath", "Fetih", "Al-Fath", "فتح"),
    (49, "Al-Hujurat", "الحجرات", "আল-হুজুরাত", "الحجرات", "अल-हुजुरात", "Al-Hujurat", "Hucurât", "Al-Hujurat", "حجرات"),
    (50, "Qaf", "ق", "ক্বাফ", "ق", "क़ाफ़", "Qaf", "Kâf", "Qaf", "ق"),
    (51, "Adh-Dhariyat", "الذاريات", "আজ-যারিয়াত", "الذاریات", "अज़-ज़ारियात", "Az-Zariyat", "Zâriyât", "Adh-Dhariyat", "ذاریات"),
    (52, "At-Tur", "الطور", "আত-তূর", "الطور", "अत-तूर", "At-Tur", "Tûr", "At-Tur", "طور"),
    (53, "An-Najm", "النجم", "আন-নাজম", "النجم", "अन-नज्म", "An-Najm", "Necm", "An-Najm", "نجم"),
    (54, "Al-Qamar", "القمر", "আল-ক্বামার", "القمر", "अल-क़मर", "Al-Qamar", "Kamer", "Al-Qamar", "قمر"),
    (55, "Ar-Rahman", "الرحمن", "আর-রহমান", "الرحمن", "अर-रहमान", "Ar-Rahman", "Rahmân", "Ar-Rahman", "رحمن"),
    (56, "Al-Waqi'ah", "الواقعة", "আল-ওয়াকিয়াহ", "الواقعۃ", "अल-वाक़िया", "Al-Waqi'ah", "Vâkıa", "Al-Waqi'a", "واقعه"),
    (57, "Al-Hadid", "الحديد", "আল-হাদীদ", "الحدید", "अल-हदीद", "Al-Hadid", "Hadîd", "Al-Hadid", "حدید"),
    (58, "Al-Mujadila", "المجادلة", "আল-মুজাদালাহ", "المجادلہ", "अल-मुजादिला", "Al-Mujadilah", "Mücâdele", "Al-Mujadila", "مجادله"),
    (59, "Al-Hashr", "الحشر", "আল-হাশর", "الحشر", "अल-हश्र", "Al-Hasyr", "Haşr", "Al-Hashr", "حشر"),
    (60, "Al-Mumtahanah", "الممتحنة", "আল-মুমতাহিনাহ", "الممتحنہ", "अल-मुमतहिना", "Al-Mumtahanah", "Mümtehine", "Al-Mumtahana", "ممتحنه"),
    (61, "As-Saf", "الصف", "আস-সাফ", "الصف", "अस-सफ़", "As-Saff", "Saf", "As-Saff", "صف"),
    (62, "Al-Jumu'ah", "الجمعة", "আল-জুমুআহ", "الجمعہ", "अल-जुमुआ", "Al-Jumu'ah", "Cuma", "Al-Jumu'a", "جمعه"),
    (63, "Al-Munafiqun", "المنافقون", "আল-মুনাফিকুন", "المنافقون", "अल-मुनाफ़िक़ून", "Al-Munafiqun", "Münâfikûn", "Al-Munafiqun", "منافقون"),
    (64, "At-Taghabun", "التغابن", "আত-তাগাবুন", "التغابن", "अत-तग़ाबुन", "At-Tagabun", "Tegâbün", "At-Taghabun", "تغابن"),
    (65, "At-Talaq", "الطلاق", "আত-ত্বালাক", "الطلاق", "अत-तलाक़", "At-Talaq", "Talâk", "At-Talaq", "طلاق"),
    (66, "At-Tahrim", "التحريم", "আত-তাহরীম", "التحریم", "अत-तहरीम", "At-Tahrim", "Tahrîm", "At-Tahrim", "تحریم"),
    (67, "Al-Mulk", "الملك", "আল-মুলক", "الملک", "अल-मुल्क", "Al-Mulk", "Mülk", "Al-Mulk", "ملک"),
    (68, "Al-Qalam", "القلم", "আল-কলম", "القلم", "अल-क़लम", "Al-Qalam", "Kalem", "Al-Qalam", "قلم"),
    (69, "Al-Haqqah", "الحاقة", "আল-হাক্কাহ", "الحاقۃ", "अल-हाक़्क़ा", "Al-Haqqah", "Hâkka", "Al-Haqqa", "حاقه"),
    (70, "Al-Ma'arij", "المعارج", "আল-মাআরিজ", "المعارج", "अल-मआरिज", "Al-Ma'arij", "Meâric", "Al-Ma'arij", "معارج"),
    (71, "Nuh", "نوح", "নূহ", "نوح", "नूह", "Nuh", "Nûh", "Nuh", "نوح"),
    (72, "Al-Jinn", "الجن", "আল-জ্বিন", "الجن", "अल-जिन्न", "Al-Jinn", "Cin", "Al-Jinn", "جن"),
    (73, "Al-Muzzammil", "المزمل", "আল-মুযযাম্মিল", "المزمل", "अल-मुज़्ज़म्मिल", "Al-Muzzammil", "Müzzemmil", "Al-Muzzammil", "مزمل"),
    (74, "Al-Muddaththir", "المدثر", "আল-মুদ্দাসসির", "المدثر", "अल-मुद्दस्सिर", "Al-Muddassir", "Müddessir", "Al-Muddaththir", "مدثر"),
    (75, "Al-Qiyamah", "القيامة", "আল-কিয়ামাহ", "القیامہ", "अल-क़ियामा", "Al-Qiyamah", "Kıyâme", "Al-Qiyama", "قیامت"),
    (76, "Al-Insan", "الانسان", "আল-ইনসান", "الانسان", "अल-इंसान", "Al-Insan", "İnsân", "Al-Insan", "انسان"),
    (77, "Al-Mursalat", "المرسلات", "আল-মুরসালাত", "المرسلات", "अल-मुर्सलात", "Al-Mursalat", "Mürselât", "Al-Mursalat", "مرسلات"),
    (78, "An-Naba", "النبإ", "আন-নাবা", "النبا", "अन-नबा", "An-Naba'", "Nebe'", "An-Naba", "نبأ"),
    (79, "An-Nazi'at", "النازعات", "আন-নাযিয়াত", "النازعات", "अन-नाज़िआत", "An-Nazi'at", "Nâziât", "An-Nazi'at", "نازعات"),
    (80, "'Abasa", "عبس", "আবাসা", "عبس", "अबसा", "'Abasa", "Abese", "Abasa", "عبس"),
    (81, "At-Takwir", "التكوير", "আত-তাকভীর", "التکویر", "अत-तकवीर", "At-Takwir", "Tekvîr", "At-Takwir", "تکویر"),
    (82, "Al-Infitar", "الانفطار", "আল-ইনফিতার", "الانفطار", "अल-इन्फ़ितार", "Al-Infitar", "İnfitâr", "Al-Infitar", "انفطار"),
    (83, "Al-Mutaffifin", "المطففين", "আল-মুতাফফিফীন", "المطففین", "अल-मुतफ़्फ़िफ़ीन", "Al-Mutaffifin", "Mutaffifîn", "Al-Mutaffifin", "مطففین"),
    (84, "Al-Inshiqaq", "الانشقاق", "আল-ইনশিকাক", "الانشقاق", "अल-इन्शिक़ाक़", "Al-Insyiqaq", "İnşikâk", "Al-Inshiqaq", "انشقاق"),
    (85, "Al-Buruj", "البروج", "আল-বুরূজ", "البروج", "अल-बुरूज", "Al-Buruj", "Bürûc", "Al-Buruj", "بروج"),
    (86, "At-Tariq", "الطارق", "আত-ত্বারিক", "الطارق", "अत-तारिक़", "At-Tariq", "Târık", "At-Tariq", "طارق"),
    (87, "Al-A'la", "الأعلى", "আল-আ'লা", "الاعلیٰ", "अल-आला", "Al-A'la", "A'lâ", "Al-A'la", "اعلی"),
    (88, "Al-Ghashiyah", "الغاشية", "আল-গাশিয়াহ", "الغاشیہ", "अल-ग़ाशिया", "Al-Gasyiyah", "Gâşiye", "Al-Ghashiya", "غاشیه"),
    (89, "Al-Fajr", "الفجر", "আল-ফজর", "الفجر", "अल-फ़ज्र", "Al-Fajr", "Fecr", "Al-Fajr", "فجر"),
    (90, "Al-Balad", "البلد", "আল-বালাদ", "البلد", "अल-बलद", "Al-Balad", "Beled", "Al-Balad", "بلد"),
    (91, "Ash-Shams", "الشمس", "আশ-শামস", "الشمس", "अश-शम्स", "Asy-Syams", "Şems", "Ach-Chams", "شمس"),
    (92, "Al-Layl", "الليل", "আল-লাইল", "اللیل", "अल-लैल", "Al-Lail", "Leyl", "Al-Layl", "لیل"),
    (93, "Ad-Duha", "الضحى", "আদ-দুহা", "الضحیٰ", "अद-दुहा", "Ad-Duha", "Duhâ", "Ad-Duha", "ضحی"),
    (94, "Ash-Sharh", "الشرح", "আল-ইনশিরাহ", "الانشراح", "अल-इन्शिराह", "Asy-Syarh", "İnşirâh", "Al-Inshirah", "شرح"),
    (95, "At-Tin", "التين", "আত-তীন", "التین", "अत-तीन", "At-Tin", "Tîn", "At-Tin", "تین"),
    (96, "Al-'Alaq", "العلق", "আল-আলাক", "العلق", "अल-अलक़", "Al-'Alaq", "Alak", "Al-Alaq", "علق"),
    (97, "Al-Qadr", "القدر", "আল-ক্বদর", "القدر", "अल-क़द्र", "Al-Qadr", "Kadir", "Al-Qadr", "قدر"),
    (98, "Al-Bayyinah", "البينة", "আল-বায়্যিনাহ", "البینہ", "अल-बय्यिना", "Al-Bayyinah", "Beyyine", "Al-Bayyina", "بینه"),
    (99, "Az-Zalzalah", "الزلزلة", "আজ-যিলযাল", "الزلزال", "अज़-ज़लज़ला", "Az-Zalzalah", "Zilzâl", "Az-Zalzala", "زلزله"),
    (100, "Al-'Adiyat", "العاديات", "আল-আদিয়াত", "العادیات", "अल-आदियात", "Al-'Adiyat", "Âdiyât", "Al-Adiyat", "عادیات"),
    (101, "Al-Qari'ah", "القارعة", "আল-কারিয়াহ", "القارعہ", "अल-क़ारिआ", "Al-Qari'ah", "Kâria", "Al-Qari'a", "قارعه"),
    (102, "At-Takathur", "التكاثر", "আত-তাকাসুর", "التکاثر", "अत-तकासुर", "At-Takasur", "Tekâsür", "At-Takathur", "تکاثر"),
    (103, "Al-'Asr", "العصر", "আল-আসর", "العصر", "अल-अस्र", "Al-'Asr", "Asr", "Al-Asr", "عصر"),
    (104, "Al-Humazah", "الهمزة", "আল-হুমাযাহ", "الہمزہ", "अल-हुमज़ा", "Al-Humazah", "Hümeze", "Al-Humaza", "همزه"),
    (105, "Al-Fil", "الفيل", "আল-ফীল", "الفیل", "अल-फ़ील", "Al-Fil", "Fîl", "Al-Fil", "فیل"),
    (106, "Quraysh", "قريش", "কুরাইশ", "قریش", "क़ुरैश", "Quraisy", "Kureyş", "Quraych", "قریش"),
    (107, "Al-Ma'un", "الماعون", "আল-মাউন", "الماعون", "अल-माऊन", "Al-Ma'un", "Mâûn", "Al-Ma'un", "ماعون"),
    (108, "Al-Kawthar", "الكوثر", "আল-কাউসার", "الکوثر", "अल-कौसर", "Al-Kausar", "Kevser", "Al-Kawthar", "کوثر"),
    (109, "Al-Kafirun", "الكافرون", "আল-কাফিরুন", "الکافرون", "अल-काफ़िरून", "Al-Kafirun", "Kâfirûn", "Al-Kafirun", "کافرون"),
    (110, "An-Nasr", "النصر", "আন-নাসর", "النصر", "अन-नस्र", "An-Nasr", "Nasr", "An-Nasr", "نصر"),
    (111, "Al-Masad", "المسد", "আল-লাহাব", "اللھب", "अल-लहब", "Al-Lahab", "Tebbet", "Al-Masad", "مسد"),
    (112, "Al-Ikhlas", "الإخلاص", "আল-ইখলাস", "الاخلاص", "अल-इख़्लास", "Al-Ikhlas", "İhlâs", "Al-Ikhlas", "اخلاص"),
    (113, "Al-Falaq", "الفلق", "আল-ফালাক", "الفلق", "अल-फ़लक़", "Al-Falaq", "Felak", "Al-Falaq", "فلق"),
    (114, "An-Nas", "الناس", "আন-নাস", "الناس", "अन-नास", "An-Nas", "Nâs", "An-Nas", "ناس"),
]

def escape_kt(s: str) -> str:
    return s.replace('\\', '\\\\').replace('"', '\\"')

lines = []
lines.append('package com.quranicwords.app.core.util')
lines.append('')
lines.append('import com.quranicwords.app.core.domain.model.Language')
lines.append('')
lines.append('/**')
lines.append(' * Standard transliterated and localized names of all 114 Surahs across supported languages.')
lines.append(' */')
lines.append('object SurahNames {')
lines.append('')
lines.append('    data class Entry(')
lines.append('        val en: String,')
lines.append('        val ar: String,')
lines.append('        val bn: String,')
lines.append('        val ur: String,')
lines.append('        val hi: String,')
lines.append('        val id: String,')
lines.append('        val tr: String,')
lines.append('        val fr: String,')
lines.append('        val fa: String')
lines.append('    )')
lines.append('')
lines.append('    private val SURAH_ENTRIES = arrayOf(')

for s in surahs:
    num, en, ar, bn, ur, hi, id_name, tr, fr, fa = s
    e_en = escape_kt(en)
    e_ar = escape_kt(ar)
    e_bn = escape_kt(bn)
    e_ur = escape_kt(ur)
    e_hi = escape_kt(hi)
    e_id = escape_kt(id_name)
    e_tr = escape_kt(tr)
    e_fr = escape_kt(fr)
    e_fa = escape_kt(fa)
    lines.append(f'        Entry("{e_en}", "{e_ar}", "{e_bn}", "{e_ur}", "{e_hi}", "{e_id}", "{e_tr}", "{e_fr}", "{e_fa}"), // {num}')

lines.append('    )')
lines.append('')
lines.append('    /**')
lines.append('     * Returns the localized name of the surah corresponding to [surahNumber] (1..114).')
lines.append('     * Falls back to English transliteration if [language] is not specifically localized.')
lines.append('     */')
lines.append('    fun getSurahName(surahNumber: Int, language: Language): String {')
lines.append('        if (surahNumber !in 1..114) return ""')
lines.append('        val entry = SURAH_ENTRIES[surahNumber - 1]')
lines.append('        return when (language) {')
lines.append('            Language.ENGLISH -> entry.en')
lines.append('            Language.BANGLA -> entry.bn')
lines.append('            Language.URDU -> entry.ur')
lines.append('            Language.HINDI -> entry.hi')
lines.append('            Language.INDONESIAN, Language.MALAY -> entry.id')
lines.append('            Language.TURKISH -> entry.tr')
lines.append('            Language.FRENCH -> entry.fr')
lines.append('            Language.PERSIAN -> entry.fa')
lines.append('            Language.HAUSA, Language.SWAHILI -> entry.en')
lines.append('        }')
lines.append('    }')
lines.append('}')
lines.append('')

with open('app/src/main/java/com/quranicwords/app/core/util/SurahNames.kt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))

print('Wrote SurahNames.kt directly from python script file!')
