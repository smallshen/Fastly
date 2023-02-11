package org.endoqa.fastly.nio

import org.endoqa.fastly.util.protocol.writeVarInt
import java.nio.ByteBuffer
import java.util.*

@JvmInline
value class ByteBuf(private val buf: ByteBuffer) : DataWritable, DataReadable {


    override fun readUnsignedShort(): UShort {
        return buf.short.toUShort()
    }

    override fun writeUnsignedShort(uShort: UShort) {
        buf.putShort(uShort.toShort())
    }

    override fun readVarInt(): Int {
        return org.endoqa.fastly.util.protocol.readVarInt { buf.get() }
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

    override fun readByteArray(length: Int): ByteArray {
        val bytes = ByteArray(length)
        buf.get(bytes)
        return bytes
    }

    override fun writeByteArray(byteArray: ByteArray) {
        buf.put(byteArray)
    }


    override fun writeVarInt(int: Int) {
        writeVarInt(int) { buf.put(it) }
    }
}