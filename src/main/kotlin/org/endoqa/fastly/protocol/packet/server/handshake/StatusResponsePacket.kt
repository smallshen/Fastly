package org.endoqa.fastly.protocol.packet.server.handshake

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.estimateProtocolSizeInBytes

data class StatusResponsePacket(val response: String) : MinecraftPacket {
    override val handler: PacketHandler<StatusResponsePacket> = StatusResponsePacket

    companion object : PacketHandler<StatusResponsePacket>(0x00) {

        override fun read(readable: DataReadable): StatusResponsePacket {
            return StatusResponsePacket(readable.readString())
        }

        override fun write(writable: DataWritable, packet: StatusResponsePacket) {
            writable.writeString(packet.response)
        }

    }


    override fun estimateSize(): Int {
        return response.estimateProtocolSizeInBytes
    }
}