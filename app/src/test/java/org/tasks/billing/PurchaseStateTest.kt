package org.tasks.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseStateTest {

    private class ProUser : PurchaseState {
        override val hasPro: Boolean = true
    }

    private class FreeUser : PurchaseState {
        override val hasPro: Boolean = false
    }

    private class CustomThemeUser(
        override val hasPro: Boolean,
        private val customThemes: Boolean,
    ) : PurchaseState {
        override fun purchasedThemes(): Boolean = customThemes
    }

    // --- hasPro tests ---

    @Test
    fun proUserHasPro() {
        val state = ProUser()
        assertTrue(state.hasPro)
    }

    @Test
    fun freeUserDoesNotHavePro() {
        val state = FreeUser()
        assertFalse(state.hasPro)
    }

    // --- purchasedThemes default behavior ---

    @Test
    fun proUserGetsPurchasedThemesByDefault() {
        val state = ProUser()
        assertTrue(state.purchasedThemes())
    }

    @Test
    fun freeUserDoesNotGetPurchasedThemesByDefault() {
        val state = FreeUser()
        assertFalse(state.purchasedThemes())
    }

    @Test
    fun purchasedThemesMatchesHasProByDefault() {
        val pro = ProUser()
        val free = FreeUser()
        assertTrue(pro.purchasedThemes() == pro.hasPro)
        assertTrue(free.purchasedThemes() == free.hasPro)
    }

    // --- overridden purchasedThemes ---

    @Test
    fun overriddenPurchasedThemesCanReturnTrueWithoutPro() {
        val state = CustomThemeUser(hasPro = false, customThemes = true)
        assertFalse(state.hasPro)
        assertTrue(state.purchasedThemes())
    }

    @Test
    fun overriddenPurchasedThemesCanReturnFalseWithPro() {
        val state = CustomThemeUser(hasPro = true, customThemes = false)
        assertTrue(state.hasPro)
        assertFalse(state.purchasedThemes())
    }

    @Test
    fun overriddenPurchasedThemesBothTrue() {
        val state = CustomThemeUser(hasPro = true, customThemes = true)
        assertTrue(state.hasPro)
        assertTrue(state.purchasedThemes())
    }

    @Test
    fun overriddenPurchasedThemesBothFalse() {
        val state = CustomThemeUser(hasPro = false, customThemes = false)
        assertFalse(state.hasPro)
        assertFalse(state.purchasedThemes())
    }

    // --- interface contract tests ---

    @Test
    fun multipleImplementationsHaveIndependentState() {
        val pro = ProUser()
        val free = FreeUser()
        assertTrue(pro.hasPro)
        assertFalse(free.hasPro)
    }

    @Test
    fun defaultThemeMethodDelegatesToHasPro() {
        // Verify the default implementation by creating an anonymous object
        val proAnon = object : PurchaseState {
            override val hasPro = true
        }
        val freeAnon = object : PurchaseState {
            override val hasPro = false
        }
        assertTrue(proAnon.purchasedThemes())
        assertFalse(freeAnon.purchasedThemes())
    }
}
