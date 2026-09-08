package com.isro.itantra.transport

import android.content.BroadcastReceiver
import com.isro.itantra.di.ApplicationScope
import com.isro.itantra.di.IoDispatcher
import com.isro.itantra.domain.contracts.TransportManager
import com.isro.itantra.domain.contracts.models.ConnectionState
import com.isro.itantra.domain.contracts.models.DiscoveredPeer
import com.isro.itantra.domain.contracts.models.IncomingMessage
import com.isro.itantra.domain.model.LinkKind
import com.isro.itantra.domain.model.Peer
import com.isro.itantra.transport.bluetooth.BluetoothLinkFactory
import com.isro.itantra.transport.protocol.Packet
import com.isro.itantra.transport.protocol.PacketType
import com.isro.itantra.transport.wifi.WifiDirectLinkFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransportManagerImpl @Inject constructor(
    private val wifi: WifiDirectLinkFactory,
    private val bluetooth: BluetoothLinkFactory,
    private val session: ReliableSocketSession,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TransportManager, LinkListener {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _receivedText = MutableSharedFlow<String>(extraBufferCapacity = 16)
    private val _incomingMessages = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 16)
    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())

    override val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()
    override val receivedText: Flow<String> = _receivedText.asSharedFlow()
    override val incomingMessages: Flow<IncomingMessage> = _incomingMessages.asSharedFlow()
    override val discoveredPeers: Flow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private var wifiReceiver: BroadcastReceiver? = null
    private var lastPeerId: String? = null
    private var advertisedName: String = "iTantra"
    private val peersById = LinkedHashMap<String, Peer>()

    init {
        session.listener = this
    }

    override fun startAdvertising(deviceName: String) {
        advertisedName = deviceName
        ensureWifiReceiver()
        _connectionState.value = ConnectionState.Advertising(deviceName)
        appScope.launch(io) {
            runCatching { wifi.createGroup() }
                .onFailure { err ->
                    _connectionState.value = ConnectionState.Error(err.message ?: "advertise_failed")
                }
        }
    }

    override fun stopAdvertising() {
        if (_connectionState.value is ConnectionState.Advertising) {
            _connectionState.value = ConnectionState.Disconnected
        }
        wifi.removeGroup()
        unregisterWifiReceiver()
    }

    override fun startDiscovery() {
        ensureWifiReceiver()
        _connectionState.value = ConnectionState.Discovering
        appScope.launch(io) {
            mergeBluetoothPeers()
            runCatching { wifi.discover() }
                .onFailure { err ->
                    _connectionState.value = ConnectionState.Error(err.message ?: "discover_failed")
                }
        }
    }

    override fun stopDiscovery() {
        if (_connectionState.value is ConnectionState.Discovering) {
            _connectionState.value = ConnectionState.Disconnected
        }
        unregisterWifiReceiver()
    }

    override fun connect(peerId: String) {
        unregisterWifiReceiver()
        val peer = peersById[peerId] ?: Peer(
            id = peerId,
            displayName = peerId,
            kind = if (peerId.startsWith("bt:")) LinkKind.Bluetooth else LinkKind.WifiDirect,
            addressHint = peerId.substringAfter(':'),
        )
        lastPeerId = peerId
        _connectionState.value = ConnectionState.Connecting(peer.id, peer.displayName)
        appScope.launch {
            runCatching {
                withContext(io) {
                    val link = openLink(peer)
                    session.attach(link)
                }
                _connectionState.value = ConnectionState.Connected(peer.id, peer.displayName)
            }.onFailure { err ->
                _connectionState.value = ConnectionState.Error(err.message ?: "connect_failed")
                reconnectWithFallback(peer)
            }
        }
    }

    override fun disconnect() {
        session.detach()
        lastPeerId = null
        unregisterWifiReceiver()
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun sendText(text: String): Result<Unit> = runCatching {
        session.sendReliable(PacketType.Data, "hi", text, isAlert = false)
        Unit
    }

    override suspend fun broadcastAlert(alertMessage: String): Result<Unit> = runCatching {
        session.sendReliable(PacketType.Alert, "en", alertMessage, isAlert = true)
        Unit
    }

    override fun onPacket(packet: Packet) {
        if (packet.type == PacketType.Heartbeat || packet.type == PacketType.Ack) return
        val connected = _connectionState.value as? ConnectionState.Connected
        val message = IncomingMessage(
            id = packet.seq.toString(),
            senderId = connected?.peerId ?: "unknown",
            senderName = connected?.peerName ?: "peer",
            text = packet.payloadUtf8,
            languageCode = packet.languageCode.ifBlank { "hi" },
            isAlert = packet.type == PacketType.Alert,
        )
        appScope.launch {
            _incomingMessages.emit(message)
            _receivedText.emit(packet.payloadUtf8)
        }
    }

    override fun onDisconnected(reason: String) {
        _connectionState.value = ConnectionState.Error(reason)
        val peerId = lastPeerId ?: return
        connect(peerId)
    }

    private fun ensureWifiReceiver() {
        if (wifiReceiver != null) return
        val receiver = wifi.peerListReceiver { discovered ->
            discovered.forEach { peersById[it.id] = it }
            publishPeers()
        }
        wifi.register(receiver)
        wifiReceiver = receiver
    }

    private fun unregisterWifiReceiver() {
        wifiReceiver?.let { receiver ->
            wifi.unregister(receiver)
            wifiReceiver = null
        }
    }

    private fun mergeBluetoothPeers() {
        bluetooth.bondedPeers().forEach { peersById[it.id] = it }
        publishPeers()
    }

    private fun publishPeers() {
        _discoveredPeers.update {
            peersById.values.map { peer ->
                DiscoveredPeer(id = peer.id, name = peer.displayName)
            }
        }
    }

    private suspend fun openLink(peer: Peer): ByteLink {
        return when (peer.kind) {
            LinkKind.WifiDirect -> {
                try {
                    wifi.connect(peer)
                } catch (t: Throwable) {
                    val fallback = bluetooth.bondedPeers().firstOrNull() ?: throw t
                    bluetooth.connect(fallback)
                }
            }
            LinkKind.Bluetooth -> bluetooth.connect(peer)
        }
    }

    private fun reconnectWithFallback(failed: Peer) {
        if (failed.kind != LinkKind.WifiDirect) return
        val fallback = bluetooth.bondedPeers().firstOrNull() ?: return
        connect(fallback.id)
    }
}
