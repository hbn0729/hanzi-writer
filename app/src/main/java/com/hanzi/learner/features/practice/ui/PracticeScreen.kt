package com.hanzi.learner.features.practice.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanzi.learner.R
import com.hanzi.learner.features.practice.viewmodel.FlashState
import com.hanzi.learner.features.practice.viewmodel.PracticeAction
import com.hanzi.learner.features.practice.viewmodel.PracticeUiState
import com.hanzi.learner.features.practice.viewmodel.PracticeViewModel
import com.hanzi.learner.character_writer.match.StrokeMatchConfig
import com.hanzi.learner.character_writer.match.StrokeMatchOptions
import com.hanzi.learner.character_writer.match.matchesWithDefaults
import com.hanzi.learner.character_writer.practice.HanziTraceOverlay
import com.hanzi.learner.speech.rememberTtsSpeaker
import com.hanzi.learner.app.PracticeFeatureDependencies
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PracticeScreen(
    paddingValues: PaddingValues,
    onExit: () -> Unit,
    deps: PracticeFeatureDependencies,
    reviewOnly: Boolean = false,
) {
    val context = LocalContext.current
    val speaker = rememberTtsSpeaker(context)
    val factory = remember(deps, reviewOnly) {
        PracticeViewModel.Factory(
            reviewOnly = reviewOnly,
            engineFactory = deps.practiceSessionEngineFactory,
            completePracticeCharacter = deps.completePracticeCharacterUseCase,
        )
    }
    val viewModel: PracticeViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    PracticeFeedbackEffects(
        uiState = uiState,
        onAction = viewModel::onAction,
        speaker = speaker,
    )

    PracticeContent(
        uiState = uiState,
        paddingValues = paddingValues,
        reviewOnly = reviewOnly,
        onExit = onExit,
        onAction = viewModel::onAction,
        matcher = deps.strokeMatcher,
        speaker = speaker,
    )
}

@Composable
private fun PracticeFeedbackEffects(
    uiState: PracticeUiState,
    onAction: (PracticeAction) -> Unit,
    speaker: com.hanzi.learner.speech.TtsSpeakerContract,
) {
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        onAction(PracticeAction.Start)
    }

    LaunchedEffect(uiState.currentItem?.char) {
        val char = uiState.currentItem?.char
        val phrase = uiState.currentPhrase
        if (!char.isNullOrEmpty()) {
            speaker.speakCharacterAndPhrase(char, phrase)
        }
    }

    LaunchedEffect(uiState.flashColorState) {
        if (uiState.flashColorState != FlashState.None) {
            if (uiState.flashColorState == FlashState.Success) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            delay(220)
            onAction(PracticeAction.ClearFlash)
        }
    }
}

@Composable
private fun PracticeContent(
    uiState: PracticeUiState,
    paddingValues: PaddingValues,
    reviewOnly: Boolean,
    onExit: () -> Unit,
    onAction: (PracticeAction) -> Unit,
    matcher: com.hanzi.learner.character_writer.match.StrokeMatcherContract,
    speaker: com.hanzi.learner.speech.TtsSpeakerContract,
) {
    val flashColor = when (uiState.flashColorState) {
        FlashState.Success -> Color(0x992E7D32)
        FlashState.Failure -> Color(0x99C62828)
        FlashState.None -> null
    }

    val scope = rememberCoroutineScope()

    val scale = remember { Animatable(1f) }
    val triggerAnimation = {
        scope.launch {
            scale.snapTo(1f)
            scale.animateTo(
                targetValue = 1.5f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.isSessionComplete || uiState.allDisabled || uiState.noReviewsDue) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val message = when {
                reviewOnly -> "所有字都复习完了"
                uiState.allDisabled -> "所有字都被禁用了"
                else -> "所有字都学习完了，今天还没要复习的字."
            }
            Text(
                text = message,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onExit) {
                Text("Back")
            }
        }
        return
    }

    val currentItem = uiState.currentItem
    val character = uiState.currentCharacter

    if (currentItem == null || character == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CharacterHeader(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            hanzi = currentItem.char,
            phrase = uiState.currentPhrase,
            scaleValue = scale.value,
            onSpeak = { hanzi ->
                speaker.speakCharacterAndPhrase(hanzi, uiState.currentPhrase)
                triggerAnimation()
            },
        )

        TraceCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.25f),
            flashColor = flashColor,
            uiState = uiState,
            character = character,
            matcher = matcher,
            onStrokeEnd = { res -> onAction(PracticeAction.StrokeResult(res.isMatch)) },
        )

        ExitButtonRow(onExit = onExit)
    }
}

@Composable
private fun CharacterHeader(
    modifier: Modifier,
    hanzi: String,
    phrase: String,
    scaleValue: Float,
    onSpeak: (String) -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = hanzi,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    IconButton(
                        onClick = {
                            if (hanzi.isNotEmpty()) {
                                onSpeak(hanzi)
                            }
                        },
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .size(108.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_volume),
                            contentDescription = "Speak",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (phrase.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    phrase.forEach { char ->
                        val isTarget = char.toString() == hanzi
                        Text(
                            text = char.toString(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.graphicsLayer {
                                if (isTarget) {
                                    scaleX = scaleValue
                                    scaleY = scaleValue
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TraceCanvas(
    modifier: Modifier,
    flashColor: Color?,
    uiState: PracticeUiState,
    character: com.hanzi.learner.character_writer.model.CharacterData,
    matcher: com.hanzi.learner.character_writer.match.StrokeMatcherContract,
    onStrokeEnd: (com.hanzi.learner.character_writer.match.StrokeMatchResult) -> Unit,
) {
    Box(
        modifier = modifier.background(color = flashColor ?: Color.Transparent),
    ) {
        HanziTraceOverlay(
            character = character,
            modifier = Modifier.fillMaxSize(),
            strokeIndex = uiState.strokeIndex.coerceIn(0, character.medians.size - 1),
            completedStrokeCount = uiState.completedStrokeCount,
            showHintStroke = uiState.mistakesOnStroke >= uiState.hintAfterMisses,
            matcher = { userStrokePoints, c, s ->
                matcher.matchesWithDefaults(
                    userStrokePoints = userStrokePoints,
                    character = c,
                    strokeNum = s,
                    options = StrokeMatchOptions(),
                    config = StrokeMatchConfig(),
                )
            },
            onStrokeEnd = onStrokeEnd,
        )
    }
}

@Composable
private fun ExitButtonRow(onExit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onExit,
            modifier = Modifier
                .size(72.dp)
                .padding(start = 12.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Exit",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
