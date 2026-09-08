package com.isro.itantra.transport.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import com.isro.itantra.domain.model.LinkKind
import com.isro.itantra.domain.model.Peer
import com.isro.itantra.transport.ByteLink
import com.isro.itantra.transport.SocketByteLink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class WifiDirectLinkFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? =
        manager?.initialize(context, Looper.getMainLooper(), null)

    @SuppressLint("MissingPermission")
    fun peerListReceiver(onPeers: (List<Peer>) -> Unit): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) return
                val mgr = manager ?: return
                val ch = channel ?: return
                mgr.requestPeers(ch) { list ->
                    onPeers(
                        list.deviceList.map { device ->
                            Peer(
                                id = "wifi:${device.deviceAddress}",
                                displayName = device.deviceName ?: device.deviceAddress,
                                kind = LinkKind.WifiDirect,
                                addressHint = device.deviceAddress,
                            )
                        },
                    )
                }
            }
        }
    }

    fun peerFilter(): IntentFilter = IntentFilter(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

    @SuppressLint("MissingPermission")
    suspend fun discover() {
        val mgr = manager ?: error("WifiP2pManager unavailable")
        val ch = channel ?: error("WifiP2p channel unavailable")
        suspendCancellableCoroutine { cont ->
            mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onFailure(reason: Int) {
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("discover failed $reason"))
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun createGroup() {
        val mgr = manager ?: error("WifiP2pManager unavailable")
        val ch = channel ?: error("WifiP2p channel unavailable")
        suspendCancellableCoroutine { cont ->
            mgr.createGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onFailure(reason: Int) {
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("group creation failed $reason"))
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun removeGroup() {
        val mgr = manager ?: return
        val ch = channel ?: return
        mgr.removeGroup(ch, null)
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(peer: Peer): ByteLink {
        val mgr = manager ?: error("WifiP2pManager unavailable")
        val ch = channel ?: error("WifiP2p channel unavailable")
        val address = peer.addressHint ?: peer.id.removePrefix("wifi:")
        suspendCancellableCoroutine { cont ->
            val config = WifiP2pConfig().apply { deviceAddress = address }
            mgr.connect(ch, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onFailure(reason: Int) {
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("p2p connect failed $reason"))
                }
            })
        }
        return openSockets(mgr, ch)
    }

    @SuppressLint("MissingPermission")
    private suspend fun openSockets(mgr: WifiP2pManager, ch: WifiP2pManager.Channel): ByteLink {
        val info = suspendCancellableCoroutine { cont ->
            mgr.requestConnectionInfo(ch) { connection ->
                if (cont.isActive) cont.resume(connection)
            }
        }
        val groupOwner = info.isGroupOwner
        val ownerAddress = info.groupOwnerAddress?.hostAddress
            ?: error("no group owner address")
        return if (groupOwner) {
            val server = ServerSocket(PORT)
            server.soTimeout = 20_000
            val client = server.accept()
            server.close()
            SocketByteLink(client)
        } else {
            val socket = Socket()
            socket.connect(InetSocketAddress(ownerAddress, PORT), 15_000)
            SocketByteLink(socket)
        }
    }

    fun register(receiver: BroadcastReceiver) {
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, peerFilter(), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, peerFilter())
        }
    }

    fun unregister(receiver: BroadcastReceiver) {
        runCatching { context.unregisterReceiver(receiver) }
    }

    companion object {
        const val PORT: Int = 18_991
    }
}
