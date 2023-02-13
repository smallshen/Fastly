package org.endoqa.fastly

import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.connection.PlayerConnection
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.player.PlayerInfo
import org.endoqa.fastly.protocol.packet.client.handshake.HandshakePacket
import org.endoqa.fastly.protocol.packet.client.login.LoginStartPacket
import java.util.*

suspend fun FastlyServer.handleOfflineLogin(connection: Connection, handshakePacket: HandshakePacket) {

    val rp = connection.readRawPacket()
    require(rp.packetId == 0x00) { "Expected login packet, got ${rp.packetId}" }

    val packet = LoginStartPacket.read(ByteBuf(rp.buffer.position(0)))


    val playerConnection = PlayerConnection(connection)
    playerConnection.connectToBackend(backendServers.first()) //TODO: easy to tell it is todo


    val backend = playerConnection.backendConnection
    backend.startIO()

    backend.sendPacket(handshakePacket)

    val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:${packet.name}".toByteArray())

    val loginStartPacket = LoginStartPacket(
        name = packet.name,
        hasPlayerUUID = true,
        playerUUID = uuid,

        )
    backend.sendPacket(loginStartPacket)
    playerConnection.playerInfo = PlayerInfo(uuid, packet.name)

    playerConnection.packetProxy()

}