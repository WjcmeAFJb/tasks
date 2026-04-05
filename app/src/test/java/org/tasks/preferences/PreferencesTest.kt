package org.tasks.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.tasks.SuspendFreeze
import org.tasks.R
import org.tasks.time.DateTime
import java.util.concurrent.TimeUnit

class PreferencesTest {

    @Mock lateinit var context: Context
    @Mock lateinit var prefs: SharedPreferences
    @Mock lateinit var editor: SharedPreferences.Editor
    @Mock lateinit var resources: Resources

    private lateinit var preferences: Preferences

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(context.getString(anyInt())).thenAnswer { invocation ->
            "key_${invocation.getArgument<Int>(0)}"
        }
        `when`(context.resources).thenReturn(resources)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(editor)
        `when`(editor.putLong(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(editor)
        `when`(editor.remove(anyString())).thenReturn(editor)
        preferences = Preferences(context, "test_prefs")
    }

    // --- isCurrentlyQuietHours ---

    @Test
    fun quietHoursDisabledReturnsFalse() = runTest {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(false)

        assertFalse(preferences.isCurrentlyQuietHours())
    }

    @Test
    fun quietHoursNormalRange_insideRange() = runTest {
        // Quiet hours from 09:00 (32400000) to 17:00 (61200000)
        val nineAM = TimeUnit.HOURS.toMillis(9).toInt()
        val fivePM = TimeUnit.HOURS.toMillis(17).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(nineAM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(fivePM)

        // Freeze at noon — inside [09:00, 17:00)
        val noon = DateTime(2024, 6, 15, 12, 0, 0)
        SuspendFreeze.freezeAt(noon) {
            assertTrue(preferences.isCurrentlyQuietHours())
        }
    }

    @Test
    fun quietHoursNormalRange_outsideRange() = runTest {
        val nineAM = TimeUnit.HOURS.toMillis(9).toInt()
        val fivePM = TimeUnit.HOURS.toMillis(17).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(nineAM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(fivePM)

        // Freeze at 20:00 — outside [09:00, 17:00)
        val eightPM = DateTime(2024, 6, 15, 20, 0, 0)
        SuspendFreeze.freezeAt(eightPM) {
            assertFalse(preferences.isCurrentlyQuietHours())
        }
    }

    @Test
    fun quietHoursWrapsAroundMidnight_insideBeforeMidnight() = runTest {
        // Quiet hours from 22:00 to 10:00 (wraps midnight)
        val tenPM = TimeUnit.HOURS.toMillis(22).toInt()
        val tenAM = TimeUnit.HOURS.toMillis(10).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(tenPM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(tenAM)

        // Freeze at 23:00 — after start, should be quiet
        val elevenPM = DateTime(2024, 6, 15, 23, 0, 0)
        SuspendFreeze.freezeAt(elevenPM) {
            assertTrue(preferences.isCurrentlyQuietHours())
        }
    }

    @Test
    fun quietHoursWrapsAroundMidnight_insideAfterMidnight() = runTest {
        val tenPM = TimeUnit.HOURS.toMillis(22).toInt()
        val tenAM = TimeUnit.HOURS.toMillis(10).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(tenPM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(tenAM)

        // Freeze at 03:00 — before end, should be quiet
        val threeAM = DateTime(2024, 6, 15, 3, 0, 0)
        SuspendFreeze.freezeAt(threeAM) {
            assertTrue(preferences.isCurrentlyQuietHours())
        }
    }

    @Test
    fun quietHoursWrapsAroundMidnight_outsideRange() = runTest {
        val tenPM = TimeUnit.HOURS.toMillis(22).toInt()
        val tenAM = TimeUnit.HOURS.toMillis(10).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(tenPM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(tenAM)

        // Freeze at 15:00 — outside [22:00, 10:00)
        val threePM = DateTime(2024, 6, 15, 15, 0, 0)
        SuspendFreeze.freezeAt(threePM) {
            assertFalse(preferences.isCurrentlyQuietHours())
        }
    }

    // --- adjustForQuietHours ---

    @Test
    fun adjustForQuietHoursReturnsOriginalWhenDisabled() = runTest {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(false)
        val time = DateTime(2024, 6, 15, 12, 0, 0).millis

        assertEquals(time, preferences.adjustForQuietHours(time))
    }

    @Test
    fun adjustForQuietHoursReturnsOriginalWhenOutsideRange() = runTest {
        val nineAM = TimeUnit.HOURS.toMillis(9).toInt()
        val fivePM = TimeUnit.HOURS.toMillis(17).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(nineAM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(fivePM)

        // 20:00 is outside [09:00, 17:00)
        val time = DateTime(2024, 6, 15, 20, 0, 0).millis
        assertEquals(time, preferences.adjustForQuietHours(time))
    }

    @Test
    fun adjustForQuietHoursSnapsToEnd() = runTest {
        val nineAM = TimeUnit.HOURS.toMillis(9).toInt()
        val fivePM = TimeUnit.HOURS.toMillis(17).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(nineAM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(fivePM)

        // 12:00 is inside [09:00, 17:00) — should snap to 17:00
        val time = DateTime(2024, 6, 15, 12, 0, 0).millis
        val expected = DateTime(2024, 6, 15, 17, 0, 0).millis
        assertEquals(expected, preferences.adjustForQuietHours(time))
    }

    @Test
    fun adjustForQuietHoursWrapped_beforeEnd_snapsToEnd() = runTest {
        // Quiet hours from 22:00 to 08:00 (wraps midnight)
        val tenPM = TimeUnit.HOURS.toMillis(22).toInt()
        val eightAM = TimeUnit.HOURS.toMillis(8).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(tenPM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(eightAM)

        // 03:00 is before end — should snap to 08:00 same day
        val time = DateTime(2024, 6, 15, 3, 0, 0).millis
        val expected = DateTime(2024, 6, 15, 8, 0, 0).millis
        assertEquals(expected, preferences.adjustForQuietHours(time))
    }

    @Test
    fun adjustForQuietHoursWrapped_afterStart_snapsToEndNextDay() = runTest {
        val tenPM = TimeUnit.HOURS.toMillis(22).toInt()
        val eightAM = TimeUnit.HOURS.toMillis(8).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(tenPM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(eightAM)

        // 23:00 is after start — should snap to 08:00 next day
        val time = DateTime(2024, 6, 15, 23, 0, 0).millis
        val expected = DateTime(2024, 6, 16, 8, 0, 0).millis
        assertEquals(expected, preferences.adjustForQuietHours(time))
    }

    // --- getMillisPerDayPref (tested via dateShortcutMorning etc.) ---

    @Test
    fun getMillisPerDayPrefReturnsStoredValue() {
        val morningMillis = TimeUnit.HOURS.toMillis(9).toInt()
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_morning}", -1)).thenReturn(morningMillis)

        assertEquals(morningMillis, preferences.dateShortcutMorning)
    }

    @Test
    fun getMillisPerDayPrefFallsBackToDefaultForNegativeValue() {
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_morning}", -1)).thenReturn(-1)
        `when`(resources.getInteger(R.integer.default_morning)).thenReturn(32400000)

        assertEquals(32400000, preferences.dateShortcutMorning)
    }

    @Test
    fun getMillisPerDayPrefFallsBackToDefaultForExceedingMax() {
        // MAX_MILLIS_PER_DAY is TimeUnit.DAYS.toMillis(1).toInt() - 1 = 86399999
        val overMax = DateTime.MAX_MILLIS_PER_DAY + 1
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_afternoon}", -1)).thenReturn(overMax)
        `when`(resources.getInteger(R.integer.default_afternoon)).thenReturn(46800000)

        assertEquals(46800000, preferences.dateShortcutAfternoon)
    }

    @Test
    fun getMillisPerDayPrefAcceptsZero() {
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_evening}", -1)).thenReturn(0)

        assertEquals(0, preferences.dateShortcutEvening)
    }

    @Test
    fun getMillisPerDayPrefAcceptsMaxValue() {
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_night}", -1))
            .thenReturn(DateTime.MAX_MILLIS_PER_DAY)

        assertEquals(DateTime.MAX_MILLIS_PER_DAY, preferences.dateShortcutNight)
    }

    // --- isDefaultCalendarSet ---

    @Test
    fun defaultCalendarSetReturnsFalseWhenNull() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null)).thenReturn(null)
        assertFalse(preferences.isDefaultCalendarSet)
    }

    @Test
    fun defaultCalendarSetReturnsFalseWhenMinusOne() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null)).thenReturn("-1")
        assertFalse(preferences.isDefaultCalendarSet)
    }

    @Test
    fun defaultCalendarSetReturnsFalseWhenZero() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null)).thenReturn("0")
        assertFalse(preferences.isDefaultCalendarSet)
    }

    @Test
    fun defaultCalendarSetReturnsTrueWhenValidValue() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null)).thenReturn("calendar_42")
        assertTrue(preferences.isDefaultCalendarSet)
    }

    // --- getIntegerFromString ---

    @Test
    fun getIntegerFromStringReturnsDefaultWhenNull() {
        `when`(prefs.getString("someKey", null)).thenReturn(null)
        assertEquals(42, preferences.getIntegerFromString("someKey", 42))
    }

    @Test
    fun getIntegerFromStringParsesValidInt() {
        `when`(prefs.getString("someKey", null)).thenReturn("7")
        assertEquals(7, preferences.getIntegerFromString("someKey", 42))
    }

    @Test
    fun getIntegerFromStringReturnsDefaultForNonNumeric() {
        `when`(prefs.getString("someKey", null)).thenReturn("not_a_number")
        assertEquals(42, preferences.getIntegerFromString("someKey", 42))
    }

    @Test
    fun getIntegerFromStringReturnsDefaultForEmptyString() {
        `when`(prefs.getString("someKey", null)).thenReturn("")
        assertEquals(42, preferences.getIntegerFromString("someKey", 42))
    }

    // --- getStringValue exception handling ---

    @Test
    fun getStringValueReturnsNullOnException() {
        `when`(prefs.getString("bad_key", null)).thenThrow(RuntimeException("corrupt"))
        assertNull(preferences.getStringValue("bad_key"))
    }

    @Test
    fun getStringValueReturnsStoredValue() {
        `when`(prefs.getString("good_key", null)).thenReturn("hello")
        assertEquals("hello", preferences.getStringValue("good_key"))
    }

    // --- getBoolean exception handling ---

    @Test
    fun getBooleanReturnsFalseOnClassCastException() {
        `when`(prefs.getBoolean("bad_key", true)).thenThrow(ClassCastException("stored as string"))
        assertTrue(preferences.getBoolean("bad_key", true))
    }

    @Test
    fun getBooleanReturnsDefaultOnClassCastException() {
        `when`(prefs.getBoolean("bad_key", false)).thenThrow(ClassCastException("stored as string"))
        assertFalse(preferences.getBoolean("bad_key", false))
    }

    // --- fontSize ---

    @Test
    fun fontSizeReturnsDefault() {
        `when`(prefs.getInt("key_${R.string.p_fontSize}", 16)).thenReturn(16)
        assertEquals(16, preferences.fontSize)
    }

    @Test
    fun fontSizeReturnsCustomValue() {
        `when`(prefs.getInt("key_${R.string.p_fontSize}", 16)).thenReturn(24)
        assertEquals(24, preferences.fontSize)
    }

    // --- isAstridSort ---

    @Test
    fun isAstridSortRequiresBothEnabledAndSet() {
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort_enabled}", false)).thenReturn(true)
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort}", false)).thenReturn(true)
        assertTrue(preferences.isAstridSort)
    }

    @Test
    fun isAstridSortFalseWhenNotEnabled() {
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort_enabled}", false)).thenReturn(false)
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort}", false)).thenReturn(true)
        assertFalse(preferences.isAstridSort)
    }

    @Test
    fun isAstridSortFalseWhenEnabledButNotSet() {
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort_enabled}", false)).thenReturn(true)
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort}", false)).thenReturn(false)
        assertFalse(preferences.isAstridSort)
    }

    // --- swipeToSnoozeIntervalMS ---

    @Test
    fun swipeToSnoozeReturnsZeroForZeroMinutes() {
        `when`(prefs.getString("key_${R.string.p_rmd_swipe_to_snooze_time_minutes}", null))
            .thenReturn("0")
        assertEquals(0L, preferences.swipeToSnoozeIntervalMS())
    }

    @Test
    fun swipeToSnoozeConvertsMinutesToMillis() {
        `when`(prefs.getString("key_${R.string.p_rmd_swipe_to_snooze_time_minutes}", null))
            .thenReturn("15")
        assertEquals(TimeUnit.MINUTES.toMillis(15), preferences.swipeToSnoozeIntervalMS())
    }

    @Test
    fun swipeToSnoozeDefaultsToZeroForNullValue() {
        `when`(prefs.getString("key_${R.string.p_rmd_swipe_to_snooze_time_minutes}", null))
            .thenReturn(null)
        assertEquals(0L, preferences.swipeToSnoozeIntervalMS())
    }

    // --- defaultDueTime ---

    @Test
    fun defaultDueTimeReturnsDefault() = runTest {
        val defaultTime = TimeUnit.HOURS.toMillis(18).toInt()
        `when`(prefs.getInt("key_${R.string.p_rmd_time}", defaultTime)).thenReturn(defaultTime)
        assertEquals(defaultTime, preferences.defaultDueTime())
    }

    @Test
    fun defaultDueTimeReturnsCustomValue() = runTest {
        val defaultTime = TimeUnit.HOURS.toMillis(18).toInt()
        val customTime = TimeUnit.HOURS.toMillis(9).toInt()
        `when`(prefs.getInt("key_${R.string.p_rmd_time}", defaultTime)).thenReturn(customTime)
        assertEquals(customTime, preferences.defaultDueTime())
    }

    // --- removeByPrefix ---

    @Test
    fun removeByPrefixRemovesMatchingKeys() {
        val allPrefs = mapOf<String, Any>(
            "widget-id-1" to "filter1",
            "widget-id-2" to "filter2",
            "other-key" to "value"
        )
        `when`(prefs.all).thenReturn(allPrefs)

        preferences.removeByPrefix("widget-id-")

        org.mockito.Mockito.verify(editor).remove("widget-id-1")
        org.mockito.Mockito.verify(editor).remove("widget-id-2")
        org.mockito.Mockito.verify(editor, org.mockito.Mockito.never()).remove("other-key")
        org.mockito.Mockito.verify(editor).apply()
    }

    // --- sortMode / groupMode defaults ---

    @Test
    fun sortModeDefaultIsSortDue() {
        `when`(prefs.getInt("key_${R.string.p_sort_mode}", com.todoroo.astrid.core.SortHelper.SORT_DUE))
            .thenReturn(com.todoroo.astrid.core.SortHelper.SORT_DUE)
        assertEquals(com.todoroo.astrid.core.SortHelper.SORT_DUE, preferences.sortMode)
    }

    @Test
    fun groupModeDefaultIsSortDue() {
        `when`(prefs.getInt("key_${R.string.p_group_mode}", com.todoroo.astrid.core.SortHelper.SORT_DUE))
            .thenReturn(com.todoroo.astrid.core.SortHelper.SORT_DUE)
        assertEquals(com.todoroo.astrid.core.SortHelper.SORT_DUE, preferences.groupMode)
    }

    @Test
    fun completedModeDefaultIsSortCompleted() {
        `when`(prefs.getInt("key_${R.string.p_completed_mode}", com.todoroo.astrid.core.SortHelper.SORT_COMPLETED))
            .thenReturn(com.todoroo.astrid.core.SortHelper.SORT_COMPLETED)
        assertEquals(com.todoroo.astrid.core.SortHelper.SORT_COMPLETED, preferences.completedMode)
    }

    @Test
    fun subtaskModeDefaultIsSortManual() {
        `when`(prefs.getInt("key_${R.string.p_subtask_mode}", com.todoroo.astrid.core.SortHelper.SORT_MANUAL))
            .thenReturn(com.todoroo.astrid.core.SortHelper.SORT_MANUAL)
        assertEquals(com.todoroo.astrid.core.SortHelper.SORT_MANUAL, preferences.subtaskMode)
    }
}
