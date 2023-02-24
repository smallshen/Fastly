package org.endoqa.fastly

import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.connection.PlayerConnection
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.player.GameProfile
import org.endoqa.fastly.protocol.packet.client.handshake.HandshakePacket
import org.endoqa.fastly.protocol.packet.client.login.LoginStartPacket
import java.util.*

/**
 * Handle offline login
 *
 * Disclaimer: support offline is because proxy-behind-proxy, not break EULA
 *
 * @param connection
 * @param handshakePacket
 * @return
 */
suspend fun FastlyServer.handleOfflineLogin(
    connection: Connection,
    handshakePacket: HandshakePacket
): PlayerConnection {

    val rp = connection.nextPacket()
    require(rp.packetId == 0x00) { "Expected login packet, got ${rp.packetId}" }

    val packet = LoginStartPacket.read(ByteBuf(rp.contentBuffer.position(0)))

    val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:${packet.name}".toByteArray())
    val profile = GameProfile(packet.name, uuid.toString().replace("-", ""), emptyList())

    val playerConnection = PlayerConnection(connection, profile, handshakePacket)


    playerConnection.connectToBackend(backendServers.first(), this)

    return playerConnection
}