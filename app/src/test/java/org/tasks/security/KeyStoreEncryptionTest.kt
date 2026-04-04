package org.tasks.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreEncryptionTest {

    private lateinit var encryption: KeyStoreEncryption

    private class TestKeyProvider : KeyProvider {
        private val key: SecretKey = KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey()

        override fun getKey(): SecretKey = key
    }

    @Before
    fun setUp() {
        encryption = KeyStoreEncryption(TestKeyProvider())
    }

    @Test
    fun encryptReturnsNonNullForValidInput() {
        val result = encryption.encrypt("hello")
        assertNotNull(result)
    }

    @Test
    fun decryptReturnsOriginalText() {
        val plaintext = "hello world"
        val encrypted = encryption.encrypt(plaintext)
        val decrypted = encryption.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun roundtripWithEmptyString() {
        val plaintext = ""
        val encrypted = encryption.encrypt(plaintext)
        assertNotNull(encrypted)
        val decrypted = encryption.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun roundtripWithSpecialCharacters() {
        val plaintext = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"
        val encrypted = encryption.encrypt(plaintext)
        val decrypted = encryption.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun roundtripWithUnicodeCharacters() {
        val plaintext = "\u00e9\u00e8\u00ea\u00eb \u00fc\u00f6\u00e4 \u4e16\u754c \ud83c\udf0d"
        val encrypted = encryption.encrypt(plaintext)
        val decrypted = encryption.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun roundtripWithLongString() {
        val plaintext = "a".repeat(10_000)
        val encrypted = encryption.encrypt(plaintext)
        val decrypted = encryption.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun roundtripWithNewlines() {
        val plaintext = "line1\nline2\nline3"
        val encrypted = encryption.encrypt(plaintext)
        val decrypted = encryption.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun decryptNullReturnsNull() {
        val result = encryption.decrypt(null)
        assertNull(result)
    }

    @Test
    fun decryptEmptyStringReturnsNull() {
        val result = encryption.decrypt("")
        assertNull(result)
    }

    @Test
    fun decryptBlankStringReturnsNull() {
        val result = encryption.decrypt("   ")
        assertNull(result)
    }

    @Test
    fun encryptProducesValidBase64() {
        val encrypted = encryption.encrypt("test")!!
        // Should not throw
        val decoded = Base64.getDecoder().decode(encrypted)
        assertTrue(decoded.isNotEmpty())
    }

    @Test
    fun encryptedOutputContainsIvAndCiphertext() {
        val encrypted = encryption.encrypt("test")!!
        val decoded = Base64.getDecoder().decode(encrypted)
        // GCM IV is 12 bytes; ciphertext + 16-byte tag must follow
        assertTrue("Decoded output must be longer than 12 bytes (IV)", decoded.size > 12)
    }

    @Test
    fun encryptProducesDifferentCiphertextEachTime() {
        val plaintext = "same input"
        val encrypted1 = encryption.encrypt(plaintext)
        val encrypted2 = encryption.encrypt(plaintext)
        // Due to random IV, encryptions of the same plaintext should differ
        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun decryptWithInvalidBase64ReturnsEmpty() {
        // "!!!" is not valid base64
        val result = encryption.decrypt("!!!")
        assertEquals("", result)
    }

    @Test
    fun decryptWithTamperedCiphertextReturnsEmpty() {
        val encrypted = encryption.encrypt("secret data")!!
        val decoded = Base64.getDecoder().decode(encrypted)
        // Flip a byte in the ciphertext portion (after the 12-byte IV)
        decoded[decoded.size - 1] = (decoded[decoded.size - 1].toInt() xor 0xFF).toByte()
        val tampered = Base64.getEncoder().encodeToString(decoded)
        val result = encryption.decrypt(tampered)
        // Should fail decryption due to GCM auth tag mismatch
        assertEquals("", result)
    }

    @Test
    fun decryptWithDifferentKeyReturnsEmpty() {
        val encrypted = encryption.encrypt("secret")
        val otherEncryption = KeyStoreEncryption(TestKeyProvider())
        val result = otherEncryption.decrypt(encrypted)
        // Different key should fail GCM authentication
        assertEquals("", result)
    }

    @Test
    fun roundtripWithWhitespaceOnlyContent() {
        val plaintext = "   \t\n  "
        val encrypted = encryption.encrypt(plaintext)
        val decrypted = encryption.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun multipleSequentialEncryptDecryptCycles() {
        val messages = listOf("first", "second", "third", "fourth", "fifth")
        for (msg in messages) {
            val encrypted = encryption.encrypt(msg)
            val decrypted = encryption.decrypt(encrypted)
            assertEquals(msg, decrypted)
        }
    }

    @Test
    fun decryptWithTruncatedDataReturnsEmpty() {
        val encrypted = encryption.encrypt("some data")!!
        val decoded = Base64.getDecoder().decode(encrypted)
        // Truncate to just the IV (12 bytes) with no ciphertext
        val truncated = Base64.getEncoder().encodeToString(decoded.copyOfRange(0, 12))
        val result = encryption.decrypt(truncated)
        assertEquals("", result)
    }

    @Test
    fun encryptedOutputLongerThanPlaintext() {
        val plaintext = "short"
        val encrypted = encryption.encrypt(plaintext)!!
        val decoded = Base64.getDecoder().decode(encrypted)
        // IV (12) + ciphertext (>=plaintext length) + GCM tag (16)
        assertTrue(decoded.size >= plaintext.toByteArray().size + 12 + 16)
    }
}
