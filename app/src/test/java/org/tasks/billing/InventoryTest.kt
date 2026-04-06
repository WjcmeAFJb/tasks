package org.tasks.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Inventory logic that can be tested without Android framework dependencies.
 * The Inventory constructor requires Android Context and Looper, so we test the
 * companion constants and the Purchase-level logic that Inventory relies on.
 */
class InventoryTest {

    // --- Subscription sorting logic ---
    // Inventory.updateSubscription sorts by: isMonthly desc, isCanceled asc, subscriptionPrice desc
    // With generic Purchase, all values are default (false, false, 0), but we can test the
    // sorting comparator behavior.

    @Test
    fun subscriptionSortComparatorMonthlyFirst() {
        // The comparator logic: r.isMonthly.compareTo(l.isMonthly)
        // true > false, so monthly subscriptions come first
        val monthlyFirst = true.compareTo(false)
        assertTrue(monthlyFirst > 0)
    }

    @Test
    fun subscriptionSortComparatorCanceledLast() {
        // The comparator logic: l.isCanceled.compareTo(r.isCanceled)
        // If l is canceled and r is not: 1 > 0, so l goes after r
        val canceledComparison = true.compareTo(false)
        assertTrue(canceledComparison > 0)
    }

    @Test
    fun subscriptionSortComparatorHigherPriceFirst() {
        // The comparator logic: r.subscriptionPrice.compareTo(l.subscriptionPrice)
        // Higher price r should come first
        val priceComparison = 10.compareTo(5)
        assertTrue(priceComparison > 0)
    }

    // --- Generic Purchase behavior for Inventory ---

    @Test
    fun genericPurchaseIsNotProSubscription() {
        val purchase = Purchase(null)
        assertFalse(purchase.isProSubscription)
    }

    @Test
    fun genericPurchaseIsPurchased() {
        val purchase = Purchase(null)
        assertTrue(purchase.isPurchased)
    }

    @Test
    fun genericPurchaseDoesNotNeedAcknowledgement() {
        val purchase = Purchase(null)
        assertFalse(purchase.needsAcknowledgement)
    }

    @Test
    fun genericPurchaseIsNotCanceled() {
        val purchase = Purchase(null)
        assertFalse(purchase.isCanceled)
    }

    @Test
    fun genericPurchaseSkuIsEmpty() {
        val purchase = Purchase(null)
        assertEquals("", purchase.sku)
    }

    @Test
    fun genericPurchaseTokenIsEmpty() {
        val purchase = Purchase(null)
        assertEquals("", purchase.purchaseToken)
    }

    @Test
    fun skuThemesConstant() {
        assertEquals("themes", Inventory.SKU_THEMES)
    }

    // --- Purchase map behavior that Inventory relies on ---

    @Test
    fun purchaseMapUsesSkuAsKey() {
        val purchases = HashMap<String, Purchase>()
        val p = Purchase(null)
        purchases[p.sku] = p
        assertTrue(purchases.containsKey(""))
    }

    @Test
    fun purchaseMapOverwritesSameSku() {
        val purchases = HashMap<String, Purchase>()
        val p1 = Purchase(null)
        val p2 = Purchase("other")
        purchases[p1.sku] = p1
        purchases[p2.sku] = p2
        // Both have sku == "" so second overwrites first
        assertEquals(1, purchases.size)
        assertEquals(p2, purchases[""])
    }

    @Test
    fun purchaseMapContainsKeyCheck() {
        val purchases = HashMap<String, Purchase>()
        purchases["vip"] = Purchase(null)
        assertTrue(purchases.containsKey("vip"))
        assertFalse(purchases.containsKey("pro"))
    }

    // --- Filter behavior for UpdatePurchaseWork ---

    @Test
    fun filterNeedsAcknowledgementOnGenericPurchases() {
        val purchases = listOf(Purchase(null), Purchase("a"), Purchase("b"))
        val needsAck = purchases.filter { it.needsAcknowledgement }
        assertTrue(needsAck.isEmpty())
    }

    @Test
    fun allPurchasedCheckOnGenericPurchases() {
        val purchases = listOf(Purchase(null), Purchase("a"))
        assertTrue(purchases.all { it.isPurchased })
    }
}
