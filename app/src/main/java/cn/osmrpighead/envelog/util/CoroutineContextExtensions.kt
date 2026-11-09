package cn.osmrpighead.envelog.util

import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

fun CoroutineContext.wrapSuspending(block: suspend () -> Unit) : suspend () -> Unit = { withContext(this) { block() } }
