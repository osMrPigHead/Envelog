package cn.osmrpighead.envelog.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, reified R> Flow<List<T>>.mapEach(crossinline action: (T) -> Flow<R>): Flow<List<R>> = flatMapLatest {
    if (it.isEmpty()) flowOf(emptyList())
    else combine(it.map(action)) { arr -> arr.toList() }
}

inline fun <T> MutableStateFlow<T?>.updateNotNull(
    fallback: T? = null,
    crossinline function: (T) -> T?,
) = update {
    if (it != null) function(it)
    else fallback
}
