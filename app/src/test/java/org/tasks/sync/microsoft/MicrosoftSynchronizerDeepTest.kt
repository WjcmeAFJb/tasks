package org.tasks.sync.microsoft

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.analytics.Firebase
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.VtodoCache
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.http.HttpClientFactory
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.service.TaskDeleter
import org.tasks.sync.microsoft.MicrosoftConverter.applyRemote
import org.tasks.sync.microsoft.MicrosoftConverter.applySubtask
import org.tasks.sync.microsoft.MicrosoftConverter.toChecklistItem
import org.tasks.sync.microsoft.MicrosoftConverter.toRemote
import com.todoroo.astrid.service.TaskCreator

class MicrosoftSynchronizerDeepTest {

    private lateinit var caldavDao: CaldavDao
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var taskDeleter: TaskDeleter
    private lateinit var firebase: Firebase
    private lateinit var taskCreator: TaskCreator
    private lateinit var httpClientFactory: HttpClientFactory
    private lateinit var tagDao: TagDao
    private lateinit var tagDataDao: TagDataDao
    private lateinit var preferences: Preferences
    private lateinit var vtodoCache: VtodoCache
    private lateinit var defaultFilterProvider: DefaultFilterProvider

    @Before
    fun setUp() {
        caldavDao = mock()
        taskDao = mock()
        taskSaver = mock()
        refreshBroadcaster = mock()
        taskDeleter = mock()
        firebase = mock()
        taskCreator = mock()
        httpClientFactory = mock()
        tagDao = mock()
        tagDataDao = mock()
        preferences = mock()
        vtodoCache = mock()
        defaultFilterProvider = mock()
    }

    // ========== MicrosoftConverter.applyRemote ==========

    @Test
    fun applyRemote_setsTitle() {
        val task = Task()
        val remote = Tasks.Task(title = "My Title")
        task.applyRemote(remote, Task.Priority.NONE)
        assertEquals("My Title", task.title)
    }

    @Test
    fun applyRemote_setsNotes_textContent() {
        val task = Task()
        val remote = Tasks.Task(
            body = Tasks.Task.Body(content = "Some notes", contentType = "text")
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertEquals("Some notes", task.notes)
    }

    @Test
    fun applyRemote_nullNotes_whenContentTypeNotText() {
        val task = Task()
        val remote = Tasks.Task(
            body = Tasks.Task.Body(content = "HTML notes", contentType = "html")
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertNull(task.notes)
    }

    @Test
    fun applyRemote_nullNotes_whenBodyNull() {
        val task = Task()
        val remote = Tasks.Task(body = null)
        task.applyRemote(remote, Task.Priority.NONE)
        assertNull(task.notes)
    }

    @Test
    fun applyRemote_blankContent_nullNotes() {
        val task = Task()
        val remote = Tasks.Task(
            body = Tasks.Task.Body(content = "  ", contentType = "text")
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertNull(task.notes)
    }

    @Test
    fun applyRemote_highImportance_setsHighPriority() {
        val task = Task(priority = Task.Priority.NONE)
        val remote = Tasks.Task(importance = Tasks.Task.Importance.high)
        task.applyRemote(remote, Task.Priority.NONE)
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    @Test
    fun applyRemote_nonHighImportance_keepsPriority() {
        val task = Task(priority = Task.Priority.MEDIUM)
        val remote = Tasks.Task(importance = Tasks.Task.Importance.normal)
        task.applyRemote(remote, Task.Priority.NONE)
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun applyRemote_wasHigh_remoteNotHigh_usesDefault() {
        val task = Task(priority = Task.Priority.HIGH)
        val remote = Tasks.Task(importance = Tasks.Task.Importance.low)
        task.applyRemote(remote, Task.Priority.LOW)
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun applyRemote_wasHigh_remoteNotHigh_defaultHigh_becomesNone() {
        val task = Task(priority = Task.Priority.HIGH)
        val remote = Tasks.Task(importance = Tasks.Task.Importance.normal)
        task.applyRemote(remote, Task.Priority.HIGH)
        assertEquals(Task.Priority.NONE, task.priority)
    }

    @Test
    fun applyRemote_completedStatus() {
        val task = Task()
        val remote = Tasks.Task(
            completedDateTime = Tasks.Task.DateTime(
                dateTime = "2024-01-15T10:00:00.0000000",
                timeZone = "UTC",
            )
        )
        task.applyRemote(remote, Task.Priority.NONE)
        // completionDate should be > 0 (parsed from completedDateTime)
        assert(task.completionDate > 0)
    }

    @Test
    fun applyRemote_notCompleted_zeroCompletionDate() {
        val task = Task()
        val remote = Tasks.Task(completedDateTime = null)
        task.applyRemote(remote, Task.Priority.NONE)
        assertEquals(0L, task.completionDate)
    }

    @Test
    fun applyRemote_recurrence_daily() {
        val task = Task()
        val remote = Tasks.Task(
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.daily,
                    interval = 3,
                    daysOfWeek = emptyList(),
                )
            )
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertNotNull(task.recurrence)
        assert(task.recurrence!!.contains("DAILY"))
        assert(task.recurrence!!.contains("INTERVAL=3"))
    }

    @Test
    fun applyRemote_recurrence_weekly_withDays() {
        val task = Task()
        val remote = Tasks.Task(
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.weekly,
                    interval = 1,
                    daysOfWeek = listOf(
                        Tasks.Task.RecurrenceDayOfWeek.monday,
                        Tasks.Task.RecurrenceDayOfWeek.wednesday,
                        Tasks.Task.RecurrenceDayOfWeek.friday,
                    ),
                )
            )
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertNotNull(task.recurrence)
        assert(task.recurrence!!.contains("WEEKLY"))
        assert(task.recurrence!!.contains("MO"))
        assert(task.recurrence!!.contains("WE"))
        assert(task.recurrence!!.contains("FR"))
    }

    @Test
    fun applyRemote_recurrence_monthly() {
        val task = Task()
        val remote = Tasks.Task(
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.absoluteMonthly,
                    interval = 2,
                    daysOfWeek = emptyList(),
                )
            )
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertNotNull(task.recurrence)
        assert(task.recurrence!!.contains("MONTHLY"))
    }

    @Test
    fun applyRemote_recurrence_yearly() {
        val task = Task()
        val remote = Tasks.Task(
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.absoluteYearly,
                    interval = 1,
                    daysOfWeek = emptyList(),
                )
            )
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertNotNull(task.recurrence)
        assert(task.recurrence!!.contains("YEARLY"))
    }

    @Test
    fun applyRemote_recurrence_relativeMonthly_returnsNull() {
        val task = Task()
        val remote = Tasks.Task(
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.relativeMonthly,
                    interval = 1,
                    daysOfWeek = emptyList(),
                )
            )
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertNull(task.recurrence)
    }

    @Test
    fun applyRemote_recurrence_relativeYearly_returnsNull() {
        val task = Task()
        val remote = Tasks.Task(
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.relativeYearly,
                    interval = 1,
                    daysOfWeek = emptyList(),
                )
            )
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertNull(task.recurrence)
    }

    @Test
    fun applyRemote_noRecurrence_null() {
        val task = Task()
        val remote = Tasks.Task(recurrence = null)
        task.applyRemote(remote, Task.Priority.NONE)
        assertNull(task.recurrence)
    }

    @Test
    fun applyRemote_interval1_notIncludedInRRule() {
        val task = Task()
        val remote = Tasks.Task(
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.daily,
                    interval = 1,
                    daysOfWeek = emptyList(),
                )
            )
        )
        task.applyRemote(remote, Task.Priority.NONE)
        // RRULE with interval=1 should not have INTERVAL= since takeIf { it > 1 }
        assertNotNull(task.recurrence)
        assert(!task.recurrence!!.contains("INTERVAL="))
    }

    @Test
    fun applyRemote_allDaysOfWeek() {
        val task = Task()
        val remote = Tasks.Task(
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.weekly,
                    interval = 1,
                    daysOfWeek = listOf(
                        Tasks.Task.RecurrenceDayOfWeek.sunday,
                        Tasks.Task.RecurrenceDayOfWeek.monday,
                        Tasks.Task.RecurrenceDayOfWeek.tuesday,
                        Tasks.Task.RecurrenceDayOfWeek.wednesday,
                        Tasks.Task.RecurrenceDayOfWeek.thursday,
                        Tasks.Task.RecurrenceDayOfWeek.friday,
                        Tasks.Task.RecurrenceDayOfWeek.saturday,
                    ),
                )
            )
        )
        task.applyRemote(remote, Task.Priority.NONE)
        assertNotNull(task.recurrence)
        assert(task.recurrence!!.contains("SU"))
        assert(task.recurrence!!.contains("MO"))
        assert(task.recurrence!!.contains("TU"))
        assert(task.recurrence!!.contains("WE"))
        assert(task.recurrence!!.contains("TH"))
        assert(task.recurrence!!.contains("FR"))
        assert(task.recurrence!!.contains("SA"))
    }

    // ========== MicrosoftConverter.toRemote ==========

    @Test
    fun toRemote_highPriority() {
        val task = Task(priority = Task.Priority.HIGH)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertEquals(Tasks.Task.Importance.high, result.importance)
    }

    @Test
    fun toRemote_mediumPriority() {
        val task = Task(priority = Task.Priority.MEDIUM)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertEquals(Tasks.Task.Importance.normal, result.importance)
    }

    @Test
    fun toRemote_lowPriority() {
        val task = Task(priority = Task.Priority.LOW)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertEquals(Tasks.Task.Importance.low, result.importance)
    }

    @Test
    fun toRemote_nonePriority() {
        val task = Task(priority = Task.Priority.NONE)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertEquals(Tasks.Task.Importance.low, result.importance)
    }

    @Test
    fun toRemote_completed() {
        val task = Task(completionDate = 1000L)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertEquals(Tasks.Task.Status.completed, result.status)
        assertNotNull(result.completedDateTime)
    }

    @Test
    fun toRemote_notCompleted() {
        val task = Task(completionDate = 0)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertEquals(Tasks.Task.Status.notStarted, result.status)
        assertNull(result.completedDateTime)
    }

    @Test
    fun toRemote_withNotes() {
        val task = Task(notes = "Hello world")
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNotNull(result.body)
        assertEquals("Hello world", result.body!!.content)
        assertEquals("text", result.body!!.contentType)
    }

    @Test
    fun toRemote_withoutNotes() {
        val task = Task(notes = null)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNull(result.body)
    }

    @Test
    fun toRemote_withTags() {
        val task = Task()
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val tags = listOf(TagData(name = "work"), TagData(name = "urgent"))
        val result = task.toRemote(ct, tags)
        assertEquals(listOf("work", "urgent"), result.categories)
    }

    @Test
    fun toRemote_emptyTags() {
        val task = Task()
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertEquals(emptyList<String>(), result.categories)
    }

    @Test
    fun toRemote_noDueDate_noRecurrence_nullDueDateTime() {
        val task = Task(dueDate = 0L)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNull(result.dueDateTime)
    }

    @Test
    fun toRemote_setsRemoteId() {
        val task = Task()
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "remote-id")
        val result = task.toRemote(ct, emptyList())
        assertEquals("remote-id", result.id)
    }

    @Test
    fun toRemote_setsTitle() {
        val task = Task(title = "My Task")
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertEquals("My Task", result.title)
    }

    // ========== MicrosoftConverter.toChecklistItem ==========

    @Test
    fun toChecklistItem_basic() {
        val task = Task(title = "Sub item", completionDate = 0L, creationDate = 1000L)
        val result = task.toChecklistItem("chk-id")
        assertEquals("chk-id", result.id)
        assertEquals("Sub item", result.displayName)
        assertEquals(false, result.isChecked)
    }

    @Test
    fun toChecklistItem_completed() {
        val task = Task(title = "Done", completionDate = 5000L, creationDate = 1000L)
        val result = task.toChecklistItem("chk-id")
        assertEquals(true, result.isChecked)
        assertNotNull(result.checkedDateTime)
    }

    @Test
    fun toChecklistItem_nullId() {
        val task = Task(title = "Item")
        val result = task.toChecklistItem(null)
        assertNull(result.id)
    }

    @Test
    fun toChecklistItem_nullTitle() {
        val task = Task(title = null)
        val result = task.toChecklistItem("id")
        assertEquals("", result.displayName)
    }

    // ========== MicrosoftConverter.applySubtask ==========

    @Test
    fun applySubtask_basic() {
        val task = Task()
        val item = Tasks.Task.ChecklistItem(
            id = "chk-1",
            displayName = "Subtask",
            isChecked = false,
            createdDateTime = "2024-01-01T00:00:00.0000000Z",
        )
        task.applySubtask(parent = 5L, parentCompletionDate = 0L, checklistItem = item)
        assertEquals(5L, task.parent)
        assertEquals("Subtask", task.title)
    }

    @Test
    fun applySubtask_checked() {
        val task = Task()
        val item = Tasks.Task.ChecklistItem(
            id = "chk-1",
            displayName = "Done Sub",
            isChecked = true,
            checkedDateTime = "2024-06-15T12:00:00.0000000Z",
            createdDateTime = "2024-01-01T00:00:00.0000000Z",
        )
        task.applySubtask(parent = 5L, parentCompletionDate = 0L, checklistItem = item)
        assert(task.completionDate > 0)
    }

    @Test
    fun applySubtask_unchecked_usesParentCompletionDate() {
        val task = Task()
        val item = Tasks.Task.ChecklistItem(
            id = "chk-1",
            displayName = "Sub",
            isChecked = false,
            createdDateTime = "2024-01-01T00:00:00.0000000Z",
        )
        task.applySubtask(parent = 5L, parentCompletionDate = 9999L, checklistItem = item)
        assertEquals(9999L, task.completionDate)
    }

    // ========== sync() exception handling via thenAnswer ==========

    @Test
    fun sync_runtimeException_setsErrorAndReports() = runTest {
        val synchronizer = MicrosoftSynchronizer(
            caldavDao = caldavDao,
            taskDao = taskDao,
            taskSaver = taskSaver,
            refreshBroadcaster = refreshBroadcaster,
            taskDeleter = taskDeleter,
            firebase = firebase,
            taskCreator = taskCreator,
            httpClientFactory = httpClientFactory,
            tagDao = tagDao,
            tagDataDao = tagDataDao,
            preferences = preferences,
            vtodoCache = vtodoCache,
            defaultFilterProvider = defaultFilterProvider,
        )

        val account = CaldavAccount(uuid = "acc", accountType = TYPE_MICROSOFT)
        whenever(httpClientFactory.getMicrosoftService(any()))
            .thenAnswer { throw RuntimeException("unexpected") }

        synchronizer.sync(account)

        assertEquals("unexpected", account.error)
        verify(firebase).reportException(any())
        verify(caldavDao).update(account)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun sync_socketTimeoutException_setsError() = runTest {
        val synchronizer = MicrosoftSynchronizer(
            caldavDao = caldavDao,
            taskDao = taskDao,
            taskSaver = taskSaver,
            refreshBroadcaster = refreshBroadcaster,
            taskDeleter = taskDeleter,
            firebase = firebase,
            taskCreator = taskCreator,
            httpClientFactory = httpClientFactory,
            tagDao = tagDao,
            tagDataDao = tagDataDao,
            preferences = preferences,
            vtodoCache = vtodoCache,
            defaultFilterProvider = defaultFilterProvider,
        )

        val account = CaldavAccount(uuid = "acc", accountType = TYPE_MICROSOFT)
        whenever(httpClientFactory.getMicrosoftService(any()))
            .thenAnswer { throw java.net.SocketTimeoutException("timeout") }

        synchronizer.sync(account)

        assertEquals("timeout", account.error)
    }

    @Test
    fun sync_unknownHostException_setsError() = runTest {
        val synchronizer = MicrosoftSynchronizer(
            caldavDao = caldavDao,
            taskDao = taskDao,
            taskSaver = taskSaver,
            refreshBroadcaster = refreshBroadcaster,
            taskDeleter = taskDeleter,
            firebase = firebase,
            taskCreator = taskCreator,
            httpClientFactory = httpClientFactory,
            tagDao = tagDao,
            tagDataDao = tagDataDao,
            preferences = preferences,
            vtodoCache = vtodoCache,
            defaultFilterProvider = defaultFilterProvider,
        )

        val account = CaldavAccount(uuid = "acc", accountType = TYPE_MICROSOFT)
        whenever(httpClientFactory.getMicrosoftService(any()))
            .thenAnswer { throw java.net.UnknownHostException("no host") }

        synchronizer.sync(account)

        assertEquals("no host", account.error)
    }

    @Test
    fun sync_sslException_setsError() = runTest {
        val synchronizer = MicrosoftSynchronizer(
            caldavDao = caldavDao,
            taskDao = taskDao,
            taskSaver = taskSaver,
            refreshBroadcaster = refreshBroadcaster,
            taskDeleter = taskDeleter,
            firebase = firebase,
            taskCreator = taskCreator,
            httpClientFactory = httpClientFactory,
            tagDao = tagDao,
            tagDataDao = tagDataDao,
            preferences = preferences,
            vtodoCache = vtodoCache,
            defaultFilterProvider = defaultFilterProvider,
        )

        val account = CaldavAccount(uuid = "acc", accountType = TYPE_MICROSOFT)
        whenever(httpClientFactory.getMicrosoftService(any()))
            .thenAnswer { throw javax.net.ssl.SSLException("ssl error") }

        synchronizer.sync(account)

        assertEquals("ssl error", account.error)
    }

    @Test
    fun sync_connectException_setsError() = runTest {
        val synchronizer = MicrosoftSynchronizer(
            caldavDao = caldavDao,
            taskDao = taskDao,
            taskSaver = taskSaver,
            refreshBroadcaster = refreshBroadcaster,
            taskDeleter = taskDeleter,
            firebase = firebase,
            taskCreator = taskCreator,
            httpClientFactory = httpClientFactory,
            tagDao = tagDao,
            tagDataDao = tagDataDao,
            preferences = preferences,
            vtodoCache = vtodoCache,
            defaultFilterProvider = defaultFilterProvider,
        )

        val account = CaldavAccount(uuid = "acc", accountType = TYPE_MICROSOFT)
        whenever(httpClientFactory.getMicrosoftService(any()))
            .thenAnswer { throw java.net.ConnectException("connection refused") }

        synchronizer.sync(account)

        assertEquals("connection refused", account.error)
    }

    @Test
    fun sync_ioException_setsError() = runTest {
        val synchronizer = MicrosoftSynchronizer(
            caldavDao = caldavDao,
            taskDao = taskDao,
            taskSaver = taskSaver,
            refreshBroadcaster = refreshBroadcaster,
            taskDeleter = taskDeleter,
            firebase = firebase,
            taskCreator = taskCreator,
            httpClientFactory = httpClientFactory,
            tagDao = tagDao,
            tagDataDao = tagDataDao,
            preferences = preferences,
            vtodoCache = vtodoCache,
            defaultFilterProvider = defaultFilterProvider,
        )

        val account = CaldavAccount(uuid = "acc", accountType = TYPE_MICROSOFT)
        whenever(httpClientFactory.getMicrosoftService(any()))
            .thenAnswer { throw java.io.IOException("io error") }

        synchronizer.sync(account)

        assertEquals("io error", account.error)
        verify(firebase, never()).reportException(any())
    }

    // ========== toRemote recurrence round-trip ==========

    @Test
    fun toRemote_dailyRecurrence() {
        val task = Task(recurrence = "FREQ=DAILY;INTERVAL=2")
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNotNull(result.recurrence)
        assertEquals(Tasks.Task.RecurrenceType.daily, result.recurrence!!.pattern.type)
        assertEquals(2, result.recurrence!!.pattern.interval)
    }

    @Test
    fun toRemote_weeklyRecurrence_withDays() {
        val task = Task(recurrence = "FREQ=WEEKLY;BYDAY=MO,WE,FR")
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNotNull(result.recurrence)
        assertEquals(Tasks.Task.RecurrenceType.weekly, result.recurrence!!.pattern.type)
        assertEquals(3, result.recurrence!!.pattern.daysOfWeek.size)
        assert(result.recurrence!!.pattern.daysOfWeek.contains(Tasks.Task.RecurrenceDayOfWeek.monday))
        assert(result.recurrence!!.pattern.daysOfWeek.contains(Tasks.Task.RecurrenceDayOfWeek.wednesday))
        assert(result.recurrence!!.pattern.daysOfWeek.contains(Tasks.Task.RecurrenceDayOfWeek.friday))
    }

    @Test
    fun toRemote_monthlyRecurrence() {
        val task = Task(recurrence = "FREQ=MONTHLY;INTERVAL=1", dueDate = 1718409600000L)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNotNull(result.recurrence)
        assertEquals(Tasks.Task.RecurrenceType.absoluteMonthly, result.recurrence!!.pattern.type)
    }

    @Test
    fun toRemote_yearlyRecurrence() {
        val task = Task(recurrence = "FREQ=YEARLY;INTERVAL=1", dueDate = 1718409600000L)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNotNull(result.recurrence)
        assertEquals(Tasks.Task.RecurrenceType.absoluteYearly, result.recurrence!!.pattern.type)
    }

    @Test
    fun toRemote_noRecurrence() {
        val task = Task(recurrence = null)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNull(result.recurrence)
    }

    @Test
    fun toRemote_emptyRecurrence() {
        val task = Task(recurrence = "")
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNull(result.recurrence)
    }

    @Test
    fun toRemote_allDayOfWeekMappings() {
        val task = Task(recurrence = "FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA")
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        assertNotNull(result.recurrence)
        val days = result.recurrence!!.pattern.daysOfWeek
        assertEquals(7, days.size)
        assert(days.contains(Tasks.Task.RecurrenceDayOfWeek.sunday))
        assert(days.contains(Tasks.Task.RecurrenceDayOfWeek.saturday))
    }

    // ========== toRemote with recurring task but no due date ==========

    @Test
    fun toRemote_recurringNoDueDate_setsFallbackDueDateTime() {
        val task = Task(recurrence = "FREQ=DAILY", dueDate = 0L)
        val ct = CaldavTask(task = 1L, calendar = "c", remoteId = "r")
        val result = task.toRemote(ct, emptyList())
        // Recurring task without due date should still have dueDateTime
        assertNotNull(result.dueDateTime)
    }

    // ========== TaskLists.TaskList.applyTo ==========

    @Test
    fun applyTo_owner() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(
            id = "id-1",
            displayName = "List",
            isOwner = true,
            isShared = false,
        )
        remote.applyTo(list)
        assertEquals("id-1", list.uuid)
        assertEquals("id-1", list.url)
        assertEquals("List", list.name)
        assertEquals(CaldavCalendar.ACCESS_OWNER, list.access)
    }

    @Test
    fun applyTo_shared() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(
            id = "id-2",
            isOwner = false,
            isShared = true,
        )
        remote.applyTo(list)
        assertEquals(CaldavCalendar.ACCESS_READ_WRITE, list.access)
    }

    @Test
    fun applyTo_unknown() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(id = "id-3")
        remote.applyTo(list)
        assertEquals(CaldavCalendar.ACCESS_UNKNOWN, list.access)
    }

    @Test
    fun applyTo_preservesAccount() {
        val list = CaldavCalendar(account = "my-account")
        val remote = TaskLists.TaskList(id = "new-id", displayName = "New")
        remote.applyTo(list)
        assertEquals("my-account", list.account)
        assertEquals("New", list.name)
    }

    @Test
    fun applyTo_preservesCtag() {
        val list = CaldavCalendar(ctag = "token123")
        val remote = TaskLists.TaskList(id = "id")
        remote.applyTo(list)
        assertEquals("token123", list.ctag)
    }
}
