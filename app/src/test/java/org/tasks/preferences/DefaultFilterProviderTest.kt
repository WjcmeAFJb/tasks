package org.tasks.preferences

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.R
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.NotificationsFilter
import org.tasks.filters.PlaceFilter
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.filters.SnoozedFilter
import org.tasks.filters.TagFilter
import org.tasks.filters.TodayFilter

class DefaultFilterProviderTest {

    private lateinit var preferences: Preferences
    private lateinit var filterDao: FilterDao
    private lateinit var tagDataDao: TagDataDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var locationDao: LocationDao
    private lateinit var provider: DefaultFilterProvider

    @Before
    fun setUp() {
        preferences = mock(Preferences::class.java)
        filterDao = mock(FilterDao::class.java)
        tagDataDao = mock(TagDataDao::class.java)
        caldavDao = mock(CaldavDao::class.java)
        locationDao = mock(LocationDao::class.java)
        provider = DefaultFilterProvider(preferences, filterDao, tagDataDao, caldavDao, locationDao)
    }

    // --- getFilterPreferenceValue ---

    @Test
    fun filterPreferenceValueForMyTasksFilter() {
        val filter = MyTasksFilter(title = "My Tasks")
        val result = provider.getFilterPreferenceValue(filter)
        assertEquals("0:0", result)
    }

    @Test
    fun filterPreferenceValueForTodayFilter() {
        val filter = TodayFilter(title = "Today")
        val result = provider.getFilterPreferenceValue(filter)
        assertEquals("0:1", result)
    }

    @Test
    fun filterPreferenceValueForRecentlyModifiedFilter() {
        val filter = RecentlyModifiedFilter(title = "Recently Modified")
        val result = provider.getFilterPreferenceValue(filter)
        assertEquals("0:3", result)
    }

    @Test
    fun filterPreferenceValueForSnoozedFilter() {
        val filter = SnoozedFilter(title = "Snoozed")
        val result = provider.getFilterPreferenceValue(filter)
        assertEquals("0:4", result)
    }

    @Test
    fun filterPreferenceValueForNotificationsFilter() {
        val filter = NotificationsFilter(title = "Notifications")
        val result = provider.getFilterPreferenceValue(filter)
        assertEquals("0:5", result)
    }

    @Test
    fun filterPreferenceValueForCustomFilter() {
        val filter = CustomFilter(
            org.tasks.data.entity.Filter(id = 42, title = "My Filter", sql = "SELECT 1")
        )
        val result = provider.getFilterPreferenceValue(filter)
        assertEquals("1:42", result)
    }

    @Test
    fun filterPreferenceValueForTagFilter() {
        val tag = TagData(id = 1, remoteId = "tag-uuid-123", name = "Work")
        val filter = TagFilter(tagData = tag)
        val result = provider.getFilterPreferenceValue(filter)
        assertEquals("2:tag-uuid-123", result)
    }

    @Test
    fun filterPreferenceValueForCaldavFilter() {
        val calendar = CaldavCalendar(uuid = "cal-uuid-456", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        val filter = CaldavFilter(calendar = calendar, account = account)
        val result = provider.getFilterPreferenceValue(filter)
        assertEquals("4:cal-uuid-456", result)
    }

    @Test
    fun filterPreferenceValueForPlaceFilter() {
        val place = Place(uid = "place-uid-789", name = "Office")
        val filter = PlaceFilter(place = place)
        val result = provider.getFilterPreferenceValue(filter)
        assertEquals("5:place-uid-789", result)
    }

    // --- getDefaultList ---

    @Test
    fun getDefaultListReturnsWritableCaldavFilter() = runTest {
        val calendar = CaldavCalendar(
            uuid = "cal-uuid",
            account = "acc-uuid",
            access = CaldavCalendar.ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("4:cal-uuid")
        `when`(caldavDao.getCalendarByUuid("cal-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getDefaultList()

        assertEquals("cal-uuid", result.uuid)
    }

    @Test
    fun getDefaultListSkipsReadOnlyAndFallsBackToAnyList() = runTest {
        val readOnlyCalendar = CaldavCalendar(
            uuid = "read-only-uuid",
            account = "acc-uuid",
            access = CaldavCalendar.ACCESS_READ_ONLY,
        )
        val writableCalendar = CaldavCalendar(
            uuid = "writable-uuid",
            account = "acc-uuid",
            access = CaldavCalendar.ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("4:read-only-uuid")
        `when`(caldavDao.getCalendarByUuid("read-only-uuid")).thenReturn(readOnlyCalendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(writableCalendar))

        val result = provider.getDefaultList()

        assertEquals("writable-uuid", result.uuid)
    }

    // --- getList ---

    @Test
    fun getListForNewTaskWithGoogleTaskTransitory() = runTest {
        val task = org.tasks.data.entity.Task().apply {
            putTransitory("gtasks", "list-uuid")
        }
        val calendar = CaldavCalendar(uuid = "list-uuid", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getCalendarByUuid("list-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getList(task)

        assertEquals("list-uuid", result.uuid)
    }

    @Test
    fun getListForNewTaskWithCaldavTransitory() = runTest {
        val task = org.tasks.data.entity.Task().apply {
            putTransitory("caldav", "caldav-list-uuid")
        }
        val calendar = CaldavCalendar(
            uuid = "caldav-list-uuid",
            account = "acc-uuid",
            access = CaldavCalendar.ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getCalendarByUuid("caldav-list-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getList(task)

        assertEquals("caldav-list-uuid", result.uuid)
    }

    @Test
    fun getListForNewTaskWithReadOnlyCaldavTransitoryFallsBackToDefault() = runTest {
        val task = org.tasks.data.entity.Task().apply {
            putTransitory("caldav", "read-only-uuid")
        }
        val readOnlyCalendar = CaldavCalendar(
            uuid = "read-only-uuid",
            account = "acc-uuid",
            access = CaldavCalendar.ACCESS_READ_ONLY,
        )
        val writableCalendar = CaldavCalendar(
            uuid = "writable-uuid",
            account = "acc-uuid",
            access = CaldavCalendar.ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getCalendarByUuid("read-only-uuid")).thenReturn(readOnlyCalendar)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(writableCalendar))
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)

        val result = provider.getList(task)

        assertEquals("writable-uuid", result.uuid)
    }

    @Test
    fun getListForExistingTaskWithCaldavTask() = runTest {
        val task = org.tasks.data.entity.Task(id = 42)
        val caldavTask = org.tasks.data.entity.CaldavTask(
            task = 42,
            calendar = "cal-uuid",
        )
        val calendar = CaldavCalendar(uuid = "cal-uuid", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getTask(42L)).thenReturn(caldavTask)
        `when`(caldavDao.getCalendarByUuid("cal-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getList(task)

        assertEquals("cal-uuid", result.uuid)
    }

    @Test
    fun getListForExistingTaskWithNoCaldavTaskFallsBackToWritableList() = runTest {
        val task = org.tasks.data.entity.Task(id = 42)
        val writableCalendar = CaldavCalendar(
            uuid = "writable-uuid",
            account = "acc-uuid",
            access = CaldavCalendar.ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getTask(42L)).thenReturn(null)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(writableCalendar))
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)

        val result = provider.getList(task)

        assertEquals("writable-uuid", result.uuid)
    }

    @Test
    fun getListForExistingTaskWithCalendarButNoAccount() = runTest {
        val task = org.tasks.data.entity.Task(id = 42)
        val caldavTask = org.tasks.data.entity.CaldavTask(task = 42, calendar = "cal-uuid")
        val calendar = CaldavCalendar(uuid = "cal-uuid", account = "acc-uuid")
        val writableCalendar = CaldavCalendar(
            uuid = "writable-uuid",
            account = "w-acc-uuid",
            access = CaldavCalendar.ACCESS_OWNER,
        )
        val wAccount = CaldavAccount(uuid = "w-acc-uuid")
        `when`(caldavDao.getTask(42L)).thenReturn(caldavTask)
        `when`(caldavDao.getCalendarByUuid("cal-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(null)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(writableCalendar))
        `when`(caldavDao.getAccountByUuid("w-acc-uuid")).thenReturn(wAccount)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)

        val result = provider.getList(task)

        // Falls back to default since account is null
        assertEquals("writable-uuid", result.uuid)
    }

    @Test
    fun getListForNewTaskWithNoTransitoryFallsBackToDefault() = runTest {
        val task = org.tasks.data.entity.Task() // new task, no transitory
        val writableCalendar = CaldavCalendar(
            uuid = "default-uuid",
            account = "acc-uuid",
            access = CaldavCalendar.ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getCalendars()).thenReturn(listOf(writableCalendar))
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)

        val result = provider.getList(task)

        assertEquals("default-uuid", result.uuid)
    }

    // --- setBadgeFilter / setLastViewedFilter / setDefaultOpenFilter ---

    @Test
    fun setBadgeFilterSetsPreference() {
        val filter = MyTasksFilter(title = "My Tasks")
        provider.setBadgeFilter(filter)
        verify(preferences).setString(R.string.p_badge_list, "0:0")
    }

    @Test
    fun setLastViewedFilterSetsPreference() {
        val filter = TodayFilter(title = "Today")
        provider.setLastViewedFilter(filter)
        verify(preferences).setString(R.string.p_last_viewed_list, "0:1")
    }

    @Test
    fun setDefaultOpenFilterSetsPreference() {
        val filter = SnoozedFilter(title = "Snoozed")
        provider.setDefaultOpenFilter(filter)
        verify(preferences).setString(R.string.p_default_open_filter, "0:4")
    }

    @Test
    fun setBadgeFilterForCustomFilter() {
        val filter = CustomFilter(
            org.tasks.data.entity.Filter(id = 10, title = "My Custom", sql = "sql")
        )
        provider.setBadgeFilter(filter)
        verify(preferences).setString(R.string.p_badge_list, "1:10")
    }

    @Test
    fun setBadgeFilterForTagFilter() {
        val tag = TagData(id = 1, remoteId = "t-uuid", name = "Tag")
        provider.setBadgeFilter(TagFilter(tagData = tag))
        verify(preferences).setString(R.string.p_badge_list, "2:t-uuid")
    }

    @Test
    fun setBadgeFilterForCaldavFilter() {
        val calendar = CaldavCalendar(uuid = "c-uuid", account = "a-uuid")
        val account = CaldavAccount(uuid = "a-uuid")
        provider.setBadgeFilter(CaldavFilter(calendar = calendar, account = account))
        verify(preferences).setString(R.string.p_badge_list, "4:c-uuid")
    }

    @Test
    fun setBadgeFilterForPlaceFilter() {
        val place = Place(uid = "p-uuid", name = "Place")
        provider.setBadgeFilter(PlaceFilter(place = place))
        verify(preferences).setString(R.string.p_badge_list, "5:p-uuid")
    }

    @Test
    fun setLastViewedForRecentlyModified() {
        provider.setLastViewedFilter(RecentlyModifiedFilter(title = "Recent"))
        verify(preferences).setString(R.string.p_last_viewed_list, "0:3")
    }

    @Test
    fun setLastViewedForNotifications() {
        provider.setLastViewedFilter(NotificationsFilter(title = "Notifications"))
        verify(preferences).setString(R.string.p_last_viewed_list, "0:5")
    }

    // --- CaldavFilter properties ---

    @Test
    fun caldavFilterIsReadOnlyWhenCalendarIsReadOnly() {
        val calendar = CaldavCalendar(
            uuid = "uuid",
            account = "acc",
            access = CaldavCalendar.ACCESS_READ_ONLY,
        )
        val account = CaldavAccount(uuid = "acc")
        val filter = CaldavFilter(calendar = calendar, account = account)
        assertTrue(filter.isReadOnly)
    }

    @Test
    fun caldavFilterIsWritableWhenCalendarIsOwner() {
        val calendar = CaldavCalendar(
            uuid = "uuid",
            account = "acc",
            access = CaldavCalendar.ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc")
        val filter = CaldavFilter(calendar = calendar, account = account)
        assertTrue(filter.isWritable)
    }

    @Test
    fun caldavFilterIsGoogleTasksWhenAccountIsGoogleTasks() {
        val calendar = CaldavCalendar(uuid = "uuid", account = "acc")
        val account = CaldavAccount(
            uuid = "acc",
            accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
        )
        val filter = CaldavFilter(calendar = calendar, account = account)
        assertTrue(filter.isGoogleTasks)
    }

    @Test
    fun caldavFilterIsIcalendarForCaldavAccount() {
        val calendar = CaldavCalendar(uuid = "uuid", account = "acc")
        val account = CaldavAccount(
            uuid = "acc",
            accountType = CaldavAccount.TYPE_CALDAV,
        )
        val filter = CaldavFilter(calendar = calendar, account = account)
        assertTrue(filter.isIcalendar)
    }

    @Test
    fun caldavFilterIsNotIcalendarForGoogleTasks() {
        val calendar = CaldavCalendar(uuid = "uuid", account = "acc")
        val account = CaldavAccount(
            uuid = "acc",
            accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
        )
        val filter = CaldavFilter(calendar = calendar, account = account)
        assertTrue(!filter.isIcalendar)
    }

    @Test
    fun caldavFilterIsNotIcalendarForMicrosoft() {
        val calendar = CaldavCalendar(uuid = "uuid", account = "acc")
        val account = CaldavAccount(
            uuid = "acc",
            accountType = CaldavAccount.TYPE_MICROSOFT,
        )
        val filter = CaldavFilter(calendar = calendar, account = account)
        assertTrue(!filter.isIcalendar)
    }
}
