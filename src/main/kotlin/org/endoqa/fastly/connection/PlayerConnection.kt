package org.endoqa.fastly.connection

import kotlinx.coroutines.*
import org.endoqa.fastly.BackendServer
import org.endoqa.fastly.nio.AsyncSocket
import org.endoqa.fastly.player.PlayerInfo
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel

class PlayerConnection(
    val connection: Connection,
) : CoroutineScope by connection {

    lateinit var playerInfo: PlayerInfo
    lateinit var backendConnection: Connection


    suspend fun connectToBackend(target: BackendServer) {


        if (this::backendConnection.isInitialized) {
            backendConnection.coroutineContext.cancelAndJoin()
        }

        val channel = withContext(Dispatchers.IO) {
            AsynchronousSocketChannel.open() ?: error("Failed to open socket channel")
        }

        val socket = AsyncSocket(channel)
        socket.connect(InetSocketAddress(target.address, target.port))
        val newBackend = Connection(socket, connection.coroutineContext)
        backendConnection = newBackend
    }


    fun packetProxy() {
        backendConnection.launch {
            try {
                while (isActive) {
                    val p = backendConnection.readRawPacket()
                    launch { connection.packetOut.send(p) }
                    yield()
                }
            } catch (e: Throwable) {
                println("Caught exception b2c: ${e.message}")
            }
        }

        backendConnection.launch {
            try {
                while (isActive) {
                    val p = connection.readRawPacket()
                    launch { backendConnection.packetOut.send(p) }
                    yield()
                }
            } catch (e: Throwable) {
                println("Caught exception c2b: ${e.message}")
            }
        }


    }


}