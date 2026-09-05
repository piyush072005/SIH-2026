package com.isro.itantra.domain.contracts.models

/**
 * Represents a nearby peer discovered via radio discovery (Nearby Connections / Wi-Fi Direct / BLE).
 */
data class DiscoveredPeer(
    val id: String,
    val name: String,
    val rssi: Int? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
