package org.endoqa.fastly.encryption

import org.endoqa.fastly.nio.AsyncSocket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import javax.crypto.Cipher


private const val SEGMENT_BITS = 0x7F
private const val CONTINUE_BIT = 0x80

@JvmInline
value class EncryptedComplexAsyncSocket(val channel: AsynchronousSocketChannel) {


    suspend fun readVarInt(decrypt: Cipher): Int {
        val socket = AsyncSocket(channel)
        val sharedBuf = ByteBuffer.allocate(1)
        val shareDecrypt = ByteBuffer.allocate(1)

        var value = 0
        var position = 0
        var currentByte: Int


        repeat(5) {
            sharedBuf.clear()
            shareDecrypt.clear()

            decrypt.update(socket.readFully(sharedBuf).position(0), shareDecrypt)
            shareDecrypt.position(0)

            currentByte = shareDecrypt.get().toInt()
            value = value or (currentByte and SEGMENT_BITS shl position)

            if (currentByte and CONTINUE_BIT == 0) {
                return value
            }

            position += 7
            if (position >= 32) throw RuntimeException("VarInt is too big")
        }

        error("VarInt size overflow")
    }


    suspend fun writeVarInt(v: Int, encrypt: Cipher) {
        val socket = AsyncSocket(channel)
        val sharedBuf = ByteBuffer.allocate(1)
        val shareEncrypt = ByteBuffer.allocate(1)

        var value = v
        while (true) {
            sharedBuf.clear()
            shareEncrypt.clear()

            if (value and SEGMENT_BITS.inv() == 0) {

                sharedBuf.put(value.toByte()).position(0)

                encrypt.update(sharedBuf, shareEncrypt)
                require(!shareEncrypt.hasRemaining())

                socket.write(sharedBuf.position(0))
                return
            }

            sharedBuf.put((value and SEGMENT_BITS or CONTINUE_BIT).toByte()).position(0)

            encrypt.update(sharedBuf, shareEncrypt)

            socket.write(shareEncrypt.position(0))

            value = value ushr 7
        }
    }

    suspend fun readFully(len: Int, decrypt: Cipher): ByteBuffer {
        val buf = AsyncSocket(channel).readFully(len).position(0)
        val dup = buf.duplicate()

        decrypt.update(buf, dup)
        return dup
    }

    suspend fun write(buf: ByteBuffer, encrypt: Cipher) {
        println("write")
        val dup = buf.duplicate()

        encrypt.update(buf.position(0), dup)

        require(!dup.hasRemaining())

        AsyncSocket(channel).write(dup.position(0))
    }
}