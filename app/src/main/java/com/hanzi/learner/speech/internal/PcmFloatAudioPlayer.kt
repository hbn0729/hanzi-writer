package com.hanzi.learner.speech.internal

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.hanzi.learner.speech.contract.AudioPlayerContract

private const val TAG = "PcmFloatAudioPlayer"

internal class PcmFloatAudioPlayer : AudioPlayerContract {
    private var audioTrack: AudioTrack? = null
    private var sampleRate: Int = 0

    override fun initialize(sampleRate: Int) {
        this.sampleRate = sampleRate
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        Log.d(TAG, "Initializing: sampleRate=$sampleRate, bufferSize=$bufferSize")

        val attributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .build()

        audioTrack = AudioTrack(
            attributes,
            format,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        Log.d(TAG, "AudioTrack created: $audioTrack")
        audioTrack?.play()
        Log.d(TAG, "AudioTrack playing")
    }

    override fun play(samples: FloatArray) {
        Log.d(TAG, "Playing ${samples.size} samples")
        
        if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
            Log.d(TAG, "Restarting playback")
            audioTrack?.play()
        }
        
        val shortSamples = ShortArray(samples.size) { i ->
            (samples[i] * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        val written = audioTrack?.write(shortSamples, 0, shortSamples.size, AudioTrack.WRITE_BLOCKING) ?: -1
        Log.d(TAG, "Written $written samples")
    }

    override fun stop() {
        Log.d(TAG, "Stopping")
        audioTrack?.stop()
        audioTrack?.reloadStaticData()
    }

    override fun release() {
        Log.d(TAG, "Releasing")
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
