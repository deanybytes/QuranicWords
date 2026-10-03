package com.quranicwords.app.feature.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ThemeMode

/** Colour roles of the widget palette (res/values/widget_colors.xml). */
enum class WidgetColor(@ColorRes val light: Int, @ColorRes val dark: Int) {
    TEXT_PRIMARY(R.color.wg_light_text_primary, R.color.wg_dark_text_primary),
    TEXT_SECONDARY(R.color.wg_light_text_secondary, R.color.wg_dark_text_secondary),
    TEXT_TERTIARY(R.color.wg_light_text_tertiary, R.color.wg_dark_text_tertiary),
    ACCENT(R.color.wg_light_accent, R.color.wg_dark_accent),
    HIGHLIGHT(R.color.wg_light_highlight, R.color.wg_dark_highlight),
    GOLD(R.color.wg_light_gold, R.color.wg_dark_gold),
    FLAME(R.color.wg_light_flame, R.color.wg_dark_flame),
    HEART(R.color.wg_light_heart, R.color.wg_dark_heart),
    CTA_TEXT(R.color.wg_light_cta_text, R.color.wg_dark_cta_text)
}

/** Background/image drawables that exist as auto (night-qualified), light and dark variants. */
enum class WidgetSurface(@DrawableRes val auto: Int, @DrawableRes val light: Int, @DrawableRes val dark: Int) {
    ROOT(R.drawable.widget_bg, R.drawable.widget_bg_light, R.drawable.widget_bg_dark),
    CARD(R.drawable.widget_card, R.drawable.widget_card_light, R.drawable.widget_card_dark),
    CHIP(R.drawable.widget_chip, R.drawable.widget_chip_light, R.drawable.widget_chip_dark),
    CHIP_DUE(R.drawable.widget_chip_due, R.drawable.widget_chip_due_light, R.drawable.widget_chip_due_dark),
    CHIP_LEARNED(R.drawable.widget_chip_learned, R.drawable.widget_chip_learned_light, R.drawable.widget_chip_learned_dark),
    CTA(R.drawable.widget_cta, R.drawable.widget_cta_light, R.drawable.widget_cta_dark),
    ICON_BUTTON(R.drawable.widget_icon_btn, R.drawable.widget_icon_btn_light, R.drawable.widget_icon_btn_dark),
    REVEAL(R.drawable.widget_reveal, R.drawable.widget_reveal_light, R.drawable.widget_reveal_dark),
    SEGMENT_ON(R.drawable.widget_seg_on, R.drawable.widget_seg_on_light, R.drawable.widget_seg_on_dark),
    SEGMENT_OFF(R.drawable.widget_seg_off, R.drawable.widget_seg_off_light, R.drawable.widget_seg_off_dark),
    BAR(R.drawable.widget_bar, R.drawable.widget_bar_light, R.drawable.widget_bar_dark),
    RING(R.drawable.widget_ring, R.drawable.widget_ring_light, R.drawable.widget_ring_dark)
}

/**
 * Applies the in-app [ThemeMode] to RemoteViews.
 *
 * The layouts' XML defaults are the "auto" palette: `@color/wg_*` and the unsuffixed drawables
 * resolve against the *launcher's* night mode, which is exactly the System theme - and keeps
 * following it when the user flips dark mode, with no update from us. When the learner forces
 * Light or Dark in the app, [fixedNight] is set and every colour and surface is overridden
 * explicitly with the fixed light/dark variants. Only remotable-everywhere calls are used
 * (setTextColor, setColorFilter, setBackgroundResource, setImageViewResource), so this behaves the
 * same from API 24 to 36. State-dependent looks (due vs learned chip, filled meter segments) are
 * always chosen by *resource*, never by a resolved colour, so they stay correct in auto mode too.
 */
class WidgetPaint private constructor(private val context: Context, val fixedNight: Boolean?) {

    fun surface(s: WidgetSurface): Int = when (fixedNight) {
        null -> s.auto
        true -> s.dark
        false -> s.light
    }

    fun color(c: WidgetColor): Int? = fixedNight?.let { ContextCompat.getColor(context, if (it) c.dark else c.light) }

    fun background(views: RemoteViews, @IdRes id: Int, s: WidgetSurface) =
        views.setInt(id, "setBackgroundResource", surface(s))

    fun image(views: RemoteViews, @IdRes id: Int, s: WidgetSurface) = views.setImageViewResource(id, surface(s))

    /** Forced-theme only: the auto XML colours already match each view's role. */
    fun applyFixedPalette(views: RemoteViews) {
        if (fixedNight == null) return
        TEXT_ROLES.forEach { (id, role) -> color(role)?.let { views.setTextColor(id, it) } }
        TINT_ROLES.forEach { (id, role) -> color(role)?.let { views.setInt(id, "setColorFilter", it) } }
        STATIC_SURFACES.forEach { (id, s) -> background(views, id, s) }
        background(views, android.R.id.background, WidgetSurface.ROOT)
    }

    companion object {
        fun forMode(context: Context, mode: ThemeMode) = WidgetPaint(
            context,
            when (mode) {
                ThemeMode.SYSTEM -> null
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
        )

        /** Every TextView id used by a widget layout, with the role its XML colour has. */
        private val TEXT_ROLES: List<Pair<Int, WidgetColor>> = listOf(
            R.id.tv_status to WidgetColor.TEXT_PRIMARY,
            R.id.tv_status_line to WidgetColor.TEXT_SECONDARY,
            R.id.tv_category to WidgetColor.TEXT_SECONDARY,
            R.id.tv_arabic to WidgetColor.TEXT_PRIMARY,
            R.id.tv_meaning to WidgetColor.ACCENT,
            R.id.tv_reveal to WidgetColor.ACCENT,
            R.id.tv_strength to WidgetColor.TEXT_TERTIARY,
            R.id.tv_example_arabic to WidgetColor.TEXT_PRIMARY,
            R.id.tv_example_translation to WidgetColor.TEXT_SECONDARY,
            R.id.tv_example_ref to WidgetColor.ACCENT,
            R.id.tv_word_facts to WidgetColor.TEXT_TERTIARY,
            R.id.tv_cta to WidgetColor.CTA_TEXT,
            R.id.tv_empty_title to WidgetColor.TEXT_PRIMARY,
            R.id.tv_empty_desc to WidgetColor.TEXT_SECONDARY,
            R.id.tv_empty_cta to WidgetColor.CTA_TEXT,
            R.id.tv_title to WidgetColor.TEXT_PRIMARY,
            R.id.tv_streak to WidgetColor.TEXT_PRIMARY,
            R.id.tv_streak_label to WidgetColor.TEXT_SECONDARY,
            R.id.tv_hearts to WidgetColor.TEXT_PRIMARY,
            R.id.tv_goal_value to WidgetColor.TEXT_PRIMARY,
            R.id.tv_goal_unit to WidgetColor.TEXT_TERTIARY,
            R.id.tv_goal_text to WidgetColor.TEXT_SECONDARY,
            R.id.tv_level to WidgetColor.TEXT_PRIMARY,
            R.id.tv_xp to WidgetColor.TEXT_TERTIARY,
            R.id.tv_quests to WidgetColor.TEXT_SECONDARY,
            R.id.tv_due_value to WidgetColor.GOLD,
            R.id.tv_due_label to WidgetColor.TEXT_TERTIARY,
            R.id.tv_learned_value to WidgetColor.ACCENT,
            R.id.tv_learned_label to WidgetColor.TEXT_TERTIARY
        )

        /** Tinted (white-glyph) images and their roles. */
        private val TINT_ROLES: List<Pair<Int, WidgetColor>> = listOf(
            R.id.iv_arabic to WidgetColor.TEXT_PRIMARY,
            R.id.btn_next to WidgetColor.TEXT_PRIMARY,
            R.id.iv_flame to WidgetColor.FLAME,
            R.id.iv_heart to WidgetColor.HEART,
            R.id.iv_quests to WidgetColor.ACCENT
        )

        /** Views whose background never changes with state. */
        private val STATIC_SURFACES: List<Pair<Int, WidgetSurface>> = listOf(
            R.id.tv_category to WidgetSurface.CHIP,
            R.id.btn_next to WidgetSurface.ICON_BUTTON,
            R.id.tv_reveal to WidgetSurface.REVEAL,
            R.id.ll_example to WidgetSurface.CARD,
            R.id.ll_stats_card to WidgetSurface.CARD,
            R.id.ll_tile_due to WidgetSurface.CARD,
            R.id.ll_tile_learned to WidgetSurface.CARD,
            R.id.tv_cta to WidgetSurface.CTA,
            R.id.tv_empty_cta to WidgetSurface.CTA
        )
    }
}
