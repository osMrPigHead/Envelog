package cn.osmrpighead.envelog.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 仅当 [from] 仍是栈顶时才入栈。
 *
 * 页面的转场结束前依然留在组合中并接受点击，此时重复点击会产生多次导航请求。
 * 以“发起导航的页面是否仍在栈顶”作为唯一判据：页面不必保存跨点击的标记，
 * 也就不存在标记未被重置、页面再也无法导航的情况。
 */
fun <T : NavKey> NavBackStack<T>.navigate(from: T, route: T) {
    if (lastOrNull() === from) add(route)
}

/**
 * 仅当 [entry] 仍是栈顶时才出栈，[entry] 为进入该页面时传入的路由实例。
 */
fun <T : NavKey> NavBackStack<T>.pop(entry: T) {
    if (lastOrNull() === entry) removeLastOrNull()
}
