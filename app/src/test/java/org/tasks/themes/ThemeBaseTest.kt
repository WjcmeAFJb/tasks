package org.tasks.themes

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.billing.Inventory
import org.tasks.preferences.Preferences

class ThemeBaseTest {

    // --- index constants ---

    @Test
    fun defaultBaseThemeIs5() {
        assertEquals(5, ThemeBase.DEFAULT_BASE_THEME)
    }

    // --- constructor ---

    @Test
    fun constructorSetsIndex() {
        val theme = ThemeBase(0)
        assertEquals(0, theme.index)
    }

    @Test
    fun constructorSetsIndex1() {
        val theme = ThemeBase(1)
        assertEquals(1, theme.index)
    }

    @Test
    fun constructorSetsIndex3() {
        val theme = ThemeBase(3)
        assertEquals(3, theme.index)
    }

    @Test
    fun constructorSetsIndex5() {
        val theme = ThemeBase(5)
        assertEquals(5, theme.index)
    }

    // --- isFree ---

    @Test
    fun index0IsFree() {
        assertTrue(ThemeBase(0).isFree)
    }

    @Test
    fun index1IsFree() {
        assertTrue(ThemeBase(1).isFree)
    }

    @Test
    fun index2IsFree() {
        assertTrue(ThemeBase(2).isFree)
    }

    @Test
    fun index3IsNotFree() {
        assertFalse(ThemeBase(3).isFree)
    }

    @Test
    fun index4IsNotFree() {
        assertFalse(ThemeBase(4).isFree)
    }

    @Test
    fun index5IsFree() {
        assertTrue(ThemeBase(5).isFree)
    }

    // --- setDefaultNightMode ---

    @Test
    fun setDefaultNightModeDoesNotThrowForIndex0() {
        // Light theme -> MODE_NIGHT_NO
        ThemeBase(0).setDefaultNightMode()
    }

    @Test
    fun setDefaultNightModeDoesNotThrowForIndex1() {
        // Dark theme -> MODE_NIGHT_YES
        ThemeBase(1).setDefaultNightMode()
    }

    @Test
    fun setDefaultNightModeDoesNotThrowForIndex5() {
        // Follow system -> MODE_NIGHT_FOLLOW_SYSTEM
        ThemeBase(5).setDefaultNightMode()
    }

    // --- Parcelable ---

    @Test
    fun describeContentsIsZero() {
        assertEquals(0, ThemeBase(0).describeContents())
    }

    @Test
    fun creatorCreatesNewArray() {
        val array = ThemeBase.CREATOR.newArray(5)
        assertEquals(5, array.size)
    }

    // --- getThemeBase factory method ---

    @Test
    fun getThemeBaseUsesIntentOverride() {
        val preferences = mock(Preferences::class.java)
        val inventory = mock(Inventory::class.java)
        val intent = mock(Intent::class.java)

        `when`(intent.hasExtra(ThemeBase.EXTRA_THEME_OVERRIDE)).thenReturn(true)
        `when`(intent.getIntExtra(ThemeBase.EXTRA_THEME_OVERRIDE, ThemeBase.DEFAULT_BASE_THEME))
            .thenReturn(1)

        val theme = ThemeBase.getThemeBase(preferences, inventory, intent)
        assertEquals(1, theme.index)
    }

    @Test
    fun getThemeBaseUsesFreeThemeFromPreferences() {
        val preferences = mock(Preferences::class.java)
        val inventory = mock(Inventory::class.java)

        `when`(preferences.themeBase).thenReturn(0) // free theme
        `when`(inventory.purchasedThemes()).thenReturn(false)

        val theme = ThemeBase.getThemeBase(preferences, inventory, null)
        assertEquals(0, theme.index)
    }

    @Test
    fun getThemeBaseFallsBackToDefaultForNonFreeWithoutPurchase() {
        val preferences = mock(Preferences::class.java)
        val inventory = mock(Inventory::class.java)

        `when`(preferences.themeBase).thenReturn(3) // not free
        `when`(inventory.purchasedThemes()).thenReturn(false)

        val theme = ThemeBase.getThemeBase(preferences, inventory, null)
        assertEquals(ThemeBase.DEFAULT_BASE_THEME, theme.index)
    }

    @Test
    fun getThemeBaseAllowsNonFreeWithPurchase() {
        val preferences = mock(Preferences::class.java)
        val inventory = mock(Inventory::class.java)

        `when`(preferences.themeBase).thenReturn(3) // not free
        `when`(inventory.purchasedThemes()).thenReturn(true)

        val theme = ThemeBase.getThemeBase(preferences, inventory, null)
        assertEquals(3, theme.index)
    }

    @Test
    fun getThemeBaseAllowsNonFreeWithPurchaseIndex4() {
        val preferences = mock(Preferences::class.java)
        val inventory = mock(Inventory::class.java)

        `when`(preferences.themeBase).thenReturn(4) // not free
        `when`(inventory.purchasedThemes()).thenReturn(true)

        val theme = ThemeBase.getThemeBase(preferences, inventory, null)
        assertEquals(4, theme.index)
    }

    @Test
    fun getThemeBaseNullIntentUsesPreferences() {
        val preferences = mock(Preferences::class.java)
        val inventory = mock(Inventory::class.java)

        `when`(preferences.themeBase).thenReturn(5) // default, free
        `when`(inventory.purchasedThemes()).thenReturn(false)

        val theme = ThemeBase.getThemeBase(preferences, inventory, null)
        assertEquals(5, theme.index)
    }

    @Test
    fun getThemeBaseIntentWithoutExtraUsesPreferences() {
        val preferences = mock(Preferences::class.java)
        val inventory = mock(Inventory::class.java)
        val intent = mock(Intent::class.java)

        `when`(intent.hasExtra(ThemeBase.EXTRA_THEME_OVERRIDE)).thenReturn(false)
        `when`(preferences.themeBase).thenReturn(2) // free
        `when`(inventory.purchasedThemes()).thenReturn(false)

        val theme = ThemeBase.getThemeBase(preferences, inventory, intent)
        assertEquals(2, theme.index)
    }

    // --- boundary tests ---

    @Test
    fun allFreeThemesIdentified() {
        // indices 0, 1, 2, 5 are free
        val freeIndices = (0..5).filter { ThemeBase(it).isFree }
        assertEquals(listOf(0, 1, 2, 5), freeIndices)
    }

    @Test
    fun allPaidThemesIdentified() {
        // indices 3, 4 are paid
        val paidIndices = (0..5).filter { !ThemeBase(it).isFree }
        assertEquals(listOf(3, 4), paidIndices)
    }
}
