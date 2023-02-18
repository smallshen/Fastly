package org.endoqa.fastly.nio

import org.endoqa.fastly.util.protocol.nbt.NBT
import java.util.*

interface DataReadable {

    fun readInt(): Int
    fun readByte(): Byte


    fun readUnsignedByte(): UByte

    fun readLong(): Long

    fun readShort(): Short

    fun readFloat(): Float

    fun readDouble(): Double

    fun readUnsignedShort(): UShort
    fun readVarInt(): Int
    fun readString(maxLength: Int = 32767): String
    fun readBoolean(): Boolean
    fun readUUID(): UUID
    fun readByteArray(length: Int): ByteArray
    fun readRemaining(): ByteArray

    fun readNBT(): NBT

    fun available(): Int
}