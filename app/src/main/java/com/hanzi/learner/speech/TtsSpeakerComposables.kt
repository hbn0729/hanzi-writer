package com.hanzi.learner.speech

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.hanzi.learner.speech.contract.TtsSpeakerContract

@Composable
fun rememberTtsSpeaker(context: Context): TtsSpeakerContract {
    val speaker = remember(context) { SpeechModule.createTtsSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
