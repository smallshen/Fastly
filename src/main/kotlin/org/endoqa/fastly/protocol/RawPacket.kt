package org.endoqa.fastly.protocol

import kotlinx.coroutines.CompletableJob
import java.nio.ByteBuffer

data class RawPacket(
    val length: Int,
    val packetId: Int,
    val buffer: ByteBuffer,
    val attachJob: CompletableJob? = null
) {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

}