package org.endoqa.fastly.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.endoqa.fastly.encryption.EncryptedComplexAsyncSocket
import org.endoqa.fastly.nio.AsyncSocket
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.nio.ComplexAsyncSocket
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.PacketHandler
import org.endoqa.fastly.protocol.RawPacket
import org.endoqa.fastly.protocol.packet.server.login.SetCompressionPacket
import org.endoqa.fastly.util.calculateVarIntSize
import java.nio.ByteBuffer
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec


class Connection(
    val socket: AsyncSocket,
    parentJob: Job? = null
) : CoroutineScope, AutoCloseable {


    var compressionThreshold: Int = -1

    override val coroutineContext = Job(parentJob)

    private var encryption = false


    lateinit var encryptCipher: Cipher
        private set


    lateinit var decryptCipher: Cipher
        private set


    private val deflater by lazy { Deflater() }
    private val inflater by lazy { Inflater() }


    suspend fun enableCompression(threshold: Int) {
        val p = SetCompressionPacket(threshold)
        sendPacket(p)
        compressionThreshold = threshold
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
        return readPacket(
            readVarInt = { cs.readVarInt() },
            readFully = { cs.readFully(it).position(0) }
        )
    }

    private suspend fun readEncrypted(): RawPacket {
        val cs = EncryptedComplexAsyncSocket(this)
        return readPacket(
            readVarInt = { cs.readVarInt() },
            readFully = { cs.readFully(it).position(0) }
        )
    }

    suspend fun writeRawPacket(packet: RawPacket) {
        if (encryption) {
            writeEncrypted(packet)
        } else {
            writeNormal(packet)
        }
    }

    private suspend fun writeNormal(packet: RawPacket) {
        val cs = ComplexAsyncSocket(socket.channel)
        writePacket(
            packet,
            writeVarInt = { cs.writeVarInt(it) },
            writeFully = { cs.write(it) }
        )
    }

    private suspend fun writeEncrypted(packet: RawPacket) {
        val cs = EncryptedComplexAsyncSocket(this)
        writePacket(
            packet,
            writeVarInt = { cs.writeVarInt(it) },
            writeFully = { cs.write(it) }
        )
    }

    private inline fun readPacket(
        readVarInt: () -> Int,
        readFully: (Int) -> ByteBuffer
    ): RawPacket {
        return if (compressionThreshold >= 0) {
            readCompressedPacket(readVarInt, readFully)
        } else {
            val length = readVarInt()
            val packetId = readVarInt()
            val actualDataLength = length - calculateVarIntSize(packetId)
            val pData = readFully(actualDataLength).position(0)
            return RawPacket(length, packetId, pData)
        }
    }


    private inline fun readCompressedPacket(
        readVarInt: () -> Int,
        readFully: (Int) -> ByteBuffer
    ): RawPacket {
        val compressedSize = readVarInt()
        val dataLength = readVarInt()
        val rawBuffer = readFully(compressedSize - calculateVarIntSize(dataLength))

        if (dataLength == 0) {
            val packetId = ByteBuf(rawBuffer).readVarInt()

            return RawPacket(rawBuffer.limit(), packetId, rawBuffer.slice())
        } else {
            val decompressedBuffer = ByteBuffer.allocate(dataLength)

            inflater.setInput(rawBuffer.position(0))
            require(inflater.inflate(decompressedBuffer) == dataLength) { "Decompressed data length is not equal to the data length" }
            inflater.reset()

            decompressedBuffer.flip()

            val packetId = ByteBuf(decompressedBuffer).readVarInt()

            return RawPacket(dataLength, packetId, decompressedBuffer.slice())
        }
    }


    private inline fun writePacket(
        packet: RawPacket,
        writeVarInt: (Int) -> Unit,
        writeFully: (ByteBuffer) -> Unit
    ) {
        val (length, packetId, buffer) = packet

        if (compressionThreshold >= 0) {
            writeCompressedPacket(packet, writeVarInt, writeFully)
        } else {
            writeVarInt(length)
            writeVarInt(packetId)
            writeFully(buffer.position(0))
        }
    }

    private inline fun writeCompressedPacket(
        packet: RawPacket,
        writeVarInt: (Int) -> Unit,
        writeFully: (ByteBuffer) -> Unit
    ) {
        val (length, packetId, buffer) = packet

        if (length >= compressionThreshold) {
            val input = ByteBuffer.allocate(length)

            ByteBuf(input).writeVarInt(packetId)
            input.put(packet.buffer.position(0))
            input.flip()

            val output = ByteBuffer.allocate(input.limit() + 10) // ok gzip or minecraft, fuck u

            deflater.setInput(input)
            deflater.finish()
            deflater.deflate(output)
            deflater.reset()

            output.flip()

            writeVarInt(output.limit() + calculateVarIntSize(length))
            writeVarInt(length)
            writeFully(output)

        } else {
            writeVarInt(length + 1)
            writeVarInt(0)
            writeVarInt(packetId)
            writeFully(buffer.position(0))
        }
    }


    suspend fun sendPacket(p: MinecraftPacket) {
        val buf = PacketHandler.encodePacket(p)

        val p = RawPacket(buf.limit() + p.handler.packetIdSize, p.handler.packetId, buf)
        writeRawPacket(p)
    }

    fun enableEncryption(secretKey: SecretKey) {
        encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(secretKey.encoded))

        decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(secretKey.encoded))

        encryption = true
    }

    override fun close() {
        coroutineContext.complete()
        socket.close()
        deflater.end()
        inflater.end()
    }
}