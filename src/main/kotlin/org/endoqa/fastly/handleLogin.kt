package org.endoqa.fastly

import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.connection.PlayerConnection
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.player.PlayerInfo
import org.endoqa.fastly.protocol.packet.client.handshake.HandshakePacket
import org.endoqa.fastly.protocol.packet.client.login.LoginStartPacket
import java.util.*

suspend fun FastlyServer.handleLogin(connection: Connection, handshakePacket: HandshakePacket): Unit {

    val rp = connection.packetIn.receive()

    require(rp.packetId == 0x00) { "Expected login packet, got ${rp.packetId}" }

    val packet = LoginStartPacket.read(ByteBuf(rp.buffer.position(0)))

    val playerInfo = PlayerInfo(
        uuid = if (online) packet.playerUUID
            ?: error("Player UUID is null") else UUID.nameUUIDFromBytes("OfflinePlayer:${packet.name}".toByteArray()),
        name = packet.name
    )

    val playerConnection = PlayerConnection(playerInfo, connection)
    playerConnection.connectToBackend(backendServers.first()) //TODO: easy to tell it is todo


    val backend = playerConnection.backendConnection
    backend.startIO()

    backend.sendPacket(handshakePacket)
    val loginStartPacket = LoginStartPacket(playerInfo.name, true, playerInfo.uuid)
    backend.sendPacket(loginStartPacket)

    playerConnection.packetProxy()

}