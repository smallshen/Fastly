package org.endoqa.fastly.nio

import org.endoqa.fastly.nbt.NBT
import org.endoqa.fastly.util.protocol.readVarInt
import org.endoqa.fastly.util.protocol.writeVarInt
import java.nio.ByteBuffer
import java.util.*

val EMPTY_BYTE_ARRAY = ByteArray(0)

@JvmInline
value class ByteBuf(private val buf: ByteBuffer) : DataWritable, DataReadable {


    override fun readByte(): Byte {
        return buf.get()
    }

    override fun readUnsignedByte(): UByte {
        return buf.get().toUByte()
    }

    override fun writeByte(byte: Byte) {
        buf.put(byte)
    }

    override fun writeUnsignedByte(uByte: UByte) {
        buf.put(uByte.toByte())
    }

    override fun writeFloat(float: Float) {
        buf.putFloat(float)
    }

    override fun writeDouble(double: Double) {
        buf.putDouble(double)
    }

    override fun readUnsignedShort(): UShort {
        return buf.short.toUShort()
    }

    override fun writeUnsignedShort(uShort: UShort) {
        buf.putShort(uShort.toShort())
    }

    override fun readVarInt(): Int {
        return readVarInt { buf.get() }
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
        if (byteArray === EMPTY_BYTE_ARRAY) return
        if (byteArray.isEmpty()) return

        buf.put(byteArray)
    }

    override fun readRemaining(): ByteArray {
        if (!buf.hasRemaining()) return EMPTY_BYTE_ARRAY

        val bytes = ByteArray(buf.remaining())
        buf.get(bytes)
        return bytes
    }


    override fun writeVarInt(int: Int) {
        writeVarInt(int) { buf.put(it) }
    }

    override fun readNBT(): NBT = NBT.pull(buf)


    override fun writeNBT(nbt: NBT) {
        nbt.push(buf)
    }

    override fun readShort(): Short {
        return buf.short
    }

    override fun readFloat(): Float {
        return buf.float
    }

    override fun readDouble(): Double {
        return buf.double
    }

    override fun writeShort(short: Short) {
        buf.putShort(short)
    }

    override fun readInt(): Int {
        return buf.int
    }

    override fun writeInt(int: Int) {
        buf.putInt(int)
    }

    override fun available(): Int {
        return buf.remaining()
    }
}