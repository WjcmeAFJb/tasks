package org.tasks.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigurationTest {

    // --- InvalidConfigurationException tests ---

    @Test
    fun invalidConfigurationExceptionWithReason() {
        val ex = Configuration.InvalidConfigurationException("test reason")
        assertEquals("test reason", ex.message)
    }

    @Test
    fun invalidConfigurationExceptionWithReasonAndCause() {
        val cause = RuntimeException("root cause")
        val ex = Configuration.InvalidConfigurationException("test reason", cause)
        assertEquals("test reason", ex.message)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun invalidConfigurationExceptionWithNullReason() {
        val ex = Configuration.InvalidConfigurationException(null as String?)
        assertNull(ex.message)
    }

    @Test
    fun invalidConfigurationExceptionWithNullCause() {
        val ex = Configuration.InvalidConfigurationException("reason", null)
        assertEquals("reason", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun invalidConfigurationExceptionIsException() {
        val ex = Configuration.InvalidConfigurationException("test")
        assertTrue(ex is Exception)
    }

    @Test
    fun invalidConfigurationExceptionIsThrowable() {
        val ex = Configuration.InvalidConfigurationException("test")
        assertTrue(ex is Throwable)
    }

    @Test
    fun invalidConfigurationExceptionMessagePreserved() {
        val msg = "A very specific error message about configuration"
        val ex = Configuration.InvalidConfigurationException(msg)
        assertEquals(msg, ex.message)
    }

    @Test
    fun invalidConfigurationExceptionCauseChain() {
        val root = IllegalArgumentException("root")
        val mid = RuntimeException("mid", root)
        val ex = Configuration.InvalidConfigurationException("top", mid)
        assertEquals(mid, ex.cause)
        assertEquals(root, ex.cause?.cause)
    }

    @Test
    fun invalidConfigurationExceptionCanBeCaught() {
        var caught = false
        try {
            throw Configuration.InvalidConfigurationException("test")
        } catch (e: Exception) {
            caught = true
        }
        assertTrue(caught)
    }

    @Test
    fun invalidConfigurationExceptionEmptyMessage() {
        val ex = Configuration.InvalidConfigurationException("")
        assertEquals("", ex.message)
    }

    // --- Companion object tests ---

    @Test
    fun googleConfigIsAccessible() {
        assertNotNull(Configuration.GOOGLE_CONFIG)
    }

    @Test
    fun githubConfigIsAccessible() {
        assertNotNull(Configuration.GITHUB_CONFIG)
    }

    @Test
    fun googleConfigIsResourceId() {
        assertTrue(Configuration.GOOGLE_CONFIG > 0)
    }

    @Test
    fun githubConfigIsResourceId() {
        assertTrue(Configuration.GITHUB_CONFIG > 0)
    }

    @Test
    fun googleAndGithubConfigsAreDifferent() {
        assertFalse(Configuration.GOOGLE_CONFIG == Configuration.GITHUB_CONFIG)
    }
}
