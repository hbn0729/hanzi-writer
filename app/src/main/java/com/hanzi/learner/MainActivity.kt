package com.hanzi.learner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hanzi.learner.app.AppContainer
import com.hanzi.learner.app.HanziLearnerApp
import com.hanzi.learner.app.theme.HanziLearnerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val container by produceState<AppContainer?>(null) {
                value = (application as HanziLearnerApplication).containerDeferred.await()
            }
            HanziLearnerTheme {
                if (container != null) {
                    HanziLearnerApp(appDeps = container!!)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
