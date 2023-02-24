package org.endoqa.fastly.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.endoqa.fastly.encryption.EncryptedComplexAsyncSocket
import org.endoqa.fastly.nio.AsyncSocket
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.nio.ComplexAsyncSocket
import org.endoqa.fastly.protocol.*
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

    fun enableEncryption(secretKey: SecretKey) {
        encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(secretKey.encoded))

        decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(secretKey.encoded))

        encryption = true
    }


    suspend fun readRawPacket(): RawPacket {
        return if (encryption) {
            readEncrypted()
        } else {
            readNormal()
        }
    }

    suspend fun nextPacket(): NormalRawPacket {
        return readRawPacket().ensureDecompressed()
    }

    private fun RawPacket.ensureCompressed(): CompressedRawPacket {
        return if (this is CompressedRawPacket) this
        else compress(Deflater())
    }

    private fun RawPacket.ensureDecompressed(): NormalRawPacket {
        return if (this is NormalRawPacket) this
        else decompress(Inflater())
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
            val packetBuffer = readFully(length)
            return NormalRawPacket(
                length,
                ByteBuf(packetBuffer).readVarInt(),
                packetBuffer.slice().asReadOnlyBuffer(),
                packetBuffer.position(0).asReadOnlyBuffer(),
            )
        }
    }


    private inline fun readCompressedPacket(
        readVarInt: () -> Int,
        readFully: (Int) -> ByteBuffer
    ): RawPacket {
        val length = readVarInt()
        val dataLength = readVarInt()
        val rawBuffer = readFully(length - calculateVarIntSize(dataLength))

        if (dataLength == 0) {
            val packetId = ByteBuf(rawBuffer).readVarInt()

            return NormalRawPacket(
                rawBuffer.limit(),
                packetId,
                rawBuffer.slice().asReadOnlyBuffer(),
                rawBuffer.position(0).asReadOnlyBuffer()
            )
        } else {

            return CompressedRawPacket(
                length,
                dataLength,
                rawBuffer
            )
        }
    }


    private inline fun writePacket(
        packet: RawPacket,
        writeVarInt: (Int) -> Unit,
        writeFully: (ByteBuffer) -> Unit
    ) {
        if (compressionThreshold >= 0) {
            writeCompressedPacket(packet, writeVarInt, writeFully)
        } else {
            val (length, _, _, buffer) = packet.ensureDecompressed()
            writeVarInt(length)
            writeFully(buffer.position(0))
        }
    }

    private inline fun writeCompressedPacket(
        packet: RawPacket,
        writeVarInt: (Int) -> Unit,
        writeFully: (ByteBuffer) -> Unit
    ) {
        val rp = if (packet.needCompression(compressionThreshold)) packet.ensureCompressed() else packet

        when (rp) {
            is CompressedRawPacket -> {
                writeVarInt(rp.length)

                writeVarInt(rp.originalLength)
                writeFully(rp.dataBuffer.position(0))
            }

            is NormalRawPacket -> {
                writeVarInt(rp.length + 1)

                writeVarInt(0) // uncompressed
                writeFully(rp.dataBuffer.position(0))
            }
        }

    }


    suspend fun sendPacket(p: MinecraftPacket) {
        val buf = PacketHandler.encodePacket(p)

        val data = ByteBuffer.allocate(buf.limit() + p.handler.packetIdSize)

        ByteBuf(data).writeVarInt(p.handler.packetId)
        data.put(buf)
        data.flip()

        val rp = NormalRawPacket(
            buf.limit() + p.handler.packetIdSize,
            p.handler.packetId,
            buf.position(0).asReadOnlyBuffer(),
            data.position(0).asReadOnlyBuffer()
        )

        writeRawPacket(rp)
    }


    override fun close() {
        coroutineContext.complete()
        socket.close()
        deflater.end()
        inflater.end()
    }
}