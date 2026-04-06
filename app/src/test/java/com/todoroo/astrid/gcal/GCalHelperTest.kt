package com.todoroo.astrid.gcal

import android.content.ContentResolver
import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.calendars.CalendarEventProvider
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences

class GCalHelperTest {

    private lateinit var context: Context
    private lateinit var taskDao: TaskDao
    private lateinit var preferences: Preferences
    private lateinit var permissionChecker: PermissionChecker
    private lateinit var calendarEventProvider: CalendarEventProvider
    private lateinit var contentResolver: ContentResolver
    private lateinit var helper: GCalHelper

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        taskDao = mock(TaskDao::class.java)
        preferences = mock(Preferences::class.java)
        permissionChecker = mock(PermissionChecker::class.java)
        calendarEventProvider = mock(CalendarEventProvider::class.java)
        contentResolver = mock(ContentResolver::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        helper = GCalHelper(context, taskDao, preferences, permissionChecker, calendarEventProvider)
    }

    // --- createTaskEventIfEnabled ---

    @Test
    fun createTaskEventIfEnabledWithNoDueDate() = runTest {
        val task = Task(id = 1, dueDate = 0)
        helper.createTaskEventIfEnabled(task)
        // Should return early without checking preferences
        verify(preferences, never()).isDefaultCalendarSet
    }

    @Test
    fun createTaskEventIfEnabledWhenCalendarNotSet() = runTest {
        val task = Task(id = 1, dueDate = 1234567890000L)
        `when`(preferences.isDefaultCalendarSet).thenReturn(false)
        helper.createTaskEventIfEnabled(task)
        verify(permissionChecker, never()).canAccessCalendars()
    }

    @Test
    fun createTaskEventIfEnabledWhenNoCalendarPermission() = runTest {
        val task = Task(id = 1, dueDate = 1234567890000L)
        `when`(preferences.isDefaultCalendarSet).thenReturn(true)
        `when`(permissionChecker.canAccessCalendars()).thenReturn(false)
        helper.createTaskEventIfEnabled(task)
        // Should return null, so calendarURI stays null
        org.junit.Assert.assertNull(task.calendarURI)
    }

    // --- updateEvent ---

    @Test
    fun updateEventWithNullUri() {
        val task = Task(id = 1, calendarURI = null)
        helper.updateEvent(task)
        verify(permissionChecker, never()).canAccessCalendars()
    }

    @Test
    fun updateEventWithBlankUri() {
        val task = Task(id = 1, calendarURI = "")
        helper.updateEvent(task)
        verify(permissionChecker, never()).canAccessCalendars()
    }

    @Test
    fun updateEventWithWhitespaceOnlyUri() {
        val task = Task(id = 1, calendarURI = "   ")
        helper.updateEvent(task)
        verify(permissionChecker, never()).canAccessCalendars()
    }

    @Test
    fun updateEventWithoutPermission() {
        val task = Task(id = 1, calendarURI = "content://cal/events/1")
        `when`(permissionChecker.canAccessCalendars()).thenReturn(false)
        helper.updateEvent(task)
        verify(contentResolver, never()).update(any(), any(), any(), any())
    }

    // --- rescheduleRepeatingTask ---

    @Test
    fun rescheduleRepeatingTaskWithNullUri() = runTest {
        val task = Task(id = 1, calendarURI = null)
        `when`(taskDao.fetch(1)).thenReturn(null)
        helper.rescheduleRepeatingTask(task)
        verify(calendarEventProvider, never()).getEvent(any())
    }

    @Test
    fun rescheduleRepeatingTaskWithBlankUri() = runTest {
        val task = Task(id = 1, calendarURI = "")
        `when`(taskDao.fetch(1)).thenReturn(Task(id = 1, calendarURI = ""))
        helper.rescheduleRepeatingTask(task)
        verify(calendarEventProvider, never()).getEvent(any())
    }
}
