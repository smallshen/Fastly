package org.endoqa.fastly

import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.connection.PlayerConnection
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.player.GameProfile
import org.endoqa.fastly.protocol.packet.client.handshake.HandshakePacket
import org.endoqa.fastly.protocol.packet.client.login.LoginStartPacket
import java.util.*

suspend fun FastlyServer.handleOfflineLogin(
    connection: Connection,
    handshakePacket: HandshakePacket
): PlayerConnection {

    val rp = connection.readRawPacket()
    require(rp.packetId == 0x00) { "Expected login packet, got ${rp.packetId}" }

    val packet = LoginStartPacket.read(ByteBuf(rp.buffer.position(0)))

    val playerConnection = PlayerConnection(connection, handshakePacket)

    val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:${packet.name}".toByteArray())
    playerConnection.profile = GameProfile(packet.name, uuid.toString(), emptyList())

    playerConnection.connectToBackend(backendServers.first(), this)

    return playerConnection
}