package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.time.ONE_DAY

class AlarmTest {
    @Test
    fun defaultAlarmValues() {
        val alarm = Alarm()
        assertEquals(0L, alarm.id)
        assertEquals(0L, alarm.task)
        assertEquals(0L, alarm.time)
        assertEquals(0, alarm.type)
        assertEquals(0, alarm.repeat)
        assertEquals(0L, alarm.interval)
    }

    @Test
    fun sameReturnsTrueForIdenticalAlarms() {
        val a = Alarm(type = Alarm.TYPE_REL_START, time = 1000, repeat = 3, interval = 60000)
        val b = Alarm(type = Alarm.TYPE_REL_START, time = 1000, repeat = 3, interval = 60000)
        assertTrue(a.same(b))
    }

    @Test
    fun sameIgnoresIdAndTask() {
        val a = Alarm(id = 1, task = 10, type = Alarm.TYPE_DATE_TIME, time = 5000)
        val b = Alarm(id = 2, task = 20, type = Alarm.TYPE_DATE_TIME, time = 5000)
        assertTrue(a.same(b))
    }

    @Test
    fun sameReturnsFalseForDifferentType() {
        val a = Alarm(type = Alarm.TYPE_REL_START, time = 1000)
        val b = Alarm(type = Alarm.TYPE_REL_END, time = 1000)
        assertFalse(a.same(b))
    }

    @Test
    fun sameReturnsFalseForDifferentTime() {
        val a = Alarm(type = Alarm.TYPE_DATE_TIME, time = 1000)
        val b = Alarm(type = Alarm.TYPE_DATE_TIME, time = 2000)
        assertFalse(a.same(b))
    }

    @Test
    fun sameReturnsFalseForDifferentRepeat() {
        val a = Alarm(type = Alarm.TYPE_REL_END, repeat = 3, interval = 60000)
        val b = Alarm(type = Alarm.TYPE_REL_END, repeat = 5, interval = 60000)
        assertFalse(a.same(b))
    }

    @Test
    fun sameReturnsFalseForDifferentInterval() {
        val a = Alarm(type = Alarm.TYPE_REL_END, repeat = 3, interval = 60000)
        val b = Alarm(type = Alarm.TYPE_REL_END, repeat = 3, interval = 120000)
        assertFalse(a.same(b))
    }

    @Test
    fun whenStartedCreatesRelStartAlarm() {
        val alarm = Alarm.whenStarted(42)
        assertEquals(42L, alarm.task)
        assertEquals(Alarm.TYPE_REL_START, alarm.type)
        assertEquals(0L, alarm.time)
        assertEquals(0, alarm.repeat)
        assertEquals(0L, alarm.interval)
    }

    @Test
    fun whenDueCreatesRelEndAlarm() {
        val alarm = Alarm.whenDue(42)
        assertEquals(42L, alarm.task)
        assertEquals(Alarm.TYPE_REL_END, alarm.type)
        assertEquals(0L, alarm.time)
    }

    @Test
    fun whenOverdueCreatesRepeatingRelEndAlarm() {
        val alarm = Alarm.whenOverdue(42)
        assertEquals(42L, alarm.task)
        assertEquals(Alarm.TYPE_REL_END, alarm.type)
        assertEquals(ONE_DAY, alarm.time)
        assertEquals(6, alarm.repeat)
        assertEquals(ONE_DAY, alarm.interval)
    }

    @Test
    fun typeConstants() {
        assertEquals(0, Alarm.TYPE_DATE_TIME)
        assertEquals(1, Alarm.TYPE_REL_START)
        assertEquals(2, Alarm.TYPE_REL_END)
        assertEquals(3, Alarm.TYPE_RANDOM)
        assertEquals(4, Alarm.TYPE_SNOOZE)
        assertEquals(5, Alarm.TYPE_GEO_ENTER)
        assertEquals(6, Alarm.TYPE_GEO_EXIT)
    }

    @Test
    fun toStringIncludesAllFields() {
        val alarm = Alarm(id = 1, task = 2, time = 3000, type = Alarm.TYPE_REL_START, repeat = 1, interval = 60000)
        val str = alarm.toString()
        assertTrue(str.contains("id=1"))
        assertTrue(str.contains("task=2"))
        assertTrue(str.contains("type=1"))
        assertTrue(str.contains("repeat=1"))
        assertTrue(str.contains("interval=60000"))
    }

    @Test
    fun dataCopyPreservesValues() {
        val original = Alarm(id = 1, task = 2, time = 3000, type = Alarm.TYPE_SNOOZE)
        val copy = original.copy(id = 5)
        assertEquals(5L, copy.id)
        assertEquals(2L, copy.task)
        assertEquals(3000L, copy.time)
        assertEquals(Alarm.TYPE_SNOOZE, copy.type)
    }

    @Test
    fun dataClassEquality() {
        val a = Alarm(id = 1, task = 2, time = 3000, type = Alarm.TYPE_DATE_TIME)
        val b = Alarm(id = 1, task = 2, time = 3000, type = Alarm.TYPE_DATE_TIME)
        assertEquals(a, b)
    }

    @Test
    fun dataClassInequalityOnId() {
        val a = Alarm(id = 1, task = 2, time = 3000)
        val b = Alarm(id = 2, task = 2, time = 3000)
        assertNotEquals(a, b)
    }

    @Test
    fun tableNameIsAlarms() {
        assertEquals("alarms", Alarm.TABLE_NAME)
    }

    @Test
    fun sameWithAllFieldsMatching() {
        val a = Alarm(
            id = 100, task = 200,
            type = Alarm.TYPE_RANDOM, time = 3600000,
            repeat = 10, interval = 900000
        )
        val b = Alarm(
            id = 999, task = 888,
            type = Alarm.TYPE_RANDOM, time = 3600000,
            repeat = 10, interval = 900000
        )
        assertTrue(a.same(b))
    }

    @Test
    fun sameWithDefaultAlarms() {
        assertTrue(Alarm().same(Alarm()))
    }

    @Test
    fun sameWhenStartedNotSameAsWhenDue() {
        assertFalse(Alarm.whenStarted(1).same(Alarm.whenDue(1)))
    }

    @Test
    fun sameWhenDueNotSameAsWhenOverdue() {
        assertFalse(Alarm.whenDue(1).same(Alarm.whenOverdue(1)))
    }

    @Test
    fun whenOverdueNotSameAsWhenStarted() {
        assertFalse(Alarm.whenOverdue(1).same(Alarm.whenStarted(1)))
    }

    @Test
    fun sameWhenStartedAcrossTasks() {
        assertTrue(Alarm.whenStarted(1).same(Alarm.whenStarted(99)))
    }

    @Test
    fun sameWhenDueAcrossTasks() {
        assertTrue(Alarm.whenDue(1).same(Alarm.whenDue(99)))
    }

    @Test
    fun sameWhenOverdueAcrossTasks() {
        assertTrue(Alarm.whenOverdue(1).same(Alarm.whenOverdue(99)))
    }

    @Test
    fun whenStartedIdIsZero() {
        assertEquals(0L, Alarm.whenStarted(1).id)
    }

    @Test
    fun whenDueIdIsZero() {
        assertEquals(0L, Alarm.whenDue(1).id)
    }

    @Test
    fun whenOverdueIdIsZero() {
        assertEquals(0L, Alarm.whenOverdue(1).id)
    }

    @Test
    fun whenStartedIntervalIsZero() {
        assertEquals(0L, Alarm.whenStarted(1).interval)
    }

    @Test
    fun whenDueIntervalIsZero() {
        assertEquals(0L, Alarm.whenDue(1).interval)
    }

    @Test
    fun whenDueRepeatIsZero() {
        assertEquals(0, Alarm.whenDue(1).repeat)
    }

    @Test
    fun copyChangesType() {
        val original = Alarm(type = Alarm.TYPE_DATE_TIME)
        val copy = original.copy(type = Alarm.TYPE_SNOOZE)
        assertEquals(Alarm.TYPE_SNOOZE, copy.type)
    }

    @Test
    fun copyChangesRepeatAndInterval() {
        val original = Alarm(repeat = 0, interval = 0)
        val copy = original.copy(repeat = 5, interval = 60000)
        assertEquals(5, copy.repeat)
        assertEquals(60000L, copy.interval)
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = Alarm(id = 1, task = 2, time = 3000, type = Alarm.TYPE_REL_START)
        val b = Alarm(id = 1, task = 2, time = 3000, type = Alarm.TYPE_REL_START)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun geoEnterAlarmValues() {
        val alarm = Alarm(type = Alarm.TYPE_GEO_ENTER, task = 7, time = 12345L)
        assertEquals(Alarm.TYPE_GEO_ENTER, alarm.type)
        assertEquals(7L, alarm.task)
        assertEquals(12345L, alarm.time)
    }

    @Test
    fun geoExitAlarmValues() {
        val alarm = Alarm(type = Alarm.TYPE_GEO_EXIT, task = 8, time = 67890L)
        assertEquals(Alarm.TYPE_GEO_EXIT, alarm.type)
        assertEquals(8L, alarm.task)
        assertEquals(67890L, alarm.time)
    }

    @Test
    fun snoozeAlarmValues() {
        val snoozeTime = 1680537600000L
        val alarm = Alarm(type = Alarm.TYPE_SNOOZE, time = snoozeTime, task = 3)
        assertEquals(Alarm.TYPE_SNOOZE, alarm.type)
        assertEquals(snoozeTime, alarm.time)
        assertEquals(3L, alarm.task)
    }

    @Test
    fun randomAlarmValues() {
        val alarm = Alarm(type = Alarm.TYPE_RANDOM, time = 3600000L, task = 4)
        assertEquals(Alarm.TYPE_RANDOM, alarm.type)
        assertEquals(3600000L, alarm.time)
    }

    @Test
    fun oneDayConstantIs86400000() {
        assertEquals(86400000L, ONE_DAY)
    }
}
