package org.tasks.extensions

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Deep tests for IntentExtensions.kt covering all branches including flagsToString.
 */
class IntentExtensionsDeepTest {

    private val FLAG_ACTIVITY_NEW_TASK = 0x10000000
    private val FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 0x00100000
    private val FLAG_ACTIVITY_BROUGHT_TO_FRONT = 0x00400000

    private fun intentWithFlags(flags: Int): Intent {
        val intent = mock(Intent::class.java)
        `when`(intent.flags).thenReturn(flags)
        return intent
    }

    // ===== FLAG_FROM_HISTORY constant =====

    @Test
    fun flagFromHistory_isCombinedFlags() {
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            FLAG_FROM_HISTORY
        )
    }

    // ===== isFromHistory =====

    @Test
    fun isFromHistory_withBothFlags() {
        val intent = intentWithFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        assertTrue(intent.isFromHistory)
    }

    @Test
    fun isFromHistory_withOnlyNewTask() {
        val intent = intentWithFlags(FLAG_ACTIVITY_NEW_TASK)
        assertFalse(intent.isFromHistory)
    }

    @Test
    fun isFromHistory_withOnlyLaunchedFromHistory() {
        val intent = intentWithFlags(FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        assertFalse(intent.isFromHistory)
    }

    @Test
    fun isFromHistory_noFlags() {
        val intent = intentWithFlags(0)
        assertFalse(intent.isFromHistory)
    }

    @Test
    fun isFromHistory_allFlags() {
        val intent = intentWithFlags(
            FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY or FLAG_ACTIVITY_BROUGHT_TO_FRONT
        )
        assertTrue(intent.isFromHistory)
    }

    // ===== broughtToFront =====

    @Test
    fun broughtToFront_withFlag() {
        val intent = intentWithFlags(FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        assertTrue(intent.broughtToFront)
    }

    @Test
    fun broughtToFront_noFlags() {
        val intent = intentWithFlags(0)
        assertFalse(intent.broughtToFront)
    }

    @Test
    fun broughtToFront_withOtherFlags() {
        val intent = intentWithFlags(FLAG_ACTIVITY_NEW_TASK)
        assertFalse(intent.broughtToFront)
    }

    @Test
    fun broughtToFront_combinedFlags() {
        val intent = intentWithFlags(FLAG_ACTIVITY_BROUGHT_TO_FRONT or FLAG_ACTIVITY_NEW_TASK)
        assertTrue(intent.broughtToFront)
    }

    // ===== flagsToString =====

    @Test
    fun flagsToString_noFlags_producesString() {
        val intent = intentWithFlags(0)
        val result = intent.flagsToString
        assertNotNull(result)
    }

    @Test
    fun flagsToString_withNewTaskFlag_containsFlagName() {
        val intent = intentWithFlags(FLAG_ACTIVITY_NEW_TASK)
        val result = intent.flagsToString
        assertNotNull(result)
        // Should contain FLAG_ prefix
        assertTrue(result.contains("FLAG_"))
    }

    @Test
    fun flagsToString_withBroughtToFront() {
        val intent = intentWithFlags(FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        val result = intent.flagsToString
        assertNotNull(result)
    }

    @Test
    fun flagsToString_multipleFlags() {
        val intent = intentWithFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        val result = intent.flagsToString
        assertNotNull(result)
        assertTrue(result.contains("|") || result.contains("FLAG_"))
    }
}
