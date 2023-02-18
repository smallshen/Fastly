package org.endoqa.fastly.util.protocol

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe

class PositionTest : FunSpec({

    test("validate decode") {
        val position = Position(5046110948485792575)

        position.x shouldBeExactly 18357644
        position.y shouldBeExactly 831
        position.z shouldBeExactly -20882616
    }

    test("validate encode") {
        val position = Position(18357644, 831, -20882616)

        position shouldBe Position(5046110948485792575)
    }
})
