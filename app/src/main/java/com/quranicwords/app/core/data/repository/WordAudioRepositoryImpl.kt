package com.quranicwords.app.core.data.repository

import android.content.Context
import android.util.Log
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.WordAudioRepository
import com.quranicwords.app.core.util.AudioConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WordAudioRepository"

@Singleton
class WordAudioRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentRepository: ContentRepository
) : WordAudioRepository {

    private val cacheDir: File by lazy { File(context.filesDir, "audio").apply { mkdirs() } }

    private fun fileName(assetPath: String) = assetPath.substringAfterLast('/')
    private fun cacheFile(assetPath: String) = File(cacheDir, fileName(assetPath))
    private fun remoteUrl(assetPath: String) = AudioConfig.WORD_AUDIO_BASE_URL + fileName(assetPath)

    override suspend fun resolvePlayableUri(assetPath: String): String = withContext(Dispatchers.IO) {
        val cached = cacheFile(assetPath)
        if (cached.exists()) "file://${cached.absolutePath}" else remoteUrl(assetPath)
    }

    override suspend fun isCachedLocally(assetPath: String): Boolean = withContext(Dispatchers.IO) {
        cacheFile(assetPath).exists()
    }

    override suspend fun downloadAll(onProgress: (done: Int, total: Int) -> Unit) = withContext(Dispatchers.IO) {
        val assetPaths = contentRepository.getWordCandidates().mapNotNull { it.audioAssetPath }.distinct()
        var done = 0
        onProgress(done, assetPaths.size)
        for (assetPath in assetPaths) {
            downloadOne(assetPath)
            done += 1
            onProgress(done, assetPaths.size)
        }
    }

    private fun downloadOne(assetPath: String) {
        val dest = cacheFile(assetPath)
        if (dest.exists()) return
        val tmp = File(cacheDir, "${dest.name}.part")
        try {
            (URL(remoteUrl(assetPath)).openConnection() as HttpURLConnection).run {
                connectTimeout = 15_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                connect()
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "download failed ($responseCode) for $assetPath")
                    disconnect()
                    return
                }
                inputStream.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
                disconnect()
            }
            tmp.renameTo(dest)
        } catch (e: Exception) {
            Log.w(TAG, "download failed for $assetPath", e)
            tmp.delete()
        }
    }
}
