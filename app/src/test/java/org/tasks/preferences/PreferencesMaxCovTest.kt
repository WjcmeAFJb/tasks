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
import org.mockito.Mock
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anySet
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.nullable
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.tasks.R
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Task
import org.tasks.time.DateTime
import java.util.concurrent.TimeUnit

class PreferencesMaxCovTest {

    @Mock lateinit var context: Context
    @Mock lateinit var prefs: SharedPreferences
    @Mock lateinit var editor: SharedPreferences.Editor
    @Mock lateinit var resources: Resources

    private lateinit var preferences: Preferences

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(context.getString(anyInt())).thenReturn("key_placeholder")
        `when`(context.getString(anyInt(), anyString(), anyString())).thenReturn("key_formatted")
        `when`(context.resources).thenReturn(resources)
        `when`(context.packageName).thenReturn("org.tasks")
        `when`(resources.getInteger(anyInt())).thenReturn(50000)
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

    // ===== getStringValue =====

    @Test
    fun getStringValueByKeyNull() {
        `when`(prefs.getString("some_key", null)).thenReturn(null)
        assertNull(preferences.getStringValue("some_key"))
    }

    @Test
    fun getStringValueByKeyReturns() {
        `when`(prefs.getString("some_key", null)).thenReturn("hello")
        assertEquals("hello", preferences.getStringValue("some_key"))
    }

    @Test
    fun getStringValueHandlesException() {
        `when`(prefs.getString("some_key", null)).thenThrow(ClassCastException("bad"))
        assertNull(preferences.getStringValue("some_key"))
    }

    // ===== getBoolean =====

    @Test
    fun getBooleanByKeyTrue() {
        `when`(prefs.getBoolean("key", false)).thenReturn(true)
        assertTrue(preferences.getBoolean("key", false))
    }

    @Test
    fun getBooleanClassCastReturnsDefault() {
        `when`(prefs.getBoolean("key", true)).thenThrow(ClassCastException("bad"))
        assertTrue(preferences.getBoolean("key", true))
    }

    // ===== setString =====

    @Test
    fun setStringByKey() {
        preferences.setString("my_key", "my_value")
        verify(editor).putString("my_key", "my_value")
        verify(editor).apply()
    }

    @Test
    fun setStringNull() {
        preferences.setString("my_key", null)
        verify(editor).putString("my_key", null)
    }

    // ===== setInt / getInt =====

    @Test
    fun setIntByKey() {
        preferences.setInt("my_key", 99)
        verify(editor).putInt("my_key", 99)
    }

    @Test
    fun getIntByKey() {
        `when`(prefs.getInt("key", 5)).thenReturn(10)
        assertEquals(10, preferences.getInt("key", 5))
    }

    // ===== setLong / getLong =====

    @Test
    fun setLongByKey() {
        preferences.setLong("my_key", 999L)
        verify(editor).putLong("my_key", 999L)
    }

    @Test
    fun getLongByKey() {
        `when`(prefs.getLong("key", 0L)).thenReturn(555L)
        assertEquals(555L, preferences.getLong("key", 0L))
    }

    // ===== setBoolean =====

    @Test
    fun setBooleanByKey() {
        preferences.setBoolean("my_key", false)
        verify(editor).putBoolean("my_key", false)
    }

    // ===== clear / remove =====

    @Test
    fun clearByKey() {
        preferences.clear("my_key")
        verify(editor).remove("my_key")
        verify(editor).apply()
    }

    // ===== removeByPrefix =====

    @Test
    fun removeByPrefixRemovesMatching() {
        `when`(prefs.all).thenReturn(mapOf("widget_1" to "v1", "widget_2" to "v2", "other" to "v3"))
        preferences.removeByPrefix("widget_")
        verify(editor).remove("widget_1")
        verify(editor).remove("widget_2")
        verify(editor, never()).remove("other")
        verify(editor).apply()
    }

    @Test
    fun removeByPrefixNoMatch() {
        `when`(prefs.all).thenReturn(mapOf("other" to "val"))
        preferences.removeByPrefix("widget_")
        verify(editor, never()).remove(anyString())
        verify(editor).apply()
    }

    // ===== setStringFromInteger =====

    @Test
    fun setStringFromInteger() {
        preferences.setStringFromInteger(600, 42)
        verify(editor).putString(anyString(), org.mockito.Mockito.eq("42"))
    }

    // ===== getIntegerFromString =====

    @Test
    fun getIntegerFromStringReturnsDefault() {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn(null)
        assertEquals(99, preferences.getIntegerFromString("key", 99))
    }

    @Test
    fun getIntegerFromStringParsesValue() {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn("42")
        assertEquals(42, preferences.getIntegerFromString("key", 99))
    }

    @Test
    fun getIntegerFromStringNonNumeric() {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn("not_a_number")
        assertEquals(99, preferences.getIntegerFromString("key", 99))
    }

    // ===== setStringSet / getStringSet =====

    @Test
    fun setStringSetByKey() {
        preferences.setStringSet("my_key", setOf("x", "y"))
        verify(editor).putStringSet("my_key", setOf("x", "y"))
    }

    // ===== isDefaultCalendarSet =====

    @Test
    fun isDefaultCalendarSetFalseForNull() {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn(null)
        assertFalse(preferences.isDefaultCalendarSet)
    }

    @Test
    fun isDefaultCalendarSetFalseForMinusOne() {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn("-1")
        assertFalse(preferences.isDefaultCalendarSet)
    }

    @Test
    fun isDefaultCalendarSetFalseForZero() {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn("0")
        assertFalse(preferences.isDefaultCalendarSet)
    }

    @Test
    fun isDefaultCalendarSetTrueForValid() {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn("my-calendar")
        assertTrue(preferences.isDefaultCalendarSet)
    }

    // ===== swipeToSnoozeIntervalMS =====

    @Test
    fun swipeToSnoozeIntervalMSReturnsMillis() {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn("5")
        assertEquals(TimeUnit.MINUTES.toMillis(5), preferences.swipeToSnoozeIntervalMS())
    }

    @Test
    fun swipeToSnoozeIntervalMSZeroOnNull() {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn(null)
        assertEquals(0L, preferences.swipeToSnoozeIntervalMS())
    }

    // ===== alreadyNotified =====

    @Test
    fun alreadyNotifiedFalse() {
        `when`(prefs.getBoolean(anyString(), anyBoolean())).thenReturn(false)
        assertFalse(preferences.alreadyNotified("acct", "scope"))
    }

    @Test
    fun alreadyNotifiedTrue() {
        `when`(prefs.getBoolean(anyString(), anyBoolean())).thenReturn(true)
        assertTrue(preferences.alreadyNotified("acct", "scope"))
    }

    @Test
    fun setAlreadyNotified() {
        preferences.setAlreadyNotified("acct", "scope", true)
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(true))
    }

    // ===== getPrefs =====

    @Test
    fun getPrefsReturnsFiltered() {
        `when`(prefs.all).thenReturn(mapOf("k1" to "str", "k2" to 42, "k3" to "str2"))
        val result = preferences.getPrefs(String::class.java)
        assertEquals(2, result.size)
    }

    @Test
    fun getPrefsEmpty() {
        `when`(prefs.all).thenReturn(mapOf("k1" to 42))
        assertTrue(preferences.getPrefs(String::class.java).isEmpty())
    }

    // ===== defaultAlarms =====

    @Test
    fun defaultAlarmsInvalidJson() = runTest {
        `when`(prefs.getStringSet(anyString(), anySet())).thenReturn(setOf("not_valid_json"))
        val alarms = preferences.defaultAlarms()
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun defaultAlarmsValid() = runTest {
        val alarm = Alarm(time = 0, type = Alarm.TYPE_REL_START)
        val json = Json.encodeToString(alarm)
        `when`(prefs.getStringSet(anyString(), anySet())).thenReturn(setOf(json))
        val alarms = preferences.defaultAlarms()
        assertEquals(1, alarms.size)
        assertEquals(Alarm.TYPE_REL_START, alarms[0].type)
    }

    // ===== dateShortcuts =====

    @Test
    fun dateShortcutMorningFallback() {
        `when`(prefs.getInt(anyString(), org.mockito.Mockito.eq(-1))).thenReturn(-1)
        assertEquals(50000, preferences.dateShortcutMorning)
    }

    @Test
    fun dateShortcutMorningStored() {
        `when`(prefs.getInt(anyString(), org.mockito.Mockito.eq(-1))).thenReturn(30000)
        assertEquals(30000, preferences.dateShortcutMorning)
    }

    @Test
    fun dateShortcutTooLargeFallback() {
        `when`(prefs.getInt(anyString(), org.mockito.Mockito.eq(-1))).thenReturn(DateTime.MAX_MILLIS_PER_DAY + 1)
        assertEquals(50000, preferences.dateShortcutMorning)
    }

    // ===== purchases =====

    @Test
    fun purchasesEmptyOnException() {
        `when`(prefs.getStringSet(anyString(), anySet())).thenThrow(RuntimeException("oops"))
        assertTrue(preferences.purchases.isEmpty())
    }

    // ===== defaultPriority =====

    @Test
    fun defaultPriorityDefault() = runTest {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn(null)
        assertEquals(Task.Priority.LOW, preferences.defaultPriority())
    }

    // ===== defaultRingMode =====

    @Test
    fun defaultRingModeDefault() = runTest {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn(null)
        assertEquals(0, preferences.defaultRingMode())
    }

    // ===== defaultLocationReminder =====

    @Test
    fun defaultLocationReminderDefault() = runTest {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn(null)
        assertEquals(1, preferences.defaultLocationReminder())
    }

    // ===== locationUpdateIntervalMinutes =====

    @Test
    fun locationUpdateIntervalMinutesDefault() = runTest {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn(null)
        assertEquals(15, preferences.locationUpdateIntervalMinutes())
    }

    // ===== defaultRandomHours =====

    @Test
    fun defaultRandomHoursDefault() = runTest {
        `when`(prefs.getString(anyString(), nullable(String::class.java))).thenReturn(null)
        assertEquals(0, preferences.defaultRandomHours())
    }

    // ===== setDefaultAlarms =====

    @Test
    fun setDefaultAlarms() {
        val alarms = listOf(Alarm(time = 0, type = Alarm.TYPE_REL_START))
        preferences.setDefaultAlarms(alarms)
        verify(editor).putStringSet(anyString(), anySet())
    }

    // ===== sortMode set =====

    @Test
    fun sortModeSet() {
        preferences.sortMode = 5
        verify(editor).putInt(anyString(), org.mockito.Mockito.eq(5))
    }

    // ===== showHidden set =====

    @Test
    fun showHiddenSet() {
        preferences.showHidden = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== showCompleted set =====

    @Test
    fun showCompletedSet() {
        preferences.showCompleted = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== isManualSort =====

    @Test
    fun isManualSortSet() {
        preferences.isManualSort = true
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(true))
    }

    // ===== isAstridSort =====

    @Test
    fun isAstridSortSet() {
        preferences.isAstridSort = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== sortAscending =====

    @Test
    fun sortAscendingSet() {
        preferences.sortAscending = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== groupAscending =====

    @Test
    fun groupAscendingSet() {
        preferences.groupAscending = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== completedAscending =====

    @Test
    fun completedAscendingSet() {
        preferences.completedAscending = true
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(true))
    }

    // ===== subtaskAscending =====

    @Test
    fun subtaskAscendingSet() {
        preferences.subtaskAscending = true
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(true))
    }

    // ===== alwaysDisplayFullDate =====

    @Test
    fun alwaysDisplayFullDateSet() {
        preferences.alwaysDisplayFullDate = true
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(true))
    }

    // ===== completedTasksAtBottom =====

    @Test
    fun completedTasksAtBottomSet() {
        preferences.completedTasksAtBottom = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== setCurrentVersion / installVersion / installDate / deviceInstallVersion =====

    @Test
    fun setCurrentVersion() {
        preferences.setCurrentVersion(12345)
        verify(editor).putInt(anyString(), org.mockito.Mockito.eq(12345))
    }

    @Test
    fun installVersionSet() {
        preferences.installVersion = 100
        verify(editor).putInt(anyString(), org.mockito.Mockito.eq(100))
    }

    @Test
    fun installDateSet() {
        preferences.installDate = 999L
        verify(editor).putLong(anyString(), org.mockito.Mockito.eq(999L))
    }

    @Test
    fun deviceInstallVersionSet() {
        preferences.deviceInstallVersion = 200
        verify(editor).putInt(anyString(), org.mockito.Mockito.eq(200))
    }

    // ===== lastSync =====

    @Test
    fun lastSyncSet() {
        preferences.lastSync = 456L
        verify(editor).putLong(anyString(), org.mockito.Mockito.eq(456L))
    }

    // ===== warnNotificationsDisabled =====

    @Test
    fun warnNotificationsDisabledSet() {
        preferences.warnNotificationsDisabled = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== warnAlarmsDisabled =====

    @Test
    fun warnAlarmsDisabledSet() {
        preferences.warnAlarmsDisabled = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== warnMicrosoft =====

    @Test
    fun warnMicrosoftSet() {
        preferences.warnMicrosoft = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== warnGoogleTasks =====

    @Test
    fun warnGoogleTasksSet() {
        preferences.warnGoogleTasks = false
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(false))
    }

    // ===== shownBeastModeHint =====

    @Test
    fun shownBeastModeHintSet() {
        preferences.shownBeastModeHint = true
        verify(editor).putBoolean(anyString(), org.mockito.Mockito.eq(true))
    }

    // ===== lastReviewRequest =====

    @Test
    fun lastReviewRequestSet() {
        preferences.lastReviewRequest = 12345L
        verify(editor).putLong(anyString(), org.mockito.Mockito.eq(12345L))
    }

    // ===== lastSubscribeRequest =====

    @Test
    fun lastSubscribeRequestSet() {
        preferences.lastSubscribeRequest = 54321L
        verify(editor).putLong(anyString(), org.mockito.Mockito.eq(54321L))
    }

    // ===== groupMode set =====

    @Test
    fun groupModeSet() {
        preferences.groupMode = 3
        verify(editor).putInt(anyString(), org.mockito.Mockito.eq(3))
    }

    // ===== completedMode set =====

    @Test
    fun completedModeSet() {
        preferences.completedMode = 2
        verify(editor).putInt(anyString(), org.mockito.Mockito.eq(2))
    }

    // ===== subtaskMode set =====

    @Test
    fun subtaskModeSet() {
        preferences.subtaskMode = 4
        verify(editor).putInt(anyString(), org.mockito.Mockito.eq(4))
    }
}
