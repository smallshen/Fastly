package org.endoqa.fastly.protocol.packet.server.login

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.estimateStringSizeInBytes

class LoginPluginRequestPacket(
    val messageId: Int,
    val channel: String,
    val data: ByteArray?
) : MinecraftPacket {

    override val handler: PacketHandler<LoginPluginRequestPacket> = LoginPluginRequestPacket

    companion object : PacketHandler<LoginPluginRequestPacket>(0x04) {
        override fun read(readable: DataReadable): LoginPluginRequestPacket {
            val messageId = readable.readVarInt()
            val channel = readable.readString()
            val data = readable.readRemaining()

            return LoginPluginRequestPacket(messageId, channel, data)
        }

        override fun write(writable: DataWritable, packet: LoginPluginRequestPacket) {
            writable.writeVarInt(packet.messageId)
            writable.writeString(packet.channel)
            if (packet.data != null) {
                writable.writeByteArray(packet.data)
            }
        }

    }


    override fun estimateSize(): Int {
        return 5 + channel.estimateStringSizeInBytes + (data?.size ?: 0)
    }

    override fun toString(): String {
        return "LoginPluginRequestPacket(messageId=$messageId, channel='$channel', data=${data?.contentToString()})"
    }


}