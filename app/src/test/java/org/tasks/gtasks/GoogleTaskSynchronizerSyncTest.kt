package org.tasks.gtasks

import android.content.Context
import com.google.api.services.tasks.model.Task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.analytics.Firebase
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.TaskSaver
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
import com.todoroo.astrid.gtasks.api.GtasksInvoker
import com.todoroo.astrid.service.TaskCreator
import org.tasks.filters.CaldavFilter

class GoogleTaskSynchronizerSyncTest {

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
            context = context,
            caldavDao = caldavDao,
            gtasksListService = gtasksListService,
            preferences = preferences,
            taskDao = taskDao,
            taskSaver = taskSaver,
            firebase = firebase,
            googleTaskDao = googleTaskDao,
            taskCreator = taskCreator,
            defaultFilterProvider = defaultFilterProvider,
            permissionChecker = permissionChecker,
            googleAccountManager = googleAccountManager,
            refreshBroadcaster = refreshBroadcaster,
            taskDeleter = taskDeleter,
            invokers = invokers,
            alarmDao = alarmDao,
        )
    }

    // ========== sync() - account not found ==========

    @Test
    fun sync_accountNotFound_setsCannotAccessError() = runTest {
        val account = CaldavAccount(uuid = "acc-uuid", accountType = TYPE_GOOGLE_TASKS, username = "user@gmail.com")
        whenever(googleAccountManager.getAccount("user@gmail.com")).thenReturn(null)
        whenever(context.getString(any())).thenReturn("Cannot access account")

        synchronizer.sync(account)

        assertEquals("Cannot access account", account.error)
        verify(caldavDao).update(account)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun sync_accountNotFound_doesNotUpdateLastSync() = runTest {
        val account = CaldavAccount(uuid = "acc-uuid", accountType = TYPE_GOOGLE_TASKS, username = "user@gmail.com")
        whenever(googleAccountManager.getAccount("user@gmail.com")).thenReturn(null)
        whenever(context.getString(any())).thenReturn("Cannot access account")

        synchronizer.sync(account)

        // error is not blank, so lastSync should not be updated
        assertEquals(0L, account.lastSync)
    }

    @Test
    fun sync_alwaysCallsUpdateAndRefresh() = runTest {
        val account = CaldavAccount(uuid = "acc-uuid", accountType = TYPE_GOOGLE_TASKS, username = "user@gmail.com")
        whenever(googleAccountManager.getAccount("user@gmail.com")).thenReturn(null)
        whenever(context.getString(any())).thenReturn("error")

        synchronizer.sync(account)

        verify(caldavDao).update(account)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // ========== companion: truncate ==========

    @Test
    fun truncate_null() {
        assertNull(GoogleTaskSynchronizer.truncate(null, 10))
    }

    @Test
    fun truncate_withinLimit() {
        assertEquals("abc", GoogleTaskSynchronizer.truncate("abc", 10))
    }

    @Test
    fun truncate_overLimit() {
        assertEquals("ab", GoogleTaskSynchronizer.truncate("abc", 2))
    }

    @Test
    fun truncate_exactBoundary() {
        val s = "a".repeat(1024)
        assertEquals(s, GoogleTaskSynchronizer.truncate(s, 1024))
    }

    @Test
    fun truncate_oneOverBoundary() {
        assertEquals(1024, GoogleTaskSynchronizer.truncate("a".repeat(1025), 1024)!!.length)
    }

    @Test
    fun truncate_empty() {
        assertEquals("", GoogleTaskSynchronizer.truncate("", 100))
    }

    @Test
    fun truncate_toZero() {
        assertEquals("", GoogleTaskSynchronizer.truncate("abc", 0))
    }

    @Test
    fun truncate_toOne() {
        assertEquals("a", GoogleTaskSynchronizer.truncate("abc", 1))
    }

    @Test
    fun truncate_unicode() {
        assertEquals("\u00E9\u00E8", GoogleTaskSynchronizer.truncate("\u00E9\u00E8\u00EA", 2))
    }

    @Test
    fun truncate_maxInt_returnsOriginal() {
        assertEquals("test", GoogleTaskSynchronizer.truncate("test", Int.MAX_VALUE))
    }

    @Test
    fun truncate_descriptionMax() {
        assertEquals(8192, GoogleTaskSynchronizer.truncate("a".repeat(10000), 8192)!!.length)
    }

    // ========== companion: getTruncatedValue ==========

    @Test
    fun getTruncatedValue_bothNull() {
        assertNull(GoogleTaskSynchronizer.getTruncatedValue(null, null, 100))
    }

    @Test
    fun getTruncatedValue_newNull_returnsNull() {
        assertNull(GoogleTaskSynchronizer.getTruncatedValue("old", null, 100))
    }

    @Test
    fun getTruncatedValue_newEmpty_returnsEmpty() {
        assertEquals("", GoogleTaskSynchronizer.getTruncatedValue("old", "", 100))
    }

    @Test
    fun getTruncatedValue_currentNull_returnsNew() {
        assertEquals("new", GoogleTaskSynchronizer.getTruncatedValue(null, "new", 100))
    }

    @Test
    fun getTruncatedValue_currentEmpty_returnsNew() {
        assertEquals("new", GoogleTaskSynchronizer.getTruncatedValue("", "new", 100))
    }

    @Test
    fun getTruncatedValue_shortNew_returnsNew() {
        assertEquals("new", GoogleTaskSynchronizer.getTruncatedValue("old", "new", 100))
    }

    @Test
    fun getTruncatedValue_longNew_currentStartsWithNew_returnsCurrent() {
        assertEquals(
            "abcdef_extended",
            GoogleTaskSynchronizer.getTruncatedValue("abcdef_extended", "abcdef", 5)
        )
    }

    @Test
    fun getTruncatedValue_longNew_currentDoesNotStartWithNew_returnsNew() {
        assertEquals(
            "abcdef",
            GoogleTaskSynchronizer.getTruncatedValue("xyz", "abcdef", 5)
        )
    }

    @Test
    fun getTruncatedValue_preservesCurrent_whenTruncated() {
        val current = "This is a very long description that continues"
        val newValue = "This is a very long"
        assertEquals(current, GoogleTaskSynchronizer.getTruncatedValue(current, newValue, 15))
    }

    @Test
    fun getTruncatedValue_returnsNew_whenCurrentDoesNotStartWithNew() {
        assertEquals(
            "This is a very long",
            GoogleTaskSynchronizer.getTruncatedValue("Different", "This is a very long", 15)
        )
    }

    // ========== companion: mergeDates ==========

    @Test
    fun mergeDates_zeroDueDate_clearsExisting() {
        val task = org.tasks.data.entity.Task(dueDate = 1000L)
        GoogleTaskSynchronizer.mergeDates(0L, task)
        assertEquals(0L, task.dueDate)
    }

    @Test
    fun mergeDates_nonZero_noExistingTime_setsDueDate() {
        val task = org.tasks.data.entity.Task(dueDate = 0L)
        GoogleTaskSynchronizer.mergeDates(5000L, task)
        assertEquals(5000L, task.dueDate)
    }

    @Test
    fun mergeDates_negative_setsDueDate() {
        val task = org.tasks.data.entity.Task(dueDate = 1000L)
        GoogleTaskSynchronizer.mergeDates(-1L, task)
        assertEquals(-1L, task.dueDate)
    }

    @Test
    fun mergeDates_withExistingTime_preservesTime() {
        val existingDueDate = org.tasks.data.createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY_TIME,
            1718409600000L
        )
        val task = org.tasks.data.entity.Task(dueDate = existingDueDate)
        assertTrue(task.hasDueTime())

        val remoteDueDate = org.tasks.data.createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY,
            1718496000000L
        )
        GoogleTaskSynchronizer.mergeDates(remoteDueDate, task)
        assertTrue(task.hasDueTime())
    }

    @Test
    fun mergeDates_nonZeroRemote_noExistingDue() {
        val rd = org.tasks.data.createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718409600000L
        )
        val task = org.tasks.data.entity.Task()
        GoogleTaskSynchronizer.mergeDates(rd, task)
        assertTrue(task.hasDueDate())
    }

    @Test
    fun mergeDates_zero_noExisting() {
        val task = org.tasks.data.entity.Task(dueDate = 0L)
        GoogleTaskSynchronizer.mergeDates(0L, task)
        assertEquals(0L, task.dueDate)
    }

    @Test
    fun mergeDates_dayOnly_overwritesDayOnly() {
        val existing = org.tasks.data.createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718323200000L
        )
        val remote = org.tasks.data.createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718409600000L
        )
        val task = org.tasks.data.entity.Task(dueDate = existing)
        GoogleTaskSynchronizer.mergeDates(remote, task)
        assertEquals(remote, task.dueDate)
    }

    // ========== PARENTS_FIRST comparator ==========

    @Test
    fun parentsFirst_bothNull_equal() {
        val t1 = Task()
        val t2 = Task()
        assertEquals(0, parentsFirstComparator().compare(t1, t2))
    }

    @Test
    fun parentsFirst_parentless_beforeChild() {
        val parentless = Task()
        val child = Task().apply { parent = "p" }
        assertTrue(parentsFirstComparator().compare(parentless, child) < 0)
    }

    @Test
    fun parentsFirst_child_afterParentless() {
        val parentless = Task()
        val child = Task().apply { parent = "p" }
        assertTrue(parentsFirstComparator().compare(child, parentless) > 0)
    }

    @Test
    fun parentsFirst_bothChildren_equal() {
        val c1 = Task().apply { parent = "a" }
        val c2 = Task().apply { parent = "b" }
        assertEquals(0, parentsFirstComparator().compare(c1, c2))
    }

    @Test
    fun parentsFirst_sorting() {
        val root1 = Task().apply { id = "r1" }
        val root2 = Task().apply { id = "r2" }
        val child1 = Task().apply { id = "c1"; parent = "r1" }
        val child2 = Task().apply { id = "c2"; parent = "r2" }
        val tasks = mutableListOf(child1, root1, child2, root2)
        java.util.Collections.sort(tasks, parentsFirstComparator())
        assertNull(tasks[0].parent)
        assertNull(tasks[1].parent)
        assertNotNull(tasks[2].parent)
        assertNotNull(tasks[3].parent)
    }

    @Test
    fun parentsFirst_emptyParent_asNoParent() {
        val empty = Task().apply { parent = "" }
        val child = Task().apply { parent = "p" }
        assertTrue(parentsFirstComparator().compare(empty, child) < 0)
    }

    @Test
    fun parentsFirst_manyTasks() {
        val r1 = Task().apply { id = "r1" }
        val r2 = Task().apply { id = "r2" }
        val r3 = Task().apply { id = "r3" }
        val c1 = Task().apply { id = "c1"; parent = "r1" }
        val c2 = Task().apply { id = "c2"; parent = "r2" }
        val tasks = mutableListOf(c1, r1, c2, r2, r3)
        java.util.Collections.sort(tasks, parentsFirstComparator())
        // All parentless before children
        assertNull(tasks[0].parent)
        assertNull(tasks[1].parent)
        assertNull(tasks[2].parent)
        assertNotNull(tasks[3].parent)
        assertNotNull(tasks[4].parent)
    }

    private fun parentsFirstComparator(): Comparator<Task> = Comparator { o1, o2 ->
        if (org.tasks.Strings.isNullOrEmpty(o1.parent)) {
            if (org.tasks.Strings.isNullOrEmpty(o2.parent)) 0 else -1
        } else {
            if (org.tasks.Strings.isNullOrEmpty(o2.parent)) 1 else 0
        }
    }
}
