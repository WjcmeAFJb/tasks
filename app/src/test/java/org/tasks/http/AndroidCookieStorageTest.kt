package org.tasks.http

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AndroidCookieStorageTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mock()
        prefs = mock()
        editor = mock()
        whenever(context.getSharedPreferences(any(), eq(Context.MODE_PRIVATE))).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(prefs.all).thenReturn(emptyMap<String, Any>())
    }

    @Test
    fun constructionWithNullKey() {
        whenever(prefs.all).thenReturn(emptyMap<String, Any>())
        val storage = AndroidCookieStorage(context, null)
        // Should not throw
        storage.close() // no-op
    }

    @Test
    fun constructionWithKey() {
        whenever(prefs.all).thenReturn(emptyMap<String, Any>())
        val storage = AndroidCookieStorage(context, "test_user")
        storage.close()
    }

    @Test
    fun getReturnsEmptyForNewStorage() = runTest {
        whenever(prefs.all).thenReturn(emptyMap<String, Any>())
        val storage = AndroidCookieStorage(context, "key")
        val url = io.ktor.http.Url("https://example.com")
        val cookies = storage.get(url)
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun closeIsNoOp() {
        whenever(prefs.all).thenReturn(emptyMap<String, Any>())
        val storage = AndroidCookieStorage(context, "key")
        storage.close() // Should not throw
    }

    @Test
    fun initLoadsFromPrefs() {
        whenever(prefs.all).thenReturn(
            mapOf<String, Any>(
                "session" to "session=abc123; Path=/; Domain=example.com"
            )
        )
        val storage = AndroidCookieStorage(context, "key")
        // Constructor reads prefs.all and parses cookie strings
        // No crash = success
        storage.close()
    }

    @Test
    fun initSkipsNonStringPrefs() {
        whenever(prefs.all).thenReturn(
            mapOf<String, Any>(
                "intVal" to 42,
                "boolVal" to true,
            )
        )
        // Should not throw, just logs
        val storage = AndroidCookieStorage(context, "key")
        storage.close()
    }

    @Test
    fun addCookieStoresInPrefs() = runTest {
        whenever(prefs.all).thenReturn(emptyMap<String, Any>())
        val storage = AndroidCookieStorage(context, "key")
        val url = io.ktor.http.Url("https://example.com")
        val cookie = io.ktor.http.Cookie("session", "abc123", domain = ".example.com", path = "/")
        storage.addCookie(url, cookie)
        val cookies = storage.get(url)
        assertEquals(1, cookies.size)
        assertEquals("session", cookies[0].name)
        assertEquals("abc123", cookies[0].value)
    }

    @Test
    fun addCookieOverwritesSameName() = runTest {
        whenever(prefs.all).thenReturn(emptyMap<String, Any>())
        val storage = AndroidCookieStorage(context, "key")
        val url = io.ktor.http.Url("https://example.com")
        storage.addCookie(url, io.ktor.http.Cookie("session", "old", domain = ".example.com", path = "/"))
        storage.addCookie(url, io.ktor.http.Cookie("session", "new", domain = ".example.com", path = "/"))
        val cookies = storage.get(url)
        assertEquals(1, cookies.size)
        assertEquals("new", cookies[0].value)
    }

    @Test
    fun getFiltersNonMatchingDomain() = runTest {
        whenever(prefs.all).thenReturn(emptyMap<String, Any>())
        val storage = AndroidCookieStorage(context, "key")
        val url1 = io.ktor.http.Url("https://example.com")
        val url2 = io.ktor.http.Url("https://other.com")
        storage.addCookie(url1, io.ktor.http.Cookie("session", "abc", domain = ".example.com", path = "/"))
        val cookies = storage.get(url2)
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun multipleCookiesSameDomain() = runTest {
        whenever(prefs.all).thenReturn(emptyMap<String, Any>())
        val storage = AndroidCookieStorage(context, "key")
        val url = io.ktor.http.Url("https://example.com")
        storage.addCookie(url, io.ktor.http.Cookie("session", "abc", domain = ".example.com", path = "/"))
        storage.addCookie(url, io.ktor.http.Cookie("token", "xyz", domain = ".example.com", path = "/"))
        val cookies = storage.get(url)
        assertEquals(2, cookies.size)
        assertTrue(cookies.any { it.name == "session" })
        assertTrue(cookies.any { it.name == "token" })
    }
}
