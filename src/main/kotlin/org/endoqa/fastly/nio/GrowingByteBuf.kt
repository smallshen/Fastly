package org.endoqa.fastly.nio


import org.endoqa.fastly.nbt.NBT
import org.endoqa.fastly.util.protocol.writeVarInt
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.max

class GrowingByteBuf(
    cap: Int = 1024,
    private val growthFactor: Double = 1.2
) : DataWritable {

    var buf: ByteBuffer = ByteBuffer.allocate(cap)

    override fun writeInt(int: Int) {
        ensureCapacity(4)
        ByteBuf(buf).writeInt(int)
    }

    override fun writeByte(byte: Byte) {
        ensureCapacity(1)
        ByteBuf(buf).writeByte(byte)
    }

    override fun writeUnsignedByte(uByte: UByte) {
        ensureCapacity(1)
        ByteBuf(buf).writeUnsignedByte(uByte)
    }

    override fun writeFloat(float: Float) {
        ensureCapacity(4)
        ByteBuf(buf).writeFloat(float)
    }

    override fun writeDouble(double: Double) {
        ensureCapacity(8)
        ByteBuf(buf).writeDouble(double)
    }

    override fun writeVarInt(int: Int) {
        ensureCapacity(5)
        writeVarInt(int) { writeByte(it) }
    }

    override fun writeString(string: String) {
        val bytes = string.toByteArray()
        writeVarInt(bytes.size)
        writeByteArray(bytes)
    }

    override fun writeUnsignedShort(uShort: UShort) {
        ensureCapacity(2)
        ByteBuf(buf).writeUnsignedShort(uShort)
    }

    override fun writeLong(long: Long) {
        ensureCapacity(8)
        ByteBuf(buf).writeLong(long)
    }


    override fun writeBoolean(boolean: Boolean) {
        ensureCapacity(1)
        ByteBuf(buf).writeBoolean(boolean)
    }

    override fun writeUUID(uuid: UUID) {
        ensureCapacity(16)
        ByteBuf(buf).writeUUID(uuid)
    }

    override fun writeByteArray(byteArray: ByteArray) {
        ensureCapacity(byteArray.size)
        ByteBuf(buf).writeByteArray(byteArray)
    }

    override fun writeShort(short: Short) {
        ensureCapacity(2)
        ByteBuf(buf).writeShort(short)
    }

    override fun writeNBT(nbt: NBT) {
        nbt.push(this)
    }

    override fun available(): Int {
        return buf.remaining()
    }


    private fun ensureCapacity(size: Int) {
        if (buf.remaining() < size) {
            val newCapacity = max(
                (buf.capacity() * growthFactor).toInt(),
                (buf.capacity() + size)
            )
            val newUnderlying = ByteBuffer.allocate(newCapacity)
            buf.flip()
            newUnderlying.put(buf)
            buf = newUnderlying
        }
    }


}