package com.isro.itantra.transport

import com.isro.itantra.transport.protocol.Packet
import java.io.InputStream
import java.io.OutputStream

interface ByteLink {
    val isOpen: Boolean
    fun output(): OutputStream
    fun input(): InputStream
    fun close()
}

interface LinkListener {
    fun onPacket(packet: Packet)
    fun onDisconnected(reason: String)
}
