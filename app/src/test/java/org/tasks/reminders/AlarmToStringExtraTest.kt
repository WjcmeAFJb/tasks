package org.tasks.reminders

import android.content.Context
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.R
import org.tasks.data.entity.Alarm
import org.tasks.extensions.Context.is24HourOverride
import org.tasks.kmp.org.tasks.time.getFullDateTime
import org.tasks.reminders.AlarmToString.Companion.getDurationString
import org.tasks.reminders.AlarmToString.Companion.getRepeatString
import java.util.concurrent.TimeUnit

/**
 * Additional tests for [AlarmToString] covering uncovered branches
 * beyond those in [AlarmToStringTest].
 */
class AlarmToStringExtraTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var alarmToString: AlarmToString

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        resources = mock(Resources::class.java)
        `when`(context.resources).thenReturn(resources)
        is24HourOverride = true
        alarmToString = AlarmToString(context)
    }

    // ===== TYPE_REL_START edge cases =====

    @Test
    fun relStartLargeNegativeTimeMultipleUnits() {
        // 2 days, 3 hours, 30 minutes before start
        val duration = -(TimeUnit.DAYS.toMillis(2) + TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(30))
        stubDurationString(duration, "2 days 3 hours 30 minutes")
        `when`(resources.getString(R.string.alarm_before_start, "2 days 3 hours 30 minutes"))
            .thenReturn("2 days 3 hours 30 minutes before start")
        val alarm = Alarm(type = Alarm.TYPE_REL_START, time = duration)
        assertEquals("2 days 3 hours 30 minutes before start", alarmToString.toString(alarm))
    }

    @Test
    fun relStartOneMinuteBefore() {
        val oneMinute = -TimeUnit.MINUTES.toMillis(1)
        stubDurationString(oneMinute, "1 minute")
        `when`(resources.getString(R.string.alarm_before_start, "1 minute"))
            .thenReturn("1 minute before start")
        val alarm = Alarm(type = Alarm.TYPE_REL_START, time = oneMinute)
        assertEquals("1 minute before start", alarmToString.toString(alarm))
    }

    @Test
    fun relStartOneMinuteAfter() {
        val oneMinute = TimeUnit.MINUTES.toMillis(1)
        stubDurationString(oneMinute, "1 minute")
        `when`(resources.getString(R.string.alarm_after_start, "1 minute"))
            .thenReturn("1 minute after start")
        val alarm = Alarm(type = Alarm.TYPE_REL_START, time = oneMinute)
        assertEquals("1 minute after start", alarmToString.toString(alarm))
    }

    // ===== TYPE_REL_END edge cases =====

    @Test
    fun relEndOneWeekBefore() {
        val oneWeek = -TimeUnit.DAYS.toMillis(7)
        stubDurationString(oneWeek, "1 week")
        `when`(resources.getString(R.string.alarm_before_due, "1 week"))
            .thenReturn("1 week before due")
        val alarm = Alarm(type = Alarm.TYPE_REL_END, time = oneWeek)
        assertEquals("1 week before due", alarmToString.toString(alarm))
    }

    @Test
    fun relEndOneWeekAfter() {
        val oneWeek = TimeUnit.DAYS.toMillis(7)
        stubDurationString(oneWeek, "1 week")
        `when`(resources.getString(R.string.alarm_after_due, "1 week"))
            .thenReturn("1 week after due")
        val alarm = Alarm(type = Alarm.TYPE_REL_END, time = oneWeek)
        assertEquals("1 week after due", alarmToString.toString(alarm))
    }

    @Test
    fun relEndOneDayBefore() {
        val oneDay = -TimeUnit.DAYS.toMillis(1)
        stubDurationString(oneDay, "1 day")
        `when`(resources.getString(R.string.alarm_before_due, "1 day"))
            .thenReturn("1 day before due")
        val alarm = Alarm(type = Alarm.TYPE_REL_END, time = oneDay)
        assertEquals("1 day before due", alarmToString.toString(alarm))
    }

    // ===== TYPE_RANDOM edge cases =====

    @Test
    fun randomAlarmWithDays() {
        val twoDays = TimeUnit.DAYS.toMillis(2)
        stubDurationString(twoDays, "2 days")
        `when`(resources.getString(R.string.randomly_every, "2 days"))
            .thenReturn("Randomly every 2 days")
        val alarm = Alarm(type = Alarm.TYPE_RANDOM, time = twoDays)
        assertEquals("Randomly every 2 days", alarmToString.toString(alarm))
    }

    @Test
    fun randomAlarmWithMinutes() {
        val thirtyMinutes = TimeUnit.MINUTES.toMillis(30)
        stubDurationString(thirtyMinutes, "30 minutes")
        `when`(resources.getString(R.string.randomly_every, "30 minutes"))
            .thenReturn("Randomly every 30 minutes")
        val alarm = Alarm(type = Alarm.TYPE_RANDOM, time = thirtyMinutes)
        assertEquals("Randomly every 30 minutes", alarmToString.toString(alarm))
    }

    // ===== TYPE_SNOOZE with different format =====

    @Test
    fun snoozeAlarmWith24HourFormat() {
        is24HourOverride = true
        alarmToString = AlarmToString(context)
        val time = 1700000000000L
        val formatted = getFullDateTime(time, true)
        `when`(resources.getString(R.string.snoozed_until, formatted))
            .thenReturn("Snoozed until $formatted")
        val alarm = Alarm(type = Alarm.TYPE_SNOOZE, time = time)
        assertEquals("Snoozed until $formatted", alarmToString.toString(alarm))
    }

    // ===== Default type (TYPE_DATE_TIME) =====

    @Test
    fun dateTimeAlarmAtEpoch() {
        val alarm = Alarm(type = Alarm.TYPE_DATE_TIME, time = 0L)
        val expected = getFullDateTime(0L, true)
        assertEquals(expected, alarmToString.toString(alarm))
    }

    @Test
    fun dateTimeAlarmWithLargeTimestamp() {
        val time = 2000000000000L
        val alarm = Alarm(type = Alarm.TYPE_DATE_TIME, time = time)
        val expected = getFullDateTime(time, true)
        assertEquals(expected, alarmToString.toString(alarm))
    }

    // ===== Repeat with various repeat counts =====

    @Test
    fun repeatWithHighCount() {
        `when`(resources.getString(R.string.when_started)).thenReturn("When started")
        val interval = TimeUnit.HOURS.toMillis(4)
        stubDurationString(interval, "4 hours")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 100))
            .thenReturn("times")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "4 hours", 100, "times"))
            .thenReturn("Repeats every 4 hours, occurs 100 times")
        val alarm = Alarm(type = Alarm.TYPE_REL_START, time = 0L, repeat = 100, interval = interval)
        assertEquals(
            "When started\nRepeats every 4 hours, occurs 100 times",
            alarmToString.toString(alarm)
        )
    }

    @Test
    fun repeatWithNegativeRepeatDoesNotAppendRepeat() {
        `when`(resources.getString(R.string.when_due)).thenReturn("When due")
        val alarm = Alarm(type = Alarm.TYPE_REL_END, time = 0L, repeat = -1, interval = 0L)
        assertEquals("When due", alarmToString.toString(alarm))
    }

    // ===== getDurationString edge cases =====

    @Test
    fun durationStringForZero() {
        val result = resources.getDurationString(0L)
        assertEquals("", result)
    }

    @Test
    fun durationStringForExactlyTwoWeeks() {
        stubDurationForWeeks(2)
        val result = resources.getDurationString(TimeUnit.DAYS.toMillis(14))
        assertEquals("2 weeks", result)
    }

    @Test
    fun durationStringForThreeWeeksAndFourDays() {
        // 25 days = 3 weeks + 4 days
        stubDurationForWeeks(3)
        stubDurationForDays(4)
        val result = resources.getDurationString(TimeUnit.DAYS.toMillis(25))
        assertEquals("3 weeks 4 days", result)
    }

    @Test
    fun durationStringForSixDays() {
        // 6 days = 0 weeks + 6 days
        stubDurationForDays(6)
        val result = resources.getDurationString(TimeUnit.DAYS.toMillis(6))
        assertEquals("6 days", result)
    }

    @Test
    fun durationStringForOneDayAndOneHour() {
        stubDurationForDays(1)
        stubDurationForHours(1)
        val duration = TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(1)
        val result = resources.getDurationString(duration)
        assertEquals("1 day 1 hour", result)
    }

    @Test
    fun durationStringForOneHourAndOneMinute() {
        stubDurationForHours(1)
        stubDurationForMinutes(1)
        val duration = TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(1)
        val result = resources.getDurationString(duration)
        assertEquals("1 hour 1 minute", result)
    }

    // ===== getRepeatString =====

    @Test
    fun repeatStringFormatting() {
        stubDurationString(TimeUnit.HOURS.toMillis(1), "1 hour")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 5))
            .thenReturn("times")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "1 hour", 5, "times"))
            .thenReturn("Repeats every 1 hour, occurs 5 times")

        val result = resources.getRepeatString(5, TimeUnit.HOURS.toMillis(1))
        assertEquals("Repeats every 1 hour, occurs 5 times", result)
    }

    @Test
    fun repeatStringWithOneTime() {
        stubDurationString(TimeUnit.MINUTES.toMillis(15), "15 minutes")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 1))
            .thenReturn("time")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "15 minutes", 1, "time"))
            .thenReturn("Repeats every 15 minutes, occurs 1 time")

        val result = resources.getRepeatString(1, TimeUnit.MINUTES.toMillis(15))
        assertEquals("Repeats every 15 minutes, occurs 1 time", result)
    }

    // ===== Alarm types combined with repeat =====

    @Test
    fun snoozeAlarmWithRepeat() {
        val snoozeTime = 1680537600000L
        val formatted = getFullDateTime(snoozeTime, true)
        `when`(resources.getString(R.string.snoozed_until, formatted))
            .thenReturn("Snoozed until $formatted")
        val interval = TimeUnit.MINUTES.toMillis(10)
        stubDurationString(interval, "10 minutes")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 3))
            .thenReturn("times")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "10 minutes", 3, "times"))
            .thenReturn("Repeats every 10 minutes, occurs 3 times")
        val alarm = Alarm(type = Alarm.TYPE_SNOOZE, time = snoozeTime, repeat = 3, interval = interval)
        assertEquals(
            "Snoozed until $formatted\nRepeats every 10 minutes, occurs 3 times",
            alarmToString.toString(alarm)
        )
    }

    @Test
    fun dateTimeAlarmWithRepeat() {
        val dateTime = 1680537600000L
        val formatted = getFullDateTime(dateTime, true)
        val interval = TimeUnit.HOURS.toMillis(2)
        stubDurationString(interval, "2 hours")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 4))
            .thenReturn("times")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "2 hours", 4, "times"))
            .thenReturn("Repeats every 2 hours, occurs 4 times")
        val alarm = Alarm(type = Alarm.TYPE_DATE_TIME, time = dateTime, repeat = 4, interval = interval)
        assertEquals(
            "$formatted\nRepeats every 2 hours, occurs 4 times",
            alarmToString.toString(alarm)
        )
    }

    @Test
    fun geoEnterAlarmWithRepeat() {
        val dateTime = 1680537600000L
        val formatted = getFullDateTime(dateTime, true)
        val interval = TimeUnit.MINUTES.toMillis(5)
        stubDurationString(interval, "5 minutes")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 2))
            .thenReturn("times")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "5 minutes", 2, "times"))
            .thenReturn("Repeats every 5 minutes, occurs 2 times")
        val alarm = Alarm(type = Alarm.TYPE_GEO_ENTER, time = dateTime, repeat = 2, interval = interval)
        assertEquals(
            "$formatted\nRepeats every 5 minutes, occurs 2 times",
            alarmToString.toString(alarm)
        )
    }

    @Test
    fun geoExitAlarmWithRepeat() {
        val dateTime = 1680537600000L
        val formatted = getFullDateTime(dateTime, true)
        val interval = TimeUnit.MINUTES.toMillis(15)
        stubDurationString(interval, "15 minutes")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 1))
            .thenReturn("time")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "15 minutes", 1, "time"))
            .thenReturn("Repeats every 15 minutes, occurs 1 time")
        val alarm = Alarm(type = Alarm.TYPE_GEO_EXIT, time = dateTime, repeat = 1, interval = interval)
        assertEquals(
            "$formatted\nRepeats every 15 minutes, occurs 1 time",
            alarmToString.toString(alarm)
        )
    }

    // --- Helper methods ---

    private fun stubDurationString(duration: Long, result: String) {
        val seconds = kotlin.math.abs(duration)
        val days = TimeUnit.MILLISECONDS.toDays(seconds)
        val weeks = days / 7
        val hours = TimeUnit.MILLISECONDS.toHours(seconds) - days * 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(seconds) - TimeUnit.MILLISECONDS.toHours(seconds) * 60
        val leftoverDays = days - weeks * 7

        if (weeks > 0) {
            `when`(resources.getQuantityString(R.plurals.repeat_n_weeks, weeks.toInt(), weeks.toInt()))
                .thenReturn("$weeks week${if (weeks > 1) "s" else ""}")
        }
        if (leftoverDays > 0) {
            `when`(resources.getQuantityString(R.plurals.repeat_n_days, leftoverDays.toInt(), leftoverDays.toInt()))
                .thenReturn("$leftoverDays day${if (leftoverDays > 1) "s" else ""}")
        }
        if (hours > 0) {
            `when`(resources.getQuantityString(R.plurals.repeat_n_hours, hours.toInt(), hours.toInt()))
                .thenReturn("$hours hour${if (hours > 1) "s" else ""}")
        }
        if (minutes > 0) {
            `when`(resources.getQuantityString(R.plurals.repeat_n_minutes, minutes.toInt(), minutes.toInt()))
                .thenReturn("$minutes minute${if (minutes > 1) "s" else ""}")
        }
    }

    private fun stubDurationForMinutes(count: Int) {
        `when`(resources.getQuantityString(R.plurals.repeat_n_minutes, count, count))
            .thenReturn("$count minute${if (count > 1) "s" else ""}")
    }

    private fun stubDurationForHours(count: Int) {
        `when`(resources.getQuantityString(R.plurals.repeat_n_hours, count, count))
            .thenReturn("$count hour${if (count > 1) "s" else ""}")
    }

    private fun stubDurationForDays(count: Int) {
        `when`(resources.getQuantityString(R.plurals.repeat_n_days, count, count))
            .thenReturn("$count day${if (count > 1) "s" else ""}")
    }

    private fun stubDurationForWeeks(count: Int) {
        `when`(resources.getQuantityString(R.plurals.repeat_n_weeks, count, count))
            .thenReturn("$count week${if (count > 1) "s" else ""}")
    }

    /**
     * Import the getDurationString companion extension so we can call it directly in tests.
     */
    private fun Resources.getDurationString(duration: Long): String =
        AlarmToString.Companion.run { getDurationString(duration) }

    private fun Resources.getRepeatString(repeat: Int, interval: Long): String =
        AlarmToString.Companion.run { getRepeatString(repeat, interval) }
}
