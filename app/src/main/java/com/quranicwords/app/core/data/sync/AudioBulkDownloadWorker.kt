package com.quranicwords.app.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranicwords.app.core.domain.repository.WordAudioRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking

/** Backs the Settings "download all pronunciation audio" action - see [WordAudioRepository
 * .downloadAll]. Runs as a background [CoroutineWorker] (survives the Settings screen closing,
 * process death, etc.) rather than a plain ViewModel-scoped coroutine, since a full download can
 * genuinely take a while. Progress is published via [setProgress] as `done`/`total` ints, read by
 * [com.quranicwords.app.feature.settings.SettingsViewModel] through WorkManager's own
 * work-info Flow rather than a separate channel. */
@HiltWorker
class AudioBulkDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val wordAudioRepository: WordAudioRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // downloadAll's progress callback isn't suspend (it's a plain callback the repository
        // invokes from its own IO-dispatcher work) - runBlocking here just publishes a small
        // WorkManager Data write synchronously on that same IO thread, not a real blocking wait.
        wordAudioRepository.downloadAll { done, total ->
            runBlocking { setProgress(workDataOf(KEY_DONE to done, KEY_TOTAL to total)) }
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "audio_bulk_download"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
    }
}
