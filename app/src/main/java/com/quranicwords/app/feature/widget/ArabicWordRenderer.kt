package com.quranicwords.app.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import android.util.LruCache
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.QuranFontStyle
import kotlin.math.ceil
import kotlin.math.max

/**
 * Draws the word in the learner's chosen Qur'anic typeface. RemoteViews can't apply an app font
 * to a TextView - the launcher inflates widget layouts in a restricted context that ignores font
 * resources - so the word is rendered here, in-process, to a small alpha-only bitmap: white glyph
 * coverage that the ImageView tints to the palette's text colour (auto or forced theme alike).
 * One bitmap is shared by every size variant of an update, which RemoteViews de-duplicates.
 * Returns null on any failure; the layout then falls back to its system-font TextView.
 */
object ArabicWordRenderer {
    private const val TAG = "ArabicWordRenderer"

    /** Rendered text size; layouts scale the bitmap down (fitCenter), never up past this. */
    private const val TEXT_SIZE_DP = 44f
    private const val MAX_WIDTH_DP = 300f

    private val typefaces = HashMap<Int, Typeface>()
    private val bitmaps = LruCache<String, Bitmap>(6)

    @FontRes
    fun fontFor(style: QuranFontStyle): Int = when (style.fontKey) {
        "kfgqpc_hafs" -> R.font.kfgqpc_hafs_regular
        "lateef" -> R.font.lateef_regular
        "amiri" -> R.font.amiri_regular
        "scheherazade" -> R.font.scheherazade_regular
        "noto_naskh" -> R.font.noto_naskh_regular
        "noorehuda" -> R.font.noorehuda_regular
        "noorehira" -> R.font.noorehira_regular
        else -> R.font.kfgqpc_hafs_regular
    }

    @Synchronized
    fun render(context: Context, word: String, style: QuranFontStyle): Bitmap? {
        if (word.isBlank()) return null
        val text = style.script(word)
        val key = "${style.name}|$text"
        bitmaps.get(key)?.let { return it }
        return try {
            val typeface = typefaceFor(context, fontFor(style)) ?: return null
            val density = context.resources.displayMetrics.density
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                this.typeface = typeface
                textSize = TEXT_SIZE_DP * density
                color = android.graphics.Color.WHITE
            }
            val maxWidth = MAX_WIDTH_DP * density
            val measured = paint.measureText(text)
            if (measured > maxWidth) paint.textSize *= maxWidth / measured

            // Ink bounds, not font metrics: Qur'anic fonts carry very tall ascent/descent for
            // stacked marks, which would leave the word floating in empty space.
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val pad = ceil(paint.textSize * 0.08f).toInt()
            val width = max(1, bounds.width() + pad * 2)
            val height = max(1, bounds.height() + pad * 2)
            val bitmap = createBitmap(width, height, Bitmap.Config.ALPHA_8)
            val x = pad - bounds.left.toFloat()
            val y = pad - bounds.top.toFloat()
            Canvas(bitmap).drawText(text, x, y, paint)
            bitmaps.put(key, bitmap)
            bitmap
        } catch (e: Exception) {
            Log.w(TAG, "Could not render word", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Could not render word", e)
            null
        }
    }

    private fun typefaceFor(context: Context, @FontRes res: Int): Typeface? =
        typefaces[res] ?: runCatching { ResourcesCompat.getFont(context, res) }.getOrNull()?.also { typefaces[res] = it }
}
