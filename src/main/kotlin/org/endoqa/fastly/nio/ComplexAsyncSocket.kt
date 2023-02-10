package org.endoqa.fastly.nio

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel


private const val SEGMENT_BITS = 0x7F
private const val CONTINUE_BIT = 0x80

@JvmInline
value class ComplexAsyncSocket(val channel: AsynchronousSocketChannel) {


    suspend fun readVarInt(): Int {
        val socket = AsyncSocket(channel)
        val sharedBuf = ByteBuffer.allocate(1)

        var value = 0
        var position = 0
        var currentByte: Int


        repeat(5) {
            sharedBuf.clear()
            currentByte = socket.readFully(sharedBuf).position(0).get().toInt()
            value = value or (currentByte and SEGMENT_BITS shl position)

            if (currentByte and CONTINUE_BIT == 0) {
                return value
            }

            position += 7
            if (position >= 32) throw RuntimeException("VarInt is too big")
        }

        error("VarInt size overflow")
    }

    suspend fun writeVarInt(v: Int) {
        val socket = AsyncSocket(channel)
        val sharedBuf = ByteBuffer.allocate(1)

        var value = v
        while (true) {
            sharedBuf.clear()

            if (value and SEGMENT_BITS.inv() == 0) {
                socket.write(sharedBuf.put(value.toByte()).position(0))
                return
            }

            socket.write(sharedBuf.put((value and SEGMENT_BITS or CONTINUE_BIT).toByte()).position(0))

            value = value ushr 7
        }
    }

    suspend fun readFully(len: Int) = AsyncSocket(channel).readFully(len)

    suspend fun write(buf: ByteBuffer) = AsyncSocket(channel).write(buf)
}