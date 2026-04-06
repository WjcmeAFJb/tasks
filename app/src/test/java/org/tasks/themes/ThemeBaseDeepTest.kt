package org.tasks.themes

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.billing.Inventory
import org.tasks.preferences.Preferences

class ThemeBaseDeepTest {

    // ===== Index and isFree =====

    @Test
    fun allIndicesReturnCorrectIndex() {
        for (i in 0..5) {
            assertEquals(i, ThemeBase(i).index)
        }
    }

    @Test
    fun freeThemesList() {
        val free = (0..5).filter { ThemeBase(it).isFree }
        assertEquals(listOf(0, 1, 2, 5), free)
    }

    @Test
    fun paidThemesList() {
        val paid = (0..5).filter { !ThemeBase(it).isFree }
        assertEquals(listOf(3, 4), paid)
    }

    // ===== setDefaultNightMode =====

    @Test
    fun setDefaultNightModeForAllIndices() {
        // Should not throw for any valid index
        for (i in 0..5) {
            ThemeBase(i).setDefaultNightMode()
        }
    }

    // ===== getThemeBase factory =====

    @Test
    fun intentOverrideTakesPriority() {
        val prefs = mock<Preferences>()
        val inv = mock<Inventory>()
        val intent = mock<Intent>()

        whenever(intent.hasExtra(ThemeBase.EXTRA_THEME_OVERRIDE)).thenReturn(true)
        whenever(intent.getIntExtra(ThemeBase.EXTRA_THEME_OVERRIDE, ThemeBase.DEFAULT_BASE_THEME))
            .thenReturn(2)

        val theme = ThemeBase.getThemeBase(prefs, inv, intent)
        assertEquals(2, theme.index)
    }

    @Test
    fun freeThemeUsedWithoutPurchase() {
        val prefs = mock<Preferences>()
        val inv = mock<Inventory>()

        whenever(prefs.themeBase).thenReturn(0)
        whenever(inv.purchasedThemes()).thenReturn(false)

        val theme = ThemeBase.getThemeBase(prefs, inv, null)
        assertEquals(0, theme.index)
    }

    @Test
    fun paidThemeFallsBackToDefaultWithoutPurchase() {
        val prefs = mock<Preferences>()
        val inv = mock<Inventory>()

        whenever(prefs.themeBase).thenReturn(3)
        whenever(inv.purchasedThemes()).thenReturn(false)

        val theme = ThemeBase.getThemeBase(prefs, inv, null)
        assertEquals(ThemeBase.DEFAULT_BASE_THEME, theme.index)
    }

    @Test
    fun paidThemeAllowedWithPurchase() {
        val prefs = mock<Preferences>()
        val inv = mock<Inventory>()

        whenever(prefs.themeBase).thenReturn(4)
        whenever(inv.purchasedThemes()).thenReturn(true)

        val theme = ThemeBase.getThemeBase(prefs, inv, null)
        assertEquals(4, theme.index)
    }

    @Test
    fun intentWithoutExtraFallsToPreferences() {
        val prefs = mock<Preferences>()
        val inv = mock<Inventory>()
        val intent = mock<Intent>()

        whenever(intent.hasExtra(ThemeBase.EXTRA_THEME_OVERRIDE)).thenReturn(false)
        whenever(prefs.themeBase).thenReturn(1)
        whenever(inv.purchasedThemes()).thenReturn(false)

        val theme = ThemeBase.getThemeBase(prefs, inv, intent)
        assertEquals(1, theme.index)
    }

    @Test
    fun nullIntentUsesPreferences() {
        val prefs = mock<Preferences>()
        val inv = mock<Inventory>()

        whenever(prefs.themeBase).thenReturn(5)
        whenever(inv.purchasedThemes()).thenReturn(false)

        val theme = ThemeBase.getThemeBase(prefs, inv, null)
        assertEquals(5, theme.index)
    }

    // ===== Parcelable =====

    @Test
    fun describeContentsIsZero() {
        assertEquals(0, ThemeBase(0).describeContents())
    }

    @Test
    fun creatorNewArrayCorrectSize() {
        assertEquals(3, ThemeBase.CREATOR.newArray(3).size)
    }

    @Test
    fun creatorNewArraySizeZero() {
        assertEquals(0, ThemeBase.CREATOR.newArray(0).size)
    }

    // ===== Constants =====

    @Test
    fun defaultBaseThemeIs5() {
        assertEquals(5, ThemeBase.DEFAULT_BASE_THEME)
    }

    @Test
    fun extraThemeOverrideConstant() {
        assertEquals("extra_theme_override", ThemeBase.EXTRA_THEME_OVERRIDE)
    }

    // ===== Edge cases for isFree boundary =====

    @Test
    fun index2IsFree() {
        assertTrue(ThemeBase(2).isFree)
    }

    @Test
    fun index3IsNotFree() {
        assertFalse(ThemeBase(3).isFree)
    }
}
