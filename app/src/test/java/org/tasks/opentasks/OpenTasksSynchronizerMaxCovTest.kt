package org.tasks.opentasks

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

class OpenTasksSynchronizerMaxCovTest {

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

    // ================================================================
    // sync() — empty case
    // ================================================================

    @Test
    fun syncEmptyNoAccountsNoLists() = runTest {
        whenever(openTaskDao.getListsByAccount()).thenReturn(emptyMap())
        whenever(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(emptyList())
        synchronizer.sync()
        verify(openTaskDao).getListsByAccount()
        verify(caldavDao).getAccounts(CaldavAccount.TYPE_OPENTASKS)
    }

    // ================================================================
    // sync() — removes stale accounts
    // ================================================================

    @Test
    fun syncRemovesStaleAccount() = runTest {
        whenever(openTaskDao.getListsByAccount()).thenReturn(emptyMap())
        val account = CaldavAccount(
            uuid = "$ACCOUNT_TYPE_DAVX5:user@host",
            accountType = CaldavAccount.TYPE_OPENTASKS,
        )
        whenever(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(account))
        synchronizer.sync()
        verify(taskDeleter).delete(account)
    }

    @Test
    fun syncRemovesMultipleStaleAccounts() = runTest {
        whenever(openTaskDao.getListsByAccount()).thenReturn(emptyMap())
        val a1 = CaldavAccount(uuid = "$ACCOUNT_TYPE_DAVX5:a@b", accountType = CaldavAccount.TYPE_OPENTASKS)
        val a2 = CaldavAccount(uuid = "$ACCOUNT_TYPE_DAVX5:c@d", accountType = CaldavAccount.TYPE_OPENTASKS)
        whenever(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(a1, a2))
        synchronizer.sync()
        verify(taskDeleter).delete(a1)
        verify(taskDeleter).delete(a2)
    }

    // ================================================================
    // sync() — requires pro
    // ================================================================

    @Test
    fun syncSetsErrorWhenNoPro() = runTest {
        val uuid = "$ACCOUNT_TYPE_DAVX5:u@e"
        val cal = CaldavCalendar(id = 1L, account = uuid, name = "P", url = "https://c/p")
        val account = CaldavAccount(uuid = uuid, accountType = CaldavAccount.TYPE_OPENTASKS)
        whenever(openTaskDao.getListsByAccount()).thenReturn(mapOf(uuid to listOf(cal)))
        whenever(caldavDao.anyExist(listOf("https://c/p"))).thenReturn(false)
        whenever(caldavDao.getAccountByUuid(uuid)).thenReturn(account)
        whenever(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(account))
        whenever(inventory.hasPro).thenReturn(false)
        synchronizer.sync()
        assertEquals("Requires pro", account.error)
        verify(caldavDao).update(account)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // ================================================================
    // sync() — creates new accounts
    // ================================================================

    @Test
    fun syncCreatesAccountForNewUuid() = runTest {
        val uuid = "$ACCOUNT_TYPE_DAVX5:new@host"
        val cal = CaldavCalendar(id = 1L, account = uuid, name = "Work", url = "https://dav/cal")
        whenever(openTaskDao.getListsByAccount()).thenReturn(mapOf(uuid to listOf(cal)))
        whenever(caldavDao.anyExist(listOf("https://dav/cal"))).thenReturn(false)
        whenever(caldavDao.getAccountByUuid(uuid)).thenReturn(null)
        whenever(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(emptyList())
        synchronizer.sync()
        verify(caldavDao).insert(any<CaldavAccount>())
    }

    // ================================================================
    // Account type detection
    // ================================================================

    @Test
    fun isDavx5Account() = assertTrue("$ACCOUNT_TYPE_DAVX5:u@e".isDavx5())

    @Test
    fun isDavx5ManagedAccount() = assertTrue("$ACCOUNT_TYPE_DAVX5_MANAGED:u@e".isDavx5Managed())

    @Test
    fun isEteSyncAccount() = assertTrue("$ACCOUNT_TYPE_ETESYNC:u@e".isEteSync())

    @Test
    fun isDecSyncAccount() = assertTrue("$ACCOUNT_TYPE_DECSYNC:u@e".isDecSync())

    @Test
    fun isDavx5FalseForEteSync() = assertFalse("$ACCOUNT_TYPE_ETESYNC:u@e".isDavx5())

    @Test
    fun isEteSyncFalseForDavx5() = assertFalse("$ACCOUNT_TYPE_DAVX5:u@e".isEteSync())

    // ================================================================
    // Account UUID parsing
    // ================================================================

    @Test
    fun accountUuidSplitExtractsName() {
        val name = "$ACCOUNT_TYPE_DAVX5:user@example.com".split(":")[1]
        assertEquals("user@example.com", name)
    }

    @Test
    fun accountUuidSplitExtractsType() {
        val type = "$ACCOUNT_TYPE_DAVX5:user@example.com".split(":")[0]
        assertEquals(ACCOUNT_TYPE_DAVX5, type)
    }

    // ================================================================
    // CaldavCalendar access constants
    // ================================================================

    @Test
    fun accessOwner() = assertEquals(0, CaldavCalendar.ACCESS_OWNER)

    @Test
    fun accessReadOnly() = assertEquals(2, CaldavCalendar.ACCESS_READ_ONLY)

    @Test
    fun accessReadWrite() = assertEquals(1, CaldavCalendar.ACCESS_READ_WRITE)

    // ================================================================
    // OpenTaskDao supported types
    // ================================================================

    @Test
    fun supportedTypesContainsAll() {
        assertTrue(OpenTaskDao.SUPPORTED_TYPES.contains(ACCOUNT_TYPE_DAVX5))
        assertTrue(OpenTaskDao.SUPPORTED_TYPES.contains(ACCOUNT_TYPE_DAVX5_MANAGED))
        assertTrue(OpenTaskDao.SUPPORTED_TYPES.contains(ACCOUNT_TYPE_ETESYNC))
        assertTrue(OpenTaskDao.SUPPORTED_TYPES.contains(ACCOUNT_TYPE_DECSYNC))
    }
}
