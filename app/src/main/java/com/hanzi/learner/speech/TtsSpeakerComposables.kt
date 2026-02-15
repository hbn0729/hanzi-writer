package com.hanzi.learner.speech

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

@Composable
fun rememberTtsSpeaker(context: Context): TtsSpeakerContract {
    val speaker = remember(context) { TtsSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
