package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.http.OkHttpClientFactory
import org.tasks.preferences.TasksPreferences
import org.tasks.security.KeyStoreEncryption

class CaldavClientProviderTest {

    private lateinit var encryption: KeyStoreEncryption
    private lateinit var tasksPreferences: TasksPreferences
    private lateinit var environment: TasksServerEnvironment
    private lateinit var httpClientFactory: OkHttpClientFactory

    @Before
    fun setUp() {
        encryption = mock(KeyStoreEncryption::class.java)
        tasksPreferences = mock(TasksPreferences::class.java)
        environment = mock(TasksServerEnvironment::class.java)
        httpClientFactory = mock(OkHttpClientFactory::class.java)
    }

    @Test
    fun forTasksAccountThrowsForNonTasksOrg() {
        val provider = CaldavClientProvider(
            encryption = encryption,
            tasksPreferences = tasksPreferences,
            environment = environment,
            httpClientFactory = httpClientFactory,
        )
        val account = CaldavAccount(
            accountType = TYPE_CALDAV,
            url = "https://other.example.com/dav/",
        )
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { provider.forTasksAccount(account) }
        }
    }
}
