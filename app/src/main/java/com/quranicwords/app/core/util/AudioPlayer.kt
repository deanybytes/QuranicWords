package com.quranicwords.app.core.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.util.Log
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper for short audio clips bundled directly as app assets. Deliberately simple
 * `MediaPlayer` rather than media3/ExoPlayer - no streaming/queuing needed for a clip that's
 * already on disk. [play] no-ops safely when [assetPath] isn't actually bundled.
 *
 * Word-pronunciation clips (`audio/words/wf_<rank>.mp3`, referenced via
 * `WordFrequencyEntity.audioAssetPath`/`ExerciseContent.WordIntro.audioAssetPath`) are generated
 * ahead of time by `tools/ingestion/12_generate_word_audio.py` and bundled directly in the APK -
 * no runtime download, no remote-resolving repository. This class plays anything genuinely
 * shipped inside the APK, pronunciation clips included.
 *
 * Gated on [UserPreferencesDataStore.pronunciationAudioEnabledFlow] - independent of
 * [SfxPlayer]'s own [UserPreferencesDataStore.soundEnabledFlow] gate, since the two are separate
 * "Sound" settings toggles. [play] itself stays synchronous (its callers, e.g.
 * `AudioPlayButton`'s `onPlay: () -> Boolean`, need an immediate result to show the "unavailable"
 * fallback text), so the preference is mirrored into a plain field via a continuously-collecting
 * app-scoped coroutine rather than suspending on every call.
 */
@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    preferences: UserPreferencesDataStore
) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var pronunciationEnabled = true

    init {
        scope.launch {
            preferences.pronunciationAudioEnabledFlow.collect { pronunciationEnabled = it }
        }
    }

    /** Returns true if playback actually started; false (no throw) if the asset is missing or
     * pronunciation audio is turned off in Settings. */
    fun play(assetPath: String, onCompletion: () -> Unit = {}): Boolean {
        if (!pronunciationEnabled) return false
        release()
        val afd = openAsset(assetPath) ?: return false
        return try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnCompletionListener { onCompletion() }
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            Log.w("AudioPlayer", "play() failed for $assetPath", e)
            release()
            false
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /** Non-destructive existence check - doesn't touch [mediaPlayer]/playback state, so it's
     * safe to call while filtering a lesson's exercise list (e.g. before including a
     * [com.quranicwords.app.core.domain.model.ExerciseContent.ListenAndType] item,
     * which has no valid interaction without audio). */
    fun isAvailable(assetPath: String): Boolean = openAsset(assetPath)?.also { it.close() } != null

    /** Shared asset-open/error-handling for [play] and [isAvailable], so asset-resolution logic
     * only lives in one place. Null (never throws) if [assetPath] isn't bundled. */
    private fun openAsset(assetPath: String): AssetFileDescriptor? =
        try {
            context.assets.openFd(assetPath)
        } catch (e: Exception) {
            Log.w("AudioPlayer", "asset not found: $assetPath", e)
            null
        }
}
