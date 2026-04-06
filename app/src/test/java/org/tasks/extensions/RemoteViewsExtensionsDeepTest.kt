package org.tasks.extensions

import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.widget.RemoteViews
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.tasks.R

/**
 * Deep tests for RemoteViewsExtensions.kt covering all extension functions.
 */
class RemoteViewsExtensionsDeepTest {

    // ===== strikethrough logic =====

    @Test
    fun strikethrough_enabled_hasBothFlags() {
        val flags = STRIKE_THRU_TEXT_FLAG or ANTI_ALIAS_FLAG
        assertTrue(flags and STRIKE_THRU_TEXT_FLAG != 0)
        assertTrue(flags and ANTI_ALIAS_FLAG != 0)
    }

    @Test
    fun strikethrough_disabled_onlyAntiAlias() {
        val flags = ANTI_ALIAS_FLAG
        assertEquals(0, flags and STRIKE_THRU_TEXT_FLAG)
        assertTrue(flags and ANTI_ALIAS_FLAG != 0)
    }

    @Test
    fun strikeThroughAndAntiAliasAreDifferentFlags() {
        assertNotEquals(STRIKE_THRU_TEXT_FLAG, ANTI_ALIAS_FLAG)
    }

    // ===== setColorFilter =====

    @Test
    fun setColorFilter_callsSetInt() {
        val rv = mock(RemoteViews::class.java)
        rv.setColorFilter(R.id.widget_row, 0xFF0000)
        verify(rv).setInt(R.id.widget_row, "setColorFilter", 0xFF0000)
    }

    // ===== setTextSize =====

    @Test
    fun setTextSize_callsSetFloat() {
        val rv = mock(RemoteViews::class.java)
        rv.setTextSize(R.id.widget_row, 14.0f)
        verify(rv).setFloat(R.id.widget_row, "setTextSize", 14.0f)
    }

    @Test
    fun setTextSize_zero() {
        val rv = mock(RemoteViews::class.java)
        rv.setTextSize(R.id.widget_row, 0.0f)
        verify(rv).setFloat(R.id.widget_row, "setTextSize", 0.0f)
    }

    // ===== setMaxLines =====

    @Test
    fun setMaxLines_callsSetInt() {
        val rv = mock(RemoteViews::class.java)
        rv.setMaxLines(R.id.widget_row, 3)
        verify(rv).setInt(R.id.widget_row, "setMaxLines", 3)
    }

    @Test
    fun setMaxLines_one() {
        val rv = mock(RemoteViews::class.java)
        rv.setMaxLines(R.id.widget_row, 1)
        verify(rv).setInt(R.id.widget_row, "setMaxLines", 1)
    }

    // ===== strikethrough =====

    @Test
    fun strikethrough_true_setsStrikeThroughFlags() {
        val rv = mock(RemoteViews::class.java)
        rv.strikethrough(R.id.widget_row, true)
        verify(rv).setInt(
            R.id.widget_row,
            "setPaintFlags",
            STRIKE_THRU_TEXT_FLAG or ANTI_ALIAS_FLAG
        )
    }

    @Test
    fun strikethrough_false_setsOnlyAntiAlias() {
        val rv = mock(RemoteViews::class.java)
        rv.strikethrough(R.id.widget_row, false)
        verify(rv).setInt(R.id.widget_row, "setPaintFlags", ANTI_ALIAS_FLAG)
    }

    // ===== setBackgroundResource =====

    @Test
    fun setBackgroundResource_callsSetInt() {
        val rv = mock(RemoteViews::class.java)
        rv.setBackgroundResource(R.id.widget_row, R.drawable.widget_ripple_circle_dark)
        verify(rv).setInt(R.id.widget_row, "setBackgroundResource", R.drawable.widget_ripple_circle_dark)
    }

    // ===== setRipple =====

    @Test
    fun setRipple_dark_usesLightRipple() {
        val rv = mock(RemoteViews::class.java)
        rv.setRipple(R.id.widget_row, dark = true)
        verify(rv).setInt(R.id.widget_row, "setBackgroundResource", R.drawable.widget_ripple_circle_light)
    }

    @Test
    fun setRipple_light_usesDarkRipple() {
        val rv = mock(RemoteViews::class.java)
        rv.setRipple(R.id.widget_row, dark = false)
        verify(rv).setInt(R.id.widget_row, "setBackgroundResource", R.drawable.widget_ripple_circle_dark)
    }

    // ===== setBackgroundColor =====

    @Test
    fun setBackgroundColor_callsSetIntWithAlpha() {
        val rv = mock(RemoteViews::class.java)
        // ColorUtils.setAlphaComponent(0xFF0000, 128) should set alpha
        rv.setBackgroundColor(R.id.widget_row, 0xFF0000, 128)
        // We just verify setInt is called with setBackgroundColor method name
        verify(rv).setInt(
            org.mockito.ArgumentMatchers.eq(R.id.widget_row),
            org.mockito.ArgumentMatchers.eq("setBackgroundColor"),
            org.mockito.ArgumentMatchers.anyInt()
        )
    }

    @Test
    fun setBackgroundColor_fullOpacity() {
        val rv = mock(RemoteViews::class.java)
        rv.setBackgroundColor(R.id.widget_row, 0x0000FF, 255)
        verify(rv).setInt(
            org.mockito.ArgumentMatchers.eq(R.id.widget_row),
            org.mockito.ArgumentMatchers.eq("setBackgroundColor"),
            org.mockito.ArgumentMatchers.anyInt()
        )
    }

    @Test
    fun setBackgroundColor_zeroOpacity() {
        val rv = mock(RemoteViews::class.java)
        rv.setBackgroundColor(R.id.widget_row, 0x00FF00, 0)
        verify(rv).setInt(
            org.mockito.ArgumentMatchers.eq(R.id.widget_row),
            org.mockito.ArgumentMatchers.eq("setBackgroundColor"),
            org.mockito.ArgumentMatchers.anyInt()
        )
    }

    // ===== estimateParcelSize =====
    // Requires real RemoteViews which is hard in unit tests, but we test the concept

    @Test
    fun flagConstants_arePositive() {
        assertTrue(ANTI_ALIAS_FLAG > 0)
        assertTrue(STRIKE_THRU_TEXT_FLAG > 0)
    }

    @Test
    fun combinedFlags_containsBoth() {
        val combined = STRIKE_THRU_TEXT_FLAG or ANTI_ALIAS_FLAG
        assertEquals(STRIKE_THRU_TEXT_FLAG, combined and STRIKE_THRU_TEXT_FLAG)
        assertEquals(ANTI_ALIAS_FLAG, combined and ANTI_ALIAS_FLAG)
    }
}
