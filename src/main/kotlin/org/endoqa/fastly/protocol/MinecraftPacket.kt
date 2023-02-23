package org.endoqa.fastly.protocol

interface MinecraftPacket {
    val handler: PacketHandler<MinecraftPacket>


    fun estimateSize(): Int
}