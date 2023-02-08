package org.endoqa.fastly.io

import java.io.ByteArrayOutputStream

private const val SEGMENT_BITS = 0x7F
private const val CONTINUE_BIT = 0x80


fun ByteArrayOutputStream.writeVarInt(v: Int) {
    var value = v
    while (true) {
        if (value and SEGMENT_BITS.inv() == 0) {
            write(value)
            return
        }
        write(value and SEGMENT_BITS or CONTINUE_BIT)

        // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
        value = value ushr 7
    }
}