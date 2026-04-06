package org.tasks.opentasks

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.iCalendar
import org.tasks.data.OpenTaskDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.service.TaskDeleter

class OpenTasksSynchronizerTest {

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

        `when`(context.getString(org.tasks.R.string.requires_pro_subscription))
            .thenReturn("Requires pro")

        synchronizer = OpenTasksSynchronizer(
            context = context,
            caldavDao = caldavDao,
            taskDeleter = taskDeleter,
            refreshBroadcaster = refreshBroadcaster,
            taskDao = taskDao,
            firebase = firebase,
            iCalendar = ical,
            openTaskDao = openTaskDao,
            inventory = inventory,
        )
    }

    // --- sync with no accounts and no lists ---

    @Test
    fun syncWithEmptyListsAndNoAccounts() = runTest {
        `when`(openTaskDao.getListsByAccount()).thenReturn(emptyMap())
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(emptyList())

        synchronizer.sync()

        verify(openTaskDao).getListsByAccount()
        verify(caldavDao).getAccounts(CaldavAccount.TYPE_OPENTASKS)
    }

    // --- sync removes account when lists are gone ---

    @Test
    fun syncRemovesAccountWhenListsDisappear() = runTest {
        // filterActive returns empty since there are no lists
        `when`(openTaskDao.getListsByAccount()).thenReturn(emptyMap())

        val account = CaldavAccount(
            uuid = "davx5:user@example.com",
            accountType = CaldavAccount.TYPE_OPENTASKS,
        )
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS)).thenReturn(listOf(account))

        synchronizer.sync()

        // Account's lists are null (not in the map), so it should be deleted
        verify(taskDeleter).delete(account)
    }

    // --- sync removes multiple stale accounts ---

    @Test
    fun syncRemovesMultipleStaleAccounts() = runTest {
        `when`(openTaskDao.getListsByAccount()).thenReturn(emptyMap())

        val account1 = CaldavAccount(uuid = "davx5:a@b.com", accountType = CaldavAccount.TYPE_OPENTASKS)
        val account2 = CaldavAccount(uuid = "davx5:c@d.com", accountType = CaldavAccount.TYPE_OPENTASKS)
        `when`(caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS))
            .thenReturn(listOf(account1, account2))

        synchronizer.sync()

        verify(taskDeleter).delete(account1)
        verify(taskDeleter).delete(account2)
    }
}
