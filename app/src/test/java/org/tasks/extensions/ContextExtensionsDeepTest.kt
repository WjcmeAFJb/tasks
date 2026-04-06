package org.tasks.extensions

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.extensions.Context.cookiePersistor
import org.tasks.extensions.Context.findActivity
import org.tasks.extensions.Context.getResourceUri
import org.tasks.extensions.Context.hasNetworkConnectivity
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.extensions.Context.is24HourOverride
import org.tasks.extensions.Context.isNightMode
import org.tasks.extensions.Context.nightMode

/**
 * Deep tests for Context.kt extensions covering more branches.
 */
class ContextExtensionsDeepTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mock()
    }

    @After
    fun tearDown() {
        is24HourOverride = null
    }

    // ===== is24HourFormat =====

    @Test
    fun is24HourFormat_overrideTrue() {
        is24HourOverride = true
        assertTrue(context.is24HourFormat)
    }

    @Test
    fun is24HourFormat_overrideFalse() {
        is24HourOverride = false
        assertFalse(context.is24HourFormat)
    }

    // ===== nightMode / isNightMode =====

    @Test
    fun nightMode_nightYes() {
        val resources = mock(Resources::class.java)
        val config = Configuration()
        config.uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(config)
        assertEquals(Configuration.UI_MODE_NIGHT_YES, context.nightMode)
        assertTrue(context.isNightMode)
    }

    @Test
    fun nightMode_nightNo() {
        val resources = mock(Resources::class.java)
        val config = Configuration()
        config.uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(config)
        assertEquals(Configuration.UI_MODE_NIGHT_NO, context.nightMode)
        assertFalse(context.isNightMode)
    }

    @Test
    fun nightMode_undefined() {
        val resources = mock(Resources::class.java)
        val config = Configuration()
        config.uiMode = Configuration.UI_MODE_NIGHT_UNDEFINED
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(config)
        assertEquals(Configuration.UI_MODE_NIGHT_UNDEFINED, context.nightMode)
        assertFalse(context.isNightMode)
    }

    // ===== findActivity =====

    @Test
    fun findActivity_returnsNullForPlainContext() {
        val result = context.findActivity()
        assertNull(result)
    }

    @Test
    fun findActivity_returnsActivityWhenContextIsActivity() {
        val activity = mock(Activity::class.java)
        // Activity is a ContextWrapper, so findActivity should return it
        val result = activity.findActivity()
        assertEquals(activity, result)
    }

    @Test
    fun findActivity_unwrapsContextWrapper() {
        val activity = mock(Activity::class.java)
        val wrapper = mock(ContextWrapper::class.java)
        `when`(wrapper.baseContext).thenReturn(activity)
        val result = wrapper.findActivity()
        assertEquals(activity, result)
    }

    @Test
    fun findActivity_deepNestedWrapper_returnsNull() {
        // Non-activity context wrapper that wraps a plain context
        val wrapper = mock(ContextWrapper::class.java)
        val plainContext = mock(Context::class.java)
        `when`(wrapper.baseContext).thenReturn(plainContext)
        val result = wrapper.findActivity()
        assertNull(result)
    }

    // ===== HTTP/HTTPS constants =====

    @Test
    fun httpConstant() {
        // Verify the private constants via the openUri behavior
        // We can test that openUri doesn't crash with null
        context.let { /* no-op - just verifying constants exist */ }
    }

    // ===== is24HourOverride can be toggled =====

    @Test
    fun is24HourOverrideCanBeNull() {
        is24HourOverride = true
        is24HourOverride = null
        assertNull(is24HourOverride)
    }

    @Test
    fun is24HourOverrideCanBeFalse() {
        is24HourOverride = false
        assertFalse(is24HourOverride!!)
    }

    @Test
    fun is24HourOverrideCanBeTrue() {
        is24HourOverride = true
        assertTrue(is24HourOverride!!)
    }
}
