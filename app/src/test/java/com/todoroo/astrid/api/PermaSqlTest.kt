package com.todoroo.astrid.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Freeze
import org.tasks.TestUtilities.withTZ
import org.tasks.time.DateTime

class PermaSqlTest {

    @Test fun noPlaceholdersReturnsOriginal() =
        assertEquals("SELECT * FROM tasks", PermaSql.replacePlaceholdersForQuery("SELECT * FROM tasks"))

    @Test fun nowPlaceholderReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0)) {
                val result = PermaSql.replacePlaceholdersForQuery("time > ${PermaSql.VALUE_NOW}")
                assertFalse(result.contains("NOW()"))
                assertTrue(result.contains(DateTime(2023, 6, 15, 12, 0).millis.toString()))
            }
        }
    }

    @Test fun eodPlaceholderReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0)) {
                val result = PermaSql.replacePlaceholdersForQuery("due < ${PermaSql.VALUE_EOD}")
                assertFalse(result.contains("EOD()"))
                // Should contain a timestamp for end of 2023-06-15
                val ts = result.substringAfter("< ").toLong()
                assertTrue(ts > DateTime(2023, 6, 15, 23, 0).millis)
            }
        }
    }

    @Test fun eodTomorrowReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0)) {
                val result = PermaSql.replacePlaceholdersForQuery("x=${PermaSql.VALUE_EOD_TOMORROW}")
                assertFalse(result.contains("EODT()"))
                val ts = result.substringAfter("=").toLong()
                assertTrue(ts > DateTime(2023, 6, 16, 23, 0).millis)
            }
        }
    }

    @Test fun eodYesterdayReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0)) {
                val result = PermaSql.replacePlaceholdersForQuery("x=${PermaSql.VALUE_EOD_YESTERDAY}")
                assertFalse(result.contains("EODY()"))
                val ts = result.substringAfter("=").toLong()
                assertTrue(ts > DateTime(2023, 6, 14, 23, 0).millis)
                assertTrue(ts < DateTime(2023, 6, 15, 0, 0).millis)
            }
        }
    }

    @Test fun eodNextWeekReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0)) {
                val result = PermaSql.replacePlaceholdersForQuery("x=${PermaSql.VALUE_EOD_NEXT_WEEK}")
                assertFalse(result.contains("EODW()"))
                val ts = result.substringAfter("=").toLong()
                assertTrue(ts > DateTime(2023, 6, 22, 23, 0).millis)
            }
        }
    }

    @Test fun eodNextMonthReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0)) {
                val result = PermaSql.replacePlaceholdersForQuery("x=${PermaSql.VALUE_EOD_NEXT_MONTH}")
                assertFalse(result.contains("EODM()"))
                val ts = result.substringAfter("=").toLong()
                assertTrue(ts > DateTime(2023, 7, 14, 23, 0).millis)
            }
        }
    }

    @Test fun eodDayAfterReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0)) {
                val result = PermaSql.replacePlaceholdersForQuery("x=${PermaSql.VALUE_EOD_DAY_AFTER}")
                assertFalse(result.contains("EODTT()"))
            }
        }
    }

    @Test fun noonPlaceholderReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0)) {
                val result = PermaSql.replacePlaceholdersForQuery("x=${PermaSql.VALUE_NOON}")
                assertFalse(result.contains("NOON()"))
                val ts = result.substringAfter("=").toLong()
                assertEquals(DateTime(2023, 6, 15, 12, 0).millis, ts)
            }
        }
    }

    @Test fun multiplePlaceholders() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0)) {
                val result = PermaSql.replacePlaceholdersForQuery(
                    "due > ${PermaSql.VALUE_EOD} AND due < ${PermaSql.VALUE_EOD_TOMORROW}"
                )
                assertFalse(result.contains("EOD()"))
                assertFalse(result.contains("EODT()"))
            }
        }
    }

    // --- replacePlaceholdersForNewTask ---

    @Test fun newTaskNowReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0)) {
                val result = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_NOW}")
                assertFalse(result.contains("NOW()"))
            }
        }
    }

    @Test fun newTaskEodReplacedWithNoon() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0)) {
                val result = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_EOD}")
                assertFalse(result.contains("EOD()"))
                // For new tasks, EOD uses noon() instead of endOfDay()
                val ts = result.substringAfter("=").toLong()
                assertEquals(DateTime(2023, 6, 15, 12, 0).millis, ts)
            }
        }
    }

    @Test fun newTaskNoonReplaced() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0)) {
                val result = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_NOON}")
                assertFalse(result.contains("NOON()"))
            }
        }
    }

    @Test fun noReplacementNeeded() {
        assertEquals("plain text", PermaSql.replacePlaceholdersForQuery("plain text"))
        assertEquals("plain text", PermaSql.replacePlaceholdersForNewTask("plain text"))
    }
}
