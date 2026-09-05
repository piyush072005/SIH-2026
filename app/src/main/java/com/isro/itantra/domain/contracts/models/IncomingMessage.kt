package com.isro.itantra.domain.contracts.models

/**
 * Message received from a remote peer over the transport layer.
 */
data class IncomingMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val languageCode: String = "en",
    val isAlert: Boolean = false
)
