package org.endoqa.fastly.protocol.packet.client.handshake

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.calculateVarIntSize
import org.endoqa.fastly.util.estimateProtocolSizeInBytes

// now is around 700, hope no one use this in the future
private val maxProtocolVersionVarIntSize = calculateVarIntSize(1000)
private val maxPortVarIntSize = calculateVarIntSize(65535)
private const val NEXT_STATE_VARINT_SIZE = 1

data class HandshakePacket(
    val protocolVersion: Int,
    val serverAddress: String,
    val port: UShort,
    val nextState: Int
) : MinecraftPacket {
    override val handler: PacketHandler<HandshakePacket> = HandshakePacket

    companion object : PacketHandler<HandshakePacket>(0x00) {
        override fun read(readable: DataReadable): HandshakePacket {
            return HandshakePacket(
                readable.readVarInt(),
                readable.readString(),
                readable.readUnsignedShort(),
                readable.readVarInt()
            )
        }

        override fun write(writable: DataWritable, packet: HandshakePacket) {
            writable.writeVarInt(packet.protocolVersion)
            writable.writeString(packet.serverAddress)
            writable.writeUnsignedShort(packet.port)
            writable.writeVarInt(packet.nextState)
        }

    }


    override fun estimateSize(): Int {
        return maxProtocolVersionVarIntSize +
                serverAddress.estimateProtocolSizeInBytes +
                maxPortVarIntSize +
                NEXT_STATE_VARINT_SIZE

    }
}