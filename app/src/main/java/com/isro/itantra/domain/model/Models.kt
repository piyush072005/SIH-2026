package com.isro.itantra.domain.model

enum class IndicLanguage(val code: String) {
    HINDI("hi"),
    GUJARATI("gu"),
    MARATHI("mr"),
    KANNADA("kn"),
    MALAYALAM("ml"),
    TAMIL("ta"),
    TELUGU("te"),
    ODIA("or"),
    BENGALI("bn"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String): IndicLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: HINDI
    }
}

enum class SessionMode {
    PushToTalk,
    Duplex,
    Alert,
}

enum class PlaybackKind {
    VoiceNote,
    Alert,
}

enum class PlaybackState {
    Idle,
    Synthesizing,
    Playing,
    Interrupted,
}

enum class AudioEngineState {
    Uninitialized,
    Ready,
    Capturing,
    Failed,
}

enum class LinkKind {
    WifiDirect,
    Bluetooth,
}

enum class LinkPhase {
    Idle,
    Discovering,
    Connecting,
    Connected,
    Reconnecting,
    Failed,
}

data class Peer(
    val id: String,
    val displayName: String,
    val kind: LinkKind,
    val addressHint: String? = null,
)

data class LinkState(
    val phase: LinkPhase = LinkPhase.Idle,
    val kind: LinkKind? = null,
    val peer: Peer? = null,
    val lastError: String? = null,
)

data class SttResult(
    val text: String,
    val languageCode: String,
    val isEndpoint: Boolean = true,
)

data class InboundMessage(
    val seq: Int,
    val text: String,
    val languageCode: String,
    val isAlert: Boolean,
)

data class OutboundMessage(
    val text: String,
    val languageCode: String,
    val isAlert: Boolean = false,
)

enum class SendStatus {
    Queued,
    Acked,
    Failed,
}

data class SendResult(
    val seq: Int,
    val status: SendStatus,
)
