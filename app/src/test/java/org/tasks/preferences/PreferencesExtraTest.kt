package org.tasks.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.tasks.R
import org.tasks.themes.ThemeBase

class PreferencesExtraTest {

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
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.putLong(anyString(), anyLong())).thenReturn(editor)
        `when`(editor.remove(anyString())).thenReturn(editor)
        `when`(editor.clear()).thenReturn(editor)
        preferences = Preferences(context, "test_prefs")
    }

    // --- remove ---

    @Test
    fun removeByResId() {
        preferences.remove(R.string.p_fontSize)

        verify(editor).remove("key_${R.string.p_fontSize}")
        verify(editor).apply()
    }

    // --- clear(key) ---

    @Test
    fun clearByKey() {
        preferences.clear("some_key")

        verify(editor).remove("some_key")
        verify(editor).apply()
    }

    // --- markdown ---

    @Test
    fun markdownDefaultIsFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_markdown}", false)).thenReturn(false)
        assertFalse(preferences.markdown)
    }

    @Test
    fun markdownReturnsTrueWhenEnabled() {
        `when`(prefs.getBoolean("key_${R.string.p_markdown}", false)).thenReturn(true)
        assertTrue(preferences.markdown)
    }

    // --- sortMode setter ---

    @Test
    fun sortModeSetValue() {
        preferences.sortMode = com.todoroo.astrid.core.SortHelper.SORT_IMPORTANCE
        verify(editor).putInt("key_${R.string.p_sort_mode}", com.todoroo.astrid.core.SortHelper.SORT_IMPORTANCE)
        verify(editor).apply()
    }

    @Test
    fun sortModeGetCustom() {
        `when`(prefs.getInt("key_${R.string.p_sort_mode}", com.todoroo.astrid.core.SortHelper.SORT_DUE))
            .thenReturn(com.todoroo.astrid.core.SortHelper.SORT_ALPHA)
        assertEquals(com.todoroo.astrid.core.SortHelper.SORT_ALPHA, preferences.sortMode)
    }

    // --- groupMode setter ---

    @Test
    fun groupModeSet() {
        preferences.groupMode = com.todoroo.astrid.core.SortHelper.SORT_CREATED
        verify(editor).putInt("key_${R.string.p_group_mode}", com.todoroo.astrid.core.SortHelper.SORT_CREATED)
    }

    // --- completedMode setter ---

    @Test
    fun completedModeSet() {
        preferences.completedMode = com.todoroo.astrid.core.SortHelper.SORT_ALPHA
        verify(editor).putInt("key_${R.string.p_completed_mode}", com.todoroo.astrid.core.SortHelper.SORT_ALPHA)
    }

    // --- subtaskMode setter ---

    @Test
    fun subtaskModeSet() {
        preferences.subtaskMode = com.todoroo.astrid.core.SortHelper.SORT_DUE
        verify(editor).putInt("key_${R.string.p_subtask_mode}", com.todoroo.astrid.core.SortHelper.SORT_DUE)
    }

    // --- showHidden setter ---

    @Test
    fun showHiddenSetFalse() {
        preferences.showHidden = false
        verify(editor).putBoolean("key_${R.string.p_show_hidden_tasks}", false)
    }

    // --- showCompleted setter ---

    @Test
    fun showCompletedSetFalse() {
        preferences.showCompleted = false
        verify(editor).putBoolean("key_${R.string.p_show_completed_tasks}", false)
    }

    // --- completedTasksAtBottom setter ---

    @Test
    fun completedTasksAtBottomSetFalse() {
        preferences.completedTasksAtBottom = false
        verify(editor).putBoolean("key_${R.string.p_completed_tasks_at_bottom}", false)
    }

    // --- alwaysDisplayFullDate setter ---

    @Test
    fun alwaysDisplayFullDateSetTrue() {
        preferences.alwaysDisplayFullDate = true
        verify(editor).putBoolean("key_${R.string.p_always_display_full_date}", true)
    }

    // --- sortAscending setter ---

    @Test
    fun sortAscendingSetFalse() {
        preferences.sortAscending = false
        verify(editor).putBoolean("key_${R.string.p_sort_ascending}", false)
    }

    // --- groupAscending setter ---

    @Test
    fun groupAscendingSetFalse() {
        preferences.groupAscending = false
        verify(editor).putBoolean("key_${R.string.p_group_ascending}", false)
    }

    // --- completedAscending setter ---

    @Test
    fun completedAscendingSetTrue() {
        preferences.completedAscending = true
        verify(editor).putBoolean("key_${R.string.p_completed_ascending}", true)
    }

    // --- subtaskAscending setter ---

    @Test
    fun subtaskAscendingSetTrue() {
        preferences.subtaskAscending = true
        verify(editor).putBoolean("key_${R.string.p_subtask_ascending}", true)
    }

    // --- isManualSort setter ---

    @Test
    fun isManualSortSetTrue() {
        preferences.isManualSort = true
        verify(editor).putBoolean("key_${R.string.p_manual_sort}", true)
    }

    // --- isAstridSort setter ---

    @Test
    fun isAstridSortSet() {
        preferences.isAstridSort = true
        verify(editor).putBoolean("key_${R.string.p_astrid_sort}", true)
    }

    // --- themeBase ---

    @Test
    fun themeBaseDefault() {
        `when`(prefs.getInt("key_${R.string.p_theme}", ThemeBase.DEFAULT_BASE_THEME))
            .thenReturn(ThemeBase.DEFAULT_BASE_THEME)
        assertEquals(ThemeBase.DEFAULT_BASE_THEME, preferences.themeBase)
    }

    @Test
    fun themeBaseCustom() {
        `when`(prefs.getInt("key_${R.string.p_theme}", ThemeBase.DEFAULT_BASE_THEME)).thenReturn(2)
        assertEquals(2, preferences.themeBase)
    }

    // --- defaultThemeColor ---

    @Test
    fun defaultThemeColorReturnsPref() {
        val blue = org.tasks.kmp.org.tasks.themes.ColorProvider.BLUE_500
        `when`(prefs.getInt("key_${R.string.p_theme_color}", blue)).thenReturn(blue)
        assertEquals(blue, preferences.defaultThemeColor)
    }

    // --- chip visibility preferences ---

    @Test
    fun showSubtaskChipDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_subtask_chips}", true)).thenReturn(true)
        assertTrue(preferences.showSubtaskChip)
    }

    @Test
    fun showStartDateChipDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_start_date_chip}", true)).thenReturn(true)
        assertTrue(preferences.showStartDateChip)
    }

    @Test
    fun showPlaceChipDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_place_chips}", true)).thenReturn(true)
        assertTrue(preferences.showPlaceChip)
    }

    @Test
    fun showListChipDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_list_chips}", true)).thenReturn(true)
        assertTrue(preferences.showListChip)
    }

    @Test
    fun showTagChipDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_tag_chips}", true)).thenReturn(true)
        assertTrue(preferences.showTagChip)
    }

    // --- lastSetVersion / setCurrentVersion ---

    @Test
    fun lastSetVersionDefaultIsZero() {
        `when`(prefs.getInt("key_${R.string.p_current_version}", 0)).thenReturn(0)
        assertEquals(0, preferences.lastSetVersion)
    }

    @Test
    fun setCurrentVersion() {
        preferences.setCurrentVersion(123)
        verify(editor).putInt("key_${R.string.p_current_version}", 123)
    }

    // --- installVersion ---

    @Test
    fun installVersionGetAndSet() {
        `when`(prefs.getInt("key_${R.string.p_install_version}", 0)).thenReturn(42)
        assertEquals(42, preferences.installVersion)

        preferences.installVersion = 99
        verify(editor).putInt("key_${R.string.p_install_version}", 99)
    }

    // --- installDate ---

    @Test
    fun installDateGetAndSet() {
        `when`(prefs.getLong("key_${R.string.p_install_date}", 0L)).thenReturn(1000L)
        assertEquals(1000L, preferences.installDate)

        preferences.installDate = 2000L
        verify(editor).putLong("key_${R.string.p_install_date}", 2000L)
    }

    // --- deviceInstallVersion ---

    @Test
    fun deviceInstallVersionGetAndSet() {
        `when`(prefs.getInt("key_${R.string.p_device_install_version}", 0)).thenReturn(5)
        assertEquals(5, preferences.deviceInstallVersion)

        preferences.deviceInstallVersion = 10
        verify(editor).putInt("key_${R.string.p_device_install_version}", 10)
    }

    // --- lastSync ---

    @Test
    fun lastSyncGetAndSet() {
        `when`(prefs.getLong("key_${R.string.p_last_sync}", 0L)).thenReturn(5000L)
        assertEquals(5000L, preferences.lastSync)

        preferences.lastSync = 9000L
        verify(editor).putLong("key_${R.string.p_last_sync}", 9000L)
    }

    // --- lastReviewRequest ---

    @Test
    fun lastReviewRequestGetAndSet() {
        `when`(prefs.getLong("key_${R.string.p_last_review_request}", 0L)).thenReturn(0L)
        assertEquals(0L, preferences.lastReviewRequest)

        preferences.lastReviewRequest = 42000L
        verify(editor).putLong("key_${R.string.p_last_review_request}", 42000L)
    }

    // --- lastSubscribeRequest ---

    @Test
    fun lastSubscribeRequestGetAndSet() {
        `when`(prefs.getLong("key_${R.string.p_last_subscribe_request}", 0L)).thenReturn(100L)
        assertEquals(100L, preferences.lastSubscribeRequest)

        preferences.lastSubscribeRequest = 200L
        verify(editor).putLong("key_${R.string.p_last_subscribe_request}", 200L)
    }

    // --- warnNotificationsDisabled ---

    @Test
    fun warnNotificationsDisabledGetAndSet() {
        `when`(prefs.getBoolean("key_${R.string.p_warn_notifications_disabled}", true)).thenReturn(true)
        assertTrue(preferences.warnNotificationsDisabled)

        preferences.warnNotificationsDisabled = false
        verify(editor).putBoolean("key_${R.string.p_warn_notifications_disabled}", false)
    }

    // --- warnAlarmsDisabled ---

    @Test
    fun warnAlarmsDisabledGetAndSet() {
        `when`(prefs.getBoolean("key_${R.string.p_warn_alarms_disabled}", true)).thenReturn(true)
        assertTrue(preferences.warnAlarmsDisabled)

        preferences.warnAlarmsDisabled = false
        verify(editor).putBoolean("key_${R.string.p_warn_alarms_disabled}", false)
    }

    // --- warnMicrosoft ---

    @Test
    fun warnMicrosoftGetAndSet() {
        `when`(prefs.getBoolean("key_${R.string.p_warn_microsoft}", true)).thenReturn(true)
        assertTrue(preferences.warnMicrosoft)

        preferences.warnMicrosoft = false
        verify(editor).putBoolean("key_${R.string.p_warn_microsoft}", false)
    }

    // --- warnGoogleTasks ---

    @Test
    fun warnGoogleTasksGetAndSet() {
        `when`(prefs.getBoolean("key_${R.string.p_warn_google_tasks}", true)).thenReturn(true)
        assertTrue(preferences.warnGoogleTasks)

        preferences.warnGoogleTasks = false
        verify(editor).putBoolean("key_${R.string.p_warn_google_tasks}", false)
    }

    // --- shownBeastModeHint ---

    @Test
    fun shownBeastModeHintGetAndSet() {
        `when`(prefs.getBoolean("key_${R.string.p_shown_beast_mode_hint}", false)).thenReturn(false)
        assertFalse(preferences.shownBeastModeHint)

        preferences.shownBeastModeHint = true
        verify(editor).putBoolean("key_${R.string.p_shown_beast_mode_hint}", true)
    }

    // --- linkify ---

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

    // --- multilineTitle ---

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

    // --- dynamicColor ---

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

    // --- bundleNotifications ---

    @Test
    fun bundleNotificationsDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_bundle_notifications}", true)).thenReturn(true)
        assertTrue(preferences.bundleNotifications())
    }

    @Test
    fun bundleNotificationsFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_bundle_notifications}", true)).thenReturn(false)
        assertFalse(preferences.bundleNotifications())
    }

    // --- isPerListSortEnabled ---

    @Test
    fun isPerListSortEnabledDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_per_list_sort}", false)).thenReturn(false)
        assertFalse(preferences.isPerListSortEnabled)
    }

    // --- addTasksToTop ---

    @Test
    fun addTasksToTopDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_add_to_top}", true)).thenReturn(true)
        assertTrue(preferences.addTasksToTop())
    }

    // --- backButtonSavesTask ---

    @Test
    fun backButtonSavesTaskDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_back_button_saves_task}", false)).thenReturn(false)
        assertFalse(preferences.backButtonSavesTask())
    }

    // --- androidBackupServiceEnabled ---

    @Test
    fun androidBackupServiceEnabledDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_backups_android_backup_enabled}", true)).thenReturn(true)
        assertTrue(preferences.androidBackupServiceEnabled())
    }

    // --- showBackupWarnings ---

    @Test
    fun showBackupWarningsDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_backups_ignore_warnings}", false)).thenReturn(false)
        assertTrue(preferences.showBackupWarnings())
    }

    @Test
    fun showBackupWarningsFalseWhenIgnored() {
        `when`(prefs.getBoolean("key_${R.string.p_backups_ignore_warnings}", false)).thenReturn(true)
        assertFalse(preferences.showBackupWarnings())
    }

    // --- isTrackingEnabled ---

    @Test
    fun isTrackingEnabledDefaultTrue() {
        `when`(prefs.getBoolean("key_${R.string.p_collect_statistics}", true)).thenReturn(true)
        assertTrue(preferences.isTrackingEnabled)
    }

    // --- showEditScreenWithoutUnlock ---

    @Test
    fun showEditScreenWithoutUnlockDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_show_edit_screen_without_unlock}", false)).thenReturn(false)
        assertFalse(preferences.showEditScreenWithoutUnlock)
    }

    // --- setString ---

    @Test
    fun setStringByKey() {
        preferences.setString("myKey", "myValue")
        verify(editor).putString("myKey", "myValue")
        verify(editor).apply()
    }

    @Test
    fun setStringByResId() {
        preferences.setString(R.string.p_fontSize, "18")
        verify(editor).putString("key_${R.string.p_fontSize}", "18")
        verify(editor).apply()
    }

    // --- setStringFromInteger ---

    @Test
    fun setStringFromInteger() {
        preferences.setStringFromInteger(R.string.p_fontSize, 24)
        verify(editor).putString("key_${R.string.p_fontSize}", "24")
        verify(editor).apply()
    }

    // --- setBoolean ---

    @Test
    fun setBooleanByKey() {
        preferences.setBoolean("myKey", true)
        verify(editor).putBoolean("myKey", true)
        verify(editor).apply()
    }

    // --- setInt ---

    @Test
    fun setIntByKey() {
        preferences.setInt("myKey", 42)
        verify(editor).putInt("myKey", 42)
        verify(editor).apply()
    }

    // --- setLong ---

    @Test
    fun setLongByKey() {
        preferences.setLong("myKey", 999L)
        verify(editor).putLong("myKey", 999L)
        verify(editor).apply()
    }

    // --- getLong ---

    @Test
    fun getLongByKey() {
        `when`(prefs.getLong("myKey", 0L)).thenReturn(123L)
        assertEquals(123L, preferences.getLong("myKey", 0L))
    }

    @Test
    fun getLongByResId() {
        `when`(prefs.getLong("key_${R.string.p_last_sync}", 0L)).thenReturn(500L)
        assertEquals(500L, preferences.getLong(R.string.p_last_sync, 0L))
    }

    // --- getInt ---

    @Test
    fun getIntByKey() {
        `when`(prefs.getInt("myKey", 0)).thenReturn(7)
        assertEquals(7, preferences.getInt("myKey", 0))
    }

    // --- defaultPriority ---

    @Test
    fun defaultPriorityReturnsDefault() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_importance_key}", null)).thenReturn(null)
        assertEquals(
            org.tasks.data.entity.Task.Priority.LOW,
            preferences.defaultPriority()
        )
    }

    @Test
    fun defaultPriorityReturnsCustom() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_importance_key}", null)).thenReturn("0")
        assertEquals(0, preferences.defaultPriority())
    }

    // --- defaultRingMode ---

    @Test
    fun defaultRingModeDefault() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_reminders_mode_key}", null)).thenReturn(null)
        assertEquals(0, preferences.defaultRingMode())
    }

    @Test
    fun defaultRingModeCustom() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_reminders_mode_key}", null)).thenReturn("2")
        assertEquals(2, preferences.defaultRingMode())
    }

    // --- defaultLocationReminder ---

    @Test
    fun defaultLocationReminderDefault() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_location_reminder_key}", null)).thenReturn(null)
        assertEquals(1, preferences.defaultLocationReminder())
    }

    @Test
    fun defaultLocationReminderCustom() = runTest {
        `when`(prefs.getString("key_${R.string.p_default_location_reminder_key}", null)).thenReturn("3")
        assertEquals(3, preferences.defaultLocationReminder())
    }

    // --- locationUpdateIntervalMinutes ---

    @Test
    fun locationUpdateIntervalDefault() = runTest {
        `when`(prefs.getString("key_${R.string.p_location_update_interval}", null)).thenReturn(null)
        assertEquals(15, preferences.locationUpdateIntervalMinutes())
    }

    @Test
    fun locationUpdateIntervalCustom() = runTest {
        `when`(prefs.getString("key_${R.string.p_location_update_interval}", null)).thenReturn("30")
        assertEquals(30, preferences.locationUpdateIntervalMinutes())
    }

    // --- defaultRandomHours ---

    @Test
    fun defaultRandomHoursDefault() = runTest {
        `when`(prefs.getString("key_${R.string.p_rmd_default_random_hours}", null)).thenReturn(null)
        assertEquals(0, preferences.defaultRandomHours())
    }

    @Test
    fun defaultRandomHoursCustom() = runTest {
        `when`(prefs.getString("key_${R.string.p_rmd_default_random_hours}", null)).thenReturn("6")
        assertEquals(6, preferences.defaultRandomHours())
    }

    // --- isDefaultDueTimeEnabled ---

    @Test
    fun isDefaultDueTimeEnabledDefaultTrue() = runTest {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_time_enabled}", true)).thenReturn(true)
        assertTrue(preferences.isDefaultDueTimeEnabled())
    }

    @Test
    fun isDefaultDueTimeEnabledFalse() = runTest {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_time_enabled}", true)).thenReturn(false)
        assertFalse(preferences.isDefaultDueTimeEnabled())
    }

    // --- alreadyNotified / setAlreadyNotified ---

    @Test
    fun alreadyNotifiedDefaultFalse() {
        val key = "key_${R.string.p_notified_oauth_error}_account@test.com_scope1"
        `when`(prefs.getBoolean(key, false)).thenReturn(false)
        assertFalse(preferences.alreadyNotified("account@test.com", "scope1"))
    }

    @Test
    fun setAlreadyNotified() {
        preferences.setAlreadyNotified("account@test.com", "scope1", true)
        val key = "key_${R.string.p_notified_oauth_error}_account@test.com_scope1"
        verify(editor).putBoolean(key, true)
    }

    // --- useSwipeToSnooze ---

    @Test
    fun useSwipeToSnoozeDefaultFalse() {
        `when`(prefs.getBoolean("key_${R.string.p_rmd_swipe_to_snooze_enabled}", false)).thenReturn(false)
        assertFalse(preferences.useSwipeToSnooze())
    }

    // --- getPrefs ---

    @Test
    fun getPrefsFiltersByType() {
        val allPrefs = mapOf<String, Any>(
            "key1" to "value1",
            "key2" to 42,
            "key3" to "value3",
            "key4" to true,
        )
        `when`(prefs.all).thenReturn(allPrefs)

        val stringPrefs = preferences.getPrefs(String::class.java)
        assertEquals(2, stringPrefs.size)
        assertEquals("value1", stringPrefs["key1"])
        assertEquals("value3", stringPrefs["key3"])
    }

    // --- setStringSet ---

    @Test
    fun setStringSetByKey() {
        val set = setOf("a", "b", "c")
        `when`(editor.putStringSet(anyString(), org.mockito.ArgumentMatchers.anySet())).thenReturn(editor)

        preferences.setStringSet("myKey", set)
        verify(editor).putStringSet("myKey", set)
        verify(editor).apply()
    }

    @Test
    fun setStringSetByResId() {
        val set = setOf("x", "y")
        `when`(editor.putStringSet(anyString(), org.mockito.ArgumentMatchers.anySet())).thenReturn(editor)

        preferences.setStringSet(R.string.p_purchases, set)
        verify(editor).putStringSet("key_${R.string.p_purchases}", set)
    }
}
