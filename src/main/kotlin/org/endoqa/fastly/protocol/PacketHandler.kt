package org.endoqa.fastly.protocol

import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.util.calculateVarIntSize

abstract class PacketHandler<out T : MinecraftPacket>(val packetId: Int) {

    val packetIdSize = calculateVarIntSize(packetId)


    abstract fun read(readable: DataReadable): T
    abstract fun write(writable: DataWritable, packet: @UnsafeVariance T)

}