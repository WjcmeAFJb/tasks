package org.tasks.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpErrorHandlerDeepTest {

    // ===== GraphError.isTokenError() comprehensive =====

    @Test
    fun invalidAuthenticationTokenIsTokenError() {
        assertTrue(GraphError("InvalidAuthenticationToken", "msg").isTokenError())
    }

    @Test
    fun authenticationErrorIsTokenError() {
        assertTrue(GraphError("AuthenticationError", "msg").isTokenError())
    }

    @Test
    fun unknownErrorIsTokenError() {
        assertTrue(GraphError("UnknownError", "msg").isTokenError())
    }

    @Test
    fun notFoundCodeIsNotTokenError() {
        assertFalse(GraphError("NotFound", "msg").isTokenError())
    }

    @Test
    fun accessDeniedIsNotTokenError() {
        assertFalse(GraphError("AccessDenied", "msg").isTokenError())
    }

    @Test
    fun serviceNotAvailableIsNotTokenError() {
        assertFalse(GraphError("ServiceNotAvailable", "msg").isTokenError())
    }

    @Test
    fun caseSensitiveCheckInvalidauthenticationtoken() {
        assertFalse(GraphError("invalidauthenticationtoken", "msg").isTokenError())
    }

    @Test
    fun caseSensitiveCheckAUTHENTICATIONERROR() {
        assertFalse(GraphError("AUTHENTICATIONERROR", "msg").isTokenError())
    }

    @Test
    fun caseSensitiveCheckUnknownERROR() {
        assertFalse(GraphError("unknownerror", "msg").isTokenError())
    }

    // ===== GraphError data class =====

    @Test
    fun graphErrorCopy() {
        val error = GraphError("code1", "msg1")
        val copy = error.copy(code = "code2")
        assertEquals("code2", copy.code)
        assertEquals("msg1", copy.message)
    }

    @Test
    fun graphErrorEquality() {
        val e1 = GraphError("code", "msg")
        val e2 = GraphError("code", "msg")
        assertEquals(e1, e2)
    }

    @Test
    fun graphErrorWithInnerError() {
        val inner = GraphInnerError("2024-01-01", "req-1", "cli-1")
        val error = GraphError("code", "msg", inner)
        assertEquals("req-1", error.innerError!!.requestId)
        assertEquals("cli-1", error.innerError!!.clientRequestId)
        assertEquals("2024-01-01", error.innerError!!.date)
    }

    @Test
    fun graphErrorToStringContainsCode() {
        val error = GraphError("TestCode", "TestMessage")
        val str = error.toString()
        assertTrue(str.contains("TestCode"))
        assertTrue(str.contains("TestMessage"))
    }

    // ===== GraphInnerError =====

    @Test
    fun graphInnerErrorEquality() {
        val i1 = GraphInnerError("d", "r", "c")
        val i2 = GraphInnerError("d", "r", "c")
        assertEquals(i1, i2)
    }

    @Test
    fun graphInnerErrorCopy() {
        val inner = GraphInnerError("d1", "r1", "c1")
        val copy = inner.copy(requestId = "r2")
        assertEquals("r2", copy.requestId)
        assertEquals("d1", copy.date)
    }

    // ===== GraphErrorResponse =====

    @Test
    fun graphErrorResponseEquality() {
        val error = GraphError("code", "msg")
        val r1 = GraphErrorResponse(error)
        val r2 = GraphErrorResponse(error)
        assertEquals(r1, r2)
    }

    @Test
    fun graphErrorResponseCopy() {
        val r1 = GraphErrorResponse(GraphError("c1", "m1"))
        val r2 = r1.copy(error = GraphError("c2", "m2"))
        assertEquals("c2", r2.error.code)
    }

    // ===== Exception hierarchy =====

    @Test
    fun networkExceptionIsException() {
        val ex = NetworkException("test")
        assertTrue(ex is Exception)
    }

    @Test
    fun unauthorizedExtendsNetworkException() {
        val ex = UnauthorizedException("auth error")
        assertTrue(ex is NetworkException)
        assertTrue(ex is Exception)
    }

    @Test
    fun notFoundExtendsNetworkException() {
        val ex = NotFoundException("404")
        assertTrue(ex is NetworkException)
    }

    @Test
    fun serviceUnavailableExtendsNetworkException() {
        val ex = ServiceUnavailableException("503")
        assertTrue(ex is NetworkException)
    }

    @Test
    fun httpExceptionExtendsNetworkException() {
        val ex = HttpException(418, "I'm a teapot")
        assertTrue(ex is NetworkException)
        assertEquals(418, ex.code)
    }

    @Test
    fun networkExceptionNullCause() {
        val ex = NetworkException("msg", null)
        assertNull(ex.cause)
    }

    @Test
    fun networkExceptionWithCausePreservesCause() {
        val cause = IllegalStateException("root")
        val ex = NetworkException("msg", cause)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun unauthorizedExceptionWithCause() {
        val cause = RuntimeException("expired")
        val ex = UnauthorizedException("auth", cause)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun notFoundExceptionWithCause() {
        val cause = RuntimeException("missing")
        val ex = NotFoundException("not found", cause)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun serviceUnavailableWithCause() {
        val cause = RuntimeException("overloaded")
        val ex = ServiceUnavailableException("unavailable", cause)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun httpExceptionCode400() {
        assertEquals(400, HttpException(400, "bad request").code)
    }

    @Test
    fun httpExceptionCode401() {
        assertEquals(401, HttpException(401, "unauthorized").code)
    }

    @Test
    fun httpExceptionCode403() {
        assertEquals(403, HttpException(403, "forbidden").code)
    }

    @Test
    fun httpExceptionCode500() {
        assertEquals(500, HttpException(500, "internal server error").code)
    }

    @Test
    fun httpExceptionCode503() {
        assertEquals(503, HttpException(503, "service unavailable").code)
    }

    // ===== Plugin =====

    @Test
    fun pluginKeyIsStable() {
        assertEquals("HttpErrorHandler", HttpErrorHandler.key.name)
    }

    @Test
    fun pluginPrepareReturnsHandler() {
        val handler = HttpErrorHandler.prepare {}
        assertNotNull(handler)
    }

    @Test
    fun pluginConfigHasDefaultErrorHandler() {
        val config = HttpErrorHandler.Config()
        assertNotNull(config.handleError)
    }
}
