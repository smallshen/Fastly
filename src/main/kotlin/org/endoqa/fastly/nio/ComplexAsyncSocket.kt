package org.endoqa.fastly.nio

import org.endoqa.fastly.util.protocol.readVarInt
import org.endoqa.fastly.util.protocol.writeVarInt
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel


@JvmInline
value class ComplexAsyncSocket(val channel: AsynchronousSocketChannel) {


    suspend fun readVarInt(): Int {
        val socket = AsyncSocket(channel)
        val sharedBuf = ByteBuffer.allocate(1)

        return readVarInt {
            socket.readFully(sharedBuf.clear()).position(0).get()
        }
    }

    suspend fun writeVarInt(v: Int) {
        val socket = AsyncSocket(channel)
        val sharedBuf = ByteBuffer.allocate(1)

        writeVarInt(v) {
            sharedBuf.clear()
            socket.write(sharedBuf.put(it).position(0))
        }
    }

    suspend fun readFully(len: Int) = AsyncSocket(channel).readFully(len)

    suspend fun write(buf: ByteBuffer) = AsyncSocket(channel).write(buf)
}