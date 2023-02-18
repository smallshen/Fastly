/*
MIT License

Copyright (c) 2020 Camden B

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package org.endoqa.fastly.util.protocol.nbt

import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.nio.DataReadable
import org.endoqa.fastly.nio.DataWritable
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


class NBT(
    val name: String,
    map: Map<String, Any>,
    internal val listIDs: MutableMap<String, Int> = mutableMapOf()
) {

    internal val map = map.toMutableMap()


    operator fun <T> invoke(key: String): Lazy<T> {
        return lazy { get(key) }
    }

    fun <T> get(key: String): T {
        return map[key] as T
    }


    fun push(output: ByteBuffer) {
        push(ByteBuf(output))
    }

    fun push(output: DataWritable) {
        if (output is AutoCloseable) {
            output.use { write(output, name, this) }
        } else {
            write(output, name, this)
        }
    }

    // Gets the total amount of bytes needed to serialize this
    fun getNBTSize(): Int {
        return getTagSize(name, this)
    }


    override fun toString(): String {

        return map.entries.joinToString("\n", "{\n", "\n}") { (name, value) ->

            val valueText = when (value) {
                is IntArray -> value.contentToString()
                is ByteArray -> value.contentToString()
                is LongArray -> value.contentToString()
                is String -> "\"$value\""
                is List<*> -> "\n${"$value".prependIndent("  ")}"
                else -> "$value"
            }

            "[${value::class.simpleName}] $name = $valueText".prependIndent("  ")
        }
    }

    override fun equals(other: Any?): Boolean {
        return toString() == other.toString()
    }

    override fun hashCode(): Int {

        var result = name.hashCode()
        result = 31 * result + listIDs.hashCode()

        map.forEach { (key, value) ->

            result = 31 * result + key.hashCode()

            result = 31 * result + when (value) {
                is IntArray -> value.contentHashCode()
                is ByteArray -> value.contentHashCode()
                is LongArray -> value.contentHashCode()
                else -> value.hashCode()
            }
        }

        return result
    }


    // Gets the total amount of bytes needed to serialize this
    private fun getTagSize(key: String, value: Any): Int {

        // Key Size Short Size + Key Size + TagID Size + Value Size
        return Short.SIZE_BYTES + key.length + Byte.SIZE_BYTES + when (value) {

            is Byte -> Byte.SIZE_BYTES
            is Short -> Short.SIZE_BYTES
            is Int, is Float -> Int.SIZE_BYTES
            is Long, is Double -> Long.SIZE_BYTES
            is String -> Short.SIZE_BYTES + value.encodeToByteArray().size

            // Size of List + List Type ID Size
            is List<*> -> Int.SIZE_BYTES + Byte.SIZE_BYTES + value.sumOf {
                // Doesn't need Key Size Short Size nor TagID hence minus
                getTagSize("", it as Any) - Short.SIZE_BYTES - Byte.SIZE_BYTES
            }

            is NBT -> {

                val dataSize = value.map.entries.sumOf {
                    getTagSize(it.key, it.value)
                }

                // Map Size + End Tag Size
                dataSize + Byte.SIZE_BYTES
            }

            is ByteArray -> Int.SIZE_BYTES + value.size
            is IntArray -> Int.SIZE_BYTES + value.size * Int.SIZE_BYTES
            is LongArray -> Int.SIZE_BYTES + value.size * Long.SIZE_BYTES

            else -> error("Unknown tag: [${value::class.simpleName}]")
        }
    }


    // Push without ID
    private fun write(output: DataWritable, name: String?, value: Any) {

        when (value) {

            is Byte -> output.writeByte(value)
            is Short -> output.writeShort(value)
            is Int -> output.writeInt(value)
            is Long -> output.writeLong(value)
            is Float -> output.writeFloat(value)
            is Double -> output.writeDouble(value)

            is ByteArray -> {
                output.writeInt(value.size)
                output.writeByteArray(value)
            }


            is String -> output.writeUTF8(value)

            is List<*> -> {

                // Defaults to tag end type
                val listId = listIDs[name]!!

                output.writeByte(listId.toByte())
                output.writeInt(value.size)

                value.forEach {
                    write(output, null, it!!)
                }
            }

            is NBT -> {

                // If not in a list
                if (name != null) {
                    output.writeByte(10)   // Compound ID
                    output.writeUTF8(name) // Compound Name
                }

                value.map.forEach { (name, value) ->

                    val id = idFor(value)

                    // Is not compound
                    if (id != 10) {
                        output.writeByte(id.toByte())
                        output.writeUTF8(name)
                    }

                    write(output, name, value)
                }

                // End tag
                output.writeByte(0)
            }

            is IntArray -> {
                output.writeInt(value.size)
                value.forEach { output.writeInt(it) }
            }

            is LongArray -> {
                output.writeInt(value.size)
                value.forEach { output.writeLong(it) }
            }

            else -> error("Unknown tag: [${value::class.simpleName}] $name = $value")
        }
    }

    private fun idFor(value: Any) = when (value) {

        is Byte -> 1
        is Short -> 2
        is Int -> 3
        is Long -> 4
        is Float -> 5
        is Double -> 6
        is ByteArray -> 7
        is String -> 8
        is List<*> -> 9
        is NBT -> 10
        is IntArray -> 11
        is LongArray -> 12

        else -> error("Unknown tag: [${value::class.simpleName}] $name = $value")
    }


    companion object {

        fun pull(input: File, endianness: ByteOrder = ByteOrder.BIG_ENDIAN): NBT {
            return pull(input.readBytes(), endianness)
        }

        // Don't forget to close yourself
        // Provide a buffered stream please <3
        fun pull(input: ByteArray, endianness: ByteOrder = ByteOrder.BIG_ENDIAN): NBT {
            return pull(ByteBuffer.wrap(input).apply { order(endianness) })
        }

        fun pull(input: ByteBuffer): NBT {
            return pull(ByteBuf(input))
        }

        fun pull(input: DataReadable): NBT {
            return if (input is AutoCloseable) {
                input.use { read(input) }
            } else {
                read(input)
            }
        }


        private fun read(input: DataReadable, readID: Boolean = true, name: String? = null): NBT {

            if (readID) {
                check(input.readByte() == 10.toByte()) {
                    "Expected a compound, didn't get one :C"
                }
            }

            val nbt = NBT(name ?: input.readUTF8(), mapOf())


            while (true) {

                val inID = input.readByte().toInt()

                val inName = if (inID != 0) input.readUTF8() else ""

                // Is end
                if (inID == 0) {
                    break
                }

                nbt.map[inName] = readTag(input, inID, nbt, inName)
            }

            return nbt
        }

        private fun readList(name: String, nbt: NBT, input: DataReadable): List<*> {

            val inID = input.readByte().toInt()
            val size = input.readInt()

            nbt.listIDs[name] = inID

            return List(size) {
                readTag(input, inID, nbt, "")
            }
        }

        private fun readTag(input: DataReadable, id: Int, nbt: NBT, name: String?): Any = when (id) {

            1 -> input.readByte()
            2 -> input.readShort()
            3 -> input.readInt()
            4 -> input.readLong()
            5 -> input.readFloat()
            6 -> input.readDouble()
            7 -> input.readByteArray(input.readInt())
            8 -> input.readUTF8()
            9 -> readList(name!!, nbt, input)
            10 -> read(input, false, name)
            11 -> IntArray(input.readInt()) { input.readInt() }
            12 -> LongArray(input.readInt()) { input.readLong() }

            else -> error("Invalid NBT id: $id")
        }

        private fun DataReadable.readUTF8(): String {
            return readByteArray(readShort().toInt()).decodeToString()
        }

        private fun DataWritable.writeUTF8(text: String) {
            val byteArray = text.encodeToByteArray()
            writeShort(byteArray.size.toShort())
            writeByteArray(byteArray)
        }
    }


}