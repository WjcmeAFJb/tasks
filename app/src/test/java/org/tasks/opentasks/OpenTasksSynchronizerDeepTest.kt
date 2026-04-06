package org.tasks.opentasks

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
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
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5
import org.tasks.data.entity.CaldavAccount.Companion.isEteSync
import org.tasks.data.entity.CaldavAccount.Companion.isDecSync
import org.tasks.data.entity.CaldavCalendar
import org.tasks.service.TaskDeleter

class OpenTasksSynchronizerDeepTest {

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
        `when`(context.getString(R.string.requires_pro_subscription)).thenReturn("Requires pro")
        synchronizer = OpenTasksSynchronizer(context, caldavDao, taskDeleter, refreshBroadcaster, taskDao, firebase, ical, openTaskDao, inventory)
    }

    @Test
    fun syncEmpty() = runTest {
        `when`(openTaskDao.getListsByAccount()).thenReturn(emptyMap())
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(emptyList())
        synchronizer.sync()
        verify(openTaskDao).getListsByAccount()
        verify(caldavDao).getAccounts(CaldavAccount.TYPE_OPENTASKS)
    }

    @Test
    fun syncRemovesStaleAccount() = runTest {
        `when`(openTaskDao.getListsByAccount()).thenReturn(emptyMap())
        val a = CaldavAccount(uuid = "$ACCOUNT_TYPE_DAVX5:u@e.com", accountType = CaldavAccount.TYPE_OPENTASKS)
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(a))
        synchronizer.sync()
        verify(taskDeleter).delete(a)
    }

    @Test
    fun syncRemovesMultipleStaleAccounts() = runTest {
        `when`(openTaskDao.getListsByAccount()).thenReturn(emptyMap())
        val a1 = CaldavAccount(uuid = "$ACCOUNT_TYPE_DAVX5:a@b", accountType = CaldavAccount.TYPE_OPENTASKS)
        val a2 = CaldavAccount(uuid = "$ACCOUNT_TYPE_DAVX5:c@d", accountType = CaldavAccount.TYPE_OPENTASKS)
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(a1, a2))
        synchronizer.sync()
        verify(taskDeleter).delete(a1)
        verify(taskDeleter).delete(a2)
    }

    @Test
    fun syncSetsErrorWhenNoPro() = runTest {
        val cal = CaldavCalendar(id = 1L, account = "$ACCOUNT_TYPE_DAVX5:u@e.com", name = "P", url = "https://c/p")
        val a = CaldavAccount(uuid = "$ACCOUNT_TYPE_DAVX5:u@e.com", accountType = CaldavAccount.TYPE_OPENTASKS)
        `when`(openTaskDao.getListsByAccount()).thenReturn(mapOf("$ACCOUNT_TYPE_DAVX5:u@e.com" to listOf(cal)))
        `when`(caldavDao.anyExist(listOf("https://c/p"))).thenReturn(false)
        `when`(caldavDao.getAccountByUuid("$ACCOUNT_TYPE_DAVX5:u@e.com")).thenReturn(a)
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(a))
        `when`(inventory.hasPro).thenReturn(false)
        synchronizer.sync()
        assertEquals("Requires pro", a.error)
        verify(caldavDao).update(a)
    }

    @Test
    fun syncBroadcastsRefreshOnProError() = runTest {
        val cal = CaldavCalendar(id = 1L, account = "$ACCOUNT_TYPE_DAVX5:u@e.com", name = "P", url = "https://c/p")
        val a = CaldavAccount(uuid = "$ACCOUNT_TYPE_DAVX5:u@e.com", accountType = CaldavAccount.TYPE_OPENTASKS)
        `when`(openTaskDao.getListsByAccount()).thenReturn(mapOf("$ACCOUNT_TYPE_DAVX5:u@e.com" to listOf(cal)))
        `when`(caldavDao.anyExist(listOf("https://c/p"))).thenReturn(false)
        `when`(caldavDao.getAccountByUuid("$ACCOUNT_TYPE_DAVX5:u@e.com")).thenReturn(a)
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(a))
        `when`(inventory.hasPro).thenReturn(false)
        synchronizer.sync()
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // ===== CaldavAccount type constants =====

    @Test
    fun openTasksAccountType() {
        assertEquals(3, CaldavAccount.TYPE_OPENTASKS)
    }

    // ===== CaldavCalendar access constants =====

    @Test
    fun accessOwner() {
        assertEquals(0, CaldavCalendar.ACCESS_OWNER)
    }

    @Test
    fun accessReadOnly() {
        assertEquals(2, CaldavCalendar.ACCESS_READ_ONLY)
    }

    // ===== CaldavAccount type checks =====

    @Test
    fun isDavx5Account() {
        assertTrue("$ACCOUNT_TYPE_DAVX5:u@e.com".isDavx5())
    }

    @Test
    fun isEteSyncAccount() {
        assertTrue("${CaldavAccount.ACCOUNT_TYPE_ETESYNC}:u@e.com".isEteSync())
    }

    @Test
    fun isDecSyncAccount() {
        assertTrue("${CaldavAccount.ACCOUNT_TYPE_DECSYNC}:u@e.com".isDecSync())
    }

    // ===== OpenTaskDao supported types =====

    @Test
    fun supportedTypesContainsDavx5() {
        assertTrue(org.tasks.data.OpenTaskDao.SUPPORTED_TYPES.contains(CaldavAccount.ACCOUNT_TYPE_DAVX5))
    }

    @Test
    fun supportedTypesContainsEteSync() {
        assertTrue(org.tasks.data.OpenTaskDao.SUPPORTED_TYPES.contains(CaldavAccount.ACCOUNT_TYPE_ETESYNC))
    }

    @Test
    fun supportedTypesContainsDecSync() {
        assertTrue(org.tasks.data.OpenTaskDao.SUPPORTED_TYPES.contains(CaldavAccount.ACCOUNT_TYPE_DECSYNC))
    }

    @Test
    fun supportedTypeFilterNotEmpty() {
        assertTrue(org.tasks.data.OpenTaskDao.SUPPORTED_TYPE_FILTER.isNotEmpty())
    }
}
