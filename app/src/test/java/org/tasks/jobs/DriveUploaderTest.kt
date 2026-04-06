package org.tasks.jobs

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveUploaderTest {

    @Test
    fun extraUriConstant() {
        assertEquals("extra_uri", DriveUploader.EXTRA_URI)
    }

    @Test
    fun extraPurgeConstant() {
        assertEquals("extra_purge", DriveUploader.EXTRA_PURGE)
    }
}
