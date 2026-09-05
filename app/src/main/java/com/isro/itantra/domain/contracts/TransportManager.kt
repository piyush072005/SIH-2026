package com.isro.itantra.domain.contracts

import com.isro.itantra.domain.contracts.models.ConnectionState
import com.isro.itantra.domain.contracts.models.DiscoveredPeer
import com.isro.itantra.domain.contracts.models.IncomingMessage
import kotlinx.coroutines.flow.Flow

/**
 * Shared contract for offline peer-to-peer and mesh communication.
 *
 * Implemented by Backend scope (`transport/`).
 * Consumed by UI scope (`ui/`).
 */
interface TransportManager {
    /**
     * Emits the current connectivity state of the transport subsystem.
     */
    val connectionState: Flow<ConnectionState>

    /**
     * Emits plain text messages received from remote connected peers.
     */
    val receivedText: Flow<String>

    /**
     * Emits structured incoming messages containing sender metadata, timestamp, and alert status.
     */
    val incomingMessages: Flow<IncomingMessage>

    /**
     * Emits the list of currently visible nearby peers discovered over local radio.
     */
    val discoveredPeers: Flow<List<DiscoveredPeer>>

    /**
     * Begins advertising device availability to nearby peers over local radio.
     *
     * @param deviceName Human-readable callsign/identifier broadcasted to peers.
     */
    fun startAdvertising(deviceName: String)

    /**
     * Halts advertising device presence.
     */
    fun stopAdvertising()

    /**
     * Begins scanning for advertising nearby peers.
     */
    fun startDiscovery()

    /**
     * Halts scanning for nearby peers.
     */
    fun stopDiscovery()

    /**
     * Initiates a connection handshake with a discovered peer.
     *
     * @param peerId Unique identifier of the discovered peer.
     */
    fun connect(peerId: String)

    /**
     * Disconnects from the current peer or teardown the active mesh cluster.
     */
    fun disconnect()

    /**
     * Broadcasts or sends text data across the active connection.
     *
     * @param text The message string to transmit.
     * @return Result indicating whether the payload was successfully enqueued/transmitted.
     */
    suspend fun sendText(text: String): Result<Unit>

    /**
     * Broadcasts an emergency alert payload across all connected channels with high priority.
     *
     * @param alertMessage Optional text description accompanying the alert broadcast.
     */
    suspend fun broadcastAlert(alertMessage: String): Result<Unit>
}
