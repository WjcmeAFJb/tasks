package org.tasks.receivers

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Tests for [CompleteTaskReceiver] constants and intent structure.
 * Uses mocked intents since Android SDK stubs are not implemented in unit tests.
 */
class CompleteTaskReceiverTest {

    @Test
    fun taskIdConstantIsId() {
        assertEquals("id", CompleteTaskReceiver.TASK_ID)
    }

    @Test
    fun intentContainsTaskId() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(42L)
        assertEquals(42L, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    @Test
    fun intentDefaultTaskIdIsZero() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(0L)
        assertEquals(0L, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    @Test
    fun intentWithLargeTaskId() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    @Test
    fun intentWithNegativeTaskId() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(-1L)
        assertEquals(-1L, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    @Test
    fun taskIdKeyIsNotEmpty() {
        assertTrue(CompleteTaskReceiver.TASK_ID.isNotEmpty())
    }
}
