package com.hanzi.learner.speech.internal

internal sealed class TtsRequest {
    abstract val text: String

    data class Speak(override val text: String) : TtsRequest()

    data class SpeakCharacterAndPhrase(
        val character: String,
        val phrase: String,
    ) : TtsRequest() {
        override val text: String = "$character $phrase"
    }
}

internal class PendingRequestHandler {
    private var pendingRequest: TtsRequest? = null

    val hasPending: Boolean
        get() = pendingRequest != null

    fun enqueue(request: TtsRequest) {
        pendingRequest = request
    }

    fun dequeue(): TtsRequest? {
        val request = pendingRequest
        pendingRequest = null
        return request
    }

    fun peek(): TtsRequest? = pendingRequest

    fun clear() {
        pendingRequest = null
    }

    inline fun processIfReady(
        isReady: Boolean,
        processor: (TtsRequest) -> Unit,
    ) {
        if (isReady) {
            dequeue()?.let(processor)
        }
    }
}
