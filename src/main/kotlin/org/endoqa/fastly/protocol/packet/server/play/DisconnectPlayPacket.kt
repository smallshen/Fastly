package org.endoqa.fastly.protocol.packet.server.play

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.estimateProtocolSizeInBytes

class DisconnectPlayPacket(val reason: String) : MinecraftPacket {

    override val handler: PacketHandler<DisconnectPlayPacket> = DisconnectPlayPacket

    companion object : PacketHandler<DisconnectPlayPacket>(0x17) {
        override fun read(readable: DataReadable): DisconnectPlayPacket {
            return DisconnectPlayPacket(readable.readString())
        }

        override fun write(writable: DataWritable, packet: DisconnectPlayPacket) {
            writable.writeString(packet.reason)
        }
    }

    override fun estimateSize(): Int {
        return reason.estimateProtocolSizeInBytes
    }
}