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
        val decryptBuf = sharedBuf.duplicate()

        return readVarInt {
            sharedBuf.clear()
            decryptBuf.clear()

            socket.readFully(sharedBuf)

            decrypt.update(sharedBuf.position(0), decryptBuf)

            decryptBuf.position(0).get()
        }
    }


    suspend fun writeVarInt(v: Int, encrypt: Cipher) {
        val socket = AsyncSocket(channel)
        val sharedBuf = ByteBuffer.allocate(1)
        val encryptBuf = sharedBuf.duplicate()

        writeVarInt(v) {
            sharedBuf.clear()
            encryptBuf.clear()

            sharedBuf.put(it).position(0)

            encrypt.update(sharedBuf, encryptBuf)

            socket.write(encryptBuf.position(0))
        }
    }

    suspend fun readFully(len: Int, decrypt: Cipher): ByteBuffer {
        val buf = AsyncSocket(channel).readFully(len).position(0)
        val dup = buf.duplicate()

        decrypt.update(buf, dup)
        return dup
    }

    suspend fun write(buf: ByteBuffer, encrypt: Cipher) {
        buf.position(0)
        val encryptedBuf = buf.duplicate()
        encrypt.update(buf, encryptedBuf)
        AsyncSocket(channel).write(encryptedBuf.flip())
    }
}