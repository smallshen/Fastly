package org.endoqa.fastly.protocol.packet.server.login

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.player.Property
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.calculateVarIntSize
import org.endoqa.fastly.util.estimateStringSizeInBytes
import java.util.*

private val maxUsernameVarIntSize = calculateVarIntSize(16)

data class LoginSuccessPacket(
    val uuid: UUID,
    val username: String,
    val properties: List<Property>
) : MinecraftPacket {

    override val handler: PacketHandler<LoginSuccessPacket> = LoginSuccessPacket


    companion object : PacketHandler<LoginSuccessPacket>(0x02) {

        override fun read(readable: DataReadable): LoginSuccessPacket {
            val uuid = readable.readUUID()
            val username = readable.readString(16)
            val numProperties = readable.readVarInt()
            val properties = (0 until numProperties).map {
                Property(
                    name = readable.readString(32767),
                    value = readable.readString(32767),
                    signature = if (readable.readBoolean()) readable.readString(32767) else null
                )
            }

            return LoginSuccessPacket(uuid, username, properties)
        }

        override fun write(writable: DataWritable, packet: LoginSuccessPacket) {
            writable.writeUUID(packet.uuid)
            writable.writeString(packet.username)
            writable.writeVarInt(packet.properties.size)
            packet.properties.forEach {
                writable.writeString(it.name)
                writable.writeString(it.value)
                writable.writeBoolean(it.signature != null)
                it.signature?.let { sig -> writable.writeString(sig) }
            }
        }

    }

    override fun estimateSize(): Int {
        return 8 + 8 +
                maxUsernameVarIntSize +
                5 +
                properties.sumOf { (name, value, sig) ->
                    name.estimateStringSizeInBytes +
                            value.estimateStringSizeInBytes +
                            1 + (sig?.estimateStringSizeInBytes ?: 0)
                }

    }

}