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
import org.endoqa.fastly.protocol.packet.server.play.DisconnectPlayPacket
import org.endoqa.fastly.protocol.packet.server.play.JoinGamePacket
import org.endoqa.fastly.protocol.packet.server.play.RespawnPacket
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

    private var spawned = false


    suspend fun connectToBackend(target: BackendServer, server: FastlyServer) {

        val channel = withContext(Dispatchers.IO) {
            AsynchronousSocketChannel.open() ?: error("Failed to open socket channel")
        }

        val socket = AsyncSocket(channel)

        val backend = Connection(socket, connection.coroutineContext)

        connection.launch {
            try {
                backend.coroutineContext.join()
            } finally {
                backend.socket.close()
            }
        }

        socket.connect(InetSocketAddress(target.address, target.port))

        backendConnection = backend



        backend.startIO()

        backend.sendPacket(handshakePacket)
        backend.sendPacket(LoginStartPacket(profile.name, true, profile.uuid))


        if (server.online) {
            onlineModeHandshake(backend, server)
        }

        val loginSuccessRp = backend.readRawPacket()
        require(loginSuccessRp.packetId == 0x02) { "Expected login success packet, got ${loginSuccessRp.packetId}" }

        if (spawned) {
            val joinGame = backend.readRawPacket()
            require(joinGame.packetId == 0x24) { "Expected join game packet, got ${joinGame.packetId}" }

            val joinGamePacket = JoinGamePacket.read(ByteBuf(joinGame.buffer.position(0)))
            joinGame.buffer.position(0)

            connection.packetOut.send(joinGame)

            val respawn = RespawnPacket.fromJoinGamePacket(joinGamePacket)

            connection.sendPacket(respawn)
        } else {
            spawned = true
            connection.packetOut.send(loginSuccessRp)
        }
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

                    when (p.packetId) {
                        DisconnectPlayPacket.packetId -> {
                            //TODO: fallback handlers
                            throw Exception("Disconnected from backend")
                        }

                        else -> {
                            connection.packetOut.send(p)
                        }
                    }


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