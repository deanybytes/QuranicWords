package com.quranicwords.app.core.domain.model

/**
 * Prompts for exercises the app generates itself (Open Practice verse quizzes built in
 * `ProgressRepositoryImpl.getOpenPracticeExercises`) rather than reads from authored content.
 * They're stored inside the generated [ExerciseContent] as [LocalizedText] - the same shape as
 * every authored prompt - so the lesson UI renders both identically; this object is the single
 * place those texts live.
 */
object GeneratedExercisePrompts {
    val TAP_WORD_PROMPT = mapOf(
        "en" to "Tap the Arabic word in the verse",
        "bn" to "আয়াত থেকে সঠিক আরবি শব্দটি স্পর্শ করুন",
        "ur" to "آیت میں سے درست عربی لفظ منتخب کریں",
        "hi" to "आयत में से सही अरबी शब्द चुनें",
        "in" to "Ketuk kata Arab yang benar dalam ayat",
        "tr" to "Ayetteki doğru Arapça kelimeye dokunun",
        "fa" to "کلمه عربی درست را در آیه لمس کنید",
        "fr" to "Touchez le mot arabe correct dans le verset"
    )

    val FILL_BLANK_PROMPT = mapOf(
        "en" to "Complete the verse",
        "bn" to "আয়াতটি সম্পূর্ণ করুন",
        "ur" to "آیت مکمل کریں",
        "hi" to "आयत पूरी करें",
        "in" to "Lengkapi ayat berikut",
        "tr" to "Ayeti tamamlayın",
        "fa" to "آیه را کامل کنید",
        "fr" to "Complétez le verset"
    )

    val MULTIPLE_CHOICE_PROMPT = mapOf(
        "en" to "Choose the correct meaning",
        "bn" to "সঠিক অর্থ নির্বাচন করুন",
        "ur" to "درست معنی کا انتخاب کریں",
        "hi" to "सही अर्थ चुनें",
        "in" to "Pilih arti yang benar",
        "tr" to "Doğru anlamı seçin",
        "fa" to "معنی درست را انتخاب کنید",
        "fr" to "Choisissez la bonne signification"
    )
}
