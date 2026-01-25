package com.cognihertz

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun gammaFrequency_is40Hz() {
        val gammaHz = 40.0
        assertEquals(40.0, gammaHz, 0.001)
    }
}
