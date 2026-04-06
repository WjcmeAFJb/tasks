package org.tasks.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the generic Purchase class (non-Google Play variant).
 */
class PurchaseTest {

    @Test
    fun constructFromNullJson() {
        val purchase = Purchase(null)
        assertEquals("", purchase.sku)
    }

    @Test
    fun constructFromEmptyJson() {
        val purchase = Purchase("")
        assertEquals("", purchase.sku)
    }

    @Test
    fun constructFromAnyJson() {
        val purchase = Purchase("{\"sku\": \"pro\"}")
        assertEquals("", purchase.sku)
    }

    @Test
    fun toJsonReturnsEmptyString() {
        val purchase = Purchase(null)
        assertEquals("", purchase.toJson())
    }

    @Test
    fun isCanceledIsFalse() {
        val purchase = Purchase(null)
        assertFalse(purchase.isCanceled)
    }

    @Test
    fun subscriptionPriceIsZero() {
        val purchase = Purchase(null)
        assertEquals(0, purchase.subscriptionPrice)
    }

    @Test
    fun isMonthlyIsFalse() {
        val purchase = Purchase(null)
        assertFalse(purchase.isMonthly)
    }

    @Test
    fun isProSubscriptionIsFalse() {
        val purchase = Purchase(null)
        assertFalse(purchase.isProSubscription)
    }

    @Test
    fun isTasksSubscriptionIsFalse() {
        val purchase = Purchase(null)
        assertFalse(purchase.isTasksSubscription)
    }

    @Test
    fun purchaseTokenIsEmpty() {
        val purchase = Purchase(null)
        assertEquals("", purchase.purchaseToken)
    }

    @Test
    fun needsAcknowledgementIsFalse() {
        val purchase = Purchase(null)
        assertFalse(purchase.needsAcknowledgement)
    }

    @Test
    fun isPurchasedIsTrue() {
        val purchase = Purchase(null)
        assertTrue(purchase.isPurchased)
    }

    @Test
    fun skuIsEmptyString() {
        val purchase = Purchase("any")
        assertEquals("", purchase.sku)
    }
}
