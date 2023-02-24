package org.endoqa.fastly

import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.exception.MalformedException
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.protocol.packet.client.handshake.HandshakePacket
import org.tinylog.kotlin.Logger

/**
 * @return true, if next is 2(login)
 */
internal suspend fun FastlyServer.handleHandshake(connection: Connection): HandshakePacket? {

    val p = connection.nextPacket()

    if (p.packetId != 0x00) {
        Logger.error("${connection.socket.channel.remoteAddress} sent invalid packet id during handshake")
        throw MalformedException("Invalid packet id: ${p.packetId} (expected 0x00)")
    }

    val packet = HandshakePacket.read(ByteBuf(p.contentBuffer.position(0)))
    val (_, _, _, nextState) = packet

    return when (nextState) {
        // status
        1 -> {
            handleStatus(connection)
            null
        }

        // login
        2 -> packet

        else -> {
            connection.close()
            null
        }
    }


}