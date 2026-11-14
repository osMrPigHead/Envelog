package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalFocusManager
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormError
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormModalState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetFormState
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSetBudgetModalBottomSheet(
    yearMonth: YearMonth,
    modalState: QuickSetFormModalState,
    update: (QuickSetFormState) -> Unit,
    apply: (Boolean) -> Unit,
    dismiss: () -> Unit,
    clear: () -> Unit,
    cancelOverwrite: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val numKeypadHost = rememberNumKeypadHostState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(sheetState) {
        sheetState.show()
    }

    LaunchedEffect(modalState.state, modalState.dismissed) {
        if (modalState.state != FormState.Completed && !modalState.dismissed) return@LaunchedEffect
        if (sheetState.isVisible) sheetState.hide()
        clear()
    }

    val closeKeypadAndFocus: () -> Unit = {
        numKeypadHost.safeClose()
        focusManager.clearFocus()
    }

    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
    ) {
        QuickSetSheetContent(
            yearMonth = yearMonth,
            modalState = modalState,
            numKeypadHost = numKeypadHost,
            onModeChange = closeKeypadAndFocus,
            onTextInputFocus = {
                numKeypadHost.safeClose()
                scope.launch { sheetState.expand() }
            },
            onPickerOpen = closeKeypadAndFocus,
            onUpdate = update,
            onCancel = dismiss,
            // 键盘打开时第一次点击只确认键盘，等待下一次点击才提交；提交中不提交。
            onApply = {
                when {
                    modalState.state is FormState.Busy -> {}
                    numKeypadHost.isOpen -> numKeypadHost.confirm()
                    else -> {
                        focusManager.clearFocus()
                        apply(false)
                    }
                }
            },
        )
        if (modalState.state is FormState.Active && modalState.state.error is QuickSetFormError.BudgetOverlaps)
            QuickSetConflictDialog(
                conflicts = modalState.state.error.conflicts,
                onConfirm = { apply(true) },
                onCancel = cancelOverwrite,
            )
    }
}
