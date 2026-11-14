package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope2.QuickSetFormError
import cn.osmrpighead.envelog.ui.screen.envelope2.QuickSetFormModalState
import cn.osmrpighead.envelog.ui.screen.envelope2.model.QuickSetFormState
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
    val controller = rememberQuickSetSheetController(
        modalState = modalState,
        clear = clear,
    )

    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = controller.sheetState,
    ) {
        QuickSetSheetContent(
            yearMonth = yearMonth,
            modalState = modalState,
            controller = controller,
            onUpdate = update,
            onCancel = dismiss,
            onApply = {
                if (controller.requestApply(modalState.state is FormState.Busy)) {
                    controller.onApply()
                    apply(false)
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
