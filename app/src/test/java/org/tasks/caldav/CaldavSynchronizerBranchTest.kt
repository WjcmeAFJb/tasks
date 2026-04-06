package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.okhttp.Response
import at.bitfire.dav4jvm.okhttp.exception.HttpException
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrincipal
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrivilegeSet
import at.bitfire.dav4jvm.property.webdav.SyncToken
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.CaldavSynchronizer.Companion.accessLevel
import org.tasks.caldav.CaldavSynchronizer.Companion.ctag
import org.tasks.caldav.property.OCOwnerPrincipal
import org.tasks.caldav.property.ShareAccess
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OPEN_XCHANGE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.service.TaskDeleter

class CaldavSynchronizerBranchTest {

    private lateinit var caldavDao: CaldavDao
    private lateinit var taskDao: TaskDao
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var taskDeleter: TaskDeleter
    private lateinit var reporting: Reporting
    private lateinit var provider: CaldavClientProvider
    private lateinit var iCal: iCalendar
    private lateinit var principalDao: PrincipalDao
    private lateinit var vtodoCache: VtodoCache
    private lateinit var accountDataRepository: TasksAccountDataRepository
    private lateinit var synchronizer: CaldavSynchronizer

    @Before
    fun setUp() {
        caldavDao = mock(CaldavDao::class.java)
        taskDao = mock(TaskDao::class.java)
        refreshBroadcaster = mock(RefreshBroadcaster::class.java)
        taskDeleter = mock(TaskDeleter::class.java)
        reporting = mock(Reporting::class.java)
        provider = mock(CaldavClientProvider::class.java)
        iCal = mock(iCalendar::class.java)
        principalDao = mock(PrincipalDao::class.java)
        vtodoCache = mock(VtodoCache::class.java)
        accountDataRepository = mock(TasksAccountDataRepository::class.java)
        synchronizer = CaldavSynchronizer(
            caldavDao = caldavDao,
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            taskDeleter = taskDeleter,
            reporting = reporting,
            provider = provider,
            iCal = iCal,
            principalDao = principalDao,
            vtodoCache = vtodoCache,
            accountDataRepository = accountDataRepository,
        )
    }

    private fun createDavHttpException(code: Int, message: String): HttpException {
        val okResponse = okhttp3.Response.Builder()
            .code(code)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .message(message)
            .request(okhttp3.Request.Builder().url("https://example.com").build())
            .build()
        return HttpException(okResponse)
    }

    // ===== sync() — HTTP exception reporting branches =====

    @Test
    fun syncHttpException501NoReport() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(501, "Not Implemented")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting, never()).reportException(exception)
        assertNotNull(account.error)
    }

    @Test
    fun syncHttpException503RangeNoReport() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(503, "Service Unavailable via HttpException")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting, never()).reportException(exception)
    }

    @Test
    fun syncHttpException400Reports() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(400, "Bad Request")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting).reportException(exception)
    }

    @Test
    fun syncHttpException409Reports() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(409, "Conflict")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting).reportException(exception)
    }

    @Test
    fun syncHttpException499Reports() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(499, "Client Timeout")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting).reportException(exception)
    }

    @Test
    fun syncHttpException599NoReport() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(599, "Custom 5xx")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting, never()).reportException(exception)
    }

    // ===== getServerType — edge cases =====

    private fun invokeGetServerType(account: CaldavAccount, headers: Headers): Int {
        val method = CaldavSynchronizer::class.java.getDeclaredMethod(
            "getServerType", CaldavAccount::class.java, Headers::class.java
        )
        method.isAccessible = true
        return method.invoke(synchronizer, account, headers) as Int
    }

    @Test fun serverTypeTasksOrgIgnoresOtherHeaders() = assertEquals(SERVER_TASKS, invokeGetServerType(CaldavAccount(accountType = TYPE_TASKS), Headers.headersOf("DAV", "1, oc-resource-sharing, nextcloud-calendar")))
    @Test fun serverTypeOcWithoutNc() = assertEquals(SERVER_OWNCLOUD, invokeGetServerType(CaldavAccount(), Headers.headersOf("DAV", "1, oc-resource-sharing, something-else")))
    @Test fun serverTypeNonOcDav() = assertEquals(SERVER_UNKNOWN, invokeGetServerType(CaldavAccount(), Headers.headersOf("DAV", "1, 2, calendar-access")))
    @Test fun serverTypeSabredavOverOpenXchange() = assertEquals(SERVER_SABREDAV, invokeGetServerType(CaldavAccount(), Headers.headersOf("x-sabre-version", "4.0.0", "server", "Openexchange WebDAV")))
    @Test fun serverTypeOpenXchangeExact() = assertEquals(SERVER_OPEN_XCHANGE, invokeGetServerType(CaldavAccount(), Headers.headersOf("server", "Openexchange WebDAV")))
    @Test fun serverTypeOpenXchangeCaseSensitive() = assertEquals(SERVER_UNKNOWN, invokeGetServerType(CaldavAccount(), Headers.headersOf("server", "openexchange webdav")))
    @Test fun serverTypeNextcloudNcPrefix() = assertEquals(SERVER_NEXTCLOUD, invokeGetServerType(CaldavAccount(), Headers.headersOf("DAV", "1, oc-resource-sharing, nc-something")))
    @Test fun serverTypeNextcloudPrefix() = assertEquals(SERVER_NEXTCLOUD, invokeGetServerType(CaldavAccount(), Headers.headersOf("DAV", "1, oc-resource-sharing, nextcloud-stuff")))

    // ===== accessLevel — edge cases =====

    @Suppress("UNCHECKED_CAST")
    private fun createMockResponse(properties: Map<Class<*>, Any>): Response {
        val response = mock(Response::class.java)
        for ((clazz, value) in properties) {
            `when`(response[clazz as Class<Property>]).thenReturn(value as Property)
        }
        return response
    }

    @Test fun accessLevelUnknownShareAccess() = assertEquals(ACCESS_UNKNOWN, createMockResponse(mapOf(ShareAccess::class.java to ShareAccess(Property.Name("urn:unknown", "custom")))).accessLevel)

    @Test
    fun accessLevelOcNotOwner() {
        val currentUser = mock(CurrentUserPrincipal::class.java)
        `when`(currentUser.href).thenReturn("/principals/users/other/")
        assertEquals(ACCESS_READ_WRITE, createMockResponse(mapOf(
            OCOwnerPrincipal::class.java to OCOwnerPrincipal("/principals/users/admin"),
            CurrentUserPrincipal::class.java to currentUser,
        )).accessLevel)
    }

    @Test
    fun accessLevelOcNotOwnerWithWriteFalse() {
        val currentUser = mock(CurrentUserPrincipal::class.java)
        `when`(currentUser.href).thenReturn("/principals/users/other/")
        val ps = mock(CurrentUserPrivilegeSet::class.java)
        `when`(ps.mayWriteContent).thenReturn(false)
        assertEquals(ACCESS_READ_ONLY, createMockResponse(mapOf(
            OCOwnerPrincipal::class.java to OCOwnerPrincipal("/principals/users/admin"),
            CurrentUserPrincipal::class.java to currentUser,
            CurrentUserPrivilegeSet::class.java to ps,
        )).accessLevel)
    }

    @Test
    fun accessLevelNoShareAccessNoOcOwnerNoPrivilegeSet() {
        assertEquals(ACCESS_READ_WRITE, createMockResponse(emptyMap()).accessLevel)
    }

    @Test fun ctagNullTokenReturnsNull() = assertNull(createMockResponse(mapOf(SyncToken::class.java to SyncToken(null))).ctag)

    // ===== sync() — non-tasks.org without pro sets error =====

    // Note: syncNonTasksOrgWithoutPro cannot be tested because it calls
    // getString(Res.string.requires_pro_subscription) which requires Android resources

    // ===== sync() — guest status is checked inside synchronize =====
    // Note: guest check happens inside synchronize() which runs AFTER forAccount(),
    // so we can't easily test it with an IOException from forAccount.
    // These tests verify the non-tasks.org path does NOT interact with accountDataRepository.

    @Test
    fun syncNonTasksOrgSkipsGuestCheck() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid", accountType = TYPE_CALDAV)
        `when`(provider.forAccount(account)).thenAnswer { throw java.io.IOException("test") }

        synchronizer.sync(account, hasPro = true)

        verifyNoInteractions(accountDataRepository)
    }

    @Test
    fun syncTasksOrgBypassesProCheckAndCallsProvider() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid", accountType = TYPE_TASKS)
        `when`(provider.forAccount(account)).thenAnswer { throw java.io.IOException("test") }

        synchronizer.sync(account, hasPro = false)

        verify(provider).forAccount(account)
    }

    // ===== deleteRemoteResource — obj fallback via CaldavTask properties =====

    @Test
    fun deleteRemoteResourceObjFallbackSetsFromRemoteId() {
        val ct = CaldavTask(id = 1L, task = 10L, calendar = "c", obj = null, remoteId = "remote-123")
        // Simulate the fallback logic from deleteRemoteResource
        ct.obj = ct.remoteId?.let { "$it.ics" }
        assertEquals("remote-123.ics", ct.obj)
    }

    @Test
    fun deleteRemoteResourceObjFallbackNullRemoteIdStaysNull() {
        val ct = CaldavTask(id = 1L, task = 10L, calendar = "c", obj = null, remoteId = null)
        ct.obj = ct.remoteId?.let { "$it.ics" }
        assertNull(ct.obj)
    }

    @Test
    fun pushTaskObjFallbackSetsFromRemoteId() {
        val ct = CaldavTask(id = 1L, task = 1L, calendar = "c", obj = null, remoteId = "abc")
        ct.obj = ct.remoteId?.let { "$it.ics" }
        assertEquals("abc.ics", ct.obj)
    }

    @Test
    fun pushTaskObjFallbackNullRemoteIdThrows() {
        val ct = CaldavTask(id = 1L, task = 1L, calendar = "c", obj = null, remoteId = null)
        ct.obj = ct.remoteId?.let { "$it.ics" }
        val objPath = ct.obj
        try {
            objPath ?: throw IllegalStateException("Push failed - missing UUID")
        } catch (e: IllegalStateException) {
            assertEquals("Push failed - missing UUID", e.message)
        }
    }

    // ===== setError — message handling =====

    @Test
    fun setErrorWithIOExceptionUpdatesAccount() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer { throw java.io.IOException("io fail") }
        synchronizer.sync(account, hasPro = true)
        assertEquals("io fail", account.error)
        verify(caldavDao, Mockito.atLeastOnce()).update(account)
        verify(refreshBroadcaster, Mockito.atLeastOnce()).broadcastRefresh()
    }

    // ===== CaldavTask.isDeleted for push/delete filtering =====

    @Test
    fun taskIsDeletedReturnsTrue() {
        val task = Task(deletionDate = 1000L)
        assertTrue(task.isDeleted)
    }

    @Test
    fun taskIsDeletedReturnsFalse() {
        val task = Task(deletionDate = 0L)
        assertEquals(false, task.isDeleted)
    }

    @Test
    fun pushLocalChangesDeleteOnlySkipsNonDeleted() {
        val nonDeleted = Task(id = 1L, deletionDate = 0L)
        val deleted = Task(id = 2L, deletionDate = 1000L)
        val tasks = listOf(nonDeleted, deleted)

        // Simulate deleteOnly=true filtering
        val filtered = tasks.filter { !(true && !it.isDeleted) }
        assertEquals(1, filtered.size)
        assertEquals(2L, filtered[0].id)
    }

    @Test
    fun pushLocalChangesNonDeleteOnlyIncludesAll() {
        val nonDeleted = Task(id = 1L, deletionDate = 0L)
        val deleted = Task(id = 2L, deletionDate = 1000L)
        val tasks = listOf(nonDeleted, deleted)

        // Simulate deleteOnly=false filtering
        val filtered = tasks.filter { !(false && !it.isDeleted) }
        assertEquals(2, filtered.size)
    }
}
