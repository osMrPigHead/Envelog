package cn.osmrpighead.envelog.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

fun <T : NavKey> NavBackStack<T>.navigate(from: T, route: T) {
    if (lastOrNull() === from) add(route)
}

fun <T : NavKey> NavBackStack<T>.pop(entry: T) {
    if (lastOrNull() === entry) removeLastOrNull()
}
