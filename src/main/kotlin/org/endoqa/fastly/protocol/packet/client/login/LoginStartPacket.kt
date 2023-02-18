package org.endoqa.fastly.protocol.packet.client.login

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.estimateProtocolSizeInBytes
import java.util.*

data class LoginStartPacket(
    val name: String,
    val hasPlayerUUID: Boolean,
    val playerUUID: UUID?
) : MinecraftPacket {

    override val handler: PacketHandler<LoginStartPacket> = LoginStartPacket

    companion object : PacketHandler<LoginStartPacket>(0x00) {
        override fun read(readable: DataReadable): LoginStartPacket {
            val name = readable.readString(16)
            val hasUUID = readable.readBoolean()
            val uuid = if (hasUUID) readable.readUUID() else null

            return LoginStartPacket(name, hasUUID, uuid)
        }

        override fun write(writable: DataWritable, packet: LoginStartPacket) {
            writable.writeString(packet.name)
            writable.writeBoolean(packet.hasPlayerUUID)
            if (packet.hasPlayerUUID) {
                writable.writeUUID(packet.playerUUID!!)
            }
        }

    }

    override fun estimateSize(): Int {
        return name.estimateProtocolSizeInBytes +
                1 +
                if (hasPlayerUUID) (2 * 8) else 0
    }


}