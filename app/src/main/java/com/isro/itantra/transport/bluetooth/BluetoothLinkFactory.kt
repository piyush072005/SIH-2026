package com.isro.itantra.transport.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.isro.itantra.domain.model.LinkKind
import com.isro.itantra.domain.model.Peer
import com.isro.itantra.transport.ByteLink
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothLinkFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    @SuppressLint("MissingPermission")
    fun bondedPeers(): List<Peer> {
        return adapter?.bondedDevices.orEmpty().map { device ->
            Peer(
                id = "bt:${device.address}",
                displayName = device.name ?: device.address,
                kind = LinkKind.Bluetooth,
                addressHint = device.address,
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(peer: Peer): ByteLink {
        val address = peer.addressHint ?: peer.id.removePrefix("bt:")
        val device = adapter?.getRemoteDevice(address) ?: error("Bluetooth adapter missing")
        val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        adapter.cancelDiscovery()
        socket.connect()
        return BluetoothByteLink(socket)
    }

    @SuppressLint("MissingPermission")
    fun listen(): ByteLink {
        val adapter = adapter ?: error("Bluetooth adapter missing")
        val server = adapter.listenUsingRfcommWithServiceRecord("iTantra", SPP_UUID)
        val socket = server.accept()
        server.close()
        return BluetoothByteLink(socket)
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("8e1f0a20-6c3a-4d7e-9c11-a1b2c3d4e5f6")
    }
}

private class BluetoothByteLink(private val socket: BluetoothSocket) : ByteLink {
    override val isOpen: Boolean get() = socket.isConnected
    override fun output(): OutputStream = socket.outputStream
    override fun input(): InputStream = socket.inputStream
    override fun close() {
        runCatching { socket.close() }
    }
}
