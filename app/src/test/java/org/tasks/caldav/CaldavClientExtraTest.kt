package org.tasks.caldav

import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OPEN_XCHANGE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN

class CaldavClientExtraTest {

    private lateinit var provider: CaldavClientProvider
    private lateinit var httpClient: OkHttpClient
    private val testUrl = "https://example.com/calendars/user/".toHttpUrl()

    @Before
    fun setUp() {
        provider = mock()
        httpClient = OkHttpClient.Builder().build()
    }

    private fun createClient(): CaldavClient {
        return CaldavClient(provider, httpClient, testUrl)
    }

    // ===== getMkcolString tests (via reflection) =====

    @Test
    fun getMkcolStringContainsDisplayName() {
        val client = createClient()
        val method = CaldavClient::class.java.getDeclaredMethod(
            "getMkcolString", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val result = method.invoke(client, "My Calendar", 0) as String
        assertTrue(result.contains("My Calendar"))
    }

    @Test
    fun getMkcolStringContainsMkcol() {
        val client = createClient()
        val method = CaldavClient::class.java.getDeclaredMethod(
            "getMkcolString", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val result = method.invoke(client, "Test", 0) as String
        assertTrue(result.contains("mkcol"))
    }

    @Test
    fun getMkcolStringContainsVtodo() {
        val client = createClient()
        val method = CaldavClient::class.java.getDeclaredMethod(
            "getMkcolString", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val result = method.invoke(client, "Test", 0) as String
        assertTrue(result.contains("VTODO"))
    }

    @Test
    fun getMkcolStringContainsResourceType() {
        val client = createClient()
        val method = CaldavClient::class.java.getDeclaredMethod(
            "getMkcolString", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val result = method.invoke(client, "Test", 0) as String
        assertTrue(result.contains("resourcetype"))
        assertTrue(result.contains("collection"))
        assertTrue(result.contains("calendar"))
    }

    @Test
    fun getMkcolStringWithColorIncludesColor() {
        val client = createClient()
        val method = CaldavClient::class.java.getDeclaredMethod(
            "getMkcolString", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val color = 0xFF0000FF.toInt() // Blue with full alpha
        val result = method.invoke(client, "Test", color) as String
        assertTrue(result.contains("calendar-color"))
    }

    @Test
    fun getMkcolStringWithoutColorOmitsColor() {
        val client = createClient()
        val method = CaldavClient::class.java.getDeclaredMethod(
            "getMkcolString", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val result = method.invoke(client, "Test", 0) as String
        assertTrue(!result.contains("calendar-color"))
    }

    @Test
    fun getMkcolStringIsValidXml() {
        val client = createClient()
        val method = CaldavClient::class.java.getDeclaredMethod(
            "getMkcolString", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val result = method.invoke(client, "My Calendar", 0xFF0000FF.toInt()) as String
        // Should not throw when parsed as XML
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(result.byteInputStream())
        assertNotNull(doc)
    }

    @Test
    fun getMkcolStringSpecialCharactersInName() {
        val client = createClient()
        val method = CaldavClient::class.java.getDeclaredMethod(
            "getMkcolString", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val result = method.invoke(client, "Test <>&\"'", 0) as String
        // XML should still be valid even with special characters
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(result.byteInputStream())
        assertNotNull(doc)
    }

    // ===== setColor format tests =====

    @Test
    fun setColorFormatsCorrectly() {
        val client = createClient()
        val method = CaldavClient::class.java.getDeclaredMethod(
            "getMkcolString", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        // Color 0xAABBCCDD: RGB = BBCCDD, Alpha = AA
        val color = 0xAABBCCDD.toInt()
        val result = method.invoke(client, "Test", color) as String
        // Expected format: #BBCCDDAA
        assertTrue(result.contains("#BBCCDDAA"))
    }

    // ===== share routing tests =====

    @Test
    fun shareThrowsForUnknownServer() {
        val client = createClient()
        val account = CaldavAccount(serverType = SERVER_UNKNOWN)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { client.share(account, "/principals/users/alice") }
        }
    }

    @Test
    fun shareThrowsForOpenXchangeServer() {
        val client = createClient()
        val account = CaldavAccount(serverType = SERVER_OPEN_XCHANGE)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { client.share(account, "/principals/users/alice") }
        }
    }

    // ===== removePrincipal routing tests =====

    @Test
    fun removePrincipalThrowsForUnknownServer() {
        val client = createClient()
        val account = CaldavAccount(serverType = SERVER_UNKNOWN)
        val calendar = org.tasks.data.entity.CaldavCalendar(url = "https://example.com/calendars/cal1/")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { client.removePrincipal(account, calendar, "/principals/users/alice") }
        }
    }

    @Test
    fun removePrincipalThrowsForOpenXchangeServer() {
        val client = createClient()
        val account = CaldavAccount(serverType = SERVER_OPEN_XCHANGE)
        val calendar = org.tasks.data.entity.CaldavCalendar(url = "https://example.com/calendars/cal1/")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { client.removePrincipal(account, calendar, "/principals/users/alice") }
        }
    }

    // ===== calendarProperties companion field =====

    private fun runBlocking(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking { block() }
    }
}
