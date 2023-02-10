package org.endoqa.fastly.protocol.packet.server.login

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.estimateStringSizeInBytes

class DisconnectPacket(val reason: String) : MinecraftPacket {

    override val handler: PacketHandler<DisconnectPacket> = DisconnectPacket

    companion object : PacketHandler<DisconnectPacket>(0x00) {
        override fun read(readable: DataReadable): DisconnectPacket {
            return DisconnectPacket(readable.readString())
        }

        override fun write(writable: DataWritable, packet: DisconnectPacket) {
            writable.writeString(packet.reason)
        }
    }

    override fun estimateSize(): Int {
        return reason.estimateStringSizeInBytes
    }

}