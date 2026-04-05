package org.tasks.time

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities.withTZ

class LongExtensionsTest {

    @Test fun zeroNoonReturnsZero() = assertEquals(0L, 0L.noon())
    @Test fun zeroStartOfDayReturnsZero() = assertEquals(0L, 0L.startOfDay())
    @Test fun zeroStartOfMinuteReturnsZero() = assertEquals(0L, 0L.startOfMinute())
    @Test fun zeroStartOfSecondReturnsZero() = assertEquals(0L, 0L.startOfSecond())
    @Test fun zeroEndOfMinuteReturnsZero() = assertEquals(0L, 0L.endOfMinute())
    @Test fun zeroEndOfDayReturnsZero() = assertEquals(0L, 0L.endOfDay())
    @Test fun zeroWithMillisOfDayReturnsZero() = assertEquals(0L, 0L.withMillisOfDay(3600000))
    @Test fun zeroPlusDaysReturnsZero() = assertEquals(0L, 0L.plusDays(1))
    @Test fun zeroMinusDaysReturnsZero() = assertEquals(0L, 0L.minusDays(1))
    @Test fun zeroMinusMinutesReturnsZero() = assertEquals(0L, 0L.minusMinutes(1))
    @Test fun zeroMinusMillisReturnsZero() = assertEquals(0L, 0L.minusMillis(1000))
    @Test fun zeroMillisOfDayReturnsZero() = assertEquals(0, 0L.millisOfDay)
    @Test fun zeroHourOfDayReturnsZero() = assertEquals(0, 0L.hourOfDay)
    @Test fun zeroMinuteOfHourReturnsZero() = assertEquals(0, 0L.minuteOfHour)
    @Test fun zeroYearReturnsZero() = assertEquals(0, 0L.year)

    @Test fun noonSetsTo12() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 8, 30).millis
            val noon = ts.noon()
            assertEquals(12, noon.hourOfDay)
        }
    }

    @Test fun startOfDaySetsTo0() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 14, 30).millis
            val sod = ts.startOfDay()
            assertEquals(0, sod.hourOfDay)
            assertEquals(0, sod.minuteOfHour)
        }
    }

    @Test fun startOfMinuteClearsSeconds() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 14, 30, 45).millis
            val som = ts.startOfMinute()
            assertEquals(14, som.hourOfDay)
            assertEquals(30, som.minuteOfHour)
        }
    }

    @Test fun endOfDaySetsTo2359() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 8, 0).millis
            val eod = ts.endOfDay()
            assertEquals(23, eod.hourOfDay)
            assertEquals(59, eod.minuteOfHour)
        }
    }

    @Test fun plusDaysAddsCorrectly() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 10, 0).millis
            val result = ts.plusDays(3)
            assertEquals(2023, result.year)
            assertEquals(10, result.hourOfDay)
            // Should be June 18
            val diff = result - ts
            assertEquals(3 * 24 * 60 * 60 * 1000L, diff)
        }
    }

    @Test fun minusDaysSubtractsCorrectly() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 10, 0).millis
            val result = ts.minusDays(5)
            val diff = ts - result
            assertEquals(5 * 24 * 60 * 60 * 1000L, diff)
        }
    }

    @Test fun minusMinutesSubtractsCorrectly() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 10, 30).millis
            val result = ts.minusMinutes(30)
            assertEquals(10, result.hourOfDay)
            assertEquals(0, result.minuteOfHour)
        }
    }

    @Test fun minusMillisSubtractsCorrectly() {
        val ts = 1000000L
        assertEquals(999000L, ts.minusMillis(1000))
    }

    @Test fun withMillisOfDaySetsTime() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 0, 0).millis
            // Set to 14:30:00 = 14*3600000 + 30*60000
            val result = ts.withMillisOfDay(14 * 3600000 + 30 * 60000)
            assertEquals(14, result.hourOfDay)
            assertEquals(30, result.minuteOfHour)
        }
    }

    @Test fun yearExtractsCorrectly() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 10, 0).millis
            assertEquals(2023, ts.year)
        }
    }

    @Test fun hourOfDayExtractsCorrectly() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 17, 45).millis
            assertEquals(17, ts.hourOfDay)
        }
    }

    @Test fun minuteOfHourExtractsCorrectly() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 10, 42).millis
            assertEquals(42, ts.minuteOfHour)
        }
    }

    @Test fun millisOfDayExtractsCorrectly() {
        withTZ("UTC") {
            val ts = DateTime(2023, 6, 15, 1, 0).millis
            assertEquals(3600000, ts.millisOfDay)
        }
    }

    @Test fun noonPreservesDate() {
        withTZ("UTC") {
            val ts = DateTime(2023, 12, 25, 20, 0).millis
            val noon = ts.noon()
            assertEquals(2023, noon.year)
            assertEquals(12, noon.hourOfDay)
        }
    }

    @Test fun startOfDayPreservesDate() {
        withTZ("UTC") {
            val ts = DateTime(2023, 12, 25, 20, 0).millis
            val sod = ts.startOfDay()
            assertEquals(2023, sod.year)
        }
    }
}
