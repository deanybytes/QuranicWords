package com.quranicwords.app.core.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.quranicwords.app.R
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Which short UI sound to play - see [SfxPlayer.play]. */
enum class SfxEffect { CORRECT, WRONG, LESSON_COMPLETE, EXAM_PASS, STREAK_MILESTONE, OPENING }

/**
 * Short, low-latency UI sound effects (correct/wrong dings, celebratory chimes) - distinct from
 * [AudioPlayer], which streams/plays longer word-pronunciation recitation clips. [SoundPool] is
 * the right tool here (not [android.media.MediaPlayer]): it preloads every clip up front and can
 * play overlapping instances with near-zero start latency, which matters for "ding the instant
 * the learner taps the correct answer" responsiveness.
 *
 * Checks [UserPreferencesDataStore.soundEnabledFlow] once inside [play] (one choke point) rather
 * than requiring every call site to remember the check itself.
 */
@Singleton
class SfxPlayer @Inject constructor(
    @ApplicationContext context: Context,
    private val preferences: UserPreferencesDataStore
) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds: Map<SfxEffect, Int> = mapOf(
        SfxEffect.CORRECT to soundPool.load(context, R.raw.sfx_correct, 1),
        SfxEffect.WRONG to soundPool.load(context, R.raw.sfx_wrong, 1),
        SfxEffect.LESSON_COMPLETE to soundPool.load(context, R.raw.sfx_lesson_complete, 1),
        SfxEffect.EXAM_PASS to soundPool.load(context, R.raw.sfx_exam_pass, 1),
        SfxEffect.STREAK_MILESTONE to soundPool.load(context, R.raw.sfx_streak_milestone, 1),
        SfxEffect.OPENING to soundPool.load(context, R.raw.sfx_opening, 1)
    )

    /** No-ops silently if the master sound toggle is off or the clip hasn't finished loading yet
     * (a benign race only possible in the first instant after construction). */
    suspend fun play(effect: SfxEffect) {
        if (!preferences.soundEnabledFlow.first()) return
        val soundId = soundIds[effect] ?: return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
