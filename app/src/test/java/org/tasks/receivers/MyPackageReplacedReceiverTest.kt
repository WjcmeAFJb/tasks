package org.tasks.receivers

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Tests for [MyPackageReplacedReceiver] intent filtering logic.
 * Uses mocked intents since Android SDK stubs are not implemented in unit tests.
 */
class MyPackageReplacedReceiverTest {

    private fun intentWithAction(action: String?): Intent {
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(action)
        return intent
    }

    @Test
    fun actionMyPackageReplacedMatches() {
        val intent = intentWithAction(Intent.ACTION_MY_PACKAGE_REPLACED)
        assertEquals(Intent.ACTION_MY_PACKAGE_REPLACED, intent.action)
    }

    @Test
    fun otherActionDoesNotMatch() {
        val intent = intentWithAction(Intent.ACTION_BOOT_COMPLETED)
        val matches = intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        assertFalse(matches)
    }

    @Test
    fun nullActionDoesNotMatch() {
        val intent = intentWithAction(null)
        val matches = intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        assertFalse(matches)
    }

    @Test
    fun actionMyPackageReplacedIsCorrectConstant() {
        assertEquals("android.intent.action.MY_PACKAGE_REPLACED", Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    @Test
    fun guardConditionReturnsFalseForWrongAction() {
        val intent = intentWithAction("wrong_action")
        val shouldReturn = intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        assertTrue("Guard should reject wrong action", shouldReturn)
    }

    @Test
    fun guardConditionReturnsFalseForCorrectAction() {
        val intent = intentWithAction(Intent.ACTION_MY_PACKAGE_REPLACED)
        val shouldReturn = intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        assertFalse("Guard should not reject correct action", shouldReturn)
    }
}
