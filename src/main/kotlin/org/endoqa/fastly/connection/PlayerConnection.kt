package org.endoqa.fastly.connection

import kotlinx.coroutines.*
import org.endoqa.fastly.BackendServer
import org.endoqa.fastly.FastlyServer
import org.endoqa.fastly.createForwardingData
import org.endoqa.fastly.nio.AsyncSocket
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.player.GameProfile
import org.endoqa.fastly.protocol.packet.client.handshake.HandshakePacket
import org.endoqa.fastly.protocol.packet.client.login.LoginPluginResponsePacket
import org.endoqa.fastly.protocol.packet.client.login.LoginStartPacket
import org.endoqa.fastly.protocol.packet.server.login.LoginPluginRequestPacket
import org.endoqa.fastly.remoteAddressAsString
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel

class PlayerConnection(
    val connection: Connection,
    private val handshakePacket: HandshakePacket,
) : CoroutineScope by connection {

    lateinit var profile: GameProfile
    lateinit var backendConnection: Connection
        private set


    suspend fun connectToBackend(target: BackendServer, server: FastlyServer) {

        val channel = withContext(Dispatchers.IO) {
            AsynchronousSocketChannel.open() ?: error("Failed to open socket channel")
        }

        val socket = AsyncSocket(channel)
        socket.connect(InetSocketAddress(target.address, target.port))
        val backend = Connection(socket, connection.coroutineContext)
        backendConnection = backend

        backend.startIO()

        backend.sendPacket(handshakePacket)
        backend.sendPacket(LoginStartPacket(profile.name, true, profile.uuid))


        if (server.online) {
            onlineModeHandshake(backend, server)
        }

        val loginSuccessRp = backend.readRawPacket()
        require(loginSuccessRp.packetId == 0x02) { "Expected login success packet, got ${loginSuccessRp.packetId}" }

        connection.packetOut.send(loginSuccessRp)
    }

    private suspend fun onlineModeHandshake(backend: Connection, server: FastlyServer) {


        val loginRequestRp = backend.readRawPacket()
        require(loginRequestRp.packetId == LoginPluginRequestPacket.packetId) { "Expected login plugin response packet, got ${loginRequestRp.packetId}" }

        val loginRequest = LoginPluginRequestPacket.read(ByteBuf(loginRequestRp.buffer.position(0)))

        val loginResponsePacket = LoginPluginResponsePacket(
            loginRequest.messageId,
            true,
            server.createForwardingData(connection.remoteAddressAsString(), profile)
        )

        backend.sendPacket(loginResponsePacket)
    }


    fun packetProxy() {
        backendConnection.launch {
            try {
                while (isActive) {
                    val p = backendConnection.readRawPacket()
                    connection.packetOut.send(p)
                    yield()
                }
            } catch (e: Exception) {
                coroutineContext.job.cancel()
            }
        }

        backendConnection.launch {
            try {
                while (isActive) {
                    val p = connection.readRawPacket()
                    backendConnection.packetOut.send(p)
                    yield()
                }
            } catch (e: Exception) {
                coroutineContext.job.cancel()
            }
        }


    }


}