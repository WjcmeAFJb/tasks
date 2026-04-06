package org.tasks.data

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UUIDHelperTest {

    @Test
    fun newUUIDReturnsNonNull() {
        assertNotNull(UUIDHelper.newUUID())
    }

    @Test
    fun newUUIDReturnsNonEmptyString() {
        assertTrue(UUIDHelper.newUUID().isNotEmpty())
    }

    @Test
    fun newUUIDReturnsNumericString() {
        val uuid = UUIDHelper.newUUID()
        // Should be parseable as a Long
        uuid.toLong()
    }

    @Test
    fun newUUIDIsAboveMinimum() {
        val uuid = UUIDHelper.newUUID().toLong()
        assertTrue("UUID should be >= 100000000, was $uuid", uuid >= 100000000L)
    }

    @Test
    fun newUUIDIsPositive() {
        val uuid = UUIDHelper.newUUID().toLong()
        assertTrue("UUID should be positive, was $uuid", uuid > 0)
    }

    @Test
    fun consecutiveUUIDsAreDifferent() {
        val uuid1 = UUIDHelper.newUUID()
        val uuid2 = UUIDHelper.newUUID()
        assertNotEquals(uuid1, uuid2)
    }

    @Test
    fun manyUUIDsAreUnique() {
        val uuids = (1..100).map { UUIDHelper.newUUID() }.toSet()
        assertTrue("Expected 100 unique UUIDs but got ${uuids.size}", uuids.size == 100)
    }

    @Test
    fun uuidFitsInLong() {
        // The implementation masks with 0x7fffffffffffffffL ensuring positive Long values
        repeat(50) {
            val value = UUIDHelper.newUUID().toLong()
            assertTrue("UUID should fit in Long range, was $value", value > 0)
        }
    }
}
