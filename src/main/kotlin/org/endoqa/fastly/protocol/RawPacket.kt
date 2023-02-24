package org.endoqa.fastly.protocol

import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.util.calculateVarIntSize
import java.nio.ByteBuffer
import java.util.zip.Deflater
import java.util.zip.Inflater

sealed interface RawPacket {
    val length: Int
    val dataBuffer: ByteBuffer

    fun needCompression(threshold: Int): Boolean

    fun compress(deflater: Deflater): CompressedRawPacket

    fun decompress(inflater: Inflater): NormalRawPacket


}

/**
 * Compressed packet
 *
 * @property length the length of uncompressedLength field and dataBuffer
 */
class CompressedRawPacket(
    override val length: Int,
    val originalLength: Int,
    override val dataBuffer: ByteBuffer
) : RawPacket {

    override fun needCompression(threshold: Int): Boolean {
        return originalLength >= threshold
    }

    override fun compress(deflater: Deflater): CompressedRawPacket {
        deflater.end()
        return this
    }

    override fun decompress(inflater: Inflater): NormalRawPacket {
        val decompressedBuffer = ByteBuffer.allocate(originalLength)

        inflater.setInput(dataBuffer.position(0))
        val finalSize = inflater.inflate(decompressedBuffer)
        require(finalSize == originalLength) { "Decompressed size is not equal to original size" }
        inflater.reset()
        inflater.end()
        decompressedBuffer.flip()


        return NormalRawPacket(
            originalLength,
            ByteBuf(decompressedBuffer).readVarInt(),
            decompressedBuffer.flip().asReadOnlyBuffer(),
            decompressedBuffer.position(0).asReadOnlyBuffer()
        )

    }
}

/**
 * Normal raw packet
 *
 * @property length
 * @property packetId
 * @property contentBuffer buffer without packetId
 * @property dataBuffer
 */
data class NormalRawPacket(
    override val length: Int,
    val packetId: Int,
    val contentBuffer: ByteBuffer,
    override val dataBuffer: ByteBuffer
) : RawPacket {

    override fun needCompression(threshold: Int): Boolean {
        return length >= threshold
    }


    override fun compress(deflater: Deflater): CompressedRawPacket {
        val output = ByteBuffer.allocate(length + 10) // ok gzip or minecraft, fuck u

        deflater.setInput(dataBuffer.position(0))
        deflater.finish()
        deflater.deflate(output)
        deflater.reset()
        deflater.end()

        output.flip()

        return CompressedRawPacket(
            output.limit() + calculateVarIntSize(length),
            length,
            output.position(0).asReadOnlyBuffer()
        )
    }

    override fun decompress(inflater: Inflater): NormalRawPacket {
        inflater.end()
        return this
    }

}