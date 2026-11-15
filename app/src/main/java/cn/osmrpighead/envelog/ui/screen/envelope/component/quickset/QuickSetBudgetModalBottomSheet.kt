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
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetField
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormModalState
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSetBudgetModalBottomSheet(
    yearMonth: YearMonth,
    modalState: QuickSetFormModalState,
    update: (QuickSetField) -> Unit,
    apply: (Boolean) -> Unit,
    dismiss: () -> Unit,
    clear: () -> Unit,
    cancelOverwrite: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val numKeypadHost = rememberNumKeypadHostState()

    LaunchedEffect(sheetState) {
        sheetState.show()
    }

    LaunchedEffect(modalState.state, modalState.dismissed) {
        if (modalState.state != FormState.Completed && !modalState.dismissed) return@LaunchedEffect
        if (sheetState.isVisible) sheetState.hide()
        clear()
    }

    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
    ) {
        QuickSetSheetContent(
            yearMonth, modalState, numKeypadHost, update, dismiss,
            apply = {
                if (numKeypadHost.isOpen) numKeypadHost.safeClose()
                else apply(false)
            },
        )
        if (modalState.state is FormState.Active && modalState.pendingOverwrite != null)
            QuickSetConflictDialog(
                conflicts = modalState.pendingOverwrite,
                onConfirm = { apply(true) },
                onCancel = cancelOverwrite,
            )
    }
}
