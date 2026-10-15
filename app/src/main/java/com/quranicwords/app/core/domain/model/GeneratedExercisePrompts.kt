package com.quranicwords.app.core.domain.model

/**
 * Prompts for exercises the app generates itself (Open Practice quizzes built in
 * `ProgressRepositoryImpl.getOpenPracticeExercises`) rather than reads from authored content.
 * They're stored inside the generated [ExerciseContent] as [LocalizedText] - the same shape as
 * every authored prompt - so the lesson UI renders both identically; this object is the single
 * place those texts live.
 */
object GeneratedExercisePrompts {
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

    val MATCHING_PROMPT = mapOf(
        "en" to "Match each word to its meaning",
        "bn" to "প্রতিটি শব্দকে এর অর্থের সাথে মেলান",
        "ur" to "ہر لفظ کو اس کے معنی سے ملائیں",
        "hi" to "प्रत्येक शब्द को उसके अर्थ से मिलाएँ",
        "in" to "Cocokkan setiap kata dengan artinya",
        "tr" to "Her kelimeyi anlamıyla eşleştirin",
        "fa" to "هر کلمه را با معنای آن تطبیق دهید",
        "fr" to "Associez chaque mot à sa signification"
    )
}
