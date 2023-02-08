package org.endoqa.fastly.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.endoqa.fastly.nio.ByteBuf
import java.nio.ByteBuffer

class CalculateVarIntSizeKtTest : FunSpec({

    test("calculateVarIntSize") {
        val buf = ByteBuffer.allocate(10)
        repeat(Int.MAX_VALUE) { num ->
            buf.clear()

            ByteBuf(buf).writeVarInt(num)
            calculateVarIntSize(num) shouldBe buf.position()
        }
    }
})
