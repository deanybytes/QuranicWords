package com.quranicwords.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QwApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = if (::workerFactory.isInitialized) {
            Configuration.Builder().setWorkerFactory(workerFactory).build()
        } else {
            Configuration.Builder().build()
        }
}
