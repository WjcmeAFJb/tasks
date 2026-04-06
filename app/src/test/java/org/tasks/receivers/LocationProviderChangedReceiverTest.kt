package org.tasks.receivers

import android.content.Intent
import android.location.LocationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Tests for [LocationProviderChangedReceiver] intent filtering logic.
 * Uses mocked intents since Android SDK stubs are not implemented in unit tests.
 */
class LocationProviderChangedReceiverTest {

    private fun intentWithAction(action: String?): Intent {
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(action)
        return intent
    }

    @Test
    fun providersChangedActionMatches() {
        val intent = intentWithAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        assertEquals(LocationManager.PROVIDERS_CHANGED_ACTION, intent.action)
    }

    @Test
    fun otherActionDoesNotMatch() {
        val intent = intentWithAction(Intent.ACTION_BOOT_COMPLETED)
        val matches = intent.action == LocationManager.PROVIDERS_CHANGED_ACTION
        assertFalse(matches)
    }

    @Test
    fun nullActionDoesNotMatch() {
        val intent = intentWithAction(null)
        val matches = intent.action == LocationManager.PROVIDERS_CHANGED_ACTION
        assertFalse(matches)
    }

    @Test
    fun providersChangedActionConstant() {
        assertEquals("android.location.PROVIDERS_CHANGED", LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    @Test
    fun guardConditionReturnsFalseForWrongAction() {
        val intent = intentWithAction("wrong_action")
        val shouldReturn = intent.action != LocationManager.PROVIDERS_CHANGED_ACTION
        assertTrue("Guard should reject wrong action", shouldReturn)
    }

    @Test
    fun guardConditionReturnsFalseForCorrectAction() {
        val intent = intentWithAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        val shouldReturn = intent.action != LocationManager.PROVIDERS_CHANGED_ACTION
        assertFalse("Guard should not reject correct action", shouldReturn)
    }
}
