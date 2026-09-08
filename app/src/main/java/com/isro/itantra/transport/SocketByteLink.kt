package com.isro.itantra.transport

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class SocketByteLink(private val socket: Socket) : ByteLink {
    override val isOpen: Boolean get() = socket.isConnected && !socket.isClosed
    override fun output(): OutputStream = socket.getOutputStream()
    override fun input(): InputStream = socket.getInputStream()
    override fun close() {
        runCatching { socket.close() }
    }
}
