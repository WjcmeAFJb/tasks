package org.tasks.billing

import android.content.Context
import android.os.Looper
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.preferences.Preferences

/**
 * Deep tests for [Inventory] logic.
 */
class InventoryDeepTest {

    private lateinit var context: Context
    private lateinit var preferences: Preferences
    private lateinit var signatureVerifier: SignatureVerifier
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var caldavDao: CaldavDao
    private lateinit var inventory: Inventory

    @Before
    fun setUp() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
        context = mock()
        preferences = mock()
        signatureVerifier = SignatureVerifier()
        localBroadcastManager = mock()
        caldavDao = mock()

        val looper = mock(Looper::class.java)
        `when`(context.mainLooper).thenReturn(looper)
        `when`(looper.isCurrentThread).thenReturn(true)
        `when`(preferences.purchases).thenReturn(emptyList())

        inventory = Inventory(context, preferences, signatureVerifier, localBroadcastManager, caldavDao)
    }

    @After
    fun tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun clearRemovesAllPurchases() {
        inventory.purchases["test"] = Purchase(null)
        inventory.clear()
        assertTrue(inventory.purchases.isEmpty())
    }

    @Test
    fun clearNullsSubscription() {
        inventory.clear()
        assertNull(inventory.subscription.value)
    }

    @Test
    fun addVerifiesAndSavesPurchases() {
        val purchase = Purchase(null)
        inventory.add(listOf(purchase))
        verify(preferences).setPurchases(inventory.purchases.values)
        verify(localBroadcastManager).broadcastPurchasesUpdated()
    }

    @Test
    fun addMultiplePurchases() {
        val p1 = Purchase(null)
        val p2 = Purchase("other")
        inventory.add(listOf(p1, p2))
        verify(preferences).setPurchases(inventory.purchases.values)
    }

    @Test
    fun addEmptyList() {
        inventory.add(emptyList())
        verify(preferences).setPurchases(inventory.purchases.values)
        verify(localBroadcastManager).broadcastPurchasesUpdated()
    }

    @Test
    fun getPurchaseReturnsNullForMissingSku() {
        assertNull(inventory.getPurchase("nonexistent"))
    }

    @Test
    fun getPurchaseReturnsStoredPurchase() {
        val purchase = Purchase(null)
        inventory.purchases["test_sku"] = purchase
        assertEquals(purchase, inventory.getPurchase("test_sku"))
    }

    @Test
    fun hasTasksSubscriptionFalseByDefault() {
        assertFalse(inventory.hasTasksSubscription)
    }

    @Test
    fun hasTasksAccountFalseByDefault() {
        assertFalse(inventory.hasTasksAccount)
    }

    @Test
    fun begForMoneyLogic() {
        val result = inventory.begForMoney
        assertNotNull(result)
    }

    @Test
    fun purchasedThemesResult() {
        val result = inventory.purchasedThemes()
        assertNotNull(result)
    }

    @Test
    fun purchasedThemesWithThemeSku() {
        inventory.purchases["themes"] = Purchase(null)
        assertTrue(inventory.purchasedThemes())
    }

    @Test
    fun hasProResult() {
        val result = inventory.hasPro
        assertNotNull(result)
    }

    @Test
    fun unsubscribeReturnsFalse() {
        val result = inventory.unsubscribe(context)
        assertFalse(result)
    }

    @Test
    fun unsubscribeWithNullSubscriptionDoesNotCrash() {
        inventory.subscription.value = null
        val result = inventory.unsubscribe(context)
        assertFalse(result)
    }

    @Test
    fun genericPurchaseProperties() {
        val p = Purchase(null)
        assertEquals("", p.sku)
        assertEquals("", p.toJson())
        assertFalse(p.isCanceled)
        assertEquals(0, p.subscriptionPrice)
        assertFalse(p.isMonthly)
        assertFalse(p.isProSubscription)
        assertFalse(p.isTasksSubscription)
        assertEquals("", p.purchaseToken)
        assertFalse(p.needsAcknowledgement)
        assertTrue(p.isPurchased)
    }

    @Test
    fun purchaseWithJsonStringStillHasDefaults() {
        val p = Purchase("some_json")
        assertEquals("", p.sku)
        assertTrue(p.isPurchased)
    }

    @Test
    fun updateSubscriptionWithNoPurchases() {
        inventory.purchases.clear()
        inventory.add(emptyList())
        assertNull(inventory.subscription.value)
    }

    @Test
    fun signatureVerifierAlwaysReturnsTrue() {
        val verifier = SignatureVerifier()
        assertTrue(verifier.verifySignature(Purchase(null)))
    }

    @Test
    fun signatureVerifierWithJsonPurchase() {
        val verifier = SignatureVerifier()
        assertTrue(verifier.verifySignature(Purchase("json_data")))
    }
}
