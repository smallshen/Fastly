package org.endoqa.fastly

import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.protocol.packet.client.handshake.HandshakePacket

/**
 * @return true, if next is 2(login)
 */
internal suspend fun FastlyServer.handleHandshake(connection: Connection): HandshakePacket? {

    val p = connection.readRawPacket()

    if (p.packetId != 0x00) {
        //TODO: logging here
        connection.close()
        return null
    }

    val packet = HandshakePacket.read(ByteBuf(p.buffer.position(0)))
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