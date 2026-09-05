package com.isro.itantra.domain.contracts

import com.isro.itantra.domain.contracts.models.AudioEngineState
import kotlinx.coroutines.flow.Flow

/**
 * Shared contract for offline speech-to-text (STT) and text-to-speech (TTS) audio operations.
 *
 * Implemented by Backend scope (`audio/`).
 * Consumed by UI scope (`ui/`).
 */
interface AudioEngineManager {
    /**
     * Emits real-time transcription segments and final utterances captured from the local microphone.
     */
    val transcribedText: Flow<String>

    /**
     * Emits the current lifecycle and activity state of the audio engine.
     */
    val audioState: Flow<AudioEngineState>

    /**
     * Emits whether the microphone is currently active and processing speech input.
     */
    val isListening: Flow<Boolean>

    /**
     * Initializes the offline STT and TTS models into memory.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Starts audio capture from the microphone and streams recognition results through [transcribedText].
     */
    fun startListening()

    /**
     * Stops audio capture and halts recognition streaming.
     */
    fun stopListening()

    /**
     * Synthesizes and plays back speech for the provided [text] offline.
     *
     * @param text The textual utterance to synthesize into speech.
     * @param languageCode ISO 639-1 / BCP-47 language tag (e.g., "hi", "en", "ta", "te").
     */
    suspend fun speak(text: String, languageCode: String = "hi"): Result<Unit>

    /**
     * Immediately aborts any ongoing TTS playback.
     */
    fun stopSpeaking()

    /**
     * Releases native model resources and audio buffers.
     */
    fun release()
}
