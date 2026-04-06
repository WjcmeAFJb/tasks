package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionsTest {
    @Test fun upper() {
        val result = Functions.upper(Field("title"))
        assertEquals("UPPER(title)", result.expression)
    }

    @Test fun now() {
        val result = Functions.now()
        assertTrue(result.expression.contains("strftime"))
    }
}
