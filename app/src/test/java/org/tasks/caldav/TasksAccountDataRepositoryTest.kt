package org.tasks.caldav

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.TasksPreferences.Companion.cachedAccountData

class TasksAccountDataRepositoryTest {

    private lateinit var provider: CaldavClientProvider
    private lateinit var caldavDao: CaldavDao
    private lateinit var tasksPreferences: TasksPreferences
    private lateinit var repository: TasksAccountDataRepository

    @Before
    fun setUp() {
        provider = Mockito.mock(CaldavClientProvider::class.java)
        caldavDao = Mockito.mock(CaldavDao::class.java)
        tasksPreferences = Mockito.mock(TasksPreferences::class.java)
        repository = TasksAccountDataRepository(
            provider = provider,
            caldavDao = caldavDao,
            tasksPreferences = tasksPreferences,
        )
    }

    @Test
    fun getAccountResponseReturnsNullForBlankCache() = runTest {
        `when`(tasksPreferences.get(cachedAccountData, "")).thenReturn("")
        assertNull(repository.getAccountResponse())
    }

    @Test
    fun getAccountResponseParsesValidJson() = runTest {
        `when`(tasksPreferences.get(cachedAccountData, "")).thenReturn("""{"guest": true, "max_guests": 3}""")
        val result = repository.getAccountResponse()
        assertTrue(result!!.guest)
        assertEquals(3, result.maxGuests)
    }

    @Test
    fun getAccountResponseReturnsNullForInvalidJson() = runTest {
        `when`(tasksPreferences.get(cachedAccountData, "")).thenReturn("not-json")
        assertNull(repository.getAccountResponse())
    }

    @Test
    fun clearSetsEmptyString() = runTest {
        repository.clear()
        Mockito.verify(tasksPreferences).set(cachedAccountData, "")
    }

    @Test
    fun fetchAndCacheReturnsNullWhenNoAccounts() = runTest {
        `when`(caldavDao.getAccounts()).thenReturn(emptyList())
        assertNull(repository.fetchAndCache())
    }

    @Test
    fun fetchAndCacheReturnsNullWhenNoTasksOrgAccount() = runTest {
        val account = CaldavAccount(
            accountType = CaldavAccount.TYPE_CALDAV,
            url = "https://other.example.com/"
        )
        `when`(caldavDao.getAccounts()).thenReturn(listOf(account))
        assertNull(repository.fetchAndCache())
    }

    @Test
    fun fetchAndCacheUsesFirstTasksOrgAccount() = runTest {
        val tasksAccount = CaldavAccount(accountType = TYPE_TASKS, password = "enc", username = "user")
        `when`(caldavDao.getAccounts()).thenReturn(listOf(tasksAccount))
        val tasksClient = Mockito.mock(TasksClient::class.java)
        `when`(provider.forTasksAccount(tasksAccount)).thenReturn(tasksClient)
        `when`(tasksClient.getAccount()).thenReturn("""{"guest": false}""")
        val result = repository.fetchAndCache()
        assertFalse(result!!.guest)
    }

    @Test
    fun fetchAndCacheWithAccountStoresData() = runTest {
        val account = CaldavAccount(accountType = TYPE_TASKS, password = "enc")
        val tasksClient = Mockito.mock(TasksClient::class.java)
        `when`(provider.forTasksAccount(account)).thenReturn(tasksClient)
        val json = """{"guest": true, "max_guests": 7}"""
        `when`(tasksClient.getAccount()).thenReturn(json)
        val result = repository.fetchAndCache(account)
        Mockito.verify(tasksPreferences).set(cachedAccountData, json)
        assertTrue(result!!.guest)
        assertEquals(7, result.maxGuests)
    }

    @Test
    fun fetchAndCacheReturnsNullWhenClientReturnsNull() = runTest {
        val account = CaldavAccount(accountType = TYPE_TASKS, password = "enc")
        val tasksClient = Mockito.mock(TasksClient::class.java)
        `when`(provider.forTasksAccount(account)).thenReturn(tasksClient)
        `when`(tasksClient.getAccount()).thenReturn(null)
        assertNull(repository.fetchAndCache(account))
    }
}
