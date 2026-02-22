package com.hanzi.learner.speech

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.internal.PreferenceTtsSpeaker

@Composable
fun rememberTtsSpeaker(
    context: Context,
    preferenceRepository: TtsPreferenceRepositoryContract,
    downloadManager: TtsModelDownloadManagerContract,
): TtsSpeakerContract {
    val speaker = remember(context, preferenceRepository, downloadManager) {
        PreferenceTtsSpeaker(
            context = context,
            preferenceRepository = preferenceRepository,
            downloadManager = downloadManager,
        )
    }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
