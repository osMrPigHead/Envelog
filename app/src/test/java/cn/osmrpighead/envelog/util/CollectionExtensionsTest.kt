package cn.osmrpighead.envelog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionExtensionsTest {
    @Test
    fun toSetIfNonDuplicates_returnsSetWithoutLoss() {
        assertEquals(setOf(1, 2, 3), listOf(1, 2, 3).toSetIfNonDuplicates())
    }

    @Test
    fun toSetIfNonDuplicates_returnsNullOnDuplicate() {
        assertNull(listOf(1, 2, 1).toSetIfNonDuplicates())
    }

    @Test
    fun toSetIfNonDuplicates_returnsEmptySetForEmptyList() {
        assertEquals(emptySet<Int>(), emptyList<Int>().toSetIfNonDuplicates())
    }

    @Test
    fun toSetIfNonDuplicates_comparesElementsWithEquals() {
        assertNull(listOf("a", "a").toSetIfNonDuplicates())
        assertEquals(setOf("a", "b"), listOf("a", "b").toSetIfNonDuplicates())
    }
}
