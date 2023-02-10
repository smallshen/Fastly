package org.endoqa.fastly.protocol.packet.client.login

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler

class EncryptionResponsePacket(
    val sharedSecret: ByteArray,
    val verifyToken: ByteArray
) : MinecraftPacket {

    override val handler: PacketHandler<MinecraftPacket> = EncryptionResponsePacket

    companion object : PacketHandler<EncryptionResponsePacket>(0x01) {
        override fun read(readable: DataReadable): EncryptionResponsePacket {
            val sharedSecret = readable.readByteArray(readable.readVarInt())
            val verifyToken = readable.readByteArray(readable.readVarInt())

            return EncryptionResponsePacket(sharedSecret, verifyToken)
        }

        override fun write(writable: DataWritable, packet: EncryptionResponsePacket) {
            writable.writeVarInt(packet.sharedSecret.size)
            writable.writeByteArray(packet.sharedSecret)

            writable.writeVarInt(packet.verifyToken.size)
            writable.writeByteArray(packet.verifyToken)
        }
    }

    override fun estimateSize(): Int {
        return 5 + sharedSecret.size +
                5 + verifyToken.size
    }
}