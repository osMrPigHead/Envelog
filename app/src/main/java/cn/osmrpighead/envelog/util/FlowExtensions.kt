package cn.osmrpighead.envelog.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, reified R> Flow<List<T>>.mapEach(crossinline action: (T) -> Flow<R>): Flow<List<R>> = flatMapLatest {
    if (it.isEmpty()) flowOf(emptyList())
    else combine(it.map(action)) { arr -> arr.toList() }
}
