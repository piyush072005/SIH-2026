package com.isro.itantra.domain.contracts.models

/**
 * Operational modes supported by the iTantra walkie-talkie system.
 */
enum class OperationalMode {
    /**
     * Half-duplex push-to-talk mode.
     * Mic is active only while the PTT trigger is actively held.
     */
    PushToTalk,

    /**
     * Full-duplex continuous voice communication with acoustic echo cancellation / VAD.
     */
    Duplex,

    /**
     * High-priority emergency broadcast mode.
     * Overrides normal traffic and triggers siren/visual alert on peer devices.
     */
    Alert
}
