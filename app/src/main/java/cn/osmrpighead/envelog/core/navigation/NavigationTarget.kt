package cn.osmrpighead.envelog.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavigationTarget<T> {
    @Serializable
    data class Existing<T>(val value: T) : NavigationTarget<T>
    @Serializable
    class New<T> : NavigationTarget<T>
}
