package org.tasks.jobs

import org.junit.Assert.assertEquals
import org.junit.Test

class MigrateLocalWorkTest {

    @Test
    fun extraAccountConstant() {
        assertEquals("extra_account", MigrateLocalWork.EXTRA_ACCOUNT)
    }
}
