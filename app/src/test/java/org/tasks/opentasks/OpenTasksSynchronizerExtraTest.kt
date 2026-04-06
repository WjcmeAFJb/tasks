package org.tasks.opentasks

import android.content.Context
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
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.iCalendar
import org.tasks.data.OpenTaskDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DAVX5
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DAVX5_MANAGED
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DECSYNC
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_ETESYNC
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5Managed
import org.tasks.data.entity.CaldavAccount.Companion.isDecSync
import org.tasks.data.entity.CaldavAccount.Companion.isEteSync
import org.tasks.data.entity.CaldavCalendar
import org.tasks.service.TaskDeleter

class OpenTasksSynchronizerExtraTest {

    private lateinit var context: Context
    private lateinit var caldavDao: CaldavDao
    private lateinit var taskDeleter: TaskDeleter
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var taskDao: TaskDao
    private lateinit var firebase: Firebase
    private lateinit var ical: iCalendar
    private lateinit var openTaskDao: OpenTaskDao
    private lateinit var inventory: Inventory
    private lateinit var synchronizer: OpenTasksSynchronizer

    @Before
    fun setUp() {
        context = mock()
        caldavDao = mock()
        taskDeleter = mock()
        refreshBroadcaster = mock()
        taskDao = mock()
        firebase = mock()
        ical = mock()
        openTaskDao = mock()
        inventory = mock()
        whenever(context.getString(R.string.requires_pro_subscription)).thenReturn("Requires pro")
        synchronizer = OpenTasksSynchronizer(
            context, caldavDao, taskDeleter, refreshBroadcaster,
            taskDao, firebase, ical, openTaskDao, inventory
        )
    }

    // ===== Account type checks =====

    @Test
    fun isDavx5True() {
        assertTrue("$ACCOUNT_TYPE_DAVX5:user@example.com".isDavx5())
    }

    @Test
    fun isDavx5ManagedTrue() {
        assertTrue("$ACCOUNT_TYPE_DAVX5_MANAGED:user@example.com".isDavx5Managed())
    }

    @Test
    fun isEteSyncTrue() {
        assertTrue("$ACCOUNT_TYPE_ETESYNC:user@example.com".isEteSync())
    }

    @Test
    fun isDecSyncTrue() {
        assertTrue("$ACCOUNT_TYPE_DECSYNC:user@example.com".isDecSync())
    }

    @Test
    fun isDavx5FalseForOther() {
        assertTrue(!"$ACCOUNT_TYPE_ETESYNC:user@example.com".isDavx5())
    }

    @Test
    fun isEteSyncFalseForDavx5() {
        assertTrue(!"$ACCOUNT_TYPE_DAVX5:user@example.com".isEteSync())
    }

    // ===== sync with no lists =====

    @Test
    fun syncWithNoListsAndNoAccounts() = runTest {
        whenever(openTaskDao.getListsByAccount()).thenReturn(emptyMap())
        whenever(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(emptyList())

        synchronizer.sync()

        verify(openTaskDao).getListsByAccount()
        verify(caldavDao).getAccounts(CaldavAccount.TYPE_OPENTASKS)
        verify(taskDeleter, never()).delete(any<CaldavAccount>())
    }

    // ===== sync removes accounts not in lists =====

    @Test
    fun syncRemovesOrphanedAccount() = runTest {
        whenever(openTaskDao.getListsByAccount()).thenReturn(emptyMap())
        val account = CaldavAccount(
            uuid = "$ACCOUNT_TYPE_DAVX5:test@test.com",
            accountType = CaldavAccount.TYPE_OPENTASKS
        )
        whenever(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(account))

        synchronizer.sync()

        verify(taskDeleter).delete(account)
    }

    // ===== sync sets error without pro =====

    @Test
    fun syncSetsErrorWhenNoPro() = runTest {
        val uuid = "$ACCOUNT_TYPE_DAVX5:user@host"
        val cal = CaldavCalendar(id = 1L, account = uuid, name = "Personal", url = "https://a/b")
        val account = CaldavAccount(uuid = uuid, accountType = CaldavAccount.TYPE_OPENTASKS)
        whenever(openTaskDao.getListsByAccount()).thenReturn(mapOf(uuid to listOf(cal)))
        whenever(caldavDao.anyExist(listOf("https://a/b"))).thenReturn(false)
        whenever(caldavDao.getAccountByUuid(uuid)).thenReturn(account)
        whenever(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(account))
        whenever(inventory.hasPro).thenReturn(false)

        synchronizer.sync()

        assertEquals("Requires pro", account.error)
        verify(caldavDao).update(account)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // ===== sync creates new accounts for new lists =====

    @Test
    fun syncCreatesNewAccountForNewList() = runTest {
        val uuid = "$ACCOUNT_TYPE_DAVX5:newuser@host"
        val cal = CaldavCalendar(id = 1L, account = uuid, name = "Work", url = "https://dav/cal")
        whenever(openTaskDao.getListsByAccount()).thenReturn(mapOf(uuid to listOf(cal)))
        whenever(caldavDao.anyExist(listOf("https://dav/cal"))).thenReturn(false)
        whenever(caldavDao.getAccountByUuid(uuid)).thenReturn(null)
        whenever(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(emptyList())

        synchronizer.sync()

        verify(caldavDao).insert(any<CaldavAccount>())
    }

    // ===== CaldavCalendar access constants =====

    @Test
    fun accessOwner() {
        assertEquals(0, CaldavCalendar.ACCESS_OWNER)
    }

    @Test
    fun accessReadWrite() {
        assertEquals(1, CaldavCalendar.ACCESS_READ_WRITE)
    }

    @Test
    fun accessReadOnly() {
        assertEquals(2, CaldavCalendar.ACCESS_READ_ONLY)
    }

    // ===== CaldavAccount type constant =====

    @Test
    fun typeOpenTasks() {
        assertEquals(3, CaldavAccount.TYPE_OPENTASKS)
    }

    // ===== OpenTaskDao supported types =====

    @Test
    fun supportedTypesContainsDavx5() {
        assertTrue(OpenTaskDao.SUPPORTED_TYPES.contains(ACCOUNT_TYPE_DAVX5))
    }

    @Test
    fun supportedTypesContainsEteSync() {
        assertTrue(OpenTaskDao.SUPPORTED_TYPES.contains(ACCOUNT_TYPE_ETESYNC))
    }

    @Test
    fun supportedTypesContainsDecSync() {
        assertTrue(OpenTaskDao.SUPPORTED_TYPES.contains(ACCOUNT_TYPE_DECSYNC))
    }

    @Test
    fun supportedTypesContainsDavx5Managed() {
        assertTrue(OpenTaskDao.SUPPORTED_TYPES.contains(ACCOUNT_TYPE_DAVX5_MANAGED))
    }

    // ===== Account UUID parsing =====

    @Test
    fun accountUuidSplitExtractsName() {
        val uuid = "$ACCOUNT_TYPE_DAVX5:user@example.com"
        val name = uuid.split(":")[1]
        assertEquals("user@example.com", name)
    }

    @Test
    fun accountUuidSplitExtractsType() {
        val uuid = "$ACCOUNT_TYPE_ETESYNC:user@example.com"
        val type = uuid.split(":")[0]
        assertEquals(ACCOUNT_TYPE_ETESYNC, type)
    }
}
