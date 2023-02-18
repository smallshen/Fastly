package org.endoqa.fastly.protocol.packet.server.play

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.util.protocol.Position
import org.endoqa.fastly.util.protocol.nbt.NBT
import org.endoqa.fastly.util.protocolSizeInBytes

class JoinGamePacket(
    val entityId: Int,
    val isHardcore: Boolean,
    val gameMode: UByte,
    val previousGameMode: Byte,
    val dimensions: List<String>,
    val dimensionCodec: NBT,
    val dimensionType: String,
    val dimensionName: String,
    val hashedSeed: Long,
    val maxPlayers: Int,
    val viewDistance: Int,
    val simulationDistance: Int,
    val reducedDebugInfo: Boolean,
    val enableRespawnScreen: Boolean,
    val isDebug: Boolean,
    val isFlat: Boolean,
    val hasDeathLocation: Boolean,
    val deathDimensionName: String?,
    val deathLocation: Position // will be Position(-1L) if hasDeathLocation is false
) : MinecraftPacket {

    override val handler: PacketHandler<JoinGamePacket> = JoinGamePacket

    companion object : PacketHandler<JoinGamePacket>(0x24, true) {
        override fun read(readable: DataReadable): JoinGamePacket {
            var hasDeathLocation: Boolean

            return JoinGamePacket(
                entityId = readable.readInt(),
                isHardcore = readable.readBoolean(),
                gameMode = readable.readUnsignedByte(),
                previousGameMode = readable.readByte(),
                dimensions = List(readable.readVarInt()) { readable.readString() },
                dimensionCodec = readable.readNBT(),
                dimensionType = readable.readString(),
                dimensionName = readable.readString(),
                hashedSeed = readable.readLong(),
                maxPlayers = readable.readVarInt(),
                viewDistance = readable.readVarInt(),
                simulationDistance = readable.readVarInt(),
                reducedDebugInfo = readable.readBoolean(),
                enableRespawnScreen = readable.readBoolean(),
                isDebug = readable.readBoolean(),
                isFlat = readable.readBoolean(),
                hasDeathLocation = readable.readBoolean().also { hasDeathLocation = it },
                deathDimensionName = if (hasDeathLocation) readable.readString() else null,
                deathLocation = if (hasDeathLocation) Position(readable.readLong()) else Position(-1L)
            )
        }

        override fun write(writable: DataWritable, packet: JoinGamePacket) {
            writable.writeInt(packet.entityId)
            writable.writeBoolean(packet.isHardcore)
            writable.writeUnsignedByte(packet.gameMode)
            writable.writeByte(packet.previousGameMode)
            writable.writeVarInt(packet.dimensions.size)
            packet.dimensions.forEach { writable.writeString(it) }
            writable.writeNBT(packet.dimensionCodec)
            writable.writeString(packet.dimensionType)
            writable.writeString(packet.dimensionName)
            writable.writeLong(packet.hashedSeed)
            writable.writeVarInt(packet.maxPlayers)
            writable.writeVarInt(packet.viewDistance)
            writable.writeVarInt(packet.simulationDistance)
            writable.writeBoolean(packet.reducedDebugInfo)
            writable.writeBoolean(packet.enableRespawnScreen)
            writable.writeBoolean(packet.isDebug)
            writable.writeBoolean(packet.isFlat)
            writable.writeBoolean(packet.hasDeathLocation)
            if (packet.hasDeathLocation) {
                writable.writeString(packet.deathDimensionName!!)
                writable.writeLong(packet.deathLocation.value)
            }
        }
    }


    override fun estimateSize(): Int {
        return 4 + //entityId
                1 + // isHardcore
                1 + // gameMode
                1 + // previousGameMode
                5 + // dimension's count
                dimensions.sumOf { it.protocolSizeInBytes } + // dimensions, no utf8
                128 + // dimensionCodec smallest size
                dimensionType.protocolSizeInBytes +
                dimensionName.protocolSizeInBytes +
                16 + // hashedSeed
                5 + // maxPlayers
                5 + // viewDistance
                5 + // simulationDistance
                1 + // reducedDebugInfo
                1 + // enableRespawnScreen
                1 + // isDebug
                1 + // isFlat
                1 + // hasDeathLocation
                if (hasDeathLocation) {
                    deathDimensionName!!.protocolSizeInBytes + 8
                } else {
                    0
                }

    }
}