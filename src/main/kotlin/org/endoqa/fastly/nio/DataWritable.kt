package org.endoqa.fastly.nio


import org.endoqa.fastly.nbt.NBT
import java.util.*

interface DataWritable {

    fun writeInt(int: Int)
    fun writeByte(byte: Byte)
    fun writeUnsignedByte(uByte: UByte)

    fun writeFloat(float: Float)

    fun writeDouble(double: Double)

    fun writeVarInt(int: Int)
    fun writeString(string: String)
    fun writeShort(short: Short)
    fun writeUnsignedShort(uShort: UShort)
    fun writeLong(long: Long)
    fun writeBoolean(boolean: Boolean)
    fun writeUUID(uuid: UUID)
    fun writeByteArray(byteArray: ByteArray)

    fun writeNBT(nbt: NBT)

    fun available(): Int
}