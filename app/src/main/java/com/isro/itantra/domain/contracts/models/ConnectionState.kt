package com.isro.itantra.domain.contracts.models

/**
 * Represents the connection state of the offline transport manager.
 */
sealed interface ConnectionState {
    /**
     * Transport is idle with no active connections, advertising, or discovery.
     */
    data object Disconnected : ConnectionState

    /**
     * Actively advertising presence to nearby peers.
     */
    data class Advertising(val deviceName: String) : ConnectionState

    /**
     * Actively scanning for nearby advertising peers.
     */
    data object Discovering : ConnectionState

    /**
     * Establishing a handshake or cryptographic session with a peer.
     */
    data class Connecting(val peerId: String, val peerName: String) : ConnectionState

    /**
     * Active peer-to-peer or mesh link established.
     */
    data class Connected(
        val peerId: String,
        val peerName: String,
        val connectedPeersCount: Int = 1
    ) : ConnectionState

    /**
     * Transport error state.
     */
    data class Error(val message: String, val errorCode: Int? = null) : ConnectionState
}
