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
    onUpdate: (QuickSetFormState) -> Unit,
    onSubmit: () -> Unit,
    onDismissRequest: () -> Unit,
    onDismissed: () -> Unit,
    onConfirmOverwrite: () -> Unit,
    onCancelOverwrite: () -> Unit,
) {
    val state = modalState.state
    val controller = rememberQuickSetSheetController(
        isCompleted = state is FormState.Completed,
        onDismissed = onDismissed,
    )

    ModalBottomSheet(
        onDismissRequest = {
            controller.onDismissRequest()
            onDismissRequest()
        },
        sheetState = controller.sheetState,
    ) {
        QuickSetSheetContent(
            yearMonth = yearMonth,
            modalState = modalState,
            controller = controller,
            onUpdate = onUpdate,
            onCancel = {
                controller.onDismissRequest()
                onDismissRequest()
            },
            onApply = {
                if (controller.requestApply(state is FormState.Busy)) {
                    controller.onApply()
                    onSubmit()
                }
            },
        )
        val error = (state as? FormState.Active)?.error
        if (error is QuickSetFormError.BudgetOverlaps) QuickSetConflictDialog(
            conflicts = error.conflicts,
            onConfirm = onConfirmOverwrite,
            onCancel = onCancelOverwrite,
        )
    }
}
