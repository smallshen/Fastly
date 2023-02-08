package org.endoqa.fastly

import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.protocol.packet.server.handshake.PongResponse
import org.endoqa.fastly.protocol.packet.server.handshake.StatusResponsePacket


suspend fun handleStatus(connection: Connection) {
    val packet = connection.packetIn.receive()
    val (_, packetId, _) = packet

    if (packetId == 0x00) {
        val p =
            StatusResponsePacket("{\n  \"version\": {\n    \"name\": \"1.19.3\",\n    \"protocol\": 761\n  },\n  \"players\": {\n    \"max\": 100,\n    \"online\": 5,\n    \"sample\": [\n      {\n        \"name\": \"thinkofdeath\",\n        \"id\": \"4566e69f-c907-48ee-8d71-d7ba5aa00d20\"\n      }\n    ]\n  },\n  \"description\": {\n    \"text\": \"Hello world\"\n  },\n  \"favicon\": \"data:image/png;base64,<data>\",\n  \"previewsChat\": true,\n  \"enforcesSecureChat\": true\n}")
        connection.sendPacket(p)
        handlePing(connection)

        return
    }

    if (packetId == 0x01) {
        connection.sendPacket(PongResponse(System.currentTimeMillis())).join()
    }

    //TODO: logging here
    connection.close()
    return

}

suspend fun handlePing(connection: Connection) {
    val packet = connection.packetIn.receive()

    if (packet.packetId != 0x01) {
        //TODO: logging here
        connection.close()
        return
    }

    connection.sendPacket(PongResponse(System.currentTimeMillis())).join()
    connection.close()
    return
}
