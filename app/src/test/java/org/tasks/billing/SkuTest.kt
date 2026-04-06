package org.tasks.billing

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SkuTest {

    // --- construction ---

    @Test
    fun constructWithProductIdAndPrice() {
        val sku = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        assertEquals("tasks_pro_monthly", sku.productId)
        assertEquals("$4.99", sku.price)
    }

    @Test
    fun constructWithEmptyStrings() {
        val sku = Sku(productId = "", price = "")
        assertEquals("", sku.productId)
        assertEquals("", sku.price)
    }

    @Test
    fun constructWithUnicodePrice() {
        val sku = Sku(productId = "pro_yearly", price = "\u20AC9.99")
        assertEquals("\u20AC9.99", sku.price)
    }

    // --- equality ---

    @Test
    fun equalSkusAreEqual() {
        val sku1 = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val sku2 = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        assertEquals(sku1, sku2)
    }

    @Test
    fun differentProductIdsAreNotEqual() {
        val sku1 = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val sku2 = Sku(productId = "tasks_pro_yearly", price = "$4.99")
        assertNotEquals(sku1, sku2)
    }

    @Test
    fun differentPricesAreNotEqual() {
        val sku1 = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val sku2 = Sku(productId = "tasks_pro_monthly", price = "$9.99")
        assertNotEquals(sku1, sku2)
    }

    @Test
    fun notEqualToNull() {
        val sku = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        assertNotEquals(sku, null)
    }

    @Test
    fun notEqualToOtherType() {
        val sku = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        assertNotEquals(sku, "not a sku")
    }

    // --- hashCode ---

    @Test
    fun equalSkusHaveSameHashCode() {
        val sku1 = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val sku2 = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        assertEquals(sku1.hashCode(), sku2.hashCode())
    }

    @Test
    fun differentSkusHaveDifferentHashCodes() {
        val sku1 = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val sku2 = Sku(productId = "tasks_pro_yearly", price = "$49.99")
        assertNotEquals(sku1.hashCode(), sku2.hashCode())
    }

    // --- copy ---

    @Test
    fun copyPreservesValues() {
        val original = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val copy = original.copy()
        assertEquals(original, copy)
    }

    @Test
    fun copyWithModifiedProductId() {
        val original = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val copy = original.copy(productId = "tasks_pro_yearly")
        assertEquals("tasks_pro_yearly", copy.productId)
        assertEquals("$4.99", copy.price)
    }

    @Test
    fun copyWithModifiedPrice() {
        val original = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val copy = original.copy(price = "$9.99")
        assertEquals("tasks_pro_monthly", copy.productId)
        assertEquals("$9.99", copy.price)
    }

    // --- toString ---

    @Test
    fun toStringContainsProductId() {
        val sku = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val str = sku.toString()
        assert(str.contains("tasks_pro_monthly")) { "toString should contain productId" }
    }

    @Test
    fun toStringContainsPrice() {
        val sku = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val str = sku.toString()
        assert(str.contains("$4.99")) { "toString should contain price" }
    }

    // --- serialization ---

    @Test
    fun serializeToJson() {
        val sku = Sku(productId = "tasks_pro_monthly", price = "$4.99")
        val json = Json.encodeToString(sku)
        assert(json.contains("tasks_pro_monthly")) { "JSON should contain productId" }
        assert(json.contains("\$4.99")) { "JSON should contain price" }
    }

    @Test
    fun deserializeFromJson() {
        val json = """{"productId":"tasks_pro_monthly","price":"$4.99"}"""
        val sku = Json.decodeFromString<Sku>(json)
        assertEquals("tasks_pro_monthly", sku.productId)
        assertEquals("$4.99", sku.price)
    }

    @Test
    fun roundTripSerialization() {
        val original = Sku(productId = "tasks_pro_yearly", price = "$49.99")
        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<Sku>(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun deserializeWithExtraFieldsIgnored() {
        val json = """{"productId":"test","price":"$1.00","extra":"ignored"}"""
        val lenientJson = Json { ignoreUnknownKeys = true }
        val sku = lenientJson.decodeFromString<Sku>(json)
        assertEquals("test", sku.productId)
        assertEquals("$1.00", sku.price)
    }

    // --- destructuring ---

    @Test
    fun destructuringWorks() {
        val sku = Sku(productId = "pro", price = "$1.00")
        val (id, price) = sku
        assertEquals("pro", id)
        assertEquals("$1.00", price)
    }
}
