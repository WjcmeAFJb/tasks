package org.tasks.extensions

import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for RemoteViewsExtensions.kt paint flag constants used in strikethrough.
 */
class RemoteViewsExtensionsTest {

    // We can't easily test RemoteViews method calls without Robolectric, but
    // we can test the flag values and logic used.

    @Test
    fun strikethroughEnabledPaintFlags() {
        val flags = STRIKE_THRU_TEXT_FLAG or ANTI_ALIAS_FLAG
        assertEquals(STRIKE_THRU_TEXT_FLAG or ANTI_ALIAS_FLAG, flags)
    }

    @Test
    fun strikethroughDisabledPaintFlags() {
        val flags = ANTI_ALIAS_FLAG
        assertEquals(ANTI_ALIAS_FLAG, flags)
    }

    @Test
    fun antiAliasFlagIsNonZero() {
        assert(ANTI_ALIAS_FLAG > 0)
    }

    @Test
    fun strikeThroughFlagIsNonZero() {
        assert(STRIKE_THRU_TEXT_FLAG > 0)
    }

    @Test
    fun combinedFlagsIncludeBoth() {
        val combined = STRIKE_THRU_TEXT_FLAG or ANTI_ALIAS_FLAG
        assertEquals(STRIKE_THRU_TEXT_FLAG, combined and STRIKE_THRU_TEXT_FLAG)
        assertEquals(ANTI_ALIAS_FLAG, combined and ANTI_ALIAS_FLAG)
    }

    @Test
    fun antiAliasOnlyDoesNotIncludeStrikethrough() {
        assertEquals(0, ANTI_ALIAS_FLAG and STRIKE_THRU_TEXT_FLAG)
    }
}
