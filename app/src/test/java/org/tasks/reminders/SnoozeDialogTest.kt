package org.tasks.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.Freeze
import org.tasks.R
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime

/**
 * Tests for [SnoozeDialog.getSnoozeOptions] covering all branches
 * (morning, afternoon, evening, night, past-all).
 */
class SnoozeDialogTest {

    private lateinit var preferences: Preferences

    @Before
    fun setUp() {
        preferences = mock()
    }

    private fun setupTimePreferences(
        morning: Int = 9 * 3600000,
        afternoon: Int = 13 * 3600000,
        evening: Int = 17 * 3600000,
        night: Int = 20 * 3600000,
    ) {
        `when`(preferences.dateShortcutMorning).thenReturn(morning)
        `when`(preferences.dateShortcutAfternoon).thenReturn(afternoon)
        `when`(preferences.dateShortcutEvening).thenReturn(evening)
        `when`(preferences.dateShortcutNight).thenReturn(night)
    }

    @Test
    fun snoozeOptionsAlwaysHasFiveEntries() {
        setupTimePreferences()
        val options = SnoozeDialog.getSnoozeOptions(preferences)
        assertEquals(5, options.size)
    }

    @Test
    fun firstOptionIsAlwaysOneHour() {
        setupTimePreferences()
        val options = SnoozeDialog.getSnoozeOptions(preferences)
        assertEquals(R.string.date_shortcut_hour, options[0].resId)
    }

    @Test
    fun snoozeOptionsAtEarlyMorning_showsMorningFirst() {
        // If now is before morning minus 75 minutes, morning is after hourCutoff
        setupTimePreferences(
            morning = 10 * 3600000,    // 10:00
            afternoon = 13 * 3600000,  // 13:00
            evening = 17 * 3600000,    // 17:00
            night = 20 * 3600000,      // 20:00
        )
        Freeze.freezeAt(DateTime(2025, 1, 15, 6, 0, 0).millis) {
            val options = SnoozeDialog.getSnoozeOptions(preferences)
            assertEquals(5, options.size)
            assertEquals(R.string.date_shortcut_hour, options[0].resId)
            assertEquals(R.string.date_shortcut_morning, options[1].resId)
            assertEquals(R.string.date_shortcut_afternoon, options[2].resId)
            assertEquals(R.string.date_shortcut_evening, options[3].resId)
            assertEquals(R.string.date_shortcut_night, options[4].resId)
        }
    }

    @Test
    fun snoozeOptionsAfterMorning_showsAfternoonFirst() {
        setupTimePreferences(
            morning = 9 * 3600000,     // 09:00
            afternoon = 13 * 3600000,  // 13:00
            evening = 17 * 3600000,    // 17:00
            night = 20 * 3600000,      // 20:00
        )
        // After morning (09:00) but before afternoon (13:00)
        // hourCutoff = now + 75min. If now = 10:00, cutoff = 11:15
        // morning at 09:00 is NOT after 11:15 -> morning branch fails
        // afternoon at 13:00 IS after 11:15 -> afternoon branch
        Freeze.freezeAt(DateTime(2025, 1, 15, 10, 0, 0).millis) {
            val options = SnoozeDialog.getSnoozeOptions(preferences)
            assertEquals(5, options.size)
            assertEquals(R.string.date_shortcut_hour, options[0].resId)
            assertEquals(R.string.date_shortcut_afternoon, options[1].resId)
            assertEquals(R.string.date_shortcut_evening, options[2].resId)
            assertEquals(R.string.date_shortcut_night, options[3].resId)
            assertEquals(R.string.date_shortcut_tomorrow_morning, options[4].resId)
        }
    }

    @Test
    fun snoozeOptionsAfterAfternoon_showsEveningFirst() {
        setupTimePreferences(
            morning = 9 * 3600000,     // 09:00
            afternoon = 13 * 3600000,  // 13:00
            evening = 17 * 3600000,    // 17:00
            night = 20 * 3600000,      // 20:00
        )
        // now = 14:00, cutoff = 15:15
        // morning 09:00 < 15:15, afternoon 13:00 < 15:15, evening 17:00 > 15:15
        Freeze.freezeAt(DateTime(2025, 1, 15, 14, 0, 0).millis) {
            val options = SnoozeDialog.getSnoozeOptions(preferences)
            assertEquals(5, options.size)
            assertEquals(R.string.date_shortcut_hour, options[0].resId)
            assertEquals(R.string.date_shortcut_evening, options[1].resId)
            assertEquals(R.string.date_shortcut_night, options[2].resId)
            assertEquals(R.string.date_shortcut_tomorrow_morning, options[3].resId)
            assertEquals(R.string.date_shortcut_tomorrow_afternoon, options[4].resId)
        }
    }

    @Test
    fun snoozeOptionsAfterEvening_showsNightFirst() {
        setupTimePreferences(
            morning = 9 * 3600000,     // 09:00
            afternoon = 13 * 3600000,  // 13:00
            evening = 17 * 3600000,    // 17:00
            night = 20 * 3600000,      // 20:00
        )
        // now = 18:00, cutoff = 19:15
        // morning, afternoon, evening all < 19:15; night 20:00 > 19:15
        Freeze.freezeAt(DateTime(2025, 1, 15, 18, 0, 0).millis) {
            val options = SnoozeDialog.getSnoozeOptions(preferences)
            assertEquals(5, options.size)
            assertEquals(R.string.date_shortcut_hour, options[0].resId)
            assertEquals(R.string.date_shortcut_night, options[1].resId)
            assertEquals(R.string.date_shortcut_tomorrow_morning, options[2].resId)
            assertEquals(R.string.date_shortcut_tomorrow_afternoon, options[3].resId)
            assertEquals(R.string.date_shortcut_tomorrow_evening, options[4].resId)
        }
    }

    @Test
    fun snoozeOptionsLateAtNight_showsTomorrowOptions() {
        setupTimePreferences(
            morning = 9 * 3600000,     // 09:00
            afternoon = 13 * 3600000,  // 13:00
            evening = 17 * 3600000,    // 17:00
            night = 20 * 3600000,      // 20:00
        )
        // now = 22:00, cutoff = 23:15
        // All today options < 23:15 -> all tomorrow
        Freeze.freezeAt(DateTime(2025, 1, 15, 22, 0, 0).millis) {
            val options = SnoozeDialog.getSnoozeOptions(preferences)
            assertEquals(5, options.size)
            assertEquals(R.string.date_shortcut_hour, options[0].resId)
            assertEquals(R.string.date_shortcut_tomorrow_morning, options[1].resId)
            assertEquals(R.string.date_shortcut_tomorrow_afternoon, options[2].resId)
            assertEquals(R.string.date_shortcut_tomorrow_evening, options[3].resId)
            assertEquals(R.string.date_shortcut_tomorrow_night, options[4].resId)
        }
    }

    @Test
    fun snoozeOptionDateTimesAreInFuture() {
        setupTimePreferences()
        val options = SnoozeDialog.getSnoozeOptions(preferences)
        val now = DateTime()
        // The first option (1 hour) should always be in the future
        assertTrue(options[0].dateTime.millis > now.millis - 1000)
    }

    @Test
    fun firstOptionHasZeroSeconds() {
        setupTimePreferences()
        val options = SnoozeDialog.getSnoozeOptions(preferences)
        val dt = options[0].dateTime
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = dt.millis
        assertEquals(0, calendar.get(java.util.Calendar.SECOND))
        assertEquals(0, calendar.get(java.util.Calendar.MILLISECOND))
    }

    @Test
    fun snoozeOptionToStringContainsResIdAndDateTime() {
        val option = SnoozeOption(R.string.date_shortcut_hour, DateTime(1000L))
        val str = option.toString()
        assertNotNull(str)
        assertTrue(str.contains("resId"))
        assertTrue(str.contains("dateTime"))
    }

    @Test
    fun snoozeOptionResId() {
        val option = SnoozeOption(R.string.date_shortcut_morning, DateTime(2000L))
        assertEquals(R.string.date_shortcut_morning, option.resId)
    }

    @Test
    fun snoozeOptionDateTime() {
        val dt = DateTime(3000L)
        val option = SnoozeOption(R.string.date_shortcut_evening, dt)
        assertEquals(dt, option.dateTime)
    }
}
