package org.tasks.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryConstantsTest {

    @Test
    fun skuThemesConstant() {
        assertEquals("themes", Inventory.SKU_THEMES)
    }

    @Test
    fun skuVipIsAccessibleViaReflection() {
        val field = Inventory::class.java.getDeclaredField("SKU_VIP")
        field.isAccessible = true
        assertEquals("vip", field.get(null))
    }
}
