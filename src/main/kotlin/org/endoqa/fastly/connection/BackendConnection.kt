package org.endoqa.fastly.connection

import org.endoqa.fastly.BackendServer
import org.endoqa.fastly.nio.AsyncSocket
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel

class BackendConnection(val target: BackendServer) {

    val channel = AsynchronousSocketChannel.open() ?: error("Failed to open socket channel")
    lateinit var connection: Connection

    suspend fun connect() {
        val socket = AsyncSocket(channel)
        socket.connect(InetSocketAddress(target.address, target.port))
        connection = Connection(socket)
    }


}