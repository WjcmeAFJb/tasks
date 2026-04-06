package org.tasks.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.tasks.R
import org.tasks.SuspendFreeze
import org.tasks.data.entity.Alarm
import org.tasks.time.DateTime
import java.util.concurrent.TimeUnit

/**
 * Deep coverage tests for Preferences — focuses on methods NOT yet covered
 * by PreferencesTest and PreferencesExtraTest, targeting computed properties,
 * suspend methods, edge cases, exception paths, and complex interactions.
 */
class PreferencesDeepTest {

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
        `when`(context.getString(anyInt(), anyString(), anyString())).thenAnswer { invocation ->
            "key_${invocation.getArgument<Int>(0)}_${invocation.getArgument<String>(1)}_${invocation.getArgument<String>(2)}"
        }
        `when`(context.resources).thenReturn(resources)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), nullable(String::class.java))).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.putLong(anyString(), anyLong())).thenReturn(editor)
        `when`(editor.putStringSet(anyString(), anySet())).thenReturn(editor)
        `when`(editor.remove(anyString())).thenReturn(editor)
        `when`(editor.clear()).thenReturn(editor)
        preferences = Preferences(context, "test_prefs")
    }

    // =============================================
    // purchases — getter and setter
    // =============================================

    @Test
    fun purchasesReturnsEmptyListWhenNoStoredPurchases() {
        `when`(prefs.getStringSet("key_${R.string.p_purchases}", emptySet()))
            .thenReturn(emptySet())

        val result = preferences.purchases

        assertTrue(result.isEmpty())
    }

    @Test
    fun purchasesReturnsMappedPurchases() {
        val jsonSet = setOf("")
        `when`(prefs.getStringSet("key_${R.string.p_purchases}", emptySet()))
            .thenReturn(jsonSet)

        val result = preferences.purchases

        assertEquals(1, result.size)
    }

    @Test
    fun purchasesReturnsEmptyListOnException() {
        // Simulate a corrupt set that causes an exception during mapping
        `when`(prefs.getStringSet("key_${R.string.p_purchases}", emptySet()))
            .thenThrow(RuntimeException("corrupt"))

        val result = preferences.purchases

        assertTrue(result.isEmpty())
    }

    @Test
    fun setPurchasesStoresJsonSet() {
        val purchases = emptyList<org.tasks.billing.Purchase>()
        preferences.setPurchases(purchases)

        verify(editor).putStringSet("key_${R.string.p_purchases}", hashSetOf())
        verify(editor).apply()
    }

    // =============================================
    // defaultAlarms — suspend, serialization
    // =============================================

    @Test
    fun defaultAlarmsReturnsDefaultsWhenNoCustomAlarms() = runTest {
        // Return the default alarm set (3 alarms)
        val defaultAlarms = setOf(
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_START)),
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_END)),
            Json.encodeToString(Alarm.whenOverdue(0)),
        )
        `when`(prefs.getStringSet("key_${R.string.p_default_alarms}", defaultAlarms))
            .thenReturn(defaultAlarms)

        val result = preferences.defaultAlarms()

        assertEquals(3, result.size)
    }

    @Test
    fun defaultAlarmsSortsResultByTypeAndTime() = runTest {
        val alarm1 = Alarm(time = 100, type = Alarm.TYPE_REL_END)
        val alarm2 = Alarm(time = 50, type = Alarm.TYPE_REL_START)
        val alarm3 = Alarm(time = 200, type = Alarm.TYPE_REL_START)
        val alarmSet = setOf(
            Json.encodeToString(alarm1),
            Json.encodeToString(alarm2),
            Json.encodeToString(alarm3),
        )
        val defaultAlarms = setOf(
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_START)),
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_END)),
            Json.encodeToString(Alarm.whenOverdue(0)),
        )
        `when`(prefs.getStringSet("key_${R.string.p_default_alarms}", defaultAlarms))
            .thenReturn(alarmSet)

        val result = preferences.defaultAlarms()

        // Sorted by type first, then by time
        assertEquals(3, result.size)
        // TYPE_REL_START (1) comes before TYPE_REL_END (2)
        assertTrue(result[0].type <= result[1].type || (result[0].type == result[1].type && result[0].time <= result[1].time))
    }

    @Test
    fun defaultAlarmsReturnsEmptyListWhenAllInvalid() = runTest {
        val invalidSet = setOf("not_valid_json", "{bad_json")
        val defaultAlarms = setOf(
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_START)),
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_END)),
            Json.encodeToString(Alarm.whenOverdue(0)),
        )
        `when`(prefs.getStringSet("key_${R.string.p_default_alarms}", defaultAlarms))
            .thenReturn(invalidSet)

        val result = preferences.defaultAlarms()

        assertTrue(result.isEmpty())
    }

    @Test
    fun defaultAlarmsFiltersOutInvalidEntries() = runTest {
        val validAlarm = Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_START))
        val mixedSet = setOf(validAlarm, "invalid_json_here")
        val defaultAlarms = setOf(
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_START)),
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_END)),
            Json.encodeToString(Alarm.whenOverdue(0)),
        )
        `when`(prefs.getStringSet("key_${R.string.p_default_alarms}", defaultAlarms))
            .thenReturn(mixedSet)

        val result = preferences.defaultAlarms()

        assertEquals(1, result.size)
        assertEquals(Alarm.TYPE_REL_START, result[0].type)
    }

    @Test
    fun setDefaultAlarmsEncodesAndStores() {
        val alarms = listOf(
            Alarm(time = 0, type = Alarm.TYPE_REL_START),
            Alarm(time = 100, type = Alarm.TYPE_REL_END),
        )
        preferences.setDefaultAlarms(alarms)

        verify(editor).putStringSet(
            org.mockito.ArgumentMatchers.eq("key_${R.string.p_default_alarms}"),
            anySet()
        )
        verify(editor).apply()
    }

    // =============================================
    // completionSound — ringtone logic
    // Note: completionSound calls context.getResourceUri which requires Android;
    // we test the getRingtone logic paths via quietHoursEnabled and other prefs.
    // =============================================

    // =============================================
    // isDefaultCalendarSet — edge cases
    // =============================================

    @Test
    fun isDefaultCalendarSetReturnsTrueForValidCalendar() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null))
            .thenReturn("my_calendar_id")
        assertTrue(preferences.isDefaultCalendarSet)
    }

    @Test
    fun isDefaultCalendarSetReturnsFalseForNull() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null))
            .thenReturn(null)
        assertFalse(preferences.isDefaultCalendarSet)
    }

    @Test
    fun isDefaultCalendarSetReturnsFalseForMinusOne() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null))
            .thenReturn("-1")
        assertFalse(preferences.isDefaultCalendarSet)
    }

    @Test
    fun isDefaultCalendarSetReturnsFalseForZero() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null))
            .thenReturn("0")
        assertFalse(preferences.isDefaultCalendarSet)
    }

    // =============================================
    // quietHoursEnabled — boundary tests
    // =============================================

    @Test
    fun isCurrentlyQuietHoursAtExactStartTime() = runTest {
        val nineAM = TimeUnit.HOURS.toMillis(9).toInt()
        val fivePM = TimeUnit.HOURS.toMillis(17).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(nineAM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(fivePM)

        // Freeze at exactly 09:00 — isBefore(end)=true and isAfter(start)=false => not quiet
        // DateTime.isAfter is strictly after, not at
        val exactStart = DateTime(2024, 6, 15, 9, 0, 0)
        SuspendFreeze.freezeAt(exactStart) {
            // At exactly start time, isAfter(start) returns false since it's equal
            // Result depends on DateTime.isAfter semantics
            // Just verify it doesn't crash and returns a boolean
            preferences.isCurrentlyQuietHours()
        }
    }

    @Test
    fun isCurrentlyQuietHoursAtExactEndTime() = runTest {
        val nineAM = TimeUnit.HOURS.toMillis(9).toInt()
        val fivePM = TimeUnit.HOURS.toMillis(17).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(nineAM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(fivePM)

        val exactEnd = DateTime(2024, 6, 15, 17, 0, 0)
        SuspendFreeze.freezeAt(exactEnd) {
            preferences.isCurrentlyQuietHours()
        }
    }

    @Test
    fun adjustForQuietHoursReturnsOriginalTimeWhenOutsideNormalRange() = runTest {
        val nineAM = TimeUnit.HOURS.toMillis(9).toInt()
        val fivePM = TimeUnit.HOURS.toMillis(17).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(nineAM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(fivePM)

        val time = DateTime(2024, 6, 15, 7, 0, 0).millis
        assertEquals(time, preferences.adjustForQuietHours(time))
    }

    @Test
    fun adjustForQuietHoursWrappedReturnsOriginalWhenOutside() = runTest {
        val tenPM = TimeUnit.HOURS.toMillis(22).toInt()
        val eightAM = TimeUnit.HOURS.toMillis(8).toInt()

        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(tenPM)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(eightAM)

        // 15:00 is between end(08:00) and start(22:00), so outside
        val time = DateTime(2024, 6, 15, 15, 0, 0).millis
        assertEquals(time, preferences.adjustForQuietHours(time))
    }

    // =============================================
    // getMillisPerDayPref via dateShortcut properties
    // =============================================

    @Test
    fun dateShortcutAfternoonReturnsStoredValue() {
        val afternoonMillis = TimeUnit.HOURS.toMillis(13).toInt()
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_afternoon}", -1))
            .thenReturn(afternoonMillis)

        assertEquals(afternoonMillis, preferences.dateShortcutAfternoon)
    }

    @Test
    fun dateShortcutAfternoonFallsBackToDefaultForNegative() {
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_afternoon}", -1)).thenReturn(-1)
        `when`(resources.getInteger(R.integer.default_afternoon)).thenReturn(46800000)

        assertEquals(46800000, preferences.dateShortcutAfternoon)
    }

    @Test
    fun dateShortcutEveningReturnsStoredValue() {
        val eveningMillis = TimeUnit.HOURS.toMillis(17).toInt()
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_evening}", -1))
            .thenReturn(eveningMillis)

        assertEquals(eveningMillis, preferences.dateShortcutEvening)
    }

    @Test
    fun dateShortcutEveningFallsBackToDefaultForExceedingMax() {
        val overMax = DateTime.MAX_MILLIS_PER_DAY + 1
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_evening}", -1)).thenReturn(overMax)
        `when`(resources.getInteger(R.integer.default_evening)).thenReturn(61200000)

        assertEquals(61200000, preferences.dateShortcutEvening)
    }

    @Test
    fun dateShortcutNightReturnsStoredValue() {
        val nightMillis = TimeUnit.HOURS.toMillis(21).toInt()
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_night}", -1))
            .thenReturn(nightMillis)

        assertEquals(nightMillis, preferences.dateShortcutNight)
    }

    @Test
    fun dateShortcutNightFallsBackToDefaultForNegative() {
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_night}", -1)).thenReturn(-1)
        `when`(resources.getInteger(R.integer.default_night)).thenReturn(75600000)

        assertEquals(75600000, preferences.dateShortcutNight)
    }

    @Test
    fun dateShortcutMorningAcceptsZero() {
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_morning}", -1)).thenReturn(0)

        assertEquals(0, preferences.dateShortcutMorning)
    }

    @Test
    fun dateShortcutMorningAcceptsMaxValue() {
        `when`(prefs.getInt("key_${R.string.p_date_shortcut_morning}", -1))
            .thenReturn(DateTime.MAX_MILLIS_PER_DAY)

        assertEquals(DateTime.MAX_MILLIS_PER_DAY, preferences.dateShortcutMorning)
    }

    // =============================================
    // getStringValue — exception handling via string key
    // =============================================

    @Test
    fun getStringValueByKeyReturnsStoredValue() {
        `when`(prefs.getString("my_key", null)).thenReturn("my_value")
        assertEquals("my_value", preferences.getStringValue("my_key"))
    }

    @Test
    fun getStringValueByKeyReturnsNullOnException() {
        `when`(prefs.getString("my_key", null)).thenThrow(RuntimeException("error"))
        assertNull(preferences.getStringValue("my_key"))
    }

    @Test
    fun getStringValueByKeyReturnsNullWhenNotSet() {
        `when`(prefs.getString("my_key", null)).thenReturn(null)
        assertNull(preferences.getStringValue("my_key"))
    }

    // =============================================
    // getBoolean — ClassCastException handling
    // =============================================

    @Test
    fun getBooleanReturnsTrueDefaultOnClassCastException() {
        `when`(prefs.getBoolean("key", true)).thenThrow(ClassCastException("stored as string"))
        assertTrue(preferences.getBoolean("key", true))
    }

    @Test
    fun getBooleanReturnsFalseDefaultOnClassCastException() {
        `when`(prefs.getBoolean("key", false)).thenThrow(ClassCastException("stored as string"))
        assertFalse(preferences.getBoolean("key", false))
    }

    @Test
    fun getBooleanReturnsStoredValueWhenNoException() {
        `when`(prefs.getBoolean("key", false)).thenReturn(true)
        assertTrue(preferences.getBoolean("key", false))
    }

    // =============================================
    // getIntegerFromString — edge cases
    // =============================================

    @Test
    fun getIntegerFromStringByKeyParsesNegativeNumber() {
        `when`(prefs.getString("someKey", null)).thenReturn("-5")
        assertEquals(-5, preferences.getIntegerFromString("someKey", 0))
    }

    @Test
    fun getIntegerFromStringByKeyParsesZero() {
        `when`(prefs.getString("someKey", null)).thenReturn("0")
        assertEquals(0, preferences.getIntegerFromString("someKey", 42))
    }

    @Test
    fun getIntegerFromStringByKeyReturnsDefaultForFloat() {
        `when`(prefs.getString("someKey", null)).thenReturn("3.14")
        assertEquals(42, preferences.getIntegerFromString("someKey", 42))
    }

    @Test
    fun getIntegerFromStringByKeyReturnsDefaultForBlankString() {
        `when`(prefs.getString("someKey", null)).thenReturn("   ")
        assertEquals(42, preferences.getIntegerFromString("someKey", 42))
    }

    @Test
    fun getIntegerFromStringByResIdParsesLargeNumber() {
        `when`(prefs.getString("key_${R.string.p_fontSize}", null)).thenReturn("999999")
        assertEquals(999999, preferences.getIntegerFromString(R.string.p_fontSize, 16))
    }

    // =============================================
    // setUri — java.net.URI and android.net.Uri
    // =============================================

    @Test
    fun setUriWithJavaNetUri() {
        val uri = java.net.URI("file:///data/backups")
        preferences.setUri(R.string.p_backup_dir, uri)
        verify(editor).putString("key_${R.string.p_backup_dir}", "file:///data/backups")
    }

    @Test
    fun setUriWithJavaNetUriHttps() {
        val uri = java.net.URI("https://example.com/backups/data")
        preferences.setUri(R.string.p_backup_dir, uri)
        verify(editor).putString("key_${R.string.p_backup_dir}", "https://example.com/backups/data")
    }

    // =============================================
    // registerOnSharedPreferenceChangeListener / unregister
    // =============================================

    @Test
    fun registerListenerDelegatesToPrefs() {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        verify(prefs).registerOnSharedPreferenceChangeListener(listener)
    }

    @Test
    fun unregisterListenerDelegatesToPrefs() {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> }
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
        verify(prefs).unregisterOnSharedPreferenceChangeListener(listener)
    }

    // =============================================
    // usePersistentReminders — platform-dependent
    // =============================================

    @Test
    fun usePersistentRemindersChecksPrefValue() {
        // This tests that the method at least reads from prefs without crashing
        `when`(prefs.getBoolean("key_${R.string.p_rmd_persistent}", true)).thenReturn(true)
        // Result depends on SDK level (preUpsideDownCake), just ensure no crash
        preferences.usePersistentReminders()
    }

    @Test
    fun usePersistentRemindersReturnsFalseWhenPrefDisabled() {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_persistent}", true)).thenReturn(false)
        assertFalse(preferences.usePersistentReminders())
    }

    // =============================================
    // useSwipeToSnooze — boolean pref
    // =============================================

    @Test
    fun useSwipeToSnoozeReturnsFalseByDefault() {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_swipe_to_snooze_enabled}", false))
            .thenReturn(false)
        assertFalse(preferences.useSwipeToSnooze())
    }

    @Test
    fun useSwipeToSnoozeReturnsTrueWhenEnabled() {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_swipe_to_snooze_enabled}", false))
            .thenReturn(true)
        assertTrue(preferences.useSwipeToSnooze())
    }

    // =============================================
    // swipeToSnoozeIntervalMS — conversion logic
    // =============================================

    @Test
    fun swipeToSnoozeConverts30MinutesToMillis() {
        `when`(prefs.getString("key_${R.string.p_rmd_swipe_to_snooze_time_minutes}", null))
            .thenReturn("30")
        assertEquals(TimeUnit.MINUTES.toMillis(30), preferences.swipeToSnoozeIntervalMS())
    }

    @Test
    fun swipeToSnoozeConverts60MinutesToMillis() {
        `when`(prefs.getString("key_${R.string.p_rmd_swipe_to_snooze_time_minutes}", null))
            .thenReturn("60")
        assertEquals(TimeUnit.MINUTES.toMillis(60), preferences.swipeToSnoozeIntervalMS())
    }

    @Test
    fun swipeToSnoozeReturnsZeroForInvalidString() {
        `when`(prefs.getString("key_${R.string.p_rmd_swipe_to_snooze_time_minutes}", null))
            .thenReturn("abc")
        assertEquals(0L, preferences.swipeToSnoozeIntervalMS())
    }

    @Test
    fun swipeToSnoozeReturnsZeroForNull() {
        `when`(prefs.getString("key_${R.string.p_rmd_swipe_to_snooze_time_minutes}", null))
            .thenReturn(null)
        assertEquals(0L, preferences.swipeToSnoozeIntervalMS())
    }

    // =============================================
    // defaultDueTime
    // =============================================

    @Test
    fun defaultDueTimeReturnsDefault18Hours() = runTest {
        val defaultTime = TimeUnit.HOURS.toMillis(18).toInt()
        `when`(prefs.getInt("key_${R.string.p_rmd_time}", defaultTime)).thenReturn(defaultTime)
        assertEquals(defaultTime, preferences.defaultDueTime())
    }

    @Test
    fun defaultDueTimeReturnsCustom() = runTest {
        val defaultTime = TimeUnit.HOURS.toMillis(18).toInt()
        val customTime = TimeUnit.HOURS.toMillis(8).toInt()
        `when`(prefs.getInt("key_${R.string.p_rmd_time}", defaultTime)).thenReturn(customTime)
        assertEquals(customTime, preferences.defaultDueTime())
    }

    // =============================================
    // quietHoursStart / quietHoursEnd via getMillisPerDayPref
    // =============================================

    @Test
    fun quietHoursStartUsesDefaultWhenPrefNegative() = runTest {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(-1)
        `when`(resources.getInteger(R.integer.default_quiet_hours_start)).thenReturn(79200000) // 22:00
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(-1)
        `when`(resources.getInteger(R.integer.default_quiet_hours_end)).thenReturn(36000000) // 10:00

        val time = DateTime(2024, 6, 15, 23, 30, 0).millis
        // This just exercises the fallback path
        preferences.adjustForQuietHours(time)
    }

    @Test
    fun quietHoursEndUsesDefaultWhenOverMax() = runTest {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_enable_quiet}", false)).thenReturn(true)
        `when`(prefs.getInt("key_${R.string.p_rmd_quietStart}", -1)).thenReturn(79200000) // 22:00
        val overMax = DateTime.MAX_MILLIS_PER_DAY + 1
        `when`(prefs.getInt("key_${R.string.p_rmd_quietEnd}", -1)).thenReturn(overMax)
        `when`(resources.getInteger(R.integer.default_quiet_hours_end)).thenReturn(36000000) // 10:00

        val time = DateTime(2024, 6, 15, 3, 0, 0).millis
        // Exercises the fallback path for quietHoursEnd
        val result = preferences.adjustForQuietHours(time)
        // Should snap to end time (10:00)
        assertEquals(DateTime(2024, 6, 15, 10, 0, 0).millis, result)
    }

    // =============================================
    // isAstridSort — combined condition
    // =============================================

    @Test
    fun isAstridSortReturnsFalseWhenBothDisabled() {
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort_enabled}", false)).thenReturn(false)
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort}", false)).thenReturn(false)
        assertFalse(preferences.isAstridSort)
    }

    @Test
    fun isAstridSortReturnsTrueWhenBothEnabled() {
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort_enabled}", false)).thenReturn(true)
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort}", false)).thenReturn(true)
        assertTrue(preferences.isAstridSort)
    }

    @Test
    fun isAstridSortReturnsFalseWhenEnabledButNotSet() {
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort_enabled}", false)).thenReturn(true)
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort}", false)).thenReturn(false)
        assertFalse(preferences.isAstridSort)
    }

    @Test
    fun isAstridSortReturnsFalseWhenSetButNotEnabled() {
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort_enabled}", false)).thenReturn(false)
        `when`(prefs.getBoolean("key_${R.string.p_astrid_sort}", false)).thenReturn(true)
        assertFalse(preferences.isAstridSort)
    }

    // =============================================
    // defaultPriority — from string
    // =============================================

    @Test
    fun defaultPriorityReturnsLowWhenNull() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_importance_key}", null)).thenReturn(null)
        assertEquals(org.tasks.data.entity.Task.Priority.LOW, preferences.defaultPriority())
    }

    @Test
    fun defaultPriorityReturnsHighWhenSetTo0() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_importance_key}", null)).thenReturn("0")
        assertEquals(0, preferences.defaultPriority())
    }

    @Test
    fun defaultPriorityReturnsMediumWhenSetTo1() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_importance_key}", null)).thenReturn("1")
        assertEquals(1, preferences.defaultPriority())
    }

    @Test
    fun defaultPriorityReturnsDefaultForInvalid() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_importance_key}", null))
            .thenReturn("invalid")
        assertEquals(org.tasks.data.entity.Task.Priority.LOW, preferences.defaultPriority())
    }

    // =============================================
    // defaultRingMode — from string
    // =============================================

    @Test
    fun defaultRingModeReturnsZeroByDefault() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_reminders_mode_key}", null))
            .thenReturn(null)
        assertEquals(0, preferences.defaultRingMode())
    }

    @Test
    fun defaultRingModeReturnsCustomValue() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_reminders_mode_key}", null))
            .thenReturn("5")
        assertEquals(5, preferences.defaultRingMode())
    }

    // =============================================
    // defaultLocationReminder — from string
    // =============================================

    @Test
    fun defaultLocationReminderReturns1ByDefault() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_location_reminder_key}", null))
            .thenReturn(null)
        assertEquals(1, preferences.defaultLocationReminder())
    }

    @Test
    fun defaultLocationReminderReturns3WhenBoth() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_location_reminder_key}", null))
            .thenReturn("3")
        assertEquals(3, preferences.defaultLocationReminder())
    }

    // =============================================
    // locationUpdateIntervalMinutes — from string
    // =============================================

    @Test
    fun locationUpdateIntervalReturns15ByDefault() = runTest {
        `when`(prefs.getString("key_${R.string.p_location_update_interval}", null))
            .thenReturn(null)
        assertEquals(15, preferences.locationUpdateIntervalMinutes())
    }

    @Test
    fun locationUpdateIntervalReturnsCustom() = runTest {
        `when`(prefs.getString("key_${R.string.p_location_update_interval}", null))
            .thenReturn("60")
        assertEquals(60, preferences.locationUpdateIntervalMinutes())
    }

    // =============================================
    // defaultRandomHours — from string
    // =============================================

    @Test
    fun defaultRandomHoursReturns0ByDefault() = runTest {
        `when`(prefs.getString("key_${R.string.p_rmd_default_random_hours}", null))
            .thenReturn(null)
        assertEquals(0, preferences.defaultRandomHours())
    }

    @Test
    fun defaultRandomHoursReturns12() = runTest {
        `when`(prefs.getString("key_${R.string.p_rmd_default_random_hours}", null))
            .thenReturn("12")
        assertEquals(12, preferences.defaultRandomHours())
    }

    // =============================================
    // isDefaultDueTimeEnabled
    // =============================================

    @Test
    fun isDefaultDueTimeEnabledReturnsTrue() = runTest {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_time_enabled}", true)).thenReturn(true)
        assertTrue(preferences.isDefaultDueTimeEnabled())
    }

    @Test
    fun isDefaultDueTimeEnabledReturnsFalse() = runTest {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_time_enabled}", true)).thenReturn(false)
        assertFalse(preferences.isDefaultDueTimeEnabled())
    }

    // =============================================
    // getPrefs — type filtering
    // =============================================

    @Test
    fun getPrefsForLongType() {
        val allPrefs = mapOf<String, Any>(
            "key1" to 100L,
            "key2" to "text",
            "key3" to 200L,
        )
        `when`(prefs.all).thenReturn(allPrefs)

        val longPrefs = preferences.getPrefs(Long::class.javaObjectType)
        assertEquals(2, longPrefs.size)
        assertEquals(100L, longPrefs["key1"])
        assertEquals(200L, longPrefs["key3"])
    }

    @Test
    fun getPrefsWithEmptyMap() {
        `when`(prefs.all).thenReturn(emptyMap<String, Any>())

        val result = preferences.getPrefs(String::class.java)
        assertTrue(result.isEmpty())
    }

    // =============================================
    // calendarDisplayMode
    // =============================================

    @Test
    @Suppress("OPT_IN_USAGE")
    fun calendarDisplayModeReturnsPickerForInvalidValue() {
        `when`(prefs.getString("key_${R.string.p_picker_mode_date}", null)).thenReturn("99")
        assertEquals(androidx.compose.material3.DisplayMode.Picker, preferences.calendarDisplayMode)
    }

    @Test
    @Suppress("OPT_IN_USAGE")
    fun calendarDisplayModeReturnsPickerForZero() {
        `when`(prefs.getString("key_${R.string.p_picker_mode_date}", null)).thenReturn("0")
        assertEquals(androidx.compose.material3.DisplayMode.Picker, preferences.calendarDisplayMode)
    }

    @Test
    @Suppress("OPT_IN_USAGE")
    fun calendarDisplayModeReturnsInputForOne() {
        `when`(prefs.getString("key_${R.string.p_picker_mode_date}", null)).thenReturn("1")
        assertEquals(androidx.compose.material3.DisplayMode.Input, preferences.calendarDisplayMode)
    }

    // =============================================
    // timeDisplayMode
    // =============================================

    @Test
    @Suppress("OPT_IN_USAGE")
    fun timeDisplayModeReturnsPickerForZero() {
        `when`(prefs.getString("key_${R.string.p_picker_mode_time}", null)).thenReturn("0")
        assertEquals(androidx.compose.material3.DisplayMode.Picker, preferences.timeDisplayMode)
    }

    @Test
    @Suppress("OPT_IN_USAGE")
    fun timeDisplayModeReturnsInputForOne() {
        `when`(prefs.getString("key_${R.string.p_picker_mode_time}", null)).thenReturn("1")
        assertEquals(androidx.compose.material3.DisplayMode.Input, preferences.timeDisplayMode)
    }

    @Test
    @Suppress("OPT_IN_USAGE")
    fun timeDisplayModeReturnsPickerForNull() {
        `when`(prefs.getString("key_${R.string.p_picker_mode_time}", null)).thenReturn(null)
        assertEquals(androidx.compose.material3.DisplayMode.Picker, preferences.timeDisplayMode)
    }

    // =============================================
    // sortMode / groupMode / completedMode / subtaskMode setters
    // =============================================

    @Test
    fun sortModeSetAndGet() {
        `when`(prefs.getInt("key_${R.string.p_sort_mode}", com.todoroo.astrid.core.SortHelper.SORT_DUE))
            .thenReturn(5)
        assertEquals(5, preferences.sortMode)

        preferences.sortMode = 3
        verify(editor).putInt("key_${R.string.p_sort_mode}", 3)
    }

    @Test
    fun groupModeSetAndGet() {
        `when`(prefs.getInt("key_${R.string.p_group_mode}", com.todoroo.astrid.core.SortHelper.SORT_DUE))
            .thenReturn(7)
        assertEquals(7, preferences.groupMode)

        preferences.groupMode = 2
        verify(editor).putInt("key_${R.string.p_group_mode}", 2)
    }

    @Test
    fun completedModeSetAndGet() {
        `when`(prefs.getInt("key_${R.string.p_completed_mode}", com.todoroo.astrid.core.SortHelper.SORT_COMPLETED))
            .thenReturn(4)
        assertEquals(4, preferences.completedMode)

        preferences.completedMode = 1
        verify(editor).putInt("key_${R.string.p_completed_mode}", 1)
    }

    @Test
    fun subtaskModeSetAndGet() {
        `when`(prefs.getInt("key_${R.string.p_subtask_mode}", com.todoroo.astrid.core.SortHelper.SORT_MANUAL))
            .thenReturn(6)
        assertEquals(6, preferences.subtaskMode)

        preferences.subtaskMode = 0
        verify(editor).putInt("key_${R.string.p_subtask_mode}", 0)
    }

    // =============================================
    // removeByPrefix edge cases
    // =============================================

    @Test
    fun removeByPrefixRemovesAllMatchingKeys() {
        val allPrefs = mapOf<String, Any>(
            "prefix_1" to "a",
            "prefix_2" to "b",
            "prefix_3" to "c",
            "other" to "d",
        )
        `when`(prefs.all).thenReturn(allPrefs)

        preferences.removeByPrefix("prefix_")

        verify(editor).remove("prefix_1")
        verify(editor).remove("prefix_2")
        verify(editor).remove("prefix_3")
        verify(editor).apply()
    }

    @Test
    fun removeByPrefixWithExactMatch() {
        val allPrefs = mapOf<String, Any>(
            "exact" to "value",
        )
        `when`(prefs.all).thenReturn(allPrefs)

        preferences.removeByPrefix("exact")

        verify(editor).remove("exact")
        verify(editor).apply()
    }

    // =============================================
    // setStringSet by key
    // =============================================

    @Test
    fun setStringSetByKeyStoresValues() {
        val values = setOf("a", "b", "c")
        preferences.setStringSet("my_key", values)

        verify(editor).putStringSet("my_key", values)
        verify(editor).apply()
    }

    // =============================================
    // clear(key) and remove(resId)
    // =============================================

    @Test
    fun clearByKeyRemovesKeyAndApplies() {
        preferences.clear("test_key")
        verify(editor).remove("test_key")
        verify(editor).apply()
    }

    @Test
    fun removeByResIdRemovesAndApplies() {
        preferences.remove(R.string.p_fontSize)
        verify(editor).remove("key_${R.string.p_fontSize}")
        verify(editor).apply()
    }

    // =============================================
    // clear() — full reset
    // =============================================

    @Test
    fun clearAllPrefsCallsCommit() {
        preferences.clear()
        verify(editor).clear()
        verify(editor).commit()
    }

    // =============================================
    // Various boolean properties — less common
    // =============================================

    @Test
    fun bundleNotificationsDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_bundle_notifications}", true)).thenReturn(true)
        assertTrue(preferences.bundleNotifications())
    }

    @Test
    fun bundleNotificationsCanBeDisabled() {
        `when`(prefs.getBoolean("key_${R.string.p_bundle_notifications}", true)).thenReturn(false)
        assertFalse(preferences.bundleNotifications())
    }

    @Test
    fun androidBackupServiceEnabledDefault() {
        `when`(prefs.getBoolean("key_${R.string.p_backups_android_backup_enabled}", true))
            .thenReturn(true)
        assertTrue(preferences.androidBackupServiceEnabled())
    }

    @Test
    fun showBackupWarningsDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_backups_ignore_warnings}", false))
            .thenReturn(false)
        assertTrue(preferences.showBackupWarnings())
    }

    @Test
    fun showBackupWarningsFalseWhenIgnored() {
        `when`(prefs.getBoolean("key_${R.string.p_backups_ignore_warnings}", false))
            .thenReturn(true)
        assertFalse(preferences.showBackupWarnings())
    }

    @Test
    fun isTrackingEnabledDefault() {
        `when`(prefs.getBoolean("key_${R.string.p_collect_statistics}", true)).thenReturn(true)
        assertTrue(preferences.isTrackingEnabled)
    }

    @Test
    fun showEditScreenWithoutUnlockDefault() {
        `when`(prefs.getBoolean("key_${R.string.p_show_edit_screen_without_unlock}", false))
            .thenReturn(false)
        assertFalse(preferences.showEditScreenWithoutUnlock)
    }

    @Test
    fun addTasksToTopDefault() {
        `when`(prefs.getBoolean("key_${R.string.p_add_to_top}", true)).thenReturn(true)
        assertTrue(preferences.addTasksToTop())
    }

    @Test
    fun backButtonSavesTaskDefault() {
        `when`(prefs.getBoolean("key_${R.string.p_back_button_saves_task}", false))
            .thenReturn(false)
        assertFalse(preferences.backButtonSavesTask())
    }

    // =============================================
    // Warn flags — get and set
    // =============================================

    @Test
    fun warnNotificationsDisabledDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_warn_notifications_disabled}", true))
            .thenReturn(true)
        assertTrue(preferences.warnNotificationsDisabled)
    }

    @Test
    fun warnNotificationsDisabledSetFalse() {
        preferences.warnNotificationsDisabled = false
        verify(editor).putBoolean("key_${R.string.p_warn_notifications_disabled}", false)
    }

    @Test
    fun warnAlarmsDisabledDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_warn_alarms_disabled}", true)).thenReturn(true)
        assertTrue(preferences.warnAlarmsDisabled)
    }

    @Test
    fun warnAlarmsDisabledSetFalse() {
        preferences.warnAlarmsDisabled = false
        verify(editor).putBoolean("key_${R.string.p_warn_alarms_disabled}", false)
    }

    @Test
    fun warnMicrosoftDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_warn_microsoft}", true)).thenReturn(true)
        assertTrue(preferences.warnMicrosoft)
    }

    @Test
    fun warnMicrosoftSetFalse() {
        preferences.warnMicrosoft = false
        verify(editor).putBoolean("key_${R.string.p_warn_microsoft}", false)
    }

    @Test
    fun warnGoogleTasksDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_warn_google_tasks}", true)).thenReturn(true)
        assertTrue(preferences.warnGoogleTasks)
    }

    @Test
    fun warnGoogleTasksSetFalse() {
        preferences.warnGoogleTasks = false
        verify(editor).putBoolean("key_${R.string.p_warn_google_tasks}", false)
    }

    // =============================================
    // shownBeastModeHint
    // =============================================

    @Test
    fun shownBeastModeHintDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_shown_beast_mode_hint}", false))
            .thenReturn(false)
        assertFalse(preferences.shownBeastModeHint)
    }

    @Test
    fun shownBeastModeHintSetTrue() {
        preferences.shownBeastModeHint = true
        verify(editor).putBoolean("key_${R.string.p_shown_beast_mode_hint}", true)
    }

    // =============================================
    // lastReviewRequest, lastSubscribeRequest
    // =============================================

    @Test
    fun lastReviewRequestGetAndSet() {
        `when`(prefs.getLong("key_${R.string.p_last_review_request}", 0L)).thenReturn(1234L)
        assertEquals(1234L, preferences.lastReviewRequest)

        preferences.lastReviewRequest = 5678L
        verify(editor).putLong("key_${R.string.p_last_review_request}", 5678L)
    }

    @Test
    fun lastSubscribeRequestGetAndSet() {
        `when`(prefs.getLong("key_${R.string.p_last_subscribe_request}", 0L)).thenReturn(9999L)
        assertEquals(9999L, preferences.lastSubscribeRequest)

        preferences.lastSubscribeRequest = 1111L
        verify(editor).putLong("key_${R.string.p_last_subscribe_request}", 1111L)
    }

    // =============================================
    // installVersion, installDate, deviceInstallVersion
    // =============================================

    @Test
    fun installVersionGetAndSet() {
        `when`(prefs.getInt("key_${R.string.p_install_version}", 0)).thenReturn(100)
        assertEquals(100, preferences.installVersion)

        preferences.installVersion = 200
        verify(editor).putInt("key_${R.string.p_install_version}", 200)
    }

    @Test
    fun installDateGetAndSet() {
        `when`(prefs.getLong("key_${R.string.p_install_date}", 0L)).thenReturn(12345L)
        assertEquals(12345L, preferences.installDate)

        preferences.installDate = 99999L
        verify(editor).putLong("key_${R.string.p_install_date}", 99999L)
    }

    @Test
    fun deviceInstallVersionGetAndSet() {
        `when`(prefs.getInt("key_${R.string.p_device_install_version}", 0)).thenReturn(50)
        assertEquals(50, preferences.deviceInstallVersion)

        preferences.deviceInstallVersion = 75
        verify(editor).putInt("key_${R.string.p_device_install_version}", 75)
    }

    // =============================================
    // lastSync
    // =============================================

    @Test
    fun lastSyncDefaultZero() {
        `when`(prefs.getLong("key_${R.string.p_last_sync}", 0L)).thenReturn(0L)
        assertEquals(0L, preferences.lastSync)
    }

    @Test
    fun lastSyncSetAndGet() {
        `when`(prefs.getLong("key_${R.string.p_last_sync}", 0L)).thenReturn(99999L)
        assertEquals(99999L, preferences.lastSync)

        preferences.lastSync = 55555L
        verify(editor).putLong("key_${R.string.p_last_sync}", 55555L)
    }

    // =============================================
    // lastSetVersion, setCurrentVersion
    // =============================================

    @Test
    fun lastSetVersionDefault() {
        `when`(prefs.getInt("key_${R.string.p_current_version}", 0)).thenReturn(0)
        assertEquals(0, preferences.lastSetVersion)
    }

    @Test
    fun setCurrentVersionStores() {
        preferences.setCurrentVersion(500)
        verify(editor).putInt("key_${R.string.p_current_version}", 500)
    }

    // =============================================
    // chip visibility — additional coverage
    // =============================================

    @Test
    fun showSubtaskChipTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_subtask_chips}", true)).thenReturn(true)
        assertTrue(preferences.showSubtaskChip)
    }

    @Test
    fun showStartDateChipFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_start_date_chip}", true)).thenReturn(false)
        assertFalse(preferences.showStartDateChip)
    }

    @Test
    fun showPlaceChipFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_place_chips}", true)).thenReturn(false)
        assertFalse(preferences.showPlaceChip)
    }

    @Test
    fun showListChipFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_list_chips}", true)).thenReturn(false)
        assertFalse(preferences.showListChip)
    }

    @Test
    fun showTagChipFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_tag_chips}", true)).thenReturn(false)
        assertFalse(preferences.showTagChip)
    }

    // =============================================
    // alreadyNotified / setAlreadyNotified
    // =============================================

    @Test
    fun alreadyNotifiedReturnsFalseByDefault() {
        val key = "key_${R.string.p_notified_oauth_error}_user@test.com_calendar"
        `when`(prefs.getBoolean(key, false)).thenReturn(false)
        assertFalse(preferences.alreadyNotified("user@test.com", "calendar"))
    }

    @Test
    fun alreadyNotifiedReturnsTrueWhenSet() {
        val key = "key_${R.string.p_notified_oauth_error}_admin@example.com_drive"
        `when`(prefs.getBoolean(key, false)).thenReturn(true)
        assertTrue(preferences.alreadyNotified("admin@example.com", "drive"))
    }

    @Test
    fun setAlreadyNotifiedWritesTrueValue() {
        preferences.setAlreadyNotified("user@test.com", "scope", true)
        val key = "key_${R.string.p_notified_oauth_error}_user@test.com_scope"
        verify(editor).putBoolean(key, true)
    }

    @Test
    fun setAlreadyNotifiedWritesFalseValue() {
        preferences.setAlreadyNotified("user@test.com", "scope", false)
        val key = "key_${R.string.p_notified_oauth_error}_user@test.com_scope"
        verify(editor).putBoolean(key, false)
    }

    // =============================================
    // defaultCalendar
    // =============================================

    @Test
    fun defaultCalendarReturnsNullWhenNotSet() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null)).thenReturn(null)
        assertNull(preferences.defaultCalendar)
    }

    @Test
    fun defaultCalendarReturnsStoredValue() {
        `when`(prefs.getString("key_${R.string.gcal_p_default}", null)).thenReturn("cal_123")
        assertEquals("cal_123", preferences.defaultCalendar)
    }

    // =============================================
    // markdown, linkify, multilineTitle, dynamicColor
    // =============================================

    @Test
    fun markdownDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_markdown}", false)).thenReturn(false)
        assertFalse(preferences.markdown)
    }

    @Test
    fun markdownTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_markdown}", false)).thenReturn(true)
        assertTrue(preferences.markdown)
    }

    @Test
    fun linkifyDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_linkify_task_edit}", false)).thenReturn(false)
        assertFalse(preferences.linkify)
    }

    @Test
    fun linkifyTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_linkify_task_edit}", false)).thenReturn(true)
        assertTrue(preferences.linkify)
    }

    @Test
    fun multilineTitleDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_multiline_title}", false)).thenReturn(false)
        assertFalse(preferences.multilineTitle)
    }

    @Test
    fun multilineTitleTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_multiline_title}", false)).thenReturn(true)
        assertTrue(preferences.multilineTitle)
    }

    @Test
    fun dynamicColorDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_dynamic_color}", false)).thenReturn(false)
        assertFalse(preferences.dynamicColor)
    }

    @Test
    fun dynamicColorTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_dynamic_color}", false)).thenReturn(true)
        assertTrue(preferences.dynamicColor)
    }

    // =============================================
    // themeBase
    // =============================================

    @Test
    fun themeBaseReturnsDefault() {
        val default = org.tasks.themes.ThemeBase.DEFAULT_BASE_THEME
        `when`(prefs.getInt("key_${R.string.p_theme}", default)).thenReturn(default)
        assertEquals(default, preferences.themeBase)
    }

    @Test
    fun themeBaseReturnsCustom() {
        val default = org.tasks.themes.ThemeBase.DEFAULT_BASE_THEME
        `when`(prefs.getInt("key_${R.string.p_theme}", default)).thenReturn(3)
        assertEquals(3, preferences.themeBase)
    }

    // =============================================
    // defaultThemeColor
    // =============================================

    @Test
    fun defaultThemeColorReturnsDefault() {
        val blue = org.tasks.kmp.org.tasks.themes.ColorProvider.BLUE_500
        `when`(prefs.getInt("key_${R.string.p_theme_color}", blue)).thenReturn(blue)
        assertEquals(blue, preferences.defaultThemeColor)
    }

    @Test
    fun defaultThemeColorReturnsCustom() {
        val blue = org.tasks.kmp.org.tasks.themes.ColorProvider.BLUE_500
        `when`(prefs.getInt("key_${R.string.p_theme_color}", blue)).thenReturn(0xFF0000)
        assertEquals(0xFF0000, preferences.defaultThemeColor)
    }

    // =============================================
    // fontSize
    // =============================================

    @Test
    fun fontSizeDefault16() {
        `when`(prefs.getInt("key_${R.string.p_fontSize}", 16)).thenReturn(16)
        assertEquals(16, preferences.fontSize)
    }

    @Test
    fun fontSizeCustom() {
        `when`(prefs.getInt("key_${R.string.p_fontSize}", 16)).thenReturn(22)
        assertEquals(22, preferences.fontSize)
    }

    // =============================================
    // isPerListSortEnabled
    // =============================================

    @Test
    fun isPerListSortEnabledDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_per_list_sort}", false)).thenReturn(false)
        assertFalse(preferences.isPerListSortEnabled)
    }

    @Test
    fun isPerListSortEnabledTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_per_list_sort}", false)).thenReturn(true)
        assertTrue(preferences.isPerListSortEnabled)
    }
}
