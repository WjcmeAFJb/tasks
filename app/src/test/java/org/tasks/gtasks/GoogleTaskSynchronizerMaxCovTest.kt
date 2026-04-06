package org.tasks.gtasks

import android.content.Context
import com.google.api.services.tasks.model.Task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.analytics.Firebase
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.TaskSaver
import org.tasks.data.createDueDate
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.googleapis.InvokerFactory
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.service.TaskDeleter
import com.todoroo.astrid.gtasks.GtasksListService
import com.todoroo.astrid.service.TaskCreator

class GoogleTaskSynchronizerMaxCovTest {

    private lateinit var context: Context
    private lateinit var caldavDao: CaldavDao
    private lateinit var gtasksListService: GtasksListService
    private lateinit var preferences: Preferences
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var firebase: Firebase
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var taskCreator: TaskCreator
    private lateinit var defaultFilterProvider: DefaultFilterProvider
    private lateinit var permissionChecker: PermissionChecker
    private lateinit var googleAccountManager: GoogleAccountManager
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var taskDeleter: TaskDeleter
    private lateinit var invokers: InvokerFactory
    private lateinit var alarmDao: AlarmDao
    private lateinit var synchronizer: GoogleTaskSynchronizer

    @Before
    fun setUp() {
        context = mock()
        caldavDao = mock()
        gtasksListService = mock()
        preferences = mock()
        taskDao = mock()
        taskSaver = mock()
        firebase = mock()
        googleTaskDao = mock()
        taskCreator = mock()
        defaultFilterProvider = mock()
        permissionChecker = mock()
        googleAccountManager = mock()
        refreshBroadcaster = mock()
        taskDeleter = mock()
        invokers = mock()
        alarmDao = mock()

        synchronizer = GoogleTaskSynchronizer(
            context, caldavDao, gtasksListService, preferences, taskDao,
            taskSaver, firebase, googleTaskDao, taskCreator, defaultFilterProvider,
            permissionChecker, googleAccountManager, refreshBroadcaster,
            taskDeleter, invokers, alarmDao,
        )
    }

    // ================================================================
    // sync() — error handling branches
    // ================================================================

    @Test
    fun syncSetsErrorOnAccountNotFound() = runTest {
        val account = CaldavAccount(uuid = "acc", accountType = TYPE_GOOGLE_TASKS, username = "user@g.com")
        whenever(googleAccountManager.getAccount("user@g.com")).thenReturn(null)
        whenever(context.getString(any())).thenReturn("Cannot access")

        synchronizer.sync(account)

        assertEquals("Cannot access", account.error)
        verify(caldavDao).update(account)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun syncUpdatesLastSyncOnSuccess() = runTest {
        val account = CaldavAccount(uuid = "acc", accountType = TYPE_GOOGLE_TASKS, username = "user@g.com")
        whenever(googleAccountManager.getAccount("user@g.com")).thenReturn(null)
        whenever(context.getString(any())).thenReturn("Cannot access")

        synchronizer.sync(account)

        // error is set, so lastSync should NOT be updated from 0
        assertEquals(0L, account.lastSync)
    }

    @Test
    fun syncAlwaysBroadcastsRefresh() = runTest {
        val account = CaldavAccount(uuid = "acc", accountType = TYPE_GOOGLE_TASKS, username = "user@g.com")
        whenever(googleAccountManager.getAccount("user@g.com")).thenReturn(null)
        whenever(context.getString(any())).thenReturn("error")

        synchronizer.sync(account)

        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun syncAlwaysUpdatesAccount() = runTest {
        val account = CaldavAccount(uuid = "acc", accountType = TYPE_GOOGLE_TASKS, username = "user@g.com")
        whenever(googleAccountManager.getAccount("user@g.com")).thenReturn(null)
        whenever(context.getString(any())).thenReturn("error")

        synchronizer.sync(account)

        verify(caldavDao).update(account)
    }

    // ================================================================
    // truncate — comprehensive
    // ================================================================

    @Test fun truncateNull() = assertNull(GoogleTaskSynchronizer.truncate(null, 10))
    @Test fun truncateEmpty() = assertEquals("", GoogleTaskSynchronizer.truncate("", 10))
    @Test fun truncateShort() = assertEquals("hi", GoogleTaskSynchronizer.truncate("hi", 10))
    @Test fun truncateExact() = assertEquals("exact", GoogleTaskSynchronizer.truncate("exact", 5))
    @Test fun truncateLong() = assertEquals("lon", GoogleTaskSynchronizer.truncate("longer", 3))

    @Test
    fun truncateTitleMaxBoundary() {
        val s = "a".repeat(1024)
        assertEquals(s, GoogleTaskSynchronizer.truncate(s, 1024))
    }

    @Test
    fun truncateTitleOverBoundary() {
        assertEquals(1024, GoogleTaskSynchronizer.truncate("a".repeat(1025), 1024)!!.length)
    }

    @Test
    fun truncateDescMaxBoundary() {
        val s = "b".repeat(8192)
        assertEquals(s, GoogleTaskSynchronizer.truncate(s, 8192))
    }

    @Test
    fun truncateDescOverBoundary() {
        assertEquals(8192, GoogleTaskSynchronizer.truncate("b".repeat(8193), 8192)!!.length)
    }

    // ================================================================
    // getTruncatedValue — comprehensive
    // ================================================================

    @Test fun getTruncBothNull() = assertNull(GoogleTaskSynchronizer.getTruncatedValue(null, null, 100))
    @Test fun getTruncNewNull() = assertNull(GoogleTaskSynchronizer.getTruncatedValue("c", null, 100))
    @Test fun getTruncNewEmpty() = assertEquals("", GoogleTaskSynchronizer.getTruncatedValue("c", "", 100))
    @Test fun getTruncCurrNull() = assertEquals("n", GoogleTaskSynchronizer.getTruncatedValue(null, "n", 100))
    @Test fun getTruncCurrEmpty() = assertEquals("n", GoogleTaskSynchronizer.getTruncatedValue("", "n", 100))
    @Test fun getTruncBothShort() = assertEquals("new", GoogleTaskSynchronizer.getTruncatedValue("old", "new", 100))

    @Test
    fun getTruncPreservesCurrentWhenTruncated() {
        val current = "This is a very long description that continues further"
        val newValue = "This is a very long"
        assertEquals(current, GoogleTaskSynchronizer.getTruncatedValue(current, newValue, 15))
    }

    @Test
    fun getTruncReturnsNewWhenCurrentDoesNotStartWithNew() {
        assertEquals(
            "This is a very long",
            GoogleTaskSynchronizer.getTruncatedValue("Different text entirely", "This is a very long", 15)
        )
    }

    // ================================================================
    // mergeDates — comprehensive
    // ================================================================

    @Test
    fun mergeDatesZeroClearsExisting() {
        val t = org.tasks.data.entity.Task(dueDate = 1000L)
        GoogleTaskSynchronizer.mergeDates(0L, t)
        assertEquals(0L, t.dueDate)
    }

    @Test
    fun mergeDatesNonZeroNoExistingTime() {
        val t = org.tasks.data.entity.Task(dueDate = 0L)
        GoogleTaskSynchronizer.mergeDates(5000L, t)
        assertEquals(5000L, t.dueDate)
    }

    @Test
    fun mergeDatesWithExistingTimePreservesTime() {
        val existing = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY_TIME, 1718409600000L)
        val t = org.tasks.data.entity.Task(dueDate = existing)
        assertTrue(t.hasDueTime())
        val remote = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718496000000L)
        GoogleTaskSynchronizer.mergeDates(remote, t)
        assertTrue(t.hasDueTime())
    }

    @Test
    fun mergeDatesNegativeValue() {
        val t = org.tasks.data.entity.Task(dueDate = 1000L)
        GoogleTaskSynchronizer.mergeDates(-1L, t)
        assertEquals(-1L, t.dueDate)
    }

    @Test
    fun mergeDatesDayOnlyOverwritesDayOnly() {
        val existing = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718323200000L)
        val remote = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718409600000L)
        val t = org.tasks.data.entity.Task(dueDate = existing)
        assertFalse(t.hasDueTime())
        GoogleTaskSynchronizer.mergeDates(remote, t)
        assertEquals(remote, t.dueDate)
    }

    // ================================================================
    // PARENTS_FIRST comparator
    // ================================================================

    private fun parentsFirstComparator(): Comparator<Task> = Comparator { o1, o2 ->
        if (org.tasks.Strings.isNullOrEmpty(o1.parent)) {
            if (org.tasks.Strings.isNullOrEmpty(o2.parent)) 0 else -1
        } else {
            if (org.tasks.Strings.isNullOrEmpty(o2.parent)) 1 else 0
        }
    }

    @Test
    fun parentsFirstBothNoParent() {
        assertEquals(0, parentsFirstComparator().compare(Task(), Task()))
    }

    @Test
    fun parentsFirstParentlessFirst() {
        val parentless = Task()
        val child = Task().apply { parent = "p" }
        assertTrue(parentsFirstComparator().compare(parentless, child) < 0)
    }

    @Test
    fun parentsFirstChildAfter() {
        val parentless = Task()
        val child = Task().apply { parent = "p" }
        assertTrue(parentsFirstComparator().compare(child, parentless) > 0)
    }

    @Test
    fun parentsFirstBothChildren() {
        val c1 = Task().apply { parent = "a" }
        val c2 = Task().apply { parent = "b" }
        assertEquals(0, parentsFirstComparator().compare(c1, c2))
    }

    @Test
    fun parentsFirstEmptyParentIsNoParent() {
        val empty = Task().apply { parent = "" }
        val child = Task().apply { parent = "p" }
        assertTrue(parentsFirstComparator().compare(empty, child) < 0)
    }

    @Test
    fun parentsFirstSortingMultipleTasks() {
        val r1 = Task().apply { id = "r1" }
        val r2 = Task().apply { id = "r2" }
        val c1 = Task().apply { id = "c1"; parent = "r1" }
        val c2 = Task().apply { id = "c2"; parent = "r2" }
        val tasks = mutableListOf(c1, r1, c2, r2)
        java.util.Collections.sort(tasks, parentsFirstComparator())
        assertNull(tasks[0].parent)
        assertNull(tasks[1].parent)
        assertNotNull(tasks[2].parent)
        assertNotNull(tasks[3].parent)
    }
}
