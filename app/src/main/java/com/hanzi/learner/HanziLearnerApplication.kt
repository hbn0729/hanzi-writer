package com.hanzi.learner

import android.app.Application
import android.os.SystemClock
import com.hanzi.learner.app.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class HanziLearnerApplication : Application() {
    val startTimestamp: Long = SystemClock.elapsedRealtime()

    val containerDeferred: Deferred<AppContainer> by lazy {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).async {
            AppContainer(applicationContext)
        }
    }

    override fun onCreate() {
        super.onCreate()
        containerDeferred // trigger lazy to start background construction
    }
}
