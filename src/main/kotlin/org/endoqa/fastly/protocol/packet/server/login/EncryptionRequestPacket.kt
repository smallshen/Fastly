package org.endoqa.fastly.protocol.packet.server.login

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.calculateVarIntSize
import org.endoqa.fastly.util.estimateStringSizeInBytes

data class EncryptionRequestPacket(
    val serverID: String,
    val publicKey: ByteArray,
    val verifyToken: ByteArray
) : MinecraftPacket {
    override val handler: PacketHandler<EncryptionRequestPacket> = EncryptionRequestPacket


    companion object : PacketHandler<EncryptionRequestPacket>(0x01) {
        override fun read(readable: DataReadable): EncryptionRequestPacket {
            val serverID = readable.readString()
            val publicKey = readable.readByteArray(readable.readVarInt())
            val verifyToken = readable.readByteArray(readable.readVarInt())

            return EncryptionRequestPacket(serverID, publicKey, verifyToken)
        }

        override fun write(writable: DataWritable, packet: EncryptionRequestPacket) {
            writable.writeString(packet.serverID)

            writable.writeVarInt(packet.publicKey.size)
            writable.writeByteArray(packet.publicKey)

            writable.writeVarInt(packet.verifyToken.size)
            writable.writeByteArray(packet.verifyToken)
        }
    }

    override fun estimateSize(): Int {
        return serverID.estimateStringSizeInBytes +
                calculateVarIntSize(publicKey.size) + publicKey.size +
                calculateVarIntSize(verifyToken.size) + verifyToken.size
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptionRequestPacket

        if (serverID != other.serverID) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!verifyToken.contentEquals(other.verifyToken)) return false
        if (handler != other.handler) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serverID.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + verifyToken.contentHashCode()
        result = 31 * result + handler.hashCode()
        return result
    }

}