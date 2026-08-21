# -*- coding: utf-8 -*-
"""
Shared AI-drafted translations of this pipeline's 3 fixed exercise-prompt phrases, for the 10
languages added beyond the original en/bn. Not sourced from quran.gtaf.org (which only provides
Quran word data, not app UI phrases) or any other corpus - flagged as unreviewed, same discipline
as meaningBn originally was, via each WordIntro's meaningReviewed map staying false for these
languages. Used by both 13_translate_content_12lang.py (patches already-emitted content in place)
and 07_emit_content.py (for regenerating content from scratch).
"""

TEACH_PROMPT = {
    "en": "Meet a new word",
    "bn": "একটি নতুন শব্দ চিনুন",
    "sq": "Njihuni me një fjalë të re",
    "zh": "认识一个新单词",
    "fa": "با یک کلمه جدید آشنا شوید",
    "fr": "Découvrez un nouveau mot",
    "de": "Lerne ein neues Wort kennen",
    "hi": "एक नया शब्द सीखें",
    "in": "Kenali kata baru",
    "ru": "Познакомьтесь с новым словом",
    "tr": "Yeni bir kelimeyle tanışın",
    "ur": "ایک نئے لفظ سے واقفیت حاصل کریں",
}

QUIZ_PROMPT = {
    "en": "What does this word mean?",
    "bn": "এই শব্দের অর্থ কী?",
    "sq": "Çfarë do të thotë kjo fjalë?",
    "zh": "这个词是什么意思？",
    "fa": "این کلمه به چه معناست؟",
    "fr": "Que signifie ce mot ?",
    "de": "Was bedeutet dieses Wort?",
    "hi": "इस शब्द का क्या अर्थ है?",
    "in": "Apa arti kata ini?",
    "ru": "Что означает это слово?",
    "tr": "Bu kelime ne anlama geliyor?",
    "ur": "اس لفظ کا کیا مطلب ہے؟",
}

MATCH_PROMPT = {
    "en": "Match each word to its meaning.",
    "bn": "প্রতিটি শব্দকে এর অর্থের সাথে মেলান।",
    "sq": "Përputhni çdo fjalë me kuptimin e saj.",
    "zh": "将每个单词与其含义匹配。",
    "fa": "هر کلمه را با معنای آن تطبیق دهید.",
    "fr": "Associez chaque mot à sa signification.",
    "de": "Ordne jedes Wort seiner Bedeutung zu.",
    "hi": "प्रत्येक शब्द को उसके अर्थ से मिलाएँ।",
    "in": "Cocokkan setiap kata dengan artinya.",
    "ru": "Сопоставьте каждое слово с его значением.",
    "tr": "Her kelimeyi anlamıyla eşleştirin.",
    "ur": "ہر لفظ کو اس کے معنی سے ملائیں۔",
}

NEW_LANGUAGES = ["sq", "zh", "fa", "fr", "de", "hi", "in", "ru", "tr", "ur"]
ALL_LANGUAGES = ["en", "bn"] + NEW_LANGUAGES
