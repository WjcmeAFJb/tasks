package org.tasks.themes

import android.content.Context
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class ThemeColorTest {

    @Mock lateinit var context: Context
    @Mock lateinit var resources: Resources

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    // --- construction ---

    @Test
    fun constructionWithNonZeroColorSetsAlphaFull() {
        // 0x00FF0000 (red without alpha) should become 0xFFFF0000 after OR with 0xFF000000
        val color = 0x00FF0000
        val themeColor = ThemeColor(context, color)
        assertEquals(0xFFFF0000.toInt(), themeColor.primaryColor)
    }

    @Test
    fun constructionWithFullAlphaColorPreservesColor() {
        val color = 0xFF2196F3.toInt() // blue_500 with full alpha
        val themeColor = ThemeColor(context, color)
        assertEquals(color, themeColor.primaryColor)
    }

    @Test
    fun constructionWithZeroColorUsesBlueFallback() {
        val themeColor = ThemeColor(context, 0)
        // When color is 0, it falls back to BLUE constant
        assertEquals(BLUE, themeColor.primaryColor)
    }

    @Test
    fun constructionWithTwoArgsSetsOriginal() {
        val color = 0xFF2196F3.toInt()
        val themeColor = ThemeColor(context, color)
        assertEquals(color, themeColor.originalColor)
    }

    @Test
    fun constructionWithThreeArgsSetsOriginalSeparately() {
        val original = 0xFF112233.toInt()
        val display = 0xFF445566.toInt()
        val themeColor = ThemeColor(context, original, display)
        assertEquals(original, themeColor.originalColor)
        assertEquals(display, themeColor.primaryColor)
    }

    @Test
    fun constructionWithThreeArgsSetsOriginalAndColorSeparately() {
        val original = 0xFF0000FF.toInt()
        val display = 0xFFFF0000.toInt()
        val themeColor = ThemeColor(context, original, display)
        assertEquals(original, themeColor.originalColor)
        assertNotEquals(original, themeColor.primaryColor)
    }

    // --- isDark ---

    @Test
    fun lightColorIsDark() {
        // A very light color (white) should have dark content -> isDark = true
        // isDark means the content color is not white (i.e., background is light, so use dark text)
        val white = 0xFFFFFFFF.toInt()
        val themeColor = ThemeColor(context, white)
        // White background -> dark content -> isDark = true
        assertTrue(themeColor.isDark)
    }

    @Test
    fun darkColorIsDarkConsistentWithContentColor() {
        // isDark is true when colorOnPrimary != -1 (white)
        // The HCT tonal system may return a tonal value rather than pure white
        val darkColor = 0xFF111111.toInt()
        val themeColor = ThemeColor(context, darkColor)
        assertEquals(themeColor.colorOnPrimary != -1, themeColor.isDark)
    }

    @Test
    fun blueColorIsDarkness() {
        // Blue 500 (0xFF2196F3) - medium brightness
        val blue = 0xFF2196F3.toInt()
        val themeColor = ThemeColor(context, blue)
        // The isDark property determines whether to use light navigation bar
        // Blue 500 is medium, contentColor will determine isDark
        // Just verify the property is consistent
        assertEquals(themeColor.colorOnPrimary != -1, themeColor.isDark)
    }

    // --- colorOnPrimary ---

    @Test
    fun colorOnPrimaryForLightBackground() {
        val lightColor = 0xFFFFFF00.toInt() // yellow - light
        val themeColor = ThemeColor(context, lightColor)
        // Light background should have dark content
        assertNotEquals(-1, themeColor.colorOnPrimary) // not white
    }

    @Test
    fun colorOnPrimaryForDarkBackground() {
        val darkColor = 0xFF000044.toInt() // very dark blue
        val themeColor = ThemeColor(context, darkColor)
        // Dark background should have light content (high tone value)
        // The tonal system provides a light content color for contrast
        val onPrimary = themeColor.colorOnPrimary
        // The content color should be a lighter shade for readability on dark background
        // Verify it's not the same as the background
        assertNotEquals(darkColor, onPrimary)
    }

    // --- primaryColor ---

    @Test
    fun primaryColorMatchesConstructedColor() {
        val color = 0xFFABCDEF.toInt()
        val themeColor = ThemeColor(context, color)
        assertEquals(color, themeColor.primaryColor)
    }

    // --- pickerColor ---

    @Test
    fun pickerColorIsPrimaryColor() {
        val color = 0xFFABCDEF.toInt()
        val themeColor = ThemeColor(context, color)
        assertEquals(themeColor.primaryColor, themeColor.pickerColor)
    }

    // --- isFree ---

    @Test
    fun blueGreyIsFree() {
        // blue_grey_500 = -10453621
        val themeColor = ThemeColor(context, -10453621)
        assertTrue(themeColor.isFree)
    }

    @Test
    fun blue500IsFree() {
        // blue_500 = -14575885
        val themeColor = ThemeColor(context, -14575885)
        assertTrue(themeColor.isFree)
    }

    @Test
    fun grey900IsFree() {
        // grey_900 = -14606047
        val themeColor = ThemeColor(context, -14606047)
        assertTrue(themeColor.isFree)
    }

    @Test
    fun redIsNotFree() {
        // red_500 = -769226
        val themeColor = ThemeColor(context, -769226)
        assertFalse(themeColor.isFree)
    }

    @Test
    fun greenIsNotFree() {
        val themeColor = ThemeColor(context, 0xFF4CAF50.toInt())
        assertFalse(themeColor.isFree)
    }

    @Test
    fun arbitraryColorIsNotFree() {
        val themeColor = ThemeColor(context, 0xFF123456.toInt())
        assertFalse(themeColor.isFree)
    }

    @Test
    fun zeroOriginalIsNotFree() {
        // When original is 0, falls back to BLUE for display but original stays 0
        val themeColor = ThemeColor(context, 0)
        assertFalse(themeColor.isFree) // original is 0, which is not in the free set
    }

    // --- equals / hashCode ---

    @Test
    fun equalsForSameOriginalColor() {
        val color1 = ThemeColor(context, 0xFF2196F3.toInt())
        val color2 = ThemeColor(context, 0xFF2196F3.toInt())
        assertEquals(color1, color2)
    }

    @Test
    fun equalsForDifferentOriginalColor() {
        val color1 = ThemeColor(context, 0xFF2196F3.toInt())
        val color2 = ThemeColor(context, 0xFFFF0000.toInt())
        assertNotEquals(color1, color2)
    }

    @Test
    fun equalsWithThreeArgConstructorUsesOriginal() {
        val color1 = ThemeColor(context, 0xFF2196F3.toInt(), 0xFF000000.toInt())
        val color2 = ThemeColor(context, 0xFF2196F3.toInt(), 0xFFFFFFFF.toInt())
        assertEquals(color1, color2) // same original, different display
    }

    @Test
    fun notEqualToNull() {
        val color = ThemeColor(context, 0xFF2196F3.toInt())
        assertFalse(color.equals(null))
    }

    @Test
    fun notEqualToOtherType() {
        val color = ThemeColor(context, 0xFF2196F3.toInt())
        assertFalse(color.equals("not a ThemeColor"))
    }

    @Test
    fun equalToItself() {
        val color = ThemeColor(context, 0xFF2196F3.toInt())
        assertTrue(color.equals(color))
    }

    @Test
    fun hashCodeMatchesOriginalColor() {
        val original = 0xFF2196F3.toInt()
        val themeColor = ThemeColor(context, original)
        assertEquals(original, themeColor.hashCode())
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val color1 = ThemeColor(context, 0xFF2196F3.toInt())
        val color2 = ThemeColor(context, 0xFF2196F3.toInt())
        assertEquals(color1.hashCode(), color2.hashCode())
    }

    // --- Parcelable ---

    @Test
    fun describeContentsIsZero() {
        val themeColor = ThemeColor(context, 0xFF2196F3.toInt())
        assertEquals(0, themeColor.describeContents())
    }

    @Test
    fun creatorCreatesNewArray() {
        val array = ThemeColor.CREATOR.newArray(3)
        assertEquals(3, array.size)
    }

    // --- static arrays consistency ---

    @Test
    fun iconsArrayHas20Elements() {
        assertEquals(20, ThemeColor.ICONS.size)
    }

    @Test
    fun launchersArrayHas20Elements() {
        assertEquals(20, ThemeColor.LAUNCHERS.size)
    }

    @Test
    fun launcherColorsArrayHas20Elements() {
        assertEquals(20, ThemeColor.LAUNCHER_COLORS.size)
    }

    @Test
    fun allArraysHaveSameSize() {
        assertEquals(ThemeColor.ICONS.size, ThemeColor.LAUNCHERS.size)
        assertEquals(ThemeColor.ICONS.size, ThemeColor.LAUNCHER_COLORS.size)
    }

    @Test
    fun blueIsDefaultLauncherEmptyString() {
        // Index 7 is "" (default), which is Blue
        assertEquals("", ThemeColor.LAUNCHERS[7])
    }

    // --- alpha handling edge cases ---

    @Test
    fun partialAlphaGetsFullAlpha() {
        val partialAlpha = 0x80FF0000.toInt() // 50% alpha red
        val themeColor = ThemeColor(context, partialAlpha)
        // OR with 0xFF000000 makes alpha fully opaque
        assertEquals(0xFFFF0000.toInt(), themeColor.primaryColor)
    }

    @Test
    fun negativeOneColorGetsFullAlpha() {
        // -1 is 0xFFFFFFFF (white with full alpha)
        val themeColor = ThemeColor(context, -1)
        assertEquals(-1, themeColor.primaryColor) // 0xFFFFFFFF | 0xFF000000 = 0xFFFFFFFF = -1
    }
}
