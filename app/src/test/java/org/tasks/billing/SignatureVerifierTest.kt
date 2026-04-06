package org.tasks.billing

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the generic SignatureVerifier (non-Google Play variant).
 * The generic version always returns true.
 */
class SignatureVerifierTest {

    @Test
    fun verifySignatureAlwaysReturnsTrueForGeneric() {
        val verifier = SignatureVerifier()
        val purchase = Purchase(null)
        assertTrue(verifier.verifySignature(purchase))
    }

    @Test
    fun verifySignatureReturnsTrueForAnyPurchase() {
        val verifier = SignatureVerifier()
        val purchase = Purchase("any_json")
        assertTrue(verifier.verifySignature(purchase))
    }

    @Test
    fun verifySignatureReturnsTrueForEmptyJsonPurchase() {
        val verifier = SignatureVerifier()
        val purchase = Purchase("")
        assertTrue(verifier.verifySignature(purchase))
    }
}
