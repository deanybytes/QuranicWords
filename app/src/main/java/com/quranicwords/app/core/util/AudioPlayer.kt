package com.quranicwords.app.core.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper for short bundled audio clips (letter names, word pronunciations). Deliberately
 * simple `MediaPlayer` rather than media3/ExoPlayer - this increment only plays short clips from
 * app assets, no streaming/queuing. Real male-voice recitation audio is an open content
 * dependency (see plan); [play] no-ops safely when [assetPath] isn't bundled yet.
 */
@Singleton
class AudioPlayer @Inject constructor(@ApplicationContext private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    /** Returns true if playback actually started; false (no throw) if the asset is missing. */
    fun play(assetPath: String, onCompletion: () -> Unit = {}): Boolean {
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
