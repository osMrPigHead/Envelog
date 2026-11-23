package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.ui.component.ImeInvisibleBackHandler
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetField
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormModalState
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.withCurrency
import cn.osmrpighead.envelog.ui.util.hideAndThen
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSetBudgetModalBottomSheet(
    yearMonth: YearMonth,
    currency: CurrencyType,
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
        ImeInvisibleBackHandler { dismiss() }
        QuickSetForm(
            yearMonth = yearMonth,
            currency = currency,
            modalState = modalState,
            numKeypadHost = numKeypadHost,
            update = update,
            dismiss = dismiss,
            apply = {
                if (numKeypadHost.isOpen) numKeypadHost.safeClose()
                else apply(false)
            },
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        )
        if (modalState.state is FormState.Active && modalState.pendingOverwrite != null)
            QuickSetConflictDialog(
                currency = currency,
                conflicts = modalState.pendingOverwrite,
                onConfirm = { apply(true) },
                onCancel = cancelOverwrite,
            )
    }
}

@Composable
private fun QuickSetConflictDialog(
    currency: CurrencyType,
    conflicts: List<Budget>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val conflictTotal = conflicts.sumOf { it.amount }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.envelope_edit_quick_set_conflict_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.envelope_edit_quick_set_conflict_message,
                    conflicts.size,
                    conflictTotal.formatAsAmount(Formatter.AmountInteger).withCurrency(currency),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.envelope_edit_quick_set_conflict_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.common_action_cancel))
            }
        },
    )
}
