package org.endoqa.fastly.protocol.packet.server.handshake

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler

data class PongResponse(val payload: Long) : MinecraftPacket {
    override val handler: PacketHandler<PongResponse> = PongResponse

    companion object : PacketHandler<PongResponse>(0x01) {
        override fun read(readable: DataReadable): PongResponse {
            return PongResponse(readable.readLong())
        }

        override fun write(writable: DataWritable, packet: PongResponse) {
            writable.writeLong(packet.payload)
        }

    }


    override fun estimateSize(): Int {
        return 8
    }
}