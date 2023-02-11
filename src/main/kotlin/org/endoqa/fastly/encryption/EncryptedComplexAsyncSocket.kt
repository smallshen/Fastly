package org.endoqa.fastly.encryption

import org.endoqa.fastly.nio.AsyncSocket
import org.endoqa.fastly.util.protocol.readVarInt
import org.endoqa.fastly.util.protocol.writeVarInt
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import javax.crypto.Cipher


@JvmInline
value class EncryptedComplexAsyncSocket(val channel: AsynchronousSocketChannel) {


    suspend fun readVarInt(decrypt: Cipher): Int {
        val socket = AsyncSocket(channel)
        val sharedBuf = ByteBuffer.allocate(1)
        val shareDecrypt = ByteBuffer.allocate(1)

        return readVarInt {
            sharedBuf.clear()
            shareDecrypt.clear()
            decrypt.update(socket.readFully(sharedBuf).position(0), shareDecrypt)
            shareDecrypt.position(0).get()
        }
    }


    suspend fun writeVarInt(v: Int, encrypt: Cipher) {
        val socket = AsyncSocket(channel)
        val sharedBuf = ByteBuffer.allocate(1)
        val sharedEncrypt = ByteBuffer.allocate(1)

        writeVarInt(v) {
            sharedBuf.clear()
            sharedEncrypt.clear()

            sharedBuf.put(it).position(0)

            encrypt.update(sharedBuf, sharedEncrypt)

            socket.write(sharedEncrypt.position(0))
        }
    }

    suspend fun readFully(len: Int, decrypt: Cipher): ByteBuffer {
        val buf = AsyncSocket(channel).readFully(len).position(0)
        val dup = ByteBuffer.allocate(len)

        decrypt.update(buf, dup)
        return dup
    }

    suspend fun write(buf: ByteBuffer, encrypt: Cipher) {
        val dup = ByteBuffer.allocate(buf.capacity())
        encrypt.update(buf.position(0), dup)
        AsyncSocket(channel).write(dup.position(0))
    }
}