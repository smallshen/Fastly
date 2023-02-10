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
        val newBackend = Connection(socket)
        backendConnection = newBackend


        newBackend.launch {
            connection.coroutineContext.join()
            newBackend.close()
        }

    }

    fun packetProxy() {
        backendConnection.launch {
            launch {
                for (p in connection.packetIn) {
                    launch { backendConnection.packetOut.send(p) }
                }
            }

            launch {
                for (p in backendConnection.packetIn) {
                    launch { connection.packetOut.send(p) }
                }
            }


        }
    }


}