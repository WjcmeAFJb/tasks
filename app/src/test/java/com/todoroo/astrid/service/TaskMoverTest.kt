package com.todoroo.astrid.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavCalendar
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters

class TaskMoverTest {
    @Mock lateinit var taskDao: TaskDao
    @Mock lateinit var caldavDao: CaldavDao
    @Mock lateinit var googleTaskDao: GoogleTaskDao
    @Mock lateinit var preferences: Preferences
    @Mock lateinit var refreshBroadcaster: RefreshBroadcaster
    @Mock lateinit var syncAdapters: SyncAdapters
    @Mock lateinit var vtodoCache: VtodoCache

    private lateinit var mover: TaskMover

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mover = TaskMover(taskDao, caldavDao, googleTaskDao, preferences, refreshBroadcaster, syncAdapters, vtodoCache)
    }

    @Test fun getSingleFilterReturnsNullForMultipleCalendars() = runTest {
        `when`(caldavDao.getCalendars(listOf(1L))).thenReturn(
            listOf(CaldavCalendar(uuid = "a"), CaldavCalendar(uuid = "b"))
        )
        assertNull(mover.getSingleFilter(listOf(1L)))
    }

    @Test fun getSingleFilterReturnsNullForEmpty() = runTest {
        `when`(caldavDao.getCalendars(listOf(1L))).thenReturn(emptyList())
        assertNull(mover.getSingleFilter(listOf(1L)))
    }

    @Test fun getSingleFilterReturnsNullWhenAccountNull() = runTest {
        val cal = CaldavCalendar(uuid = "cal-uuid", account = null)
        `when`(caldavDao.getCalendars(listOf(1L))).thenReturn(listOf(cal))
        assertNull(mover.getSingleFilter(listOf(1L)))
    }

    @Test fun getSingleFilterReturnsNullWhenAccountNotFound() = runTest {
        val cal = CaldavCalendar(uuid = "cal-uuid", account = "acct-uuid")
        `when`(caldavDao.getCalendars(listOf(1L))).thenReturn(listOf(cal))
        `when`(caldavDao.getAccountByUuid("acct-uuid")).thenReturn(null)
        assertNull(mover.getSingleFilter(listOf(1L)))
    }

    @Test fun moveByIdReturnsIfCalendarNotFound() = runTest {
        `when`(caldavDao.getCalendarById(99L)).thenReturn(null)
        mover.move(1L, 99L)
    }

    @Test fun moveByIdReturnsIfAccountNull() = runTest {
        `when`(caldavDao.getCalendarById(99L)).thenReturn(CaldavCalendar(account = null))
        mover.move(1L, 99L)
    }

    @Test fun moveByIdReturnsIfAccountNotFound() = runTest {
        `when`(caldavDao.getCalendarById(99L)).thenReturn(CaldavCalendar(account = "x"))
        `when`(caldavDao.getAccountByUuid("x")).thenReturn(null)
        mover.move(1L, 99L)
    }
}
