package com.quranicwords.app.feature.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.srs.WordStrength
import com.quranicwords.app.core.util.VerseReferenceFormatter

/** Builds and pushes the RemoteViews for one widget instance. */
object WidgetRenderer {

    @LayoutRes
    fun layoutFor(kind: WidgetKind, size: WidgetSizeClass): Int = when (kind) {
        WidgetKind.WORD -> when (size) {
            WidgetSizeClass.COMPACT -> R.layout.widget_word_compact
            WidgetSizeClass.SMALL -> R.layout.widget_word_small
            WidgetSizeClass.MEDIUM -> R.layout.widget_word_medium
            WidgetSizeClass.LARGE -> R.layout.widget_word_large
        }
        WidgetKind.STATS -> when (size) {
            WidgetSizeClass.COMPACT -> R.layout.widget_stats_compact
            WidgetSizeClass.SMALL -> R.layout.widget_stats_small
            WidgetSizeClass.MEDIUM -> R.layout.widget_stats_medium
            WidgetSizeClass.LARGE -> R.layout.widget_stats_large
        }
        WidgetKind.COMBINED -> when (size) {
            WidgetSizeClass.COMPACT -> R.layout.widget_combined_compact
            WidgetSizeClass.SMALL -> R.layout.widget_combined_small
            WidgetSizeClass.MEDIUM -> R.layout.widget_combined_medium
            WidgetSizeClass.LARGE -> R.layout.widget_combined_large
        }
    }

    /** Pre-API 31 size: portrait width (min) x portrait height (max), the launcher convention. */
    fun sizeClassFromOptions(options: Bundle?, kind: WidgetKind): WidgetSizeClass {
        val width = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
        val height = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0
        return WidgetSizing.bestFit(width.toFloat(), height.toFloat(), WidgetSizing.breakpoints(kind))
    }

    fun push(
        context: Context,
        manager: AppWidgetManager,
        kind: WidgetKind,
        widgetId: Int,
        snapshot: WidgetSnapshot,
        word: WidgetWord?,
        revealedWordId: String?
    ) {
        val bitmap = word?.let { ArabicWordRenderer.render(context, it.arabic, snapshot.environment.fontStyle) }
        val binding = Binding(context, kind, widgetId, snapshot, word, bitmap, revealedWordId)
        val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RemoteViews(
                WidgetSizing.breakpoints(kind).associate { bp -> SizeF(bp.widthDp, bp.heightDp) to binding.build(bp.sizeClass) }
            )
        } else {
            binding.build(sizeClassFromOptions(manager.getAppWidgetOptions(widgetId), kind))
        }
        manager.updateAppWidget(widgetId, views)
    }

    /** One widget's data bound into any of its size variants. */
    class Binding(
        private val context: Context,
        private val kind: WidgetKind,
        private val widgetId: Int,
        private val snapshot: WidgetSnapshot,
        private val word: WidgetWord?,
        private val wordBitmap: Bitmap?,
        private val revealedWordId: String?
    ) {
        private val language: Language = snapshot.environment.language
        private val res: Context = context.getThemedAndLocalizedWidgetContext(language, snapshot.environment.themeMode)
        private val paint = WidgetPaint.forMode(context, snapshot.environment.themeMode)

        private fun num(value: Int) = WidgetFormat.number(value, language)

        fun build(size: WidgetSizeClass): RemoteViews {
            val v = RemoteViews(context.packageName, layoutFor(kind, size))
            // The launcher lays the widget out in *its* locale; mirror for the app's language.
            v.setInt(
                android.R.id.background,
                "setLayoutDirection",
                if (language.isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            )
            paint.applyFixedPalette(v)

            if (!snapshot.contentReady || (kind == WidgetKind.WORD && word == null)) {
                bindEmpty(v)
                return v
            }
            v.setViewVisibility(R.id.ll_empty, View.GONE)
            v.setViewVisibility(R.id.ll_content, View.VISIBLE)

            val openHome = WidgetDeepLink.pendingIntent(context, widgetId, "root", WidgetTarget(WidgetDestination.HOME))
            v.setOnClickPendingIntent(android.R.id.background, openHome)

            when (kind) {
                WidgetKind.WORD -> bindWord(v, requireNotNull(word), withCta = true)
                WidgetKind.STATS -> bindStats(v)
                WidgetKind.COMBINED -> {
                    bindStats(v)
                    if (word != null) {
                        bindWord(v, word, withCta = false)
                    } else {
                        v.setViewVisibility(R.id.ll_word, View.GONE)
                    }
                }
            }
            return v
        }

        private fun bindEmpty(v: RemoteViews) {
            v.setViewVisibility(R.id.ll_content, View.GONE)
            v.setViewVisibility(R.id.ll_empty, View.VISIBLE)
            v.setTextViewText(R.id.tv_empty_title, res.getString(R.string.widget_not_ready_title))
            v.setTextViewText(R.id.tv_empty_desc, res.getString(R.string.widget_not_ready_desc))
            v.setTextViewText(R.id.tv_empty_cta, res.getString(R.string.widget_open_app))
            val open = WidgetDeepLink.pendingIntent(context, widgetId, "root", WidgetTarget(WidgetDestination.HOME))
            v.setOnClickPendingIntent(android.R.id.background, open)
            v.setOnClickPendingIntent(R.id.tv_empty_cta, open)
        }

        private fun bindWord(v: RemoteViews, word: WidgetWord, withCta: Boolean) {
            val status = res.getString(
                when (word.kind) {
                    WordKind.DUE -> R.string.widget_status_due
                    WordKind.LEARNED -> R.string.widget_status_learned
                    WordKind.NEW -> R.string.widget_status_new
                }
            )
            v.setTextViewText(R.id.tv_status, status)
            v.setTextViewText(R.id.tv_status_line, status)
            paint.background(
                v, R.id.tv_status,
                when (word.kind) {
                    WordKind.DUE -> WidgetSurface.CHIP_DUE
                    WordKind.LEARNED -> WidgetSurface.CHIP_LEARNED
                    WordKind.NEW -> WidgetSurface.CHIP
                }
            )

            val category = categoryLabel(word.category)
            if (category != null) {
                v.setTextViewText(R.id.tv_category, category)
                v.setViewVisibility(R.id.tv_category, View.VISIBLE)
            } else {
                v.setViewVisibility(R.id.tv_category, View.GONE)
            }

            if (wordBitmap != null) {
                v.setImageViewBitmap(R.id.iv_arabic, wordBitmap)
                v.setContentDescription(R.id.iv_arabic, word.arabic)
                v.setViewVisibility(R.id.iv_arabic, View.VISIBLE)
                v.setViewVisibility(R.id.tv_arabic, View.GONE)
            } else {
                v.setTextViewText(R.id.tv_arabic, word.arabic)
                v.setViewVisibility(R.id.tv_arabic, View.VISIBLE)
                v.setViewVisibility(R.id.iv_arabic, View.GONE)
            }

            val hidden = WidgetWordPicker.isMeaningHidden(word.kind, word.id, revealedWordId)
            if (hidden) {
                v.setViewVisibility(R.id.tv_meaning, View.GONE)
                v.setViewVisibility(R.id.tv_reveal, View.VISIBLE)
                v.setContentDescription(R.id.tv_reveal, res.getString(R.string.widget_tap_to_reveal))
                v.setOnClickPendingIntent(R.id.tv_reveal, WidgetActionReceiver.reveal(context, kind, widgetId, word.id))
            } else {
                v.setTextViewText(R.id.tv_meaning, word.meaning)
                v.setViewVisibility(R.id.tv_meaning, View.VISIBLE)
                v.setViewVisibility(R.id.tv_reveal, View.GONE)
                if (word.kind == WordKind.DUE) {
                    // Tapping the revealed meaning hides it again - quick self-testing.
                    v.setOnClickPendingIntent(R.id.tv_meaning, WidgetActionReceiver.reveal(context, kind, widgetId, word.id))
                }
            }

            bindMeter(v, word.strength)

            val example = word.example
            if (example != null) {
                v.setViewVisibility(R.id.ll_example, View.VISIBLE)
                v.setTextViewText(R.id.tv_example_arabic, highlighted(example.arabic, example.arabicStart, example.arabicEnd))
                val line = example.translation?.takeIf { !hidden && it.isNotBlank() }
                if (line == null) {
                    v.setViewVisibility(R.id.tv_example_translation, View.GONE)
                } else {
                    val body = highlighted(line, example.translationStart, example.translationEnd)
                    val text = if (example.isWordByWord) {
                        SpannableStringBuilder()
                            .append(res.getString(R.string.verse_word_by_word_label), StyleSpan(Typeface.BOLD), 0)
                            .append(": ")
                            .append(body)
                    } else {
                        body
                    }
                    v.setTextViewText(R.id.tv_example_translation, text)
                    v.setViewVisibility(R.id.tv_example_translation, View.VISIBLE)
                }
                bindOptionalText(
                    v, R.id.tv_example_ref,
                    example.reference?.let { VerseReferenceFormatter.format(it, language) }?.takeIf { it.isNotBlank() }
                )
            } else {
                v.setViewVisibility(R.id.ll_example, View.GONE)
            }

            v.setTextViewText(R.id.tv_word_facts, res.getString(R.string.widget_word_facts, num(word.occurrences), num(word.rank)))

            val wordTarget = WidgetActions.forWord(word.kind, word.id)
            val openWord = WidgetDeepLink.pendingIntent(context, widgetId, "word", wordTarget)
            v.setOnClickPendingIntent(R.id.fl_word, openWord)
            v.setOnClickPendingIntent(R.id.ll_word, openWord)
            if (kind == WidgetKind.WORD) v.setOnClickPendingIntent(android.R.id.background, openWord)

            if (withCta) {
                v.setTextViewText(
                    R.id.tv_cta,
                    res.getString(
                        when (word.kind) {
                            WordKind.DUE -> R.string.widget_review_now
                            WordKind.LEARNED -> R.string.widget_practice_word
                            WordKind.NEW -> R.string.widget_start_learning
                        }
                    )
                )
                v.setOnClickPendingIntent(R.id.tv_cta, openWord)
            }

            v.setOnClickPendingIntent(R.id.btn_next, WidgetActionReceiver.next(context, kind, widgetId))
            // Only worth a "next" button when there's more than one word to rotate through.
            val poolSize = when (word.kind) {
                WordKind.DUE -> snapshot.pools.due.size
                WordKind.LEARNED -> snapshot.pools.learned.size
                WordKind.NEW -> 1
            }
            v.setViewVisibility(R.id.btn_next, if (poolSize > 1) View.VISIBLE else View.GONE)
        }

        private fun bindMeter(v: RemoteViews, strength: WordStrength) {
            SEGMENTS.forEachIndexed { index, id ->
                paint.image(v, id, if (index < strength.level) WidgetSurface.SEGMENT_ON else WidgetSurface.SEGMENT_OFF)
            }
            val label = res.getString(strengthLabel(strength))
            v.setTextViewText(R.id.tv_strength, label)
            v.setContentDescription(R.id.ll_meter, res.getString(R.string.a11y_word_strength, label))
        }

        private fun bindStats(v: RemoteViews) {
            val s = snapshot.stats
            val streak = num(s.streakDays)
            v.setTextViewText(R.id.tv_streak, streak)
            v.setTextViewText(R.id.tv_streak_label, res.getString(R.string.widget_streak_label))
            v.setContentDescription(R.id.ll_streak, res.getString(R.string.widget_a11y_streak, streak))
            // The flame is lit once today's practice is done; dimmed while the streak is at risk.
            v.setInt(R.id.iv_flame, "setImageAlpha", if (s.practicedToday) 255 else 115)

            val goalLevel = WidgetFormat.level(WidgetFormat.goalFraction(s.todayMinutes, s.goalMinutes))
            paint.image(v, R.id.iv_ring, WidgetSurface.RING)
            v.setInt(R.id.iv_ring, "setImageLevel", goalLevel)
            paint.image(v, R.id.iv_goal_bar, WidgetSurface.BAR)
            v.setInt(R.id.iv_goal_bar, "setImageLevel", goalLevel)
            val today = num(s.todayMinutes)
            val goal = num(s.goalMinutes)
            v.setTextViewText(R.id.tv_goal_value, today)
            v.setTextViewText(
                R.id.tv_goal_unit,
                if (s.goalMet) res.getString(R.string.widget_goal_done) else res.getString(R.string.widget_goal_of, goal)
            )
            v.setTextViewText(
                R.id.tv_goal_text,
                if (s.goalMet) res.getString(R.string.widget_goal_done) else res.getString(R.string.widget_goal_value, today, goal)
            )
            v.setContentDescription(R.id.fl_goal, res.getString(R.string.widget_a11y_goal, today, goal))

            val level = s.level
            v.setTextViewText(R.id.tv_level, res.getString(R.string.level_label, num(level.level)))
            v.setTextViewText(R.id.tv_xp, res.getString(R.string.level_xp_progress, num(level.xpIntoLevel), num(level.xpForNextLevel)))
            paint.image(v, R.id.iv_xp_bar, WidgetSurface.BAR)
            v.setInt(R.id.iv_xp_bar, "setImageLevel", WidgetFormat.level(level.fraction))

            if (s.hearts != null) {
                v.setViewVisibility(R.id.ll_hearts, View.VISIBLE)
                v.setTextViewText(R.id.tv_hearts, num(s.hearts))
                v.setContentDescription(R.id.ll_hearts, res.getString(R.string.a11y_hearts, num(s.hearts), num(s.maxHearts)))
            } else {
                v.setViewVisibility(R.id.ll_hearts, View.GONE)
            }

            if (s.questsTotal > 0) {
                v.setViewVisibility(R.id.ll_quests, View.VISIBLE)
                v.setTextViewText(R.id.tv_quests, res.getString(R.string.widget_quests_value, num(s.questsDone), num(s.questsTotal)))
            } else {
                v.setViewVisibility(R.id.ll_quests, View.GONE)
            }

            v.setTextViewText(R.id.tv_due_value, num(s.dueCount))
            v.setTextViewText(R.id.tv_due_label, res.getString(R.string.widget_review_label))
            v.setTextViewText(R.id.tv_learned_value, num(s.learnedCount))
            v.setTextViewText(R.id.tv_learned_label, res.getString(R.string.widget_words_learned_label))

            val target = WidgetActions.forStats(snapshot.contentReady, s.dueCount)
            v.setTextViewText(
                R.id.tv_cta,
                if (target.destination == WidgetDestination.DAILY_REVIEW) {
                    res.getString(R.string.widget_review_count, num(s.dueCount))
                } else {
                    res.getString(R.string.widget_continue_learning)
                }
            )
            val cta = WidgetDeepLink.pendingIntent(context, widgetId, "cta", target)
            v.setOnClickPendingIntent(R.id.tv_cta, cta)
            v.setOnClickPendingIntent(R.id.ll_tile_due, cta)
        }

        /** [text] with only `[start, end)` in bold highlight colour (green on light, gold on dark,
         *  as in the app) - the content's explicit span, never a search. */
        private fun highlighted(text: String, start: Int?, end: Int?): CharSequence {
            if (start == null || end == null || start !in 0 until end || end > text.length) return text
            val green = paint.color(WidgetColor.HIGHLIGHT) ?: ContextCompat.getColor(context, R.color.wg_highlight)
            return SpannableString(text).apply {
                setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(green), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        private fun bindOptionalText(v: RemoteViews, id: Int, text: String?) {
            if (text.isNullOrBlank()) {
                v.setViewVisibility(id, View.GONE)
            } else {
                v.setTextViewText(id, text)
                v.setViewVisibility(id, View.VISIBLE)
            }
        }

        private fun categoryLabel(category: LemmaCategory?): String? = when (category) {
            LemmaCategory.NOUN -> res.getString(R.string.word_category_noun)
            LemmaCategory.VERB -> res.getString(R.string.word_category_verb)
            LemmaCategory.PARTICLE -> res.getString(R.string.word_category_particle)
            LemmaCategory.MIXED, null -> null
        }
    }

    private val SEGMENTS = intArrayOf(R.id.iv_seg1, R.id.iv_seg2, R.id.iv_seg3, R.id.iv_seg4)

    fun strengthLabel(strength: WordStrength): Int = when (strength) {
        WordStrength.NEW -> R.string.word_strength_new
        WordStrength.LEARNING -> R.string.word_strength_learning
        WordStrength.FAMILIAR -> R.string.word_strength_familiar
        WordStrength.STRONG -> R.string.word_strength_strong
        WordStrength.MASTERED -> R.string.word_strength_mastered
    }

    private val Language.isRtl: Boolean get() = this == Language.URDU || this == Language.PERSIAN
}
