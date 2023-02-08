package org.endoqa.fastly.nio

import java.util.*

interface DataReadable {

    fun readLong(): Long
    fun readUnsignedShort(): UShort
    fun readVarInt(): Int
    fun readString(maxLength: Int = 32767): String
    fun readBoolean(): Boolean
    fun readUUID(): UUID
}