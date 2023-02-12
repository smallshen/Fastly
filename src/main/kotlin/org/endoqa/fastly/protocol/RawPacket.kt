package org.endoqa.fastly.protocol

import kotlinx.coroutines.CompletableJob
import java.nio.ByteBuffer

data class RawPacket(
    val length: Int,
    val packetId: Int,
    val buffer: ByteBuffer,
    val attachJob: CompletableJob? = null
) {

    override fun toString(): String {
        return "RawPacket(length=$length, packetId=0x${Integer.toHexString(packetId)}, buffer=$buffer, attachJob=$attachJob)"
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

}