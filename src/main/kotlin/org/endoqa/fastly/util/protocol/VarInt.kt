package org.endoqa.fastly.util.protocol


const val SEGMENT_BITS = 0x7F
const val CONTINUE_BIT = 0x80

inline fun readVarInt(
    nextByte: () -> Byte
): Int {
    var value = 0
    var position = 0
    var currentByte: Int


    repeat(5) {
        currentByte = nextByte().toInt()
        value = value or (currentByte and SEGMENT_BITS shl position)

        if (currentByte and CONTINUE_BIT == 0) {
            return value
        }

        position += 7
        if (position >= 32) throw RuntimeException("VarInt is too big")
    }

    error("VarInt size overflow")
}

inline fun writeVarInt(
    int: Int,
    writeByte: (Byte) -> Unit,
) {
    var value = int
    while (true) {
        if (value and SEGMENT_BITS.inv() == 0) {
            writeByte(value.toByte())
            return
        }
        writeByte((value and SEGMENT_BITS or CONTINUE_BIT).toByte())

        // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
        value = value ushr 7
    }
}