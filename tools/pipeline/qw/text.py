"""Localized template text for the 8 content languages (titles, prompts, labels)."""
from . import config

DIGITS = {
    "bn": "০১২৩৪৫৬৭৮৯", "ur": "۰۱۲۳۴۵۶۷۸۹", "fa": "۰۱۲۳۴۵۶۷۸۹", "hi": "०१२३४५६७८९",
}


def digits(n, lang):
    s = str(n)
    table = DIGITS.get(lang)
    return "".join(table[int(c)] if table and c.isdigit() else c for c in s)


def loc(templates, **kw):
    """templates: {lang: "…{n}…"} -> LocalizedText with localized digits for numeric args."""
    out = {}
    for lang in config.LANGS:
        args = {k: (digits(v, lang) if isinstance(v, int) else (v[lang] if isinstance(v, dict) else v))
                for k, v in kw.items()}
        out[lang] = templates[lang].format(**args)
    return out


LESSON = {"en": "Lesson {n}: {a} – {b}", "bn": "পাঠ {n}: {a} – {b}", "ur": "سبق {n}: {a} – {b}",
          "hi": "पाठ {n}: {a} – {b}", "in": "Pelajaran {n}: {a} – {b}", "tr": "Ders {n}: {a} – {b}",
          "fa": "درس {n}: {a} – {b}", "fr": "Leçon {n} : {a} – {b}"}
SECTION = {"en": "Section {n}: {t}", "bn": "পর্ব {n}: {t}", "ur": "حصہ {n}: {t}", "hi": "खंड {n}: {t}",
           "in": "Bagian {n}: {t}", "tr": "Bölüm {n}: {t}", "fa": "بخش {n}: {t}", "fr": "Section {n} : {t}"}
FLASHBACK = {"en": "Section {n} Review", "bn": "পর্ব {n} পুনরাবৃত্তি", "ur": "حصہ {n} کا اعادہ",
             "hi": "खंड {n} पुनरावृत्ति", "in": "Ulasan Bagian {n}", "tr": "Bölüm {n} Tekrarı",
             "fa": "مرور بخش {n}", "fr": "Révision de la section {n}"}
SECTION_EXAM = {"en": "Section {n} Exam", "bn": "পর্ব {n} পরীক্ষা", "ur": "حصہ {n} کا امتحان",
                "hi": "खंड {n} परीक्षा", "in": "Ujian Bagian {n}", "tr": "Bölüm {n} Sınavı",
                "fa": "آزمون بخش {n}", "fr": "Examen de la section {n}"}
CHAPTER_INTRO = {"en": "Chapter {n} Overview", "bn": "অধ্যায় {n} পরিচিতি", "ur": "باب {n} کا تعارف",
                 "hi": "अध्याय {n} परिचय", "in": "Ikhtisar Bab {n}", "tr": "Bölüm {n} Tanıtımı",
                 "fa": "معرفی فصل {n}", "fr": "Aperçu du chapitre {n}"}
CHAPTER_EXAM = {"en": "Chapter {n} Exam", "bn": "অধ্যায় {n} পরীক্ষা", "ur": "باب {n} کا امتحان",
                "hi": "अध्याय {n} परीक्षा", "in": "Ujian Bab {n}", "tr": "Bölüm {n} Sınavı (Genel)",
                "fa": "آزمون فصل {n}", "fr": "Examen du chapitre {n}"}

PROMPT_INTRO = {"en": "Learn this word", "bn": "শব্দটি শিখুন", "ur": "یہ لفظ سیکھیں", "hi": "यह शब्द सीखें",
                "in": "Pelajari kata ini", "tr": "Bu kelimeyi öğrenin", "fa": "این واژه را بیاموزید",
                "fr": "Apprenez ce mot"}
PROMPT_MC = {"en": "Choose the correct meaning", "bn": "সঠিক অর্থ নির্বাচন করুন", "ur": "درست معنی کا انتخاب کریں",
             "hi": "सही अर्थ चुनें", "in": "Pilih arti yang benar", "tr": "Doğru anlamı seçin",
             "fa": "معنی درست را انتخاب کنید", "fr": "Choisissez la bonne signification"}
PROMPT_MATCH = {"en": "Match each word to its meaning", "bn": "প্রতিটি শব্দকে এর অর্থের সাথে মেলান",
                "ur": "ہر لفظ کو اس کے معنی سے ملائیں", "hi": "प्रत्येक शब्द को उसके अर्थ से मिलाएँ",
                "in": "Cocokkan setiap kata dengan artinya", "tr": "Her kelimeyi anlamıyla eşleştirin",
                "fa": "هر کلمه را با معنای آن تطبیق دهید", "fr": "Associez chaque mot à sa signification"}
PROMPT_FILL = {"en": "Complete the verse", "bn": "আয়াতটি সম্পূর্ণ করুন", "ur": "آیت مکمل کریں",
               "hi": "आयत पूरी करें", "in": "Lengkapi ayat berikut", "tr": "Ayeti tamamlayın",
               "fa": "آیه را کامل کنید", "fr": "Complétez le verset"}
PROMPT_TAP = {"en": "Tap the word that means this", "bn": "এই অর্থের শব্দটি স্পর্শ করুন",
              "ur": "اس معنی والا لفظ منتخب کریں", "hi": "इस अर्थ वाले शब्द को छुएँ",
              "in": "Ketuk kata yang berarti ini", "tr": "Bu anlama gelen kelimeye dokunun",
              "fa": "کلمه‌ای را که این معنا را دارد لمس کنید", "fr": "Touchez le mot qui a ce sens"}
PROMPT_LISTEN = {"en": "Tap what you hear", "bn": "যা শুনছেন তা স্পর্শ করুন", "ur": "جو سنیں اسے منتخب کریں",
                 "hi": "जो सुनें उसे छुएँ", "in": "Ketuk yang Anda dengar", "tr": "Duyduğunuza dokunun",
                 "fa": "آنچه می‌شنوید را لمس کنید", "fr": "Touchez ce que vous entendez"}

CHAPTER_TITLES = [
    {"en": "Particles & Function Words", "bn": "অব্যয় ও কার্যবাচক শব্দ", "ur": "حروف اور معاون الفاظ",
     "hi": "अव्यय और सहायक शब्द", "in": "Partikel & Kata Tugas", "tr": "Edatlar ve İşlev Kelimeleri",
     "fa": "حروف و واژه‌های دستوری", "fr": "Particules et mots-outils"},
    {"en": "Essential Verbs", "bn": "মৌলিক ক্রিয়া", "ur": "بنیادی افعال", "hi": "मूल क्रियाएँ",
     "in": "Kata Kerja Inti", "tr": "Temel Fiiller", "fa": "افعال بنیادین", "fr": "Verbes essentiels"},
    {"en": "Essential Nouns", "bn": "মৌলিক বিশেষ্য", "ur": "بنیادی اسماء", "hi": "मूल संज्ञाएँ",
     "in": "Kata Benda Inti", "tr": "Temel İsimler", "fa": "اسم‌های بنیادین", "fr": "Noms essentiels"},
    {"en": "Common Verbs", "bn": "প্রচলিত ক্রিয়া", "ur": "عام افعال", "hi": "सामान्य क्रियाएँ",
     "in": "Kata Kerja Umum", "tr": "Yaygın Fiiller", "fa": "افعال رایج", "fr": "Verbes courants"},
    {"en": "Common Nouns", "bn": "প্রচলিত বিশেষ্য", "ur": "عام اسماء", "hi": "सामान्य संज्ञाएँ",
     "in": "Kata Benda Umum", "tr": "Yaygın İsimler", "fa": "اسم‌های رایج", "fr": "Noms courants"},
    {"en": "Frequent Verbs", "bn": "বহুল ব্যবহৃত ক্রিয়া", "ur": "کثیر الاستعمال افعال", "hi": "प्रचलित क्रियाएँ",
     "in": "Kata Kerja Sering", "tr": "Sık Fiiller", "fa": "افعال پرکاربرد", "fr": "Verbes fréquents"},
    {"en": "Frequent Nouns", "bn": "বহুল ব্যবহৃত বিশেষ্য", "ur": "کثیر الاستعمال اسماء", "hi": "प्रचलित संज्ञाएँ",
     "in": "Kata Benda Sering", "tr": "Sık İsimler", "fa": "اسم‌های پرکاربرد", "fr": "Noms fréquents"},
    {"en": "Further Verbs", "bn": "আরও ক্রিয়া", "ur": "مزید افعال", "hi": "और क्रियाएँ",
     "in": "Kata Kerja Lanjutan", "tr": "İleri Fiiller", "fa": "افعال بیشتر", "fr": "Autres verbes"},
    {"en": "Further Nouns", "bn": "আরও বিশেষ্য", "ur": "مزید اسماء", "hi": "और संज्ञाएँ",
     "in": "Kata Benda Lanjutan", "tr": "İleri İsimler", "fa": "اسم‌های بیشتر", "fr": "Autres noms"},
    {"en": "Rare & Unique Nouns", "bn": "দুর্লভ ও অনন্য বিশেষ্য", "ur": "نادر اور منفرد اسماء",
     "hi": "दुर्लभ और अनोखी संज्ञाएँ", "in": "Kata Benda Langka & Unik", "tr": "Nadir ve Eşsiz İsimler",
     "fa": "اسم‌های کمیاب و یگانه", "fr": "Noms rares et uniques"},
]
CHAPTER_DESCRIPTIONS = [
    {"en": "The small words that hold every verse together - prepositions, conjunctions, pronouns and particles. Learn these first: they make up a huge share of the Qur'an.",
     "bn": "যে ছোট শব্দগুলো প্রতিটি আয়াতকে জুড়ে রাখে - অব্যয়, সংযোজক, সর্বনাম। এগুলো আগে শিখুন: কুরআনের বিশাল অংশ এগুলো দিয়েই।",
     "ur": "وہ چھوٹے الفاظ جو ہر آیت کو جوڑتے ہیں - حروفِ جار، حروفِ عطف، ضمائر۔ انہیں پہلے سیکھیں: قرآن کا بڑا حصہ انہی پر مشتمل ہے۔",
     "hi": "वे छोटे शब्द जो हर आयत को जोड़ते हैं - संबंधबोधक, समुच्चयबोधक, सर्वनाम। इन्हें पहले सीखें: क़ुरआन का बड़ा हिस्सा इन्हीं से बना है।",
     "in": "Kata-kata kecil yang merangkai setiap ayat - preposisi, konjungsi, kata ganti. Pelajari lebih dulu: jumlahnya sangat besar dalam Al-Qur'an.",
     "tr": "Her ayeti birbirine bağlayan küçük kelimeler - edatlar, bağlaçlar, zamirler. Önce bunları öğrenin: Kur'an'ın büyük bir kısmını oluştururlar.",
     "fa": "واژه‌های کوچکی که هر آیه را به هم پیوند می‌دهند - حروف اضافه، ربط و ضمایر. اول این‌ها را بیاموزید: بخش بزرگی از قرآن را می‌سازند.",
     "fr": "Les petits mots qui relient chaque verset - prépositions, conjonctions, pronoms. Apprenez-les d'abord : ils forment une grande part du Coran."},
] + [None] * 9
VERB_DESC = {"en": "Qur'anic verbs, taught in order of how often they occur.",
             "bn": "কুরআনের ক্রিয়াপদ, ব্যবহারের পরিমাণ অনুযায়ী ক্রমানুসারে।",
             "ur": "قرآنی افعال، ان کے استعمال کی کثرت کی ترتیب سے۔",
             "hi": "क़ुरआनी क्रियाएँ, उनकी आवृत्ति के क्रम में।",
             "in": "Kata kerja Al-Qur'an, diajarkan menurut seberapa sering muncul.",
             "tr": "Kur'an fiilleri, geçme sıklıklarına göre sıralı.",
             "fa": "افعال قرآنی، به ترتیب بسامد کاربرد.",
             "fr": "Les verbes du Coran, enseignés par ordre de fréquence."}
NOUN_DESC = {"en": "Qur'anic nouns and adjectives, taught in order of how often they occur.",
             "bn": "কুরআনের বিশেষ্য ও বিশেষণ, ব্যবহারের পরিমাণ অনুযায়ী ক্রমানুসারে।",
             "ur": "قرآنی اسماء و صفات، ان کے استعمال کی کثرت کی ترتیب سے۔",
             "hi": "क़ुरआनी संज्ञाएँ और विशेषण, उनकी आवृत्ति के क्रम में।",
             "in": "Kata benda dan sifat Al-Qur'an, diajarkan menurut seberapa sering muncul.",
             "tr": "Kur'an isim ve sıfatları, geçme sıklıklarına göre sıralı.",
             "fa": "اسم‌ها و صفت‌های قرآنی، به ترتیب بسامد کاربرد.",
             "fr": "Les noms et adjectifs du Coran, enseignés par ordre de fréquence."}
