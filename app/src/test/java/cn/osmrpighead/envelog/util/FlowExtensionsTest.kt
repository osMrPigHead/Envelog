package cn.osmrpighead.envelog.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlowExtensionsTest {
    @Test
    fun mapEach_emptyList_emitsEmptyWithoutCallingAction() = runTest {
        var calls = 0

        val result = flowOf(emptyList<Int>()).mapEach { calls++; flowOf(it) }.first()

        assertEquals(emptyList<Int>(), result)
        assertEquals(0, calls)
    }

    @Test
    fun mapEach_combinesChildFlowsInListOrder() = runTest {
        val first = MutableStateFlow(1)
        val second = MutableStateFlow(2)
        val values = mutableListOf<List<Int>>()

        val job = launch {
            flowOf(listOf(first, second)).mapEach { it.map { value -> value } }.collect { values += it }
        }
        runCurrent()

        assertEquals(listOf(listOf(1, 2)), values)
        job.cancel()
    }

    @Test
    fun mapEach_reemitsWhenChildFlowChanges() = runTest {
        val item = MutableStateFlow(5)
        val source = MutableStateFlow(listOf(item))
        val values = mutableListOf<Int>()

        val job = launch {
            source.mapEach { it.map { value -> value } }.collect { values += it.sum() }
        }
        runCurrent()
        item.value = 7
        runCurrent()

        assertEquals(listOf(5, 7), values)
        job.cancel()
    }

    @Test
    fun mapEach_remapsWhenSourceListChanges() = runTest {
        val source = MutableStateFlow(listOf(1, 2))
        val values = mutableListOf<List<Int>>()

        val job = launch {
            source.mapEach { flowOf(it * 10) }.collect { values += it }
        }
        runCurrent()
        source.value = listOf(3)
        runCurrent()

        assertEquals(listOf(listOf(10, 20), listOf(30)), values)
        job.cancel()
    }

    @Test
    fun mapEach_emptySourceListAfterNonEmpty_emitsEmptyList() = runTest {
        val source = MutableStateFlow(listOf(1))
        val values = mutableListOf<List<Int>>()

        val job = launch {
            source.mapEach { flowOf(it) }.collect { values += it }
        }
        runCurrent()
        source.value = emptyList()
        runCurrent()

        assertEquals(listOf(listOf(1), emptyList()), values)
        job.cancel()
    }
}
