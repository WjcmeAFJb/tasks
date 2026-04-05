package org.tasks.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpErrorHandlerTest {

    // ===== GraphError.isTokenError() =====

    @Test
    fun invalidAuthenticationTokenIsTokenError() {
        val error = GraphError(code = "InvalidAuthenticationToken", message = "Token expired")
        assertTrue(error.isTokenError())
    }

    @Test
    fun authenticationErrorIsTokenError() {
        val error = GraphError(code = "AuthenticationError", message = "Bad token")
        assertTrue(error.isTokenError())
    }

    @Test
    fun unknownErrorIsTokenError() {
        val error = GraphError(code = "UnknownError", message = "Unknown")
        assertTrue(error.isTokenError())
    }

    @Test
    fun notFoundIsNotTokenError() {
        val error = GraphError(code = "NotFound", message = "Resource not found")
        assertFalse(error.isTokenError())
    }

    @Test
    fun badRequestIsNotTokenError() {
        val error = GraphError(code = "BadRequest", message = "Bad request")
        assertFalse(error.isTokenError())
    }

    @Test
    fun emptyCodeIsNotTokenError() {
        val error = GraphError(code = "", message = "")
        assertFalse(error.isTokenError())
    }

    @Test
    fun rateLimitIsNotTokenError() {
        val error = GraphError(code = "TooManyRequests", message = "Rate limited")
        assertFalse(error.isTokenError())
    }

    // ===== GraphError data class =====

    @Test
    fun graphErrorHoldsCode() {
        val error = GraphError(code = "NotFound", message = "Not found")
        assertEquals("NotFound", error.code)
    }

    @Test
    fun graphErrorHoldsMessage() {
        val error = GraphError(code = "Error", message = "Something happened")
        assertEquals("Something happened", error.message)
    }

    @Test
    fun graphErrorInnerErrorDefaultsToNull() {
        val error = GraphError(code = "Error", message = "msg")
        assertNull(error.innerError)
    }

    @Test
    fun graphErrorInnerErrorCanBeSet() {
        val inner = GraphInnerError(
            date = "2023-06-15",
            requestId = "req-123",
            clientRequestId = "cli-456",
        )
        val error = GraphError(code = "Error", message = "msg", innerError = inner)
        assertEquals("req-123", error.innerError?.requestId)
    }

    // ===== GraphInnerError =====

    @Test
    fun graphInnerErrorHoldsDate() {
        val inner = GraphInnerError(
            date = "2023-06-15",
            requestId = "req-123",
            clientRequestId = "cli-456",
        )
        assertEquals("2023-06-15", inner.date)
    }

    @Test
    fun graphInnerErrorHoldsRequestId() {
        val inner = GraphInnerError(
            date = "2023-06-15",
            requestId = "request-abc",
            clientRequestId = "client-xyz",
        )
        assertEquals("request-abc", inner.requestId)
    }

    @Test
    fun graphInnerErrorHoldsClientRequestId() {
        val inner = GraphInnerError(
            date = "2023-06-15",
            requestId = "req-123",
            clientRequestId = "client-xyz",
        )
        assertEquals("client-xyz", inner.clientRequestId)
    }

    // ===== GraphErrorResponse =====

    @Test
    fun graphErrorResponseHoldsError() {
        val error = GraphError(code = "Error", message = "msg")
        val response = GraphErrorResponse(error = error)
        assertEquals("Error", response.error.code)
    }

    // ===== Exception types =====

    @Test
    fun networkExceptionMessage() {
        val ex = NetworkException("network error")
        assertEquals("network error", ex.message)
    }

    @Test
    fun networkExceptionWithCause() {
        val cause = RuntimeException("root cause")
        val ex = NetworkException("network error", cause)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun networkExceptionNullMessage() {
        val ex = NetworkException()
        assertNull(ex.message)
    }

    @Test
    fun unauthorizedExceptionIsNetworkException() {
        val ex = UnauthorizedException("unauthorized")
        assertTrue(ex is NetworkException)
        assertEquals("unauthorized", ex.message)
    }

    @Test
    fun notFoundExceptionIsNetworkException() {
        val ex = NotFoundException("not found")
        assertTrue(ex is NetworkException)
        assertEquals("not found", ex.message)
    }

    @Test
    fun serviceUnavailableExceptionIsNetworkException() {
        val ex = ServiceUnavailableException("unavailable")
        assertTrue(ex is NetworkException)
        assertEquals("unavailable", ex.message)
    }

    @Test
    fun httpExceptionHoldsCode() {
        val ex = HttpException(code = 429, message = "too many requests")
        assertEquals(429, ex.code)
        assertEquals("too many requests", ex.message)
    }

    @Test
    fun httpExceptionIsNetworkException() {
        val ex = HttpException(code = 400, message = "bad request")
        assertTrue(ex is NetworkException)
    }

    @Test
    fun httpExceptionNullMessage() {
        val ex = HttpException(code = 500)
        assertEquals(500, ex.code)
        assertNull(ex.message)
    }

    // ===== Plugin key =====

    @Test
    fun pluginKeyName() {
        assertEquals("HttpErrorHandler", HttpErrorHandler.key.name)
    }
}
