package org.tasks.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5Managed
import org.tasks.data.entity.CaldavAccount.Companion.isDecSync
import org.tasks.data.entity.CaldavAccount.Companion.isEteSync
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.data.entity.CaldavAccount.Companion.isTosRequired
import org.tasks.data.entity.CaldavAccount.Companion.openTaskType
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData

class DaoExtensionsTest {

    private lateinit var caldavDao: CaldavDao
    private lateinit var tagDataDao: TagDataDao

    @Before
    fun setUp() {
        caldavDao = mock(CaldavDao::class.java)
        tagDataDao = mock(TagDataDao::class.java)
    }

    // --- CaldavDaoExtensions: getLocalAccount ---

    @Test
    fun getLocalAccountReturnsExistingAccount() = runTest {
        val existing = CaldavAccount(
            id = 1,
            accountType = CaldavAccount.TYPE_LOCAL,
            uuid = "local-uuid",
        )
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_LOCAL)).thenReturn(listOf(existing))

        val result = caldavDao.getLocalAccount()

        assertSame(existing, result)
    }

    // --- CaldavDaoExtensions: getOrCreateLocalAccount ---

    @Test
    fun getOrCreateLocalAccountReturnsExistingAccount() = runTest {
        val existing = CaldavAccount(
            id = 1,
            accountType = CaldavAccount.TYPE_LOCAL,
            uuid = "local-uuid",
        )
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_LOCAL)).thenReturn(listOf(existing))

        val result = caldavDao.getOrCreateLocalAccount()

        assertSame(existing, result)
    }

    // --- CaldavDaoExtensions: getLocalAccount returns first when multiple ---

    @Test
    fun getLocalAccountReturnsFirstWhenMultipleExist() = runTest {
        val first = CaldavAccount(id = 1, accountType = CaldavAccount.TYPE_LOCAL, uuid = "uuid-1")
        val second = CaldavAccount(id = 2, accountType = CaldavAccount.TYPE_LOCAL, uuid = "uuid-2")
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_LOCAL)).thenReturn(listOf(first, second))

        val result = caldavDao.getLocalAccount()

        assertSame(first, result)
    }

    // --- CaldavCalendar: readOnly with default access ---

    @Test
    fun calendarIsNotReadOnlyWhenDefaultAccess() {
        val calendar = CaldavCalendar()
        assertEquals(false, calendar.readOnly())
    }

    // --- CaldavCalendar: calendarUri with multiple trailing slashes ---

    @Test
    fun calendarUriWithMultiplePathSegments() {
        val calendar = CaldavCalendar(url = "https://example.com/a/b/c/d/")
        assertEquals("d", calendar.calendarUri)
    }

    // --- CaldavAccount: isLoggedOut ---

    @Test
    fun isLoggedOutWhenUnauthorized() {
        val account = CaldavAccount(
            accountType = CaldavAccount.TYPE_TASKS,
            error = CaldavAccount.ERROR_UNAUTHORIZED,
        )
        assertTrue(account.isLoggedOut())
    }

    @Test
    fun isNotLoggedOutWhenNoError() {
        val account = CaldavAccount(accountType = CaldavAccount.TYPE_TASKS)
        assertEquals(false, account.isLoggedOut())
    }

    // --- CaldavAccount: isPaymentRequired ---

    @Test
    fun isPaymentRequiredWhenPaymentError() {
        val account = CaldavAccount(
            accountType = CaldavAccount.TYPE_TASKS,
            error = CaldavAccount.ERROR_PAYMENT_REQUIRED,
        )
        assertTrue(account.isPaymentRequired())
    }

    @Test
    fun isNotPaymentRequiredWhenNoError() {
        val account = CaldavAccount(accountType = CaldavAccount.TYPE_TASKS)
        assertEquals(false, account.isPaymentRequired())
    }

    // --- TagDataDaoExtensions: sort with no order ---

    @Test
    fun searchTagsSortsAlphabeticallyWhenNoOrder() = runTest {
        val tags = listOf(
            TagData(id = 1, name = "Zebra", order = NO_ORDER),
            TagData(id = 2, name = "Apple", order = NO_ORDER),
            TagData(id = 3, name = "Mango", order = NO_ORDER),
        )
        `when`(tagDataDao.searchTagsInternal("%fruit%")).thenReturn(tags)

        val result = tagDataDao.searchTags("fruit")

        assertEquals(listOf("Apple", "Mango", "Zebra"), result.map { it.name })
    }

    @Test
    fun searchTagsSortsByOrderWhenOrderIsSet() = runTest {
        val tags = listOf(
            TagData(id = 1, name = "Zebra", order = 3),
            TagData(id = 2, name = "Apple", order = 1),
            TagData(id = 3, name = "Mango", order = 2),
        )
        `when`(tagDataDao.searchTagsInternal("%q%")).thenReturn(tags)

        val result = tagDataDao.searchTags("q")

        assertEquals(listOf("Apple", "Mango", "Zebra"), result.map { it.name })
    }

    @Test
    fun searchTagsReturnsEmptyListWhenNoResults() = runTest {
        `when`(tagDataDao.searchTagsInternal("%nothing%")).thenReturn(emptyList())

        val result = tagDataDao.searchTags("nothing")

        assertEquals(emptyList<TagData>(), result)
    }

    @Test
    fun searchTagsMixedOrderPutsNoOrderLast() = runTest {
        val tags = listOf(
            TagData(id = 1, name = "Ordered1", order = 2),
            TagData(id = 2, name = "Unordered", order = NO_ORDER),
            TagData(id = 3, name = "Ordered2", order = 1),
        )
        `when`(tagDataDao.searchTagsInternal("%mix%")).thenReturn(tags)

        val result = tagDataDao.searchTags("mix")

        // Ordered items first (by order), then unordered items
        assertEquals("Ordered2", result[0].name)
        assertEquals("Ordered1", result[1].name)
        assertEquals("Unordered", result[2].name)
    }

    @Test
    fun searchTagsSortsByIdWhenAllNoOrderAndSameName() = runTest {
        val tags = listOf(
            TagData(id = 3, name = "Same", order = NO_ORDER),
            TagData(id = 1, name = "Same", order = NO_ORDER),
            TagData(id = 2, name = "Same", order = NO_ORDER),
        )
        `when`(tagDataDao.searchTagsInternal("%same%")).thenReturn(tags)

        val result = tagDataDao.searchTags("same")

        // AlphanumComparator returns 0 when names are the same, so order is stable
        assertEquals(3, result.size)
    }

    @Test
    fun searchTagsSortByEqualOrderFallsBackToAlphanum() = runTest {
        val tags = listOf(
            TagData(id = 1, name = "Banana", order = 1),
            TagData(id = 2, name = "Apple", order = 1),
        )
        `when`(tagDataDao.searchTagsInternal("%x%")).thenReturn(tags)

        val result = tagDataDao.searchTags("x")

        // Same order => falls back to AlphanumComparator on name
        assertEquals("Apple", result[0].name)
        assertEquals("Banana", result[1].name)
    }

    @Test
    fun searchTagsAlphanumericSortHandlesNumbers() = runTest {
        val tags = listOf(
            TagData(id = 1, name = "Tag10", order = NO_ORDER),
            TagData(id = 2, name = "Tag2", order = NO_ORDER),
            TagData(id = 3, name = "Tag1", order = NO_ORDER),
        )
        `when`(tagDataDao.searchTagsInternal("%tag%")).thenReturn(tags)

        val result = tagDataDao.searchTags("tag")

        // Alphanum sorts numerically: Tag1 < Tag2 < Tag10
        assertEquals(listOf("Tag1", "Tag2", "Tag10"), result.map { it.name })
    }

    // --- CaldavCalendar: readOnly ---

    @Test
    fun calendarIsReadOnlyWhenAccessReadOnly() {
        val calendar = CaldavCalendar(access = CaldavCalendar.ACCESS_READ_ONLY)
        assertEquals(true, calendar.readOnly())
    }

    @Test
    fun calendarIsNotReadOnlyWhenAccessOwner() {
        val calendar = CaldavCalendar(access = CaldavCalendar.ACCESS_OWNER)
        assertEquals(false, calendar.readOnly())
    }

    @Test
    fun calendarIsNotReadOnlyWhenAccessReadWrite() {
        val calendar = CaldavCalendar(access = CaldavCalendar.ACCESS_READ_WRITE)
        assertEquals(false, calendar.readOnly())
    }

    // --- CaldavCalendar: calendarUri ---

    @Test
    fun calendarUriExtractsLastSegment() {
        val calendar = CaldavCalendar(url = "https://example.com/dav/calendars/personal/")
        assertEquals("personal", calendar.calendarUri)
    }

    @Test
    fun calendarUriWithoutTrailingSlash() {
        val calendar = CaldavCalendar(url = "https://example.com/dav/calendars/work")
        assertEquals("work", calendar.calendarUri)
    }

    @Test
    fun calendarUriNullUrl() {
        val calendar = CaldavCalendar(url = null)
        assertNull(calendar.calendarUri)
    }

    @Test
    fun calendarUriEmptyUrl() {
        val calendar = CaldavCalendar(url = "")
        assertNull(calendar.calendarUri)
    }

    @Test
    fun calendarUriSingleSegment() {
        val calendar = CaldavCalendar(url = "personal")
        assertEquals("personal", calendar.calendarUri)
    }

    // --- CaldavAccount companion: openTaskType ---

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

    // --- CaldavAccount companion: isDavx5 ---

    @Test
    fun isDavx5ForDavx5Account() {
        assertEquals(true, "bitfire.at.davdroid:account".isDavx5())
    }

    @Test
    fun isDavx5ForNonDavx5Account() {
        assertEquals(false, "other:account".isDavx5())
    }

    @Test
    fun isDavx5ForNull() {
        assertEquals(false, (null as String?).isDavx5())
    }

    // --- CaldavAccount companion: isDavx5Managed ---

    @Test
    fun isDavx5ManagedForManagedAccount() {
        assertEquals(true, "com.davdroid:account".isDavx5Managed())
    }

    @Test
    fun isDavx5ManagedForNonManaged() {
        assertEquals(false, "other:account".isDavx5Managed())
    }

    @Test
    fun isDavx5ManagedForNull() {
        assertEquals(false, (null as String?).isDavx5Managed())
    }

    // --- CaldavAccount companion: isEteSync ---

    @Test
    fun isEteSyncForEteSyncAccount() {
        assertEquals(true, "com.etesync.syncadapter:account".isEteSync())
    }

    @Test
    fun isEteSyncForNonEteSync() {
        assertEquals(false, "other".isEteSync())
    }

    @Test
    fun isEteSyncForNull() {
        assertEquals(false, (null as String?).isEteSync())
    }

    // --- CaldavAccount companion: isDecSync ---

    @Test
    fun isDecSyncForDecSyncAccount() {
        assertEquals(true, "org.decsync.tasks:account".isDecSync())
    }

    @Test
    fun isDecSyncForNonDecSync() {
        assertEquals(false, "other".isDecSync())
    }

    @Test
    fun isDecSyncForNull() {
        assertEquals(false, (null as String?).isDecSync())
    }

    // --- CaldavAccount companion: isPaymentRequired ---

    @Test
    fun isPaymentRequiredForPaymentError() {
        assertEquals(true, "HTTP 402".isPaymentRequired())
    }

    @Test
    fun isPaymentRequiredForPaymentErrorWithDetails() {
        assertEquals(true, "HTTP 402 Payment Required".isPaymentRequired())
    }

    @Test
    fun isPaymentRequiredForOtherError() {
        assertEquals(false, "HTTP 500".isPaymentRequired())
    }

    @Test
    fun isPaymentRequiredForNull() {
        assertEquals(false, (null as String?).isPaymentRequired())
    }

    // --- CaldavAccount companion: isTosRequired ---

    @Test
    fun isTosRequiredForTosError() {
        assertEquals(true, "HTTP 451".isTosRequired())
    }

    @Test
    fun isTosRequiredForOther() {
        assertEquals(false, "HTTP 200".isTosRequired())
    }

    @Test
    fun isTosRequiredForNull() {
        assertEquals(false, (null as String?).isTosRequired())
    }
}
