package org.endoqa.fastly.nio

import java.nio.ByteBuffer
import java.util.*


private const val SEGMENT_BITS = 0x7F
private const val CONTINUE_BIT = 0x80

@JvmInline
value class ByteBuf(private val buf: ByteBuffer) : DataWritable, DataReadable {


    override fun readUnsignedShort(): UShort {
        return buf.short.toUShort()
    }

    override fun writeUnsignedShort(uShort: UShort) {
        buf.putShort(uShort.toShort())
    }

    override fun readVarInt(): Int {
        var value = 0
        var position = 0
        var currentByte: Int


        repeat(5) {
            currentByte = buf.get().toInt()
            value = value or (currentByte and SEGMENT_BITS shl position)

            if (currentByte and CONTINUE_BIT == 0) {
                return value
            }

            position += 7
            if (position >= 32) throw RuntimeException("VarInt is too big")
        }

        error("VarInt size overflow")
    }

    override fun readLong() = buf.long
    override fun writeLong(long: Long) {
        buf.putLong(long)
    }

    override fun writeBoolean(boolean: Boolean) {
        buf.put(if (boolean) 0x01 else 0x00)
    }

    override fun writeUUID(uuid: UUID) {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }

    override fun readUUID() = UUID(readLong(), readLong())


    override fun readString(maxLength: Int): String {
        val length = readVarInt()
        require(length <= maxLength) { "String too big (was $length bytes encoded, max $maxLength)" }
        val bytes = ByteArray(length)
        buf.get(bytes)
        return String(bytes)
    }

    override fun writeString(string: String) {
        val bytes = string.toByteArray()
        writeVarInt(bytes.size)
        buf.put(bytes)
    }


    override fun readBoolean(): Boolean {
        val v = buf.get().toInt()

        if (v == 0x01) return true
        if (v == 0x00) return false

        error("Invalid boolean value: $v")
    }

    override fun writeVarInt(int: Int) {
        var value = int
        while (true) {
            if (value and SEGMENT_BITS.inv() == 0) {
                buf.put(value.toByte())
                return
            }
            buf.put((value and SEGMENT_BITS or CONTINUE_BIT).toByte())

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value = value ushr 7
        }
    }
}