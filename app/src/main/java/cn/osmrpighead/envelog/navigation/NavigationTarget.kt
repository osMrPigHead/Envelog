package cn.osmrpighead.envelog.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavigationTarget<T> {
    @Serializable
    data class Existing<T>(val value: T) : NavigationTarget<T>
    @Serializable
    class New<T> : NavigationTarget<T>
}

suspend fun <T, R> NavigationTarget<T>.loadSuspend(existing: suspend (T) -> R, new: suspend () -> R): R = when (this) {
    is NavigationTarget.Existing<T> -> existing(value)
    is NavigationTarget.New<T> -> new()
}
