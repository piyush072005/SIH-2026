package com.isro.itantra.domain.contracts.models

/**
 * Lifecycle and activity state of the offline STT/TTS audio engine.
 */
sealed interface AudioEngineState {
    data object Uninitialized : AudioEngineState
    data object Initializing : AudioEngineState
    data object Ready : AudioEngineState
    data object Listening : AudioEngineState
    data class Speaking(val text: String) : AudioEngineState
    data class Error(val message: String, val cause: Throwable? = null) : AudioEngineState
}
