package org.endoqa.fastly.util.protocol

/**
 * 64-bit value split into three signed integer parts:
 *
 * x: 26 MSBs
 * z: 26 middle bits
 * y: 12 LSBs
 * For example, a 64-bit position can be broken down as follows:
 *
 * Example value (big endian): 01000110000001110110001100 10110000010101101101001000 001100111111
 * The red value is the X coordinate, which is 18357644 in this example.
 * The blue value is the Z coordinate, which is -20882616 in this example.
 * The green value is the Y coordinate, which is 831 in this example.
 * Encoded as followed:
 *
 * ((x & 0x3FFFFFF) << 38) | ((z & 0x3FFFFFF) << 12) | (y & 0xFFF)
 * And decoded as:
 *
 * val = read_long();
 * x = val >> 38;
 * y = val << 52 >> 52;
 * z = val << 26 >> 38;
 * Note: The above assumes that the right shift operator sign extends the value (this is called an arithmetic shift), so that the signedness of the coordinates is preserved. In many languages, this requires the integer type of val to be signed. In the absence of such an operator, the following may be useful:
 *
 * if x >= 1 << 25 { x -= 1 << 26 }
 * if y >= 1 << 11 { y -= 1 << 12 }
 * if z >= 1 << 25 { z -= 1 << 26 }
 */
@JvmInline
value class Position(val value: Long) {

    constructor(x: Int, y: Int, z: Int) : this(
        0L or
                ((x.toLong() and 0x3FFFFFF) shl 38) or
                ((z.toLong() and 0x3FFFFFF) shl 12) or
                (y.toLong() and 0xFFF)
    )

    val x: Int
        get() = (value shr 38).toInt()

    val y: Int
        get() = (value shl 52 shr 52).toInt()

    val z: Int
        get() = (value shl 26 shr 38).toInt()

}