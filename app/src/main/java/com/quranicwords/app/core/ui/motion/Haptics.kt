package com.quranicwords.app.core.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Intention-named haptic calls so exercise/summary screens describe *why* they're buzzing
 * instead of picking a raw [HapticFeedbackType] inline at each call site. */
class QwHaptics(private val feedback: HapticFeedback) {
    fun onCorrectAnswer() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    fun onIncorrectAnswer() = feedback.performHapticFeedback(HapticFeedbackType.Reject)
    fun onStreakMilestone() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    fun onSelect() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
}

@Composable
fun rememberQwHaptics(): QwHaptics {
    val feedback = LocalHapticFeedback.current
    return QwHaptics(feedback)
}
