package org.endoqa.fastly.protocol

import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import org.endoqa.fastly.nio.GrowingByteBuf
import org.endoqa.fastly.util.calculateVarIntSize
import java.nio.ByteBuffer

abstract class PacketHandler<out T : MinecraftPacket>(
    val packetId: Int,
    val dynamicSize: Boolean = false
) {


    companion object {
        //TODO: reuse buffers
        fun encodePacket(packet: MinecraftPacket): ByteBuffer {

            val byteBuffer: ByteBuffer = if (packet.handler.dynamicSize) {
                val buf = GrowingByteBuf(packet.estimateSize())
                packet.handler.write(buf, packet)
                buf.buf.flip()
            } else {
                val buf = ByteBuffer.allocate(packet.estimateSize())
                packet.handler.write(ByteBuf(buf), packet)
                buf.flip()
            }

            return byteBuffer

        }
    }


    val packetIdSize = calculateVarIntSize(packetId)


    abstract fun read(readable: DataReadable): T
    abstract fun write(writable: DataWritable, packet: @UnsafeVariance T)

}