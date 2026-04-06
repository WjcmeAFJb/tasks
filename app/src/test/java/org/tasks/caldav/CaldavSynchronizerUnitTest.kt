package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.okhttp.Response
import at.bitfire.dav4jvm.property.caldav.GetCTag
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrincipal
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrivilegeSet
import at.bitfire.dav4jvm.property.webdav.SyncToken
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
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
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN
import org.tasks.service.TaskDeleter

class CaldavSynchronizerUnitTest {

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

    // ===== sync() error paths =====
    // Note: Tests for no-pro and blank-password-non-tasks paths are skipped because
    // they call getString() which requires Android Resources (not available in unit tests).

    @Test
    fun syncWithBlankPasswordOnTasksOrgSetsUnauthorized() = runTest {
        val account = CaldavAccount(password = "", accountType = TYPE_TASKS)
        synchronizer.sync(account, hasPro = true)
        assertEquals(CaldavAccount.ERROR_UNAUTHORIZED, account.error)
    }

    @Test
    fun syncWithNullPasswordOnTasksOrgSetsUnauthorized() = runTest {
        val account = CaldavAccount(password = null, accountType = TYPE_TASKS)
        synchronizer.sync(account, hasPro = true)
        assertEquals(CaldavAccount.ERROR_UNAUTHORIZED, account.error)
    }

    @Test
    fun syncIOExceptionSetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer { throw java.io.IOException("network error") }
        synchronizer.sync(account, hasPro = true)
        assertEquals("network error", account.error)
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

    @Test
    fun syncHttpExceptionWith500SetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(500, "Internal Server Error")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        assertEquals("HTTP 500 Internal Server Error", account.error)
    }

    @Test
    fun syncHttpExceptionWith402SetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(402, "Payment Required")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        assertEquals("HTTP 402 Payment Required", account.error)
    }

    @Test
    fun syncHttpExceptionWith451SetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(451, "Unavailable For Legal Reasons")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        assertEquals("HTTP 451 Unavailable For Legal Reasons", account.error)
    }

    @Test
    fun syncHttpExceptionWith403ReportsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = createDavHttpException(403, "Forbidden")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(reporting).reportException(exception)
    }

    @Test
    fun syncUnauthorizedExceptionSetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val okResponse = okhttp3.Response.Builder()
            .code(401)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .message("Unauthorized")
            .request(okhttp3.Request.Builder().url("https://example.com").build())
            .build()
        val exception = at.bitfire.dav4jvm.okhttp.exception.UnauthorizedException(okResponse)
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(caldavDao).update(account)
    }

    @Test
    fun syncServiceUnavailableSetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val okResponse = okhttp3.Response.Builder()
            .code(503)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .message("Service Unavailable")
            .request(okhttp3.Request.Builder().url("https://example.com").build())
            .build()
        val exception = at.bitfire.dav4jvm.okhttp.exception.ServiceUnavailableException(okResponse)
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        verify(caldavDao).update(account)
    }

    @Test
    fun syncGenericExceptionSetsErrorAndReports() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        val exception = RuntimeException("unexpected error")
        `when`(provider.forAccount(account)).thenAnswer { throw exception }
        synchronizer.sync(account, hasPro = true)
        assertEquals("unexpected error", account.error)
        verify(reporting).reportException(exception)
    }

    @Test
    fun syncTasksOrgAllowedWithoutPro() = runTest {
        val account = CaldavAccount(password = "pw", accountType = TYPE_TASKS, uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer { throw java.io.IOException("test") }
        synchronizer.sync(account, hasPro = false)
        verify(provider).forAccount(account)
    }

    @Test
    fun syncKeyManagementExceptionSetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer { throw java.security.KeyManagementException("key error") }
        synchronizer.sync(account, hasPro = true)
        assertEquals("key error", account.error)
    }

    @Test
    fun syncNoSuchAlgorithmExceptionSetsError() = runTest {
        val account = CaldavAccount(password = "pw", uuid = "test-uuid")
        `when`(provider.forAccount(account)).thenAnswer { throw java.security.NoSuchAlgorithmException("algo missing") }
        synchronizer.sync(account, hasPro = true)
        assertEquals("algo missing", account.error)
    }

    // ===== getServerType tests (via reflection) =====

    @Test
    fun serverTypeForTasksOrg() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        val headers = Headers.headersOf()
        assertEquals(SERVER_TASKS, invokeGetServerType(account, headers))
    }

    @Test
    fun serverTypeForNextcloud() {
        val account = CaldavAccount()
        val headers = Headers.headersOf("DAV", "1, oc-resource-sharing, nextcloud-calendar")
        assertEquals(SERVER_NEXTCLOUD, invokeGetServerType(account, headers))
    }

    @Test
    fun serverTypeForNextcloudNcPrefix() {
        val account = CaldavAccount()
        val headers = Headers.headersOf("DAV", "1, oc-resource-sharing, nc-calendar")
        assertEquals(SERVER_NEXTCLOUD, invokeGetServerType(account, headers))
    }

    @Test
    fun serverTypeForOwncloud() {
        val account = CaldavAccount()
        val headers = Headers.headersOf("DAV", "1, oc-resource-sharing")
        assertEquals(SERVER_OWNCLOUD, invokeGetServerType(account, headers))
    }

    @Test
    fun serverTypeForSabredav() {
        val account = CaldavAccount()
        val headers = Headers.headersOf("x-sabre-version", "4.3.0")
        assertEquals(SERVER_SABREDAV, invokeGetServerType(account, headers))
    }

    @Test
    fun serverTypeForOpenXchange() {
        val account = CaldavAccount()
        val headers = Headers.headersOf("server", "Openexchange WebDAV")
        assertEquals(SERVER_OPEN_XCHANGE, invokeGetServerType(account, headers))
    }

    @Test
    fun serverTypeUnknown() {
        val account = CaldavAccount()
        val headers = Headers.headersOf()
        assertEquals(SERVER_UNKNOWN, invokeGetServerType(account, headers))
    }

    @Test
    fun serverTypeForBlankSabreVersion() {
        val account = CaldavAccount()
        val headers = Headers.headersOf("x-sabre-version", "")
        assertEquals(SERVER_UNKNOWN, invokeGetServerType(account, headers))
    }

    private fun invokeGetServerType(account: CaldavAccount, headers: Headers): Int {
        val method = CaldavSynchronizer::class.java.getDeclaredMethod(
            "getServerType", CaldavAccount::class.java, Headers::class.java
        )
        method.isAccessible = true
        return method.invoke(synchronizer, account, headers) as Int
    }

    // ===== Companion: ctag extension =====

    @Test
    fun ctagFromSyncToken() {
        val response = createMockResponse(
            mapOf(SyncToken::class.java to SyncToken("sync-token-123"))
        )
        assertEquals("sync-token-123", response.ctag)
    }

    @Test
    fun ctagFromGetCTag() {
        val response = createMockResponse(
            mapOf(GetCTag::class.java to GetCTag("ctag-456"))
        )
        assertEquals("ctag-456", response.ctag)
    }

    @Test
    fun ctagPrefersTokenOverCtag() {
        val response = createMockResponse(
            mapOf(
                SyncToken::class.java to SyncToken("sync-token"),
                GetCTag::class.java to GetCTag("ctag-value")
            )
        )
        assertEquals("sync-token", response.ctag)
    }

    @Test
    fun ctagNullWhenNeitherPresent() {
        val response = createMockResponse(emptyMap())
        assertNull(response.ctag)
    }

    // ===== Companion: accessLevel extension =====

    @Test
    fun accessLevelSharedOwner() {
        val response = createMockResponse(
            mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.SHARED_OWNER))
        )
        assertEquals(ACCESS_OWNER, response.accessLevel)
    }

    @Test
    fun accessLevelNotShared() {
        val response = createMockResponse(
            mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.NOT_SHARED))
        )
        assertEquals(ACCESS_OWNER, response.accessLevel)
    }

    @Test
    fun accessLevelReadWrite() {
        val response = createMockResponse(
            mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.READ_WRITE))
        )
        assertEquals(ACCESS_READ_WRITE, response.accessLevel)
    }

    @Test
    fun accessLevelNoAccess() {
        val response = createMockResponse(
            mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.NO_ACCESS))
        )
        assertEquals(ACCESS_READ_ONLY, response.accessLevel)
    }

    @Test
    fun accessLevelRead() {
        val response = createMockResponse(
            mapOf(ShareAccess::class.java to ShareAccess(ShareAccess.READ))
        )
        assertEquals(ACCESS_READ_ONLY, response.accessLevel)
    }

    @Test
    fun accessLevelUnknownShareAccess() {
        val response = createMockResponse(
            mapOf(ShareAccess::class.java to ShareAccess(Property.Name("urn:unknown", "something")))
        )
        assertEquals(ACCESS_UNKNOWN, response.accessLevel)
    }

    @Test
    fun accessLevelFromPrivilegeSetWritable() {
        val privilegeSet = mock(CurrentUserPrivilegeSet::class.java)
        `when`(privilegeSet.mayWriteContent).thenReturn(true)
        val response = createMockResponse(
            mapOf(CurrentUserPrivilegeSet::class.java to privilegeSet)
        )
        assertEquals(ACCESS_READ_WRITE, response.accessLevel)
    }

    @Test
    fun accessLevelFromPrivilegeSetReadOnly() {
        val privilegeSet = mock(CurrentUserPrivilegeSet::class.java)
        `when`(privilegeSet.mayWriteContent).thenReturn(false)
        val response = createMockResponse(
            mapOf(CurrentUserPrivilegeSet::class.java to privilegeSet)
        )
        assertEquals(ACCESS_READ_ONLY, response.accessLevel)
    }

    @Test
    fun accessLevelFromPrivilegeSetNull() {
        val response = createMockResponse(emptyMap())
        assertEquals(ACCESS_READ_WRITE, response.accessLevel)
    }

    @Test
    fun accessLevelOwncloudOwner() {
        val currentUser = mock(CurrentUserPrincipal::class.java)
        `when`(currentUser.href).thenReturn("/principals/users/admin/")
        val response = createMockResponse(
            mapOf(
                OCOwnerPrincipal::class.java to OCOwnerPrincipal("/principals/users/admin"),
                CurrentUserPrincipal::class.java to currentUser,
            )
        )
        assertEquals(ACCESS_OWNER, response.accessLevel)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createMockResponse(properties: Map<Class<*>, Any>): Response {
        val response = mock(Response::class.java)
        for ((clazz, value) in properties) {
            `when`(response[clazz as Class<Property>]).thenReturn(value as Property)
        }
        return response
    }
}
