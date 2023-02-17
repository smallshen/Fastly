package org.endoqa.fastly.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.endoqa.fastly.encryption.EncryptedComplexAsyncSocket
import org.endoqa.fastly.nio.AsyncSocket
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.nio.ComplexAsyncSocket
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.RawPacket
import org.endoqa.fastly.util.calculateVarIntSize
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec


class Connection(val socket: AsyncSocket, parentJob: Job? = null) : CoroutineScope {

    override val coroutineContext = Job(parentJob)


    val packetOut = Channel<RawPacket>()

    private var encryption = false

    init {
        coroutineContext.invokeOnCompletion {
            packetOut.close(it)
            socket.close()
        }
    }


    lateinit var encryptCipher: Cipher
        private set


    lateinit var decryptCipher: Cipher
        private set


    fun startIO() {
        launch {
            try {
                outgoingLoop()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }


    private suspend fun outgoingLoop() {
        for (p in packetOut) {
            if (socket.channel.isOpen) {
                writeRawPacket(p)
                p.attachJob?.complete()
            } else {
                coroutineContext.cancel()
                return
            }
        }
    }

    suspend fun readRawPacket(): RawPacket {
        return if (encryption) {
            readEncrypted()
        } else {
            readNormal()
        }
    }


    private suspend fun readNormal(): RawPacket {
        val cs = ComplexAsyncSocket(socket.channel)
        val length = cs.readVarInt()
        val packetId = cs.readVarInt()
        val actualDataLength = length - calculateVarIntSize(packetId)
        val pData = cs.readFully(actualDataLength).position(0)
        val rawPacket = RawPacket(length, packetId, pData)

        return rawPacket
    }

    private suspend fun readEncrypted(): RawPacket {
        val cs = EncryptedComplexAsyncSocket(socket.channel)
        val length = cs.readVarInt(decryptCipher)
        val packetId = cs.readVarInt(decryptCipher)
        val actualDataLength = length - calculateVarIntSize(packetId)
        val pData = cs.readFully(actualDataLength, decryptCipher).flip()
        return RawPacket(length, packetId, pData)
    }

    private suspend fun writeRawPacket(packet: RawPacket) {
        if (encryption) {
            writeEncrypted(packet)
        } else {
            writeNormal(packet)
        }
    }

    private suspend fun writeNormal(packet: RawPacket) {
        val (length, packetId, buffer) = packet

        val cs = ComplexAsyncSocket(socket.channel)
        cs.writeVarInt(length)
        cs.writeVarInt(packetId)
        cs.write(buffer.position(0))
    }

    private suspend fun writeEncrypted(packet: RawPacket) {
        val (length, packetId, buffer) = packet

        val cs = EncryptedComplexAsyncSocket(socket.channel)
        cs.writeVarInt(length, encryptCipher)
        cs.writeVarInt(packetId, encryptCipher)
        cs.write(buffer.position(0), encryptCipher)
    }

    suspend fun sendPacket(p: MinecraftPacket): CompletableJob {
        val buf = ByteBuffer.allocate(p.estimateSize())

        @Suppress("TYPE_MISMATCH")
        p.handler.write(ByteBuf(buf), p)
        buf.flip()

        val job = Job()

        packetOut.send(RawPacket(buf.limit() + p.handler.packetIdSize, p.handler.packetId, buf, job))

        return job
    }

    fun enableEncryption(secretKey: SecretKey) {
        encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(secretKey.encoded))

        decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(secretKey.encoded))

        encryption = true
    }

    fun close() {
        this.coroutineContext.complete()
    }
}