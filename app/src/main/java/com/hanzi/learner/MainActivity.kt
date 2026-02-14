package com.hanzi.learner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hanzi.learner.ui.AppContainer
import com.hanzi.learner.ui.HanziLearnerApp
import com.hanzi.learner.ui.theme.HanziLearnerTheme

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
