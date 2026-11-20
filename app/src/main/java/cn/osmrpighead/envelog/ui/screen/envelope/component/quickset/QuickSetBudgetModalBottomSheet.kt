package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetField
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormModalState
import cn.osmrpighead.envelog.ui.util.hideAndThen
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        sheetState.hideAndThen(clear)
    }

    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        sheetGesturesEnabled = !sheetState.isAnimationRunning,
    ) {
        val imeVisible = WindowInsets.isImeVisible
        BackHandler {
            if (!imeVisible) dismiss()
        }
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
