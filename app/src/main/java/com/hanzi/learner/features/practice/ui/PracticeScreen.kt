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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanzi.learner.app.theme.SeniorTheme
import com.hanzi.learner.app.theme.claymorphism
import com.hanzi.learner.app.theme.clayClickable
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
import com.hanzi.learner.speech.contract.TtsSpeakerContract
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
    val speaker = rememberTtsSpeaker(context = context)
    val factory = remember(deps, reviewOnly) {
        PracticeViewModel.Factory(
            reviewOnly = reviewOnly,
            engineFactory = deps.practiceSessionEngineFactory,
            completePracticeCharacter = deps.completePracticeCharacterUseCase,
        )
    }
    val viewModel: PracticeViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val flashState by viewModel.flashState.collectAsStateWithLifecycle()

    SeniorTheme {
        PracticeFeedbackEffects(
            flashState = flashState,
            onAction = viewModel::onAction,
            speaker = speaker,
            currentChar = uiState.currentItem?.char,
            currentPhrase = uiState.currentPhrase,
            autoReadAloud = uiState.autoReadAloud,
        )

        PracticeContent(
            uiState = uiState,
            flashState = flashState,
            paddingValues = paddingValues,
            reviewOnly = reviewOnly,
            onExit = onExit,
            onAction = viewModel::onAction,
            matcher = deps.strokeMatcher,
            speaker = speaker,
        )
    }
}

@Composable
private fun PracticeFeedbackEffects(
    flashState: FlashState,
    onAction: (PracticeAction) -> Unit,
    speaker: TtsSpeakerContract,
    currentChar: String?,
    currentPhrase: String,
    autoReadAloud: Boolean,
) {
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        onAction(PracticeAction.Start)
    }

    LaunchedEffect(currentChar) {
        if (!currentChar.isNullOrEmpty() && autoReadAloud) {
            speaker.speakCharacterAndPhrase(currentChar, currentPhrase)
        }
    }

    LaunchedEffect(flashState) {
        if (flashState != FlashState.None) {
            if (flashState == FlashState.Success) {
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
    flashState: FlashState,
    paddingValues: PaddingValues,
    reviewOnly: Boolean,
    onExit: () -> Unit,
    onAction: (PracticeAction) -> Unit,
    matcher: com.hanzi.learner.character_writer.match.StrokeMatcherContract,
    speaker: TtsSpeakerContract,
) {
    val flashColor = when (flashState) {
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
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PracticeTopBar(
            modifier = Modifier.fillMaxWidth(),
            hanzi = currentItem.char,
            phrase = uiState.currentPhrase,
            scaleValueProvider = { scale.value },
            onSpeak = { hanzi ->
                speaker.speakCharacterAndPhrase(hanzi, uiState.currentPhrase)
                triggerAnimation()
            },
            onExit = onExit
        )

        TraceCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Takes all remaining space to make tracing character as large as possible
            flashColor = flashColor,
            uiState = uiState,
            character = character,
            matcher = matcher,
            onStrokeEnd = { res -> onAction(PracticeAction.StrokeResult(res.isMatch)) },
        )
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
        modifier = modifier
            .claymorphism(
                backgroundColor = flashColor ?: MaterialTheme.colorScheme.surface,
                cornerRadius = 16.dp,
                elevation = 6.dp
            )
            .padding(4.dp), // Minimal padding so canvas can be as large as possible
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
private fun PracticeTopBar(
    modifier: Modifier = Modifier,
    hanzi: String,
    phrase: String,
    scaleValueProvider: () -> Float,
    onSpeak: (String) -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = modifier.claymorphism(
            backgroundColor = MaterialTheme.colorScheme.surface,
            cornerRadius = 24.dp,
            elevation = 4.dp
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: TTS Button
            IconButton(
                onClick = {
                    if (hanzi.isNotEmpty()) {
                        onSpeak(hanzi)
                    }
                },
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_volume),
                    contentDescription = "Speak",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            // Center: Phrase/Character
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (phrase.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val chars = buildList {
                            var i = 0
                            while (i < phrase.length) {
                                val cp = phrase.codePointAt(i)
                                add(String(Character.toChars(cp)))
                                i += Character.charCount(cp)
                            }
                        }
                        chars.forEach { charStr ->
                            val isTarget = charStr == hanzi
                            Text(
                                text = charStr,
                                fontSize = 50.sp,
                                fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (isTarget) androidx.compose.ui.text.style.TextDecoration.Underline else null,
                                modifier = Modifier.graphicsLayer {
                                    val currentScale = scaleValueProvider()
                                    if (isTarget) {
                                        scaleX = currentScale
                                        scaleY = currentScale
                                    }
                                },
                            )
                        }
                    }
                } else {
                    Text(
                        text = hanzi,
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Right: Exit Button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .claymorphism(
                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                        borderColor = MaterialTheme.colorScheme.error,
                        cornerRadius = 32.dp,
                        elevation = 4.dp
                    )
                    .clayClickable(onClick = onExit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Exit",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
