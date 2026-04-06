package org.tasks.location

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.location.LocationServiceAndroid.Companion.COMPARATOR
import java.util.concurrent.TimeUnit

class LocationServiceAndroidTest {

    private fun location(time: Long, accuracy: Float): Location {
        val loc = mock<Location>()
        whenever(loc.time).thenReturn(time)
        whenever(loc.accuracy).thenReturn(accuracy)
        return loc
    }

    // ===== COMPARATOR: time delta > 2 minutes =====

    @Test
    fun newerByMoreThanTwoMinutesWins() {
        val now = System.currentTimeMillis()
        val old = now - TimeUnit.MINUTES.toMillis(3)
        val l1 = location(now, 50f)
        val l2 = location(old, 10f)
        assertEquals(-1, COMPARATOR.compare(l1, l2))
    }

    @Test
    fun olderByMoreThanTwoMinutesLoses() {
        val now = System.currentTimeMillis()
        val old = now - TimeUnit.MINUTES.toMillis(3)
        val l1 = location(old, 10f)
        val l2 = location(now, 50f)
        assertEquals(1, COMPARATOR.compare(l1, l2))
    }

    // ===== COMPARATOR: within 2 minutes, accuracy matters =====

    @Test
    fun moreAccurateWithinTwoMinutesWins() {
        val now = System.currentTimeMillis()
        val l1 = location(now, 5f)
        val l2 = location(now - TimeUnit.SECONDS.toMillis(30), 20f)
        assertEquals(-1, COMPARATOR.compare(l1, l2))
    }

    @Test
    fun lessAccurateWithinTwoMinutesLoses() {
        val now = System.currentTimeMillis()
        val l1 = location(now, 20f)
        val l2 = location(now - TimeUnit.SECONDS.toMillis(30), 5f)
        assertEquals(1, COMPARATOR.compare(l1, l2))
    }

    // ===== COMPARATOR: same accuracy, use time =====

    @Test
    fun newerWithSameAccuracyWins() {
        val now = System.currentTimeMillis()
        val l1 = location(now, 10f)
        val l2 = location(now - TimeUnit.SECONDS.toMillis(30), 10f)
        assertEquals(-1, COMPARATOR.compare(l1, l2))
    }

    @Test
    fun olderWithSameAccuracyLoses() {
        val now = System.currentTimeMillis()
        val l1 = location(now - TimeUnit.SECONDS.toMillis(30), 10f)
        val l2 = location(now, 10f)
        assertEquals(1, COMPARATOR.compare(l1, l2))
    }

    // ===== COMPARATOR: identical locations =====

    @Test
    fun identicalLocationsCompareToZero() {
        val now = System.currentTimeMillis()
        val l1 = location(now, 10f)
        val l2 = location(now, 10f)
        assertEquals(0, COMPARATOR.compare(l1, l2))
    }

    // ===== COMPARATOR: boundary at exactly 2 minutes =====

    @Test
    fun exactlyTwoMinutesNewerNotSignificant() {
        val now = System.currentTimeMillis()
        val twoMinAgo = now - TimeUnit.MINUTES.toMillis(2)
        val l1 = location(now, 10f)
        val l2 = location(twoMinAgo, 10f)
        // timeDelta == TWO_MINUTES, not > TWO_MINUTES
        // same accuracy -> falls through to time delta (positive -> -1)
        assertEquals(-1, COMPARATOR.compare(l1, l2))
    }

    @Test
    fun exactlyTwoMinutesOlderNotSignificant() {
        val now = System.currentTimeMillis()
        val twoMinAgo = now - TimeUnit.MINUTES.toMillis(2)
        val l1 = location(twoMinAgo, 10f)
        val l2 = location(now, 10f)
        assertEquals(1, COMPARATOR.compare(l1, l2))
    }

    // ===== COMPARATOR: one millisecond past 2 minutes =====

    @Test
    fun oneMsPastTwoMinutesNewerIsSignificant() {
        val now = System.currentTimeMillis()
        val past = now - TimeUnit.MINUTES.toMillis(2) - 1
        val l1 = location(now, 100f) // worse accuracy but much newer
        val l2 = location(past, 1f)
        assertEquals(-1, COMPARATOR.compare(l1, l2))
    }

    @Test
    fun oneMsPastTwoMinutesOlderIsSignificant() {
        val now = System.currentTimeMillis()
        val past = now - TimeUnit.MINUTES.toMillis(2) - 1
        val l1 = location(past, 1f) // better accuracy but much older
        val l2 = location(now, 100f)
        assertEquals(1, COMPARATOR.compare(l1, l2))
    }

    // ===== COMPARATOR: sorting a list =====

    @Test
    fun sortingPutsBestLocationFirst() {
        val now = System.currentTimeMillis()
        val locations = listOf(
            location(now - TimeUnit.MINUTES.toMillis(5), 5f),  // old
            location(now, 10f),                                  // newest
            location(now - TimeUnit.SECONDS.toMillis(30), 3f),  // recent + accurate
        )
        val sorted = locations.sortedWith(COMPARATOR)
        // most accurate recent (within 2 min) should be first
        assertEquals(3f, sorted[0].accuracy, 0f)
        assertEquals(10f, sorted[1].accuracy, 0f)
    }

    @Test
    fun sortingWithAllSameTime() {
        val now = System.currentTimeMillis()
        val locations = listOf(
            location(now, 30f),
            location(now, 10f),
            location(now, 20f),
        )
        val sorted = locations.sortedWith(COMPARATOR)
        assertEquals(10f, sorted[0].accuracy, 0f)
        assertEquals(20f, sorted[1].accuracy, 0f)
        assertEquals(30f, sorted[2].accuracy, 0f)
    }

    // ===== COMPARATOR: zero accuracy =====

    @Test
    fun zeroAccuracyTreatedNormally() {
        val now = System.currentTimeMillis()
        val l1 = location(now, 0f)
        val l2 = location(now, 10f)
        assertEquals(-1, COMPARATOR.compare(l1, l2))
    }

    // ===== COMPARATOR: very large accuracy =====

    @Test
    fun veryLargeAccuracyLoses() {
        val now = System.currentTimeMillis()
        val l1 = location(now, 100000f)
        val l2 = location(now, 10f)
        assertEquals(1, COMPARATOR.compare(l1, l2))
    }
}
