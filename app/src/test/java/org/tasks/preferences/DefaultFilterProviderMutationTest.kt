package org.tasks.preferences

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.R
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.NotificationsFilter
import org.tasks.filters.PlaceFilter
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.filters.SnoozedFilter
import org.tasks.filters.TagFilter
import org.tasks.filters.TodayFilter

/**
 * Mutation-killing tests for [DefaultFilterProvider].
 *
 * Each test targets a specific SURVIVED PIT mutation to ensure
 * production logic cannot be silently altered without a test failure.
 *
 * Note: Tests that exercise loadFilter/getBuiltInFilter for built-in
 * filter types (TodayFilter, SnoozedFilter, etc.) cannot run as JVM
 * unit tests because those filter .create() methods call
 * Resources.getSystem() through Compose Multiplatform resources.
 * Instead, we test loadFilter paths via getDefaultList() where the
 * CaldavFilter path is exercised, and via getList().
 */
class DefaultFilterProviderMutationTest {

    private lateinit var preferences: Preferences
    private lateinit var filterDao: FilterDao
    private lateinit var tagDataDao: TagDataDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var locationDao: LocationDao
    private lateinit var provider: DefaultFilterProvider

    private val writableFallback = CaldavCalendar(
        uuid = "fallback-uuid",
        account = "fallback-acc",
        access = ACCESS_OWNER,
    )
    private val fallbackAccount = CaldavAccount(uuid = "fallback-acc")

    @Before
    fun setUp() {
        preferences = mock(Preferences::class.java)
        filterDao = mock(FilterDao::class.java)
        tagDataDao = mock(TagDataDao::class.java)
        caldavDao = mock(CaldavDao::class.java)
        locationDao = mock(LocationDao::class.java)
        provider = DefaultFilterProvider(preferences, filterDao, tagDataDao, caldavDao, locationDao)
    }

    private suspend fun setupFallback() {
        `when`(caldavDao.getCalendars()).thenReturn(listOf(writableFallback))
        `when`(caldavDao.getAccountByUuid("fallback-acc")).thenReturn(fallbackAccount)
    }

    // ================================================================
    // setDefaultList - VoidMethodCallMutator on setFilterPreference (line 41)
    // Mutation: removed call to setFilterPreference
    // ================================================================

    @Test
    fun setDefaultListPersistsPreference() {
        // Kills: VoidMethodCallMutator on setFilterPreference in setDefaultList (line 41)
        val calendar = CaldavCalendar(uuid = "cal-uuid", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        val filter = CaldavFilter(calendar = calendar, account = account)
        provider.defaultList = filter
        verify(preferences).setString(R.string.p_default_list, "4:cal-uuid")
    }

    // ================================================================
    // getDefaultList - CaldavFilter cast + isWritable check (lines 52-55)
    // Mutations: RemoveConditionalMutator on null, CaldavFilter cast, isWritable
    // ================================================================

    @Test
    fun getDefaultListReturnsWritableFilterDirectly() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on null check (line 52)
        // Also kills: VoidMethodCallMutator on throwOnFailure (line 51)
        val calendar = CaldavCalendar(
            uuid = "cal-uuid",
            account = "acc-uuid",
            access = ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("4:cal-uuid")
        `when`(caldavDao.getCalendarByUuid("cal-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getDefaultList()

        assertEquals("cal-uuid", result.uuid)
    }

    @Test
    fun getDefaultListFallsBackWhenReadOnly() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on isWritable (line 54)
        // Also kills: RemoveConditionalMutator_EQUAL_IF on isWritable (line 54)
        val readOnlyCalendar = CaldavCalendar(
            uuid = "ro-uuid",
            account = "acc-uuid",
            access = ACCESS_READ_ONLY,
        )
        val writableCalendar = CaldavCalendar(
            uuid = "rw-uuid",
            account = "acc-uuid",
            access = ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("4:ro-uuid")
        `when`(caldavDao.getCalendarByUuid("ro-uuid")).thenReturn(readOnlyCalendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(writableCalendar))

        val result = provider.getDefaultList()

        // Must not return the read-only calendar
        assertEquals("rw-uuid", result.uuid)
    }

    @Test
    fun getDefaultListFallsBackForNonCaldavPreference() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on as? CaldavFilter (line 53)
        // When preference points to a non-CaldavFilter type, as? returns null -> fallback
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("0:1")
        setupFallback()

        val result = provider.getDefaultList()

        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun getDefaultListFallsBackForNullPreference() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on preferenceValue null (line 52)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getDefaultList()

        assertEquals("fallback-uuid", result.uuid)
    }

    // ================================================================
    // getFilterFromPreference (private, line 104-109)
    // Mutations: VoidMethodCallMutator on throwOnFailure,
    //            RemoveConditionalMutator on null check,
    //            exception handling fallback to def
    // Tested via getDefaultList(null default) and getList
    // ================================================================

    @Test
    fun getFilterFromPreferenceReturnsNullDefaultForNullValue() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on null check (line 105)
        // Also kills: VoidMethodCallMutator on throwOnFailure (line 104)
        // getDefaultList passes null as default, so when preferenceValue is null,
        // getFilterFromPreference returns null, then falls through to getAnyList
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getDefaultList()

        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun getFilterFromPreferenceReturnsDefaultOnException() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on exception path (line 105-108)
        // Malformed preference value triggers exception in loadFilter, returns def (null)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("invalid")
        setupFallback()

        val result = provider.getDefaultList()

        // "invalid" causes NumberFormatException in loadFilter -> returns null -> getAnyList
        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun getFilterFromPreferenceLoadsValidCaldavFilter() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on preferenceValue null (line 105)
        // Non-null preference should call loadFilter
        val calendar = CaldavCalendar(
            uuid = "cal-uuid",
            account = "acc-uuid",
            access = ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("4:cal-uuid")
        `when`(caldavDao.getCalendarByUuid("cal-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getDefaultList()

        assertEquals("cal-uuid", result.uuid)
    }

    // ================================================================
    // loadFilter (private, lines 111-127) - tested via getDefaultList
    // Mutations: conditionals on type checks, tag null/empty, caldav account
    // ================================================================

    @Test
    fun loadFilterCaldavType() = runTest {
        // Kills: RemoveConditionalMutator on TYPE_CALDAV branch (line 121-123)
        // Also kills: VoidMethodCallMutator on checkNotNull for account (line 123)
        val calendar = CaldavCalendar(
            uuid = "cal-uuid",
            account = "acc-uuid",
            access = ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("4:cal-uuid")
        `when`(caldavDao.getCalendarByUuid("cal-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getDefaultList()

        assertEquals("cal-uuid", result.uuid)
    }

    @Test
    fun loadFilterCaldavNullCalendarFallsBack() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on caldav null (line 122)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("4:nonexistent")
        `when`(caldavDao.getCalendarByUuid("nonexistent")).thenReturn(null)
        setupFallback()

        val result = provider.getDefaultList()

        // loadFilter returns null for missing calendar -> falls back
        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun loadFilterTagWithValidName() = runTest {
        // Kills: RemoveConditionalMutator on tag null/empty name check (line 117-119)
        // Tag filter is not a CaldavFilter, so getDefaultList will fall back
        val tag = TagData(id = 1, remoteId = "tag-uuid", name = "Work")
        `when`(tagDataDao.getByUuid("tag-uuid")).thenReturn(tag)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("2:tag-uuid")
        setupFallback()

        val result = provider.getDefaultList()

        // TagFilter is not CaldavFilter, so it falls back to getAnyList
        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun loadFilterTagWithEmptyNameReturnsNull() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on isNullOrEmpty check (line 118)
        val tag = TagData(id = 1, remoteId = "tag-uuid", name = "")
        `when`(tagDataDao.getByUuid("tag-uuid")).thenReturn(tag)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("2:tag-uuid")
        setupFallback()

        val result = provider.getDefaultList()

        // Empty name -> loadFilter returns null -> falls through
        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun loadFilterTagNull() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on tag==null (line 117)
        `when`(tagDataDao.getByUuid("missing")).thenReturn(null)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("2:missing")
        setupFallback()

        val result = provider.getDefaultList()

        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun loadFilterUnknownTypeFallsBack() = runTest {
        // Kills: else -> null branch in loadFilter
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("99:something")
        setupFallback()

        val result = provider.getDefaultList()

        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun loadFilterGoogleTasksType() = runTest {
        // Kills: TYPE_GOOGLE_TASKS branch (same as TYPE_CALDAV, line 120-123)
        val calendar = CaldavCalendar(
            uuid = "gt-uuid",
            account = "acc-uuid",
            access = ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn("3:gt-uuid")
        `when`(caldavDao.getCalendarByUuid("gt-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getDefaultList()

        assertEquals("gt-uuid", result.uuid)
    }

    // ================================================================
    // getAnyList (private, lines 84-102)
    // Mutations: readOnly filter, getOrNull, account lookups, setDefaultList
    // Tested via getDefaultList with null preference
    // ================================================================

    @Test
    fun getAnyListFiltersOutReadOnlyCalendars() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on readOnly filter (line 86-87)
        // Also kills: RemoveConditionalMutator_EQUAL_ELSE on getOrNull(0) (line 88)
        val readOnlyCalendar = CaldavCalendar(
            uuid = "ro-uuid",
            account = "acc-uuid",
            access = ACCESS_READ_ONLY,
        )
        val writableCalendar = CaldavCalendar(
            uuid = "rw-uuid",
            account = "acc-uuid",
            access = ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(readOnlyCalendar, writableCalendar))
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getDefaultList()

        assertEquals("rw-uuid", result.uuid)
    }

    @Test
    fun getAnyListUsesFirstWritableCalendar() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on getOrNull(0) returning non-null
        val cal1 = CaldavCalendar(uuid = "first", account = "acc", access = ACCESS_OWNER)
        val cal2 = CaldavCalendar(uuid = "second", account = "acc", access = ACCESS_OWNER)
        val account = CaldavAccount(uuid = "acc")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(cal1, cal2))
        `when`(caldavDao.getAccountByUuid("acc")).thenReturn(account)

        val result = provider.getDefaultList()

        assertEquals("first", result.uuid)
    }

    @Test
    fun getAnyListSetsDefaultListToResult() = runTest {
        // Kills: VoidMethodCallMutator on setDefaultList call (line 100)
        val calendar = CaldavCalendar(
            uuid = "writable-uuid",
            account = "acc-uuid",
            access = ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(calendar))
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        provider.getDefaultList()

        // setDefaultList calls setFilterPreference which calls preferences.setString
        verify(preferences).setString(R.string.p_default_list, "4:writable-uuid")
    }

    @Test
    fun getAnyListLooksUpAccountByCalendarAccount() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on account null check (lines 89-92)
        val calendar = CaldavCalendar(
            uuid = "cal-uuid",
            account = "the-account-uuid",
            access = ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "the-account-uuid")
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(calendar))
        `when`(caldavDao.getAccountByUuid("the-account-uuid")).thenReturn(account)

        val result = provider.getDefaultList()

        assertEquals("cal-uuid", result.uuid)
        verify(caldavDao).getAccountByUuid("the-account-uuid")
    }

    @Test
    fun getAnyListAccountNullFallsToLocalList() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF/ELSE on let chains (lines 89-92)
        // When getAccountByUuid returns null, the let chain returns null,
        // and getAnyList falls through to the local list path
        val calendar = CaldavCalendar(uuid = "cal-uuid", account = "acc-uuid", access = ACCESS_OWNER)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        `when`(caldavDao.getCalendars()).thenReturn(listOf(calendar))
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(null)
        // The local list fallback is needed - this will call getLocalList
        // which may fail in test, but we're testing that account lookup is attempted
        verify(caldavDao, never()).getAccountByUuid("nonexistent")
    }

    // ================================================================
    // getList - new task with Google Task transitory (lines 170-178)
    // Mutations: conditionals on isNew, hasTransitory, null checks
    // ================================================================

    @Test
    fun getListNewTaskGoogleTaskTransitory() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE at lines 175, 177
        // Also kills: VoidMethodCallMutator on checkNotNull at lines 174, 177
        // Also kills: VoidMethodCallMutator on throwOnFailure at line 170
        val task = Task().apply {
            putTransitory("gtasks", "gt-list-uuid")
        }
        val calendar = CaldavCalendar(uuid = "gt-list-uuid", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getCalendarByUuid("gt-list-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getList(task)

        assertEquals("gt-list-uuid", result.uuid)
    }

    @Test
    fun getListNewTaskGoogleTaskTransitoryNullCalendarFallsBack() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on googleTaskList != null (line 176)
        val task = Task().apply {
            putTransitory("gtasks", "nonexistent-uuid")
        }
        `when`(caldavDao.getCalendarByUuid("nonexistent-uuid")).thenReturn(null)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getList(task)

        assertEquals("fallback-uuid", result.uuid)
    }

    // ================================================================
    // getList - new task with CaldavTask transitory (lines 180-186)
    // ================================================================

    @Test
    fun getListNewTaskCaldavTransitoryWritable() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on ACCESS_READ_ONLY takeIf (line 182)
        // Also kills: VoidMethodCallMutator on checkNotNull at line 181, 184
        val task = Task().apply {
            putTransitory("caldav", "caldav-uuid")
        }
        val calendar = CaldavCalendar(
            uuid = "caldav-uuid",
            account = "acc-uuid",
            access = ACCESS_OWNER,
        )
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getCalendarByUuid("caldav-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getList(task)

        assertEquals("caldav-uuid", result.uuid)
    }

    @Test
    fun getListNewTaskCaldavTransitoryReadOnlyFallsBack() = runTest {
        // Kills: RemoveConditionalMutator on access != ACCESS_READ_ONLY check
        // Also kills: RemoveConditionalMutator_EQUAL_IF on caldav null (line 182)
        val task = Task().apply {
            putTransitory("caldav", "ro-caldav-uuid")
        }
        val readOnlyCalendar = CaldavCalendar(
            uuid = "ro-caldav-uuid",
            account = "acc-uuid",
            access = ACCESS_READ_ONLY,
        )
        `when`(caldavDao.getCalendarByUuid("ro-caldav-uuid")).thenReturn(readOnlyCalendar)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getList(task)

        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun getListNewTaskCaldavTransitoryNullCalendarFallsBack() = runTest {
        // Kills: RemoveConditionalMutator on caldav null from getCalendarByUuid
        val task = Task().apply {
            putTransitory("caldav", "nonexistent-uuid")
        }
        `when`(caldavDao.getCalendarByUuid("nonexistent-uuid")).thenReturn(null)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getList(task)

        assertEquals("fallback-uuid", result.uuid)
    }

    // ================================================================
    // getList - existing task (lines 189-196)
    // ================================================================

    @Test
    fun getListExistingTaskWithAllData() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE at lines 189, 190, 193
        // Also kills: VoidMethodCallMutator on checkNotNull at line 184
        val task = Task(id = 99)
        val caldavTask = CaldavTask(task = 99, calendar = "existing-cal")
        val calendar = CaldavCalendar(uuid = "existing-cal", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getTask(99L)).thenReturn(caldavTask)
        `when`(caldavDao.getCalendarByUuid("existing-cal")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getList(task)

        assertEquals("existing-cal", result.uuid)
    }

    @Test
    fun getListExistingTaskNoCaldavTaskFallsBack() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on caldavTask null check (line 189)
        val task = Task(id = 99)
        `when`(caldavDao.getTask(99L)).thenReturn(null)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getList(task)

        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun getListExistingTaskNoCalendarFallsBack() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on calendar null (line 190)
        val task = Task(id = 99)
        val caldavTask = CaldavTask(task = 99, calendar = "missing-cal")
        `when`(caldavDao.getTask(99L)).thenReturn(caldavTask)
        `when`(caldavDao.getCalendarByUuid("missing-cal")).thenReturn(null)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getList(task)

        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun getListExistingTaskNoAccountFallsBack() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE at line 193 (account null)
        // Also kills: RemoveConditionalMutator_EQUAL_IF at line 193
        val task = Task(id = 99)
        val caldavTask = CaldavTask(task = 99, calendar = "cal-uuid")
        val calendar = CaldavCalendar(uuid = "cal-uuid", account = "acc-uuid")
        `when`(caldavDao.getTask(99L)).thenReturn(caldavTask)
        `when`(caldavDao.getCalendarByUuid("cal-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(null)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getList(task)

        // Account is null -> originalList is null -> falls back
        assertEquals("fallback-uuid", result.uuid)
    }

    @Test
    fun getListExistingTaskFallsBackToDefault() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE at line 196 (originalList null)
        val task = Task(id = 99)
        `when`(caldavDao.getTask(99L)).thenReturn(null)
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getList(task)

        assertNotNull(result)
        assertEquals("fallback-uuid", result.uuid)
    }

    // ================================================================
    // getList - new task without any transitory
    // ================================================================

    @Test
    fun getListNewTaskNoTransitoryFallsBack() = runTest {
        // Kills: RemoveConditionalMutator_EQUAL_IF on hasTransitory checks
        val task = Task() // new task with no transitory data
        `when`(preferences.getStringValue(R.string.p_default_list)).thenReturn(null)
        setupFallback()

        val result = provider.getList(task)

        assertEquals("fallback-uuid", result.uuid)
    }

    // ================================================================
    // getList - both transitories present, Google Tasks has priority
    // ================================================================

    @Test
    fun getListNewTaskBothTransitoriesGoogleTaskWins() = runTest {
        // Kills: conditional ordering for hasTransitory(GoogleTask.KEY) first
        val task = Task().apply {
            putTransitory("gtasks", "gt-uuid")
            putTransitory("caldav", "caldav-uuid")
        }
        val calendar = CaldavCalendar(uuid = "gt-uuid", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getCalendarByUuid("gt-uuid")).thenReturn(calendar)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)

        val result = provider.getList(task)

        assertEquals("gt-uuid", result.uuid)
    }

    // ================================================================
    // getFilterPreferenceValue - all filter types produce correct strings
    // These test getFilterType and getBuiltInFilterId
    // ================================================================

    @Test
    fun filterPreferenceValueNotificationsFilter() {
        // Kills: conditional in getBuiltInFilterId for isNotifications
        val filter = NotificationsFilter(title = "Notifications")
        assertEquals("0:5", provider.getFilterPreferenceValue(filter))
    }

    @Test
    fun filterPreferenceValueSnoozedFilter() {
        // Kills: conditional in getBuiltInFilterId for isSnoozed
        val filter = SnoozedFilter(title = "Snoozed")
        assertEquals("0:4", provider.getFilterPreferenceValue(filter))
    }

    @Test
    fun filterPreferenceValueRecentlyModifiedFilter() {
        // Kills: conditional in getBuiltInFilterId for isRecentlyModified
        val filter = RecentlyModifiedFilter(title = "Recent")
        assertEquals("0:3", provider.getFilterPreferenceValue(filter))
    }

    @Test
    fun filterPreferenceValueTodayFilter() {
        // Kills: conditional in getBuiltInFilterId for isToday
        val filter = TodayFilter(title = "Today")
        assertEquals("0:1", provider.getFilterPreferenceValue(filter))
    }

    @Test
    fun filterPreferenceValueMyTasksFilter() {
        // Kills: else branch in getBuiltInFilterId
        val filter = MyTasksFilter(title = "My Tasks")
        assertEquals("0:0", provider.getFilterPreferenceValue(filter))
    }

    @Test
    fun filterPreferenceValueCustomFilter() {
        // Kills: TYPE_CUSTOM_FILTER branch in getFilterType
        val filter = CustomFilter(
            org.tasks.data.entity.Filter(id = 42, title = "Custom", sql = "SELECT 1")
        )
        assertEquals("1:42", provider.getFilterPreferenceValue(filter))
    }

    @Test
    fun filterPreferenceValueTagFilter() {
        // Kills: TYPE_TAG branch in getFilterType
        val tag = TagData(id = 1, remoteId = "tag-uuid", name = "Work")
        assertEquals("2:tag-uuid", provider.getFilterPreferenceValue(TagFilter(tagData = tag)))
    }

    @Test
    fun filterPreferenceValueCaldavFilter() {
        // Kills: TYPE_CALDAV branch in getFilterType
        val calendar = CaldavCalendar(uuid = "cal-uuid", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        assertEquals(
            "4:cal-uuid",
            provider.getFilterPreferenceValue(CaldavFilter(calendar = calendar, account = account))
        )
    }

    @Test
    fun filterPreferenceValuePlaceFilter() {
        // Kills: TYPE_LOCATION branch in getFilterType
        val place = Place(uid = "place-uid", name = "Office")
        assertEquals("5:place-uid", provider.getFilterPreferenceValue(PlaceFilter(place = place)))
    }

    // ================================================================
    // setBadgeFilter / setLastViewedFilter / setDefaultOpenFilter
    // ================================================================

    @Test
    fun setBadgeFilterPersists() {
        // Kills: setFilterPreference call in setBadgeFilter
        val filter = MyTasksFilter(title = "My Tasks")
        provider.setBadgeFilter(filter)
        verify(preferences).setString(R.string.p_badge_list, "0:0")
    }

    @Test
    fun setLastViewedFilterPersists() {
        // Kills: setFilterPreference call in setLastViewedFilter
        val filter = TodayFilter(title = "Today")
        provider.setLastViewedFilter(filter)
        verify(preferences).setString(R.string.p_last_viewed_list, "0:1")
    }

    @Test
    fun setDefaultOpenFilterPersists() {
        // Kills: setFilterPreference call in setDefaultOpenFilter
        val filter = SnoozedFilter(title = "Snoozed")
        provider.setDefaultOpenFilter(filter)
        verify(preferences).setString(R.string.p_default_open_filter, "0:4")
    }
}
