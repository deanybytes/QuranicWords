package com.quranicwords.app.core.util

import com.quranicwords.app.core.domain.model.Language

/**
 * Standard transliterated and localized names of all 114 Surahs across supported languages.
 */
object SurahNames {

    data class Entry(
        val en: String,
        val ar: String,
        val bn: String,
        val ur: String,
        val hi: String,
        val id: String,
        val tr: String,
        val fr: String,
        val fa: String
    )

    private val SURAH_ENTRIES = arrayOf(
        Entry("Al-Fatihah", "الفاتحة", "আল-ফাতিহা", "الفاتحۃ", "अल-फ़ातिहा", "Al-Fatihah", "Fâtiha", "Al-Fatiha", "فاتحه"), // 1
        Entry("Al-Baqarah", "البقرة", "আল-বাকারা", "البقرۃ", "अल-बक़रह", "Al-Baqarah", "Bakara", "Al-Baqara", "بقره"), // 2
        Entry("Ali 'Imran", "آل عمران", "আলে ইমরান", "آل عمران", "आले-इमरान", "Ali 'Imran", "Âl-i İmrân", "Al-Imran", "آل عمران"), // 3
        Entry("An-Nisa", "النساء", "আন-নিসা", "النساء", "अन-निसा", "An-Nisa'", "Nisâ", "An-Nisa", "نساء"), // 4
        Entry("Al-Ma'idah", "المائدة", "আল-মায়িদাহ", "المائدۃ", "अल-माइदा", "Al-Ma'idah", "Mâide", "Al-Ma'ida", "مائده"), // 5
        Entry("Al-An'am", "الأنعام", "আল-আন'আম", "الانعام", "अल-अनआम", "Al-An'am", "En'âm", "Al-An'am", "انعام"), // 6
        Entry("Al-A'raf", "الأعراف", "আল-আ'রাফ", "الاعراف", "अल-अराफ़", "Al-A'raf", "A'râf", "Al-A'raf", "اعراف"), // 7
        Entry("Al-Anfal", "الأنفال", "আল-আনফাল", "الانفال", "अल-अनफ़ाल", "Al-Anfal", "Enfâl", "Al-Anfal", "انفال"), // 8
        Entry("At-Tawbah", "التوبة", "আত-তাওবাহ", "التوبۃ", "अत-तौबा", "At-Taubah", "Tevbe", "At-Tawba", "توبه"), // 9
        Entry("Yunus", "يونس", "ইউনুস", "یونس", "यूनुस", "Yunus", "Yûnus", "Yunus", "یونس"), // 10
        Entry("Hud", "هود", "হূদ", "ہود", "हूद", "Hud", "Hûd", "Hud", "هود"), // 11
        Entry("Yusuf", "يوسف", "ইউসুফ", "یوسف", "यूसुफ़", "Yusuf", "Yûsuf", "Yusuf", "یوسف"), // 12
        Entry("Ar-Ra'd", "الرعد", "আর-রাদ", "الرعد", "अर-रअद", "Ar-Ra'd", "Ra'd", "Ar-Ra'd", "رعد"), // 13
        Entry("Ibrahim", "ابراهيم", "ইবরাহীম", "ابراہیم", "इब्राहीम", "Ibrahim", "İbrâhîm", "Ibrahim", "ابراهیم"), // 14
        Entry("Al-Hijr", "الحجر", "আল-হিজর", "الحجر", "अल-हिज्र", "Al-Hijr", "Hicr", "Al-Hijr", "حجر"), // 15
        Entry("An-Nahl", "النحل", "আন-নাহল", "النحل", "अन-नहल", "An-Nahl", "Nahl", "An-Nahl", "نحل"), // 16
        Entry("Al-Isra", "الإسراء", "আল-ইসরা", "الاسراء", "अल-इस्रा", "Al-Isra'", "İsrâ", "Al-Isra", "اسراء"), // 17
        Entry("Al-Kahf", "الكهف", "আল-কাহফ", "الکہف", "अल-कहफ़", "Al-Kahf", "Kehf", "Al-Kahf", "کهف"), // 18
        Entry("Maryam", "مريم", "মারইয়াম", "مریم", "मरयम", "Maryam", "Meryem", "Maryam", "مریم"), // 19
        Entry("Taha", "طه", "ত্বা-হা", "طٰہٰ", "ता-हा", "Taha", "Tâhâ", "Ta-Ha", "طه"), // 20
        Entry("Al-Anbya", "الأنبياء", "আল-আম্বিয়া", "الانبیاء", "अल-अम्बिया", "Al-Anbiya'", "Enbiyâ", "Al-Anbiya", "انبیاء"), // 21
        Entry("Al-Hajj", "الحج", "আল-হাজ্জ", "الحج", "अल-हज", "Al-Hajj", "Hac", "Al-Hajj", "حج"), // 22
        Entry("Al-Mu'minun", "المؤمنون", "আল-মু'মিনূন", "المؤمنون", "अल-मोमिनून", "Al-Mu'minun", "Mü'minûn", "Al-Mu'minun", "مؤمنون"), // 23
        Entry("An-Nur", "النور", "আন-নূর", "النور", "अन-नूर", "An-Nur", "Nûr", "An-Nur", "نور"), // 24
        Entry("Al-Furqan", "الفرقان", "আল-ফুরকান", "الفرقان", "अल-फ़ुरक़ान", "Al-Furqan", "Furkân", "Al-Furqan", "فرقان"), // 25
        Entry("Ash-Shu'ara", "الشعراء", "আশ-শু'আরা", "الشعراء", "अश-शुअरा", "Asy-Syu'ara'", "Şuarâ", "Ach-Chu'ara", "شعراء"), // 26
        Entry("An-Naml", "النمل", "আন-নামল", "النمل", "अन-नम्ल", "An-Naml", "Neml", "An-Naml", "نمل"), // 27
        Entry("Al-Qasas", "القصص", "আল-কাসাস", "القصص", "अल-क़सस", "Al-Qasas", "Kasas", "Al-Qasas", "قصص"), // 28
        Entry("Al-'Ankabut", "العنكبوت", "আল-আনকাবূত", "العنکبوت", "अल-अंकबूत", "Al-'Ankabut", "Ankebût", "Al-Ankabut", "عنکبوت"), // 29
        Entry("Ar-Rum", "الروم", "আর-রূম", "الروم", "अर-रूम", "Ar-Rum", "Rûm", "Ar-Rum", "روم"), // 30
        Entry("Luqman", "لقمان", "লুকমান", "لقمان", "लुक़्मान", "Luqman", "Lokmân", "Luqman", "لقمان"), // 31
        Entry("As-Sajdah", "السجدة", "আস-সাজদাহ", "السجدۃ", "अस-सजदा", "As-Sajdah", "Secde", "As-Sajda", "سجده"), // 32
        Entry("Al-Ahzab", "الأحزاب", "আল-আহযাব", "الاحزاب", "अल-अहज़ाब", "Al-Ahzab", "Ahzâb", "Al-Ahzab", "احزاب"), // 33
        Entry("Saba", "سبإ", "সাবা", "سبا", "सबा", "Saba'", "Sebe'", "Saba", "سبأ"), // 34
        Entry("Fatir", "فاطر", "ফাতির", "فاطر", "फ़ातिर", "Fatir", "Fâtır", "Fatir", "فاطر"), // 35
        Entry("Ya-Sin", "يس", "ইয়াসীন", "یٰسین", "या-सीन", "Yasin", "Yâsîn", "Ya-Sin", "یاسین"), // 36
        Entry("As-Saffat", "الصافات", "আস-সাফফাত", "الصافات", "अस-साफ़्फ़ात", "As-Saffat", "Sâffât", "As-Saffat", "صافات"), // 37
        Entry("Sad", "ص", "সোয়াদ", "ص", "साद", "Sad", "Sâd", "Sad", "ص"), // 38
        Entry("Az-Zumar", "الزمر", "আজ-জুমার", "الزمر", "अज-ज़ुमर", "Az-Zumar", "Zümer", "Az-Zumar", "زمر"), // 39
        Entry("Ghafir", "غافر", "গাফির", "غافر", "ग़ाफ़िर", "Gafir", "Mü'min", "Ghafir", "غافر"), // 40
        Entry("Fussilat", "فصلت", "ফুসসিলাত", "فصلت", "फ़ुस्स़िलत", "Fussilat", "Fussilet", "Fussilat", "فصلت"), // 41
        Entry("Ash-Shura", "الشورى", "আশ-শূরা", "الشوریٰ", "अश-शूरा", "Asy-Syura", "Şûrâ", "Ach-Chura", "شوری"), // 42
        Entry("Az-Zukhruf", "الزخرف", "আজ-যুখরুফ", "الزخرف", "अज़-ज़ुख़रुफ़", "Az-Zukhruf", "Zuhruf", "Az-Zukhruf", "زخرف"), // 43
        Entry("Ad-Dukhan", "الدخان", "আদ-দুখান", "الدخان", "अद-दुख़ान", "Ad-Dukhan", "Duhân", "Ad-Dukhan", "دخان"), // 44
        Entry("Al-Jathiyah", "الجاثية", "আল-জাসিয়াহ", "الجاثیہ", "अल-जासिया", "Al-Jasiyah", "Câsiye", "Al-Jathiya", "جاثیه"), // 45
        Entry("Al-Ahqaf", "الأحقاف", "আল-আহকাফ", "الاحقاف", "अल-अहक़ाफ़", "Al-Ahqaf", "Ahkâf", "Al-Ahqaf", "احقاف"), // 46
        Entry("Muhammad", "محمد", "মুহাম্মদ", "محمد", "मुहम्मद", "Muhammad", "Muhammed", "Muhammad", "محمد"), // 47
        Entry("Al-Fath", "الفتح", "আল-ফাতহ", "الفتح", "अल-फ़तह", "Al-Fath", "Fetih", "Al-Fath", "فتح"), // 48
        Entry("Al-Hujurat", "الحجرات", "আল-হুজুরাত", "الحجرات", "अल-हुजुरात", "Al-Hujurat", "Hucurât", "Al-Hujurat", "حجرات"), // 49
        Entry("Qaf", "ق", "ক্বাফ", "ق", "क़ाफ़", "Qaf", "Kâf", "Qaf", "ق"), // 50
        Entry("Adh-Dhariyat", "الذاريات", "আজ-যারিয়াত", "الذاریات", "अज़-ज़ारियात", "Az-Zariyat", "Zâriyât", "Adh-Dhariyat", "ذاریات"), // 51
        Entry("At-Tur", "الطور", "আত-তূর", "الطور", "अत-तूर", "At-Tur", "Tûr", "At-Tur", "طور"), // 52
        Entry("An-Najm", "النجم", "আন-নাজম", "النجم", "अन-नज्म", "An-Najm", "Necm", "An-Najm", "نجم"), // 53
        Entry("Al-Qamar", "القمر", "আল-ক্বামার", "القمر", "अल-क़मर", "Al-Qamar", "Kamer", "Al-Qamar", "قمر"), // 54
        Entry("Ar-Rahman", "الرحمن", "আর-রহমান", "الرحمن", "अर-रहमान", "Ar-Rahman", "Rahmân", "Ar-Rahman", "رحمن"), // 55
        Entry("Al-Waqi'ah", "الواقعة", "আল-ওয়াকিয়াহ", "الواقعۃ", "अल-वाक़िया", "Al-Waqi'ah", "Vâkıa", "Al-Waqi'a", "واقعه"), // 56
        Entry("Al-Hadid", "الحديد", "আল-হাদীদ", "الحدید", "अल-हदीद", "Al-Hadid", "Hadîd", "Al-Hadid", "حدید"), // 57
        Entry("Al-Mujadila", "المجادلة", "আল-মুজাদালাহ", "المجادلہ", "अल-मुजादिला", "Al-Mujadilah", "Mücâdele", "Al-Mujadila", "مجادله"), // 58
        Entry("Al-Hashr", "الحشر", "আল-হাশর", "الحشر", "अल-हश्र", "Al-Hasyr", "Haşr", "Al-Hashr", "حشر"), // 59
        Entry("Al-Mumtahanah", "الممتحنة", "আল-মুমতাহিনাহ", "الممتحنہ", "अल-मुमतहिना", "Al-Mumtahanah", "Mümtehine", "Al-Mumtahana", "ممتحنه"), // 60
        Entry("As-Saf", "الصف", "আস-সাফ", "الصف", "अस-सफ़", "As-Saff", "Saf", "As-Saff", "صف"), // 61
        Entry("Al-Jumu'ah", "الجمعة", "আল-জুমুআহ", "الجمعہ", "अल-जुमुआ", "Al-Jumu'ah", "Cuma", "Al-Jumu'a", "جمعه"), // 62
        Entry("Al-Munafiqun", "المنافقون", "আল-মুনাফিকুন", "المنافقون", "अल-मुनाफ़िक़ून", "Al-Munafiqun", "Münâfikûn", "Al-Munafiqun", "منافقون"), // 63
        Entry("At-Taghabun", "التغابن", "আত-তাগাবুন", "التغابن", "अत-तग़ाबुन", "At-Tagabun", "Tegâbün", "At-Taghabun", "تغابن"), // 64
        Entry("At-Talaq", "الطلاق", "আত-ত্বালাক", "الطلاق", "अत-तलाक़", "At-Talaq", "Talâk", "At-Talaq", "طلاق"), // 65
        Entry("At-Tahrim", "التحريم", "আত-তাহরীম", "التحریم", "अत-तहरीम", "At-Tahrim", "Tahrîm", "At-Tahrim", "تحریم"), // 66
        Entry("Al-Mulk", "الملك", "আল-মুলক", "الملک", "अल-मुल्क", "Al-Mulk", "Mülk", "Al-Mulk", "ملک"), // 67
        Entry("Al-Qalam", "القلم", "আল-কলম", "القلم", "अल-क़लम", "Al-Qalam", "Kalem", "Al-Qalam", "قلم"), // 68
        Entry("Al-Haqqah", "الحاقة", "আল-হাক্কাহ", "الحاقۃ", "अल-हाक़्क़ा", "Al-Haqqah", "Hâkka", "Al-Haqqa", "حاقه"), // 69
        Entry("Al-Ma'arij", "المعارج", "আল-মাআরিজ", "المعارج", "अल-मआरिज", "Al-Ma'arij", "Meâric", "Al-Ma'arij", "معارج"), // 70
        Entry("Nuh", "نوح", "নূহ", "نوح", "नूह", "Nuh", "Nûh", "Nuh", "نوح"), // 71
        Entry("Al-Jinn", "الجن", "আল-জ্বিন", "الجن", "अल-जिन्न", "Al-Jinn", "Cin", "Al-Jinn", "جن"), // 72
        Entry("Al-Muzzammil", "المزمل", "আল-মুযযাম্মিল", "المزمل", "अल-मुज़्ज़म्मिल", "Al-Muzzammil", "Müzzemmil", "Al-Muzzammil", "مزمل"), // 73
        Entry("Al-Muddaththir", "المدثر", "আল-মুদ্দাসসির", "المدثر", "अल-मुद्दस्सिर", "Al-Muddassir", "Müddessir", "Al-Muddaththir", "مدثر"), // 74
        Entry("Al-Qiyamah", "القيامة", "আল-কিয়ামাহ", "القیامہ", "अल-क़ियामा", "Al-Qiyamah", "Kıyâme", "Al-Qiyama", "قیامت"), // 75
        Entry("Al-Insan", "الانسان", "আল-ইনসান", "الانسان", "अल-इंसान", "Al-Insan", "İnsân", "Al-Insan", "انسان"), // 76
        Entry("Al-Mursalat", "المرسلات", "আল-মুরসালাত", "المرسلات", "अल-मुर्सलात", "Al-Mursalat", "Mürselât", "Al-Mursalat", "مرسلات"), // 77
        Entry("An-Naba", "النبإ", "আন-নাবা", "النبا", "अन-नबा", "An-Naba'", "Nebe'", "An-Naba", "نبأ"), // 78
        Entry("An-Nazi'at", "النازعات", "আন-নাযিয়াত", "النازعات", "अन-नाज़िआत", "An-Nazi'at", "Nâziât", "An-Nazi'at", "نازعات"), // 79
        Entry("'Abasa", "عبس", "আবাসা", "عبس", "अबसा", "'Abasa", "Abese", "Abasa", "عبس"), // 80
        Entry("At-Takwir", "التكوير", "আত-তাকভীর", "التکویر", "अत-तकवीर", "At-Takwir", "Tekvîr", "At-Takwir", "تکویر"), // 81
        Entry("Al-Infitar", "الانفطار", "আল-ইনফিতার", "الانفطار", "अल-इन्फ़ितार", "Al-Infitar", "İnfitâr", "Al-Infitar", "انفطار"), // 82
        Entry("Al-Mutaffifin", "المطففين", "আল-মুতাফফিফীন", "المطففین", "अल-मुतफ़्फ़िफ़ीन", "Al-Mutaffifin", "Mutaffifîn", "Al-Mutaffifin", "مطففین"), // 83
        Entry("Al-Inshiqaq", "الانشقاق", "আল-ইনশিকাক", "الانشقاق", "अल-इन्शिक़ाक़", "Al-Insyiqaq", "İnşikâk", "Al-Inshiqaq", "انشقاق"), // 84
        Entry("Al-Buruj", "البروج", "আল-বুরূজ", "البروج", "अल-बुरूज", "Al-Buruj", "Bürûc", "Al-Buruj", "بروج"), // 85
        Entry("At-Tariq", "الطارق", "আত-ত্বারিক", "الطارق", "अत-तारिक़", "At-Tariq", "Târık", "At-Tariq", "طارق"), // 86
        Entry("Al-A'la", "الأعلى", "আল-আ'লা", "الاعلیٰ", "अल-आला", "Al-A'la", "A'lâ", "Al-A'la", "اعلی"), // 87
        Entry("Al-Ghashiyah", "الغاشية", "আল-গাশিয়াহ", "الغاشیہ", "अल-ग़ाशिया", "Al-Gasyiyah", "Gâşiye", "Al-Ghashiya", "غاشیه"), // 88
        Entry("Al-Fajr", "الفجر", "আল-ফজর", "الفجر", "अल-फ़ज्र", "Al-Fajr", "Fecr", "Al-Fajr", "فجر"), // 89
        Entry("Al-Balad", "البلد", "আল-বালাদ", "البلد", "अल-बलद", "Al-Balad", "Beled", "Al-Balad", "بلد"), // 90
        Entry("Ash-Shams", "الشمس", "আশ-শামস", "الشمس", "अश-शम्स", "Asy-Syams", "Şems", "Ach-Chams", "شمس"), // 91
        Entry("Al-Layl", "الليل", "আল-লাইল", "اللیل", "अल-लैल", "Al-Lail", "Leyl", "Al-Layl", "لیل"), // 92
        Entry("Ad-Duha", "الضحى", "আদ-দুহা", "الضحیٰ", "अद-दुहा", "Ad-Duha", "Duhâ", "Ad-Duha", "ضحی"), // 93
        Entry("Ash-Sharh", "الشرح", "আল-ইনশিরাহ", "الانشراح", "अल-इन्शिराह", "Asy-Syarh", "İnşirâh", "Al-Inshirah", "شرح"), // 94
        Entry("At-Tin", "التين", "আত-তীন", "التین", "अत-तीन", "At-Tin", "Tîn", "At-Tin", "تین"), // 95
        Entry("Al-'Alaq", "العلق", "আল-আলাক", "العلق", "अल-अलक़", "Al-'Alaq", "Alak", "Al-Alaq", "علق"), // 96
        Entry("Al-Qadr", "القدر", "আল-ক্বদর", "القدر", "अल-क़द्र", "Al-Qadr", "Kadir", "Al-Qadr", "قدر"), // 97
        Entry("Al-Bayyinah", "البينة", "আল-বায়্যিনাহ", "البینہ", "अल-बय्यिना", "Al-Bayyinah", "Beyyine", "Al-Bayyina", "بینه"), // 98
        Entry("Az-Zalzalah", "الزلزلة", "আজ-যিলযাল", "الزلزال", "अज़-ज़लज़ला", "Az-Zalzalah", "Zilzâl", "Az-Zalzala", "زلزله"), // 99
        Entry("Al-'Adiyat", "العاديات", "আল-আদিয়াত", "العادیات", "अल-आदियात", "Al-'Adiyat", "Âdiyât", "Al-Adiyat", "عادیات"), // 100
        Entry("Al-Qari'ah", "القارعة", "আল-কারিয়াহ", "القارعہ", "अल-क़ारिआ", "Al-Qari'ah", "Kâria", "Al-Qari'a", "قارعه"), // 101
        Entry("At-Takathur", "التكاثر", "আত-তাকাসুর", "التکاثر", "अत-तकासुर", "At-Takasur", "Tekâsür", "At-Takathur", "تکاثر"), // 102
        Entry("Al-'Asr", "العصر", "আল-আসর", "العصر", "अल-अस्र", "Al-'Asr", "Asr", "Al-Asr", "عصر"), // 103
        Entry("Al-Humazah", "الهمزة", "আল-হুমাযাহ", "الہمزہ", "अल-हुमज़ा", "Al-Humazah", "Hümeze", "Al-Humaza", "همزه"), // 104
        Entry("Al-Fil", "الفيل", "আল-ফীল", "الفیل", "अल-फ़ील", "Al-Fil", "Fîl", "Al-Fil", "فیل"), // 105
        Entry("Quraysh", "قريش", "কুরাইশ", "قریش", "क़ुरैश", "Quraisy", "Kureyş", "Quraych", "قریش"), // 106
        Entry("Al-Ma'un", "الماعون", "আল-মাউন", "الماعون", "अल-माऊन", "Al-Ma'un", "Mâûn", "Al-Ma'un", "ماعون"), // 107
        Entry("Al-Kawthar", "الكوثر", "আল-কাউসার", "الکوثر", "अल-कौसर", "Al-Kausar", "Kevser", "Al-Kawthar", "کوثر"), // 108
        Entry("Al-Kafirun", "الكافرون", "আল-কাফিরুন", "الکافرون", "अल-काफ़िरून", "Al-Kafirun", "Kâfirûn", "Al-Kafirun", "کافرون"), // 109
        Entry("An-Nasr", "النصر", "আন-নাসর", "النصر", "अन-नस्र", "An-Nasr", "Nasr", "An-Nasr", "نصر"), // 110
        Entry("Al-Masad", "المسد", "আল-লাহাব", "اللھب", "अल-लहब", "Al-Lahab", "Tebbet", "Al-Masad", "مسد"), // 111
        Entry("Al-Ikhlas", "الإخلاص", "আল-ইখলাস", "الاخلاص", "अल-इख़्लास", "Al-Ikhlas", "İhlâs", "Al-Ikhlas", "اخلاص"), // 112
        Entry("Al-Falaq", "الفلق", "আল-ফালাক", "الفلق", "अल-फ़लक़", "Al-Falaq", "Felak", "Al-Falaq", "فلق"), // 113
        Entry("An-Nas", "الناس", "আন-নাস", "الناس", "अन-नास", "An-Nas", "Nâs", "An-Nas", "ناس"), // 114
    )

    /**
     * Returns the localized name of the surah corresponding to [surahNumber] (1..114).
     * Falls back to English transliteration if [language] is not specifically localized.
     */
    fun getSurahName(surahNumber: Int, language: Language): String {
        if (surahNumber !in 1..114) return ""
        val entry = SURAH_ENTRIES[surahNumber - 1]
        return when (language) {
            Language.ENGLISH -> entry.en
            Language.BANGLA -> entry.bn
            Language.URDU -> entry.ur
            Language.HINDI -> entry.hi
            Language.INDONESIAN, Language.MALAY -> entry.id
            Language.TURKISH -> entry.tr
            Language.FRENCH -> entry.fr
            Language.PERSIAN -> entry.fa
            Language.HAUSA, Language.SWAHILI -> entry.en
        }
    }
}
