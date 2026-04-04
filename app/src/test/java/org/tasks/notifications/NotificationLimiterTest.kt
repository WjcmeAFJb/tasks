package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationLimiterTest {

    private lateinit var limiter: NotificationLimiter

    @Before
    fun setUp() {
        limiter = NotificationLimiter(3)
    }

    @Test
    fun addSingleIdNoEviction() {
        val evicted = limiter.add(1L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun addUpToMaxSizeNoEviction() {
        val evicted1 = limiter.add(1L)
        val evicted2 = limiter.add(2L)
        val evicted3 = limiter.add(3L)
        assertTrue(evicted1.isEmpty())
        assertTrue(evicted2.isEmpty())
        assertTrue(evicted3.isEmpty())
    }

    @Test
    fun addBeyondMaxSizeEvictsOldest() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        val evicted = limiter.add(4L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun evictsInFifoOrder() {
        limiter.add(10L)
        limiter.add(20L)
        limiter.add(30L)
        val evicted1 = limiter.add(40L)
        assertEquals(listOf(10L), evicted1)
        val evicted2 = limiter.add(50L)
        assertEquals(listOf(20L), evicted2)
    }

    @Test
    fun summaryNotificationDoesNotGoIntoQueue() {
        val summaryId = NotificationManager.SUMMARY_NOTIFICATION_ID.toLong()
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        // Adding summary should not evict because it doesn't go in the queue,
        // but it does count toward size
        val evicted = limiter.add(summaryId)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun summaryCountsTowardSizeLimit() {
        val summaryId = NotificationManager.SUMMARY_NOTIFICATION_ID.toLong()
        limiter.add(summaryId)
        limiter.add(1L)
        limiter.add(2L)
        // Size is now 3 (2 in queue + 1 summary). Adding another should evict.
        val evicted = limiter.add(3L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun removeSummaryFreesSpace() {
        val summaryId = NotificationManager.SUMMARY_NOTIFICATION_ID.toLong()
        limiter.add(summaryId)
        limiter.add(1L)
        limiter.add(2L)
        limiter.remove(summaryId)
        // Now size is 2, so adding another should not evict
        val evicted = limiter.add(3L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun removeRegularIdFreesSpace() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        limiter.remove(2L)
        // Now size is 2, so adding another should not evict
        val evicted = limiter.add(4L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun removeMultipleIds() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        limiter.remove(listOf(1L, 2L))
        // Size is now 1; can add 2 more without eviction
        val evicted1 = limiter.add(4L)
        val evicted2 = limiter.add(5L)
        assertTrue(evicted1.isEmpty())
        assertTrue(evicted2.isEmpty())
    }

    @Test
    fun addDuplicateIdDoesNotIncrementSize() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        // Adding 2 again (already present) should remove it first, then re-add
        val evicted = limiter.add(2L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun duplicateIdMovesToEndOfQueue() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        // Re-add 1, which removes it and adds to end: queue = [2, 3, 1]
        limiter.add(1L)
        // Adding 4 should evict 2 (new oldest)
        val evicted = limiter.add(4L)
        assertEquals(listOf(2L), evicted)
    }

    @Test
    fun removeNonExistentIdDoesNothing() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.remove(999L)
        // Size should still be 2
        val evicted = limiter.add(3L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun maxSizeOneEvictsImmediately() {
        val smallLimiter = NotificationLimiter(1)
        smallLimiter.add(1L)
        val evicted = smallLimiter.add(2L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun addManySummaryIdempotent() {
        val summaryId = NotificationManager.SUMMARY_NOTIFICATION_ID.toLong()
        limiter.add(summaryId)
        limiter.add(summaryId)
        limiter.add(summaryId)
        // Summary only counts as 1 regardless of how many times added
        limiter.add(1L)
        limiter.add(2L)
        // Size = 3 (2 queue + 1 summary), next add evicts
        val evicted = limiter.add(3L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun multipleEvictionsAtOnce() {
        // With max size 3, add 5 regular IDs
        // After first 3, each new add evicts one.
        // But if we simulate going from 0 to over capacity in a different way:
        val tinyLimiter = NotificationLimiter(2)
        tinyLimiter.add(1L)
        tinyLimiter.add(2L)
        tinyLimiter.add(3L)
        // Should have evicted 1
        val evicted = tinyLimiter.add(4L)
        assertEquals(listOf(2L), evicted)
    }

    @Test
    fun removeEmptyIterableDoesNothing() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.remove(emptyList())
        // Size should still be 2
        val evicted = limiter.add(3L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun removeSummaryWhenNotSetDoesNothing() {
        val summaryId = NotificationManager.SUMMARY_NOTIFICATION_ID.toLong()
        limiter.add(1L)
        limiter.add(2L)
        limiter.remove(summaryId) // summary was never added
        // Size should still be 2
        val evicted = limiter.add(3L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun largeLimiterHandlesManyItems() {
        val largeLimiter = NotificationLimiter(100)
        for (i in 1L..100L) {
            val evicted = largeLimiter.add(i)
            assertTrue(evicted.isEmpty())
        }
        // 101st should evict 1
        val evicted = largeLimiter.add(101L)
        assertEquals(listOf(1L), evicted)
    }
}
