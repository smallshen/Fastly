package org.endoqa.fastly.nio

import java.util.*

interface DataWritable {
    fun writeVarInt(int: Int)
    fun writeString(string: String)
    fun writeUnsignedShort(uShort: UShort)
    fun writeLong(long: Long)
    fun writeBoolean(boolean: Boolean)
    fun writeUUID(uuid: UUID)
    fun writeByteArray(byteArray: ByteArray)
}