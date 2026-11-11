package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetError
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlan
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetConfig
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSetBudgetModalBottomSheet(
    yearMonth: YearMonth,
    complete: Boolean,
    conflict: QuickSetBudgetPlan?,
    error: QuickSetError?,
    onDismissRequest: () -> Unit,
    onDismissed: () -> Unit,
    onApply: (QuickSetConfig, ClosedDateSpan) -> Unit,
    onConfirmOverwrite: () -> Unit,
    onCancelOverwrite: () -> Unit,
) {
    val formState = rememberSaveable(yearMonth, saver = QuickSetFormState.saver) {
        QuickSetFormState(yearMonth)
    }
    val controller = rememberQuickSetSheetController(
        complete = complete,
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
            formState = formState,
            controller = controller,
            error = error,
            complete = complete,
            onCancel = {
                controller.onDismissRequest()
                onDismissRequest()
            },
            onApply = {
                if (controller.requestApply()) {
                    controller.onApply()
                    onApply(formState.config, formState.targetSpan)
                }
            },
        )
        if (conflict != null && !complete) {
            QuickSetConflictDialog(
                conflict = conflict,
                onConfirm = onConfirmOverwrite,
                onCancel = onCancelOverwrite,
            )
        }
    }
}
