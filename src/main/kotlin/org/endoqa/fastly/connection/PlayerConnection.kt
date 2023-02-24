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
import org.endoqa.fastly.protocol.packet.server.play.JoinGamePacket
import org.endoqa.fastly.protocol.packet.server.play.RespawnPacket
import org.endoqa.fastly.remoteAddressAsString
import org.tinylog.kotlin.Logger
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel

class PlayerConnection(
    val connection: Connection,
    val profile: GameProfile,
    private val handshakePacket: HandshakePacket,
) : CoroutineScope by connection {

    lateinit var backendConnection: Connection
        private set

    private var spawned = false

    init {
        Logger.info("${connection.socket.channel.remoteAddress} ${profile.name}(${profile.uuid}) logged in")
        launch {
            try {
                connection.coroutineContext.join()
            } finally {
                if (this@PlayerConnection::backendConnection.isInitialized) {
                    backendConnection.close()
                }
                Logger.info("${profile.name}(${profile.uuid}) logged out")
            }
        }
    }

    suspend fun connectToBackend(target: BackendServer, server: FastlyServer) {

        val channel = withContext(Dispatchers.IO) {
            AsynchronousSocketChannel.open() ?: error("Failed to open socket channel, returned null")
        }

        val socket = AsyncSocket(channel)

        val backend = Connection(socket, connection.coroutineContext)


        connection.launch {
            try {
                backend.coroutineContext.join()
            } finally {
                backend.close()
                Logger.debug("Backend connection closed for ${profile.name}(${profile.uuid})")
            }
        }

        socket.connect(InetSocketAddress(target.address, target.port))

        backendConnection = backend

        backend.sendPacket(handshakePacket)
        backend.sendPacket(LoginStartPacket(profile.name, true, profile.uuid))

        if (server.online) {
            onlineModeHandshake(backend, server)
        }

        val loginSuccessRp = backend.nextPacket()
        require(loginSuccessRp.packetId == 0x02) { "Expected login success packet, got ${loginSuccessRp.packetId}" }

        if (spawned) {
            val joinGame = backend.nextPacket()
            require(joinGame.packetId == 0x24) { "Expected join game packet, got ${joinGame.packetId}" }

            val joinGamePacket = JoinGamePacket.read(ByteBuf(joinGame.contentBuffer.position(0)))
            joinGame.contentBuffer.position(0)

            connection.writeRawPacket(joinGame)

            val respawn = RespawnPacket.fromJoinGamePacket(joinGamePacket)

            connection.sendPacket(respawn)
        } else {
            spawned = true
            connection.enableCompression(server.compressionThreshold)
            connection.writeRawPacket(loginSuccessRp)
        }
    }

    private suspend fun onlineModeHandshake(backend: Connection, server: FastlyServer) {


        val loginRequestRp = backend.nextPacket()
        require(loginRequestRp.packetId == LoginPluginRequestPacket.packetId) { "Expected login plugin response packet, got ${loginRequestRp.packetId}" }

        val loginRequest = LoginPluginRequestPacket.read(ByteBuf(loginRequestRp.contentBuffer.position(0)))

        val loginResponsePacket = LoginPluginResponsePacket(
            loginRequest.messageId,
            true,
            server.createForwardingData(connection.remoteAddressAsString(), profile)
        )

        backend.sendPacket(loginResponsePacket)
    }


    fun packetProxy() {

        val backendJob = backendConnection.coroutineContext

        backendConnection.launch {
            try {
                while (isActive) {
                    val p = backendConnection.readRawPacket()

                    connection.writeRawPacket(p)
//                    yield()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                backendJob.cancel("Canceled due to error", e)
            }
        }

        backendConnection.launch {
            try {
                while (isActive) {
                    backendConnection.writeRawPacket(connection.readRawPacket())
//                    yield()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                backendJob.cancel("Canceled due to error", e)
            }
        }


    }


}