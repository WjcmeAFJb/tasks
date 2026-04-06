package org.tasks.extensions

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class IntentExtensionsTest {

    // Known Android constant values
    private val FLAG_ACTIVITY_NEW_TASK = 0x10000000
    private val FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 0x00100000
    private val FLAG_ACTIVITY_BROUGHT_TO_FRONT = 0x00400000

    private fun intentWithFlags(flags: Int): Intent {
        val intent = mock(Intent::class.java)
        `when`(intent.flags).thenReturn(flags)
        return intent
    }

    // --- isFromHistory tests ---

    @Test
    fun isFromHistory_withBothFlags_returnsTrue() {
        val flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
        val intent = intentWithFlags(flags)
        assertTrue(intent.isFromHistory)
    }

    @Test
    fun isFromHistory_withOnlyNewTask_returnsFalse() {
        val intent = intentWithFlags(FLAG_ACTIVITY_NEW_TASK)
        assertFalse(intent.isFromHistory)
    }

    @Test
    fun isFromHistory_withOnlyLaunchedFromHistory_returnsFalse() {
        val intent = intentWithFlags(FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        assertFalse(intent.isFromHistory)
    }

    @Test
    fun isFromHistory_withNoFlags_returnsFalse() {
        val intent = intentWithFlags(0)
        assertFalse(intent.isFromHistory)
    }

    @Test
    fun isFromHistory_withExtraFlags_returnsTrue() {
        val flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY or FLAG_ACTIVITY_BROUGHT_TO_FRONT
        val intent = intentWithFlags(flags)
        assertTrue(intent.isFromHistory)
    }

    // --- broughtToFront tests ---

    @Test
    fun broughtToFront_withFlag_returnsTrue() {
        val intent = intentWithFlags(FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        assertTrue(intent.broughtToFront)
    }

    @Test
    fun broughtToFront_withNoFlags_returnsFalse() {
        val intent = intentWithFlags(0)
        assertFalse(intent.broughtToFront)
    }

    @Test
    fun broughtToFront_withOtherFlags_returnsFalse() {
        val intent = intentWithFlags(FLAG_ACTIVITY_NEW_TASK)
        assertFalse(intent.broughtToFront)
    }

    @Test
    fun broughtToFront_withMultipleFlagsIncludingBroughtToFront_returnsTrue() {
        val flags = FLAG_ACTIVITY_BROUGHT_TO_FRONT or FLAG_ACTIVITY_NEW_TASK
        val intent = intentWithFlags(flags)
        assertTrue(intent.broughtToFront)
    }

    // --- FLAG_FROM_HISTORY constant test ---

    @Test
    fun flagFromHistoryConstant_isCombinationOfTwoFlags() {
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            FLAG_FROM_HISTORY
        )
    }
}
