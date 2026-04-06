package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.okhttp.Response
import at.bitfire.dav4jvm.property.caldav.GetCTag
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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.atLeastOnce
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
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN
import org.tasks.service.TaskDeleter

class CaldavSynchronizerDeepTest {

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

    private fun createDavHttpException(code: Int, message: String): at.bitfire.dav4jvm.okhttp.exception.HttpException {
        val okResponse = okhttp3.Response.Builder()
            .code(code)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .message(message)
            .request(okhttp3.Request.Builder().url("https://example.com").build())
            .build()
        return at.bitfire.dav4jvm.okhttp.exception.HttpException(okResponse)
    }

    // ===== sync() entry — tasks.org specific paths =====

    @Test
    fun syncTasksOrgWithBlankPasswordSetsUnauthorized() = runTest {
        val account = CaldavAccount(password = "", accountType = TYPE_TASKS)
        synchronizer.sync(account, hasPro = true)
        assertEquals(CaldavAccount.ERROR_UNAUTHORIZED, account.error)
        verify(caldavDao).update(account)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun syncTasksOrgWithNullPasswordSetsUnauthorized() = runTest {
        val account = CaldavAccount(password = null, accountType = TYPE_TASKS)
        synchronizer.sync(account, hasPro = true)
        assertEquals(CaldavAccount.ERROR_UNAUTHORIZED, account.error)
    }

    @Test
    fun syncTasksOrgBypassesProCheck() = runTest {
        val account = CaldavAccount(password = "pw", accountType = TYPE_TASKS, uuid = "u")
        `when`(provider.forAccount(account)).thenAnswer { throw java.io.IOException("test") }
        synchronizer.sync(account, hasPro = false)
        verify(provider).forAccount(account)
    }

    // ===== sync() exception handling =====

    @Test
    fun syncIOExceptionSetsErrorAndUpdatesAccount() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer { throw java.io.IOException("network fail") }
        synchronizer.sync(account, hasPro = true)
        assertEquals("network fail", account.error)
        verify(caldavDao).update(account)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun syncUnauthorizedExceptionSetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val okResponse = okhttp3.Response.Builder()
            .code(401).protocol(okhttp3.Protocol.HTTP_1_1).message("Unauthorized")
            .request(okhttp3.Request.Builder().url("https://example.com").build()).build()
        `when`(provider.forAccount(account)).thenAnswer {
            throw at.bitfire.dav4jvm.okhttp.exception.UnauthorizedException(okResponse)
        }
        synchronizer.sync(account, hasPro = true)
        assertNotNull(account.error)
        verify(caldavDao).update(account)
    }

    @Test
    fun syncServiceUnavailableExceptionSetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val okResponse = okhttp3.Response.Builder()
            .code(503).protocol(okhttp3.Protocol.HTTP_1_1).message("Service Unavailable")
            .request(okhttp3.Request.Builder().url("https://example.com").build()).build()
        `when`(provider.forAccount(account)).thenAnswer {
            throw at.bitfire.dav4jvm.okhttp.exception.ServiceUnavailableException(okResponse)
        }
        synchronizer.sync(account, hasPro = true)
        assertNotNull(account.error)
    }

    @Test
    fun syncKeyManagementExceptionSetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer {
            throw java.security.KeyManagementException("key problem")
        }
        synchronizer.sync(account, hasPro = true)
        assertEquals("key problem", account.error)
    }

    @Test
    fun syncNoSuchAlgorithmExceptionSetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer {
            throw java.security.NoSuchAlgorithmException("no algo")
        }
        synchronizer.sync(account, hasPro = true)
        assertEquals("no algo", account.error)
    }

    @Test
    fun syncHttpException402NoReport() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(402, "Payment Required")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        assertEquals("HTTP 402 Payment Required", account.error)
        verify(reporting, never()).reportException(exception)
    }

    @Test
    fun syncHttpException451NoReport() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(451, "Unavailable For Legal Reasons")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting, never()).reportException(exception)
    }

    @Test
    fun syncHttpException500NoReport() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(500, "Internal Server Error")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting, never()).reportException(exception)
    }

    @Test
    fun syncHttpException502NoReport() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(502, "Bad Gateway")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting, never()).reportException(exception)
    }

    @Test
    fun syncHttpException599NoReport() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(599, "Custom 5xx")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting, never()).reportException(exception)
    }

    @Test
    fun syncHttpException403Reports() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(403, "Forbidden")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting).reportException(exception)
    }

    @Test
    fun syncHttpException404Reports() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(404, "Not Found")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting).reportException(exception)
    }

    @Test
    fun syncGenericExceptionReports() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = RuntimeException("unexpected")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        assertEquals("unexpected", account.error)
        verify(reporting).reportException(exception)
    }

    // ===== getServerType (via reflection) =====

    private fun invokeGetServerType(account: CaldavAccount, headers: Headers): Int {
        val method = CaldavSynchronizer::class.java.getDeclaredMethod(
            "getServerType", CaldavAccount::class.java, Headers::class.java
        )
        method.isAccessible = true
        return method.invoke(synchronizer, account, headers) as Int
    }

    @Test fun serverTypeTasksOrg() = assertEquals(SERVER_TASKS, invokeGetServerType(CaldavAccount(accountType = TYPE_TASKS), Headers.headersOf()))
    @Test fun serverTypeNextcloud() = assertEquals(SERVER_NEXTCLOUD, invokeGetServerType(CaldavAccount(), Headers.headersOf("DAV", "1, oc-resource-sharing, nextcloud-calendar")))
    @Test fun serverTypeNextcloudNc() = assertEquals(SERVER_NEXTCLOUD, invokeGetServerType(CaldavAccount(), Headers.headersOf("DAV", "1, oc-resource-sharing, nc-calendar")))
    @Test fun serverTypeOwncloud() = assertEquals(SERVER_OWNCLOUD, invokeGetServerType(CaldavAccount(), Headers.headersOf("DAV", "1, oc-resource-sharing")))
    @Test fun serverTypeSabredav() = assertEquals(SERVER_SABREDAV, invokeGetServerType(CaldavAccount(), Headers.headersOf("x-sabre-version", "4.3.0")))
    @Test fun serverTypeOpenXchange() = assertEquals(SERVER_OPEN_XCHANGE, invokeGetServerType(CaldavAccount(), Headers.headersOf("server", "Openexchange WebDAV")))
    @Test fun serverTypeUnknown() = assertEquals(SERVER_UNKNOWN, invokeGetServerType(CaldavAccount(), Headers.headersOf()))
    @Test fun serverTypeBlankSabre() = assertEquals(SERVER_UNKNOWN, invokeGetServerType(CaldavAccount(), Headers.headersOf("x-sabre-version", "")))
    @Test fun serverTypeNonOcDav() = assertEquals(SERVER_UNKNOWN, invokeGetServerType(CaldavAccount(), Headers.headersOf("DAV", "1, 2, 3")))

    // ===== ctag companion property =====

    @Suppress("UNCHECKED_CAST")
    private fun createMockResponse(properties: Map<Class<*>, Any>): Response {
        val response = mock(Response::class.java)
        for ((clazz, value) in properties) {
            `when`(response[clazz as Class<Property>]).thenReturn(value as Property)
        }
        return response
    }

    @Test fun ctagFromSyncToken() = assertEquals("sync-token-val", createMockResponse(mapOf(SyncToken::class.java to SyncToken("sync-token-val"), GetCTag::class.java to GetCTag("ctag-val"))).ctag)
    @Test fun ctagFromGetCTag() = assertEquals("ctag-only", createMockResponse(mapOf(GetCTag::class.java to GetCTag("ctag-only"))).ctag)
    @Test fun ctagNull() = assertNull(createMockResponse(emptyMap()).ctag)
    @Test fun ctagSyncTokenAlone() = assertEquals("token-alone", createMockResponse(mapOf(SyncToken::class.java to SyncToken("token-alone"))).ctag)

    // ===== accessLevel companion property =====

    @Test fun accessLevelSharedOwner() = assertEquals(ACCESS_OWNER, createMockResponse(mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.SHARED_OWNER))).accessLevel)
    @Test fun accessLevelNotShared() = assertEquals(ACCESS_OWNER, createMockResponse(mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.NOT_SHARED))).accessLevel)
    @Test fun accessLevelReadWrite() = assertEquals(ACCESS_READ_WRITE, createMockResponse(mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.READ_WRITE))).accessLevel)
    @Test fun accessLevelRead() = assertEquals(ACCESS_READ_ONLY, createMockResponse(mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.READ))).accessLevel)
    @Test fun accessLevelNoAccess() = assertEquals(ACCESS_READ_ONLY, createMockResponse(mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.NO_ACCESS))).accessLevel)
    @Test fun accessLevelUnknown() = assertEquals(ACCESS_UNKNOWN, createMockResponse(mapOf(ShareAccess::class.java to ShareAccess(Property.Name("urn:unknown", "x")))).accessLevel)

    @Test
    fun accessLevelOwncloudOwner() {
        val currentUser = mock(CurrentUserPrincipal::class.java)
        `when`(currentUser.href).thenReturn("/principals/users/admin/")
        assertEquals(ACCESS_OWNER, createMockResponse(mapOf(
            OCOwnerPrincipal::class.java to OCOwnerPrincipal("/principals/users/admin"),
            CurrentUserPrincipal::class.java to currentUser,
        )).accessLevel)
    }

    @Test
    fun accessLevelPrivilegeWritable() {
        val ps = mock(CurrentUserPrivilegeSet::class.java)
        `when`(ps.mayWriteContent).thenReturn(true)
        assertEquals(ACCESS_READ_WRITE, createMockResponse(mapOf(CurrentUserPrivilegeSet::class.java to ps)).accessLevel)
    }

    @Test
    fun accessLevelPrivilegeReadOnly() {
        val ps = mock(CurrentUserPrivilegeSet::class.java)
        `when`(ps.mayWriteContent).thenReturn(false)
        assertEquals(ACCESS_READ_ONLY, createMockResponse(mapOf(CurrentUserPrivilegeSet::class.java to ps)).accessLevel)
    }

    @Test fun accessLevelDefault() = assertEquals(ACCESS_READ_WRITE, createMockResponse(emptyMap()).accessLevel)

    @Test
    fun accessLevelOcNotOwnerFallsThrough() {
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

    // ===== setError tested indirectly via IOException =====

    @Test
    fun setErrorFromIOException() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer { throw java.io.IOException("io fail") }
        synchronizer.sync(account, hasPro = true)
        assertEquals("io fail", account.error)
        verify(caldavDao, atLeastOnce()).update(account)
        verify(refreshBroadcaster, atLeastOnce()).broadcastRefresh()
    }

    @Test
    fun setErrorClearsOnIOExceptionThenSuccess() = runTest {
        // First sync with error
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer { throw java.io.IOException("fail") }
        synchronizer.sync(account, hasPro = true)
        assertEquals("fail", account.error)
    }
}
