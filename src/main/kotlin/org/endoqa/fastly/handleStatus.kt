package org.endoqa.fastly

import kotlinx.coroutines.withTimeout
import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.exception.MalformedException
import org.endoqa.fastly.protocol.packet.server.handshake.PongResponse
import org.endoqa.fastly.protocol.packet.server.handshake.StatusResponsePacket
import org.tinylog.kotlin.Logger
import kotlin.time.Duration.Companion.seconds


suspend fun handleStatus(connection: Connection) {
    val packet = withTimeout(30.seconds) {
        connection.readRawPacket()
    }
    val (_, packetId, _) = packet

    when (packetId) {
        0x00 -> {
            val p =
                StatusResponsePacket("{\n  \"version\": {\n    \"name\": \"1.19.3\",\n    \"protocol\": 761\n  },\n  \"players\": {\n    \"max\": 100,\n    \"online\": 5,\n    \"sample\": [\n      {\n        \"name\": \"thinkofdeath\",\n        \"id\": \"4566e69f-c907-48ee-8d71-d7ba5aa00d20\"\n      }\n    ]\n  },\n  \"description\": {\n    \"text\": \"Hello world\"\n  },\n  \"favicon\": \"data:image/png;base64,<data>\",\n  \"previewsChat\": true,\n  \"enforcesSecureChat\": true\n}")
            connection.sendPacket(p)
            handlePing(connection)
        }

        0x01 -> connection.sendPacket(PongResponse(System.currentTimeMillis()))
        else -> {
            Logger.error("${connection.socket.channel.remoteAddress} sent invalid packet id during status")
        }
    }


    connection.close()
    return

}

suspend fun handlePing(connection: Connection) {
    val packet = withTimeout(30.seconds) {
        connection.readRawPacket()
    }

    if (packet.packetId != 0x01) {
        Logger.error("${connection.socket.channel.remoteAddress} sent invalid packet id during ping")
        throw MalformedException("Invalid packet id: ${packet.packetId} (expected 0x01)")
    }

    connection.sendPacket(PongResponse(System.currentTimeMillis()))
    connection.close()
    return
}
