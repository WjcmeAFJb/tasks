package org.tasks.jobs

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncWorkTest {

    @Test
    fun extraSourceConstant() {
        assertEquals("extra_source", SyncWork.EXTRA_SOURCE)
    }
}
