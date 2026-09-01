package com.quranicwords.app.core.domain.model

/**
 * Complete identification & specification of 10 authentic Quranic Arabic script styles offered in the app.
 *
 * Each style maps directly to a genuine, verified OpenType typeface bundled in `res/font`.
 */
enum class QuranFontStyle(
    val displayName: LocalizedText,
    val scriptFamily: LocalizedText,
    val originRegion: LocalizedText,
    val keyFeatures: List<LocalizedText>,
    val fontKey: String,
    val isBundled: Boolean = true
) {
    MADANI_KFGQPC(
        displayName = mapOf(
            "en" to "Madinah Mushaf (KFGQPC Hafs)",
            "bn" to "মদিনা মুসহাফ (কিং ফাহাদ কমপ্লেক্স)",
            "ur" to "مدینہ مصحف (شاہ فہد کمپلیکس حفص)",
            "in" to "Mushaf Madinah (KFGQPC Hafs)",
            "tr" to "Medine Mushafı (KFGQPC Hafs)",
            "fr" to "Mushaf de Médine (KFGQPC Hafs)"
        ),
        scriptFamily = mapOf(
            "en" to "Official Medina Mushaf (Hafs)",
            "bn" to "অফিসিয়াল মদিনা মুসহাফ (হাফস)",
            "ur" to "سرکاری مدینہ مصحف (حفص)",
            "in" to "Mushaf Resmi Madinah (Hafs)",
            "tr" to "Resmî Medine Mushafı (Hafs)",
            "fr" to "Mushaf Officiel de Médine (Hafs)"
        ),
        originRegion = mapOf(
            "en" to "King Fahd Glorious Quran Complex, Medina",
            "bn" to "কিং ফাহাদ কুরআন প্রিন্টিং কমপ্লেক্স, মদিনা",
            "ur" to "شاہ فہد قرآن کمپلیکس، مدینہ منورہ",
            "in" to "Kompleks Percetakan Al-Qur'an Raja Fahd, Madinah",
            "tr" to "Kral Fehd Kur'an Basım Kompleksi, Medine",
            "fr" to "Complexe du Roi Fahd pour l'Impression du Noble Coran, Médine"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "Official King Fahd Complex Madinah typography",
                "bn" to "কিং ফাহাদ কমপ্লেক্সের অফিসিয়াল মদিনা মুদ্রণরীতি",
                "ur" to "شاہ فہد کمپلیکس مدینہ منورہ کا معیاری رسم الخط",
                "in" to "Tipografi resmi Kompleks Madinah Raja Fahd",
                "tr" to "Resmî Kral Fehd Kompleksi Medine tipografisi",
                "fr" to "Typographie officielle du Complexe du Roi Fahd à Médine"
            ),
            mapOf(
                "en" to "Open-head Kha Jazm ( ۡ ) and subtle Maddah marks",
                "bn" to "উন্মুক্ত খ-যুক্ত জযম ( ۡ ) ও সূক্ষ্ম মাদ্দ চিহ্ন",
                "ur" to "کھلا جزم ( ۡ ) اور خوبصورت مد کے علامات",
                "in" to "Tanda sukun khas kepala kha ( ۡ ) dan tanda mad halus",
                "tr" to "Açık uçlu cezim ( ۡ ) ve zarif med işaretleri",
                "fr" to "Jazm en tête de Kha ouvert ( ۡ ) et signes de Maddah subtils"
            ),
            mapOf(
                "en" to "Classic, universally recognized Uthman Taha hand",
                "bn" to "উসমান ত্বাহার আন্তর্জাতিকভাবে সমাদৃত হস্তলিপি",
                "ur" to "عثمان طہٰ کا عالمی سطح پر معروف خط",
                "in" to "Gaya kaligrafi klasik Utsman Thaha yang mendunia",
                "tr" to "Klasik, evrensel olarak tanınan Osman Taha hattı",
                "fr" to "Calligraphie classique reconnue d'Othman Taha"
            ),
            mapOf(
                "en" to "Gold standard across international Quran prints",
                "bn" to "বিশ্বজুড়ে সর্বাধিক স্বীকৃত ও মানসম্মত লিপি",
                "ur" to "بین الاقوامی سطح پر سب سے معتبر قرآنی خط",
                "in" to "Standar utama cetakan Al-Qur'an internasional",
                "tr" to "Uluslararası Kur'an basımlarında altın standart",
                "fr" to "Standard de référence des impressions coraniques internationales"
            )
        ),
        fontKey = "kfgqpc_hafs"
    ),

    INDOPAK_NASKH(
        displayName = mapOf(
            "en" to "Indo-Pak / Noorani Naskh (Lateef)",
            "bn" to "ইন্দো-পাক / নূরানী নাসখ (লতীফ)",
            "ur" to "ہند و پاک / نورانی نسخ (لطیف)",
            "in" to "Indo-Pak / Naskh Noorani (Lateef)",
            "tr" to "Hint-Pak / Nurani Nesih (Latif)",
            "fr" to "Indo-Pak / Naskh Nourani (Lateef)"
        ),
        scriptFamily = mapOf(
            "en" to "Subcontinental Noorani Naskh",
            "bn" to "উপমহাদেশীয় নূরানী ও হাফেজী নাসখ",
            "ur" to "برصغیر کا نورانی و حافظی نسخ",
            "in" to "Naskh Noorani Tradisi Asia Selatan",
            "tr" to "Güney Asya Nurani Nesih Geleneği",
            "fr" to "Naskh Nourani du Sous-Continent"
        ),
        originRegion = mapOf(
            "en" to "South Asia (Bangladesh, India, Pakistan)",
            "bn" to "দক্ষিণ এশিয়া (বাংলাদেশ, ভারত, পাকিস্তান)",
            "ur" to "جنوبی ایشیا (پاکستان، بھارت، بنگلہ دیش)",
            "in" to "Asia Selatan (Indonesia, Bangladesh, India, Pakistan)",
            "tr" to "Güney Asya (Pakistan, Hindistan, Bangladeş)",
            "fr" to "Asie du Sud (Bangladesh, Inde, Pakistan)"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "Bold, rounded letters for effortless reading",
                "bn" to "সহজ পাঠযোগ্য স্পষ্ট ও গোলাকার হরফ",
                "ur" to "آسان تلاوت کے لیے موٹے اور گول حروف",
                "in" to "Huruf tebal dan bulat untuk kemudahan membaca",
                "tr" to "Kolay okuma için kalın ve yuvarlak harfler",
                "fr" to "Lettres grasses et arrondies pour une lecture aisée"
            ),
            mapOf(
                "en" to "Prominent Tashdeed and clear vocalization marks",
                "bn" to "স্পষ্ট তাশদীদ ও সুস্পষ্ট হরকত চিহ্ন",
                "ur" to "نمایاں تشدید اور واضح اعراب",
                "in" to "Tasydid mencolok dan tanda harakat yang sangat jelas",
                "tr" to "Belirgin şedde ve net hareke işaretleri",
                "fr" to "Tashdid proéminent et signes de vocalisation clairs"
            ),
            mapOf(
                "en" to "Traditional layout used in South Asian Quran prints",
                "bn" to "দক্ষিণ এশিয়ার ঐতিহ্যবাহী কুরআন মুদ্রণ শৈলী",
                "ur" to "برصغیر کے روایتی قرآنی طباعت کا انداز",
                "in" to "Tata letak tradisional cetakan mushaf Asia Selatan",
                "tr" to "Güney Asya Kur'an basımlarında kullanılan geleneksel düzen",
                "fr" to "Mise en page traditionnelle des impressions d'Asie du Sud"
            ),
            mapOf(
                "en" to "Specially optimized for easy recitation",
                "bn" to "সহজে তিলাওয়াত শেখার জন্য বিশেষভাবে উপযোগী",
                "ur" to "آسان قرات کے لیے خاص طور پر موزوں",
                "in" to "Dioptimalkan khusus untuk tilawah yang lancar",
                "tr" to "Rahat tilavet için özel olarak optimize edilmiştir",
                "fr" to "Spécialement optimisé pour une récitation facile"
            )
        ),
        fontKey = "lateef"
    ),

    INDOPAK_NASTALEEQ(
        displayName = mapOf(
            "en" to "Quranic Nastaliq (KFGQPC)",
            "bn" to "কুরআনিক নাসতালীক (কিং ফাহাদ কমপ্লেক্স)",
            "ur" to "قرآنی نستعلیق (شاہ فہد کمپلیکس)",
            "in" to "Nastaliq Al-Qur'an (KFGQPC)",
            "tr" to "Kur'an Nesitaliği (KFGQPC)",
            "fr" to "Nastaliq Coranique (KFGQPC)"
        ),
        scriptFamily = mapOf(
            "en" to "Authentic Quranic Nastaliq (Hafs)",
            "bn" to "শাস্ত্রীয় কুরআনিক নাসতালীক (হাফস)",
            "ur" to "مستند قرآنی نستعلیق (حفص)",
            "in" to "Nastaliq Al-Qur'an Autentik (Hafs)",
            "tr" to "Özgün Kur'an Nesitaliği (Hafs)",
            "fr" to "Nastaliq Coranique Authentique (Hafs)"
        ),
        originRegion = mapOf(
            "en" to "King Fahd Complex / South Asian Tradition",
            "bn" to "উপমহাদেশ ও কিং ফাহাদ কমপ্লেক্স",
            "ur" to "شاہ فہد کمپلیکس / برصغیر کی روایت",
            "in" to "Kompleks Raja Fahd / Tradisi Asia Selatan",
            "tr" to "Kral Fehd Kompleksi / Güney Asya Geleneği",
            "fr" to "Complexe Roi Fahd / Tradition Sud-Asiatique"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "Authentic King Fahd Complex Quranic Nastaliq",
                "bn" to "কিং ফাহাদ কমপ্লেক্সের শাস্ত্রীয় কুরআনিক নাসতালীক",
                "ur" to "شاہ فہد کمپلیکس کا مستند قرآنی نستعلیق",
                "in" to "Nastaliq Al-Qur'an resmi Kompleks Raja Fahd",
                "tr" to "Resmî Kral Fehd Kompleksi Kur'an nesitaliği",
                "fr" to "Nastaliq coranique authentique du Complexe Roi Fahd"
            ),
            mapOf(
                "en" to "Graceful cascading baselines with complete Quranic diacritics",
                "bn" to "নান্দনিক ঝুলন্ত বেইজলাইন ও পূর্ণাঙ্গ কুরআনিক হরকত",
                "ur" to "خوبصورت ڈھلوان سطریں اور مکمل قرآنی اعراب",
                "in" to "Garis bertingkat anggun dengan harakat Al-Qur'an lengkap",
                "tr" to "Eksiksiz Kur'an harekeleriyle zarif kademeli hat",
                "fr" to "Lignes fluides et diacritiques coraniques complets"
            ),
            mapOf(
                "en" to "Traditional Urdu/Persian Quran calligraphy style",
                "bn" to "ঐতিহ্যবাহী উর্দু ও ফার্সি কুরআন ক্যালিগ্রাফি শৈলী",
                "ur" to "روایتی اردو و فارسی قرآنی خطاطی",
                "in" to "Gaya kaligrafi Al-Qur'an tradisional Urdu/Persia",
                "tr" to "Geleneksel Urdu/Farsça Kur'an hat sanatı tarzı",
                "fr" to "Style calligraphique coranique traditionnel ourdou/persan"
            ),
            mapOf(
                "en" to "Full Tashkeel and Quranic pause symbols",
                "bn" to "পূর্ণাঙ্গ তাশকিল ও ওয়াকফ (থামা) নির্দেশক চিহ্ন",
                "ur" to "مکمل تشکیل اور قرآنی اوقاف کی علامتیں",
                "in" to "Tasykil lengkap dan simbol tanda waqaf Al-Qur'an",
                "tr" to "Tam teşkil ve Kur'an vakıf işaretleri",
                "fr" to "Tashkeel complet et symboles d'arrêt coraniques"
            )
        ),
        fontKey = "hafs_nastaleeq"
    ),

    AMIRI_CLASSIC(
        displayName = mapOf(
            "en" to "Classic Amiri Quran",
            "bn" to "ক্লাসিক আমিরী কুরআন",
            "ur" to "کلاسک امیری قرآن",
            "in" to "Al-Qur'an Klasik Amiri",
            "tr" to "Klasik Emîrî Kur'an",
            "fr" to "Coran Classique Amiri"
        ),
        scriptFamily = mapOf(
            "en" to "Royal Amiri / Bulaq Naskh",
            "bn" to "ঐতিহ্যবাহী রাজকীয় আমিরী নাসখ",
            "ur" to "شاہی امیری / بولاق نسخ",
            "in" to "Naskh Kerajaan Amiri / Bulaq",
            "tr" to "Kraliyet Emîrî / Bulak Neshi",
            "fr" to "Naskh Royal Amiri / Boulaq"
        ),
        originRegion = mapOf(
            "en" to "Egypt & Greater Middle East",
            "bn" to "মিশর ও আরব বিশ্ব",
            "ur" to "مصر اور مشرقِ وسطیٰ",
            "in" to "Mesir & Timur Tengah",
            "tr" to "Mısır ve Orta Doğu",
            "fr" to "Égypte & Moyen-Orient"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "Prestigious classical Cairo 1924 Amiri typography",
                "bn" to "ঐতিহাসিক কায়রো ১৯২৪ আমিরিয়া মুদ্রণরীতির ঐতিহ্য",
                "ur" to "قاہرہ ۱۹۲۴ء کا تاریخی اور پروقار امیری خط",
                "in" to "Tipografi Amiri klasik Kairo 1924 yang bergengsi",
                "tr" to "Seçkin klasik Kahire 1924 Emîrî tipografisi",
                "fr" to "Typographie classique prestigieuse Amiri du Caire 1924"
            ),
            mapOf(
                "en" to "Exquisite calligraphic balance and proportions",
                "bn" to "অনবদ্য ক্যালিগ্রাফিক ভারসাম্য ও হরফ বিন্যাস",
                "ur" to "شاندار خطاطی کا توازن اور متناسب بناوٹ",
                "in" to "Keseimbangan dan proporsi kaligrafi yang sangat indah",
                "tr" to "Zarif hat dengesi ve kusursuz oranlar",
                "fr" to "Équilibre et proportions calligraphiques d'une grande finesse"
            ),
            mapOf(
                "en" to "Historic standard for Islamic scholarship",
                "bn" to "ইসলামি জ্ঞানচর্চার ঐতিহাসিক মানদণ্ড",
                "ur" to "اسلامی علمی روایات کا تاریخی معیار",
                "in" to "Standar bersejarah untuk kajian keilmuan Islam",
                "tr" to "İslam ilim dünyasının tarihi standardı",
                "fr" to "Norme historique des études et sciences islamiques"
            ),
            mapOf(
                "en" to "Crisp rendering with traditional Quranic ligatures",
                "bn" to "ঐতিহ্যবাহী কুরআনিক সংযুক্ত হরফের নিখুঁত রূপায়ন",
                "ur" to "روایتی قرآنی جوڑوں کے ساتھ واضح نمائش",
                "in" to "Tampilan tajam dengan ligatur Al-Qur'an tradisional",
                "tr" to "Geleneksel Kur'an birleşimleriyle net görünüm",
                "fr" to "Rendu net avec ligatures coraniques traditionnelles"
            )
        ),
        fontKey = "amiri"
    ),

    SCHEHERAZADE_NASKH(
        displayName = mapOf(
            "en" to "Scheherazade Traditional Naskh",
            "bn" to "শেহেরেযাদ ঐতিহ্যবাহী নাসখ",
            "ur" to "شہرزاد روایتی نسخ",
            "in" to "Naskh Tradisional Scheherazade",
            "tr" to "Şehrazad Geleneksel Nesih",
            "fr" to "Naskh Traditionnel Shéhérazade"
        ),
        scriptFamily = mapOf(
            "en" to "Traditional Calligraphic Naskh",
            "bn" to "ঐতিহ্যবাহী ক্যালিগ্রাফিক নাসখ",
            "ur" to "روایتی خطاطی نسخ",
            "in" to "Naskh Kaligrafi Tradisional",
            "tr" to "Geleneksel Hattat Neshi",
            "fr" to "Naskh Calligraphique Traditionnel"
        ),
        originRegion = mapOf(
            "en" to "Middle East & International",
            "bn" to "মধ্যপ্রাচ্য ও আন্তর্জাতিক",
            "ur" to "مشرقِ وسطیٰ اور بین الاقوامی",
            "in" to "Timur Tengah & Internasional",
            "tr" to "Orta Doğu ve Uluslararası",
            "fr" to "Moyen-Orient & International"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "Graceful ligatures and extended baseline flow",
                "bn" to "মনোরম সংযুক্ত হরফ ও দীর্ঘায়িত বেইজলাইন প্রবাহ",
                "ur" to "خوبصورت جوڑ اور متوازن سطر بندی",
                "in" to "Ligatur anggun dengan aliran garis dasar yang luas",
                "tr" to "Zarif harf birleşimleri ve akıcı taban çizgisi",
                "fr" to "Ligatures gracieuses et belle continuité de ligne"
            ),
            mapOf(
                "en" to "Carefully proportioned diacritics and signs",
                "bn" to "যথাযথ অনুপাতের হরকত ও তিলাওয়াত নির্দেশক চিহ্ন",
                "ur" to "انتہائی مناسب اور متوازن اعراب",
                "in" to "Tanda diakritik dan harakat dengan proporsi presisi",
                "tr" to "Özenle oranlanmış harekeler ve işaretler",
                "fr" to "Diacritiques et signes proportionnés avec précision"
            ),
            mapOf(
                "en" to "SIL OpenType optimization for high-DPI screens",
                "bn" to "উচ্চ রেজোলিউশনের স্ক্রিনের জন্য এসআইএল অপটিমাইজেশন",
                "ur" to "جدید ہائی ریزولوشن اسکرینز کے لیے خصوصی ترتیب",
                "in" to "Optimasi SIL OpenType untuk layar beresolusi tinggi",
                "tr" to "Yüksek çözünürlüklü ekranlar için SIL OpenType optimizasyonu",
                "fr" to "Optimisation SIL OpenType pour écrans haute résolution"
            ),
            mapOf(
                "en" to "Exceptional clarity across all font sizes",
                "bn" to "যেকোনো ফন্ট সাইজে চমৎকার স্পষ্টতা",
                "ur" to "ہر سائز میں بے مثال اور واضح پڑھائی",
                "in" to "Kejelasan luar biasa di semua ukuran teks",
                "tr" to "Tüm yazı boyutlarında olağanüstü netlik",
                "fr" to "Clarté exceptionnelle à toutes les tailles de police"
            )
        ),
        fontKey = "scheherazade"
    ),

    KITAB_QURAN(
        displayName = mapOf(
            "en" to "Kitab Clear Quran",
            "bn" to "কিতাব স্বচ্ছ কুরআন",
            "ur" to "کتاب واضح قرآن",
            "in" to "Al-Qur'an Jelas Kitab",
            "tr" to "Kitab Berrak Kur'an",
            "fr" to "Coran Clair Kitab"
        ),
        scriptFamily = mapOf(
            "en" to "High-Legibility Modern Quranic Naskh",
            "bn" to "উচ্চ স্পষ্টতার আধুনিক কুরআনিক নাসখ",
            "ur" to "انتہائی واضح جدید قرآنی نسخ",
            "in" to "Naskh Al-Qur'an Modern Keterbacaan Tinggi",
            "tr" to "Yüksek Okunaklı Modern Kur'an Neshi",
            "fr" to "Naskh Coranique Moderne Haute Lisibilité"
        ),
        originRegion = mapOf(
            "en" to "Modern Digital Quran Standard",
            "bn" to "আধুনিক ডিজিটাল স্বচ্ছ লিপি",
            "ur" to "جدید ڈیجیٹل قرآنی معیار",
            "in" to "Standar Al-Qur'an Digital Modern",
            "tr" to "Modern Dijital Kur'an Standardı",
            "fr" to "Standard Numérique Moderne du Coran"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "High contrast and wide letter spacing",
                "bn" to "উচ্চ কন্ট্রাস্ট ও আরামদায়ক হরফ দূরত্ব",
                "ur" to "واضح کنٹراسٹ اور کھلا فاصلہ",
                "in" to "Kontras tinggi dan jarak antar huruf yang lega",
                "tr" to "Yüksek kontrast ve ferah harf aralığı",
                "fr" to "Contraste élevé et espacement aéré des lettres"
            ),
            mapOf(
                "en" to "Distinct diacritic marks that never collide",
                "bn" to "স্বতন্ত্র হরকত যা পরস্পর কখনোই মিশে যায় না",
                "ur" to "الگ الگ اعراب جو باہم ٹکراتے نہیں",
                "in" to "Tanda harakat jelas yang tidak saling bertumpuk",
                "tr" to "Birbirine karışmayan belirgin hareke işaretleri",
                "fr" to "Signes diacritiques distincts sans chevauchement"
            ),
            mapOf(
                "en" to "Optimized specifically for mobile app reading",
                "bn" to "মোবাইল স্ক্রিনে পাঠের জন্য বিশেষভাবে অপ্টিমাইজড",
                "ur" to "موبائل اسکرین پر تلاوت کے لیے خصوصی طور پر تیار",
                "in" to "Dioptimalkan khusus untuk membaca di aplikasi seluler",
                "tr" to "Mobil uygulama okumaları için özel olarak uyarlanmıştır",
                "fr" to "Optimisé spécialement pour la lecture sur écran mobile"
            ),
            mapOf(
                "en" to "Modern, crisp vector strokes",
                "bn" to "আধুনিক ও নিখুঁত ভেক্টর স্ট্রোক",
                "ur" to "جدید اور شفاف ویکٹر خطوط",
                "in" to "Goresan vektor modern dan tajam",
                "tr" to "Modern ve keskin vektörel çizgiler",
                "fr" to "Traits vectoriels modernes et nets"
            )
        ),
        fontKey = "kitab"
    ),

    KFGQPC_WARSH(
        displayName = mapOf(
            "en" to "Warsh Mushaf (KFGQPC)",
            "bn" to "ওয়ারশ মুসহাফ (কিং ফাহাদ কমপ্লেক্স)",
            "ur" to "ورش مصحف (شاہ فہد کمپلیکس)",
            "in" to "Mushaf Warsh (KFGQPC)",
            "tr" to "Verş Mushafı (KFGQPC)",
            "fr" to "Mushaf Warsh (KFGQPC)"
        ),
        scriptFamily = mapOf(
            "en" to "Authentic Uthmanic Warsh an-Nafi'",
            "bn" to "উসমানী ওয়ারশ আন-নাফে'",
            "ur" to "مستند عثمانی ورش عن نافع",
            "in" to "Warsh an-Nafi' Utsmani Autentik",
            "tr" to "Özgün Osmanî Verş an-Nafi'",
            "fr" to "Warsh an-Nafi' Othmanique Authentique"
        ),
        originRegion = mapOf(
            "en" to "North Africa (Morocco, Algeria, Tunisia)",
            "bn" to "উত্তর আফ্রিকা ও মাগরিব",
            "ur" to "شمالی افریقہ (مراکش، الجزائر، تیونس)",
            "in" to "Afrika Utara (Maroko, Aljazair, Tunisia)",
            "tr" to "Kuzey Afrika (Fas, Cezayir, Tunus)",
            "fr" to "Afrique du Nord (Maroc, Algérie, Tunisie)"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "Official King Fahd Complex Warsh calligraphy",
                "bn" to "কিং ফাহাদ কমপ্লেক্সের অফিসিয়াল ওয়ারশ ক্যালিগ্রাফি",
                "ur" to "شاہ فہد کمپلیکس کی مستند ورش خطاطی",
                "in" to "Kaligrafi riwayat Warsh resmi Kompleks Raja Fahd",
                "tr" to "Resmî Kral Fehd Kompleksi Verş hat sanatı",
                "fr" to "Calligraphie officielle Warsh du Complexe Roi Fahd"
            ),
            mapOf(
                "en" to "Distinct Maghrebi dotting and vowel placements",
                "bn" to "মাগরিবি রীতির স্বতন্ত্র নুকতা ও হরকত বিন্যাস",
                "ur" to "مغربی انداز کے مخصوص نقطے اور اعراب",
                "in" to "Penempatan titik dan harakat khas tradisi Maghribi",
                "tr" to "Mağrib geleneğine özgü nokta ve hareke yerleşimi",
                "fr" to "Points et voyelles caractéristiques du style maghrébin"
            ),
            mapOf(
                "en" to "Traditional North African Mushaf aesthetic",
                "bn" to "উত্তর আফ্রিকার ঐতিহ্যবাহী মুসহাফ শৈলী",
                "ur" to "شمالی افریقہ کی روایتی مصحف کی خوبصورتی",
                "in" to "Estetika Mushaf tradisional Afrika Utara",
                "tr" to "Geleneksel Kuzey Afrika Mushaf estetiği",
                "fr" to "Esthétique traditionnelle des Mushafs d'Afrique du Nord"
            ),
            mapOf(
                "en" to "Full support for Riwayah Warsh vocalization",
                "bn" to "রিওয়ায়াতে ওয়ারশের পূর্ণাঙ্গ তিলাওয়াত চিহ্ন সমর্থন",
                "ur" to "روایتِ ورش کے تمام قرآنی رموز کی مکمل تائید",
                "in" to "Dukungan penuh untuk tanda baca riwayat Warsh",
                "tr" to "Verş rivayeti harekeleri için tam destek",
                "fr" to "Support complet de la vocalisation selon la Riwayah Warsh"
            )
        ),
        fontKey = "kfgqpc_warsh"
    ),

    KFGQPC_QALOUN(
        displayName = mapOf(
            "en" to "Qaloun Mushaf (KFGQPC)",
            "bn" to "কালূন মুসহাফ (কিং ফাহাদ কমপ্লেক্স)",
            "ur" to "قالون مصحف (شاہ فہد کمپلیکس)",
            "in" to "Mushaf Qalun (KFGQPC)",
            "tr" to "Kâlûn Mushafı (KFGQPC)",
            "fr" to "Mushaf Qaloun (KFGQPC)"
        ),
        scriptFamily = mapOf(
            "en" to "Authentic Uthmanic Qaloun an-Nafi'",
            "bn" to "উসমানী কালূন আন-নাফে'",
            "ur" to "مستند عثمانی قالون عن نافع",
            "in" to "Qalun an-Nafi' Utsmani Autentik",
            "tr" to "Özgün Osmanî Kâlûn an-Nafi'",
            "fr" to "Qaloun an-Nafi' Othmanique Authentique"
        ),
        originRegion = mapOf(
            "en" to "Libya, Tunisia & West Africa",
            "bn" to "লিবিয়া, তিউনিসিয়া ও পশ্চিম আফ্রিকা",
            "ur" to "لیبیا، تیونس اور مغربی افریقہ",
            "in" to "Libya, Tunisia & Afrika Barat",
            "tr" to "Libya, Tunus ve Batı Afrika",
            "fr" to "Libye, Tunisie & Afrique de l'Ouest"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "Official King Fahd Complex Qaloun calligraphy",
                "bn" to "কিং ফাহাদ কমপ্লেক্সের অফিসিয়াল কালূন ক্যালিগ্রাফি",
                "ur" to "شاہ فہد کمپلیکس کی مستند قالون خطاطی",
                "in" to "Kaligrafi riwayat Qalun resmi Kompleks Raja Fahd",
                "tr" to "Resmî Kral Fehd Kompleksi Kâlûn hat sanatı",
                "fr" to "Calligraphie officielle Qaloun du Complexe Roi Fahd"
            ),
            mapOf(
                "en" to "Distinct Qaloun recitation diacritical conventions",
                "bn" to "কালূন কিরাআতের স্বতন্ত্র হরকত ও চিহ্ন",
                "ur" to "قراءتِ قالون کے مخصوص اعراب اور علامات",
                "in" to "Konvensi diakritik bacaan riwayat Qalun yang khas",
                "tr" to "Kâlûn kıraatine özgü özel hareke kuralları",
                "fr" to "Conventions diacritiques spécifiques à la récitation Qaloun"
            ),
            mapOf(
                "en" to "High authenticity for Mediterranean/African readers",
                "bn" to "ভূমধ্যসাগরীয় ও আফ্রিকান পাঠকদের জন্য সর্বোচ্চ প্রামাণ্যতা",
                "ur" to "بحیرہ روم اور افریقی قارئین کے لیے مستند انتخاب",
                "in" to "Otentisitas tinggi bagi pembaca kawasan Afrika & Mediterania",
                "tr" to "Akdeniz ve Afrika okuyucuları için yüksek özgünlük",
                "fr" to "Haute authenticité pour les lecteurs méditerranéens et africains"
            ),
            mapOf(
                "en" to "Refined Quranic typographic rules",
                "bn" to "উন্নত ও মার্জিত কুরআনিক মুদ্রণরীতি",
                "ur" to "نہایت نکھری ہوئی قرآنی کتابت کے اصول",
                "in" to "Kaidah tipografi Al-Qur'an yang disempurnakan",
                "tr" to "Geliştirilmiş Kur'an tipografi kuralları",
                "fr" to "Règles typographiques coraniques raffinées"
            )
        ),
        fontKey = "kfgqpc_qaloun"
    ),

    MUSHAF_UNICODE(
        displayName = mapOf(
            "en" to "Modern Digital Naskh (Noto Naskh)",
            "bn" to "আধুনিক ডিজিটাল নাসখ (নোটো নাসখ)",
            "ur" to "جدید ڈیجیٹل نسخ (نوٹو نسخ)",
            "in" to "Naskh Digital Modern (Noto Naskh)",
            "tr" to "Modern Dijital Nesih (Noto Naskh)",
            "fr" to "Naskh Numérique Moderne (Noto Naskh)"
        ),
        scriptFamily = mapOf(
            "en" to "Standard Digital OpenType Naskh",
            "bn" to "স্ট্যান্ডার্ড ডিজিটাল ওপেনটাইপ নাসখ",
            "ur" to "معیاری ڈیجیٹل اوپن ٹائپ نسخ",
            "in" to "Naskh OpenType Digital Standar",
            "tr" to "Standart Dijital OpenType Nesih",
            "fr" to "Naskh Numérique OpenType Standard"
        ),
        originRegion = mapOf(
            "en" to "Universal Digital / Web & Mobile",
            "bn" to "সার্বজনীন ডিজিটাল স্ট্যান্ডার্ড",
            "ur" to "عالمگیر ڈیجیٹل / ویب اور موبائل",
            "in" to "Standar Digital Universal / Web & Seluler",
            "tr" to "Evrensel Dijital / Web ve Mobil",
            "fr" to "Standard Numérique Universel / Web & Mobile"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "Google Noto harmonized geometric proportions",
                "bn" to "গুগল নোটোর জ্যামিতিক অনুপাতের সামঞ্জস্যপূর্ণ বিন্যাস",
                "ur" to "گوگل نوٹو کا متناسب اور جدید ہندسی انداز",
                "in" to "Proporsi geometris harmonis khas Google Noto",
                "tr" to "Google Noto uyumlu geometrik oranlar",
                "fr" to "Proportions géométriques harmonisées de Google Noto"
            ),
            mapOf(
                "en" to "Clean, uniform stroke weights on all screens",
                "bn" to "সকল স্ক্রিনে সুষম ও স্পষ্ট স্ট্রোক",
                "ur" to "تمام اسکرینز پر یکساں اور خوبصورت لکیریں",
                "in" to "Ketebalan garis yang bersih dan seragam di semua layar",
                "tr" to "Tüm ekranlarda temiz ve dengeli çizgi kalınlıkları",
                "fr" to "Épaisseur de trait uniforme et nette sur tous les écrans"
            ),
            mapOf(
                "en" to "Full cross-platform HarfBuzz rendering",
                "bn" to "ক্রস-প্ল্যাটফর্ম হার্ফবাজ রেন্ডারিং সমর্থন",
                "ur" to "جدید ہارربز رینڈرنگ کی مکمل سہولت",
                "in" to "Rendering HarfBuzz lintas platform yang mulus",
                "tr" to "Eksiksiz platformlar arası HarfBuzz görselleştirme",
                "fr" to "Rendu HarfBuzz multiplateforme complet"
            ),
            mapOf(
                "en" to "Ultra-sharp readability on compact devices",
                "bn" to "ছোট স্ক্রিনেও চমৎকার ও ঝকঝকে পাঠযোগ্যতা",
                "ur" to "چھوٹے موبائل آلات پر انتہائی واضح پڑھائی",
                "in" to "Keterbacaan sangat tajam pada perangkat kecil",
                "tr" to "Küçük cihazlarda ultra net okunabilirlik",
                "fr" to "Lisibilité ultra-nette sur appareils compacts"
            )
        ),
        fontKey = "noto_naskh"
    ),

    NOTO_NASTALEEQ(
        displayName = mapOf(
            "en" to "Noto Nastaliq (Urdu / Persian)",
            "bn" to "নোটো নাসতালীক (উর্দু / ফার্সি)",
            "ur" to "نوٹو نستعلیق (اردو / فارسی)",
            "in" to "Noto Nastaliq (Urdu / Persia)",
            "tr" to "Noto Nesitalik (Urduca / Farsça)",
            "fr" to "Noto Nastaliq (Ourdou / Persan)"
        ),
        scriptFamily = mapOf(
            "en" to "Flowing Persian & Urdu Nastaliq",
            "bn" to "প্রবাহিত ফার্সি ও উর্দু নাসতালীক",
            "ur" to "خوش خط فارسی و اردو نستعلیق",
            "in" to "Nastaliq Mengalir Urdu & Persia",
            "tr" to "Akıcı Farsça ve Urduca Nesitalik",
            "fr" to "Nastaliq Fluide Persan & Ourdou"
        ),
        originRegion = mapOf(
            "en" to "Pakistan, India, Iran, Afghanistan",
            "bn" to "পাকিস্তান, ভারত ও ইরান",
            "ur" to "پاکستان، بھارت، ایران، افغانستان",
            "in" to "Pakistan, India, Iran, Afghanistan",
            "tr" to "Pakistan, Hindistan, İran, Afganistan",
            "fr" to "Pakistan, Inde, Iran, Afghanistan"
        ),
        keyFeatures = listOf(
            mapOf(
                "en" to "Elegant diagonal flowing calligraphic script",
                "bn" to "নান্দনিক তির্যক প্রবাহমান ক্যালিগ্রাফিক লিপি",
                "ur" to "خوبصورت ترچھی روانی والا خطاطی انداز",
                "in" to "Aksara kaligrafi mengalir diagonal yang elegan",
                "tr" to "Zarif çapraz akıcı hat sanatı",
                "fr" to "Écriture calligraphique fluide en diagonale élégante"
            ),
            mapOf(
                "en" to "Beloved by Urdu, Persian, and Punjabi readers",
                "bn" to "উর্দু, ফার্সি ও পাঞ্জাবি পাঠকদের অত্যন্ত প্রিয়",
                "ur" to "اردو، فارسی اور پنجابی قارئین کا پسندیدہ",
                "in" to "Disukai oleh pembaca bahasa Urdu, Persia, dan Punjabi",
                "tr" to "Urduca, Farsça ve Pencapça okuyucularının gözdesi",
                "fr" to "Très apprécié des lecteurs en ourdou, persan et pendjabi"
            ),
            mapOf(
                "en" to "Broad stroke modulation and poetic curves",
                "bn" to "বিস্তৃত স্ট্রোক ও কাব্যিক বক্ররেখার সংমিশ্রণ",
                "ur" to "شاعرانہ گھماؤ اور لکیروں کی خوبصورت کشش",
                "in" to "Modulasi goresan luas dan lekukan puitis",
                "tr" to "Geniş çizgi modülasyonu ve şiirsel kıvrımlar",
                "fr" to "Modulation large des traits et courbes poétiques"
            ),
            mapOf(
                "en" to "Traditional subcontinental translation typography",
                "bn" to "উপমহাদেশীয় অনুবাদগ্রন্থের ঐতিহ্যবাহী মুদ্রণশৈলী",
                "ur" to "برصغیر کے تراجم کی روایتی کتابت",
                "in" to "Tipografi terjemahan tradisional kawasan anak benua",
                "tr" to "Geleneksel alt kıta çeviri tipografisi",
                "fr" to "Typographie traditionnelle des traductions du sous-continent"
            )
        ),
        fontKey = "noto_nastaliq_urdu"
    );

    companion object {
        val DEFAULT = MADANI_KFGQPC

        fun fromName(name: String?): QuranFontStyle = entries.find { it.name == name } ?: when (name) {
            "UTHMANIC_HAFS", "CLASSIC_MADANI_1405", "MADANI_TAJWEED", "SHEMERLY_AMIRIYA", "UTHMANI_AMIRI" -> MADANI_KFGQPC
            "NURANI_HAFEZI", "INDOPAK" -> INDOPAK_NASKH
            "MODERN_MADANI_1440", "SCHEHERAZADE" -> SCHEHERAZADE_NASKH
            "MUSHAF_WARSH" -> KFGQPC_WARSH
            "MUSHAF_QALOON" -> KFGQPC_QALOUN
            "NOTO_NASKH" -> MUSHAF_UNICODE
            "AMIRI" -> AMIRI_CLASSIC
            "KITAB" -> KITAB_QURAN
            else -> DEFAULT
        }
    }
}
