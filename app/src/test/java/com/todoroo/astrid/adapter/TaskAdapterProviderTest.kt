package com.todoroo.astrid.adapter

import com.todoroo.astrid.service.TaskMover
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.LocalBroadcastManager
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.preferences.Preferences
import org.tasks.preferences.TasksPreferences

class TaskAdapterProviderTest {
    private lateinit var preferences: Preferences
    private lateinit var tasksPreferences: TasksPreferences
    private lateinit var taskListMetadataDao: TaskListMetadataDao
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var taskMover: TaskMover
    private lateinit var provider: TaskAdapterProvider

    @Before
    fun setUp() {
        preferences = mock(Preferences::class.java)
        tasksPreferences = mock(TasksPreferences::class.java)
        taskListMetadataDao = mock(TaskListMetadataDao::class.java)
        taskDao = mock(TaskDao::class.java)
        taskSaver = mock(TaskSaver::class.java)
        googleTaskDao = mock(GoogleTaskDao::class.java)
        caldavDao = mock(CaldavDao::class.java)
        localBroadcastManager = mock(LocalBroadcastManager::class.java)
        taskMover = mock(TaskMover::class.java)

        provider = TaskAdapterProvider(
            preferences, tasksPreferences, taskListMetadataDao, taskDao, taskSaver,
            googleTaskDao, caldavDao, localBroadcastManager, taskMover
        )
    }

    @Test
    fun returnsDefaultTaskAdapterForGenericFilter() {
        `when`(preferences.isPerListSortEnabled).thenReturn(false)
        `when`(preferences.isAstridSort).thenReturn(false)
        `when`(preferences.isManualSort).thenReturn(false)
        `when`(preferences.addTasksToTop()).thenReturn(false)

        val filter = mock(Filter::class.java)
        val adapter = provider.createTaskAdapter(filter)

        assertTrue(adapter is TaskAdapter)
        assertFalse(adapter is AstridTaskAdapter)
        assertFalse(adapter is GoogleTaskManualSortAdapter)
        assertFalse(adapter is CaldavManualSortTaskAdapter)
    }

    @Test
    fun returnsGoogleTaskManualSortForGoogleTasksManualSort() {
        `when`(preferences.isPerListSortEnabled).thenReturn(false)
        `when`(preferences.isAstridSort).thenReturn(false)
        `when`(preferences.isManualSort).thenReturn(true)

        val calendar = CaldavCalendar(uuid = "test-uuid", name = "Test")
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        val filter = CaldavFilter(calendar = calendar, account = account)

        val adapter = provider.createTaskAdapter(filter)

        assertTrue(adapter is GoogleTaskManualSortAdapter)
    }

    @Test
    fun returnsCaldavManualSortForCaldavManualSort() {
        `when`(preferences.isPerListSortEnabled).thenReturn(false)
        `when`(preferences.isAstridSort).thenReturn(false)
        `when`(preferences.isManualSort).thenReturn(true)

        val calendar = CaldavCalendar(uuid = "test-uuid", name = "Test")
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        val filter = CaldavFilter(calendar = calendar, account = account)

        val adapter = provider.createTaskAdapter(filter)

        assertTrue(adapter is CaldavManualSortTaskAdapter)
    }

    @Test
    fun returnsDefaultAdapterWhenManualSortNotSupported() {
        `when`(preferences.isPerListSortEnabled).thenReturn(false)
        `when`(preferences.isAstridSort).thenReturn(false)
        `when`(preferences.isManualSort).thenReturn(true)
        `when`(preferences.addTasksToTop()).thenReturn(false)

        val filter = mock(Filter::class.java)
        `when`(filter.supportsManualSort()).thenReturn(false)

        val adapter = provider.createTaskAdapter(filter)

        assertTrue(adapter is TaskAdapter)
        assertFalse(adapter is GoogleTaskManualSortAdapter)
    }

    @Test
    fun returnsDefaultForNonAstridFilterEvenWhenAstridSortEnabled() {
        `when`(preferences.isPerListSortEnabled).thenReturn(false)
        `when`(preferences.isAstridSort).thenReturn(true)
        `when`(preferences.isManualSort).thenReturn(false)
        `when`(preferences.addTasksToTop()).thenReturn(false)

        // A generic filter (not AstridOrderingFilter) should get default adapter
        val filter = mock(Filter::class.java)
        val adapter = provider.createTaskAdapter(filter)

        assertTrue(adapter is TaskAdapter)
        assertFalse(adapter is AstridTaskAdapter)
    }

    @Test
    fun defaultAdapterRespectsAddTasksToTop() {
        `when`(preferences.isPerListSortEnabled).thenReturn(false)
        `when`(preferences.isAstridSort).thenReturn(false)
        `when`(preferences.isManualSort).thenReturn(false)
        `when`(preferences.addTasksToTop()).thenReturn(true)

        val filter = mock(Filter::class.java)
        val adapter = provider.createTaskAdapter(filter)

        assertTrue(adapter is TaskAdapter)
        verify(preferences).addTasksToTop()
    }

    @Test
    fun returnsDefaultAdapterForMicrosoftAccount() {
        `when`(preferences.isPerListSortEnabled).thenReturn(false)
        `when`(preferences.isAstridSort).thenReturn(false)
        `when`(preferences.isManualSort).thenReturn(true)
        `when`(preferences.addTasksToTop()).thenReturn(false)

        val calendar = CaldavCalendar(uuid = "test-uuid", name = "Test")
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        val filter = CaldavFilter(calendar = calendar, account = account)

        // Microsoft is neither Google Tasks nor iCalendar, so it should fall through
        val adapter = provider.createTaskAdapter(filter)

        assertTrue(adapter is TaskAdapter)
        assertFalse(adapter is GoogleTaskManualSortAdapter)
        assertFalse(adapter is CaldavManualSortTaskAdapter)
    }

    @Test
    fun returnsDefaultAdapterWhenManualSortDisabled() {
        `when`(preferences.isPerListSortEnabled).thenReturn(false)
        `when`(preferences.isAstridSort).thenReturn(false)
        `when`(preferences.isManualSort).thenReturn(false)
        `when`(preferences.addTasksToTop()).thenReturn(false)

        val calendar = CaldavCalendar(uuid = "test-uuid", name = "Test")
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        val filter = CaldavFilter(calendar = calendar, account = account)

        val adapter = provider.createTaskAdapter(filter)

        assertTrue(adapter is TaskAdapter)
        assertFalse(adapter is GoogleTaskManualSortAdapter)
    }
}
