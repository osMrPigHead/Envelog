package cn.osmrpighead.envelog.ui.util

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState

@OptIn(ExperimentalMaterial3Api::class)
suspend fun SheetState.hideAndThen(callback: suspend () -> Unit) {
    try {
        hide()
    } finally {
        callback()
    }
}
