package org.endoqa.fastly.protocol.packet.server.login

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler

data class SetCompressionPacket(val threshold: Int) : MinecraftPacket {

    override val handler: PacketHandler<SetCompressionPacket> = SetCompressionPacket

    companion object : PacketHandler<SetCompressionPacket>(0x03) {
        override fun read(readable: DataReadable): SetCompressionPacket {
            return SetCompressionPacket(readable.readVarInt())
        }

        override fun write(writable: DataWritable, packet: SetCompressionPacket) {
            writable.writeVarInt(packet.threshold)
        }
    }

    override fun estimateSize(): Int {
        return 5
    }

}