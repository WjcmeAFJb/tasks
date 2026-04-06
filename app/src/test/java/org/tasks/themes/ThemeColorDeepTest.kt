package org.tasks.themes

import android.content.Context
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class ThemeColorDeepTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mock()
    }

    // ===== Alpha handling =====

    @Test
    fun zeroAlphaGetsFullAlpha() {
        val color = 0x00FF0000 // red without alpha
        val tc = ThemeColor(context, color)
        assertEquals(0xFFFF0000.toInt(), tc.primaryColor)
    }

    @Test
    fun halfAlphaGetsFullAlpha() {
        val color = 0x7FFF0000.toInt()
        val tc = ThemeColor(context, color)
        assertEquals(0xFFFF0000.toInt(), tc.primaryColor)
    }

    @Test
    fun fullAlphaPreserved() {
        val color = 0xFFABCDEF.toInt()
        val tc = ThemeColor(context, color)
        assertEquals(color, tc.primaryColor)
    }

    @Test
    fun zeroColorFallsBackToBlue() {
        val tc = ThemeColor(context, 0)
        assertEquals(BLUE, tc.primaryColor)
    }

    @Test
    fun whiteColor() {
        val tc = ThemeColor(context, 0xFFFFFFFF.toInt())
        assertEquals(0xFFFFFFFF.toInt(), tc.primaryColor)
    }

    @Test
    fun blackColor() {
        val tc = ThemeColor(context, 0xFF000000.toInt())
        assertEquals(0xFF000000.toInt(), tc.primaryColor)
    }

    // ===== Three-arg constructor =====

    @Test
    fun threeArgOriginalDiffersFromDisplay() {
        val original = 0xFF112233.toInt()
        val display = 0xFF445566.toInt()
        val tc = ThemeColor(context, original, display)
        assertEquals(original, tc.originalColor)
        assertEquals(display, tc.primaryColor)
    }

    @Test
    fun threeArgZeroDisplayFallsBackToBlue() {
        val tc = ThemeColor(context, 0xFF123456.toInt(), 0)
        assertEquals(BLUE, tc.primaryColor)
        assertEquals(0xFF123456.toInt(), tc.originalColor)
    }

    // ===== isDark =====

    @Test
    fun whiteBackgroundIsDark() {
        // White -> content should be dark, isDark = true
        val tc = ThemeColor(context, 0xFFFFFFFF.toInt())
        assertTrue(tc.isDark)
    }

    @Test
    fun isDarkConsistentWithContentColor() {
        for (color in listOf(
            0xFF000000.toInt(),
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFF0000FF.toInt(),
            0xFFFFFF00.toInt(),
        )) {
            val tc = ThemeColor(context, color)
            assertEquals(
                "isDark should be consistent with colorOnPrimary for ${Integer.toHexString(color)}",
                tc.colorOnPrimary != -1,
                tc.isDark
            )
        }
    }

    // ===== isFree =====

    @Test
    fun blue500IsFree() {
        assertTrue(ThemeColor(context, -14575885).isFree) // blue_500
    }

    @Test
    fun blueGrey500IsFree() {
        assertTrue(ThemeColor(context, -10453621).isFree) // blue_grey_500
    }

    @Test
    fun grey900IsFree() {
        assertTrue(ThemeColor(context, -14606047).isFree) // grey_900
    }

    @Test
    fun red500IsNotFree() {
        assertFalse(ThemeColor(context, -769226).isFree)
    }

    @Test
    fun arbitraryColorIsNotFree() {
        assertFalse(ThemeColor(context, 0xFF123456.toInt()).isFree)
    }

    @Test
    fun zeroOriginalIsNotFree() {
        assertFalse(ThemeColor(context, 0).isFree)
    }

    @Test
    fun negativeOneIsNotFree() {
        assertFalse(ThemeColor(context, -1).isFree) // 0xFFFFFFFF
    }

    // ===== pickerColor =====

    @Test
    fun pickerColorEqualsPrimaryColor() {
        val tc = ThemeColor(context, 0xFFABCDEF.toInt())
        assertEquals(tc.primaryColor, tc.pickerColor)
    }

    // ===== equals / hashCode =====

    @Test
    fun equalsBySameOriginal() {
        val a = ThemeColor(context, 0xFF112233.toInt())
        val b = ThemeColor(context, 0xFF112233.toInt())
        assertEquals(a, b)
    }

    @Test
    fun notEqualsByDifferentOriginal() {
        val a = ThemeColor(context, 0xFF112233.toInt())
        val b = ThemeColor(context, 0xFF445566.toInt())
        assertNotEquals(a, b)
    }

    @Test
    fun equalsIgnoresDisplayColor() {
        val a = ThemeColor(context, 0xFF112233.toInt(), 0xFF000000.toInt())
        val b = ThemeColor(context, 0xFF112233.toInt(), 0xFFFFFFFF.toInt())
        assertEquals(a, b)
    }

    @Test
    fun notEqualToNull() {
        assertFalse(ThemeColor(context, 1).equals(null))
    }

    @Test
    fun notEqualToString() {
        assertFalse(ThemeColor(context, 1).equals("string"))
    }

    @Test
    fun equalToSelf() {
        val tc = ThemeColor(context, 1)
        assertTrue(tc.equals(tc))
    }

    @Test
    fun hashCodeIsOriginal() {
        val original = 0xFFABCDEF.toInt()
        assertEquals(original, ThemeColor(context, original).hashCode())
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = ThemeColor(context, 42)
        val b = ThemeColor(context, 42)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ===== Parcelable =====

    @Test
    fun describeContentsIsZero() {
        assertEquals(0, ThemeColor(context, 1).describeContents())
    }

    @Test
    fun creatorNewArrayReturnsCorrectSize() {
        assertEquals(5, ThemeColor.CREATOR.newArray(5).size)
    }

    @Test
    fun creatorNewArraySizeZero() {
        assertEquals(0, ThemeColor.CREATOR.newArray(0).size)
    }

    // ===== Static arrays =====

    @Test
    fun allArraysSameSize() {
        assertEquals(ThemeColor.ICONS.size, ThemeColor.LAUNCHERS.size)
        assertEquals(ThemeColor.ICONS.size, ThemeColor.LAUNCHER_COLORS.size)
    }

    @Test
    fun arraysSizeIs20() {
        assertEquals(20, ThemeColor.ICONS.size)
        assertEquals(20, ThemeColor.LAUNCHERS.size)
        assertEquals(20, ThemeColor.LAUNCHER_COLORS.size)
    }

    @Test
    fun defaultLauncherIsBlueAtIndex7() {
        assertEquals("", ThemeColor.LAUNCHERS[7])
    }

    @Test
    fun allLauncherStringsStartWithDotOrEmpty() {
        for (launcher in ThemeColor.LAUNCHERS) {
            assertTrue(
                "Launcher '$launcher' should start with '.' or be empty",
                launcher.isEmpty() || launcher.startsWith(".")
            )
        }
    }

    @Test
    fun iconsArrayHasNoZeros() {
        for (icon in ThemeColor.ICONS) {
            assertNotEquals("Icon resource should not be 0", 0, icon)
        }
    }

    @Test
    fun launcherColorsArrayHasNoZeros() {
        for (color in ThemeColor.LAUNCHER_COLORS) {
            assertNotEquals("Launcher color resource should not be 0", 0, color)
        }
    }
}
