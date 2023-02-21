package org.endoqa.fastly.encryption

import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.util.protocol.readVarInt
import org.endoqa.fastly.util.protocol.writeVarInt
import java.nio.ByteBuffer


@JvmInline
value class EncryptedComplexAsyncSocket(val connection: Connection) {

    private val socket inline get() = connection.socket

    private val encrypt inline get() = connection.encryptCipher
    private val decrypt inline get() = connection.decryptCipher

    suspend fun readVarInt(): Int {
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


    suspend fun writeVarInt(v: Int) {
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

    suspend fun readFully(len: Int): ByteBuffer {
        val buf = socket.readFully(len).position(0)
        val dup = buf.duplicate()

        decrypt.update(buf, dup)
        return dup
    }

    suspend fun write(buf: ByteBuffer) {
        buf.position(0)
        val encryptedBuf = buf.duplicate()
        encrypt.update(buf, encryptedBuf)
        socket.write(encryptedBuf.flip())
    }
}