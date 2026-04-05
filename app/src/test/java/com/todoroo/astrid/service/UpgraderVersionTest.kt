package com.todoroo.astrid.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpgraderVersionTest {

    // ===== getLegacyColor mapping =====

    @Test
    fun legacyColorIndex0() {
        assertNotEquals(0, Upgrader.getLegacyColor(0, 0))
    }

    @Test
    fun legacyColorIndex1() {
        assertNotEquals(0, Upgrader.getLegacyColor(1, 0))
    }

    @Test
    fun legacyColorIndex20() {
        assertNotEquals(0, Upgrader.getLegacyColor(20, 0))
    }

    @Test
    fun legacyColorOutOfRangeReturnsDefault() {
        assertEquals(0, Upgrader.getLegacyColor(21, 0))
    }

    @Test
    fun legacyColorOutOfRangeReturnsCustomDefault() {
        assertEquals(42, Upgrader.getLegacyColor(21, 42))
    }

    @Test
    fun legacyColorNegativeIndexReturnsDefault() {
        assertEquals(0, Upgrader.getLegacyColor(-1, 0))
    }

    @Test
    fun legacyColorNegativeIndexReturnsCustomDefault() {
        assertEquals(99, Upgrader.getLegacyColor(-1, 99))
    }

    @Test
    fun legacyColorLargeIndexReturnsDefault() {
        assertEquals(0, Upgrader.getLegacyColor(1000, 0))
    }

    @Test
    fun allValidIndicesReturnNonZero() {
        for (i in 0..20) {
            assertNotEquals(
                "Index $i should return a non-zero color resource",
                0, Upgrader.getLegacyColor(i, 0)
            )
        }
    }

    @Test
    fun allValidIndicesReturnDistinctColors() {
        val colors = (0..20).map { Upgrader.getLegacyColor(it, 0) }
        assertEquals(
            "All 21 color indices should map to distinct resource IDs",
            21, colors.toSet().size
        )
    }

    @Test
    fun legacyColorDefaultNotUsedForValidIndex() {
        val sentinel = -999
        for (i in 0..20) {
            assertNotEquals(
                "Index $i should not return default",
                sentinel, Upgrader.getLegacyColor(i, sentinel)
            )
        }
    }

    // ===== Version constant ordering =====

    @Test
    fun versionConstantsArePublic() {
        // These constants are used by backup importer and other components
        assertTrue(Upgrader.V6_4 > 0)
        assertTrue(Upgrader.V8_2 > 0)
        assertTrue(Upgrader.V9_6 > 0)
        assertTrue(Upgrader.V9_7 > 0)
        assertTrue(Upgrader.V9_7_3 > 0)
        assertTrue(Upgrader.V10_0_2 > 0)
        assertTrue(Upgrader.V11_13 > 0)
        assertTrue(Upgrader.V12_4 > 0)
        assertTrue(Upgrader.V12_6 > 0)
        assertTrue(Upgrader.V12_8 > 0)
        assertTrue(Upgrader.V14_5_4 > 0)
        assertTrue(Upgrader.V14_6_1 > 0)
        assertTrue(Upgrader.V14_8 > 0)
    }

    @Test
    fun versionsAreMonotonicallyIncreasing() {
        val versions = listOf(
            Upgrader.V6_4,
            Upgrader.V8_2,
            Upgrader.V9_6,
            Upgrader.V9_7,
            Upgrader.V9_7_3,
            Upgrader.V10_0_2,
            Upgrader.V11_13,
            Upgrader.V12_4,
            Upgrader.V12_6,
            Upgrader.V12_8,
            Upgrader.V14_5_4,
            Upgrader.V14_6_1,
            Upgrader.V14_8,
        )
        for (i in 0 until versions.size - 1) {
            assertTrue(
                "V${versions[i]} should be less than V${versions[i + 1]}",
                versions[i] < versions[i + 1]
            )
        }
    }

    @Test
    fun v6_4Value() {
        assertEquals(546, Upgrader.V6_4)
    }

    @Test
    fun v8_2Value() {
        assertEquals(675, Upgrader.V8_2)
    }

    @Test
    fun v9_6Value() {
        assertEquals(90600, Upgrader.V9_6)
    }

    @Test
    fun v9_7Value() {
        assertEquals(90700, Upgrader.V9_7)
    }

    @Test
    fun v9_7_3Value() {
        assertEquals(90704, Upgrader.V9_7_3)
    }

    @Test
    fun v10_0_2Value() {
        assertEquals(100012, Upgrader.V10_0_2)
    }

    @Test
    fun v11_13Value() {
        assertEquals(111300, Upgrader.V11_13)
    }

    @Test
    fun v12_4Value() {
        assertEquals(120400, Upgrader.V12_4)
    }

    @Test
    fun v12_6Value() {
        assertEquals(120601, Upgrader.V12_6)
    }

    @Test
    fun v12_8Value() {
        assertEquals(120800, Upgrader.V12_8)
    }

    @Test
    fun v14_5_4Value() {
        assertEquals(140516, Upgrader.V14_5_4)
    }

    @Test
    fun v14_6_1Value() {
        assertEquals(140602, Upgrader.V14_6_1)
    }

    @Test
    fun v14_8Value() {
        assertEquals(140800, Upgrader.V14_8)
    }

    // ===== Version scheme validation =====

    @Test
    fun v9_7_3IsGreaterThanV9_7() {
        assertTrue(Upgrader.V9_7_3 > Upgrader.V9_7)
    }

    @Test
    fun v14_6_1IsGreaterThanV14_5_4() {
        assertTrue(Upgrader.V14_6_1 > Upgrader.V14_5_4)
    }

    @Test
    fun v14_8IsGreaterThanV14_6_1() {
        assertTrue(Upgrader.V14_8 > Upgrader.V14_6_1)
    }

    @Test
    fun earlyVersionsUsedSmallNumbers() {
        assertTrue("V6_4 should be a small number (old scheme)", Upgrader.V6_4 < 1000)
    }

    @Test
    fun laterVersionsUsedLargeNumbers() {
        assertTrue("V9_6 should use the new version scheme", Upgrader.V9_6 > 10000)
    }

    @Test
    fun versionSchemeTransition() {
        // V8_2 was the last major version using old scheme
        // V9_3 (90300, private) was the first using new scheme
        // V9_6 is the first public constant in the new scheme
        assertTrue(Upgrader.V8_2 < Upgrader.V9_6)
        assertTrue(Upgrader.V8_2 < 1000)
        assertTrue(Upgrader.V9_6 >= 90000)
    }
}
