package com.hanzi.learner.speech.model

/**
 * Represents the download/installation status of a TTS model.
 */
enum class TtsModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    ACTIVE,
}