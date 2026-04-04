package org.tasks.reminders

import android.content.Context
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.R
import org.tasks.data.entity.Alarm
import org.tasks.extensions.Context.is24HourOverride
import org.tasks.kmp.org.tasks.time.getFullDateTime
import java.util.concurrent.TimeUnit

class AlarmToStringTest {

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

    // --- TYPE_REL_START tests ---

    @Test
    fun relStartAtZeroShowsWhenStarted() {
        `when`(resources.getString(R.string.when_started)).thenReturn("When started")
        val alarm = Alarm(type = Alarm.TYPE_REL_START, time = 0L)
        assertEquals("When started", alarmToString.toString(alarm))
    }

    @Test
    fun relStartNegativeTimeShowsBeforeStart() {
        val fiveMinutes = -TimeUnit.MINUTES.toMillis(5)
        stubDurationString(fiveMinutes, "5 minutes")
        `when`(resources.getString(R.string.alarm_before_start, "5 minutes"))
            .thenReturn("5 minutes before start")
        val alarm = Alarm(type = Alarm.TYPE_REL_START, time = fiveMinutes)
        assertEquals("5 minutes before start", alarmToString.toString(alarm))
    }

    @Test
    fun relStartPositiveTimeShowsAfterStart() {
        val tenMinutes = TimeUnit.MINUTES.toMillis(10)
        stubDurationString(tenMinutes, "10 minutes")
        `when`(resources.getString(R.string.alarm_after_start, "10 minutes"))
            .thenReturn("10 minutes after start")
        val alarm = Alarm(type = Alarm.TYPE_REL_START, time = tenMinutes)
        assertEquals("10 minutes after start", alarmToString.toString(alarm))
    }

    // --- TYPE_REL_END tests ---

    @Test
    fun relEndAtZeroShowsWhenDue() {
        `when`(resources.getString(R.string.when_due)).thenReturn("When due")
        val alarm = Alarm(type = Alarm.TYPE_REL_END, time = 0L)
        assertEquals("When due", alarmToString.toString(alarm))
    }

    @Test
    fun relEndNegativeTimeShowsBeforeDue() {
        val oneHour = -TimeUnit.HOURS.toMillis(1)
        stubDurationString(oneHour, "1 hour")
        `when`(resources.getString(R.string.alarm_before_due, "1 hour"))
            .thenReturn("1 hour before due")
        val alarm = Alarm(type = Alarm.TYPE_REL_END, time = oneHour)
        assertEquals("1 hour before due", alarmToString.toString(alarm))
    }

    @Test
    fun relEndPositiveTimeShowsAfterDue() {
        val thirtyMinutes = TimeUnit.MINUTES.toMillis(30)
        stubDurationString(thirtyMinutes, "30 minutes")
        `when`(resources.getString(R.string.alarm_after_due, "30 minutes"))
            .thenReturn("30 minutes after due")
        val alarm = Alarm(type = Alarm.TYPE_REL_END, time = thirtyMinutes)
        assertEquals("30 minutes after due", alarmToString.toString(alarm))
    }

    // --- TYPE_RANDOM tests ---

    @Test
    fun randomAlarmShowsRandomlyEvery() {
        val twoHours = TimeUnit.HOURS.toMillis(2)
        stubDurationString(twoHours, "2 hours")
        `when`(resources.getString(R.string.randomly_every, "2 hours"))
            .thenReturn("Randomly every 2 hours")
        val alarm = Alarm(type = Alarm.TYPE_RANDOM, time = twoHours)
        assertEquals("Randomly every 2 hours", alarmToString.toString(alarm))
    }

    // --- TYPE_SNOOZE tests ---

    @Test
    fun snoozeAlarmShowsSnoozedUntil() {
        val snoozeTime = 1680537600000L // some timestamp
        val formatted = getFullDateTime(snoozeTime, true)
        `when`(resources.getString(R.string.snoozed_until, formatted))
            .thenReturn("Snoozed until $formatted")
        val alarm = Alarm(type = Alarm.TYPE_SNOOZE, time = snoozeTime)
        assertEquals("Snoozed until $formatted", alarmToString.toString(alarm))
    }

    // --- TYPE_DATE_TIME / default tests ---

    @Test
    fun dateTimeAlarmShowsFormattedDateTime() {
        val dateTime = 1680537600000L
        val alarm = Alarm(type = Alarm.TYPE_DATE_TIME, time = dateTime)
        val expected = getFullDateTime(dateTime, true)
        assertEquals(expected, alarmToString.toString(alarm))
    }

    @Test
    fun geoEnterFallsToDefaultFormatting() {
        val dateTime = 1680537600000L
        val alarm = Alarm(type = Alarm.TYPE_GEO_ENTER, time = dateTime)
        val expected = getFullDateTime(dateTime, true)
        assertEquals(expected, alarmToString.toString(alarm))
    }

    @Test
    fun geoExitFallsToDefaultFormatting() {
        val dateTime = 1680537600000L
        val alarm = Alarm(type = Alarm.TYPE_GEO_EXIT, time = dateTime)
        val expected = getFullDateTime(dateTime, true)
        assertEquals(expected, alarmToString.toString(alarm))
    }

    // --- Repeat formatting tests ---

    @Test
    fun alarmWithRepeatAppendsRepeatString() {
        `when`(resources.getString(R.string.when_due)).thenReturn("When due")
        val interval = TimeUnit.HOURS.toMillis(1)
        stubDurationString(interval, "1 hour")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 3))
            .thenReturn("times")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "1 hour", 3, "times"))
            .thenReturn("Repeats every 1 hour, occurs 3 times")
        val alarm = Alarm(type = Alarm.TYPE_REL_END, time = 0L, repeat = 3, interval = interval)
        assertEquals("When due\nRepeats every 1 hour, occurs 3 times", alarmToString.toString(alarm))
    }

    @Test
    fun alarmWithZeroRepeatDoesNotAppendRepeatString() {
        `when`(resources.getString(R.string.when_started)).thenReturn("When started")
        val alarm = Alarm(type = Alarm.TYPE_REL_START, time = 0L, repeat = 0, interval = 0L)
        assertEquals("When started", alarmToString.toString(alarm))
    }

    @Test
    fun repeatWithSingleOccurrence() {
        `when`(resources.getString(R.string.when_due)).thenReturn("When due")
        val interval = TimeUnit.MINUTES.toMillis(30)
        stubDurationString(interval, "30 minutes")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 1))
            .thenReturn("time")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "30 minutes", 1, "time"))
            .thenReturn("Repeats every 30 minutes, occurs 1 time")
        val alarm = Alarm(type = Alarm.TYPE_REL_END, time = 0L, repeat = 1, interval = interval)
        assertEquals("When due\nRepeats every 30 minutes, occurs 1 time", alarmToString.toString(alarm))
    }

    // --- Duration formatting tests ---

    @Test
    fun durationStringForMinutesOnly() {
        val minutes = TimeUnit.MINUTES.toMillis(45)
        stubDurationForMinutes(45)
        val result = resources.getDurationString(minutes)
        assertEquals("45 minutes", result)
    }

    @Test
    fun durationStringForHoursOnly() {
        val hours = TimeUnit.HOURS.toMillis(3)
        stubDurationForHours(3)
        val result = resources.getDurationString(hours)
        assertEquals("3 hours", result)
    }

    @Test
    fun durationStringForDaysOnly() {
        val days = TimeUnit.DAYS.toMillis(5)
        stubDurationForDays(5)
        val result = resources.getDurationString(days)
        assertEquals("5 days", result)
    }

    @Test
    fun durationStringForWeeks() {
        val weeks = TimeUnit.DAYS.toMillis(14)
        stubDurationForWeeks(2)
        val result = resources.getDurationString(weeks)
        assertEquals("2 weeks", result)
    }

    @Test
    fun durationStringForMixedWeeksAndDays() {
        val duration = TimeUnit.DAYS.toMillis(10) // 1 week + 3 days
        stubDurationForWeeks(1)
        stubDurationForDays(3)
        val result = resources.getDurationString(duration)
        assertEquals("1 week 3 days", result)
    }

    @Test
    fun durationStringForNegativeValueUsesAbsolute() {
        val negativeDuration = -TimeUnit.HOURS.toMillis(2)
        stubDurationForHours(2)
        val result = resources.getDurationString(negativeDuration)
        assertEquals("2 hours", result)
    }

    // --- 12-hour format tests ---

    @Test
    fun dateTimeAlarmWith12HourFormat() {
        is24HourOverride = false
        alarmToString = AlarmToString(context)
        val dateTime = 1680537600000L
        val alarm = Alarm(type = Alarm.TYPE_DATE_TIME, time = dateTime)
        val expected = getFullDateTime(dateTime, false)
        assertEquals(expected, alarmToString.toString(alarm))
    }

    @Test
    fun snoozeAlarmWith12HourFormat() {
        is24HourOverride = false
        alarmToString = AlarmToString(context)
        val snoozeTime = 1680537600000L
        val formatted = getFullDateTime(snoozeTime, false)
        `when`(resources.getString(R.string.snoozed_until, formatted))
            .thenReturn("Snoozed until $formatted")
        val alarm = Alarm(type = Alarm.TYPE_SNOOZE, time = snoozeTime)
        assertEquals("Snoozed until $formatted", alarmToString.toString(alarm))
    }

    // --- Complex duration tests ---

    @Test
    fun durationStringForHoursAndMinutes() {
        val duration = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30)
        stubDurationForHours(2)
        stubDurationForMinutes(30)
        val result = resources.getDurationString(duration)
        assertEquals("2 hours 30 minutes", result)
    }

    @Test
    fun durationStringForWeeksDaysHoursMinutes() {
        // 1 week + 2 days + 3 hours + 45 minutes
        val duration = TimeUnit.DAYS.toMillis(9) + TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(45)
        stubDurationForWeeks(1)
        stubDurationForDays(2)
        stubDurationForHours(3)
        stubDurationForMinutes(45)
        val result = resources.getDurationString(duration)
        assertEquals("1 week 2 days 3 hours 45 minutes", result)
    }

    @Test
    fun durationStringForExactlyOneWeek() {
        val duration = TimeUnit.DAYS.toMillis(7)
        stubDurationForWeeks(1)
        val result = resources.getDurationString(duration)
        assertEquals("1 week", result)
    }

    @Test
    fun durationStringForSingleMinute() {
        val duration = TimeUnit.MINUTES.toMillis(1)
        stubDurationForMinutes(1)
        val result = resources.getDurationString(duration)
        assertEquals("1 minute", result)
    }

    @Test
    fun durationStringForSingleHour() {
        val duration = TimeUnit.HOURS.toMillis(1)
        stubDurationForHours(1)
        val result = resources.getDurationString(duration)
        assertEquals("1 hour", result)
    }

    @Test
    fun durationStringForSingleDay() {
        val duration = TimeUnit.DAYS.toMillis(1)
        stubDurationForDays(1)
        val result = resources.getDurationString(duration)
        assertEquals("1 day", result)
    }

    // --- Repeat with various alarm types ---

    @Test
    fun repeatWithRelStartAlarm() {
        val fifteenMinutes = -TimeUnit.MINUTES.toMillis(15)
        stubDurationString(fifteenMinutes, "15 minutes")
        `when`(resources.getString(R.string.alarm_before_start, "15 minutes"))
            .thenReturn("15 minutes before start")
        val interval = TimeUnit.HOURS.toMillis(2)
        stubDurationString(interval, "2 hours")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 5))
            .thenReturn("times")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "2 hours", 5, "times"))
            .thenReturn("Repeats every 2 hours, occurs 5 times")
        val alarm = Alarm(type = Alarm.TYPE_REL_START, time = fifteenMinutes, repeat = 5, interval = interval)
        assertEquals(
            "15 minutes before start\nRepeats every 2 hours, occurs 5 times",
            alarmToString.toString(alarm)
        )
    }

    @Test
    fun repeatWithRandomAlarm() {
        val oneHour = TimeUnit.HOURS.toMillis(1)
        stubDurationString(oneHour, "1 hour")
        `when`(resources.getString(R.string.randomly_every, "1 hour"))
            .thenReturn("Randomly every 1 hour")
        val interval = TimeUnit.MINUTES.toMillis(30)
        stubDurationString(interval, "30 minutes")
        `when`(resources.getQuantityString(R.plurals.repeat_times, 2))
            .thenReturn("times")
        `when`(resources.getString(R.string.repeats_plural_number_of_times, "30 minutes", 2, "times"))
            .thenReturn("Repeats every 30 minutes, occurs 2 times")
        val alarm = Alarm(type = Alarm.TYPE_RANDOM, time = oneHour, repeat = 2, interval = interval)
        assertEquals(
            "Randomly every 1 hour\nRepeats every 30 minutes, occurs 2 times",
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
}
