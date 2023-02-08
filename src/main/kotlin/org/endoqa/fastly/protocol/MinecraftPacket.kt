package org.endoqa.fastly.protocol

interface MinecraftPacket {
    val handler: PacketHandler<out MinecraftPacket>


    fun estimateSize(): Int
}