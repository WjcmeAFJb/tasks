package org.tasks.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_PAYMENT_REQUIRED
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_UNAUTHORIZED
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5Managed
import org.tasks.data.entity.CaldavAccount.Companion.isDecSync
import org.tasks.data.entity.CaldavAccount.Companion.isEteSync
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.data.entity.CaldavAccount.Companion.isTosRequired
import org.tasks.data.entity.CaldavAccount.Companion.openTaskType
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences

/**
 * Deep tests for data extension functions, entities, and extension logic.
 * Covers TaskDaoExtensions, LocationDaoExtensions, CaldavAccount properties,
 * CaldavCalendar properties, and TagData/Place entities.
 */
class DataDeepTest {

    private lateinit var taskDao: TaskDao
    private lateinit var locationDao: LocationDao
    private lateinit var preferences: AppPreferences

    @Before
    fun setUp() {
        taskDao = mock(TaskDao::class.java)
        locationDao = mock(LocationDao::class.java)
        preferences = mock(AppPreferences::class.java)
    }

    // =============================================
    // TaskDaoExtensions: countSql
    // =============================================

    @Test
    fun countSqlDelegatesToCount() = runTest {
        `when`(taskDao.count(any(String::class.java) ?: "")).thenReturn(42)

        val result = taskDao.countSql("WHERE tasks.deleted<=0")

        assertEquals(42, result)
    }

    // =============================================
    // TaskDaoExtensions: countCompletedSql — various SQL patterns
    // =============================================

    @Test
    fun countCompletedSqlWithMultipleCompletedClauses() = runTest {
        val sql = "WHERE tasks.completed<=0 AND (tasks.completed<=0 OR other)"
        `when`(taskDao.count(any(String::class.java) ?: "")).thenReturn(3)

        val result = taskDao.countCompletedSql(sql)
        assertEquals(3, result)
    }

    @Test
    fun countCompletedSqlReturnsZeroWhenNoCompletedClause() = runTest {
        val sql = "WHERE tasks.deleted<=0 AND tasks.hideUntil<=12345"
        val result = taskDao.countCompletedSql(sql)
        assertEquals(0, result)
    }

    @Test
    fun countCompletedSqlReplacesCorrectly() = runTest {
        val sql = "WHERE tasks.completed<=0"
        `when`(taskDao.count(any(String::class.java) ?: "")).thenReturn(10)

        val result = taskDao.countCompletedSql(sql)
        assertEquals(10, result)
    }

    @Test
    fun countCompletedSqlEmptyString() = runTest {
        assertEquals(0, taskDao.countCompletedSql(""))
    }

    @Test
    fun countCompletedSqlWithOnlyCompletedClause() = runTest {
        val sql = "tasks.completed<=0"
        `when`(taskDao.count(any(String::class.java) ?: "")).thenReturn(7)

        val result = taskDao.countCompletedSql(sql)
        assertEquals(7, result)
    }

    // =============================================
    // TaskDaoExtensions: fetchFiltered
    // =============================================

    @Test
    fun fetchFilteredMapsContainersToTasks() = runTest {
        val task1 = Task(id = 1, title = "Buy milk")
        val task2 = Task(id = 2, title = "Call dentist")
        val containers = listOf(
            TaskContainer(task = task1),
            TaskContainer(task = task2),
        )
        `when`(taskDao.fetchTasks(any(String::class.java) ?: "")).thenReturn(containers)

        val result = taskDao.fetchFiltered("WHERE tasks.completed<=0")

        assertEquals(2, result.size)
        assertEquals("Buy milk", result[0].title)
        assertEquals("Call dentist", result[1].title)
    }

    @Test
    fun fetchFilteredReturnsEmptyWhenNoMatches() = runTest {
        `when`(taskDao.fetchTasks(any(String::class.java) ?: "")).thenReturn(emptyList())

        val result = taskDao.fetchFiltered("WHERE 1=0")
        assertTrue(result.isEmpty())
    }

    @Test
    fun fetchFilteredPreservesTaskFields() = runTest {
        val task = Task(
            id = 99,
            title = "Important",
            priority = Task.Priority.HIGH,
            notes = "some notes"
        )
        `when`(taskDao.fetchTasks(any(String::class.java) ?: "")).thenReturn(
            listOf(TaskContainer(task = task))
        )

        val result = taskDao.fetchFiltered("WHERE tasks.deleted<=0")

        assertEquals(1, result.size)
        assertEquals(99, result[0].id)
        assertEquals("Important", result[0].title)
        assertEquals(Task.Priority.HIGH, result[0].priority)
        assertEquals("some notes", result[0].notes)
    }

    // =============================================
    // LocationDaoExtensions: createGeofence
    // =============================================

    @Test
    fun createGeofenceNoReminders() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(0)

        val geofence = createGeofence("test-place", preferences)

        assertFalse(geofence.isArrival)
        assertFalse(geofence.isDeparture)
    }

    @Test
    fun createGeofenceArrivalOnly() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        val geofence = createGeofence("test-place", preferences)

        assertTrue(geofence.isArrival)
        assertFalse(geofence.isDeparture)
    }

    @Test
    fun createGeofenceDepartureOnly() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(2)

        val geofence = createGeofence("test-place", preferences)

        assertFalse(geofence.isArrival)
        assertTrue(geofence.isDeparture)
    }

    @Test
    fun createGeofenceBothArrivalAndDeparture() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(3)

        val geofence = createGeofence("test-place", preferences)

        assertTrue(geofence.isArrival)
        assertTrue(geofence.isDeparture)
    }

    @Test
    fun createGeofenceWithNullPlace() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        val geofence = createGeofence(null, preferences)

        assertNull(geofence.place)
        assertTrue(geofence.isArrival)
    }

    @Test
    fun createGeofenceValue4IsNotArrivalOrDeparture() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(4)

        val geofence = createGeofence("test", preferences)

        assertFalse(geofence.isArrival)
        assertFalse(geofence.isDeparture)
    }

    // =============================================
    // LocationDaoExtensions: getLocation
    // =============================================

    @Test
    fun getLocationForNewTaskWithPlaceTransitory() = runTest {
        val task = Task(id = Task.NO_ID)
        task.putTransitory(Place.KEY, "place-uid")
        val place = Place(uid = "place-uid", name = "Test Place")
        `when`(locationDao.getPlace("place-uid")).thenReturn(place)
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        val result = locationDao.getLocation(task, preferences)

        assertNotNull(result)
        assertEquals("Test Place", result!!.place.name)
        assertTrue(result.geofence.isArrival)
    }

    @Test
    fun getLocationForNewTaskWithoutTransitory() = runTest {
        val task = Task(id = Task.NO_ID)

        val result = locationDao.getLocation(task, preferences)

        assertNull(result)
    }

    @Test
    fun getLocationForNewTaskWhenPlaceNotFound() = runTest {
        val task = Task(id = Task.NO_ID)
        task.putTransitory(Place.KEY, "missing-uid")
        `when`(locationDao.getPlace("missing-uid")).thenReturn(null)

        val result = locationDao.getLocation(task, preferences)

        assertNull(result)
    }

    @Test
    fun getLocationForExistingTaskWithGeofence() = runTest {
        val task = Task(id = 100)
        val place = Place(uid = "uid-1", name = "Office")
        val geofence = Geofence(task = 100, place = "uid-1", isArrival = true)
        `when`(locationDao.getGeofences(100)).thenReturn(Location(geofence, place))

        val result = locationDao.getLocation(task, preferences)

        assertNotNull(result)
        assertEquals("Office", result!!.place.name)
    }

    @Test
    fun getLocationForExistingTaskWithoutGeofence() = runTest {
        val task = Task(id = 100)
        `when`(locationDao.getGeofences(100)).thenReturn(null)

        val result = locationDao.getLocation(task, preferences)

        assertNull(result)
    }

    // =============================================
    // CaldavAccount — type checks
    // =============================================

    @Test
    fun isGoogleTasksAccount() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        assertTrue(account.isGoogleTasks)
    }

    @Test
    fun isCaldavAccount() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertTrue(account.isCaldavAccount)
    }

    @Test
    fun isEtebaseAccount() {
        val account = CaldavAccount(accountType = TYPE_ETEBASE)
        assertTrue(account.isEtebaseAccount)
    }

    @Test
    fun isTasksOrgAccount() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertTrue(account.isTasksOrg)
    }

    @Test
    fun isMicrosoftAccount() {
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        assertTrue(account.isMicrosoft)
    }

    @Test
    fun isLocalAccount() {
        val account = CaldavAccount(accountType = TYPE_LOCAL)
        assertTrue(account.isLocalList)
    }

    @Test
    fun isOpenTasksAccount() {
        val account = CaldavAccount(accountType = TYPE_OPENTASKS)
        assertTrue(account.isOpenTasks)
    }

    @Test
    fun isNotGoogleTasksWhenCaldav() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertFalse(account.isGoogleTasks)
    }

    @Test
    fun isNotCaldavWhenGoogleTasks() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        assertFalse(account.isCaldavAccount)
    }

    // =============================================
    // CaldavAccount — error states
    // =============================================

    @Test
    fun isLoggedOutWhenUnauthorized() {
        val account = CaldavAccount(
            accountType = TYPE_TASKS,
            error = ERROR_UNAUTHORIZED,
        )
        assertTrue(account.isLoggedOut())
    }

    @Test
    fun isNotLoggedOutWhenNoError() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertFalse(account.isLoggedOut())
    }

    @Test
    fun isPaymentRequired() {
        val account = CaldavAccount(
            accountType = TYPE_TASKS,
            error = ERROR_PAYMENT_REQUIRED,
        )
        assertTrue(account.isPaymentRequired())
    }

    @Test
    fun isNotPaymentRequiredWhenNoError() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertFalse(account.isPaymentRequired())
    }

    // =============================================
    // CaldavCalendar — readOnly
    // =============================================

    @Test
    fun calendarIsReadOnlyWhenAccessReadOnly() {
        val calendar = CaldavCalendar(access = CaldavCalendar.ACCESS_READ_ONLY)
        assertTrue(calendar.readOnly())
    }

    @Test
    fun calendarIsNotReadOnlyWithDefaultAccess() {
        val calendar = CaldavCalendar()
        assertFalse(calendar.readOnly())
    }

    @Test
    fun calendarIsNotReadOnlyWhenOwner() {
        val calendar = CaldavCalendar(access = CaldavCalendar.ACCESS_OWNER)
        assertFalse(calendar.readOnly())
    }

    @Test
    fun calendarIsNotReadOnlyWhenReadWrite() {
        val calendar = CaldavCalendar(access = CaldavCalendar.ACCESS_READ_WRITE)
        assertFalse(calendar.readOnly())
    }

    // =============================================
    // CaldavCalendar — calendarUri
    // =============================================

    @Test
    fun calendarUriExtractsLastSegment() {
        val calendar = CaldavCalendar(url = "https://example.com/calendars/personal/")
        assertEquals("personal", calendar.calendarUri)
    }

    @Test
    fun calendarUriWithoutTrailingSlash() {
        val calendar = CaldavCalendar(url = "https://example.com/calendars/work")
        assertEquals("work", calendar.calendarUri)
    }

    @Test
    fun calendarUriNullReturnsNull() {
        val calendar = CaldavCalendar(url = null)
        assertNull(calendar.calendarUri)
    }

    @Test
    fun calendarUriEmptyReturnsNull() {
        val calendar = CaldavCalendar(url = "")
        assertNull(calendar.calendarUri)
    }

    @Test
    fun calendarUriSingleSegment() {
        val calendar = CaldavCalendar(url = "personal")
        assertEquals("personal", calendar.calendarUri)
    }

    @Test
    fun calendarUriMultipleSegments() {
        val calendar = CaldavCalendar(url = "https://host.com/a/b/c/d/")
        assertEquals("d", calendar.calendarUri)
    }

    // =============================================
    // TagFilters and LocationFilters — toFilter extensions
    // =============================================

    @Test
    fun tagFiltersToTagFilter() {
        val tagData = TagData(name = "Work", remoteId = "tag-uuid")
        val tagFilters = TagFilters(tagData = tagData, count = 10)

        val filter = tagFilters.toTagFilter()

        assertEquals("Work", filter.tagData.name)
        assertEquals("tag-uuid", filter.tagData.remoteId)
        assertEquals(10, filter.count)
    }

    @Test
    fun tagFiltersWithZeroCount() {
        val tagData = TagData(name = "Empty")
        val tagFilters = TagFilters(tagData = tagData, count = 0)

        assertEquals(0, tagFilters.toTagFilter().count)
    }

    @Test
    fun locationFiltersToLocationFilter() {
        val place = Place(uid = "uid-1", name = "Home", latitude = 40.0, longitude = -74.0)
        val locationFilters = LocationFilters(place = place, count = 5)

        val filter = locationFilters.toLocationFilter()

        assertEquals("Home", filter.place.name)
        assertEquals("uid-1", filter.place.uid)
        assertEquals(5, filter.count)
    }

    @Test
    fun locationFiltersWithZeroCount() {
        val place = Place(uid = "uid", name = "Nowhere")
        val locationFilters = LocationFilters(place = place, count = 0)

        assertEquals(0, locationFilters.toLocationFilter().count)
    }

    @Test
    fun locationFiltersPreservesCoordinates() {
        val place = Place(uid = "uid", name = "Precise", latitude = 51.5074, longitude = -0.1278)
        val locationFilters = LocationFilters(place = place, count = 1)

        val filter = locationFilters.toLocationFilter()

        assertEquals(51.5074, filter.place.latitude, 0.0001)
        assertEquals(-0.1278, filter.place.longitude, 0.0001)
    }

    // =============================================
    // CaldavAccount companion — string extension functions
    // =============================================

    @Test
    fun openTaskTypeExtractsPrefix() {
        assertEquals("bitfire.at.davdroid", "bitfire.at.davdroid:account_name".openTaskType())
    }

    @Test
    fun openTaskTypeWithNoColon() {
        assertEquals("some_type", "some_type".openTaskType())
    }

    @Test
    fun openTaskTypeNullReturnsNull() {
        assertNull((null as String?).openTaskType())
    }

    @Test
    fun isDavx5True() {
        assertTrue("bitfire.at.davdroid:account".isDavx5())
    }

    @Test
    fun isDavx5False() {
        assertFalse("other:account".isDavx5())
    }

    @Test
    fun isDavx5Null() {
        assertFalse((null as String?).isDavx5())
    }

    @Test
    fun isDavx5ManagedTrue() {
        assertTrue("com.davdroid:account".isDavx5Managed())
    }

    @Test
    fun isDavx5ManagedFalse() {
        assertFalse("other:account".isDavx5Managed())
    }

    @Test
    fun isEteSyncTrue() {
        assertTrue("com.etesync.syncadapter:account".isEteSync())
    }

    @Test
    fun isEteSyncFalse() {
        assertFalse("other".isEteSync())
    }

    @Test
    fun isDecSyncTrue() {
        assertTrue("org.decsync.tasks:account".isDecSync())
    }

    @Test
    fun isDecSyncFalse() {
        assertFalse("other".isDecSync())
    }

    @Test
    fun isPaymentRequiredStringTrue() {
        assertTrue("HTTP 402".isPaymentRequired())
    }

    @Test
    fun isPaymentRequiredStringFalse() {
        assertFalse("HTTP 500".isPaymentRequired())
    }

    @Test
    fun isPaymentRequiredStringNull() {
        assertFalse((null as String?).isPaymentRequired())
    }

    @Test
    fun isTosRequiredTrue() {
        assertTrue("HTTP 451".isTosRequired())
    }

    @Test
    fun isTosRequiredFalse() {
        assertFalse("HTTP 200".isTosRequired())
    }

    @Test
    fun isTosRequiredNull() {
        assertFalse((null as String?).isTosRequired())
    }

    // =============================================
    // CaldavAccount — composeIcon coverage
    // =============================================

    @Test
    fun composeIconForTasksOrg() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertNotNull(account.composeIcon)
    }

    @Test
    fun composeIconForCaldav() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertNotNull(account.composeIcon)
    }

    @Test
    fun composeIconForEtebase() {
        val account = CaldavAccount(accountType = TYPE_ETEBASE)
        assertNotNull(account.composeIcon)
    }

    @Test
    fun composeIconForMicrosoft() {
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        assertNotNull(account.composeIcon)
    }

    @Test
    fun composeIconForGoogleTasks() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        assertNotNull(account.composeIcon)
    }

    @Test
    fun composeIconForLocal() {
        val account = CaldavAccount(accountType = TYPE_LOCAL)
        assertNotNull(account.composeIcon)
    }

    @Test
    fun composeIconForDavx5OpenTasks() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "bitfire.at.davdroid:account"
        )
        assertNotNull(account.composeIcon)
    }

    @Test
    fun composeIconForDavx5ManagedOpenTasks() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "com.davdroid:account"
        )
        assertNotNull(account.composeIcon)
    }

    @Test
    fun composeIconForDecSyncOpenTasks() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "org.decsync.tasks:account"
        )
        assertNotNull(account.composeIcon)
    }

    @Test
    fun composeIconForEteSyncOpenTasks() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "com.etesync.syncadapter:account"
        )
        assertNotNull(account.composeIcon)
    }

    // =============================================
    // CaldavAccount — openTaskApp
    // =============================================

    @Test
    fun openTaskAppForDavx5() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "bitfire.at.davdroid:account"
        )
        assertNotNull(account.openTaskApp)
        assertEquals("DAVx\u2075", account.openTaskApp!!.name)
    }

    @Test
    fun openTaskAppForDecSync() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "org.decsync.tasks:account"
        )
        assertNotNull(account.openTaskApp)
        assertEquals("DecSync CC", account.openTaskApp!!.name)
    }

    @Test
    fun openTaskAppNullForUnknownUuid() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "unknown:account"
        )
        assertNull(account.openTaskApp)
    }

    @Test
    fun openTaskAppNullForNullUuid() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = null
        )
        assertNull(account.openTaskApp)
    }

    @Test
    fun openTaskAppNullForTasksAccount() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertNull(account.openTaskApp)
    }
}
