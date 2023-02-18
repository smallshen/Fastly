package org.endoqa.fastly.protocol.packet.server.play

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.protocol.Position
import org.endoqa.fastly.util.protocolSizeInBytes

data class RespawnPacket(
    val dimensionType: String,
    val dimensionName: String,
    val hashedSeed: Long,
    val gameMode: UByte,
    val previousGameMode: UByte,
    val isDebug: Boolean,
    val isFlat: Boolean,
    val copyMetadata: Boolean,
    val hasDeathLocation: Boolean,
    val deathDimensionName: String?,
    val deathLocation: Position // will be Position(-1L) if hasDeathLocation is false
) : MinecraftPacket {

    override val handler: PacketHandler<RespawnPacket> = RespawnPacket


    companion object : PacketHandler<RespawnPacket>(0x3D) {

        fun fromJoinGamePacket(joinGamePacket: JoinGamePacket): RespawnPacket {
            return RespawnPacket(
                joinGamePacket.dimensionType,
                joinGamePacket.dimensionName,
                joinGamePacket.hashedSeed,
                joinGamePacket.gameMode,
                joinGamePacket.previousGameMode.toUByte(),
                joinGamePacket.isDebug,
                joinGamePacket.isFlat,
                false,
                joinGamePacket.hasDeathLocation,
                joinGamePacket.deathDimensionName,
                joinGamePacket.deathLocation
            )
        }

        override fun read(readable: DataReadable): RespawnPacket {
            return RespawnPacket(
                readable.readString(),
                readable.readString(),
                readable.readLong(),
                readable.readUnsignedByte(),
                readable.readUnsignedByte(),
                readable.readBoolean(),
                readable.readBoolean(),
                readable.readBoolean(),
                readable.readBoolean(),
                if (readable.readBoolean()) readable.readString() else null,
                if (readable.readBoolean()) Position(readable.readLong()) else Position(-1L)
            )
        }

        override fun write(writable: DataWritable, packet: RespawnPacket) {
            writable.writeString(packet.dimensionType)
            writable.writeString(packet.dimensionName)
            writable.writeLong(packet.hashedSeed)
            writable.writeUnsignedByte(packet.gameMode)
            writable.writeUnsignedByte(packet.previousGameMode)
            writable.writeBoolean(packet.isDebug)
            writable.writeBoolean(packet.isFlat)
            writable.writeBoolean(packet.copyMetadata)
            writable.writeBoolean(packet.hasDeathLocation)
            if (packet.hasDeathLocation) {
                writable.writeString(packet.deathDimensionName!!)
                writable.writeLong(packet.deathLocation.value)
            }
        }

    }

    override fun estimateSize(): Int {
        return (dimensionType.protocolSizeInBytes) +
                (dimensionName.protocolSizeInBytes) +
                8 + // hashedSeed
                1 + // gameMode
                1 + // previousGameMode
                1 + // isDebug
                1 + // isFlat
                1 + // copyMetadata
                1 + // hasDeathLocation
                if (hasDeathLocation) {
                    deathDimensionName!!.protocolSizeInBytes + 8
                } else {
                    0
                }

    }

}