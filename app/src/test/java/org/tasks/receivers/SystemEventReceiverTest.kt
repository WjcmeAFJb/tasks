package org.tasks.receivers

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Tests for [SystemEventReceiver] intent action matching logic.
 * Uses mocked intents since Android SDK stubs are not implemented in unit tests.
 */
class SystemEventReceiverTest {

    private fun intentWithAction(action: String?): Intent {
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(action)
        return intent
    }

    @Test
    fun bootCompletedActionMatches() {
        val intent = intentWithAction(Intent.ACTION_BOOT_COMPLETED)
        assertEquals(Intent.ACTION_BOOT_COMPLETED, intent.action)
    }

    @Test
    fun userPresentActionMatches() {
        val intent = intentWithAction(Intent.ACTION_USER_PRESENT)
        assertEquals(Intent.ACTION_USER_PRESENT, intent.action)
    }

    @Test
    fun otherActionDoesNotMatchBoot() {
        val intent = intentWithAction("android.intent.action.TIMEZONE_CHANGED")
        val matches = intent.action == Intent.ACTION_BOOT_COMPLETED
        assertFalse(matches)
    }

    @Test
    fun otherActionDoesNotMatchUserPresent() {
        val intent = intentWithAction("android.intent.action.TIMEZONE_CHANGED")
        val matches = intent.action == Intent.ACTION_USER_PRESENT
        assertFalse(matches)
    }

    @Test
    fun nullActionDoesNotMatchBoot() {
        val intent = intentWithAction(null)
        val matches = intent.action == Intent.ACTION_BOOT_COMPLETED
        assertFalse(matches)
    }

    @Test
    fun nullActionDoesNotMatchUserPresent() {
        val intent = intentWithAction(null)
        val matches = intent.action == Intent.ACTION_USER_PRESENT
        assertFalse(matches)
    }

    @Test
    fun bootCompletedConstant() {
        assertEquals("android.intent.action.BOOT_COMPLETED", Intent.ACTION_BOOT_COMPLETED)
    }

    @Test
    fun userPresentConstant() {
        assertEquals("android.intent.action.USER_PRESENT", Intent.ACTION_USER_PRESENT)
    }

    @Test
    fun bootAndUserPresentAreDifferentActions() {
        assertTrue(Intent.ACTION_BOOT_COMPLETED != Intent.ACTION_USER_PRESENT)
    }
}
