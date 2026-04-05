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
    fun addSingleItemNoEviction() {
        val evicted = limiter.add(1L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun addUpToMaxNoEviction() {
        val evicted1 = limiter.add(1L)
        val evicted2 = limiter.add(2L)
        val evicted3 = limiter.add(3L)
        assertTrue(evicted1.isEmpty())
        assertTrue(evicted2.isEmpty())
        assertTrue(evicted3.isEmpty())
    }

    @Test
    fun addBeyondMaxEvictsOldest() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        val evicted = limiter.add(4L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun addMultipleBeyondMaxEvictsMultiple() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        limiter.add(4L)
        val evicted = limiter.add(5L)
        assertEquals(listOf(2L), evicted)
    }

    @Test
    fun addExistingItemDoesNotDuplicate() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        val evicted = limiter.add(1L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun addExistingItemMovesToEnd() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        limiter.add(1L)
        val evicted = limiter.add(4L)
        assertEquals(listOf(2L), evicted)
    }

    @Test
    fun summaryNotificationCountsTowardLimit() {
        limiter.add(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())
        limiter.add(1L)
        limiter.add(2L)
        val evicted = limiter.add(3L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun summaryNotificationIsNeverEvicted() {
        limiter.add(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())
        limiter.add(1L)
        limiter.add(2L)
        val evicted = limiter.add(3L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun addSummaryTwiceDoesNotDoubleCounting() {
        limiter.add(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())
        limiter.add(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())
        limiter.add(1L)
        limiter.add(2L)
        val evicted = limiter.add(3L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun removeFreesSlot() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        limiter.remove(1L)
        val evicted = limiter.add(4L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun removeNonexistentIdIsNoOp() {
        limiter.add(1L)
        limiter.remove(999L)
        val evicted = limiter.add(2L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun removeSummaryFreesSlot() {
        limiter.add(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())
        limiter.add(1L)
        limiter.add(2L)
        limiter.remove(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())
        val evicted = limiter.add(3L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun removeMultipleIds() {
        limiter.add(1L)
        limiter.add(2L)
        limiter.add(3L)
        limiter.remove(listOf(1L, 2L))
        val evicted1 = limiter.add(4L)
        val evicted2 = limiter.add(5L)
        assertTrue(evicted1.isEmpty())
        assertTrue(evicted2.isEmpty())
    }

    @Test
    fun removeEmptyIterableIsNoOp() {
        limiter.add(1L)
        limiter.remove(emptyList())
        val evicted = limiter.add(2L)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun limiterWithSizeOne() {
        val smallLimiter = NotificationLimiter(1)
        smallLimiter.add(1L)
        val evicted = smallLimiter.add(2L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun limiterWithSizeOneAndSummary() {
        val smallLimiter = NotificationLimiter(1)
        smallLimiter.add(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())
        val evicted = smallLimiter.add(1L)
        assertEquals(listOf(1L), evicted)
    }

    @Test
    fun evictionOrderIsFifo() {
        limiter = NotificationLimiter(2)
        limiter.add(10L)
        limiter.add(20L)
        val evicted1 = limiter.add(30L)
        assertEquals(listOf(10L), evicted1)
        val evicted2 = limiter.add(40L)
        assertEquals(listOf(20L), evicted2)
    }
}
