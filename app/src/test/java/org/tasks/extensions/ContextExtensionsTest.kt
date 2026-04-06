package org.tasks.extensions

import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.extensions.Context.getResourceUri
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.extensions.Context.is24HourOverride
import org.tasks.extensions.Context.nightMode
import org.tasks.extensions.Context.isNightMode
import org.tasks.extensions.Context.cookiePersistor

/**
 * Tests for Context.kt extension functions that can be unit tested
 * without a real Android environment.
 */
class ContextExtensionsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mock(Context::class.java)
    }

    // ===== is24HourFormat tests =====

    @Test
    fun is24HourFormatReturnsTrueWhenOverrideTrue() {
        is24HourOverride = true
        assertTrue(context.is24HourFormat)
    }

    @Test
    fun is24HourFormatReturnsFalseWhenOverrideFalse() {
        is24HourOverride = false
        assertEquals(false, context.is24HourFormat)
    }

    // ===== nightMode tests =====

    @Test
    fun nightModeReturnsCorrectValue() {
        val resources = mock(Resources::class.java)
        val configuration = Configuration()
        configuration.uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(configuration)

        val result = context.nightMode
        assertEquals(Configuration.UI_MODE_NIGHT_YES, result)
    }

    @Test
    fun nightModeReturnsNightNo() {
        val resources = mock(Resources::class.java)
        val configuration = Configuration()
        configuration.uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(configuration)

        val result = context.nightMode
        assertEquals(Configuration.UI_MODE_NIGHT_NO, result)
    }

    @Test
    fun nightModeReturnsUndefined() {
        val resources = mock(Resources::class.java)
        val configuration = Configuration()
        configuration.uiMode = Configuration.UI_MODE_NIGHT_UNDEFINED or Configuration.UI_MODE_TYPE_NORMAL
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(configuration)

        val result = context.nightMode
        assertEquals(Configuration.UI_MODE_NIGHT_UNDEFINED, result)
    }

    // ===== isNightMode tests =====

    @Test
    fun isNightModeReturnsTrueWhenNightYes() {
        val resources = mock(Resources::class.java)
        val configuration = Configuration()
        configuration.uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(configuration)

        assertTrue(context.isNightMode)
    }

    @Test
    fun isNightModeReturnsFalseWhenNightNo() {
        val resources = mock(Resources::class.java)
        val configuration = Configuration()
        configuration.uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(configuration)

        assertEquals(false, context.isNightMode)
    }

    @Test
    fun isNightModeReturnsFalseWhenUndefined() {
        val resources = mock(Resources::class.java)
        val configuration = Configuration()
        configuration.uiMode = Configuration.UI_MODE_NIGHT_UNDEFINED or Configuration.UI_MODE_TYPE_NORMAL
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(configuration)

        assertEquals(false, context.isNightMode)
    }

    // Note: getResourceUri tests require Uri.Builder which is not
    // available in unit test stubs. These would need Robolectric.

    // ===== is24HourOverride reset =====

    @Test
    fun is24HourOverrideCanBeSetToNull() {
        is24HourOverride = true
        is24HourOverride = null
        // When null, it would use DateFormat.is24HourFormat in production
        // We just verify it doesn't crash
        assertEquals(null, is24HourOverride)
    }
}
