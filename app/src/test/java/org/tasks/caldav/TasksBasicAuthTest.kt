package org.tasks.caldav

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasksBasicAuthTest {

    private var capturedRequest: Request? = null

    private fun intercept(auth: TasksBasicAuth): Request {
        val chain = object : Interceptor.Chain {
            private val originalRequest = Request.Builder().url("https://example.com/test").build()
            override fun request(): Request = originalRequest
            override fun proceed(request: Request): Response {
                capturedRequest = request
                return Response.Builder()
                    .code(200)
                    .protocol(Protocol.HTTP_1_1)
                    .message("OK")
                    .request(request)
                    .build()
            }
            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun readTimeoutMillis() = 0
            override fun writeTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
        auth.intercept(chain)
        return capturedRequest!!
    }

    @Test
    fun addsAuthorizationHeader() {
        val auth = TasksBasicAuth(user = "user", token = "token", tosVersion = 1)
        val req = intercept(auth)
        assertNotNull(req.header("Authorization"))
    }

    @Test
    fun authorizationIsBasicAuth() {
        val auth = TasksBasicAuth(user = "user", token = "token", tosVersion = 1)
        val req = intercept(auth)
        assertTrue(req.header("Authorization")!!.startsWith("Basic "))
    }

    @Test
    fun basicAuthCredentialsAreCorrect() {
        val auth = TasksBasicAuth(user = "testuser", token = "testtoken", tosVersion = 0)
        val req = intercept(auth)
        val authHeader = req.header("Authorization")!!
        val decoded = java.util.Base64.getDecoder()
            .decode(authHeader.removePrefix("Basic "))
            .toString(Charsets.UTF_8)
        assertEquals("testuser:testtoken", decoded)
    }

    @Test
    fun addsTosVersionHeader() {
        val auth = TasksBasicAuth(user = "user", token = "token", tosVersion = 5)
        val req = intercept(auth)
        assertEquals("5", req.header("tasks-tos-version"))
    }

    @Test
    fun tosVersionZero() {
        val auth = TasksBasicAuth(user = "user", token = "token", tosVersion = 0)
        val req = intercept(auth)
        assertEquals("0", req.header("tasks-tos-version"))
    }

    @Test
    fun addsPushTokenHeader() {
        val auth = TasksBasicAuth(
            user = "user", token = "token", tosVersion = 1, pushToken = "push-abc"
        )
        val req = intercept(auth)
        assertEquals("push-abc", req.header("X-Push-Token"))
    }

    @Test
    fun noPushTokenWhenNull() {
        val auth = TasksBasicAuth(
            user = "user", token = "token", tosVersion = 1, pushToken = null
        )
        val req = intercept(auth)
        assertNull(req.header("X-Push-Token"))
    }

    @Test
    fun addsSubscriptionSkuHeader() {
        val auth = TasksBasicAuth(
            user = "user",
            token = "token",
            tosVersion = 1,
            subscriptionInfo = TasksBasicAuth.SubscriptionInfo(
                sku = "test-sku",
                purchaseToken = "pt-123"
            )
        )
        val req = intercept(auth)
        assertEquals("test-sku", req.header("tasks-sku"))
    }

    @Test
    fun addsSubscriptionTokenHeader() {
        val auth = TasksBasicAuth(
            user = "user",
            token = "token",
            tosVersion = 1,
            subscriptionInfo = TasksBasicAuth.SubscriptionInfo(
                sku = "test-sku",
                purchaseToken = "pt-123"
            )
        )
        val req = intercept(auth)
        assertEquals("pt-123", req.header("tasks-token"))
    }

    @Test
    fun noSubscriptionHeadersWhenNull() {
        val auth = TasksBasicAuth(
            user = "user", token = "token", tosVersion = 1, subscriptionInfo = null
        )
        val req = intercept(auth)
        assertNull(req.header("tasks-sku"))
        assertNull(req.header("tasks-token"))
    }

    @Test
    fun userFieldIsAccessible() {
        val auth = TasksBasicAuth(user = "myuser", token = "mytoken", tosVersion = 3)
        assertEquals("myuser", auth.user)
    }

    @Test
    fun subscriptionInfoEquality() {
        val info1 = TasksBasicAuth.SubscriptionInfo("sku1", "token1")
        val info2 = TasksBasicAuth.SubscriptionInfo("sku1", "token1")
        assertEquals(info1, info2)
    }

    @Test
    fun subscriptionInfoCopy() {
        val info = TasksBasicAuth.SubscriptionInfo("sku", "token")
        val copy = info.copy(sku = "new-sku")
        assertEquals("new-sku", copy.sku)
        assertEquals("token", copy.purchaseToken)
    }
}
