package com.todoroo.astrid.adapter

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.filters.FilterListItem

class NavigationDrawerAdapterTest {

    @Test
    fun filterListItemTypesCount() {
        assertEquals(2, FilterListItem.Type.values().size)
    }

    @Test
    fun filterListItemTypeItemOrdinal() {
        assertEquals(0, FilterListItem.Type.ITEM.ordinal)
    }

    @Test
    fun filterListItemTypeSubheaderOrdinal() {
        assertEquals(1, FilterListItem.Type.SUBHEADER.ordinal)
    }

    @Test
    fun filterListItemTypeValueOf() {
        assertEquals(FilterListItem.Type.ITEM, FilterListItem.Type.valueOf("ITEM"))
        assertEquals(FilterListItem.Type.SUBHEADER, FilterListItem.Type.valueOf("SUBHEADER"))
    }
}
