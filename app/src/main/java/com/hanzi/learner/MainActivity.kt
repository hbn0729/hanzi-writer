package com.hanzi.learner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hanzi.learner.app.AppContainer
import com.hanzi.learner.app.HanziLearnerApp
import com.hanzi.learner.app.theme.HanziLearnerTheme

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = AppContainer(applicationContext)
        setContent {
            HanziLearnerTheme {
                HanziLearnerApp(appDeps = appContainer)
            }
        }
    }
}
