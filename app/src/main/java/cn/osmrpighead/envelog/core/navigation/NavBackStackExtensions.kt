package cn.osmrpighead.envelog.core.navigation

import androidx.navigation3.runtime.NavBackStack

fun NavBackStack<AppRoute>.navigate(from: AppRoute, route: AppRoute) {
    if (lastOrNull()?.navId == from.navId) add(route)
}

fun NavBackStack<AppRoute>.pop(entry: AppRoute) {
    if (lastOrNull()?.navId == entry.navId) removeLastOrNull()
}
