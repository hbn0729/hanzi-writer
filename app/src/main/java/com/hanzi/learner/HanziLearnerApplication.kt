package com.hanzi.learner

import android.app.Application
import android.os.StrictMode
import android.os.SystemClock
import com.hanzi.learner.app.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class HanziLearnerApplication : Application() {
    val startTimestamp: Long = SystemClock.elapsedRealtime()

    // PWR-07: Named scope with SupervisorJob for lifecycle-aware cancellation
    private val appJob = SupervisorJob()
    private val appScope = CoroutineScope(Dispatchers.IO + appJob)

    val containerDeferred: Deferred<AppContainer> by lazy {
        appScope.async { AppContainer(applicationContext) }
    }

    override fun onCreate() {
        super.onCreate()

        // PWR-08: Detect main-thread disk I/O and leaked resources in debug builds
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )
        }

        containerDeferred // trigger lazy to start background construction
    }
}
