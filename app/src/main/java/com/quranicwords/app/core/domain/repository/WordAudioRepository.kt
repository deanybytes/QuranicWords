package com.quranicwords.app.core.domain.repository

interface WordAudioRepository {
    /**
     * Resolves [assetPath] (e.g. `"audio/words/wf_1.mp3"`) to a URI a media player can open
     * directly - a local `file://` path if the clip is already cached on-device (see
     * [downloadAll]), otherwise the remote URL to stream on demand. Never throws; the caller
     * (an [android.media.MediaPlayer]/[com.quranicwords.app.core.util.AudioPlayer]-style
     * consumer) is responsible for handling a URI that turns out to 404 - not every word has a
     * real clip yet, see [com.quranicwords.app.core.util.AudioConfig]'s doc comment.
     */
    suspend fun resolvePlayableUri(assetPath: String): String

    /** Whether [assetPath]'s clip is already cached on-device (no network needed to play it). */
    suspend fun isCachedLocally(assetPath: String): Boolean

    /**
     * Downloads every word's clip to local storage, skipping ones already cached - backs the
     * Settings "download all audio for offline use" action. [onProgress] receives
     * (downloadedOrSkipped, total) after each item, for a progress indicator; failures are
     * counted as done-and-skipped rather than aborting the whole batch, since one missing/broken
     * clip shouldn't block the rest.
     */
    suspend fun downloadAll(onProgress: (done: Int, total: Int) -> Unit)
}
