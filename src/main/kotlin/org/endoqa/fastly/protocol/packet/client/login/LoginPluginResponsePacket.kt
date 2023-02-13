package org.endoqa.fastly.protocol.packet.client.login

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler

class LoginPluginResponsePacket(
    val messageId: Int,
    val successful: Boolean,
    val data: ByteArray
) : MinecraftPacket {

    override val handler: PacketHandler<LoginPluginResponsePacket> = LoginPluginResponsePacket

    companion object : PacketHandler<LoginPluginResponsePacket>(0x02) {
        override fun read(readable: DataReadable): LoginPluginResponsePacket {
            val messageId = readable.readVarInt()
            val successful = readable.readBoolean()
            val data = readable.readRemaining()

            return LoginPluginResponsePacket(messageId, successful, data)
        }

        override fun write(writable: DataWritable, packet: LoginPluginResponsePacket) {
            writable.writeVarInt(packet.messageId)
            writable.writeBoolean(packet.successful)
            writable.writeByteArray(packet.data)
        }

    }

    override fun estimateSize(): Int {
        return 5 + 1 + data.size
    }

}