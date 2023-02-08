package org.endoqa.fastly.util

/**
 * 	UTF-8 string prefixed with its size in bytes as a VarInt.
 *  Maximum length of n characters, which varies by context; up
 *  to n × 4 bytes can be used to encode n characters and both of
 *  those limits are checked. Maximum n value is 32767.
 *
 *  The + 3 is due to the max size of a valid length VarInt.
 */
val String.estimateStringSizeInBytes: Int
    get() {
        return (length * 4) + 3
    }