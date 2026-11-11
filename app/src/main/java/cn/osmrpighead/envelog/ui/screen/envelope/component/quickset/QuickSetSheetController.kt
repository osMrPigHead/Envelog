package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
internal class QuickSetSheetController(
    val sheetState: SheetState,
    val numKeypadHost: NumKeypadHostState,
    private val focusManager: FocusManager,
    private val scope: CoroutineScope,
) {
    fun onTextInputFocus() {
        numKeypadHost.safeClose()
        scope.launch { sheetState.expand() }
    }

    fun onModeChange() {
        closeKeypadAndFocus()
    }

    fun onPickerOpen() {
        closeKeypadAndFocus()
    }

    fun onDismissRequest() {
        numKeypadHost.cancel()
    }

    fun onApply() {
        focusManager.clearFocus()
    }

    /** 返回 true 表示可以继续提交；键盘打开时只确认键盘，等待下一次点击。 */
    fun requestApply(): Boolean {
        if (numKeypadHost.isOpen) {
            numKeypadHost.confirm()
            return false
        }
        return true
    }

    private fun closeKeypadAndFocus() {
        numKeypadHost.safeClose()
        focusManager.clearFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberQuickSetSheetController(
    complete: Boolean,
    onDismissed: () -> Unit,
): QuickSetSheetController {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val numKeypadHost = rememberNumKeypadHostState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val controller = remember(sheetState, numKeypadHost) {
        QuickSetSheetController(sheetState, numKeypadHost, focusManager, scope)
    }

    LaunchedEffect(controller) {
        sheetState.show()
    }

    LaunchedEffect(complete) {
        if (!complete) return@LaunchedEffect
        numKeypadHost.cancel()
        sheetState.hide()
        onDismissed()
    }

    return controller
}
