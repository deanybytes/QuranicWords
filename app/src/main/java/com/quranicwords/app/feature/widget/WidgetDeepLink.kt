package com.quranicwords.app.feature.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.quranicwords.app.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Widget taps open [MainActivity] with an explicit Intent carrying a [WidgetTarget]. The activity
 * hands the intent to [offer]; the nav host consumes the pending target once the learner is past
 * the splash/onboarding flow (see QwNavHost), so a cold start still lands on Home first and the
 * back button returns there.
 */
object WidgetDeepLink {
    const val EXTRA_DESTINATION = "com.quranicwords.app.widget.extra.DESTINATION"
    const val EXTRA_WORD_ID = "com.quranicwords.app.widget.extra.WORD_ID"

    /** Word ids are short slugs; anything longer or oddly shaped is not ours. */
    private val WORD_ID_PATTERN = Regex("^[A-Za-z0-9_.:-]{1,64}$")

    private val pendingTarget = MutableStateFlow<WidgetTarget?>(null)
    val pending: StateFlow<WidgetTarget?> = pendingTarget.asStateFlow()

    /** Pure parse of the two extras - unknown destinations, and a word practice without a valid
     * id, are dropped rather than guessed at. */
    fun parse(destination: String?, wordId: String?): WidgetTarget? {
        val dest = WidgetDestination.entries.firstOrNull { it.name == destination } ?: return null
        return when (dest) {
            WidgetDestination.PRACTICE_WORD ->
                if (wordId != null && WORD_ID_PATTERN.matches(wordId)) WidgetTarget(dest, wordId) else null
            else -> WidgetTarget(dest)
        }
    }

    /** Records [intent]'s target (if it came from a widget) and strips the extras so a later
     * re-delivery of the same intent can't navigate twice. */
    fun offer(intent: Intent?) {
        if (intent == null || !intent.hasExtra(EXTRA_DESTINATION)) return
        val target = parse(intent.getStringExtra(EXTRA_DESTINATION), intent.getStringExtra(EXTRA_WORD_ID))
        intent.removeExtra(EXTRA_DESTINATION)
        intent.removeExtra(EXTRA_WORD_ID)
        if (target != null) pendingTarget.value = target
    }

    /** Takes the pending target, at most once. */
    fun consume(): WidgetTarget? = pendingTarget.getAndUpdate { null }

    fun intentFor(context: Context, target: WidgetTarget): Intent =
        Intent(context, MainActivity::class.java).apply {
            // SINGLE_TOP + CLEAR_TOP reuse a running MainActivity (onNewIntent) instead of
            // recreating it, so an in-progress screen isn't torn down just to open the app.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, target.destination.name)
            target.wordId?.let { putExtra(EXTRA_WORD_ID, it) }
        }

    /** One PendingIntent per (widget, slot): the data URI keeps them distinct, so extras can't
     * bleed between widgets or between the word tap and the button on the same widget. */
    fun pendingIntent(context: Context, widgetId: Int, slot: String, target: WidgetTarget): PendingIntent {
        val intent = intentFor(context, target).apply {
            data = "quranicwords-widget://open/$widgetId/$slot".toUri()
        }
        return PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
