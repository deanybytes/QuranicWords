package com.quranicwords.app.feature.widget

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.LevelProgress
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.domain.srs.WordStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Inflates and applies every widget layout (3 kinds x 4 sizes x 3 themes) the way a launcher
 * does, so an unsupported view, a non-remotable setter or a missing id fails here instead of
 * showing "Can't load widget" on a home screen.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val stats = WidgetStats(
        streakDays = 12, practicedToday = true, todayMinutes = 7, goalMinutes = 10, dueCount = 8,
        learnedCount = 64, level = LevelProgress(4, 120, 300), hearts = 4, maxHearts = 5,
        questsDone = 2, questsTotal = 3
    )

    private val dueWord = WidgetWord(
        id = "wf_1", arabic = "كِتَٰب", meaning = "Book", kind = WordKind.DUE, strength = WordStrength.FAMILIAR,
        category = LemmaCategory.NOUN, occurrences = 230, rank = 41,
        example = WidgetExample("ذَٰلِكَ الْكِتَابُ", "That is the Book", "Al-Baqarah 2:2")
    )

    private fun snapshot(language: Language = Language.ENGLISH, theme: ThemeMode = ThemeMode.SYSTEM, ready: Boolean = true) =
        WidgetSnapshot(
            environment = WidgetEnvironment(language = language, themeMode = theme),
            contentReady = ready,
            stats = if (ready) stats else WidgetStats.EMPTY,
            pools = WordPools(due = listOf("wf_1", "wf_2"), learned = emptyList(), nextNew = null)
        )

    private fun apply(kind: WidgetKind, size: WidgetSizeClass, snapshot: WidgetSnapshot, word: WidgetWord?, revealed: String? = null): View {
        val binding = WidgetRenderer.Binding(context, kind, 7, snapshot, word, wordBitmap = null, revealedWordId = revealed)
        return binding.build(size).apply(context, FrameLayout(context))
    }

    @Test
    fun `every layout inflates and binds in every theme`() {
        for (kind in WidgetKind.entries) for (size in WidgetSizeClass.entries) for (theme in ThemeMode.entries) {
            val word = if (kind == WidgetKind.STATS) null else dueWord
            val root = apply(kind, size, snapshot(theme = theme), word)
            assertEquals("$kind/$size/$theme content", View.VISIBLE, root.findViewById<View>(R.id.ll_content).visibility)
        }
    }

    @Test
    fun `not-ready content shows the friendly empty state with an open-app action`() {
        for (kind in WidgetKind.entries) for (size in WidgetSizeClass.entries) {
            val root = apply(kind, size, snapshot(ready = false), word = null)
            assertEquals("$kind/$size", View.VISIBLE, root.findViewById<View>(R.id.ll_empty).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.ll_content).visibility)
            assertEquals(context.getString(R.string.widget_open_app), root.findViewById<TextView>(R.id.tv_empty_cta).text.toString())
        }
    }

    @Test
    fun `a due word hides its meaning until revealed`() {
        val hidden = apply(WidgetKind.WORD, WidgetSizeClass.MEDIUM, snapshot(), dueWord)
        assertEquals(View.GONE, hidden.findViewById<View>(R.id.tv_meaning).visibility)
        assertEquals(View.VISIBLE, hidden.findViewById<View>(R.id.tv_reveal).visibility)

        val shown = apply(WidgetKind.WORD, WidgetSizeClass.MEDIUM, snapshot(), dueWord, revealed = "wf_1")
        assertEquals(View.VISIBLE, shown.findViewById<View>(R.id.tv_meaning).visibility)
        assertEquals("Book", shown.findViewById<TextView>(R.id.tv_meaning).text.toString())
    }

    @Test
    fun `strings and digits follow the app language and RTL languages mirror`() {
        val root = apply(WidgetKind.STATS, WidgetSizeClass.LARGE, snapshot(language = Language.URDU), word = null)
        assertEquals("۱۲", root.findViewById<TextView>(R.id.tv_streak).text.toString())
        assertEquals("۸", root.findViewById<TextView>(R.id.tv_due_value).text.toString())
        assertEquals(View.LAYOUT_DIRECTION_RTL, root.layoutDirection)

        val bn = apply(WidgetKind.STATS, WidgetSizeClass.MEDIUM, snapshot(language = Language.BANGLA), word = null)
        assertTrue(bn.findViewById<TextView>(R.id.tv_cta).text.toString().contains("৮"))
    }

    @Test
    fun `the review call to action names the due count`() {
        val root = apply(WidgetKind.STATS, WidgetSizeClass.SMALL, snapshot(), word = null)
        assertEquals(context.getString(R.string.widget_review_count, "8"), root.findViewById<TextView>(R.id.tv_cta).text.toString())
    }

    @Test
    fun `the large word widget shows one localized citation and a single translation`() {
        val root = apply(WidgetKind.WORD, WidgetSizeClass.LARGE, snapshot(language = Language.BANGLA), dueWord, revealed = "wf_1")
        val ref = root.findViewById<TextView>(R.id.tv_example_ref).text.toString()
        assertTrue(ref, ref.endsWith("২:২"))
        assertTrue(ref, !ref.contains("Baqarah"))
        assertEquals("That is the Book", root.findViewById<TextView>(R.id.tv_example_translation).text.toString())
    }

    @Test
    fun `the Arabic word renders in the chosen Qur'anic font`() {
        val bitmap = ArabicWordRenderer.render(context, "كِتَٰب", WidgetEnvironment().fontStyle)
        assertNotNull(bitmap)
        val binding = WidgetRenderer.Binding(context, WidgetKind.WORD, 7, snapshot(), dueWord, bitmap, null)
        val root = binding.build(WidgetSizeClass.MEDIUM).apply(context, FrameLayout(context))
        assertEquals(View.VISIBLE, root.findViewById<ImageView>(R.id.iv_arabic).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.tv_arabic).visibility)
        assertEquals("كِتَٰب", root.findViewById<ImageView>(R.id.iv_arabic).contentDescription.toString())
    }
}
